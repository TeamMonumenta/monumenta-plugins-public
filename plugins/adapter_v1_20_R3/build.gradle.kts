plugins {
    id("com.playmonumenta.plugins.java-conventions")
    id("com.playmonumenta.paperweight-aw.userdev") version "1.1.1"
}

repositories {
	maven("https://maven.playmonumenta.com/releases/")
}

paperweight.awPath.set(file("monumenta.accesswidener"))

dependencies {
    implementation(project(":adapter_api"))
    paperDevBundle("1.20.4-R0.1-SNAPSHOT")
	compileOnly("com.playmonumenta.papermixins:plugin-api:1.0.0")
}

description = "adapter_v1_20_R3"
version = rootProject.version

tasks {
    // Configure reobfJar to run when invoking the build task
    assemble {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(17)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
}

configurations.all {
    resolutionStrategy {
        dependencySubstitution {
            substitute(module("org.jetbrains:annotations:20.1.+")).using(module("org.jetbrains:annotations:20.1.0"))
        }
    }
}
