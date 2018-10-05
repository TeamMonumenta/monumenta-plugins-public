package com.playmonumenta.bossfights;

import com.playmonumenta.bossfights.commands.BossFight;
import com.playmonumenta.bossfights.utils.MetadataUtils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Plugin extends JavaPlugin {
	public BossManager mBossManager;

	@Override
	public void onEnable() {
		Bukkit.getConsoleSender().sendMessage("[Monumenta_bossfights] Plugin enabled!");

		mBossManager = new BossManager(this);
		getServer().getPluginManager().registerEvents(mBossManager, this);

		getCommand("mobspell").setExecutor(new MobSpell(this));

		BossFight.register(mBossManager);
	}

	@Override
	public void onDisable() {
		mBossManager.unloadAll();

		getServer().getScheduler().cancelTasks(this);

		MetadataUtils.removeAllMetadata(this);
	}
}
