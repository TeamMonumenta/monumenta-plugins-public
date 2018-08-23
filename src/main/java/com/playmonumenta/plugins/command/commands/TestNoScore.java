package com.playmonumenta.plugins.command.commands;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.Material;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import com.playmonumenta.plugins.utils.CommandUtils;

public class TestNoScore implements CommandExecutor {
	/*
	 * These are used to cache the result of a scan. The apartments call this
	 * 200 times on the same tick - so it makes sense to re-use the result of the
	 * first invocation to avoid iterating through the entire scoreboard 200 times
	 *
	 * mScoreCache stores the set of values, and mScoreTicks stores the tick
	 * that mScoreCache was last updated on.
	 */
	HashMap<String,Set<Integer>> mScoreCache = new HashMap<String,Set<Integer>>();
	HashMap<String,Long> mScoreTicks = new HashMap<String,Long>();

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length != 2) {
			sender.sendMessage(ChatColor.RED + "Invalid number of parameters!");
			return false;
		}

		String scoreName = arg3[0];
		int scoreVal;
		try {
			scoreVal = CommandUtils.parseIntFromString(sender, arg3[1]);
		} catch (Exception e) {
			return false;
		}

		Set<Integer> allScores = getScores(sender, scoreName);
		if (allScores == null || allScores.contains(scoreVal)) {
			return false;
		}

		// Nothing on the scoreboard has this score for this objective

		/*
		 * Due to a ridiculous spigot limitation, returning false is not enough
		 * to prevent this command from "succeeding". So instead if this command is run by
		 * a command block (expected behavior), the block underneath it is set to redstone
		 * on success.
		 */
		if(sender instanceof BlockCommandSender) {
			((BlockCommandSender)sender).getBlock().getLocation().add(0, -1, 0).getBlock().setType(Material.REDSTONE_BLOCK);
		}

		return true;
	}

	private Set<Integer> getScores(CommandSender sender, String scoreName) {
		Long cachedTime = mScoreTicks.get(scoreName);
		if ((cachedTime != null) && ((System.currentTimeMillis() - cachedTime) < 1000)) {
			// Already ran this command this second for this objective - re-use the result
			return mScoreCache.get(scoreName);
		}

		// Need to iterate over all things on the scoreboard
		Set<Integer> allScores = new TreeSet<Integer>();
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		Objective objective = scoreboard.getObjective(scoreName);
		if (objective == null) {
			sender.sendMessage(ChatColor.RED + "Scoreboard '" + scoreName + "' does not exist!");
			return null;
		}

		for (String entry : scoreboard.getEntries()) {
			allScores.add(objective.getScore(entry).getScore());
		}

		mScoreCache.put(scoreName, allScores);
		mScoreTicks.put(scoreName, System.currentTimeMillis());

		return allScores;
	}
}
