plugins {
    `java-library`
    `maven-publish`
    checkstyle
    pmd
}

repositories {
    mavenCentral()
    // Monumenta
    maven("https://maven.playmonumenta.com/releases/")
    // Minecraft
    maven("https://libraries.minecraft.net/")
    // Paper
    maven("https://repo.papermc.io/repository/maven-public/")
    // ?
    maven("https://repo.maven.apache.org/maven2/")
    // PremiumVanishAPI
    maven("https://jitpack.io")
    // Velocity and Adventure
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    // CoreProtect
    maven("https://maven.playpro.com/")
    // ChestSort
    maven("https://repo.jeff-media.com/public/")
    // BKCommonLib
    maven("https://ci.mg-dev.eu/plugin/repository/everything")
    // ProtocolLib
    maven("https://repo.dmulloy2.net/repository/public/")
    // NMS?
    maven("https://repo.codemc.org/repository/nms")
    // NBT API
    maven("https://repo.codemc.org/repository/maven-public/")
    // PlaceholderAPI
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    // Velocity
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    // FAWE: This one is funky and needs to be last (doesn't respond with 404)
    maven("https://maven.enginehub.org/repo/")
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
