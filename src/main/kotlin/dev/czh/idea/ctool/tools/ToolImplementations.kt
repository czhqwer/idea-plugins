package dev.czh.idea.ctool.tools

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import dev.czh.idea.ctool.model.ToolRequest
import dev.czh.idea.ctool.model.ToolResult
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.util.ASN1Dump
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.crypto.engines.SM2Engine
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.params.ParametersWithID
import org.bouncycastle.crypto.params.ParametersWithRandom
import org.bouncycastle.crypto.signers.SM2Signer
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.bouncycastle.jce.interfaces.ECPrivateKey
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.mindrot.jbcrypt.BCrypt
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
import com.cronutils.model.time.ExecutionTime
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.awt.Color
import java.awt.image.BufferedImage
import java.math.BigInteger
import java.net.IDN
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Security
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.security.spec.ECGenParameterSpec
import java.io.StringReader
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.net.http.WebSocket
import kotlin.math.pow
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.imageio.ImageIO

object ToolImplementations {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val compactGson = GsonBuilder().disableHtmlEscaping().create()
    private val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    init {
        if (Security.getProvider("BC") == null) Security.addProvider(BouncyCastleProvider())
    }

    fun execute(id: String, request: ToolRequest): ToolResult = try {
        when (id) {
            "hash" -> hash(request)
            "base64" -> base64(request)
            "url" -> url(request)
            "unicode" -> unicode(request)
            "html" -> html(request)
            "json" -> json(request)
            "code" -> code(request)
            "hexString" -> hexString(request)
            "ascii" -> ascii(request)
            "uuid" -> uuid(request)
            "randomString" -> randomString(request)
            "variableConversion" -> variableConversion(request)
            "jwt" -> jwt(request)
            "regex" -> regex(request)
            "diffs" -> diffs(request)
            "time" -> time(request)
            "pinyin" -> pinyin(request)
            "radix" -> radix(request)
            "unit" -> unit(request)
            "text" -> text(request)
            "serialize" -> serialize(request)
            "crontab" -> crontab(request)
            "bcrypt" -> bcrypt(request)
            "hmac" -> hmac(request)
            "sign" -> sign(request)
            "gzip" -> gzip(request)
            "punycode" -> punycode(request)
            "urlParse" -> urlParse(request)
            "binary" -> binary(request)
            "ipcalc" -> ipcalc(request)
            "sqlFillParameter" -> sqlFillParameter(request)
            "dataValidation" -> dataValidation(request)
            "color" -> color(request)
            "httpSnippet" -> httpSnippet(request)
            "dockerCompose" -> dockerCompose(request)
            "zhNumber" -> zhNumber(request)
            "arm" -> arm(request)
            "qrCode" -> qrCode(request)
            "barcode" -> barcode(request)
            "encrypt", "aes", "des", "tripleDes", "rc4", "rabbit", "sm2", "sm4", "rsa" -> encrypt(id, request)
            "ip" -> ip(request)
            "websocket" -> websocket(request)
            "asn1" -> asn1(request)
            else -> ToolResult("暂未注册工具：$id", isError = true)
        }
    } catch (e: Exception) {
        ToolResult(e.message ?: e.javaClass.simpleName, isError = true)
    }

    private fun sourceBytes(request: ToolRequest): ByteArray = request.selectedFile?.let(Files::readAllBytes)
        ?: request.input.toByteArray(StandardCharsets.UTF_8)

    private fun hash(request: ToolRequest): ToolResult {
        val algorithm = request.operation.replace("-", "").replace(" ", "").uppercase(Locale.ROOT)
        val digestName = if (algorithm == "SM3") "SM3" else algorithm
        val digest = MessageDigest.getInstance(digestName)
        return ToolResult(digest.digest(sourceBytes(request)).toHex())
    }

    private fun base64(request: ToolRequest): ToolResult = when (request.operation) {
        "编码" -> ToolResult(Base64.getEncoder().encodeToString(sourceBytes(request)))
        else -> ToolResult(Base64.getDecoder().decode(request.input.trim()).toString(StandardCharsets.UTF_8))
    }

    private fun url(request: ToolRequest): ToolResult = when (request.operation) {
        "编码" -> ToolResult(URLEncoder.encode(request.input, StandardCharsets.UTF_8).replace("+", "%20"))
        else -> ToolResult(URLDecoder.decode(request.input, StandardCharsets.UTF_8))
    }

    private fun unicode(request: ToolRequest): ToolResult = when {
        request.operation == "编码" -> ToolResult(request.input.codePoints().toArray().asIterable().joinToString(separator = "") { cp ->
            if (cp <= 0xffff) "\\u%04x".format(cp) else "\\u{%x}".format(cp)
        })
        request.operation == "Emoji" -> ToolResult(request.input.codePoints().toArray().asIterable().joinToString(" ") { "U+%04X".format(it) })
        request.operation == "HTML 实体" -> ToolResult(encodeHtml(request.input))
        request.operation == "CSS 实体" -> ToolResult(request.input.codePoints().toArray().asIterable().joinToString("") { "\\%x ".format(it) }.trim())
        else -> ToolResult(decodeUnicode(request.input))
    }

