package com.playmonumenta.bungeecord.commands;

import com.playmonumenta.bungeecord.integrations.NetworkRelayIntegration;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

public class Proxy extends Command {
	public Proxy() {
		super("proxy");
	}

	@Override
	public void execute(CommandSender commandSender, String[] strings) {
		commandSender.sendMessage(new ComponentBuilder(NetworkRelayIntegration.getProxyName()).create());
	}
}
