from pathlib import Path
import json,time,wave,hashlib,argparse
import numpy as np
import onnxruntime as ort
import sherpa_onnx
import kaldi_native_fbank as knf
parser=argparse.ArgumentParser();parser.add_argument('--models-root',type=Path,required=True);root=parser.parse_args().models_root
base=root/'models'

def audio(path):
 with wave.open(str(path)) as w:
  assert w.getnchannels()==1 and w.getsampwidth()==2 and w.getframerate()==16000
  return np.frombuffer(w.readframes(w.getnframes()),dtype='<i2').astype(np.float32)/32768

class Dolphin:
 def __init__(self,path):
  opts=ort.SessionOptions();opts.intra_op_num_threads=2;opts.inter_op_num_threads=1
  self.model=ort.InferenceSession(str(path),sess_options=opts,providers=['CPUExecutionProvider'])
  self.tokens={int(line.rsplit(' ',1)[1]):line.rsplit(' ',1)[0] for line in (root/'dolphin-export/tokens.txt').read_text(encoding='utf-8').splitlines()}
  self.reset()
 def reset(self):
  options=knf.FbankOptions();options.frame_opts.samp_freq=16000;options.frame_opts.dither=0;options.frame_opts.snip_edges=True;options.mel_opts.num_bins=80;options.mel_opts.high_freq=0
  self.fb=knf.OnlineFbank(options);self.frame=0;self.offset=0;self.att=np.zeros((6,8,0,128),np.float32);self.cnn=np.zeros((6,1,1024,30),np.float32);self.last=-1;self.text=''
 def accept(self,samples):
  self.fb.accept_waveform(16000,(samples*32768).tolist());self.decode()
 def decode(self):
  while self.fb.num_frames_ready-self.frame>=67:
   features=np.array([self.fb.get_frame(self.frame+i) for i in range(67)],dtype=np.float32)[None]
   ids,self.att,self.cnn=self.model.run(None,dict(features=features,offset=np.array(self.offset,np.int64),attention_cache=self.att,convolution_cache=self.cnn,attention_mask=np.zeros((1,1,self.att.shape[2]+16),np.float32)))
   for index in ids[0]:
    t=self.tokens.get(int(index),'')
    if index!=0 and index!=self.last and not t.startswith('<'):self.text+=t.replace('▁',' ')
    self.last=index
   self.frame+=64;self.offset+=16
   assert self.att.shape[2]<=64 and self.cnn.shape==(6,1,1024,30)
 def finish(self):
  self.accept(np.zeros(16000,np.float32));self.fb.input_finished();self.decode();return self.text

results=[]
ids=['paraformer-streaming-zh-en','paraformer-streaming-zh-yue-en','zipformer-ctc-small-zh','zipformer-ctc-multi-zh','dolphin-streaming-cn-base']
for mid in ids:
 folder=base/mid;wav=next((folder/'test_wavs').glob('*.wav')) if (folder/'test_wavs').exists() else base/'paraformer-streaming-zh-en/test_wavs/0.wav'
 samples=audio(wav);duration=len(samples)/16000;timings=[];partial=False
 if mid.startswith('paraformer'):
  model=sherpa_onnx.OnlineRecognizer.from_paraformer(tokens=str(folder/'tokens.txt'),encoder=str(folder/'encoder.int8.onnx'),decoder=str(folder/'decoder.int8.onnx'),num_threads=2,enable_endpoint_detection=False)
 elif mid.startswith('zipformer'):
  model=sherpa_onnx.OnlineRecognizer.from_zipformer2_ctc(tokens=str(folder/'tokens.txt'),model=str(folder/'model.int8.onnx'),num_threads=2,enable_endpoint_detection=False)
 else:model=Dolphin(root/'dolphin-export/model.int8.onnx')
 texts=[]
 for repeat in range(2):
  if isinstance(model,Dolphin):model.reset();assert model.text==''
  else:stream=model.create_stream();assert model.get_result(stream)==''
  start=time.perf_counter()
  for offset in range(0,len(samples),1600):
   part=samples[offset:offset+1600]
   if isinstance(model,Dolphin):model.accept(part);text=model.text
   else:
    stream.accept_waveform(16000,part)
    while model.is_ready(stream):model.decode_stream(stream)
    text=model.get_result(stream)
   if text and offset+len(part)<len(samples):partial=True
  if isinstance(model,Dolphin):text=model.finish()
  else:
   stream.accept_waveform(16000,np.zeros(16000,np.float32));stream.input_finished()
   while model.is_ready(stream):model.decode_stream(stream)
   text=model.get_result(stream)
  timings.append(time.perf_counter()-start);texts.append(text)
 assert partial and texts[0] and texts[0]==texts[1],(mid,partial,texts)
 record=dict(id=mid,partialBeforeEnd=partial,resetProducesSameText=True,audioSeconds=duration,desktopDecodeSeconds=timings,finalCharacters=len(texts[0]),textSha256=hashlib.sha256(texts[0].encode()).hexdigest(),phoneTest=False)
 results.append(record);print(json.dumps(record),flush=True);print('PUBLIC_SAMPLE_TRANSCRIPT',mid,texts[0],flush=True)
(root/'streaming-cpu-proof.json').write_text(json.dumps(results,indent=2)+'\n')
print('ALL_FIVE_STREAMING_AND_RESET_CHECKS_PASSED',flush=True)
