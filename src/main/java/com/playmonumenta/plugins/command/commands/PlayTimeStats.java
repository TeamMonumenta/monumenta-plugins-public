package com.playmonumenta.plugins.command.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.playmonumenta.plugins.command.AbstractCommand;
import com.playmonumenta.plugins.command.CommandContext;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PlayTimeStats extends AbstractCommand {
	private World mWorld;

	public PlayTimeStats(Plugin plugin, World world) {
		super(
		    "playTimeStats",
		    "Get the current playtime stats of everyone online",
		    plugin
		);
		mWorld = world;
	}

	@Override
	protected void configure(ArgumentParser parser) {
	}

	@Override
	protected boolean run(CommandContext context) {
		List<Player> players = new ArrayList<>(mWorld.getPlayers());

		Map<String, Integer> playTime = new HashMap<>();
		for (Player player : players) {
			playTime.put(player.getDisplayName(), player.getStatistic(Statistic.PLAY_ONE_TICK));
		}

		List<Entry<String, Integer>> playerList = new ArrayList<>(playTime.entrySet());
		playerList.sort((obj1, obj2) -> obj2.getValue().compareTo(obj1.getValue()));

		for (int i = 0; i < playerList.size(); i++) {
			Entry<String, Integer> entry = playerList.get(i);

			int grandTotal = entry.getValue();
			int totalTicks = grandTotal;

			int days = _ticksToDays(totalTicks);
			totalTicks -= _daysToTicks(days);

			int hours = _ticksToHours(totalTicks);
			totalTicks -= _hoursToTicks(hours);

			int minutes = _ticksToMinutes(totalTicks);
			totalTicks -= _minutesToTicks(minutes);

			int seconds = _ticksToSeconds(totalTicks);

			sendMessage(context, ChatColor.GREEN + "[" + (i + 1) + "] " + entry.getKey() + ": Total Play Time - " + days + " days " + hours + " hours " + minutes + " minutes " + seconds + " seconds" + " (Total Ticks: " + grandTotal + ")");
		}

		return true;
	}

	private int _ticksToDays(int ticks) {
		return ((((ticks / 20) / 60) / 60) / 24);
	}

	private int _daysToTicks(int days) {
		return days * 24 * 60 * 60  * 20;
	}

	private int _ticksToHours(int ticks) {
		return (((ticks / 20) / 60) / 60);
	}

	private int _hoursToTicks(int hours) {
		return hours * 60 * 60  * 20;
	}

	private int _ticksToMinutes(int ticks) {
		return ((ticks / 20) / 60);
	}

	private int _minutesToTicks(int minutes) {
		return minutes * 60 * 20;
	}

	private int _ticksToSeconds(int ticks) {
		return (ticks / 20);
	}
}
