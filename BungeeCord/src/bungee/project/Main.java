package bungee.project;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import bungee.project.listeners.EventListener;
import bungee.project.reconnect.MonumentaReconnectHandler;

public class Main extends Plugin {
	private Configuration mConfig = null;

	public String mDefaultServer = null;

	@Override
    public void onEnable() {
		_loadConfig();
		_saveConfig();

		PluginManager manager = getProxy().getPluginManager();

		manager.registerListener(this, new EventListener(this));

		getProxy().setReconnectHandler(new MonumentaReconnectHandler(mDefaultServer));
    }

	private void _loadConfig() {
		// Create data directory if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

		// Load config file
		try {
			File file = new File(getDataFolder(), "config.yml");

			if (!file.exists()) {
				file.createNewFile();
			}

			mConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);

			// Load default server
			mDefaultServer = mConfig.getString("default_server", "null");
			if (mDefaultServer.equals("null")) {
				mDefaultServer = null;
			}
		} catch (IOException ex) {
			getLogger().log(Level.WARNING, "Could not load config.yml", ex);
		}
	}

	private void _saveConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

		if (mConfig != null) {
			if (mDefaultServer == null) {
				mConfig.set("default_server", "null");
			} else {
				mConfig.set("default_server", mDefaultServer);
			}

			try {
				ConfigurationProvider.getProvider(YamlConfiguration.class).save(mConfig, new File(getDataFolder(), "config.yml"));
			} catch (IOException ex) {
				getLogger().log(Level.WARNING, "Could not save config.yml", ex);
			}
		}
	}
}
