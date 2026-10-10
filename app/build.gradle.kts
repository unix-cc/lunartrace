import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    jacoco
}

// 发布签名材料来自仓库根的 keystore.properties（不入库，见 .gitignore / DEVELOP.md §3）。
// 缺它时 release 包会是 unsigned —— 手机安装会报"安装包缺乏开发者证书"，因此下面禁止产出这种包。
val releaseSigningKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
val hasReleaseSigning = releaseSigningKeys.all { !keystoreProperties.getProperty(it).isNullOrBlank() }

android {
    namespace = "io.lunartrace.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.lunartrace.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }
}

// Room schema 导出：schema JSON 入库，用于人工核对表结构漂移（见 specs/capabilities/local-data-model.md）
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    detektPlugins(libs.detekt.formatting)

    testImplementation(libs.junit)
}

// 宁可不产出，也不产出一个"看起来像正式版、装到手机上却报缺乏开发者证书"的包。
// 只有显式构建 release 时才会触发，不影响日常的 debug 构建与 CI。
tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    doFirst {
        check(hasReleaseSigning) {
            "缺少可用的 keystore.properties，release 包无法签名（手机安装会报“安装包缺乏开发者证书”）。\n" +
                "  · 只是想在手机上试：./gradlew :app:assembleDebug（debug 包由 debug keystore 签名，可直接安装）\n" +
                "  · 想发正式包：按 DEVELOP.md §3 生成 keystore 并创建 keystore.properties"
        }
    }
}

// 单元测试挂 JaCoCo agent（覆盖率硬门禁的数据来源）
tasks.withType<Test>().configureEach {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "生成单元测试覆盖率报告（XML 供 scripts/check-coverage.py 校验）"
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    sourceDirectories.setFrom(files("src/main/java"))
    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
            exclude(
                "**/R.class",
                "**/R\$*.class",
                "**/BuildConfig.*",
            )
        },
    )
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("jacoco/testDebugUnitTest.exec", "outputs/unit_test_code_coverage/debugUnitTest/*.exec")
        },
    )
}
