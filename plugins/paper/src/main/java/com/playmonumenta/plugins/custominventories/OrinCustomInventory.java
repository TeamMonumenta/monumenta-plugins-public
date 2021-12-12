package com.playmonumenta.plugins.custominventories;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.goncalomb.bukkit.mylib.utils.CustomInventory;
import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;

public class OrinCustomInventory extends CustomInventory {
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private static final int[] INSTANCE_LOCATIONS = {20, 22, 24, 38, 40, 42};
	private static final double[] PLOT_FALLBACK = {-2456.0, 56.5, 1104.0, 90, 0};
	private static final double[] VALLEY_FALLBACK = {-765.5, 106.0625, 70.5, 180, 0};
	private static final double[] ISLES_FALLBACK = {-762.5, 70.1, 1344.5, 180, 0};

	/*
	 * Pages explanation: Page x0 of the x0-x9 set is the default landing for the gui.
	 * Pages 0-9: Plots
	 * Pages 10-19: Region 1
	 * Pages 20-29: Region 2
	 * Pages 50-on: Common pages
	 */

	public static class TeleportEntry {
		int mPage = 1;
		int mSlot;
		String mName;
		@Nullable String mScoreboard;
		String mLore;
		int mScoreRequired;
		Material mType;
		String mLeftClick;
		String mRightClick;

		public TeleportEntry(int p, int s, String n, String l, Material t, @Nullable String sc, int sr, String left, String right) {
			mPage = p;
			mSlot = s;
			mName = n;
			mLore = l;
			mType = t;
			mScoreboard = sc;
			mScoreRequired = sr;
			mLeftClick = left;
			mRightClick = right;
		}

		public TeleportEntry(int p, int s, String n, String l, Material t, @Nullable String sc, int sr, String left) {
			mPage = p;
			mSlot = s;
			mName = n;
			mLore = l;
			mType = t;
			mScoreboard = sc;
			mScoreRequired = sr;
			mLeftClick = left;
			mRightClick = "";
		}
	}
	/* Page Info
	 * Page 0: Common for 1-9
	 * Page 1: Region 1
	 * Page 2: Region 2
	 * Page 3: Plots
	 * Page 10: Common for 11-19
	 * Page 11: Region Instance Choice
	 */

	private static ArrayList<TeleportEntry> ORIN_ITEMS = new ArrayList<>();
	private ArrayList<TeleportEntry> INSTANCE_ITEMS = new ArrayList<>();

