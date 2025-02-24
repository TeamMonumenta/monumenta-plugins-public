plugins {
	id("com.playmonumenta.plugins.java-conventions")
}

dependencies {
	// Velocity dependencies
	compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
	annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")

	// Monumenta plugins
	compileOnly("com.playmonumenta:redissync:5.2:all")
	compileOnly("com.playmonumenta:monumenta-network-relay:2.9")

	// Other dependencies
	compileOnly("com.github.LeonMangler:PremiumVanishAPI:2.9.0-4")
	compileOnly("net.luckperms:api:5.4")
	compileOnly("com.vexsoftware:nuvotifier-universal:3.0.0:all")
}

description = "velocity"
version = rootProject.version
