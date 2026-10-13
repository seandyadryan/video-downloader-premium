import java.util.Properties
import java.security.KeyStore
import java.security.MessageDigest

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

val signing = Properties().apply {
    val source = rootProject.file("keystore.properties")
    if (source.exists()) source.inputStream().use { load(it) }
}
fun secret(key: String, env: String): String? = System.getenv(env) ?: signing.getProperty(key)
val keyPath = secret("storeFile", "KEYSTORE_PATH")
val storePass = secret("storePassword", "KEYSTORE_PASSWORD")
val keyPass = secret("keyPassword", "KEY_PASSWORD")
val alias = secret("keyAlias", "KEY_ALIAS")
val canSign = listOf(keyPath, storePass, keyPass, alias).all { !it.isNullOrBlank() }

android {
    namespace = "com.deploydulupulangnanti.videodownloaderpremium"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.deploydulupulangnanti.videodownloaderpremium"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        if (canSign) create("premiumRelease") {
            storeFile = file(keyPath!!)
            storePassword = storePass
            keyAlias = alias
            keyPassword = keyPass
            storeType = "JKS"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (canSign) signingConfig = signingConfigs.getByName("premiumRelease")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { buildConfig = true }
    androidResources { generateLocaleConfig = true }
    lint { abortOnError = true; checkReleaseBuilds = true }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

gradle.taskGraph.whenReady {
    if (allTasks.any { it.name in listOf("assembleRelease", "bundleRelease", "packageRelease", "signReleaseBundle") }) {
        if (!canSign) error("Release signing required. Configure keystore.properties or KEYSTORE_PATH/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD.")
        val expected = rootProject.file("upload-certificate.sha1").readText().trim().replace(":", "").uppercase()
        check(expected.matches(Regex("[0-9A-F]{40}"))) { "Invalid expected upload certificate SHA-1" }
        val keyStore = KeyStore.getInstance("JKS")
        file(keyPath!!).inputStream().use { keyStore.load(it, storePass!!.toCharArray()) }
        val certificate = keyStore.getCertificate(alias!!) ?: error("Signing alias has no certificate")
        val actual = MessageDigest.getInstance("SHA-1").digest(certificate.encoded)
            .joinToString("") { "%02X".format(it.toInt() and 0xff) }
        check(actual == expected) { "Wrong release upload key. Expected SHA-1 $expected, received $actual. Check the keystore and GitHub signing secrets." }
    }
}
dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("com.google.android.material:material:1.12.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
