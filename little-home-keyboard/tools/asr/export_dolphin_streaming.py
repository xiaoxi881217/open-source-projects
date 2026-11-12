"""导出官方 Dolphin Base 中文方言流式权重；保留缓存，不重跑历史录音。"""
import argparse, hashlib, json, subprocess, sys, types
from pathlib import Path

SOURCE_COMMIT = '78ea615194777f91a3970ff010b5d678d0bbf5f0'
WEIGHTS_SHA256 = '62e4c11fe1e0e42bd34e444172c5a05e792c4b5a03750f794fa3206fc0649cd7'

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--weights', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=args.source,text=True).strip() == SOURCE_COMMIT
    assert hashlib.sha256((args.weights/'base.cn.streaming.pt').read_bytes()).hexdigest() == WEIGHTS_SHA256
    import torch, yaml, numpy as np, onnx, onnxruntime as ort
    from onnxruntime.quantization import quantize_dynamic, QuantType
    torch.set_num_threads(2)
    # 只加载模型模块，避免上游 __init__ 引入在线模型下载/VAD。
    package = types.ModuleType('dolphin'); package.__path__ = [str(args.source/'dolphin')]
    sys.modules['dolphin'] = package
    from dolphin.model import init_speech_model
    config = yaml.safe_load((args.weights/'train.yaml').read_text())
    config['cmvn_conf']['cmvn_file'] = str(args.weights/'global_cmvn')
    model = init_speech_model(config).eval()
    model.load_state_dict(torch.load(args.weights/'base.cn.streaming.pt',map_location='cpu',weights_only=True))

    class Chunk(torch.nn.Module):
        def __init__(self):
            super().__init__(); self.encoder=model.encoder; self.ctc=model.ctc
        def forward(self, features, offset, attention_cache, convolution_cache, attention_mask):
            encoded, att, cnn = self.encoder.forward_chunk(features, offset, -1, attention_cache, convolution_cache, attention_mask)
            # 负数切片在 ONNX 中保留动态长度；首块缓存为空，之后最多64帧。
            return self.ctc.log_softmax(encoded).argmax(-1), att[:,:,-64:,:], cnn
    chunk=Chunk().eval(); args.output.mkdir(parents=True,exist_ok=True)
    features=torch.randn(1,67,80)
    warm=(features,torch.tensor(64,dtype=torch.int64),torch.zeros(6,8,64,128),torch.zeros(6,1,1024,30),torch.zeros(1,1,80))
    fp32=args.output/'model.onnx'
    with torch.no_grad():
        torch.onnx.export(chunk,warm,str(fp32),input_names=['features','offset','attention_cache','convolution_cache','attention_mask'],output_names=['tokens','new_attention_cache','new_convolution_cache'],dynamic_axes={'attention_cache':{2:'history'},'attention_mask':{2:'history_plus_chunk'},'new_attention_cache':{2:'new_history'}},opset_version=17,dynamo=False)
    graph=onnx.load(fp32)
    onnx.helper.set_model_props(graph,{'model_type':'xiaojia_dolphin_streaming_ctc','source_commit':SOURCE_COMMIT,'chunk_size':'16','feature_window':'67','feature_stride':'64','cache_frames':'64','sample_rate':'16000'})
    onnx.save(graph,fp32)
    session=ort.InferenceSession(str(fp32),providers=['CPUExecutionProvider'])
    att=np.zeros((6,8,0,128),np.float32);cnn=np.zeros((6,1,1024,30),np.float32);offset=0;checks=[]
    # 验证首块空缓存、增长、饱和及第二段重置；直接对照官方缓存推理。
    with torch.no_grad():
        for i in range(7):
            x=torch.randn(1,67,80)*5+10
            mask=np.zeros((1,1,att.shape[2]+16),np.float32)
            expected=chunk(x,torch.tensor(offset),torch.from_numpy(att),torch.from_numpy(cnn),torch.from_numpy(mask))
            actual=session.run(None,dict(features=x.numpy(),offset=np.array(offset,np.int64),attention_cache=att,convolution_cache=cnn,attention_mask=mask))
            np.testing.assert_array_equal(actual[0],expected[0].numpy())
            np.testing.assert_allclose(actual[1],expected[1].numpy(),rtol=1e-3,atol=2e-3)
            np.testing.assert_allclose(actual[2],expected[2].numpy(),rtol=1e-3,atol=2e-3)
            att,cnn=actual[1:];offset+=16;checks.append(int(att.shape[2]))
    quantize_dynamic(str(fp32),str(args.output/'model.int8.onnx'),weight_type=QuantType.QInt8,op_types_to_quantize=['MatMul','Gemm'])
    # 词表原样保留，特殊标记仅由渲染端隐藏。
    (args.output/'tokens.txt').write_bytes((args.weights/'units.txt').read_bytes())
    proof=dict(sourceCommit=SOURCE_COMMIT,weightsSha256=WEIGHTS_SHA256,fp32ChunkParityChecks=checks,quantization='dynamic int8 linear layers; convolutions fp32',torch=torch.__version__,onnx=onnx.__version__,onnxruntime=ort.__version__,phoneAcceptance=False)
    (args.output/'export-proof.json').write_text(json.dumps(proof,indent=2)+'\n')
    print(json.dumps(proof),flush=True)

if __name__=='__main__':main()
