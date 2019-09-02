package com.playmonumenta.nms.utils;

import org.bukkit.entity.Player;

import com.mojang.brigadier.ParseResults;

public class NmsCommandUtils {
	public static class ParsedCommandWrapper {
		private ParseResults<?> mParse;

		protected ParsedCommandWrapper(ParseResults<?> parse) {
			mParse = parse;
		}

		protected ParseResults<?> getParseResults() {
			return mParse;
		}
	}

	public static ParsedCommandWrapper parseCommand(String cmd) throws Exception {
		return new ParsedCommandWrapper(null);
	}

	public static void runParsedCommand(ParsedCommandWrapper parsed) throws Exception {

	}

	public static void runParsedCommand(ParsedCommandWrapper parsed, Player player) throws Exception {

	}
}
