//noinspection ChromeOsAbiSupport

plugins {
	id("androidx.room")
	id("com.chaquo.python")
	id("com.android.application")
	id("com.google.devtools.ksp")
	id("org.jetbrains.kotlin.plugin.compose")
	id("com.autonomousapps.dependency-analysis")
}

val appVersionName = "1.0.0"
val appVersionCode = 1

val abiVersionCodes = mapOf(
	"armeabi-v7a" to 1,
	"arm64-v8a" to 2,
	"x86" to 3,
	"x86_64" to 4
)

@Suppress("UnstableApiUsage")
androidComponents {
	onVariants(selector().all()) { variant ->
		val abiCode = abiVersionCodes[variant.flavorName] ?: 0
		variant.outputs.forEach { output ->
			output.versionCode.set(appVersionCode * 10 + abiCode)
			output.outputFileName.set("TVBoxOS_${appVersionName}_${variant.flavorName}.apk")
		}
	}
}

android {
	namespace = "com.github.tvbox.osc"
	compileSdk = 37

	defaultConfig {
		applicationId = "com.github.tvbox.osc"
		minSdk = 28
		targetSdk = 37
		versionName = appVersionName
		versionCode = appVersionCode
		multiDexEnabled = true
	}

	flavorDimensions += "abi"

	productFlavors {
		create("armeabi-v7a") {
			dimension = "abi"
			ndk { abiFilters += listOf("armeabi-v7a") }
		}
		create("arm64-v8a") {
			dimension = "abi"
			ndk { abiFilters += listOf("arm64-v8a") }
		}
		create("x86") {
			dimension = "abi"
			ndk { abiFilters += listOf("x86") }
		}
		create("x86_64") {
			dimension = "abi"
			ndk { abiFilters += listOf("x86_64") }
		}
		create("universal") {
			dimension = "abi"
			ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64") }
		}
	}

	packaging {
		resources {
			excludes += "META-INF/DEPENDENCIES"
		}
	}

	buildTypes {
		getByName("debug") {
			isMinifyEnabled = false
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}

		getByName("release") {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}

	buildFeatures {
		aidl = true
		compose = true
	}

	lint {
		abortOnError = false
		checkReleaseBuilds = false
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	sourceSets.getByName("main") {
		setRoot("src/main")
	}
}

room {
	schemaDirectory("$projectDir/schemas")
}

chaquopy {
	defaultConfig {
		pip {
			install("lxml")
			install("ujson")
			install("pyquery")
			install("requests")
			install("jsonpath_ng")
			install("cachetools")
			install("pycryptodome")
			install("beautifulsoup4")
		}
	}
}

configurations.configureEach {
	exclude(group = "com.android.support")
}

dependencies {
	api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

	implementation(platform("androidx.compose:compose-bom:2026.06.01"))
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-graphics")
	implementation("androidx.compose.ui:ui-tooling-preview")
	implementation("androidx.compose.material3:material3:1.5.0-alpha23")
	implementation("androidx.activity:activity-compose:1.13.0")
	implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
	implementation("androidx.compose.material:material-icons-extended")
	implementation("androidx.compose.material3:material3-window-size-class")
	implementation("androidx.compose.material3.adaptive:adaptive:1.2.0")
	implementation("androidx.compose.material3.adaptive:adaptive-layout:1.2.0")
	implementation("androidx.compose.material3.adaptive:adaptive-navigation:1.2.0")
	implementation("androidx.tv:tv-foundation:1.0.0")
	implementation("androidx.navigation:navigation-compose:2.9.8")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
	implementation("com.google.accompanist:accompanist-pager:0.36.0")
	implementation("com.google.accompanist:accompanist-pager-indicators:0.36.0")
	implementation("androidx.compose.runtime:runtime-livedata")
	implementation("io.coil-kt:coil-compose:2.7.0")
	implementation("androidx.navigation:navigation-runtime-ktx:2.9.8")
	debugImplementation("androidx.compose.ui:ui-tooling")
	debugImplementation("androidx.compose.ui:ui-test-manifest")
	api("wang.harlon.quickjs:wrapper-android:3.2.3")
	implementation("org.nanohttpd:nanohttpd:2.3.1")
	implementation("com.google.zxing:core:3.5.4")
	implementation("com.google.android.material:material:1.14.0")
	implementation("androidx.appcompat:appcompat:1.7.1")
	implementation("androidx.constraintlayout:constraintlayout:2.2.1")
	implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
	implementation("androidx.recyclerview:recyclerview:1.4.0")
	implementation("com.squareup.okhttp3:okhttp:5.4.0")
	implementation("androidx.core:core-ktx:1.19.0")
	implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
	implementation("androidx.datastore:datastore-preferences:1.2.1")
	ksp("androidx.room:room-compiler:2.8.4")
	implementation("androidx.room:room-runtime:2.8.4")
	implementation("com.squareup.okio:okio:3.17.0")
	implementation("com.github.CymChad:BaseRecyclerViewAdapterHelper:2.9.45-androidx")
	implementation("com.kingja.loadsir:loadsir:1.3.8")
	implementation("com.google.code.gson:gson:2.14.0")
	implementation("com.squareup.picasso:picasso:2.71828")
	implementation("me.jessyan:autosize:1.2.1")
	implementation("com.thoughtworks.xstream:xstream:1.4.21") {
		exclude(group = "xmlpull", module = "xmlpull")
	}
	implementation("org.greenrobot:eventbus:3.3.1")
	implementation("com.lzy.net:okgo:3.0.4")
	implementation("com.owen:tv-recyclerview:3.0.0")
	implementation("org.jsoup:jsoup:1.22.2")
	implementation("com.github.hedzr:android-file-chooser:v1.2.0-final")
	implementation("commons-io:commons-io:2.22.0")
	implementation("com.googlecode.juniversalchardet:juniversalchardet:1.0.3")
	implementation("com.google.net.cronet:cronet-okhttp:0.1.1")
	implementation("org.brotli:dec:0.1.2")
	implementation("androidx.media3:media3-exoplayer:1.10.1")
	implementation("androidx.media3:media3-datasource-okhttp:1.10.1")

	implementation("io.github.lky-lockee.dkplayer:dkplayer-java:4.0.1")
	implementation("io.github.lky-lockee.dkplayer:dkplayer-ui:4.0.1")
	implementation("io.github.lky-lockee.dkplayer:videocache:4.0.1")
	implementation("io.github.lky-lockee.dkplayer:player-exo:4.0.1")
	implementation("io.github.lky-lockee.dkplayer:player-ijk:4.0.1")
}