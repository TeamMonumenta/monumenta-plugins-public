package com.playmonumenta.plugins.network;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HttpManager {
	/* Must process a tick within this many milliseconds to be considered alive */
	private static final int ALIVE_TICK_THRESHOLD = 15000;

	private HttpServer mServer;
	private static long mLastTickTime = 0;
	private final Plugin mPlugin;

	public HttpManager(Plugin plugin) throws IOException {
		mPlugin = plugin;

		int port = ServerProperties.getHTTPStatusPort();

		mServer = HttpServer.create(new InetSocketAddress(port), -1);
		mServer.createContext("/ready", new ReadyHandler());
		mServer.createContext("/alive", new AliveHandler());
		mServer.setExecutor(null);
		mServer.start();

		mPlugin.getLogger().info("HTTP server listening on port " + port + " for /alive and /ready");
	}

	public void start() {
		/*
		 * Update the last tick time every tick so we can tell when
		 * the server stops processing ticks
		 */
		new BukkitRunnable() {
			@Override
			public void run() {
				mLastTickTime = System.currentTimeMillis();
			}
		}.runTaskTimer(mPlugin, 1, 1);
	}

	public void stop() {
		if (mServer != null) {
			mServer.stop(0);
		}
	}

	/*
	 * /ready indicates if the server has ever started processing ticks
	 */
	private static class ReadyHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange request) throws IOException {
			final byte[] response;
			if (mLastTickTime > 0) {
				response = "Ready!".getBytes();
				request.sendResponseHeaders(200, response.length);
			} else {
				response = "No ticks processed yet".getBytes();
				request.sendResponseHeaders(503, response.length);
			}
			OutputStream stream = request.getResponseBody();
			stream.write(response);
			stream.close();
		}
	}

	/*
	 * /alive indicates if the server has processed any ticks recently
	 */
	private static class AliveHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange request) throws IOException {
			final byte[] response;
			if (System.currentTimeMillis() - mLastTickTime < ALIVE_TICK_THRESHOLD) {
				response = "Alive!".getBytes();
				request.sendResponseHeaders(200, response.length);
			} else {
				response = "Server not ticking - possibly hung".getBytes();
				request.sendResponseHeaders(503, response.length);
			}
			OutputStream stream = request.getResponseBody();
			stream.write(response);
			stream.close();
		}
	}
}
