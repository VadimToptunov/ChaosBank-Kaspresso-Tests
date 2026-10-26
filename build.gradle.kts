import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

val kaspresso = "1.6.1"

// Graft the suite onto the app module: its sources become the app's androidTest
// source set, so tests run in-process with MainActivity and the Compose testTags.
project(":app") {
    pluginManager.withPlugin("com.android.application") {
        extensions.configure<ApplicationExtension> {
            sourceSets.getByName("androidTest").kotlin.srcDir(rootProject.file("suite/src"))
            testOptions.animationsDisabled = true
        }
        dependencies {
            "androidTestImplementation"("com.kaspersky.android-components:kaspresso:$kaspresso")
            "androidTestImplementation"("com.kaspersky.android-components:kaspresso-compose-support:$kaspresso")
            // The web login is an HTML form in a WebView: Espresso Web drives its DOM.
            "androidTestImplementation"("androidx.test.espresso:espresso-web:3.7.0")
        }
    }
}
