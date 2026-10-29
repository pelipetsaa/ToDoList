plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.kover)
}

// Coverage across modules: ./gradlew koverHtmlReport -> build/reports/kover/html/index.html
val coveredModules = listOf(":core", ":server", ":app:domain", ":app:data", ":app:shared")
coveredModules.forEach { path ->
    project(path).pluginManager.apply("org.jetbrains.kotlinx.kover")
    // Name the module in `koverLog` output so CI summaries show which number is which.
    project(path).extensions.configure<kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension> {
        reports { total { log { format = "$path line coverage: <value>%" } } }
    }
}

dependencies {
    coveredModules.forEach { kover(project(it)) }
}

kover {
    reports {
        filters {
            excludes {
                // Generated code and Compose UI, which unit tests do not exercise.
                packages("todolist.app.shared.generated.resources")
                classes("*BuildConfig", "*ComposableSingletons*")
                annotatedBy("androidx.compose.runtime.Composable")
            }
        }
    }
}

// ./gradlew spotlessCheck verifies formatting, ./gradlew spotlessApply fixes it. Rules live in .editorconfig.
// Excluded directories are not walked at all, so formatting can run next to a build writing into build/.
fun sources(pattern: String) = fileTree(rootDir) {
    include(pattern)
    exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**")
}

spotless {
    kotlin {
        target(sources("**/*.kt"))
        ktlint(libs.versions.ktlint.get())
            .setEditorConfigPath(rootProject.file(".editorconfig"))
            .customRuleSets(listOf("io.nlopez.compose.rules:ktlint:${libs.versions.composeRulesKtlint.get()}"))
            // Compose rule settings; Spotless does not pick these up from .editorconfig reliably.
            .editorConfigOverride(
                mapOf(
                    "compose_allowed_composition_locals" to "LocalGoogleSignInProvider",
                    "ktlint_compose_modifier-missing-check" to "disabled",
                ),
            )
    }
    kotlinGradle {
        target(sources("**/*.gradle.kts"))
        ktlint(libs.versions.ktlint.get()).setEditorConfigPath(rootProject.file(".editorconfig"))
    }
}
