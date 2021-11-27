package com.playmonumenta.plugins.custominventories;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.goncalomb.bukkit.mylib.utils.CustomInventory;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class OrinCustomInventory extends CustomInventory {
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private static final int PLOTS_DEFAULT = 0;
	private static final int REGION1_DEFAULT = 10;
	private static final int REGION2_DEFAULT = 20;
	private static final String PAGE_SCOREBOARD = "OrinPage";

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
		String mScoreboard;
		String mLore;
		int mScoreRequired;
		Material mType;
		String mCommand;

		public TeleportEntry(int s, String n, String sc, String l, Material t, String c) {
			mSlot = s;
			mName = n;
			mScoreboard = sc;
			mLore = l;
			mScoreRequired = 1;
			mType = t;
			mCommand = c;
		}

		public TeleportEntry(int s, String n, String sc, String l, Material t, String c, int sr) {
			mSlot = s;
			mName = n;
			mScoreboard = sc;
			mLore = l;
			mScoreRequired = sr;
			mType = t;
			mCommand = c;
		}

		public TeleportEntry(int s, String n, String sc, String l, Material t, String c, int sr, int p) {
			mSlot = s;
			mName = n;
			mScoreboard = sc;
			mLore = l;
			mScoreRequired = sr;
			mType = t;
			mCommand = c;
			mPage = p;
		}
	}

	private static ArrayList<TeleportEntry> LOCATIONS_COMMON = new ArrayList<>();

	static {
		LOCATIONS_COMMON.add(new TeleportEntry(20, "Labs", "D0Access", "Click to teleport!", Material.GLASS_BOTTLE, "execute as @S run function monumenta:lobbies/send_one/d0"));
		LOCATIONS_COMMON.add(new TeleportEntry(21, "White", "D1Access", "Click to teleport!", Material.WHITE_WOOL, "execute as @S run function monumenta:lobbies/send_one/d1"));
		LOCATIONS_COMMON.add(new TeleportEntry(22, "Orange", "D2Access", "Click to teleport!", Material.ORANGE_WOOL, "execute as @S run function monumenta:lobbies/send_one/d2"));
		LOCATIONS_COMMON.add(new TeleportEntry(29, "Magenta", "D3Access", "Click to teleport!", Material.MAGENTA_WOOL, "execute as @S run function monumenta:lobbies/send_one/d3"));
		LOCATIONS_COMMON.add(new TeleportEntry(30, "Light Blue", "D4Access", "Click to teleport!", Material.LIGHT_BLUE_WOOL, "execute as @S run function monumenta:lobbies/send_one/d4"));
		LOCATIONS_COMMON.add(new TeleportEntry(31, "Yellow", "D5Access", "Click to teleport!", Material.YELLOW_WOOL, "execute as @S run function monumenta:lobbies/send_one/d5"));
		LOCATIONS_COMMON.add(new TeleportEntry(38, "Willows", "DB1Access", "Click to teleport!", Material.JUNGLE_LEAVES, "execute as @S run function monumenta:lobbies/send_one/db1"));
		LOCATIONS_COMMON.add(new TeleportEntry(39, "Reverie", "DCAccess", "Click to teleport!", Material.FIRE_CORAL, "execute as @S run function monumenta:lobbies/send_one/dc"));
		LOCATIONS_COMMON.add(new TeleportEntry(40, "Sanctum", "DS1Access", "Click to teleport!", Material.GRASS_BLOCK, "execute as @S run function monumenta:lobbies/send_one/ds1"));

		LOCATIONS_COMMON.add(new TeleportEntry(24, "Lime", "D6Access", "Click to teleport!", Material.LIME_WOOL, "execute as @S run function monumenta:lobbies/send_one/d6"));
		LOCATIONS_COMMON.add(new TeleportEntry(25, "Pink", "D7Access", "Click to teleport!", Material.PINK_WOOL, "execute as @S run function monumenta:lobbies/send_one/d7"));
		LOCATIONS_COMMON.add(new TeleportEntry(26, "Gray", "D8Access", "Click to teleport!", Material.GRAY_WOOL, "execute as @S run function monumenta:lobbies/send_one/d8"));
		LOCATIONS_COMMON.add(new TeleportEntry(33, "Light Gray", "D9Access", "Click to teleport!", Material.LIGHT_GRAY_WOOL, "execute as @S run function monumenta:lobbies/send_one/d9"));
		LOCATIONS_COMMON.add(new TeleportEntry(34, "Cyan", "D10Access", "Click to teleport!", Material.CYAN_WOOL, "execute as @S run function monumenta:lobbies/send_one/d10"));
		LOCATIONS_COMMON.add(new TeleportEntry(35, "Purple", "D11Access", "Click to teleport!", Material.PURPLE_WOOL, "execute as @S run function monumenta:lobbies/send_one/d11"));
		LOCATIONS_COMMON.add(new TeleportEntry(42, "Teal", "DTLAccess", "Click to teleport!", Material.CYAN_CONCRETE_POWDER, "execute as @S run function monumenta:lobbies/send_one/dtl"));
		LOCATIONS_COMMON.add(new TeleportEntry(43, "Shifting City", "DRL2Access", "Click to teleport!", Material.PRISMARINE_BRICKS, "execute as @S run function monumenta:lobbies/send_one/drl2"));
		LOCATIONS_COMMON.add(new TeleportEntry(44, "The Fallen Forum", "DFFAccess", "Click to teleport!", Material.BOOKSHELF, "execute as @S run function monumenta:lobbies/send_one/dff"));
	}

	private static ArrayList<TeleportEntry> LOCATIONS_PLOTS = new ArrayList<>();

	static {
		LOCATIONS_PLOTS.add(new TeleportEntry(0, "Docks", null, "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, "tp @S -2456.0 56.5 1104.0 90 0"));
		LOCATIONS_PLOTS.add(new TeleportEntry(18, "Market", null, "Click to teleport!", Material.BARREL, "execute as @S run function monumenta:mechanisms/teleporters/enter_new_market"));
		LOCATIONS_PLOTS.add(new TeleportEntry(27, "Personal Plot", null, "Click to teleport!", Material.GRASS_BLOCK, "execute as @S run function plot:plot/home"));
		LOCATIONS_PLOTS.add(new TeleportEntry(36, "Guild Plot", null, "Click to teleport!", Material.YELLOW_BANNER, "teleportguild @S"));
		LOCATIONS_PLOTS.add(new TeleportEntry(45, "Build Server", null, "Click to teleport!", Material.STONE_PICKAXE, "transferserver build -2456.0 56.5 1104.0 90 0"));

		LOCATIONS_PLOTS.add(new TeleportEntry(3, "Sierhaven", null, "Click to teleport!", Material.GREEN_CONCRETE, "execute as @S run function monumenta:mechanisms/teleporters/tp/plots_to_valley"));

		LOCATIONS_PLOTS.add(new TeleportEntry(7, "Mistport", "Quest101", "Click to teleport!", Material.SAND, "execute as @S run function monumenta:mechanisms/teleporters/tp/plots_to_isles", 13));

	}

	private static ArrayList<TeleportEntry> LOCATIONS_REGION1 = new ArrayList<>();

	static {
		LOCATIONS_REGION1.add(new TeleportEntry(9, "Plots", null, "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, "execute as @S run function monumenta:mechanisms/teleporters/tp/sierhaven_to_plots"));
		LOCATIONS_REGION1.add(new TeleportEntry(3, "Sierhaven", null, "Click to teleport!", Material.GREEN_CONCRETE, "tp @S -765.5 106.0625 70.5 180 0"));
		LOCATIONS_REGION1.add(new TeleportEntry(7, "Mistport", "Quest101", "Click to teleport!", Material.SAND, "execute as @S run function monumenta:mechanisms/teleporters/tp/sierhaven_to_isles", 13));
		LOCATIONS_REGION1.add(new TeleportEntry(36, "Build Server", null, "Click to teleport!", Material.STONE_PICKAXE, "transferserver build -765.5 106.0625 70.5 180 0"));
	}

	private static ArrayList<TeleportEntry> LOCATIONS_REGION2 = new ArrayList<>();

	static {
		LOCATIONS_REGION2.add(new TeleportEntry(9, "Plots", null, "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, "execute as @S run function monumenta:mechanisms/teleporters/tp/mistport_to_plots"));
		LOCATIONS_REGION2.add(new TeleportEntry(3, "Sierhaven", null, "Click to teleport!", Material.GREEN_CONCRETE, "execute as @S run function monumenta:mechanisms/teleporters/tp/mistport_to_valley"));
		LOCATIONS_REGION2.add(new TeleportEntry(7, "Mistport", "Quest101", "Click to teleport!", Material.SAND, "tp @S -762.5 70.1 1344.5 180 0", 13));
		LOCATIONS_REGION2.add(new TeleportEntry(36, "Build Server", null, "Click to teleport!", Material.STONE_PICKAXE, "transferserver build -762.5 70.1 1344.5 180 0"));

	}

	public OrinCustomInventory(Player player) {
		super(player, 54, "Teleportation Choices");

		String currentShardName = ServerProperties.getShardName();
		if (currentShardName.contains("valley")) {
			setLayout(REGION1_DEFAULT, player);
		} else if (currentShardName.contains("isles")) {
			setLayout(REGION2_DEFAULT, player);
		} else {
			setLayout(PLOTS_DEFAULT, player);
		}
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		Player player = null;
		event.setCancelled(true);
		if (event.getWhoClicked() instanceof Player) {
			player = (Player) event.getWhoClicked();
		} else {
			return;
		}
		ItemStack clickedItem = event.getCurrentItem();
		if (event.getClickedInventory() != _inventory) {
			return;
		}
		int currentPage = ScoreboardUtils.getScoreboardValue(player, PAGE_SCOREBOARD);
		ArrayList<TeleportEntry> listOfItems = null;

		switch (currentPage) {
		case PLOTS_DEFAULT:
			listOfItems = LOCATIONS_PLOTS;
			break;
		case REGION1_DEFAULT:
			listOfItems = LOCATIONS_REGION1;
			break;
		case REGION2_DEFAULT:
			listOfItems = LOCATIONS_REGION2;
			break;
		default:
			player.sendActionBar(Component.text("Page not recognized, defaulting to plots configuration.", NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false));
			listOfItems = LOCATIONS_PLOTS;
			break;
		}

		if (clickedItem != null && clickedItem.getType() != FILLER && !event.isShiftClick()) {
			int chosenSlot = event.getSlot();
			for (TeleportEntry location : LOCATIONS_COMMON) {
				if (location.mSlot == chosenSlot) {
					completeCommand(player, location.mCommand);
					player.closeInventory();
					return;
				}
			}
			for (TeleportEntry location : listOfItems) {
				if (location.mSlot == chosenSlot) {
					completeCommand(player, location.mCommand);
					player.closeInventory();
					return;
				}
			}
		}
	}

	public void completeCommand(Player player, String command) {
		if (command.startsWith("transferserver")) {
			//input format should be "transferserver <shard_name> x, y, z, yaw, pitch"
			String[] splitCommand = command.split(" ");

			double x = Double.valueOf(splitCommand[2]);
			double y = Double.valueOf(splitCommand[3]);
			double z = Double.valueOf(splitCommand[4]);
			float yaw = Float.valueOf(splitCommand[5]);
			float pitch = Float.valueOf(splitCommand[6]);
			try {
				MonumentaRedisSyncAPI.sendPlayer(Plugin.getInstance(), player, splitCommand[1],
						new Location(player.getWorld(), x, y, z),
						yaw, pitch);
			} catch (Exception e) {
				player.sendMessage("Exception Message: " + e.getMessage());
				player.sendMessage("Stack Trace:\n" + e.getStackTrace());
			}
			return;
		} else if (command.startsWith("teleportguild")) {
			String finalCommand = command.replace("@S", player.getName());
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
		} else {
			String finalCommand = command.replace("@S", player.getUniqueId().toString());
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
		}
	}

	public ItemStack createCustomItem(TeleportEntry location) {
		ItemStack newItem = new ItemStack(location.mType, 1);
		ItemMeta meta = newItem.getItemMeta();
		meta.displayName(Component.text(location.mName, NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false));
		ArrayList<String> lore = new ArrayList<String>();
		if (location.mLore != "") {
			lore.add(location.mLore);
		}
		meta.setLore(lore);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		newItem.setItemMeta(meta);
		return newItem;
	}

	public void setLayout(int page, Player player) {
		_inventory.clear();
		ArrayList<TeleportEntry> listOfItems = null;
		switch (page) {
		case 0:
			listOfItems = LOCATIONS_PLOTS;
			ScoreboardUtils.setScoreboardValue(player, PAGE_SCOREBOARD, 0);
			break;
		case 10:
			listOfItems = LOCATIONS_REGION1;
			ScoreboardUtils.setScoreboardValue(player, PAGE_SCOREBOARD, 10);
			break;
		case 20:
			listOfItems = LOCATIONS_REGION2;
			ScoreboardUtils.setScoreboardValue(player, PAGE_SCOREBOARD, 20);
			break;
		default:
			player.sendActionBar(Component.text("Shard not recognized, defaulting to plots.", NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false));
			listOfItems = LOCATIONS_PLOTS;
			ScoreboardUtils.setScoreboardValue(player, PAGE_SCOREBOARD, 0);
			break;
		}
		for (TeleportEntry commonLoc : LOCATIONS_COMMON) {
			if (commonLoc.mScoreboard == null || ScoreboardUtils.getScoreboardValue(player, commonLoc.mScoreboard) >= commonLoc.mScoreRequired) {
				ItemStack newItem = createCustomItem(commonLoc);
				_inventory.setItem(commonLoc.mSlot, newItem);
			}
		}

		for (TeleportEntry location : listOfItems) {
			if (location.mScoreboard == null || ScoreboardUtils.getScoreboardValue(player, location.mScoreboard) >= location.mScoreRequired) {
				ItemStack newItem = createCustomItem(location);
				_inventory.setItem(location.mSlot, newItem);
			}
		}

		for (int i = 0; i < 54; i++) {
			if (_inventory.getItem(i) == null) {
				_inventory.setItem(i, new ItemStack(FILLER, 1));
			}
		}
	}
}
