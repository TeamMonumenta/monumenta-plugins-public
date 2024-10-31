package com.playmonumenta.bungeecord;

import com.google.common.base.Ascii;
import com.playmonumenta.bungeecord.commands.Proxy;
import com.playmonumenta.bungeecord.commands.Vote;
import com.playmonumenta.bungeecord.integrations.NetworkRelayIntegration;
import com.playmonumenta.bungeecord.listeners.EventListener;
import com.playmonumenta.bungeecord.network.BungeeClientModHandler;
import com.playmonumenta.bungeecord.reconnect.MonumentaReconnectHandler;
import com.playmonumenta.bungeecord.voting.VoteManager;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

public class Main extends Plugin {
	private @Nullable Configuration mConfig = null;
	private Level mLogLevel = Level.INFO;
	private @Nullable VoteManager mVoteManager = null;

	public @Nullable String mDefaultServer = null;
	public boolean mJoinMessagesEnabled = true;

	public boolean mAllowPacketPublicizeContent = false;

	@Override
	public void onEnable() {
		loadConfig();
		saveConfig();

		PluginManager manager = getProxy().getPluginManager();

		// Hook into Monumenta Network Relay for message brokering if available
		if (manager.getPlugin("MonumentaNetworkRelay") != null) {
			manager.registerListener(this, new NetworkRelayIntegration(this.getLogger()));
			manager.registerCommand(this, new Proxy());
		}

		if (mConfig == null || !mConfig.contains("voting")) {
			getLogger().warning("No 'voting' section in config file - disabling voting features");
		} else {
			try {
				mVoteManager = new VoteManager(this, mConfig.getSection("voting"));
				manager.registerCommand(this, new Vote(mVoteManager));
				manager.registerListener(this, mVoteManager);
			} catch (IllegalArgumentException ex) {
				getLogger().log(Level.WARNING, "Failed to initialize voting system:", ex);
			}
		}

		manager.registerListener(this, new EventListener(this));

		//does not have listeners currently, but still needs to be initialized.
		new BungeeClientModHandler(mAllowPacketPublicizeContent, getLogger());

		getProxy().setReconnectHandler(new MonumentaReconnectHandler(mDefaultServer));
	}

	@Override
	public void onDisable() {
		if (mVoteManager != null) {
			mVoteManager.unload();
			mVoteManager = null;
		}
	}

	private void loadConfig() {
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

			// Load join_messages_enabled
			mJoinMessagesEnabled = mConfig.getBoolean("join_messages_enabled", mJoinMessagesEnabled);

			// Load default server
			String level = Ascii.toLowerCase(mConfig.getString("log_level", "INFO"));
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

			mAllowPacketPublicizeContent = mConfig.getBoolean("allow_packets_publicize_content");
		} catch (IOException ex) {
			getLogger().log(Level.WARNING, "Could not load config.yml", ex);
		}
	}

	private void saveConfig() {
		if (!getDataFolder().exists()) {
			getDataFolder().mkdir();
		}

		if (mConfig != null) {
			if (mDefaultServer == null) {
				mConfig.set("default_server", "null");
			} else {
				mConfig.set("default_server", mDefaultServer);
			}
			mConfig.set("join_messages_enabled", mJoinMessagesEnabled);

			if (mLogLevel.equals(Level.FINEST)) {
				mConfig.set("log_level", "FINEST");
			} else if (mLogLevel.equals(Level.FINER)) {
				mConfig.set("log_level", "FINER");
			} else if (mLogLevel.equals(Level.FINE)) {
				mConfig.set("log_level", "FINE");
			} else {
				mConfig.set("log_level", "INFO");
			}

			mConfig.set("allow_packets_publicize_content", true);

			try {
				ConfigurationProvider.getProvider(YamlConfiguration.class).save(mConfig, new File(getDataFolder(), "config.yml"));
			} catch (IOException ex) {
				getLogger().log(Level.WARNING, "Could not save config.yml", ex);
			}
		}
	}
}