	static {
		ORIN_ITEMS.add(new TeleportEntry(0, 20, "Labs", "Click to teleport!", Material.GLASS_BOTTLE, "D0Access", 1, "execute as @S run function monumenta:lobbies/send_one/d0"));
		ORIN_ITEMS.add(new TeleportEntry(0, 21, "White", "Click to teleport!", Material.WHITE_WOOL, "D1Access", 1, "execute as @S run function monumenta:lobbies/send_one/d1"));
		ORIN_ITEMS.add(new TeleportEntry(0, 22, "Orange", "Click to teleport!", Material.ORANGE_WOOL, "D2Access", 1, "execute as @S run function monumenta:lobbies/send_one/d2"));
		ORIN_ITEMS.add(new TeleportEntry(0, 29, "Magenta", "Click to teleport!", Material.MAGENTA_WOOL, "D3Access", 1, "execute as @S run function monumenta:lobbies/send_one/d3"));
		ORIN_ITEMS.add(new TeleportEntry(0, 30, "Light Blue", "Click to teleport!", Material.LIGHT_BLUE_WOOL, "D4Access", 1, "execute as @S run function monumenta:lobbies/send_one/d4"));
		ORIN_ITEMS.add(new TeleportEntry(0, 31, "Yellow", "Click to teleport!", Material.YELLOW_WOOL, "D5Access", 1, "execute as @S run function monumenta:lobbies/send_one/d5"));
		ORIN_ITEMS.add(new TeleportEntry(0, 38, "Willows", "Click to teleport!", Material.JUNGLE_LEAVES, "DB1Access", 1, "execute as @S run function monumenta:lobbies/send_one/db1"));
		ORIN_ITEMS.add(new TeleportEntry(0, 39, "Reverie", "Click to teleport!", Material.FIRE_CORAL, "DCAccess", 1, "execute as @S run function monumenta:lobbies/send_one/dc"));
		ORIN_ITEMS.add(new TeleportEntry(0, 40, "Sanctum", "Click to teleport!", Material.GRASS_BLOCK, "DS1Access", 1, "execute as @S run function monumenta:lobbies/send_one/ds1"));

		ORIN_ITEMS.add(new TeleportEntry(0, 24, "Lime", "Click to teleport!", Material.LIME_WOOL, "D6Access", 1, "execute as @S run function monumenta:lobbies/send_one/d6"));
		ORIN_ITEMS.add(new TeleportEntry(0, 25, "Pink", "Click to teleport!", Material.PINK_WOOL, "D7Access", 1, "execute as @S run function monumenta:lobbies/send_one/d7"));
		ORIN_ITEMS.add(new TeleportEntry(0, 26, "Gray", "Click to teleport!", Material.GRAY_WOOL, "D8Access", 1, "execute as @S run function monumenta:lobbies/send_one/d8"));
		ORIN_ITEMS.add(new TeleportEntry(0, 33, "Light Gray", "Click to teleport!", Material.LIGHT_GRAY_WOOL, "D9Access", 1, "execute as @S run function monumenta:lobbies/send_one/d9"));
		ORIN_ITEMS.add(new TeleportEntry(0, 34, "Cyan", "Click to teleport!", Material.CYAN_WOOL, "D10Access", 1, "execute as @S run function monumenta:lobbies/send_one/d10"));
		ORIN_ITEMS.add(new TeleportEntry(0, 35, "Purple", "Click to teleport!", Material.PURPLE_WOOL, "D11Access", 1, "execute as @S run function monumenta:lobbies/send_one/d11"));
		ORIN_ITEMS.add(new TeleportEntry(0, 42, "Teal", "Click to teleport!", Material.CYAN_CONCRETE_POWDER, "DTLAccess", 1, "execute as @S run function monumenta:lobbies/send_one/dtl"));
		ORIN_ITEMS.add(new TeleportEntry(0, 43, "Shifting City", "Click to teleport!", Material.PRISMARINE_BRICKS, "DRL2Access", 1, "execute as @S run function monumenta:lobbies/send_one/drl2"));
		ORIN_ITEMS.add(new TeleportEntry(0, 44, "The Fallen Forum", "Click to teleport!", Material.BOOKSHELF, "DFFAccess", 1, "execute as @S run function monumenta:lobbies/send_one/dff", ""));


		ORIN_ITEMS.add(new TeleportEntry(1, 9, "Plots", "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, null, 0, "execute as @S run function monumenta:mechanisms/teleporters/tp/sierhaven_to_plots"));
		ORIN_ITEMS.add(new TeleportEntry(1, 27, "Player Plot", "Click to teleport!", Material.PLAYER_HEAD, "CurrentPlot", 1, "plot send @S", "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(1, 3, "Sierhaven", "Left Click to be sorted to a shard, right click to choose the shard.", Material.GREEN_CONCRETE, null, 0, "tp @S -765.5 106.0625 70.5 180 0", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(1, 7, "Mistport", "Left Click to be sorted to a shard, right click to choose the shard.", Material.SAND, "Quest101", 13, "execute as @S run function monumenta:mechanisms/teleporters/tp/sierhaven_to_isles", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(1, 45, "Build Server", "Click to teleport!", Material.STONE_PICKAXE, null, 0, "transferserver build"));


		ORIN_ITEMS.add(new TeleportEntry(2, 9, "Plots", "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, null, 0, "execute as @S run function monumenta:mechanisms/teleporters/tp/mistport_to_plots"));
		ORIN_ITEMS.add(new TeleportEntry(2, 27, "Player Plot", "Click to teleport!", Material.PLAYER_HEAD, "CurrentPlot", 1, "plot send @S", "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(2, 3, "Sierhaven", "Left Click to be sorted to a shard, right click to choose the shard.", Material.GREEN_CONCRETE, null, 0, "execute as @S run function monumenta:mechanisms/teleporters/tp/mistport_to_valley", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(2, 7, "Mistport", "Left Click to be sorted to a shard, right click to choose the shard.", Material.SAND, "Quest101", 13, "tp @S -762.5 70.1 1344.5 180 0", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(2, 45, "Build Server", "Click to teleport!", Material.STONE_PICKAXE, null, 0, "transferserver build"));


		ORIN_ITEMS.add(new TeleportEntry(3, 0, "Docks", "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, null, 0, "tp @S -2456.0 56.5 1104.0 90 0"));
		ORIN_ITEMS.add(new TeleportEntry(3, 18, "Market", "Click to teleport!", Material.BARREL, null, 0, "execute as @S run function monumenta:mechanisms/teleporters/enter_new_market"));
		ORIN_ITEMS.add(new TeleportEntry(3, 27, "Player Plot", "Click to teleport!", Material.GRASS_BLOCK, "CurrentPlot", 1, "plot send @S", "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(3, 36, "Guild Plot", "Click to teleport!", Material.YELLOW_BANNER, null, 0, "teleportguild @S"));
		ORIN_ITEMS.add(new TeleportEntry(3, 45, "Build Server", "Click to teleport!", Material.STONE_PICKAXE, null, 0, "transferserver build"));
		ORIN_ITEMS.add(new TeleportEntry(3, 3, "Sierhaven", "Left Click to be sorted to a shard, right click to choose the shard.", Material.GREEN_CONCRETE, null, 0, "execute as @S run function monumenta:mechanisms/teleporters/tp/plots_to_valley", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(3, 7, "Mistport", "Left Click to be sorted to a shard, right click to choose the shard.", Material.SAND, "Quest101", 13, "execute as @S run function monumenta:mechanisms/teleporters/tp/plots_to_isles", "instancebot isles"));


		ORIN_ITEMS.add(new TeleportEntry(4, 9, "Plots", "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, null, 0, "transferserver plots"));
		// TODO: Eventually add some way to change to other plots you have access to without leaving & coming back
		ORIN_ITEMS.add(new TeleportEntry(4, 3, "Sierhaven", "Left Click to be sorted to a shard, right click to choose the shard.", Material.GREEN_CONCRETE, null, 0, "transferserver valley", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(4, 7, "Mistport", "Left Click to be sorted to a shard, right click to choose the shard.", Material.SAND, "Quest101", 13, "transferserver isles", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(4, 45, "Build Server", "Click to teleport!", Material.STONE_PICKAXE, null, 0, "transferserver build"));

		ORIN_ITEMS.add(new TeleportEntry(10, 0, "Back", "Return to the main page.", Material.ARROW, null, 0, "back"));
		ORIN_ITEMS.add(new TeleportEntry(10, 4, "Available Shards", "Choose your shard below.", Material.SCUTE, null, 0, ""));
	}

	private int mCurrentPage = -1;
	private String mCurrentShard = ServerProperties.getShardName();

	public OrinCustomInventory(Player player, int page) {
		super(player, 54, "Teleportation Choices");
		if (page == -1) {
			if (mCurrentShard.contains("valley")) {
				mCurrentPage = 1;
			} else if (mCurrentShard.contains("isles")) {
				mCurrentPage = 2;
			} else if (mCurrentShard.equals("plots")) {
				mCurrentPage = 3;
			} else {
				mCurrentPage = 4;
			}
		} else {
			mCurrentPage = page;
		}

		setLayout(player);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		Player player = null;
		if (event.getWhoClicked() instanceof Player) {
			player = (Player) event.getWhoClicked();
		} else {
			return;
		}
		ItemStack clickedItem = event.getCurrentItem();
		if (event.getClickedInventory() != _inventory) {
			return;
		}

		int commonPage = (int) Math.floor(mCurrentPage / 10) * 10;
		if (clickedItem != null && clickedItem.getType() != FILLER && !event.isShiftClick()) {
			int chosenSlot = event.getSlot();
			for (TeleportEntry item : ORIN_ITEMS) {
				if (item.mSlot == chosenSlot && item.mPage == mCurrentPage) {
					if (event.isLeftClick()) {
						completeCommand(player, item.mLeftClick);
					} else {
						completeCommand(player, item.mRightClick);
					}
				}
				if (item.mSlot == chosenSlot && item.mPage == commonPage) {
					if (event.isLeftClick()) {
						completeCommand(player, item.mLeftClick);
					} else {
						completeCommand(player, item.mRightClick);
					}
				}
			}
			if (mCurrentPage == 11 || mCurrentPage == 12) {
				for (TeleportEntry item : INSTANCE_ITEMS) {
					if (item.mSlot == chosenSlot && item.mPage == mCurrentPage) {
						if (event.isLeftClick()) {
							completeCommand(player, item.mLeftClick);
						} else {
							completeCommand(player, item.mRightClick);
						}
					}
				}
			}
		}
	}

	public Boolean isInternalCommand(String command) {
		if (command.equals("exit") || command.startsWith("page") || command.startsWith("instancebot") || command.equals("back")) {
			return true;
		}
		return false;
	}

	public void runInternalCommand(Player player, String cmd) {
		if (cmd.startsWith("page")) {
			mCurrentPage = Integer.parseInt(cmd.split(" ")[1]);
			setLayout(player);
			return;
		} else if (cmd.startsWith("exit")) {
			player.closeInventory();
			return;
		} else if (cmd.startsWith("instancebot")) {
			String searchTerm = cmd.split(" ")[1];
			mCurrentPage = (searchTerm.equals("valley")) ? 11 : 12;
			setLayout(player);
		} else if (cmd.equals("back")) {
			if (mCurrentShard.contains("valley")) {
				mCurrentPage = 1;
			} else if (mCurrentShard.contains("isles")) {
				mCurrentPage = 2;
			} else {
				mCurrentPage = 3;
			}
			setLayout(player);
		}
	}

	public void completeCommand(Player player, String cmd) {
		if (cmd == "") {
			return;
		}
		if (isInternalCommand(cmd)) {
			runInternalCommand(player, cmd);
			return;
		} else {
			if (cmd.startsWith("transferserver")) {
				//input format should be "transferserver <shard_name>"
				String[] splitCommand = cmd.split(" ");
				String targetShard = splitCommand[1];
				Location returnLoc = null;
				Float returnYaw = null;
				Float returnPitch = null;

				if (mCurrentShard.equals("playerplots")) {
					// Don't modify return location
				} else {
					double[] currentShardVals;
					if (mCurrentShard.contains("valley")) {
						currentShardVals = VALLEY_FALLBACK;
					} else if (mCurrentShard.contains("isles")) {
						currentShardVals = ISLES_FALLBACK;
					} else {
						currentShardVals = PLOT_FALLBACK;
					}

					double x = currentShardVals[0];
					double y = currentShardVals[1];
					double z = currentShardVals[2];
					returnYaw = (float) currentShardVals[3];
					returnPitch = (float) currentShardVals[4];

					returnLoc = new Location(player.getWorld(), x, y, z);
				}

				try {
					/* Note that this API accepts null returnLoc, returnYaw, returnPitch as default current player location */
					MonumentaRedisSyncAPI.sendPlayer(player, targetShard, returnLoc, returnYaw, returnPitch);
				} catch (Exception e) {
					player.sendMessage("Exception Message: " + e.getMessage());
					player.sendMessage("Stack Trace:\n" + e.getStackTrace());
				}
				return;
			} else {
				String finalCommand = cmd.replace("@S", player.getName());
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
				player.closeInventory();
			}
		}
	}

	public ItemStack createCustomItem(TeleportEntry location) {
		ItemStack newItem = new ItemStack(location.mType, 1);
		ItemMeta meta = newItem.getItemMeta();
		meta.displayName(Component.text(location.mName, NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false));
		ArrayList<String> lore = new ArrayList<String>();
		if (location.mLore != "") {
			splitLoreLine(meta, location.mLore, 30, ChatColor.DARK_PURPLE);
		}
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		newItem.setItemMeta(meta);
		return newItem;
	}

	public void splitLoreLine(ItemMeta meta, String lore, int maxLength, ChatColor defaultColor) {
		String[] splitLine = lore.split(" ");
		String currentString = defaultColor + "";
		List<String> finalLines = new ArrayList<String>();
		int currentLength = 0;
		for (String word : splitLine) {
			if (currentLength + word.length() > maxLength) {
				finalLines.add(currentString);
				currentString = defaultColor + "";
				currentLength = 0;
			}
			currentString += word + " ";
			currentLength += word.length() + 1;
		}
		if (currentString != defaultColor + "") {
			finalLines.add(currentString);
		}
		meta.setLore(finalLines);
	}

	public void setLayout(Player player) {
		_inventory.clear();
		int commonPage = (int) Math.floor(mCurrentPage / 10) * 10;
		for (TeleportEntry item : ORIN_ITEMS) {
			if (item.mPage == commonPage) {
				if (item.mScoreboard == null || ScoreboardUtils.getScoreboardValue(player, item.mScoreboard) >= item.mScoreRequired) {
					_inventory.setItem(item.mSlot, createCustomItem(item));
				}
			} //intentionally not else, so overrides can happen
			if (item.mPage == mCurrentPage) {
				if (item.mScoreboard == null || ScoreboardUtils.getScoreboardValue(player, item.mScoreboard) >= item.mScoreRequired) {
					_inventory.setItem(item.mSlot, createCustomItem(item));
				}
			}
		}

		for (int i = 0; i < 54; i++) {
			if (_inventory.getItem(i) == null) {
				_inventory.setItem(i, new ItemStack(FILLER, 1));
			}
		}
		if (mCurrentPage == 11) {
			showInstances(player, "valley");
		} else if (mCurrentPage == 12) {
			showInstances(player, "isles");
		}
	}

	public void showInstances(Player player, String searchTerm) {
		Set<String> results = null;
		INSTANCE_ITEMS.clear();
		try {
			results = NetworkRelayAPI.getOnlineShardNames();
		} catch (Exception e) {
			e.printStackTrace();
			player.closeInventory();
		}
		int index = 0;
		results.removeIf(item -> !item.startsWith(searchTerm));
		int page = (searchTerm.equals("valley")) ? 11 : 12;
		Material itemType = (searchTerm.equals("valley")) ? Material.GRASS : Material.PUFFERFISH;
		ArrayList<String> resultList = new ArrayList<>(results);
		Collections.sort(resultList);
		for (String shard : resultList) {
			String shardName = shard.substring(0, 1).toUpperCase() + shard.substring(1, shard.length());
			if (index <= INSTANCE_LOCATIONS.length) {
				INSTANCE_ITEMS.add(new TeleportEntry(page, INSTANCE_LOCATIONS[index++], shardName, "Click to teleport!", itemType, null, 0, "transferserver " + shard));
			}
		}

		for (TeleportEntry item : INSTANCE_ITEMS) {
			_inventory.setItem(item.mSlot, createCustomItem(item));
		}
	}
}
