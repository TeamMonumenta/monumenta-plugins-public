package com.playmonumenta.bungeecord.reconnect;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

import org.yaml.snakeyaml.Yaml;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ReconnectHandler;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class MonumentaReconnectHandler implements ReconnectHandler {

	private final Yaml yaml = new Yaml();
	private final File file = new File("locations.yml");
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	/*========================================================================*/
	private HashMap<String, String> data;
	private String mDefaultServer = null;

	@SuppressWarnings("unchecked")
	public MonumentaReconnectHandler(String defaultServer) {
		/* Note this might be null, which is ok */
		mDefaultServer = defaultServer;

		try {
			file.createNewFile();
			try (Reader rd = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
				Map<String, String> map = yaml.loadAs(rd, Map.class);
				if (map != null) {
					data = new HashMap<>(map);
				}
			}
		} catch (Exception ex) {
			/* TODO: Change this to a fatal error that prevents bungee from starting */
			file.renameTo(new File("locations.yml.old"));
			ProxyServer.getInstance().getLogger().log(Level.WARNING,
					"Could not load reconnect locations, resetting them");
		}

		if (data == null) {
			data = new HashMap<>();
		}
	}

	@Override
	public ServerInfo getServer(ProxiedPlayer player) {
		lock.readLock().lock();

		ServerInfo server = null;

		try {
			/* Upgrade the old key format to the new format */
			if (data.containsKey(oldKey(player))) {
				String oldServerName = data.get(oldKey(player));
				data.remove(oldKey(player));

				// Only upgrade the key if an upgraded key isn't already present
				if (!data.containsKey(key(player))) {
					data.put(key(player), oldServerName);
				}
			}

			if (!data.containsKey(key(player))) {
				if (mDefaultServer != null) {
					/* Player has never connected before - default server */
					server = ProxyServer.getInstance().getServerInfo(mDefaultServer);
				}
				/*
				 * If mDefaultServer is null, no default specified - let
				 * bungee handle this based on its own config file
				 */
			} else {
				/* Player has connected before */
				String storedServerName = data.get(key(player));

				server = ProxyServer.getInstance().getServerInfo(storedServerName);
				if (server == null) {
					ProxyServer.getInstance().getLogger().log(Level.WARNING,
							"Failed to connect player '" + player.getName() + "' to last server '" + storedServerName + "'");
				}
			}
		} finally {
			lock.readLock().unlock();
		}

		return server;
	}

	@Override
	public void setServer(ProxiedPlayer player) {
		lock.writeLock().lock();
		try {
			data.put(key(player), (player.getReconnectServer() != null) ? player.getReconnectServer().getName()
					 : player.getServer().getInfo().getName());
		} finally {
			lock.writeLock().unlock();
		}
	}

	private String key(ProxiedPlayer player) {
		return player.getUniqueId().toString();
	}

	private String oldKey(ProxiedPlayer player) {
		InetSocketAddress host = player.getPendingConnection().getVirtualHost();
		return player.getName() + ";" + host.getHostString() + ":" + host.getPort();
	}

	@Override
	public void save() {
		Map<String, String> copy = new HashMap<>();
		lock.readLock().lock();
		try {
			copy.putAll(data);
		} finally {
			lock.readLock().unlock();
		}

		try (Writer wr = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
			yaml.dump(copy, wr);
		} catch (IOException ex) {
			ProxyServer.getInstance().getLogger().log(Level.WARNING, "Could not save reconnect locations", ex);
		}
	}

	@Override
	public void close() {
	}
}

