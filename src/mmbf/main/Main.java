package mmbf.main;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;


public class Main extends JavaPlugin
{
	@Override
    public void onEnable() {
		Bukkit.getConsoleSender().sendMessage("[Monumenta_bossfights] Plugin enabled!");
		getCommand("bossfight").setExecutor(new BossFight(this));
		getCommand("mobspell").setExecutor(new MobSpell(this));
    }
   
    @Override
    public void onDisable() {
       
    }
}
