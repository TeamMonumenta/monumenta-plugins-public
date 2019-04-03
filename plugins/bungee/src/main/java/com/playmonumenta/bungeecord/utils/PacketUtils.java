package com.playmonumenta.bungeecord.utils;

import com.google.gson.Gson;

public class PacketUtils {
	/**
	 * Encodes string array to a single string
	 */
	public static String encodeStrings(String in[]) throws Exception {
		Gson gs = new Gson();
		return gs.toJson(in);
	}

	/**
	 * Decodes a stringified array of strings
	 */
	public static String[] decodeStrings(String encodedStr) throws Exception {
		Gson gs = new Gson();
		return gs.fromJson(encodedStr, String[].class);
	}
}