    private fun html(request: ToolRequest): ToolResult = when (request.operation) {
        "编码" -> ToolResult(encodeHtml(request.input))
        else -> ToolResult(request.input.replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&"))
    }

    private fun json(request: ToolRequest): ToolResult {
        val root = JsonParser.parseString(request.input)
        return when {
            request.operation == "校验" -> ToolResult("JSON 有效")
            request.operation == "压缩" -> ToolResult(compactGson.toJson(root))
            request.operation == "转义" -> ToolResult(gson.toJson(request.input))
            request.operation == "去除转义" -> ToolResult(gson.fromJson(request.input, String::class.java))
            request.operation == "Unicode 转中文" -> ToolResult(decodeUnicode(request.input))
            request.operation == "中文转 Unicode" -> ToolResult(encodeUnicode(request.input))
            request.operation == "转 GET 参数" -> ToolResult(jsonToQuery(root))
            request.operation == "JSONPath" -> ToolResult(jsonPath(root, request.secondaryInput.ifBlank { "$." }))
            else -> ToolResult(gson.toJson(root))
        }
    }

    private fun code(request: ToolRequest): ToolResult = when (request.operation) {
        "JSON", "JSON5" -> json(request.copy(operation = "格式化"))
        "压缩" -> ToolResult(request.input.replace(Regex("\\s+"), " ").replace(Regex("\\s*([{},;])\\s*"), "$1"))
        "XML" -> ToolResult(prettyXml(request.input))
        "YAML" -> ToolResult(yamlDump(request.input))
        "SQL" -> ToolResult(prettySql(request.input))
        else -> ToolResult(prettyCode(request.input))
    }

    private fun hexString(request: ToolRequest): ToolResult = when (request.operation) {
        "字符串转 Hex" -> ToolResult(request.input.toByteArray(StandardCharsets.UTF_8).toHex())
        else -> ToolResult(hexToBytes(request.input).toString(StandardCharsets.UTF_8))
    }

    private fun ascii(request: ToolRequest): ToolResult = if (request.operation == "字符串") {
        ToolResult(request.input.toByteArray(StandardCharsets.US_ASCII).joinToString(" "))
    } else {
        val radix = when (request.operation) { "十六进制" -> 16; "八进制" -> 8; "二进制" -> 2; else -> 10 }
        ToolResult(request.input.split(Regex("[,\\s]+"), limit = 0).filter(String::isNotBlank)
            .map { it.toInt(radix).toChar() }.joinToString(""))
    }

    private fun uuid(request: ToolRequest): ToolResult {
        val count = request.input.trim().toIntOrNull()?.coerceIn(1, 1000) ?: 1
        return ToolResult((1..count).joinToString("\n") { UUID.randomUUID().toString() })
    }

    private fun randomString(request: ToolRequest): ToolResult {
        val parts = request.secondaryInput.ifBlank { request.input }.split(Regex("[,;\\s]+"))
        val length = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(1, 4096) ?: 32
        val count = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 1000) ?: 1
        val special = parts.getOrNull(2)?.toBooleanStrictOrNull() ?: true
        val alphabet = buildString {
            append("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
            if (special) append("!@#$%^&*()-_=+[]{}:,.?/")
        }
        return ToolResult((1..count).joinToString("\n") {
            buildString { repeat(length) { append(alphabet[ThreadLocalRandom.current().nextInt(alphabet.length)]) } }
        })
    }

    private fun variableConversion(request: ToolRequest): ToolResult {
        val words = request.input.trim().replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
            .split(Regex("[\\s_\\-]+"), limit = 0).filter(String::isNotBlank).map(String::lowercase)
        return ToolResult(when (request.operation) {
            "camelCase" -> words.firstOrNull().orEmpty() + words.drop(1).joinToString("") { it.replaceFirstChar(Char::uppercase) }
            "PascalCase" -> words.joinToString("") { it.replaceFirstChar(Char::uppercase) }
            "snake_case" -> words.joinToString("_")
            "kebab-case" -> words.joinToString("-")
            "SCREAMING_SNAKE" -> words.joinToString("_").uppercase()
            else -> words.joinToString(" ")
        })
    }

    private fun jwt(request: ToolRequest): ToolResult {
        val pieces = request.input.trim().split(".")
        if (pieces.size < 2) return ToolResult("不是有效的 JWT", isError = true)
        val header = decodeUrlBase64(pieces[0])
        val payload = decodeUrlBase64(pieces[1])
        return ToolResult(if (request.operation == "Header") prettyJsonOrText(header) else prettyJsonOrText(payload))
    }

    private fun regex(request: ToolRequest): ToolResult {
        val parts = request.secondaryInput.split("\n", limit = 2)
        val pattern = parts.firstOrNull().orEmpty()
        val replacement = parts.getOrNull(1).orEmpty()
        val regex = Pattern.compile(pattern)
        return when (request.operation) {
            "匹配" -> ToolResult(regex.matcher(request.input).find().toString())
            "查找" -> ToolResult(buildList {
                val matcher = regex.matcher(request.input)
                while (matcher.find()) add(matcher.group())
            }.joinToString("\n"))
            else -> ToolResult(regex.matcher(request.input).replaceAll(replacement))
        }
    }

    private fun diffs(request: ToolRequest): ToolResult {
        if (request.operation == "按单词") {
            val left = diffTokens(request.input)
            val right = diffTokens(request.secondaryInput)
            return ToolResult(buildDiff(left, right))
        }
        val left = if (request.operation == "CSS") normalizeCss(request.input).split("\n") else request.input.split("\n")
        val right = if (request.operation == "CSS") normalizeCss(request.secondaryInput).split("\n") else request.secondaryInput.split("\n")
        return ToolResult(buildDiff(left, right))
    }

    private fun diffTokens(text: String): List<String> =
        text.split(Regex("(?=[{}()\\[\\]<>:;,=+*/-])|(?<=[{}()\\[\\]<>:;,=+*/-])|\\s+"))
            .filter(String::isNotBlank)

    private fun normalizeCss(text: String): String = text
        .replace(Regex("\\s+"), " ")
        .replace("{", "{\n")
        .replace("}", "\n}\n")
        .replace(";", ";\n")
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .joinToString("\n")

    private fun buildDiff(left: List<String>, right: List<String>): String {
        val max = maxOf(left.size, right.size)
        return (0 until max).joinToString("\n") { index ->
            val a = left.getOrNull(index)
            val b = right.getOrNull(index)
            when {
                a == b -> "  ${a.orEmpty()}"
                a == null -> "+ ${b.orEmpty()}"
                b == null -> "- $a"
                else -> "- $a\n+ $b"
            }
        }
    }

    private fun time(request: ToolRequest): ToolResult {
        val value = request.input.trim()
        if (request.operation == "时间计算器") {
            val numbers = Regex("[-+]?\\d+(?:\\.\\d+)?").findAll(value).map { it.value.toDouble() }.toList()
            return ToolResult(formatNumber(numbers.sum()))
        }
        val numeric = value.matches(Regex("\\d{10,13}"))
        val instant = if (numeric) {
            val number = value.toLong()
            Instant.ofEpochMilli(if (value.length == 10) number * 1000 else number)
        } else {
            LocalDateTime.parse(value, inputFormatter).atZone(ZoneId.systemDefault()).toInstant()
        }
        return when (request.operation) {
            "时区" -> {
                val zones = request.secondaryInput.split(Regex("[,;\\s]+"), limit = 0).filter(String::isNotBlank)
                val sourceZone = zones.getOrNull(0)?.let(ZoneId::of) ?: ZoneId.systemDefault()
                val targetZone = zones.getOrNull(1)?.let(ZoneId::of) ?: ZoneId.of("UTC")
                val sourceInstant = if (numeric) instant else LocalDateTime.parse(value, inputFormatter).atZone(sourceZone).toInstant()
                ToolResult("${sourceZone.id}: ${LocalDateTime.ofInstant(sourceInstant, sourceZone).format(inputFormatter)}\n${targetZone.id}: ${LocalDateTime.ofInstant(sourceInstant, targetZone).format(inputFormatter)}")
            }
            else -> if (numeric) {
                ToolResult("本地时间: ${LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(inputFormatter)}\nUTC: $instant")
            } else {
                ToolResult("秒: ${instant.epochSecond}\n毫秒: ${instant.toEpochMilli()}")
            }
        }
    }

    private fun pinyin(request: ToolRequest): ToolResult {
        val format = net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat().apply {
            toneType = if (request.operation == "带声调") {
                net.sourceforge.pinyin4j.format.HanyuPinyinToneType.WITH_TONE_NUMBER
            } else {
                net.sourceforge.pinyin4j.format.HanyuPinyinToneType.WITHOUT_TONE
            }
            caseType = net.sourceforge.pinyin4j.format.HanyuPinyinCaseType.LOWERCASE
        }
        val syllables = request.input.map { char ->
            if (char.code in 0x4e00..0x9fff) {
                net.sourceforge.pinyin4j.PinyinHelper.toHanyuPinyinStringArray(char, format)?.firstOrNull() ?: char.toString()
            } else char.toString()
        }
        return if (request.operation == "首字母") {
            ToolResult(syllables.joinToString("") { it.firstOrNull()?.toString().orEmpty() })
        } else {
            ToolResult(syllables.joinToString(" "))
        }
    }

    private fun radix(request: ToolRequest): ToolResult {
        val parts = request.secondaryInput.split(Regex("[,;\\s]+"), limit = 0).filter(String::isNotBlank)
        val from = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(2, 64) ?: 10
        val to = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(2, 64) ?: 16
        val value = BigInteger(request.input.trim(), from)
        return ToolResult(value.toString(to))
    }

    private fun unit(request: ToolRequest): ToolResult {
        val inputParts = request.input.trim().split(Regex("\\s+"), limit = 0).filter(String::isNotBlank)
        val optionParts = request.secondaryInput.trim().split(Regex("[,;\\s]+"), limit = 0).filter(String::isNotBlank)
        val value = inputParts.firstOrNull()?.toDoubleOrNull()
            ?: return ToolResult("格式：数值 源单位 目标单位，或在附加输入填写源单位和目标单位", isError = true)
        val from = (inputParts.getOrNull(1) ?: optionParts.getOrNull(0)).orEmpty().lowercase()
        val to = (inputParts.getOrNull(2) ?: optionParts.getOrNull(1)).orEmpty().lowercase()
        if (from.isBlank() || to.isBlank()) return ToolResult("请提供源单位和目标单位", isError = true)
        if (request.operation == "温度") {
            val result = when (from to to) {
                "c" to "f" -> value * 9 / 5 + 32
                "f" to "c" -> (value - 32) * 5 / 9
                "c" to "k" -> value + 273.15
                "k" to "c" -> value - 273.15
                "f" to "k" -> (value - 32) * 5 / 9 + 273.15
                "k" to "f" -> (value - 273.15) * 9 / 5 + 32
                else -> if (from == to) value else return ToolResult("支持 C/F/K 温度单位", isError = true)
            }
            return ToolResult(formatNumber(result))
        }
        val factors = unitFactors[request.operation]
            ?: return ToolResult("暂不支持该换算类型", isError = true)
        val fromFactor = factors[normalizeUnit(from)]
        val toFactor = factors[normalizeUnit(to)]
        if (fromFactor == null || toFactor == null) {
            return ToolResult("不支持 $from -> $to；可用单位：${factors.keys.sorted().joinToString(", ")}", isError = true)
        }
        return ToolResult(formatNumber(value * fromFactor / toFactor))
    }

    private fun text(request: ToolRequest): ToolResult = when (request.operation) {
        "大小写转换" -> ToolResult(request.input.lowercase())
        "中英文标点" -> ToolResult(request.input.map { punctuationMap[it] ?: it }.joinToString(""))
        "替换" -> ToolResult(request.input.replace(request.secondaryInput.substringBefore("\n"), request.secondaryInput.substringAfter("\n", "")))
        "字符统计" -> ToolResult("字符数: ${request.input.length}\n代码点数: ${request.input.codePointCount(0, request.input.length)}\n行数: ${request.input.lines().size}")
        "行去重" -> ToolResult(request.input.lines().distinct().joinToString("\n"))
        "添加行号" -> ToolResult(request.input.lines().mapIndexed { index, line -> "${index + 1}. $line" }.joinToString("\n"))
        "行排序" -> ToolResult(request.input.lines().sorted().joinToString("\n"))
        "过滤首尾空白" -> ToolResult(request.input.lines().joinToString("\n") { it.trim() })
        "过滤空行" -> ToolResult(request.input.lines().filter(String::isNotBlank).joinToString("\n"))
        "简繁转换" -> ToolResult(request.input.map { simplifiedTraditional[it] ?: it }.joinToString(""))
        else -> ToolResult(request.input.uppercase())
    }

    private fun serialize(request: ToolRequest): ToolResult = when (request.operation) {
        "YAML" -> ToolResult(yamlDump(request.input))
        "CSV" -> ToolResult(jsonToCsv(JsonParser.parseString(request.input)))
        "HTML Table" -> ToolResult(jsonToTable(JsonParser.parseString(request.input)))
        "Properties" -> ToolResult(jsonRows(JsonParser.parseString(request.input)).firstOrNull().orEmpty().entries.joinToString("\n") { (key, value) -> "$key=${if (value.isJsonPrimitive) value.asString else gson.toJson(value)}" })
        "PHP Array" -> ToolResult(jsonToPhpArray(JsonParser.parseString(request.input)))
        "PHP Serialize" -> ToolResult(jsonToPhpSerialize(JsonParser.parseString(request.input)))
        "XML" -> ToolResult(prettyXml(request.input))
        else -> ToolResult(gson.toJson(JsonParser.parseString(request.input)))
    }

    private fun crontab(request: ToolRequest): ToolResult {
        if (request.operation == "规则例子") {
            return ToolResult("* * * * *  每分钟\n*/5 * * * *  每 5 分钟\n0 9 * * 1-5  工作日 09:00\n0 0 1 * *  每月 1 日")
        }
        val fields = request.input.trim().split(Regex("\\s+"))
        if (fields.size !in 5..7) return ToolResult("Cron 通常需要 5 到 7 个字段", isError = true)
        val definition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
        val cron = CronParser(definition).parse(request.input.trim())
        cron.validate()
        val execution = ExecutionTime.forCron(cron)
        var cursor = java.time.ZonedDateTime.now()
        val next = buildList {
            repeat(5) {
                val value = execution.nextExecution(cursor).orElse(null) ?: return@repeat
                add(value.toString())
                cursor = value
            }
        }
        return ToolResult("Cron 有效\n未来执行时间：\n${next.joinToString("\n")}")
    }

    private fun bcrypt(request: ToolRequest): ToolResult = if (request.operation == "验证") {
        ToolResult(BCrypt.checkpw(request.input, request.secondaryInput).toString())
    } else ToolResult(BCrypt.hashpw(request.input, BCrypt.gensalt(12)))

    private fun hmac(request: ToolRequest): ToolResult {
        val algorithm = when (request.operation) {
            "HMAC-MD5" -> "HmacMD5"
            "HMAC-SHA1" -> "HmacSHA1"
            "HMAC-SHA512" -> "HmacSHA512"
            "HMAC-SM3" -> "HmacSM3"
            "HMAC-RIPEMD160" -> "HmacRIPEMD160"
            else -> "HmacSHA256"
        }
        val provider = if (algorithm in setOf("HmacSM3", "HmacRIPEMD160")) "BC" else null
        val mac = if (provider == null) Mac.getInstance(algorithm) else Mac.getInstance(algorithm, provider)
        mac.init(SecretKeySpec(request.secondaryInput.toByteArray(StandardCharsets.UTF_8), algorithm))
        return ToolResult(mac.doFinal(request.input.toByteArray(StandardCharsets.UTF_8)).toHex())
    }

    private fun sign(request: ToolRequest): ToolResult {
        if (request.operation == "生成密钥对") {
            val generator = java.security.KeyPairGenerator.getInstance("RSA")
            generator.initialize(2048, SecureRandom())
            val pair = generator.generateKeyPair()
            return ToolResult("公钥：\n${pem("PUBLIC KEY", pair.public.encoded)}\n\n私钥：\n${pem("PRIVATE KEY", pair.private.encoded)}")
        }
        val algorithm = request.operation.substringAfter(' ').ifBlank { "SHA256withRSA" }
        val keyText = extractKeyMaterial(request.secondaryInput)
        if (keyText.isBlank()) return ToolResult("请在附加输入填写 RSA 私钥/公钥；验签时第二行填写签名 Base64", isError = true)
        val signature = Signature.getInstance(algorithm)
        val data = request.input.toByteArray(StandardCharsets.UTF_8)
        return if (request.operation.startsWith("签名")) {
            signature.initSign(readRsaPrivateKey(keyText))
            signature.update(data)
            ToolResult(Base64.getEncoder().encodeToString(signature.sign()))
        } else {
            val verifyCode = extractTrailingKeyData(request.secondaryInput)
            if (verifyCode.isBlank()) return ToolResult("验签时请在附加输入第二行填写签名 Base64", isError = true)
            signature.initVerify(readRsaPublicKey(keyText))
            signature.update(data)
            ToolResult(signature.verify(Base64.getDecoder().decode(verifyCode.trim())).toString())
        }
    }

    private fun pem(type: String, encoded: ByteArray): String = buildString {
        appendLine("-----BEGIN $type-----")
        Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(encoded).lineSequence().forEach(::appendLine)
        append("-----END $type-----")
    }

    private fun gzip(request: ToolRequest): ToolResult {
        return if (request.operation == "压缩") {
            val output = java.io.ByteArrayOutputStream()
            java.util.zip.GZIPOutputStream(output).use { it.write(request.input.toByteArray(StandardCharsets.UTF_8)) }
            ToolResult(Base64.getEncoder().encodeToString(output.toByteArray()))
        } else {
            val bytes = Base64.getDecoder().decode(request.input.trim())
            ToolResult(java.util.zip.GZIPInputStream(bytes.inputStream()).readBytes().toString(StandardCharsets.UTF_8))
        }
    }

    private fun punycode(request: ToolRequest): ToolResult = if (request.operation == "编码") {
        ToolResult(request.input.split(".").joinToString(".") { IDN.toASCII(it) })
    } else ToolResult(request.input.split(".").joinToString(".") { IDN.toUnicode(it) })

    private fun urlParse(request: ToolRequest): ToolResult {
        val uri = URI(request.input.trim())
        return ToolResult(listOf("协议: ${uri.scheme}", "主机: ${uri.host}", "端口: ${uri.port}", "路径: ${uri.path}", "查询: ${uri.query}", "片段: ${uri.fragment}").joinToString("\n"))
    }

    private fun binary(request: ToolRequest): ToolResult {
        val value = request.input.trim().toBigIntegerOrNull()
            ?: return ToolResult("请输入整数", isError = true)
        val bits = (request.secondaryInput.trim().toIntOrNull() ?: 32).let { candidate ->
            listOf(8, 16, 32, 64, 128, 256).firstOrNull { it == candidate } ?: 32
        }
        val max = BigInteger.TWO.pow(bits - 1).subtract(BigInteger.ONE)
        val min = BigInteger.TWO.pow(bits - 1).negate()
        if (value < min || value > max) return ToolResult("${bits} 位整数范围：$min ~ $max", isError = true)
        val magnitude = value.abs().toString(2).padStart(bits - 1, '0')
        val trueForm = (if (value.signum() < 0) "1" else "0") + magnitude
        val inverse = if (value.signum() < 0) "1" + trueForm.drop(1).map { if (it == '0') '1' else '0' }.joinToString("") else trueForm
        val complement = if (value.signum() < 0) BigInteger(inverse, 2).add(BigInteger.ONE).toString(2).padStart(bits, '0') else trueForm
        return ToolResult("原码: $trueForm\n反码: $inverse\n补码: $complement")
    }

    private fun ipcalc(request: ToolRequest): ToolResult {
        val parts = request.input.trim().split("/")
        val ip = parts.firstOrNull() ?: return ToolResult("格式：192.168.1.10/24", isError = true)
        val prefix = parts.getOrNull(1)?.toIntOrNull() ?: return ToolResult("请输入 CIDR 前缀", isError = true)
        val value = ipToLong(ip)
        val mask = if (prefix == 0) 0 else (-1L shl (32 - prefix)) and 0xffffffffL
        val network = value and mask
        val broadcast = network or (mask.inv() and 0xffffffffL)
        return ToolResult("网络地址: ${longToIp(network)}\n广播地址: ${longToIp(broadcast)}\n子网掩码: ${longToIp(mask)}\n可用主机数: ${if (prefix >= 31) 0 else (1L shl (32 - prefix)) - 2}")
    }

    private fun sqlFillParameter(request: ToolRequest): ToolResult {
        val params = request.secondaryInput.split(Regex("\\r?\\n|,|;"), limit = 0).filter(String::isNotBlank).toMutableList()
        var index = 0
        return ToolResult(Regex("\\?").replace(request.input) {
            val value = params.getOrNull(index++)?.trim().orEmpty()
            if (value.matches(Regex("-?\\d+(?:\\.\\d+)?")) || value.equals("null", true)) value else "'${value.replace("'", "''")}'"
        })
    }

    private fun dataValidation(request: ToolRequest): ToolResult {
        val bytes = hexToBytes(request.input)
        return when (request.operation) {
            "BCC" -> ToolResult(byteArrayOf(bytes.fold(0) { acc, byte -> acc xor (byte.toInt() and 0xff) }.toByte()).toHex())
            "LRC" -> ToolResult(byteArrayOf((-bytes.sumOf { it.toInt() and 0xff }).toByte()).toHex())
            else -> ToolResult(crc16(bytes).toHex())
        }
    }

    private fun color(request: ToolRequest): ToolResult {
        val input = request.input.trim()
        val color = if (input.startsWith("#")) Color.decode(input) else {
            val values = Regex("\\d+").findAll(input).map { it.value.toInt() }.toList()
            Color(values.getOrElse(0) { 0 }, values.getOrElse(1) { 0 }, values.getOrElse(2) { 0 })
        }
        val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
        return ToolResult("HEX: #%02X%02X%02X\nRGB: ${color.red}, ${color.green}, ${color.blue}\nHSV: %.2f, %.2f, %.2f".format(color.red, color.green, color.blue, hsb[0] * 360, hsb[1] * 100, hsb[2] * 100))
    }

    private fun httpSnippet(request: ToolRequest): ToolResult {
        val url = Regex("https?://[^\\s'\"]+").find(request.input)?.value ?: return ToolResult("未找到 URL", isError = true)
        return when {
            request.operation == "生成 Kotlin" -> "val request = HttpRequest.newBuilder(URI.create(\"$url\")).GET().build()"
            request.operation == "生成 Java" -> "HttpRequest request = HttpRequest.newBuilder(URI.create(\"$url\")).GET().build();"
            request.operation == "生成 JavaScript" -> "fetch(\"$url\")"
            else -> "GET $url"
        }.let(::ToolResult)
    }

    private fun dockerCompose(request: ToolRequest): ToolResult {
        val command = request.input.trim().removePrefix("docker run ").trim()
        val image = command.split(Regex("\\s+"), limit = 0).lastOrNull().orEmpty()
        return ToolResult("services:\n  app:\n    image: $image\n    restart: unless-stopped")
    }

    private fun zhNumber(request: ToolRequest): ToolResult {
        return try {
            when (request.operation) {
                "数字转小写" -> ToolResult(numberToChinese(request.input, upper = false, money = false))
                "数字转大写" -> ToolResult(numberToChinese(request.input, upper = true, money = false))
                "数字转金额" -> ToolResult(numberToChinese(request.input, upper = true, money = true))
                "小写转数字", "大写转数字" -> ToolResult(chineseToNumber(request.input))
                else -> ToolResult(numberToChinese(request.input, upper = false, money = false))
            }
        } catch (e: Exception) {
            ToolResult("中文数字转换失败：${e.message}", isError = true)
        }
    }

    private fun arm(request: ToolRequest): ToolResult = if (request.operation == "ARM 转 HEX") {
        armConvert(request.input, "asm", listOf("arm64", "arm", "thumb"))
    } else {
        armConvert(request.input, "hex", listOf("arm64", "arm", "armbe", "thumb", "thumbbe"))
    }

    private fun armConvert(input: String, type: String, architectures: List<String>): ToolResult {
        val payload = com.google.gson.JsonObject().apply {
            addProperty(type, input)
            add("arch", com.google.gson.JsonArray().also { architectures.forEach(it::add) })
        }
        val request = HttpRequest.newBuilder(URI.create("https://armconverter.com/api/convert"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
            .build()
        val response = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(8)).build()
            .send(request, HttpResponse.BodyHandlers.ofString())
        val root = JsonParser.parseString(response.body()).asJsonObject
        val outputType = if (type == "asm") "hex" else "asm"
        val output = architectures.mapNotNull { architecture ->
            root.getAsJsonObject(outputType)?.getAsJsonArray(architecture)?.let { values ->
                if (values.size() > 1 && !values[0].asBoolean) "$architecture: ${values[1].asString}" else null
            }
        }
        return if (output.isEmpty()) ToolResult(response.body(), isError = response.statusCode() !in 200..299) else ToolResult(output.joinToString("\n"))
    }

    private fun qrCode(request: ToolRequest): ToolResult {
        if (request.operation == "解析") {
            val file = request.selectedFile ?: return ToolResult("请选择二维码图片", isError = true)
            return try {
                val image = ImageIO.read(file.toFile())
                val bitmap = BinaryBitmap(HybridBinarizer(BufferedImageLuminanceSource(image)))
                ToolResult(MultiFormatReader().decode(bitmap).text)
            } catch (_: NotFoundException) { ToolResult("图片中没有识别到二维码", isError = true) }
        }
        val matrix = QRCodeWriter().encode(request.input, BarcodeFormat.QR_CODE, 360, 360)
        return ToolResult("二维码已生成", MatrixToImageWriter.toBufferedImage(matrix))
    }

    private fun barcode(request: ToolRequest): ToolResult {
        val matrix = QRCodeWriter().encode(request.input, BarcodeFormat.CODE_128, 640, 180)
        return ToolResult("条形码已生成", MatrixToImageWriter.toBufferedImage(matrix))
    }

    private fun encrypt(id: String, request: ToolRequest): ToolResult {
        if (id == "sm2") return sm2(request)
        if (id == "rsa") return rsa(request)
        val algorithm = when (id) { "tripleDes" -> "DESede"; "rc4" -> "RC4"; "rabbit" -> "RABBIT"; "sm4" -> "SM4"; "des" -> "DES"; else -> "AES" }
        val keyText = request.secondaryInput.lineSequence().firstOrNull { it.isNotBlank() } ?: "devdock-default-key"
        val key = normalizedKey(keyText, algorithm)
        val provider = if (algorithm == "SM4" || algorithm == "RABBIT") "BC" else null
        val transformation = if (algorithm in setOf("RC4", "RABBIT")) algorithm else "$algorithm/ECB/PKCS5Padding"
        val cipher = if (provider == null) Cipher.getInstance(transformation) else Cipher.getInstance(transformation, provider)
        cipher.init(if (request.operation == "加密") Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE, SecretKeySpec(key, algorithm))
        return if (request.operation == "加密") ToolResult(Base64.getEncoder().encodeToString(cipher.doFinal(request.input.toByteArray(StandardCharsets.UTF_8))))
        else ToolResult(cipher.doFinal(Base64.getDecoder().decode(request.input.trim())).toString(StandardCharsets.UTF_8))
    }

    private fun sm2(request: ToolRequest): ToolResult {
        if (request.operation == "生成密钥对") {
            val generator = java.security.KeyPairGenerator.getInstance("EC", "BC")
            generator.initialize(ECGenParameterSpec("sm2p256v1"), SecureRandom())
            val pair = generator.generateKeyPair()
            val privateKey = pair.private as ECPrivateKey
            val publicKey = pair.public as ECPublicKey
            return ToolResult("公钥（Hex）:\n${publicKey.q.getEncoded(false).toHex()}\n私钥（Hex）:\n${privateKey.d.toString(16).padStart(64, '0')}")
        }
        val key = request.secondaryInput.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        if (key.isBlank()) return ToolResult("请在附加输入填写 SM2 公钥或私钥 Hex", isError = true)
        val domainSpec = ECNamedCurveTable.getParameterSpec("sm2p256v1")
        val domain = ECDomainParameters(domainSpec.curve, domainSpec.g, domainSpec.n, domainSpec.h, domainSpec.seed)
        return when (request.operation) {
            "加密" -> {
                val public = sm2PublicKey(key, domain)
                val engine = SM2Engine(SM2Engine.Mode.C1C3C2)
                engine.init(true, ParametersWithRandom(public, SecureRandom()))
                ToolResult(engine.processBlock(request.input.toByteArray(StandardCharsets.UTF_8), 0, request.input.toByteArray(StandardCharsets.UTF_8).size).toHex())
            }
            "解密" -> {
                val private = sm2PrivateKey(key, domain)
                val engine = SM2Engine(SM2Engine.Mode.C1C3C2)
                engine.init(false, private)
                ToolResult(engine.processBlock(hexToBytes(request.input), 0, hexToBytes(request.input).size).toString(StandardCharsets.UTF_8))
            }
            "签名" -> {
                val signer = SM2Signer()
                signer.init(true, ParametersWithID(ParametersWithRandom(sm2PrivateKey(key, domain), SecureRandom()), "1234567812345678".toByteArray()))
                val bytes = request.input.toByteArray(StandardCharsets.UTF_8)
                signer.update(bytes, 0, bytes.size)
                ToolResult(signer.generateSignature().toHex())
            }
            "验证" -> {
                val signature = request.secondaryInput.lineSequence().drop(1).firstOrNull { it.isNotBlank() }
                    ?: return ToolResult("附加输入第二行需要签名 Hex", isError = true)
                val verifier = SM2Signer()
                verifier.init(false, ParametersWithID(sm2PublicKey(key, domain), "1234567812345678".toByteArray()))
                val bytes = request.input.toByteArray(StandardCharsets.UTF_8)
                verifier.update(bytes, 0, bytes.size)
                ToolResult(verifier.verifySignature(hexToBytes(signature)).toString())
            }
            else -> ToolResult("SM2 不支持操作：${request.operation}", isError = true)
        }
    }

    private fun sm2PublicKey(value: String, domain: ECDomainParameters): ECPublicKeyParameters {
        val normalized = value.replace(Regex("-----.*?-----"), "").replace(Regex("\\s+"), "")
        val encoded = hexToBytes(if (normalized.length == 128) "04$normalized" else normalized)
        return ECPublicKeyParameters(domain.curve.decodePoint(encoded), domain)
    }

    private fun sm2PrivateKey(value: String, domain: ECDomainParameters): ECPrivateKeyParameters =
        ECPrivateKeyParameters(BigInteger(value.replace(Regex("[^0-9a-fA-F]"), ""), 16), domain)

    private fun rsa(request: ToolRequest): ToolResult {
        val keyText = extractKeyMaterial(request.secondaryInput)
        if (keyText.isBlank()) return ToolResult("请在附加输入填写 RSA 公钥或私钥 PEM/Base64", isError = true)
        val oaep = request.secondaryInput.contains("OAEP", ignoreCase = true)
        val transformation = if (oaep) "RSA/ECB/OAEPWithSHA-256AndMGF1Padding" else "RSA/ECB/PKCS1Padding"
        val encrypting = request.operation == "加密"
        val key = if (encrypting) readRsaPublicKey(keyText) else readRsaPrivateKey(keyText)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(if (encrypting) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE, key)
        return if (encrypting) {
            ToolResult(Base64.getEncoder().encodeToString(cipher.doFinal(request.input.toByteArray(StandardCharsets.UTF_8))))
        } else {
            ToolResult(cipher.doFinal(Base64.getDecoder().decode(request.input.trim())).toString(StandardCharsets.UTF_8))
        }
    }

    private fun readRsaPublicKey(value: String): PublicKey {
        val parsed = readPemKey(value)
        if (parsed is PublicKey) return parsed
        val bytes = Base64.getDecoder().decode(value.replace(Regex("\\s+"), ""))
        return KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(bytes))
    }

    private fun readRsaPrivateKey(value: String): PrivateKey {
        val parsed = readPemKey(value)
        if (parsed is PrivateKey) return parsed
        val bytes = Base64.getDecoder().decode(value.replace(Regex("\\s+"), ""))
        return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(bytes))
    }

