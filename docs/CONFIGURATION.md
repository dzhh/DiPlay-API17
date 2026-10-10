# 配置边界

公开仓库不附带可用的私人服务配置或附件认证身份。

- `legacy/assets/server.txt`、`token.txt`：可选的自建服务 URL 和访问令牌；缺失时在线更新/提交会提示失败，不会连接维护者私人服务器。客户端接口见 `OnlineUpdate.java` 和 `MainActivity.java`。后端不在本次发布范围。
- `legacy/assets/offline-mfi/`：认证适配器使用的身份配置目录，包含 `certificate.p7b` 与 `identity.pk8` 的读取逻辑；没有分发任何身份材料。调用方必须自行确保材料的使用及分发授权。自签名证书不能替代被手机信任的附件身份。
- APK 签名：由环境变量指定自己的 keystore，构建时从环境读取口令；不提交私钥。

这些路径已加入 `.gitignore`，但这不能代替发布前审计。APK 内的资产可被提取：不要把服务主密钥当作客户端令牌分发。自建服务应使用可撤销、最小权限的设备令牌，并在旧 Android 上实际测试 HTTPS 兼容性。

原车诊断、日志及配对数据可能含设备标识。公开 Issue 仅贴脱敏后的必要片段。仓库不包含原车固件、反编译 framework 或私人日志。
