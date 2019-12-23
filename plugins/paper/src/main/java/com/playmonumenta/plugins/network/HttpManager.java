package com.playmonumenta.plugins.network;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.bukkit.plugin.Plugin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HttpManager {
	private HttpServer mServer;
	private static long mLastTickTime = 0;
	private final Plugin mPlugin;

	public HttpManager(Plugin plugin) {
		mPlugin = plugin;
	}

	/* TODO: Someday move this port to a config file */
	public void start() throws IOException {
		mServer = HttpServer.create(new InetSocketAddress(8000), 0);
		mServer.createContext("/ready", new ReadyHandler());
		mServer.createContext("/alive", new AliveHandler());
		mServer.setExecutor(null);
		mServer.start();
		mPlugin.getLogger().info("HTTP server listening on port 8000 for /alive and /ready");
	}

	public void stop() {
		if (mServer != null) {
			mServer.stop(0);
		}
	}

	public void tick() {
		mLastTickTime = System.currentTimeMillis();
	}

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

	private static class AliveHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange request) throws IOException {
			final byte[] response;
			if (System.currentTimeMillis() - mLastTickTime < 10000) {
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
