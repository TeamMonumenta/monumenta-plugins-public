paperweight.awPath.set(file("src/main/resources/v1_20_R3/monumenta.accesswidener"))

dependencies {
	compileOnly("com.playmonumenta.papermixins:plugin-api:1.0.0")
}

tasks {
	remapAccessWidener {
		fileOverride.set("v1_20_R3/monumenta.accesswidener")
	}
}
