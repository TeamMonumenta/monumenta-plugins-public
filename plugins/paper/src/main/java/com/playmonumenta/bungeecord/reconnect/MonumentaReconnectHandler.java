package com.playmonumenta.bungeecord.reconnect;

import com.playmonumenta.redissync.ConfigAPI;
import com.playmonumenta.redissync.RedisAPI;
import io.lettuce.core.api.async.RedisAsyncCommands;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ReconnectHandler;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class MonumentaReconnectHandler implements ReconnectHandler {

	private final @Nullable String mDefaultServer;
	private final RedisAsyncCommands<String, String> mAPI = RedisAPI.getInstance().async();

	public MonumentaReconnectHandler(@Nullable String defaultServer) {
		mDefaultServer = defaultServer;
	}

	@Override
	public ServerInfo getServer(ProxiedPlayer player) {
		ServerInfo server = null;

		String storedServerName = null;
		try {
			storedServerName = mAPI.hget(locationsKey(), player.getUniqueId().toString()).get(8, TimeUnit.SECONDS);
		} catch (Exception ex) {
			ProxyServer.getInstance().getLogger().log(Level.WARNING, "Exception while getting player location for '" + player.getName() + "': " + ex.getMessage());
			ex.printStackTrace();
		}

		if (storedServerName == null) {
			/* Player has never connected before */
			if (mDefaultServer != null) {
				/* default server */
				server = ProxyServer.getInstance().getServerInfo(mDefaultServer);
			}
			/*
			 * If mDefaultServer is null, no default specified - let
			 * bungee handle this based on its own config file
			 */
		} else {
			/* Player has connected before */
			server = ProxyServer.getInstance().getServerInfo(storedServerName);
			if (server == null) {
				ProxyServer.getInstance().getLogger().log(Level.WARNING,
						"Failed to connect player '" + player.getName() + "' to last server '" + storedServerName + "'");
			}
		}

		return server;
	}

	@Override
	public void setServer(ProxiedPlayer player) {
		String reconnectServer = (player.getReconnectServer() != null) ? player.getReconnectServer().getName() : player.getServer().getInfo().getName();
		mAPI.hset(locationsKey(), player.getUniqueId().toString(), reconnectServer);
	}

	private String locationsKey() {
		return String.format("%s:bungee:locations", ConfigAPI.getServerDomain());
	}

	@Override
	public void close() {
	}

	@Override
	public void save() {
	}
}