    private fun readPemKey(value: String): Any? {
        if (!value.contains("BEGIN")) return null
        PEMParser(StringReader(value)).use { parser ->
            val objectValue = parser.readObject() ?: return null
            val converter = JcaPEMKeyConverter().setProvider("BC")
            return when (objectValue) {
                is PEMKeyPair -> converter.getKeyPair(objectValue).let { if (value.contains("PRIVATE")) it.private else it.public }
                is SubjectPublicKeyInfo -> converter.getPublicKey(objectValue)
                is PrivateKeyInfo -> converter.getPrivateKey(objectValue)
                else -> null
            }
        }
    }

    private fun extractKeyMaterial(value: String): String {
        val begin = value.indexOf("-----BEGIN")
        if (begin < 0) return value.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        val endStart = value.indexOf("-----END", begin)
        if (endStart < 0) return value.substring(begin).trim()
        val end = value.indexOf("-----", endStart + "-----END".length)
        return value.substring(begin, if (end < 0) value.length else end + 5).trim()
    }

    private fun extractTrailingKeyData(value: String): String {
        val begin = value.indexOf("-----BEGIN")
        if (begin < 0) return value.lineSequence().drop(1).firstOrNull { it.isNotBlank() }.orEmpty()
        val endStart = value.indexOf("-----END", begin)
        if (endStart < 0) return ""
        val end = value.indexOf("-----", endStart + "-----END".length)
        return value.substring(if (end < 0) value.length else end + 5).lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
    }

