plugins {
    id("com.playmonumenta.plugins.java-conventions")
    id("io.papermc.paperweight.userdev") version "1.3.3"
}

dependencies {
    compileOnly(project(":adapter_api"))
    paperDevBundle("1.18.1-R0.1-SNAPSHOT")
    implementation("org.checkerframework:checker-qual:3.21.0")
}

description = "adapter_v1_18_R1"
version = rootProject.version

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

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
