plugins {
	id("com.android.application") version "9.1.1" apply false
	id("com.android.library") version "9.1.1" apply false
	id("org.jetbrains.kotlin.android") version "2.2.10" apply false
	id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
	id("com.chaquo.python") version "17.0.0" apply false
}

tasks.register<Delete>("clean") {
	delete(layout.buildDirectory)
}