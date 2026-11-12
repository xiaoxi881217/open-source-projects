# 本地流式语音适配

原Zipformer和X-ASR保留。五个新增profile通过同一模型中心下载、选择；所有推理继续在独立`:asr`进程，普通输入不调用网络。

- `runtime-lock.json`固定Sherpa-ONNX Android SDK v2.0.1与每个包内文件摘要。Gradle的`prepareStreamingAsrRuntime`自动下载/验证；离线构建可先运行`python tools/asr/fetch_runtime.py --archive 官方发行AAR路径`。生成的app/libs/sherpa-onnx-2.0.1.aar不进入Git或源码ZIP。
- `model-source-lock.json`记录权重上游提交、下载地址、文件摘要；原始权重不放入APK、Git或源码ZIP。两个派生包在公开的keyboard-asr-models-20261112发布页。
- `export_dolphin_streaming.py`复用固定提交的官方Dolphin模型模块，导出仅encoder+CTC及注意力/卷积缓存；不是反复重跑整段录音。输入67帧特征，每次前进64帧，注意力最多保留64个输出帧。原FP32与ONNX在空缓存、增长和饱和7步一致。线性层动态INT8，卷积保留FP32。
- 导出环境：Python3.12、torch2.9.1 CPU、onnx1.20.0、onnxruntime1.23.2、pyyaml、torch-complex、librosa。上游Dolphin源码固定在脚本SOURCE_COMMIT；`--source`指向该checkout，`--weights`指向官方base.cn.streaming.pt/global_cmvn/train.yaml/units.txt目录，`--output`指定输出目录。先核对权重SHA256，只用weights_only加载。
- 多来源Zipformer CTC的派生量化配置与源文件SHA见verification-20261112.json的multiQuantization；使用onnxruntime.quantization.quantize_dynamic、QInt8、仅MatMul/Gemm。
- 桌面公开样例回归：`python tools/asr/verify_streaming_models.py --models-root 测试模型目录`。目录按源锁的模型id组织，另含dolphin-export；公开test_wavs须从相同上游提交取得。它不会启用麦克风，不读取私人录音。五个模型各验证结束前有partial、完整收尾、空状态重置后相同结果，详情见verification-20261112.json。

应用下载新模型时校验SHA256/文件长度，半文件不发布；归档先验证再暂存解包，拒绝越界/链接，完成后写摘要标记。两个归档模型也核对实际文件长度。原有已安装模型没有新标记要求，不强制重下或改默认选择。

授权见app/src/main/assets/open-source/STREAMING-ASR-NOTICE.txt。两款Paraformer上游ModelScope API于2026-11-12均标注Apache License2.0；转换仓库元信息缺失不再作为“已确认不明”，但完整传递许可证审计仍沿用原待办。本项目GPL-3.0，修改版非上游官方版本。

验证边界：桌面CPU样例与JVM/构建不等于手机速度、耗电、麦克风录音或方言/构音障碍准确率。尚未操作本人手机界面、录音或支付API费用，真实设备需本人手动验收。
