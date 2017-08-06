package bungee.project;

import net.md_5.bungee.api.plugin.PluginManager;

import bungee.project.listeners.EventListener;
import net.md_5.bungee.api.plugin.Plugin;

public class Main extends Plugin {
	@Override
    public void onEnable() {
		PluginManager manager = getProxy().getPluginManager();
        
		manager.registerListener(this, new EventListener(this));
    }
}
