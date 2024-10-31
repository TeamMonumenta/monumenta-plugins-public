package com.playmonumenta.velocity;

import com.google.inject.Inject;
import com.playmonumenta.velocity.commands.Rejoin;
import com.playmonumenta.velocity.commands.Vote;
import com.playmonumenta.velocity.handlers.JoinLeaveHandler;
import com.playmonumenta.velocity.handlers.MonumentaReconnectHandler;
import com.playmonumenta.velocity.integrations.LuckPermsIntegration;
import com.playmonumenta.velocity.integrations.NetworkRelayIntegration;
import com.playmonumenta.velocity.integrations.PremiumVanishIntegration;
import com.playmonumenta.velocity.network.VelocityClientModHandler;
import com.playmonumenta.velocity.voting.VoteManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ListenerCloseEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

@Plugin(
	id = "monumenta-velocity",
	name = "Monumenta-Velocity",
	version = "",
	url = "",
	description = "",
	authors = {""},
	dependencies = {
		@Dependency(id = "luckperms", optional = true),
		@Dependency(id = "premiumvanish", optional = true),
		@Dependency(id = "monumenta-network-relay", optional = true),
		@Dependency(id = "monumenta-redisapi", optional = true),
		@Dependency(id = "nuvotifier", optional = true)
	}
)
public class MonumentaVelocity {
	public final ProxyServer mServer;
	public final Logger mLogger;
	public boolean mLoaded = false;
	private final YamlConfigurationLoader mLoader; // Config reader & writer
	private @Nullable CommentedConfigurationNode mBaseConfig; // backing config node for the class
	public MonumentaVelocityConfiguration mConfig = new MonumentaVelocityConfiguration(); // class with actual data

	private @Nullable VoteManager mVoteManager = null;
	private @Nullable MonumentaReconnectHandler mReconnectHandler = null;

	@Inject
	public MonumentaVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
		this.mServer = server;
		this.mLogger = logger;

		this.mLoader = YamlConfigurationLoader.builder()
			.path(dataDirectory.resolve(Path.of("config.yaml"))) // Set where we will load and save to
			.nodeStyle(NodeStyle.BLOCK)
			.build();

		loadConfig();
		saveConfig();
	}

	@Subscribe
	public void proxyInitalizeEvent(ProxyInitializeEvent event) {
		mLoaded = true;
		PluginManager plugins = mServer.getPluginManager();

		if (plugins.isLoaded("premiumvanish")) {
			PremiumVanishIntegration.enable();
		}

		if (plugins.isLoaded("luckperms")) {
			new LuckPermsIntegration();
		}

		if (plugins.isLoaded("monumenta-redisapi")) {
			mReconnectHandler = new MonumentaReconnectHandler(this);
			mServer.getEventManager().register(this, mReconnectHandler);
			mServer.getCommandManager().register("rejoin", new Rejoin(mReconnectHandler));
		}

		if (plugins.isLoaded("monumenta-network-relay")) {
			mServer.getEventManager().register(this, new NetworkRelayIntegration(this));
		}

		if (plugins.isLoaded("nuvotifier")) {
			try {
				mVoteManager = new VoteManager(this, mConfig);
				mServer.getCommandManager().register("vote", new Vote(mVoteManager));
				mServer.getEventManager().register(this, mVoteManager);
			} catch (IllegalArgumentException ex) {
				mLogger.warn("Failed to initialize voting system:", ex);
			}
		}

		mServer.getEventManager().register(this, new JoinLeaveHandler(this));

		new VelocityClientModHandler(this, mConfig.mAllowPacketPublicizeContent);
		mServer.getChannelRegistrar().register(VelocityClientModHandler.CHANNEL_ID);
	}

	@Subscribe
	public void listenerCloseEvent(ListenerCloseEvent event) {
		mLoaded = false;
	}

	private void loadConfig() {
		try {
			// attempt to load from default
			mBaseConfig = mLoader.load(); // Load from file
			MonumentaVelocityConfiguration temp = mBaseConfig.get(MonumentaVelocityConfiguration.class); // Populate object
			if (temp != null) {
				mConfig = temp;
			}
		} catch (ConfigurateException ex) {
			mLogger.warn("Could not load config.yaml", ex);
		}
	}

	private void saveConfig() {
		if (mBaseConfig == null || mConfig == null) {
			mLogger.warn("Tried to save current config but config is null!");
			return;
		}
		try {
			mBaseConfig.set(MonumentaVelocityConfiguration.class, mConfig); // Update the backing node
			mLoader.save(mBaseConfig); // Write to the original file
		} catch (ConfigurateException ex) {
			mLogger.warn("Could not save config.yaml", ex);
		}
	}

	@ConfigSerializable
	public static class MonumentaVelocityConfiguration {
		@Setting(value = "default_server")
		public String mDefaultServer = "";

		@Setting(value = "join_messages_enabled")
		public boolean mJoinMessagesEnabled = true;

		@Setting(value = "voting")
		public MonumentaVoting mVoting = new MonumentaVoting();

		@Setting(value = "excluded_servers")
		public List<String> mExcludedServers = new ArrayList<>();

		@Setting(value = "max_player_count")
		public int mMaxPlayerCount = 400;

		@Setting(value = "version_string")
		public String mVersionString = "Monumenta 1.19.4-1.20.2";

		@Setting(value = "allow_packets_publicize_content")
		public Boolean mAllowPacketPublicizeContent = false;
	}

	@ConfigSerializable
	public static class MonumentaVoting {
		@Setting(value = "sites")
		public List<String> mUrls = new ArrayList<String>();
		@Setting(value = "alternate_names")
		public List<String> mAlternateNames = new ArrayList<String>();
		@Setting(value = "cooldown_minutes")
		public List<Integer> mCooldownMinutes = new ArrayList<Integer>();
	}

}
