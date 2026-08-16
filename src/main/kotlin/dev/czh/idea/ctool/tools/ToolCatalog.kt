package dev.czh.idea.ctool.tools

import dev.czh.idea.ctool.model.ToolDefinition

object ToolCatalog {
    val all: List<ToolDefinition> = buildList {
        add(tool("hash", "哈希", "加密", listOf("MD5", "SHA-1", "SHA-256", "SHA-512", "SM3"), "md5 sha1 sha256 sha512 sm3", file = true))
        add(tool("aes", "AES", "加密", listOf("加密", "解密")))
        add(tool("des", "DES", "加密", listOf("加密", "解密")))
        add(tool("tripleDes", "TripleDES", "加密", listOf("加密", "解密")))
        add(tool("rc4", "RC4", "加密", listOf("加密", "解密")))
        add(tool("rabbit", "Rabbit", "加密", listOf("加密", "解密")))
        add(tool("sm2", "SM2", "加密", listOf("加密", "解密", "签名", "验证", "生成密钥对")))
        add(tool("sm4", "SM4", "加密", listOf("加密", "解密")))
        add(tool("rsa", "RSA", "加密", listOf("加密", "解密")))
        add(tool("sign", "RSA 签名", "加密", listOf("签名 MD5withRSA", "验证 MD5withRSA", "签名 SHA1withRSA", "验证 SHA1withRSA", "签名 SHA256withRSA", "验证 SHA256withRSA", "签名 SHA512withRSA", "验证 SHA512withRSA", "生成密钥对"), "rsa signature"))
        add(tool("base64", "Base64", "编码解码", listOf("编码", "解码"), "base64"))
        add(tool("json", "JSON 工具", "编码解码", listOf("格式化", "校验", "压缩", "转义", "去除转义", "Unicode 转中文", "中文转 Unicode", "转 GET 参数", "JSONPath"), "json jsonpath"))
        add(tool("url", "URL 编码", "编码解码", listOf("编码", "解码"), "url uri"))
        add(tool("qrCode", "二维码", "生成", listOf("生成", "解析"), "qrcode qr", file = true))
        add(tool("barcode", "条形码", "生成", listOf("生成"), "barcode", file = true))
        add(tool("pinyin", "汉字转拼音", "转换", listOf("拼音", "首字母", "带声调"), "pinyin"))
        add(tool("ip", "IP 地址查询", "其他", listOf("查询"), "ip 地址", network = true))
        add(tool("code", "代码格式化", "转换", listOf("JSON", "JavaScript", "TypeScript", "HTML", "CSS", "Less", "SCSS", "XML", "YAML", "SQL", "GraphQL", "Markdown", "Vue", "Angular", "PHP", "JSON5", "压缩"), "format prettier js ts html css less scss xml yaml sql graphql markdown vue angular php json5"))
        add(tool("unicode", "Unicode", "编码解码", listOf("编码", "解码", "Emoji", "HTML 实体", "CSS 实体"), "unicode emoji"))
        add(tool("radix", "进制转换", "转换", listOf("2-64 进制"), "进制 binary hex radix"))
        add(tool("regex", "正则表达式", "校验", listOf("匹配", "查找", "替换"), "regex regexp"))
        add(tool("randomString", "随机字符", "生成", listOf("生成"), "random password"))
        add(tool("serialize", "序列化转换", "转换", listOf("JSON", "XML", "YAML", "CSV", "HTML Table", "PHP Array", "PHP Serialize", "Properties"), "serialize csv table php"))
        add(tool("diffs", "文本差异对比", "校验", listOf("按行", "按单词", "CSS"), "diff compare"))
        add(tool("crontab", "Crontab", "校验", listOf("校验", "规则例子"), "cron crontab"))
        add(tool("websocket", "WebSocket 调试", "其他", listOf("连接检查", "发送消息"), "websocket ws", network = true))
        add(tool("unit", "单位换算", "转换", listOf("长度", "面积", "体积", "质量", "温度", "压力", "功率", "功", "密度", "力", "时间", "速度", "数据存储", "角度"), "unit convert"))
        add(tool("time", "时间日期", "转换", listOf("时间戳", "时区", "时间计算器"), "timestamp timezone date"))
        add(tool("uuid", "UUID", "生成", listOf("生成"), "uuid guid"))
        add(tool("ascii", "ASCII 编码", "转换", listOf("字符串", "十进制", "十六进制", "八进制", "二进制"), "ascii"))
        add(tool("variableConversion", "变量名转换", "转换", listOf("camelCase", "PascalCase", "snake_case", "kebab-case", "SCREAMING_SNAKE", "空格分隔"), "variable case"))
        add(tool("jwt", "JWT 解码", "编码解码", listOf("Header", "Payload"), "jwt token"))
        add(tool("hexString", "Hex/String", "编码解码", listOf("字符串转 Hex", "Hex 转字符串"), "hex string"))
        add(tool("text", "文本处理", "其他", listOf("大小写转换", "中英文标点", "简繁转换", "替换", "字符统计", "行去重", "添加行号", "行排序", "过滤首尾空白", "过滤空行"), "text"))
        add(tool("html", "HTML 编码", "编码解码", listOf("编码", "解码"), "html entity"))
        add(tool("binary", "原码/反码/补码", "生成", listOf("生成"), "binary complement"))
        add(tool("arm", "ARM/HEX", "转换", listOf("ARM 转 HEX", "HEX 转 ARM"), "arm hex armconverter", file = true, network = true))
        add(tool("bcrypt", "Bcrypt", "加密", listOf("加密", "验证"), "bcrypt"))
        add(tool("ipcalc", "IP 网络计算器", "生成", listOf("IPv4", "IPv6"), "subnet cidr"))
        add(tool("sqlFillParameter", "SQL 参数填充", "生成", listOf("填充 MyBatis SQL"), "sql mybatis"))
        add(tool("httpSnippet", "HTTP Snippet", "转换", listOf("cURL 转请求", "生成 Java", "生成 Kotlin", "生成 JavaScript"), "curl http"))
        add(tool("dataValidation", "数据校验", "校验", listOf("BCC", "CRC16", "LRC"), "bcc crc lrc"))
        add(tool("color", "颜色工具", "转换", listOf("HEX", "RGB", "HSL"), "color rgb hsl"))
        add(tool("hmac", "HMAC", "加密", listOf("HMAC-MD5", "HMAC-SHA1", "HMAC-SHA256", "HMAC-SHA512", "HMAC-SM3", "HMAC-RIPEMD160"), "hmac"))
        add(tool("gzip", "GZIP", "编码解码", listOf("压缩", "解压"), "gzip"))
        add(tool("punycode", "Punycode", "编码解码", listOf("编码", "解码"), "punycode domain"))
        add(tool("urlParse", "URL 解析", "转换", listOf("解析"), "url parse"))
        add(tool("asn1", "ASN.1", "转换", listOf("解析"), "asn1 der"))
        add(tool("dockerCompose", "Docker Compose", "转换", listOf("生成 Docker Run", "Docker Run 转 Compose"), "docker compose"))
        add(tool("zhNumber", "中文数字", "转换", listOf("数字转小写", "数字转大写", "数字转金额", "小写转数字", "大写转数字"), "中文数字 金额 nzh"))
    }

    private fun tool(
        id: String,
        name: String,
        category: String,
        operations: List<String>,
        keywords: String = "",
        file: Boolean = false,
        network: Boolean = false,
    ): ToolDefinition = ToolDefinition(
        id = id,
        name = name,
        category = category,
        operations = operations,
        keywords = keywords.split(" ").filter(String::isNotBlank),
        supportsFile = file,
        network = network,
        handler = { request -> ToolImplementations.execute(id, request) },
    )

    fun find(id: String): ToolDefinition = all.first { it.id == id }
}
