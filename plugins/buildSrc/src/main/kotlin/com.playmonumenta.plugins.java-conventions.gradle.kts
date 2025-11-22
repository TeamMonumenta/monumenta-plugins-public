plugins {
	`java-library`
	`maven-publish`
	checkstyle
	pmd
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

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
	options.compilerArgs.add("-Xlint:unchecked")
	options.compilerArgs.add("-Xlint:deprecation")
}

pmd {
	isConsoleOutput = true
	toolVersion = "7.11.0"
	ruleSets = listOf("$rootDir/pmd-ruleset.xml")
	setIgnoreFailures(true)
}

checkstyle {
	toolVersion = "10.17.0"
}

tasks.withType<Checkstyle>().configureEach {
	minHeapSize = "200m"
	maxHeapSize = "1g"
}
