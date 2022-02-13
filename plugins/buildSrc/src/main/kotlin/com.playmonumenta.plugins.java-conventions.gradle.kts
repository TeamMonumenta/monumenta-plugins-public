plugins {
    `java-library`
    `maven-publish`
    checkstyle
    pmd
}

repositories {
    mavenLocal()
    jcenter()
    maven {
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }

    maven {
        url = uri("https://jitpack.io")
    }

    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }

    maven {
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }

    maven {
        url = uri("https://maven.playpro.com/")
    }

    maven {
        url = uri("https://repo.dmulloy2.net/nexus/repository/public/")
    }

    maven {
        url = uri("https://hub.jeff-media.com/nexus/repository/jeff-media-public/")
    }

    maven {
        url = uri("https://ci.mg-dev.eu/plugin/repository/everything")
    }

    maven {
        url = uri("https://raw.githubusercontent.com/TeamMonumenta/monumenta-redis-sync/master/mvn-repo/")
    }

    maven {
        url = uri("https://raw.githubusercontent.com/TeamMonumenta/monumenta-structure-management/master/mvn-repo/")
    }

    maven {
        url = uri("https://maven.enginehub.org/repo/")
    }

    maven {
        url = uri("https://raw.githubusercontent.com/TeamMonumenta/monumenta-network-relay/master/mvn-repo/")
    }

    maven {
        url = uri("https://raw.githubusercontent.com/TeamMonumenta/monumenta-world-management/master/mvn-repo/")
    }

    maven {
        url = uri("https://raw.githubusercontent.com/TeamMonumenta/library-of-souls/master/mvn-repo/")
    }

    maven {
        url = uri("https://raw.githubusercontent.com/TeamMonumenta/scripted-quests/master/mvn-repo/")
    }

    maven {
        url = uri("https://maven.sk89q.com/repo/")
    }

    maven {
        url = uri("https://repo.bstats.org/content/repositories/releases")
    }

    maven {
        url = uri("https://repo.codemc.org/repository/nms")
    }

    // NBT API
    maven {
        url = uri("https://repo.codemc.org/repository/maven-public/")
    }

    maven {
        url = uri("https://raw.githubusercontent.com/TeamMonumenta/monumenta-network-relay/master/mvn-repo/")
    }

    maven {
        url = uri("https://raw.githubusercontent.com/TeamMonumenta/NuVotifier/master/mvn-repo/")
    }

    // This is ridiculously jank - accessing the repo from github when it's local... but can't get it to work otherwise
    maven {
        url = uri("https://raw.githubusercontent.com/TeamMonumenta/monumenta-plugins-public/master/plugins/paper/repo/")
    }
}

group = "com.playmonumenta.plugins"
java.sourceCompatibility = JavaVersion.VERSION_16
java.targetCompatibility = JavaVersion.VERSION_16

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

pmd {
    isConsoleOutput = true
    toolVersion = "6.41.0"
    ruleSets = listOf("$rootDir/pmd-ruleset.xml")
    setIgnoreFailures(true)
}
