import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
}

group = property("group") as String
version = property("version") as String

kotlin {
    jvmToolchain(17)
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.1.7")
        testFramework(TestFrameworkType.Platform)
    }

    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.yaml:snakeyaml:2.4")
    implementation("org.bouncycastle:bcprov-jdk18on:1.80")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.80")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")
    implementation("com.belerweb:pinyin4j:2.5.1")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("com.cronutils:cron-utils:9.2.1")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

tasks {
    test {
        useJUnitPlatform()
    }
}
