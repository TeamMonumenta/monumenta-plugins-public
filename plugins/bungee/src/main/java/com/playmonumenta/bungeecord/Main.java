package com.playmonumenta.bungeecord;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import com.playmonumenta.bungeecord.listeners.EventListener;
import com.playmonumenta.bungeecord.network.SocketManager;
import com.playmonumenta.bungeecord.reconnect.MonumentaReconnectHandler;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class Main extends Plugin {
	private Configuration mConfig = null;
	private int mSocketPort = 0;
	private Level mLogLevel = Level.INFO;

	public String mDefaultServer = null;

	@Override
	public void onEnable() {
		_loadConfig();
		_saveConfig();

		PluginManager manager = getProxy().getPluginManager();
		SocketManager socketManager = new SocketManager(this, mSocketPort);
		socketManager.start();

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

			// load socket port
			mSocketPort = mConfig.getInt("socket_port", 0);

			// Load default server
			String level = mConfig.getString("log_level", "INFO").toLowerCase();
			switch (level) {
				case "finest":
					mLogLevel = Level.FINEST;
					break;
				case "finer":
					mLogLevel = Level.FINER;
					break;
				case "fine":
					mLogLevel = Level.FINE;
					break;
				default:
					mLogLevel = Level.INFO;
			}
			this.getLogger().setLevel(mLogLevel);
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

			mConfig.set("socket_port", mSocketPort);

			if (mLogLevel.equals(Level.FINEST)) {
				mConfig.set("log_level", "FINEST");
			} else if (mLogLevel.equals(Level.FINER)) {
				mConfig.set("log_level", "FINER");
			} else if (mLogLevel.equals(Level.FINE)) {
				mConfig.set("log_level", "FINE");
			} else {
				mConfig.set("log_level", "INFO");
			}

			try {
				ConfigurationProvider.getProvider(YamlConfiguration.class).save(mConfig, new File(getDataFolder(), "config.yml"));
			} catch (IOException ex) {
				getLogger().log(Level.WARNING, "Could not save config.yml", ex);
			}
		}
	}
}
