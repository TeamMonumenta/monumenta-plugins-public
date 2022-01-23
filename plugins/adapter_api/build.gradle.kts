plugins {
    id("com.playmonumenta.plugins.java-conventions")
}

dependencies {
    compileOnly("com.destroystokyo.paper:paper-api:1.15.2-R0.1-SNAPSHOT")
    compileOnly("org.checkerframework:checker-qual:3.21.0")
}

description = "adapter_api"
version = rootProject.version
