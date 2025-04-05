import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import sp.gx.core.buildDir
import sp.gx.core.camelCase
import sp.gx.core.create
import sp.gx.core.getByName
import sp.gx.core.map
import sp.gx.core.qn
import sp.gx.core.string
import sp.gx.core.xml

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.compose") version Version.compose
}

android {
    namespace = "test.android.cp"
    compileSdk = Version.Android.compileSdk

    defaultConfig {
        applicationId = namespace
        minSdk = Version.Android.minSdk
        targetSdk = Version.Android.targetSdk
        versionCode = 1
        versionName = "0.0.$versionCode"
    }

    buildTypes {
        setOf("bt1", "bt2").forEach { name ->
            create(name) {
                initWith(getByName("debug"))
                applicationIdSuffix = ".$name"
                versionNameSuffix = "-$name"
                isMinifyEnabled = false
                isShrinkResources = false
                signingConfig = signingConfigs.create(name) {
                    storeFile = file("src/$name/$name.pkcs12")
                    storePassword = "${name}1234"
                    keyPassword = storePassword
                    keyAlias = name
                }
            }
        }
        for (buildType in this) {
            getByName(buildType.name) {
//                manifestPlaceholders["buildType"] = name
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions.kotlinCompilerExtensionVersion = "1.5.15"

    productFlavors {
        "version".also { dimension ->
            flavorDimensions += dimension
            setOf("pf1").forEach { name ->
                create(name) {
                    this.dimension = dimension
                    applicationIdSuffix = ".$name"
                    versionNameSuffix = "-$name"
                }
            }
        }
    }
    applicationVariants.all {
//        variant.mergedFlavor.manifestPlaceholders["buildType"] = variant.buildType
//        variant.mergedFlavor.manifestPlaceholders["flavorName"] = variant.flavorName
//        mergedFlavor.manifestPlaceholders["applicationId"] = applicationId
    }
}

androidComponents.onVariants { variant ->
    val output = variant.outputs.single()
    check(output is com.android.build.api.variant.impl.VariantOutputImpl)
    output.outputFileName = listOf(
        rootProject.name,
        android.defaultConfig.versionName!!,
        variant.name,
        android.defaultConfig.versionCode!!.toString(),
    ).joinToString(separator = "-", postfix = ".apk")
    afterEvaluate {
        tasks.getByName<JavaCompile>("compile", variant.name, "JavaWithJavac") {
            targetCompatibility = Version.jvmTarget
        }
        tasks.getByName<KotlinCompile>("compile", variant.name, "Kotlin") {
            kotlinOptions.jvmTarget = Version.jvmTarget
        }
        val checkManifestTask = tasks.create("checkManifest", variant.name) {
            dependsOn(camelCase("compile", variant.name, "Sources"))
            doLast {
                val actual = layout.buildDir()
                    .dir("intermediates/merged_manifests/${variant.name}")
                    .dir(camelCase("process", variant.name, "Manifest"))
                    .xml("AndroidManifest.xml")
                    .map("uses-permission".qn()) {
                        it.string("{http://schemas.android.com/apk/res/android}name".qn())
                    }
                val expected = setOf(
                    "${variant.applicationId.get()}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
                    "test.android.cp.permission.provider", // todo
                )
                check(actual.sorted() == expected.sorted()) {
                    "Actual is:\n$actual\nbut expected is:\n$expected"
                }
            }
        }
        tasks.getByName(camelCase("assemble", variant.name)) {
            dependsOn(checkManifestTask)
        }
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(compose.foundation)
}
