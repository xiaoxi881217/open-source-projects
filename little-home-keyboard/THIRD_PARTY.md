# 来源与许可核对

修改日期2026-10-28。安卓基底Xime：https://github.com/ximeiorg/Xime ，GPL-3.0；修改版使用小家键盘独立名称/包名，保留原作者版权。

Rime及原生/词库源码见SOURCE_DEPENDENCIES.json锁定提交；各仓库保留自身许可证，不把引擎许可自动视为全部词库许可。

四个按键WAV来自Xime提交d2da96a3e0475b39a84d33323851abeae62fd9d7的app/src/main/res/raw，逐字节一致，保留Xime项目GPL许可和来源。

ChaiPUA-0.2.7-snow.ttf：原assets/readme.md注明https://github.com/tansongchen/input；已与该仓库public同名字体逐字节核对，SHA256 d27872811c926f62e305e6b57c08e30dfa7b47c2676e21afda3cc0bd1d0e3d97；来源仓库LICENSE为GPLv3。字体name表仅有通用Copyright fonts，未另行推断OFL授权。

AndroidX/Kotlin/Compose、ONNX、OpenCC等仍受各自许可约束，原NOTICE随源码/APK保留。本文件记录已核对来源，不声称完成全部传递依赖的法律审计。

没有捆绑本人导入的壁纸、贴纸、字体或剪贴板；自行导入/分享资源应拥有相应权利。


## 73版新增本地流式ASR

小家键盘新增本地流式ASR（73版）

原Zipformer和X-ASR及其选择记录保留。新增两个Paraformer流式模型、两个Zipformer CTC流式模型与Dolphin Base中文方言流式CTC适配。

Sherpa-ONNX运行库：k2-fsa/sherpa-onnx v2.0.1，Apache-2.0。模型与推理代码分别授权，不能用推理框架许可证代替模型许可证。
源码及组件构建来源：https://github.com/k2-fsa/sherpa-onnx/tree/v2.0.1
固定发行包、内容摘要及所保留的文件：应用源码tools/asr/runtime-lock.json。
完整运行库许可证：sherpa-onnx-LICENSE.txt。预编译库的传递组件来源依上游该版本构建脚本及许可证，本项目保留GPL-3.0许可与原有NOTICE；不宣称已完成全部传递依赖的法律审计。

Dolphin Base Streaming：Dataocean AI与清华大学，官方权重Apache-2.0；来源DataoceanAI/dolphin-cn-dialect-base-streaming。移除了不用于实时CTC识别的解码器，导出带缓存ONNX并对线性权重动态INT8量化。修改和来源固定信息在模型包conversion.json与tools/asr/export_dolphin_streaming.py；此为修改适配版，非官方APK。
Zipformer Multi-ZH CTC派生量化包：来自csukuangfj/sherpa-onnx-streaming-zipformer-ctc-multi-zh-hans-2023-12-13，Apache-2.0，保留流式缓存，线性层动态INT8量化。来源与转换摘要随模型包提供。
Paraformer中英/三语模型：上游ModelScope模型信息在2026-11-12核对标注Apache License 2.0；应用直接链接上游固定提交，不把权重混入APK。完整来源、SHA256与文件映射见tools/asr/model-source-lock.json。

模型按需另行下载，不包含个人录音、剪贴板或服务器设置。支持方言不代表对听障或构音障碍语音准确；手机速度、耗电和个体识别效果尚待验收。识别在本机:asr进程运行。
