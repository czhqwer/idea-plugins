# DevDock for IDEA

DevDock 是面向 IntelliJ IDEA 的原生开发工具箱，集中处理编码、转换、校验、生成和调试任务。

## 目标

- Kotlin/JVM + IntelliJ Platform API
- Windows/macOS 共用插件代码与发行包
- 工具代码与 UI 解耦，按工具注册表逐项迁移
- 支持读取编辑器选区、复制结果、替换编辑器选区
- 设置与常用工具列表持久化

## 构建

项目基线为 IntelliJ Platform 2024.1，构建需要 JDK 17。Windows 下可执行：

```powershell
$env:JAVA_HOME = "E:\develop\Jdk17"
.\gradlew.bat buildPlugin
```

macOS 使用对应的 `./gradlew test` 和 `./gradlew buildPlugin` 即可。生成的 ZIP 位于 `build/distributions`，在 IDEA 的 Settings/Preferences → Plugins → Install Plugin from Disk 中安装。插件发行包不区分 Windows/macOS；开发时下载的 IDEA 平台包只用于本机运行测试。

## 已实现工具

工具覆盖哈希、AES/DES/3DES/RC4/Rabbit/SM2/SM4/RSA、HMAC、Base64、URL、JSON、二维码/条形码、拼音、代码格式化、Unicode、进制、正则、随机字符、序列化、Diff、Cron、WebSocket、单位、时间、UUID、ASCII、变量名、JWT、Hex、文本、HTML、原码/反码/补码、ARM/HEX、Bcrypt、IP 网络、SQL 参数、颜色、GZIP、Punycode、URL 解析、ASN.1、Docker Compose 和中文数字。

## 参考来源

工具分类和功能命名参考 [baiy/Ctool](https://github.com/baiy/Ctool)。该项目使用 MIT License；若分发改写后的实现，需要保留原项目的版权与许可证声明。
