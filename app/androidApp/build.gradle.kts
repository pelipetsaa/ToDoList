import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":app:shared"))
    implementation(project(":app:data"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

// Override in ~/.gradle/gradle.properties or with -P.
// Debug targets a server on the host machine; release targets the deployed server and must use HTTPS.
val debugApiBaseUrl = providers.gradleProperty("todolist.debugApiBaseUrl").getOrElse("http://10.0.2.2:8080")
val releaseApiBaseUrl = providers.gradleProperty("todolist.releaseApiBaseUrl").getOrElse("")
val googleWebClientId = providers.gradleProperty("todolist.googleWebClientId").getOrElse("")

// CI passes the run number as versionCode and the git tag as versionName.
val appVersionCode = providers.gradleProperty("todolist.versionCode").getOrElse("1").toInt()
val appVersionName = providers.gradleProperty("todolist.versionName").getOrElse("1.0.0")

// Release signing key, kept outside the repository: ~/.gradle/gradle.properties locally,
// ORG_GRADLE_PROJECT_todolist.signing.* environment variables in CI. See docs/release.md.
val releaseStoreFile = providers.gradleProperty("todolist.signing.storeFile").orNull
val releaseSigningConfigured = releaseStoreFile != null

android {
    namespace = "com.pelypets.todolist"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.pelypets.todolist"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }
    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = providers.gradleProperty("todolist.signing.storePassword").get()
                keyAlias = providers.gradleProperty("todolist.signing.keyAlias").get()
                keyPassword = providers.gradleProperty("todolist.signing.keyPassword").get()
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            // Without a release key the APK is signed with the debug key: installable for testing, not for the store.
            signingConfig = signingConfigs.getByName(if (releaseSigningConfigured) "release" else "debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // Errors fail the build; warnings stay in the report (build/reports/lint-results-*.html).
        abortOnError = true
        checkDependencies = true
        // Dependency updates are proposed by Dependabot instead.
        disable += setOf("NewerVersionAvailable", "AndroidGradlePluginVersion", "GradleDependency")
    }
}

// Fail release builds early instead of shipping an app that cannot reach its server.
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    val url = releaseApiBaseUrl
    doFirst {
        require(url.startsWith("https://")) {
            "Set todolist.releaseApiBaseUrl to the HTTPS address of the deployed server (current value: '$url')."
        }
    }
}
