# Windows 源码构建

此版本使用独立 `legacy/build.py`，不需要此前的任何 AI 助手工程。需要 Python 3、JDK 21、Android SDK 的 aapt/zipalign/apksigner，以及下列外部工具。当前脚本按 Windows `.exe` 编写。

准备 `DIPLAY_TOOLCHAIN` 目录（不提交 Git）：

- `android17.jar`：Android API17 的官方 android.jar；另一个 `android.jar` 用于本地测试/资源编译。
- Kotlin 2.2.10 编译发行依赖 JAR：`kotlin-compiler.jar`、`kotlin-stdlib.jar`、`kotlin-reflect.jar`、`kotlin-script-runtime.jar`、`kotlin-daemon.jar`、`coroutines.jar`、`annotations.jar`（保持编译器 POM 依赖一致）。
- Bouncy Castle 1.79：`bcprov.jar`、`bctls.jar`、`bcutil.jar`；JmDNS 3.6.3：`jmdns.jar`。
- `junit.jar`（4.13.2）、`hamcrest.jar`（1.3）、`r8.jar`（9.5.23）。
- Zig 0.13.0，Windows 可执行文件位于 `zig/ziglang/zig.exe`。
- 官方 Android NDK 中 API17 ARM 的链接桩 libc，命名为 `libc-api17-arm.so`；不是从车机拷出的私有库。

使用官方 Android SDK、Kotlin/Maven Central、Bouncy Castle、Zig 发布渠道获取，不使用本车系统导出文件。本仓库不自动下载工具链；上述版本用于复现开发环境，不宣称其他版本已测试。

PowerShell 配置示例（路径改为自己的）：

```powershell
$env:JAVA_HOME='C:\Tools\jdk-21'
$env:DIPLAY_TOOLCHAIN='C:\Tools\diplay-toolchain'
$env:ANDROID_BUILD_TOOLS='C:\Android\Sdk\build-tools\30.0.3'
$env:ANDROID_AAPT_PLATFORM='C:\Android\Sdk\platforms\android-19\android.jar'
$env:DIPLAY_KEYSTORE='C:\Private\diplay.jks'
$env:DIPLAY_KEY_ALIAS='diplay'
# 将自己密钥的口令放到 DIPLAY_STORE_PASSWORD / DIPLAY_KEY_PASSWORD 环境变量。
# 不要提交口令、签名密钥、服务器令牌或附件身份材料。
python legacy/build.py
python legacy/test_protocols.py
```

APK 输出为 `legacy/dist/DiPlay_API17_Wired.apk`。构建过程包括 JNI、Kotlin/Java、R8、aapt、zipalign、签名及 API17 签名验证。`legacy/build/` 包含构建日志。

发布的 APK 已带上游实验性身份；源码默认不带，按 AUTH_PROVENANCE.md 准备两份固定摘要的运行资产即可复现功能。缺少材料的自行构建不能完成附件认证。发布自己的 APK 前应检查 ZIP 资产与配置，使用自己的签名并提供匹配源码；不得发布本地配对记录或私人身份材料。

公开精简版本测试：`python legacy/test_protocols.py`。`test_projection_ui.py` 需设置 `DIPLAY_TEST_WORKSPACE` 指向自备 API17 模拟器工具目录、`DIPLAY_TEST_SERIAL` 指定脚本允许的专用模拟器；测试脚本不用于真实车机。
