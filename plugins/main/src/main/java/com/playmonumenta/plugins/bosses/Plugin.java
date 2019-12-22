package com.playmonumenta.plugins.bosses;

import org.bukkit.plugin.java.JavaPlugin;

import com.playmonumenta.plugins.bosses.commands.BossFight;
import com.playmonumenta.plugins.bosses.spells.SpellDetectionCircle;
import com.playmonumenta.plugins.bosses.utils.MetadataUtils;

public class Plugin extends JavaPlugin {
	public BossManager mBossManager;

	@Override
	public void onLoad() {
		/*
		 * CommandAPI commands which register directly and are usable in functions
		 *
		 * These need to register immediately on load to prevent function loading errors
		 */

		BossFight.register(this);

		/*
		 * Register spells that can be run by themselves
		 * By convention, these are always like: /mobspell <spell_label> <args>
		 */
		SpellDetectionCircle.registerCommand(this);
	}

	@Override
	public void onEnable() {
		mBossManager = new BossManager(this);
		getServer().getPluginManager().registerEvents(mBossManager, this);
	}

	@Override
	public void onDisable() {
		mBossManager.unloadAll();

		getServer().getScheduler().cancelTasks(this);

		MetadataUtils.removeAllMetadata(this);
	}
}
