package mmbf.main;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import pe.bossfights.BossManager;
import pe.bossfights.utils.MetadataUtils;

public class Main extends JavaPlugin
{
	@Override
	public void onEnable()
	{
		//TODO: Iterate loaded entities and refresh their boss status

		Bukkit.getConsoleSender().sendMessage("[Monumenta_bossfights] Plugin enabled!");

		BossManager bossManager = new BossManager(this);
		getServer().getPluginManager().registerEvents(bossManager, this);

		getCommand("mobspell").setExecutor(new MobSpell(this));
		getCommand("bossfight").setExecutor(bossManager);
	}

	@Override
	public void onDisable()
	{
		//TODO: Iterate and serialize all loaded bosses and cancel the fights

		getServer().getScheduler().cancelTasks(this);

		MetadataUtils.removeAllMetadata(this);
	}
}
