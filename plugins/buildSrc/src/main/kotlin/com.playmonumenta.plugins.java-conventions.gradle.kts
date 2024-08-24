plugins {
    `java-library`
    `maven-publish`
    checkstyle
    pmd
}

repositories {
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.maven.apache.org/maven2/")
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://maven.playpro.com/")
    maven("https://repo.jeff-media.com/public/")
    maven("https://ci.mg-dev.eu/plugin/repository/everything")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.codemc.org/repository/nms")
    // NBT API
    maven("https://repo.codemc.org/repository/maven-public/")
	mavenCentral()
	maven("https://libraries.minecraft.net/")
	maven("https://repo.codemc.org/repository/maven-public/")
	maven("https://maven.playmonumenta.com/releases/")
    // Velocity dependencies
    maven("https://repo.kryptonmc.org/releases")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

group = "com.playmonumenta.plugins"
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

pmd {
    isConsoleOutput = true
    toolVersion = "7.2.0"
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
