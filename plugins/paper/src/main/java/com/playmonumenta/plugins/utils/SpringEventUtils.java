package com.playmonumenta.plugins.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.redis.RedisManager;
import com.playmonumenta.plugins.utils.ItemUtils.ItemRegion;

import io.github.jorelali.commandapi.api.CommandAPI;

public class SpringEventUtils {
	public enum City {
		SIERHAVEN("sierhaven"),
		NYR("nyr"),
		FARR("farr"),
		LOWTIDE("lowtide"),
		TAELDIM("taeldim"),
		HIGHWATCH("highwatch"),
		MISTPORT("mistport"),
		ALNERA("alnera"),
		RAHKERI("rahkeri"),
		MOLTA("molta"),
		FROSTGATE("frostgate"),
		NIGHTROOST("nightroost"),
		WISPERVALE("wispervale"),
		STEELMELD("steelmeld");

		private final String mLabel;
		City(String label) {
			mLabel = label;
		}

		public String getLabel() {
			return mLabel;
		}
	}

	/*
	 * Nuke all Spring Event entries in the database.
	 * Development use only. Should not be called in release version
	 *
	private static void nuke(Player player) {
		for (City c : City.values()) {
			RedisManager.del("spring:" + c.getLabel());
		}
		player.sendMessage("DB nuked successfully. I hope you meant to do this...");
	}
*/

	public static void doClean(CommandSender sender, Player player, List<Item> items, City city) throws CommandSyntaxException {
		if (!items.isEmpty()) {
			int valueOfItems = calcItemValue(items);
			for (Item i : items) { //kill all item entities before adding scores
				i.remove();
			}
			updateValues(valueOfItems, player, city);
			displayPlayerStats(sender, player);
		} else {
			CommandAPI.fail("No items were detected. Throw items on the platform to contribute to this city's goal!");
		}
	}

	/*
	 * Show top n players for a given city and display its total
	 */
	public static void displayCityStats(CommandSender sender, Player player, City city) throws CommandSyntaxException {
		player.sendMessage(ChatColor.BOLD + "Stats for " + city.getLabel().toUpperCase() + ":");
		player.sendMessage(ChatColor.AQUA + "City Total: " + getCityTotal(city));
		player.sendMessage(ChatColor.BOLD + "Top 5 contributors:");
		Map<String, Integer> leaders = getCityLeaders(city);
		if (!leaders.isEmpty()) {
			final int MAX_PLAYERS = 5;
			int i = 1;
			for (Map.Entry<String, Integer> e : leaders.entrySet()) {
				player.sendMessage(i + ". " + e.getKey() + ":     " + e.getValue());
				i++;
				if (i >= MAX_PLAYERS) {
					break;
				}
			}
		} else {
			CommandAPI.fail("This city doesn't have any contributions yet. Be the first to contribute!");
		}
	}

	public static void displayPlayerStats(CommandSender sender, Player player) throws CommandSyntaxException {
		Map<String, Integer> contributions = getPlayerContributions(player);
		if (!contributions.isEmpty()) {
			player.sendMessage(ChatColor.BOLD + "Your contributions to the Spring Cleaning Event:");
			for (Map.Entry<String, Integer> e : contributions.entrySet()) {
				player.sendMessage(e.getKey().toUpperCase() + ":     " + e.getValue());
			}
		} else {
			CommandAPI.fail("You don't have any contributions yet!");
		}
	}

