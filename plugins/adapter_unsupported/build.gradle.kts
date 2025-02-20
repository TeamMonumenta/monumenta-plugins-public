plugins {
    id("com.playmonumenta.plugins.java-conventions")
}

dependencies {
    implementation(project(":adapter_api"))
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
}

description = "adapter_unsupported"
version = rootProject.version
