package com.playmonumenta.velocity.handlers;

import com.playmonumenta.redissync.ConfigAPI;
import com.playmonumenta.redissync.RedisAPI;
import com.playmonumenta.velocity.MonumentaVelocity;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

public class MonumentaReconnectHandler {
	private final MonumentaVelocity mPlugin;
	private final @Nullable String mDefaultServer;
	private final List<String> mExcludedServers = new ArrayList<>();

	public MonumentaReconnectHandler(MonumentaVelocity plugin) {
		mPlugin = plugin;
		mDefaultServer = mPlugin.mConfig.mDefaultServer;
		mExcludedServers.addAll(mPlugin.mConfig.mExcludedServers);
	}

	public @Nullable RegisteredServer getFallbackServer(Player player) {
		@Nullable String storedServerName = null;
		try {
			storedServerName = RedisAPI.getInstance().async().hget(locationsKey(), player.getUniqueId().toString()).get(6, TimeUnit.SECONDS);
		} catch (Exception ex) {
			mPlugin.mLogger.warn("Exception while getting player location for '" + player.getUsername() + "': " + ex.getMessage(), ex);
		}
		if (storedServerName == null) {
			return null;
		}
		return mPlugin.mServer.getServer(storedServerName).orElse(null);
	}

	public boolean isExcluded(String server) {
		return mExcludedServers.contains(server);
	}

	// ReconnectHandler.getServer
	@Subscribe(order = PostOrder.LATE)
	public void playerChooseInitialServerEvent(PlayerChooseInitialServerEvent event) {
		Player player = event.getPlayer();
		@Nullable RegisteredServer server = event.getInitialServer().orElse(null);
		@Nullable String storedServerName = null;

		try {
			storedServerName = RedisAPI.getInstance().async().hget(locationsKey(), player.getUniqueId().toString()).get(6, TimeUnit.SECONDS);
		} catch (Exception ex) {
			mPlugin.mLogger.warn("Exception while getting player location for '" + player.getUsername() + "': " + ex.getMessage(), ex);
		}

		if (storedServerName == null) {
			/* Player has never connected before */
			if (mDefaultServer != null && !mDefaultServer.isEmpty()) {
				/* default server */
				server = mPlugin.mServer.getServer(mDefaultServer).orElse(null);
				storedServerName = mDefaultServer;
			}
			/*
			 * If mDefaultServer is empty, no default specified - let
			 * bungee handle this based on its own config file
			 */
		} else {
			/* Player has connected before */
			server = mPlugin.mServer.getServer(storedServerName).orElse(null);
		}

		if (server != null) {
			event.setInitialServer(server);
		} else {
			player.sendMessage(Component.text("Failed to send you to '" + storedServerName + "'. Server was not found!", NamedTextColor.RED));
			mPlugin.mLogger.warn("Failed to connect player '" + player.getUsername() + "' to last server '" + storedServerName + "' Server was not found!");
		}
	}

	// Fix for kicks from the server
	@Subscribe(order = PostOrder.EARLY)
	public void kickedFromServerEvent(KickedFromServerEvent event) {
		@Nullable Component kickReason = event.getServerKickReason().orElse(null);
		// exclude servers such as purgatory
		if (!event.kickedDuringServerConnect() && kickReason != null &&
			// We assume that if there is a message component inside RedirectPlayer, it is being done by a proxy plugin
			event.getResult() instanceof KickedFromServerEvent.RedirectPlayer redirectResult && redirectResult.getMessageComponent() == null) {
			event.setResult(KickedFromServerEvent.DisconnectPlayer.create(kickReason));
		}
	}

	// ReconnectHandler.setServer
	@Subscribe
	public void serverConnectEvent(ServerPostConnectEvent event) {
		Player player = event.getPlayer();
		ServerConnection server = player.getCurrentServer().orElse(null);
		if (server == null) {
			return;
		}
		String reconnectServer = server.getServerInfo().getName();
		// exclude servers such as purgatory
		if (reconnectServer == null || mExcludedServers.contains(reconnectServer)) {
			return;
		}
		RedisAPI.getInstance().async().hset(locationsKey(), player.getUniqueId().toString(), reconnectServer);
	}

	private String locationsKey() {
		return String.format("%s:bungee:locations", ConfigAPI.getServerDomain());
	}
}
