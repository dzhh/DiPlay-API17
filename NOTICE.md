# 来源与第三方声明

本移植基于 shihabal3amri/DiPlay，固定基准提交见 README。`legacy/src/com/shilapi/xcertplay` 中包含保留及修改的上游协议代码，`shared/src/test` 为选用的上游测试；`legacy/src/local/headunit/diplay` 为车机移植与适配层。此次为独立源码快照，不伪造上游 Git 历史。

项目及移植改动按 GPL-3.0 发布，完整许可证为根目录 LICENSE。修改记录见 CHANGELOG.md。保留文件内版权及许可证声明。

| 组件 | 用途 | 许可/来源 |
| --- | --- | --- |
| DiPlay | iAP2、USB、AirPlay 等协议基础及图标 | GPL-3.0；https://github.com/shihabal3amri/DiPlay |
| Monocypher 4.0.2 | 本地密码运算 | BSD-2-Clause OR CC0-1.0；源码内完整声明及 legacy/assets/LICENSE-Monocypher.txt |
| Bouncy Castle 1.79 | 旧系统密码/TLS 兼容 | MIT 风格许可；legacy/THIRD-PARTY-LICENSES.txt；https://www.bouncycastle.org/licence.html |
| Kotlin 2.2.10 | 编译器与运行库 | Apache-2.0；https://github.com/JetBrains/kotlin |
| JmDNS 3.6.3 | 服务发现 | Apache-2.0；https://github.com/jmdns/jmdns |
| JetBrains annotations | 编译辅助 | Apache-2.0；https://github.com/JetBrains/java-annotations |
| JUnit 4.13.2 / Hamcrest | 测试 | EPL-1.0 / BSD-3-Clause；各自发行包许可证 |

外部 JAR、JDK、Android SDK、Zig 工具链未随仓库分发，需从各自官方渠道取得并遵守各自许可。原车 framework、固件、应用反编译文件不在发布范围。后续若分发二进制，应同步提供对应源码及随包依赖所需许可声明。
