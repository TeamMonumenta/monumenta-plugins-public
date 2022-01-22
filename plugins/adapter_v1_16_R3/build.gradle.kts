plugins {
    id("com.playmonumenta.plugins.java-conventions")
}

dependencies {
    compileOnly(project(":adapter_api"))
    compileOnly("com.destroystokyo.paper:paper:1.16.5-R0.1-SNAPSHOT")
    implementation("org.checkerframework:checker-qual:3.21.0")
}

description = "adapter_v1_16_R3"
version = rootProject.version
