package com.playmonumenta.bungeecord.commands;

import com.playmonumenta.bungeecord.voting.VoteManager;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class Vote extends Command {
	private final VoteManager mVoteManager;

	public Vote(VoteManager voteManager) {
		super("vote");
		mVoteManager = voteManager;
	}

	@Override
	public void execute(CommandSender commandSender, String[] strings) {
		if (commandSender instanceof ProxiedPlayer) {
			mVoteManager.onVoteCmd((ProxiedPlayer)commandSender);
		}
	}
}
