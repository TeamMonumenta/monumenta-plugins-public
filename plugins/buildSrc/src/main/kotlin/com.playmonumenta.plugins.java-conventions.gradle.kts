plugins {
	`java-library`
}

repositories {
	mavenCentral()
	maven("https://maven.playmonumenta.com/releases/")
	maven("https://repo.papermc.io/repository/maven-public/")
	maven("https://jitpack.io")
	maven("https://repo.codemc.io/repository/maven-public/")
	maven("https://maven.playpro.com/")
}

group = "com.playmonumenta.plugins"
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17
