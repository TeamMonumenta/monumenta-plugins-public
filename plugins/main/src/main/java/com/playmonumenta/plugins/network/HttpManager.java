package com.playmonumenta.plugins.network;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HttpManager {
	HttpServer mServer;
	static long mLastTickTime = 0;
	public HttpManager() {}

	public void start() throws IOException {
		mServer = HttpServer.create(new InetSocketAddress(8000), 0);
		mServer.createContext("/ready", new ReadyHandler());
		mServer.createContext("/alive", new AliveHandler());
		mServer.setExecutor(null);
		mServer.start();
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
			if (mLastTickTime > 0) {
				byte[] response = "Ready!".getBytes();
				request.sendResponseHeaders(200, response.length);
				OutputStream stream = request.getResponseBody();
				stream.write(response);
				stream.close();
			}
		}
	}

	private static class AliveHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange request) throws IOException {
			if (System.currentTimeMillis() - mLastTickTime < 10000) {
				byte[] response = "Alive!".getBytes();
				request.sendResponseHeaders(200, response.length);
				OutputStream stream = request.getResponseBody();
				stream.write(response);
				stream.close();
			}
		}
	}
}