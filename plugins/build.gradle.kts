import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
	id("com.playmonumenta.gradle-config") version "3.3"
}

monumenta {
	id("Monumenta")
	name("Monumenta")
	pluginProject(":Monumenta")
	paper(
		"com.playmonumenta.plugins.Plugin", BukkitPluginDescription.PluginLoadOrder.POSTWORLD, "1.20",
		depends = listOf(
			"BKCommonLib",
			"CommandAPI",
			"FastAsyncWorldEdit",
			"ScriptedQuests"
		),
		softDepends = listOf(
			"NBTAPI",
			"MonumentaRedisSync",
			"PlaceholderAPI",
			"ChestSort",
			"LuckPerms",
			"CoreProtect",
			"NBTEditor",
			"LibraryOfSouls",
			"MonumentaNetworkChat",
			"MonumentaNetworkRelay",
			"PremiumVanish",
			"ProtocolLib",
			"PrometheusExporter",
			"MonumentaStructureManagement",
			"MonumentaWorldManagement",
			"TAB"
		)
	)

	versionAdapterApi("adapter_api", paper = "1.20.4")
	versionAdapter("adapter_v1_20_R3", "1.20.4")
	versionAdapterUnsupported("adapter_unsupported")
	javaSimple(":velocity")
	disableMaven()
	disableJavadoc()
}
