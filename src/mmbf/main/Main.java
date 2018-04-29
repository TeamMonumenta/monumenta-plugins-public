package mmbf.main;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import pe.bossfights.BossManager;

public class Main extends JavaPlugin
{
	@Override
	public void onEnable()
	{
		Bukkit.getConsoleSender().sendMessage("[Monumenta_bossfights] Plugin enabled!");

		BossManager bossManager = new BossManager(this);
		getServer().getPluginManager().registerEvents(bossManager, this);

		getCommand("mobspell").setExecutor(new MobSpell(this));
		getCommand("bossfight").setExecutor(bossManager);
	}

	@Override
	public void onDisable()
	{

	}
}
