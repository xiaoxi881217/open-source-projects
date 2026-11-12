// Cached streaming Dolphin CTC; graph export and model provenance live in tools/asr.
#include <jni.h>
#include <onnxruntime_cxx_api.h>
#include "kaldi-native-fbank/csrc/online-feature.h"
#include <array>
#include <algorithm>
#include <fstream>
#include <memory>
#include <sstream>
#include <stdexcept>
#include <string>
#include <vector>

namespace {
struct Dolphin {
  Ort::Env env{ORT_LOGGING_LEVEL_ERROR, "xiaojia-dolphin"};
  Ort::Session session{nullptr};
  Ort::MemoryInfo memory = Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
  std::unique_ptr<knf::OnlineFbank> fbank;
  std::vector<float> att, cnn;
  std::vector<std::string> tokens;
  int64_t offset = 0;
  int32_t frame = 0;
  int previous = -1;
  bool finished = false;
  std::string text;

  Dolphin(const char *model, const char *vocabulary) {
    Ort::SessionOptions options;
    options.SetIntraOpNumThreads(2);
    options.SetInterOpNumThreads(1);
    options.SetGraphOptimizationLevel(GraphOptimizationLevel::ORT_ENABLE_ALL);
    session = Ort::Session(env, model, options);
    std::ifstream in(vocabulary);
    if (!in) throw std::runtime_error("missing vocabulary");
    std::string line, token;
    int id;
    while (std::getline(in, line)) {
      std::istringstream stream(line);
      if (!(stream >> token >> id) || id < 0 || id > 65535) throw std::runtime_error("invalid vocabulary");
      if (tokens.size() <= static_cast<size_t>(id)) tokens.resize(id + 1);
      tokens[id] = token;
    }
    if (tokens.size() != 18173) throw std::runtime_error("unexpected Dolphin vocabulary");
    Reset();
  }
  void Reset() {
    knf::FbankOptions options;
    options.frame_opts.samp_freq = 16000;
    options.frame_opts.dither = 0;
    options.frame_opts.snip_edges = true;
    options.mel_opts.num_bins = 80;
    options.mel_opts.high_freq = 0;
    fbank = std::make_unique<knf::OnlineFbank>(options);
    att.clear(); cnn.assign(6 * 1024 * 30, 0.f);
    offset = 0; frame = 0; previous = -1; finished = false; text.clear();
  }
  void DecodeReady() {
    while (fbank->NumFramesReady() - frame >= 67) {
      std::array<float, 67 * 80> features{};
      for (int i = 0; i < 67; ++i) std::copy_n(fbank->GetFrame(frame + i), 80, features.data() + i * 80);
      const int64_t history = static_cast<int64_t>(att.size() / (6 * 8 * 128));
      std::vector<float> mask(history + 16, 0.f);
      std::array<int64_t, 3> feature_shape{1, 67, 80}, mask_shape{1, 1, history + 16};
      std::array<int64_t, 4> att_shape{6, 8, history, 128}, cnn_shape{6, 1, 1024, 30};
      float empty = 0;
      std::array<Ort::Value, 5> values{
        Ort::Value::CreateTensor<float>(memory, features.data(), features.size(), feature_shape.data(), 3),
        Ort::Value::CreateTensor<int64_t>(memory, &offset, 1, nullptr, 0),
        Ort::Value::CreateTensor<float>(memory, att.empty() ? &empty : att.data(), att.size(), att_shape.data(), 4),
        Ort::Value::CreateTensor<float>(memory, cnn.data(), cnn.size(), cnn_shape.data(), 4),
        Ort::Value::CreateTensor<float>(memory, mask.data(), mask.size(), mask_shape.data(), 3)
      };
      const char *names[] = {"features", "offset", "attention_cache", "convolution_cache", "attention_mask"};
      const char *outputs[] = {"tokens", "new_attention_cache", "new_convolution_cache"};
      auto result = session.Run(Ort::RunOptions{nullptr}, names, values.data(), values.size(), outputs, 3);
      auto ids = result[0].GetTensorData<int64_t>();
      auto count = result[0].GetTensorTypeAndShapeInfo().GetElementCount();
      for (size_t i = 0; i < count; ++i) {
        const int id = static_cast<int>(ids[i]);
        if (id != 0 && id != previous && id >= 0 && static_cast<size_t>(id) < tokens.size()) {
          auto token = tokens[id];
          if (!token.empty() && token[0] != '<') {
            const std::string boundary = "\xe2\x96\x81";
            size_t at;
            while ((at = token.find(boundary)) != std::string::npos) token.replace(at, boundary.size(), " ");
            text += token;
          }
        }
        previous = id;
      }
      if (text.size() > 65536) throw std::runtime_error("utterance too long");
      auto a = result[1].GetTensorData<float>();
      auto c = result[2].GetTensorData<float>();
      att.assign(a, a + result[1].GetTensorTypeAndShapeInfo().GetElementCount());
      cnn.assign(c, c + result[2].GetTensorTypeAndShapeInfo().GetElementCount());
      if (att.size() > 6 * 8 * 64 * 128 || cnn.size() != 6 * 1024 * 30) throw std::runtime_error("invalid streaming cache");
      offset += static_cast<int64_t>(count); frame += 64; fbank->Pop(64);
    }
  }
  void Accept(const float *samples, int n) {
    if (finished) throw std::runtime_error("session already finished");
    std::vector<float> pcm(n);
    for (int i = 0; i < n; ++i) pcm[i] = samples[i] * 32768.f;
    fbank->AcceptWaveform(16000, pcm.data(), n);
    DecodeReady();
  }
  void Finish() {
    if (finished) return;
    std::array<float, 16000> silence{};
    Accept(silence.data(), silence.size());
    fbank->InputFinished(); DecodeReady(); finished = true;
  }
};
Dolphin *Get(jlong handle) {
  if (!handle) throw std::runtime_error("no recognizer");
  return reinterpret_cast<Dolphin *>(handle);
}
void Fail(JNIEnv *env) { env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "Dolphin本地识别失败，请重新下载模型或切回原模型"); }
}
extern "C" {
JNIEXPORT jlong JNICALL Java_com_kingzcheung_xime_speech_DolphinNative_create(JNIEnv *env, jobject, jstring file, jstring tokens) {
  const char *model = env->GetStringUTFChars(file, nullptr);
  const char *vocab = env->GetStringUTFChars(tokens, nullptr);
  Dolphin *result = nullptr;
  try { result = new Dolphin(model, vocab); } catch (const std::exception &) { Fail(env); }
  env->ReleaseStringUTFChars(file, model); env->ReleaseStringUTFChars(tokens, vocab);
  return reinterpret_cast<jlong>(result);
}
JNIEXPORT void JNICALL Java_com_kingzcheung_xime_speech_DolphinNative_reset(JNIEnv *env, jobject, jlong handle) {
  try { Get(handle)->Reset(); } catch (const std::exception &) { Fail(env); }
}
JNIEXPORT void JNICALL Java_com_kingzcheung_xime_speech_DolphinNative_accept(JNIEnv *env, jobject, jlong handle, jfloatArray input) {
  const auto n = env->GetArrayLength(input);
  std::vector<float> samples(n); env->GetFloatArrayRegion(input, 0, n, samples.data());
  try { Get(handle)->Accept(samples.data(), n); } catch (const std::exception &) { Fail(env); }
}
JNIEXPORT jbyteArray JNICALL Java_com_kingzcheung_xime_speech_DolphinNative_text(JNIEnv *env, jobject, jlong handle, jboolean finish) {
  try {
    auto *rec = Get(handle); if (finish) rec->Finish();
    auto bytes = env->NewByteArray(rec->text.size());
    env->SetByteArrayRegion(bytes, 0, rec->text.size(), reinterpret_cast<const jbyte *>(rec->text.data()));
    return bytes;
  } catch (const std::exception &) { Fail(env); return nullptr; }
}
JNIEXPORT void JNICALL Java_com_kingzcheung_xime_speech_DolphinNative_release(JNIEnv *, jobject, jlong handle) { delete reinterpret_cast<Dolphin *>(handle); }
}