	private static int calcItemValue(List<Item> items) throws CommandSyntaxException {
		int value = 0;
		ItemStack stack;
		for (Item i : items) {
			stack = i.getItemStack();
			if (ItemUtils.getItemRegion(stack).equals(ItemRegion.KINGS_VALLEY)) {
				switch (ItemUtils.getItemTier(stack)) {
					case ONE:
						value += 2*stack.getAmount();
						break;
					case TWO:
						value += 4*stack.getAmount();
						break;
					case THREE:
						value += 6*stack.getAmount();
						break;
					case FOUR:
						value += 8*stack.getAmount();
						break;
					case FIVE:
						value += 12*stack.getAmount();
						break;
					case UNCOMMON:
						value += 8*stack.getAmount();
						break;
					case ENHANCED_UNCOMMON:
						value += 16*stack.getAmount();
						break;
					case RARE:
						value += 80*stack.getAmount();
						break;
					case ENHANCED_RARE:
						value += 160*stack.getAmount();
						break;
					case ARTIFACT:
						value += 160*stack.getAmount();
						break;
					case EPIC:
						value += 3200*stack.getAmount();
						break;
					default:
						CommandAPI.fail(ItemUtils.getItemTier(stack) + " tier items are not accepted by the Spring Cleaner.");
				}
			} else if (stack.getItemMeta().getDisplayName().contains("Fragment") && stack.getLore().contains("Material")) {
				value += 80*stack.getAmount();
			} else if (stack.getItemMeta().getDisplayName().contains("Pulsating Gold") && stack.getType().equals(Material.GOLD_NUGGET)) {
				value += 160*stack.getAmount();
			} else if (stack.getItemMeta().getDisplayName().contains("Concentrated Experience")) {
				value += 2*stack.getAmount();
			} else if (stack.getItemMeta().getDisplayName().contains("Hyper Experience")) {
				value += 128*stack.getAmount();
			} else {
				CommandAPI.fail(stack.getItemMeta().getDisplayName() + " is not accepted by the Spring Cleaner.");
			}
		}
		return value;
	}

	/* Set database values
	 * Storage Format:
	 * Key: 'spring:[cityname]'
	 * Value: Map<'[playername]', '[amount]'>
	 */
	private static void updateValues(int value, Player player, City city) {
		Integer prevVal;
		if (RedisManager.hexists("spring:" + city.getLabel(), player.getName())) {
			prevVal = new Integer(RedisManager.hget("spring:" + city.getLabel(), player.getName()));
		} else {
			prevVal = new Integer(0);
		}
		value += prevVal;
		RedisManager.hset("spring:" + city.getLabel(), player.getName(), Integer.toString(value));
	}

	/*
	 * Sum all contributions for a city by all players to get total for that city
	 */
	private static int getCityTotal(City city) {
		Map<String, String> contributions = RedisManager.hgetAll("spring:" + city.getLabel());
		int cityTotal = 0;
		for (String c : contributions.values()) {
			cityTotal += new Integer(c);
		}
		return cityTotal;
	}

	/*
	 * Get player's contributions to each city
	 * Format: Map<'[cityname]', amt>
	 */
	private static Map<String, Integer> getPlayerContributions(Player player) {
		Map<String, Integer> playerContributions = new HashMap<>();
		for (City c : City.values()) {
			if (RedisManager.hexists("spring:" + c.getLabel(), player.getName())) {
				playerContributions.put(c.getLabel(), new Integer(RedisManager.hget("spring:" + c.getLabel(), player.getName())));
			}
		}
		return playerContributions;
	}

	/*
	 * Returns sorted collection of city contributions
	 */
	private static Map<String, Integer> getCityLeaders(City city) {
		Map<String, String> contributions = RedisManager.hgetAll("spring:" + city.getLabel());
		Map<String, Integer> iContributions = new HashMap<>();
		for (Map.Entry<String, String> e : contributions.entrySet()) {
			iContributions.put(e.getKey(), new Integer(e.getValue()));
		}
		return sortMapByValues(iContributions);
	}

	/*
	 * Custom sort to order city contributors Map by values for leaderboard
	 * https://www.geeksforgeeks.org/sorting-a-hashmap-according-to-values/
	 */
	private static Map<String, Integer> sortMapByValues(Map<String, Integer> map) {
		List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});
		HashMap<String, Integer> tmp = new LinkedHashMap<String, Integer>();
		for (Map.Entry<String, Integer> e : list) {
			tmp.put(e.getKey(), e.getValue());
		}
		return tmp;
	}
}
