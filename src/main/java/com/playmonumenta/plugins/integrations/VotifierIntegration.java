package com.playmonumenta.plugins.integrations;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class VotifierIntegration implements Listener {
	private Plugin mPlugin;

	public VotifierIntegration(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		if (ScoreboardUtils.getScoreboardValue(player, "VoteRaffle") > 0) {
			player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You have won the weekly vote raffle! Congratulations!");
			player.sendMessage(ChatColor.GOLD + "See Pollmaster Tennenbaum in Sierhaven for your reward");
		}

		int offlineVotes = ScoreboardUtils.getScoreboardValue(player, "VoteCache");
		if (offlineVotes > 0) {
			int votesWeekly = ScoreboardUtils.getScoreboardValue(player, "VotesWeekly");
			int votesTotal = ScoreboardUtils.getScoreboardValue(player, "VotesTotal");
			int voteRewards = ScoreboardUtils.getScoreboardValue(player, "VoteRewards");

			player.sendMessage(ChatColor.GOLD + "Thanks for voting for Monumenta! Your " + offlineVotes +
			                   " votes while you were away from King's Valley have been recorded.");
			player.sendMessage(ChatColor.GOLD +
							   "You have " + Integer.toString(votesWeekly) + " vote" + (votesWeekly == 1 ? "" : "s") +
							   " this week, " + Integer.toString(votesTotal) + " lifetime vote" + (votesTotal == 1 ? "" : "s") +
							   " and " + Integer.toString(voteRewards) + " unclaimed reward" + (voteRewards == 1 ? "" : "s"));
			ScoreboardUtils.setScoreboardValue(player, "VoteCache", 0);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
    public void onVotifierEvent(VotifierEvent event) {
        Vote vote = event.getVote();
		String playerName = vote.getUsername();

		mPlugin.getLogger().log(Level.FINE, "Got vote: " + vote.toString());

		// Give the player credit for voting
		int votesWeekly = ScoreboardUtils.getScoreboardValue(playerName, "VotesWeekly").orElse(0) + 1;
		int votesTotal = ScoreboardUtils.getScoreboardValue(playerName, "VotesTotal").orElse(0) + 1;
		int voteRewards = ScoreboardUtils.getScoreboardValue(playerName, "VoteRewards").orElse(0) + 1;
		ScoreboardUtils.setScoreboardValue(playerName, "VotesWeekly", votesWeekly);
		ScoreboardUtils.setScoreboardValue(playerName, "VotesTotal", votesTotal);
		ScoreboardUtils.setScoreboardValue(playerName, "VoteRewards", voteRewards);

		Player player = null;

		//  Loop through all online players and see if this player is online
		for (Player testPlayer : mPlugin.getServer().getOnlinePlayers()) {
			if (testPlayer.getName().equals(playerName)) {
				player = testPlayer;
				break;
			}
		}

		if (player != null) {
			// Player is online
			player.sendMessage(ChatColor.GOLD + "Thanks for voting at " + vote.getServiceName() + "!");
			player.sendMessage(ChatColor.GOLD +
							   "You have " + Integer.toString(votesWeekly) + " vote" + (votesWeekly == 1 ? "" : "s") +
							   " this week, " + Integer.toString(votesTotal) + " lifetime vote" + (votesTotal == 1 ? "" : "s") +
							   " and " + Integer.toString(voteRewards) + " unclaimed reward" + (voteRewards == 1 ? "" : "s"));
		} else {
			// Player is not online - keep track of how many votes they make while offline
			int voteCache = ScoreboardUtils.getScoreboardValue(playerName, "VoteCache").orElse(0) + 1;
			ScoreboardUtils.setScoreboardValue(playerName, "VoteCache", voteCache);
		}
    }
}
