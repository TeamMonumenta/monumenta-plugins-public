package com.playmonumenta.velocity.commands;

import com.playmonumenta.velocity.voting.VoteManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

public class Vote implements SimpleCommand {
	private final VoteManager mVoteManager;

	public Vote(VoteManager voteManager) {
		mVoteManager = voteManager;
	}

	@Override
	public void execute(final Invocation invocation) {
		CommandSource sender = invocation.source();
		// Get the arguments after the command alias
		if (sender instanceof Player player) {
			mVoteManager.onVoteCmd(player);
		}
	}

	// This method allows you to control who can execute the command.
	// If the executor does not have the required permission,
	// the execution of the command and the control of its autocompletion
	// will be sent directly to the server on which the sender is located
	@Override
	public boolean hasPermission(final Invocation invocation) {
		return invocation.source().hasPermission("monumenta.command.vote");
	}
}