    private fun ip(request: ToolRequest): ToolResult {
        val url = "https://ipwho.is/${request.input.trim()}"
        val response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString())
        return ToolResult(prettyJsonOrText(response.body()))
    }

    private fun websocket(request: ToolRequest): ToolResult {
        val uri = URI(request.input.trim())
        if (uri.scheme !in setOf("ws", "wss")) return ToolResult("请输入 ws:// 或 wss:// 地址", isError = true)
        val received = StringBuilder()
        val listener = object : WebSocket.Listener {
            override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): java.util.concurrent.CompletionStage<*> {
                received.append(data)
                return java.util.concurrent.CompletableFuture.completedFuture(null)
            }
        }
        val socket = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build()
            .newWebSocketBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).buildAsync(uri, listener).get(8, TimeUnit.SECONDS)
        return try {
            if (request.operation == "发送消息") {
                if (request.secondaryInput.isBlank()) return ToolResult("请在附加输入填写要发送的消息", isError = true)
                socket.sendText(request.secondaryInput, true).get(5, TimeUnit.SECONDS)
                ToolResult("连接成功，消息已发送${if (received.isNotEmpty()) "\n收到：$received" else ""}")
            } else {
                ToolResult("WebSocket 连接成功：$uri")
            }
        } finally {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "devdock").exceptionally { null }
        }
    }

    private fun asn1(request: ToolRequest): ToolResult {
        val bytes = hexToBytes(request.input)
        val dump = ASN1InputStream(bytes.inputStream()).use { stream -> ASN1Dump.dumpAsString(stream.readObject()) }
        return ToolResult("长度: ${bytes.size} bytes\n$dump")
    }

    private fun jsonToQuery(element: JsonElement, prefix: String = ""): String = when {
        element.isJsonObject -> element.asJsonObject.entrySet().flatMap { (key, value) -> jsonToQuery(value, if (prefix.isBlank()) key else "$prefix[$key]").split("&").filter(String::isNotBlank) }.joinToString("&")
        element.isJsonArray -> element.asJsonArray.mapIndexed { index, value -> jsonToQuery(value, "$prefix[$index]") }.joinToString("&")
        else -> "${encodeQuery(prefix)}=${encodeQuery(if (element.isJsonNull) "" else element.asString)}"
    }

    private fun encodeUnicode(input: String): String = input.codePoints().toArray().asIterable().joinToString("") { codePoint ->
        if (codePoint <= 0xffff) "\\u%04x".format(codePoint) else "\\u{%x}".format(codePoint)
    }

    private fun jsonRows(root: JsonElement): List<Map<String, JsonElement>> = when {
        root.isJsonArray -> root.asJsonArray.map { item ->
            if (item.isJsonObject) item.asJsonObject.entrySet().associate { it.key to it.value } else mapOf("value" to item)
        }
        root.isJsonObject -> listOf(root.asJsonObject.entrySet().associate { it.key to it.value })
        else -> listOf(mapOf("value" to root))
    }

    private fun csvCell(element: JsonElement?): String {
        val value = when {
            element == null || element.isJsonNull -> ""
            element.isJsonPrimitive -> element.asString
            else -> gson.toJson(element)
        }
        return if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${value.replace("\"", "\"\"")}\"" else value
    }

    private fun jsonToCsv(root: JsonElement): String {
        val rows = jsonRows(root)
        val columns = rows.flatMap { it.keys }.distinct()
        return buildString {
            appendLine(columns.joinToString(",") { csvCell(JsonPrimitive(it)) })
            rows.forEach { row -> appendLine(columns.joinToString(",") { csvCell(row[it]) }) }
        }.trimEnd()
    }

    private fun jsonToTable(root: JsonElement): String {
        val rows = jsonRows(root)
        val columns = rows.flatMap { it.keys }.distinct()
        return buildString {
            append('|').append(columns.joinToString("|") { " $it " }).appendLine('|')
            append('|').append(columns.joinToString("|") { " --- " }).appendLine('|')
            rows.forEach { row -> append('|').append(columns.joinToString("|") { " ${csvCell(row[it]).replace("|", "\\|")} " }).appendLine('|') }
        }.trimEnd()
    }

    private fun jsonToPhpArray(element: JsonElement): String = when {
        element.isJsonObject -> element.asJsonObject.entrySet().joinToString(",\n", "[\n", "\n]") { (key, value) -> "    ${gson.toJson(key)} => ${jsonToPhpArray(value)}" }
        element.isJsonArray -> element.asJsonArray.joinToString(", ", "[", "]", transform = ::jsonToPhpArray)
        element.isJsonNull -> "null"
        element.asJsonPrimitive.isBoolean -> element.asBoolean.toString()
        element.asJsonPrimitive.isNumber -> element.asString
        else -> gson.toJson(element.asString)
    }

    private fun jsonToPhpSerialize(element: JsonElement): String = when {
        element.isJsonObject -> {
            val entries = element.asJsonObject.entrySet().joinToString("") { (key, value) ->
                "s:${key.toByteArray(StandardCharsets.UTF_8).size}:\"${key.replace("\"", "\\\"")}\";${jsonToPhpSerialize(value)}"
            }
            "a:${element.asJsonObject.size()}:{${entries}}"
        }
        element.isJsonArray -> "a:${element.asJsonArray.size()}:{${element.asJsonArray.mapIndexed { index, value -> "i:${index};${jsonToPhpSerialize(value)}" }.joinToString("")}}"
        element.isJsonNull -> "N;"
        element.asJsonPrimitive.isBoolean -> "b:${if (element.asBoolean) 1 else 0};"
        element.asJsonPrimitive.isNumber -> "d:${element.asString};"
        else -> "s:${element.asString.toByteArray(StandardCharsets.UTF_8).size}:\"${element.asString.replace("\"", "\\\"")}\";"
    }

    private fun jsonPath(root: JsonElement, path: String): String {
        val clean = path.removePrefix("$").removePrefix(".")
        if (clean.isBlank()) return gson.toJson(root)
        var current = root
        clean.split(".").filter(String::isNotBlank).forEach { part ->
            current = if (part.toIntOrNull() != null && current.isJsonArray) current.asJsonArray[part.toInt()]
            else if (current.isJsonObject) current.asJsonObject[part] ?: JsonPrimitive("")
            else JsonPrimitive("")
        }
        return gson.toJson(current)
    }

    private fun generateObjectCode(root: JsonElement, operation: String): String {
        val className = "Root"
        val fields = if (root.isJsonObject) root.asJsonObject.entrySet().joinToString("\n") { (key, value) -> "    ${safeName(key)}: ${jsonType(value)}" } else "    value: Any?"
        return when {
            operation.contains("Java") -> "data class $className(\n${fields.replace(": ", ": ")}\n)"
            operation.contains("Go") -> "type $className struct {\n${fields.lines().joinToString("\n") { "    ${it.trim()} `json:\"${it.trim().substringBefore(":")}\"`" }}\n}"
            operation.contains("Dart") -> "class $className {\n$fields\n}"
            operation.contains("C#") -> "public class $className {\n${fields.replace(": ", " ")}\n}"
            else -> fields
        }
    }

    private fun prettyJsonOrText(text: String): String = try { gson.toJson(JsonParser.parseString(text)) } catch (_: Exception) { text }

    private fun yamlDump(input: String): String {
        val options = DumperOptions().apply { defaultFlowStyle = DumperOptions.FlowStyle.BLOCK; isPrettyFlow = true }
        return Yaml(options).dump(JsonParser.parseString(input).let { gson.fromJson(it, Any::class.java) })
    }

    private fun prettyXml(input: String): String = input.replace(Regex("><"), ">\n<").lines().mapIndexed { index, line -> "    ".repeat((0 until index).count { false }) + line }.joinToString("\n")

    private fun prettySql(input: String): String = input.replace(Regex("\\s+"), " ").replace(Regex("(?i)\\b(FROM|WHERE|GROUP BY|ORDER BY|HAVING|LIMIT|VALUES|SET|JOIN|LEFT JOIN|RIGHT JOIN)\\b"), "\n$1").trim()

    private fun prettyCode(input: String): String = input.replace(Regex("\\s*([{};])\\s*"), "$1\n").lines().joinToString("\n")

    private fun encodeHtml(input: String): String = input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")

    private fun decodeUnicode(input: String): String = Regex("\\\\u(?:\\{([0-9a-fA-F]+)}|([0-9a-fA-F]{4}))").replace(input) { match ->
        (match.groupValues[1].ifBlank { match.groupValues[2] }).toInt(16).toChar().toString()
    }

    private fun decodeUrlBase64(value: String): String = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)

    private fun encodeQuery(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun safeName(value: String): String = value.replace(Regex("[^A-Za-z0-9_]"), "_").let { if (it.firstOrNull()?.isDigit() == true) "_${it}" else it }

    private fun jsonType(value: JsonElement): String = when {
        value.isJsonObject -> "Map<String, Any?>"
        value.isJsonArray -> "List<Any?>"
        value.isJsonNull -> "Any?"
        value.asJsonPrimitive.isBoolean -> "Boolean"
        value.asJsonPrimitive.isNumber -> "Double"
        else -> "String"
    }

    private fun normalizedKey(value: String, algorithm: String): ByteArray {
        val size = when (algorithm) { "DES" -> 8; "DESede" -> 24; "SM4" -> 16; else -> 16 }
        return MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).copyOf(size)
    }

    private fun hexToBytes(value: String): ByteArray {
        val normalized = value.replace(Regex("0x|[^0-9a-fA-F]"), "")
        return normalized.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun ipToLong(ip: String): Long = ip.split(".").fold(0L) { acc, item -> (acc shl 8) or item.toLong() }

    private fun longToIp(value: Long): String = (3 downTo 0).joinToString(".") { ((value ushr (it * 8)) and 255).toString() }

    private fun crc16(bytes: ByteArray): ByteArray {
        var crc = 0xffff
        bytes.forEach { byte ->
            crc = crc xor (byte.toInt() and 0xff)
            repeat(8) { crc = if ((crc and 1) != 0) (crc ushr 1) xor 0xa001 else crc ushr 1 }
        }
        return byteArrayOf((crc and 0xff).toByte(), ((crc ushr 8) and 0xff).toByte())
    }

    private val unitFactors: Map<String, Map<String, Double>> = mapOf(
        "长度" to mapOf("mm" to 0.001, "cm" to 0.01, "dm" to 0.1, "m" to 1.0, "km" to 1000.0, "um" to 1e-6, "nm" to 1e-9, "in" to 0.0254, "ft" to 0.3048, "yd" to 0.9144, "mi" to 1609.344, "nmi" to 1852.0),
        "面积" to mapOf("mm2" to 1e-6, "cm2" to 1e-4, "m2" to 1.0, "km2" to 1e6, "ha" to 1e4, "acre" to 4046.8564224, "in2" to 0.00064516, "ft2" to 0.09290304, "yd2" to 0.83612736, "mi2" to 2589988.110336),
        "体积" to mapOf("ml" to 1e-6, "l" to 0.001, "m3" to 1.0, "cm3" to 1e-6, "mm3" to 1e-9, "gal" to 0.003785411784, "qt" to 0.000946352946, "pt" to 0.000473176473, "cup" to 0.0002365882365, "oz" to 2.95735295625e-5, "in3" to 1.6387064e-5, "ft3" to 0.028316846592),
        "质量" to mapOf("mg" to 1e-6, "g" to 0.001, "kg" to 1.0, "t" to 1000.0, "oz" to 0.028349523125, "lb" to 0.45359237, "stone" to 6.35029318),
        "压力" to mapOf("pa" to 1.0, "kpa" to 1000.0, "mpa" to 1e6, "bar" to 1e5, "mbar" to 100.0, "atm" to 101325.0, "mmhg" to 133.322387415, "psi" to 6894.757293168, "psf" to 47.88025898),
        "功率" to mapOf("w" to 1.0, "kw" to 1000.0, "mw" to 1e6, "hp" to 745.699872, "ps" to 735.49875),
        "功" to mapOf("j" to 1.0, "kj" to 1000.0, "cal" to 4.184, "kcal" to 4184.0, "wh" to 3600.0, "kwh" to 3.6e6, "btu" to 1055.05585262, "ftlb" to 1.3558179483),
        "密度" to mapOf("kgm3" to 1.0, "gcm3" to 1000.0, "gml" to 1000.0, "kgcm3" to 1e6, "gdm3" to 1.0),
        "力" to mapOf("n" to 1.0, "kn" to 1000.0, "dyn" to 1e-5, "kgf" to 9.80665, "gf" to 0.00980665, "lbf" to 4.4482216152605, "kip" to 4448.2216152605),
        "时间" to mapOf("ns" to 1e-9, "us" to 1e-6, "ms" to 1e-3, "s" to 1.0, "min" to 60.0, "h" to 3600.0, "d" to 86400.0, "week" to 604800.0, "yr" to 31536000.0),
        "速度" to mapOf("ms" to 1.0, "kms" to 1000.0, "kmh" to 1.0 / 3.6, "mph" to 0.44704, "knot" to 0.514444444, "mach" to 340.3, "c" to 299792458.0),
        "数据存储" to mapOf("bit" to 0.125, "b" to 1.0, "kb" to 1024.0, "mb" to 1024.0.pow(2), "gb" to 1024.0.pow(3), "tb" to 1024.0.pow(4), "pb" to 1024.0.pow(5), "eb" to 1024.0.pow(6)),
        "角度" to mapOf("degree" to 1.0, "deg" to 1.0, "rad" to 180.0 / Math.PI, "gon" to 0.9, "circle" to 360.0, "arcmin" to 1.0 / 60, "arcsec" to 1.0 / 3600),
    )

    private fun normalizeUnit(value: String): String = value.lowercase(Locale.ROOT)
        .replace("²", "2")
        .replace("³", "3")
        .replace("/", "")
        .replace("·", "")
        .replace("μ", "u")

    private fun formatNumber(value: Double): String = if (value.isFinite()) {
        "%.12f".format(Locale.ROOT, value).trimEnd('0').trimEnd('.')
    } else value.toString()

    private fun numberToChinese(raw: String, upper: Boolean, money: Boolean): String {
        val value = java.math.BigDecimal(raw.replace(",", "")).toPlainString()
        val negative = value.startsWith('-')
        val unsigned = value.removePrefix("-")
        val parts = unsigned.split('.', limit = 2)
        val integer = parts[0].trimStart('0').ifBlank { "0" }
        val decimal = parts.getOrNull(1).orEmpty()
        val digits = if (upper) "零壹贰叁肆伍陆柒捌玖" else "零一二三四五六七八九"
        val result = buildString {
            if (negative) append(if (upper) "负" else "负")
            append(integerToChinese(integer, digits))
            if (money) {
                append('元')
                val jiao = decimal.getOrNull(0)?.digitToIntOrNull() ?: 0
                val fen = decimal.getOrNull(1)?.digitToIntOrNull() ?: 0
                if (jiao == 0 && fen == 0) append("整")
                else {
                    if (jiao > 0) append(digits[jiao]).append('角')
                    if (fen > 0) append(digits[fen]).append('分')
                }
            } else if (decimal.isNotBlank()) {
                append('点')
                decimal.forEach { char -> append(digits[char.digitToInt()]) }
            }
        }
        return result
    }

    private fun integerToChinese(value: String, digits: String): String {
        if (value == "0") return digits[0].toString()
        val smallUnits = arrayOf("", if (digits[1] == '壹') "拾" else "十", if (digits[1] == '壹') "佰" else "百", if (digits[1] == '壹') "仟" else "千")
        val largeUnits = arrayOf("", if (digits[1] == '壹') "萬" else "万", if (digits[1] == '壹') "億" else "亿", if (digits[1] == '壹') "兆" else "兆")
        fun section(number: String): String {
            val padded = number.padStart(4, '0')
            val result = StringBuilder()
            var zero = false
            for (index in padded.indices) {
                val digit = padded[index] - '0'
                val position = 3 - index
                if (digit == 0) {
                    if (result.isNotEmpty()) zero = true
                } else {
                    if (zero) result.append(digits[0])
                    result.append(digits[digit]).append(smallUnits[position])
                    zero = false
                }
            }
            return result.toString()
        }
        val groups = value.reversed().chunked(4).map { it.reversed() }.reversed()
        val result = StringBuilder()
        var zeroBetween = false
        groups.forEachIndexed { index, group ->
            val groupValue = group.toInt()
            if (groupValue == 0) {
                if (result.isNotEmpty()) zeroBetween = true
            } else {
                if (zeroBetween || (result.isNotEmpty() && groupValue < 1000)) result.append(digits[0])
                result.append(section(group)).append(largeUnits[groups.size - index - 1])
                zeroBetween = false
            }
        }
        return result.toString().replace(Regex("^${digits[1]}十"), if (digits[1] == '壹') "壹拾" else "十")
    }

    private fun chineseToNumber(input: String): String {
        val normalized = input.replace("人民币", "").replace("元", "").replace("整", "")
            .replace("角", "").replace("分", "").trim()
        if (normalized.all { it.isDigit() || it == '.' || it == '-' }) return normalized
        val digitMap = mapOf('零' to 0, '〇' to 0, '一' to 1, '壹' to 1, '二' to 2, '贰' to 2, '两' to 2, '三' to 3, '叁' to 3, '四' to 4, '肆' to 4, '五' to 5, '伍' to 5, '六' to 6, '陆' to 6, '七' to 7, '柒' to 7, '八' to 8, '捌' to 8, '九' to 9, '玖' to 9)
        val units = mapOf('十' to 10L, '拾' to 10L, '百' to 100L, '佰' to 100L, '千' to 1000L, '仟' to 1000L, '万' to 10000L, '萬' to 10000L, '亿' to 100000000L, '億' to 100000000L)
        var total = 0L
        var section = 0L
        var current = 0L
        normalized.substringBefore('点').forEach { char ->
            when {
                digitMap.containsKey(char) -> current = digitMap.getValue(char).toLong()
                units[char] in listOf(10L, 100L, 1000L) -> {
                    val unit = units.getValue(char)
                    section += (if (current == 0L) 1 else current) * unit
                    current = 0
                }
                units.containsKey(char) -> {
                    section += current
                    total += section * units.getValue(char)
                    section = 0
                    current = 0
                }
            }
        }
        val integer = total + section + current
        val decimal = normalized.substringAfter('点', "").mapNotNull { digitMap[it] }.joinToString("")
        return if (decimal.isBlank()) integer.toString() else "$integer.$decimal"
    }

    private val punctuationMap = mapOf('，' to ',', '。' to '.', '！' to '!', '？' to '?', '：' to ':', '；' to ';', '（' to '(', '）' to ')', '【' to '[', '】' to ']', '“' to '"', '”' to '"')
    private val simplifiedTraditional = mapOf('国' to '國', '汉' to '漢', '语' to '語', '体' to '體', '门' to '門', '开' to '開', '发' to '發', '后' to '後', '里' to '裏', '云' to '雲', '网' to '網')
}
