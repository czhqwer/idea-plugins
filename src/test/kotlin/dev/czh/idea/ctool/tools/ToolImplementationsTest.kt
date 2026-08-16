package dev.czh.idea.ctool.tools

import dev.czh.idea.ctool.model.ToolRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolImplementationsTest {
    private fun run(id: String, operation: String, input: String, secondary: String = "") =
        ToolImplementations.execute(id, ToolRequest(null, operation, input, secondary))

    @Test
    fun hashAndBase64RoundTrip() {
        assertEquals("5d41402abc4b2a76b9719d911017c592", run("hash", "MD5", "hello").text)
        val encoded = run("base64", "编码", "你好").text
        assertEquals("你好", run("base64", "解码", encoded).text)
    }

    @Test
    fun jsonConversionTools() {
        val json = "[{\"name\":\"Ada\",\"age\":36},{\"name\":\"Grace\",\"age\":37}]"
        assertEquals("37", run("json", "JSONPath", json, "$.1.age").text)
    }

    @Test
    fun jsonCompressionIsCompact() {
        val result = run("json", "压缩", "{ \"name\": \"Ada\", \"items\": [1, 2] }")
        assertFalse(result.isError)
        assertEquals("{\"name\":\"Ada\",\"items\":[1,2]}", result.text)
    }

    @Test
    fun jsonEditorFormatsCommonInputsAndFilters() {
        val params = ToolImplementations.normalizeJsonInput("name=Ada&age=36")
        assertFalse(params.isError)
        assertTrue(params.text.contains("\"name\": \"Ada\""))
        assertTrue(params.text.contains("\"age\": 36"))

        val xml = ToolImplementations.normalizeJsonInput("<user><name>Ada</name></user>")
        assertFalse(xml.isError)
        assertTrue(xml.text.contains("\"user\""))
        assertTrue(xml.text.contains("\"name\": \"Ada\""))

        val yaml = ToolImplementations.normalizeJsonInput("name: Ada\nage: 36")
        assertFalse(yaml.isError)
        assertTrue(yaml.text.contains("\"age\": 36"))

        val filtered = ToolImplementations.jsonEditorAction(
            "过滤",
            "[{\"name\":\"Ada\",\"active\":true},{\"name\":\"Grace\",\"active\":false}]",
            ".filter(x => x.active).map(x => x.name)",
        )
        assertFalse(filtered.isError)
        assertEquals("[\n  \"Ada\"\n]", filtered.text)
        assertTrue(ToolImplementations.jsonEditorAction("JSON 转 XML", "{\"name\":\"Ada\"}").text.contains("<name>Ada</name>"))
        assertTrue(ToolImplementations.jsonEditorAction("JSON 转 TypeScript", "{\"name\":\"Ada\"}").text.contains("interface Root"))
    }

    @Test
    fun conversionsAndValidation() {
        assertEquals("1000", run("unit", "长度", "1 km m").text)
        assertEquals("1101111", run("radix", "2进制", "111").text)
        assertEquals("157", run("radix", "8进制", "111").text)
        assertEquals("6f", run("radix", "16进制", "111").text)
        assertEquals("192.168.1.0", run("ipcalc", "IPv4", "192.168.1.10/24").text.substringAfter("网络地址: ").substringBefore('\n'))
        assertEquals("select 1", run("sqlFillParameter", "填充 MyBatis SQL", "select ?", "1").text)
        assertEquals("一百二十三", run("zhNumber", "数字转小写", "123").text)
        assertEquals("123", run("zhNumber", "小写转数字", "一百二十三").text)
    }

    @Test
    fun textDiffModes() {
        val lineDiff = run("diffs", "按行", "one\ntwo", "one\nthree")
        assertTrue(lineDiff.text.contains("- two"))
        assertTrue(lineDiff.text.contains("+ three"))

        val wordDiff = run("diffs", "按单词", "const a = 1", "const b = 1")
        assertTrue(wordDiff.text.contains("- a"))
        assertTrue(wordDiff.text.contains("+ b"))
    }

    @Test
    fun symmetricEncryptionRoundTrip() {
        val encrypted = run("aes", "加密", "hello", "secret")
        assertFalse(encrypted.isError)
        val decrypted = run("aes", "解密", encrypted.text, "secret")
        assertEquals("hello", decrypted.text)
    }

    @Test
    fun sm2EncryptionAndRsaSignatureRoundTrip() {
        val keyPair = run("sm2", "生成密钥对", "")
        assertFalse(keyPair.isError)
        val keyLines = keyPair.text.lines()
        val publicKey = keyLines[1]
        val privateKey = keyLines[3]
        val encrypted = run("sm2", "加密", "hello", publicKey)
        assertFalse(encrypted.isError)
        assertEquals("hello", run("sm2", "解密", encrypted.text, privateKey).text)

        val rsaKeys = run("sign", "生成密钥对", "")
        assertFalse(rsaKeys.isError)
        val rsaPrivate = rsaKeys.text.substringAfter("私钥：\n").trim()
        val rsaPublic = rsaKeys.text.substringAfter("公钥：\n").substringBefore("\n\n私钥：").trim()
        val signature = run("sign", "签名 SHA256withRSA", "hello", rsaPrivate)
        assertFalse(signature.isError)
        assertEquals("true", run("sign", "验证 SHA256withRSA", "hello", "$rsaPublic\n${signature.text}").text)
    }
}
