# 小家键盘 · 安卓源码

基于Xime/Rime的独立中文输入法，修改版按GPL-3.0分发，保留原作者版权与第三方许可。不是Xime官方版本。此目录对应20261064源码，开发中的功能和真实设备兼容性仍需验收。

## 简单使用

在系统设置中启用并切换小家键盘。装扮页点击预览选键，保存并应用。剪贴板里可写草稿、选取一小段放进输入框。连接自己的HTTPS服务器，领取绑定码；了解隐私风险后自行开启自动同步。日常使用流量请关闭“只在Wi-Fi下发送”。不上传逐个按键，过滤不能保证识别所有敏感资料。

输入英文半角 `@` 后可在候选栏选择邮箱后缀；App首页「邮箱后缀」可编辑1到12个域名、排序或关闭。此功能仅本地补全，不保存正在输入的邮箱，不在密码框或有未完成拼音、选区时显示；不提供光标上下文的输入框可能无法显示。

## 构建

需要JDK21、Android SDK/NDK/CMake（版本见Gradle文件）、Python3、Git、Node，以及构建插件所需的Rust/Cargo。下载依赖需要网络。

1. 在本目录运行 `python fetch-sources.py`，取得SOURCE_DEPENDENCIES.json固定到实际使用提交的7个源码仓库；递归子模块由上游提交锁定。
2. 配置本机SDK路径local.properties，安装Gradle声明的NDK/CMake。
3. 按scripts/build-plugins.sh构建并同步内置插件；详见tools/xime-plugin/README.md。
4. Windows运行 `gradlew.bat assembleDebug -Pandroid.injected.build.abi=arm64-v8a -Pandroid.injected.testOnly=false`；其他系统运行 `bash gradlew assembleDebug` 并选择所需ABI。

Gradle/CMake会下载指定版本的Boost、ONNX、knf等依赖，版本/来源/校验保留在构建脚本中。使用自己的签名密钥；不会公开原发行私钥。

本目录来自通过121项定向JUnit、本机HTTPS契约及arm64构建的开发工作树；导出后校验文件哈希/隐私与源码锁，尚未在第二台空白机器全量重建。普通构建无需原作者的小家服务器；本机TLS测试另需隔离夹具。

## 服务器与许可

配套服务器在同仓库 `../little-home-keyboard-server`，该目录不是安卓源码。本目录不含私人服务器配置、令牌、数据库、剪贴板、草稿或个人预设。版权及资源来源见THIRD_PARTY.md，原声明见app/src/main/assets/NOTICE.txt；各锁定上游源码仍保留各自许可证。GPL全文见LICENSE。

SOURCE_MANIFEST.json的SHA256按Git保存的文件内容计算；Windows检出时Git可能转换换行，核对时请使用Git归档内容。
