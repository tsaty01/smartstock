plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.kotlin.serialization)
}

val appVersion = providers.gradleProperty("version").get()

/** 1.2.3-dev.4 -> 1_002_003_04; a stable release sorts after its prereleases (suffix 99). */
fun versionCodeOf(version: String): Int {
	val (core, pre) = version.split("-", limit = 2).let { it[0] to it.getOrNull(1) }
	val (major, minor, patch) = core.split(".").map(String::toInt)
	val prerelease = pre?.substringAfterLast('.')?.toIntOrNull() ?: 99
	return ((major * 1000 + minor) * 1000 + patch) * 100 + prerelease
}

android {
	namespace = "com.smartstock.app"
	compileSdk = 37

	defaultConfig {
		applicationId = "com.smartstock.app"
		minSdk = 26
		targetSdk = 37
		versionName = appVersion
		versionCode = versionCodeOf(appVersion)
		buildConfigField("String", "DEFAULT_SERVER_URL", "\"http://10.0.2.2:8080\"")
	}

	signingConfigs {
		create("release") {
			// Supplied by CI; without them the release APK is left unsigned.
			System.getenv("ANDROID_KEYSTORE_FILE")?.let { storeFile = file(it) }
			storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
			keyAlias = System.getenv("ANDROID_KEY_ALIAS")
			keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
			if (System.getenv("ANDROID_KEYSTORE_FILE") != null) {
				signingConfig = signingConfigs.getByName("release")
			}
		}
	}

	buildFeatures {
		compose = true
		buildConfig = true
	}
}

kotlin {
	jvmToolchain(21)
	compilerOptions {
		optIn.addAll(
			"androidx.compose.material3.ExperimentalMaterial3Api",
			"androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
		)
	}
}

base {
	archivesName = "smartstock-android-$appVersion"
}

dependencies {
	implementation(platform(libs.compose.bom))
	implementation(libs.compose.ui)
	implementation(libs.compose.ui.tooling.preview)
	implementation(libs.compose.material3)
	implementation(libs.compose.material.icons)
	implementation(libs.activity.compose)
	implementation(libs.core.ktx)
	implementation(libs.core.splashscreen)
	implementation(libs.lifecycle.runtime.compose)
	implementation(libs.lifecycle.viewmodel.compose)
	implementation(libs.navigation.compose)
	implementation(libs.datastore.preferences)
	implementation(libs.coroutines.android)
	implementation(libs.serialization.json)
	implementation(libs.retrofit)
	implementation(libs.retrofit.kotlinx.serialization)
	implementation(libs.okhttp)
	implementation(libs.coil.compose)
	implementation(libs.coil.network.okhttp)
	debugImplementation(libs.compose.ui.tooling)
}
