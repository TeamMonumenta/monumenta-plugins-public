repositories {
	maven("https://maven.playpro.com/")
}

dependencies {
	// NOTE - Make sure if you add another version here you make sure to exclude it from minimization below!
	implementation(project(":adapter_api"))
	implementation(project(":adapter_unsupported"))
	implementation(project(":adapter_v1_20_R3", "reobf"))
	implementation(project(":velocity"))

	implementation("com.opencsv:opencsv:5.5") // generateitems
	implementation("org.apache.commons:commons-lang3:3.17.0")
	implementation("org.apache.commons:commons-math3:3.6.1")
	implementation("com.playmonumenta.papermixins:plugin-api:2.0.5")

	compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
	compileOnly("dev.jorel:commandapi-bukkit-core:9.4.1")
	compileOnly("me.clip:placeholderapi:2.10.9")
	compileOnly("de.jeff_media:ChestSortAPI:12.0.0")
	compileOnly("net.luckperms:api:5.4")
	compileOnly("net.coreprotect:coreprotect:2.15.0") {
		exclude(group = "org.bukkit")
	}
	compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.9.1")
	compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.9.1")
	compileOnly("com.playmonumenta:scripted-quests:7.0:all")
	compileOnly("com.playmonumenta:redissync:5.2:all")
	compileOnly("com.playmonumenta:monumenta-network-chat:2.13")
	compileOnly("com.playmonumenta:monumenta-network-relay:2.13")
	compileOnly("com.playmonumenta:structures:10.2")
	compileOnly("com.playmonumenta:worlds:2.3.1")
	compileOnly("com.playmonumenta:libraryofsouls:5.3.1")
	compileOnly("com.bergerkiller.bukkit:BKCommonLib:1.20.6-v1")
	compileOnly("com.mojang:brigadier:1.0.18")
	compileOnly("com.playmonumenta:nbteditor:4.1:all")
	compileOnly("de.tr7zw:item-nbt-api-plugin:2.15.2")
	compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
	compileOnly("io.prometheus:simpleclient:0.11.0")
	compileOnly("com.github.LeonMangler:PremiumVanishAPI:2.9.0-4")
	compileOnly("me.neznamy:tab-api:4.0.2")
	compileOnly("com.vexsoftware:nuvotifier-universal:3.0.0:all")

	errorprone("com.google.errorprone:error_prone_core:2.29.1")
	errorprone("com.uber.nullaway:nullaway:0.10.18")
}

// Relocation / shading
tasks {
	shadowJar {
		relocate("com.opencsv", "com.playmonumenta.plugins.internal.com.opencsv") // /generateitems
		relocate(
			"org.apache.commons.lang3",
			"com.playmonumenta.plugins.internal.org.apache.commons.lang3"
		) // Dependency of several things
		relocate(
			"org.apache.commons.math3",
			"com.playmonumenta.plugins.internal.org.apache.commons.math3"
		) // Dependency of several things
		minimize {
			exclude(project(":adapter_api"))
			exclude(project(":adapter_unsupported"))
			exclude(project(":adapter_v1_20_R3"))
			exclude(project(":velocity"))
		}

		dependsOn(":adapter_v1_20_R3:remapAccessWidener")
	}
}
