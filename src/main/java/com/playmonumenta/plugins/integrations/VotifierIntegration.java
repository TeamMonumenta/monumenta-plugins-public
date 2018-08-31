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

		//TODO: Probably want to make this less annoying...
		if (ScoreboardUtils.getScoreboardValue(player, "VoteRaffle") > 0) {
			player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You have won the weekly vote raffle! Congratulations!");
			// TODO: Replace placeholder
			player.sendMessage(ChatColor.GOLD + "See the PLACEHOLDER NPC in Sierhaven for a special reward");
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
    public void onVotifierEvent(VotifierEvent event) {
        Vote vote = event.getVote();

		//TODO: Remove this eventually
		mPlugin.getLogger().log(Level.FINE, "Got vote: " + vote.toString());
		//TODO: Forward this message if on the build shard

		//  Loop through all online players and find the matching one
		//  and send them a message
		for (Player player : mPlugin.getServer().getOnlinePlayers()) {
			if (player.getName().equals(vote.getUsername())) {
				int votesWeekly = ScoreboardUtils.getScoreboardValue(player, "VotesWeekly") + 1;
				int votesTotal = ScoreboardUtils.getScoreboardValue(player, "VotesTotal") + 1;
				int voteRewards = ScoreboardUtils.getScoreboardValue(player, "VoteRewards") + 1;
				ScoreboardUtils.setScoreboardValue(player, "VotesWeekly", votesWeekly);
				ScoreboardUtils.setScoreboardValue(player, "VotesTotal", votesTotal);
				ScoreboardUtils.setScoreboardValue(player, "VoteRewards", voteRewards);

				player.sendMessage(ChatColor.GOLD + "Thanks for voting at " + vote.getServiceName() + "!");
				player.sendMessage(ChatColor.GOLD +
				                   "You have " + Integer.toString(votesWeekly) + " vote" + (votesWeekly == 1 ? "" : "s") +
				                   " this week, " + Integer.toString(votesTotal) + " lifetime vote" + (votesTotal == 1 ? "" : "s") +
								   " and " + Integer.toString(voteRewards) + " unclaimed reward" + (voteRewards == 1 ? "" : "s"));
				break;
			}
		}
    }
}
