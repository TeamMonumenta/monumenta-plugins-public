package com.playmonumenta.nms.utils;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_13_R2.CraftServer;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;

import net.minecraft.server.v1_13_R2.CommandListenerWrapper;
import net.minecraft.server.v1_13_R2.MinecraftServer;

public class NmsCommandUtils {
	public static class ParsedCommandWrapper {
		private ParseResults<CommandListenerWrapper> mParse;

		protected ParsedCommandWrapper(ParseResults<CommandListenerWrapper> parse) {
			mParse = parse;
		}

		protected ParseResults<CommandListenerWrapper> getParseResults() {
			return mParse;
		}
	}

	public static ParsedCommandWrapper parseCommand(String cmd) throws Exception {
		//Need to make sure command does not have the / at the start - this breaks the Parser
		if(cmd.charAt(0) == '/') {
			cmd = cmd.substring(1);
		}

		ParseResults<CommandListenerWrapper> pr = null;

		MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
		net.minecraft.server.v1_13_R2.CommandDispatcher dispatcher = server.getCommandDispatcher();
		CommandDispatcher<CommandListenerWrapper> brigadierDispatcher = dispatcher.a();

		pr = brigadierDispatcher.parse(cmd, server.getServerCommandListener());

		if (pr == null) {
			throw new Exception("ParseResults are null");
		}

		return new ParsedCommandWrapper(pr);
	}

	public static void runParsedCommand(ParsedCommandWrapper parsed) throws Exception {
		ParseResults<CommandListenerWrapper> pr = parsed.getParseResults();

		MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
		net.minecraft.server.v1_13_R2.CommandDispatcher dispatcher = server.getCommandDispatcher();
		CommandDispatcher<CommandListenerWrapper> brigadierDispatcher = dispatcher.a();

		brigadierDispatcher.execute(pr);
	}

	public static void runParsedCommand(ParsedCommandWrapper parsed, Player player) throws Exception {
		CommandListenerWrapper playerContext = ((CraftPlayer) player).getHandle().getCommandListener();

		ParseResults<CommandListenerWrapper> pr = parsed.getParseResults();

		MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
		net.minecraft.server.v1_13_R2.CommandDispatcher dispatcher = server.getCommandDispatcher();
		CommandDispatcher<CommandListenerWrapper> brigadierDispatcher = dispatcher.a();

		brigadierDispatcher.execute(new ParseResults<CommandListenerWrapper>(pr.getContext().withSource(playerContext), pr.getStartIndex(), pr.getReader(), pr.getExceptions()));
	}
}
