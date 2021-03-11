package com.playmonumenta.plugins.guis.singlepageguis;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.playmonumenta.plugins.guis.SinglePageGUI;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;

public class OrinSinglePageGUI extends SinglePageGUI {

	private static final int ROWS = 6;
	private static final int COLUMNS = 9;

	//Layout, with P_x being plots, R1S as sierhaven,
	//R2M as mistport, and the rest matching access code

	//-----------------------------------------
	//P_D |x| xx |R1S | xx |x| xx |R2M | xx |x|
	//-----------------------------------------
	// xx |x| xx | xx | xx |x| xx | xx | xx |x|
	//-----------------------------------------
	//P_M |x| D0 | D1 | D3 |x| D6 | D7 | D8 |x|
	//-----------------------------------------
	//P_P |x| D3 | D4 | D5 |x| D9 |D10 |D11 |x|
	//-----------------------------------------
	//P_G |x|DB1 | DC |DS1 |x|DTL |DRL2|DTFF|x|
	//-----------------------------------------
	// xx |x| xx | xx | xx |x| xx | xx | xx |x|
	//-----------------------------------------

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

		//for situations like Mistport, which checks for >= 12 instead of > 1

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

	private static ArrayList<TeleportEntry> LOCATIONS_PAGE = new ArrayList<>();

	static {
		LOCATIONS_PAGE.add(new TeleportEntry(8, "Switch to Tree Layout", null, "", Material.PISTON, "switch"));

		LOCATIONS_PAGE.add(new TeleportEntry(0, "Docks", null, "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, "tp @S -2456.0 56.5 1104.0 90 0"));
		LOCATIONS_PAGE.add(new TeleportEntry(18, "Market", null, "Click to teleport!", Material.BARREL, "execute as @S run function monumenta:mechanisms/teleporters/enter_new_market"));
		LOCATIONS_PAGE.add(new TeleportEntry(27, "Personal Plot", null, "Click to teleport!", Material.GRASS_BLOCK, "execute as @S run function plot:plot/home"));
		LOCATIONS_PAGE.add(new TeleportEntry(36, "Guild Plot", null, "Click to teleport!", Material.YELLOW_BANNER, "teleportguild @S"));

		LOCATIONS_PAGE.add(new TeleportEntry(3, "Sierhaven", null, "Click to teleport!", Material.GREEN_CONCRETE, "transferserver region_1 -2456.0 56.5 1104.0 90 0"));
		LOCATIONS_PAGE.add(new TeleportEntry(20, "Labs", "D0Access", "Click to teleport!", Material.GLASS_BOTTLE, "execute as @S run function monumenta:lobbies/send_one/d0"));
		LOCATIONS_PAGE.add(new TeleportEntry(21, "White", "D1Access", "Click to teleport!", Material.WHITE_WOOL, "execute as @S run function monumenta:lobbies/send_one/d1"));
		LOCATIONS_PAGE.add(new TeleportEntry(22, "Orange", "D2Access", "Click to teleport!", Material.ORANGE_WOOL, "execute as @S run function monumenta:lobbies/send_one/d2"));
		LOCATIONS_PAGE.add(new TeleportEntry(29, "Magenta", "D3Access", "Click to teleport!", Material.MAGENTA_WOOL, "execute as @S run function monumenta:lobbies/send_one/d3"));
		LOCATIONS_PAGE.add(new TeleportEntry(30, "Light Blue", "D4Access", "Click to teleport!", Material.LIGHT_BLUE_WOOL, "execute as @S run function monumenta:lobbies/send_one/d4"));
		LOCATIONS_PAGE.add(new TeleportEntry(31, "Yellow", "D5Access", "Click to teleport!", Material.YELLOW_WOOL, "execute as @S run function monumenta:lobbies/send_one/d5"));
		LOCATIONS_PAGE.add(new TeleportEntry(38, "Willows", "DB1Access", "Click to teleport!", Material.JUNGLE_LEAVES, "execute as @S run function monumenta:lobbies/send_one/db1"));
		LOCATIONS_PAGE.add(new TeleportEntry(39, "Reverie", "DCAccess", "Click to teleport!", Material.FIRE_CORAL, "execute as @S run function monumenta:lobbies/send_one/dc"));
		LOCATIONS_PAGE.add(new TeleportEntry(40, "Sanctum", "DS1Access", "Click to teleport!", Material.GRASS_BLOCK, "execute as @S run function monumenta:lobbies/send_one/ds1"));

		LOCATIONS_PAGE.add(new TeleportEntry(7, "Mistport", "Quest101", "Click to teleport!", Material.SAND, "transferserver region_2 -2456.0 56.5 1104.0 90 0", 12));
		LOCATIONS_PAGE.add(new TeleportEntry(24, "Lime", "D6Access", "Click to teleport!", Material.LIME_WOOL, "execute as @S run function monumenta:lobbies/send_one/d6"));
		LOCATIONS_PAGE.add(new TeleportEntry(25, "Pink", "D7Access", "Click to teleport!", Material.PINK_WOOL, "execute as @S run function monumenta:lobbies/send_one/d7"));
		LOCATIONS_PAGE.add(new TeleportEntry(26, "Gray", "D8Access", "Click to teleport!", Material.GRAY_WOOL, "execute as @S run function monumenta:lobbies/send_one/d8"));
		LOCATIONS_PAGE.add(new TeleportEntry(33, "Light Gray", "D9Access", "Click to teleport!", Material.LIGHT_GRAY_WOOL, "execute as @S run function monumenta:lobbies/send_one/d9"));
		LOCATIONS_PAGE.add(new TeleportEntry(34, "Cyan", "D10Access", "Click to teleport!", Material.CYAN_WOOL, "execute as @S run function monumenta:lobbies/send_one/d10"));
		LOCATIONS_PAGE.add(new TeleportEntry(35, "Purple", "D11Access", "Click to teleport!", Material.PURPLE_WOOL, "execute as @S run function monumenta:lobbies/send_one/d11"));
		LOCATIONS_PAGE.add(new TeleportEntry(42, "Teal", "DTLAccess", "Click to teleport!", Material.CYAN_CONCRETE_POWDER, "execute as @S run function monumenta:lobbies/send_one/dtl"));
		LOCATIONS_PAGE.add(new TeleportEntry(43, "Shifting City", "DRL2Access", "Click to teleport!", Material.PRISMARINE_BRICKS, "execute as @S run function monumenta:lobbies/send_one/drl2"));
		LOCATIONS_PAGE.add(new TeleportEntry(44, "The Fallen Forum", "DFFAccess", "Click to teleport!", Material.BOOKSHELF, "execute as @S run function monumenta:lobbies/send_one/dff")); //TODO: access token for TFF
	}

	private static ArrayList<TeleportEntry> LOCATIONS_TREE = new ArrayList<>();

	static {
		LOCATIONS_TREE.add(new TeleportEntry(8, "Switch to Page Layout", null, "", Material.PISTON, "switch", 1, 1));
		LOCATIONS_TREE.add(new TeleportEntry(11, "Plots", null, "", Material.LIGHT_BLUE_CONCRETE, "2", 1, 1));
		LOCATIONS_TREE.add(new TeleportEntry(15, "Region 1", null, "", Material.GREEN_CONCRETE, "3", 1, 1));
		LOCATIONS_TREE.add(new TeleportEntry(38, "Region 2", null, "", Material.SAND, "4", 1, 1));
		//LOCATIONS_TREE.add(new TeleportEntry(42, "Region 3 LUL", null, "", Material.PAPER, "5", 1, 1));

		LOCATIONS_TREE.add(new TeleportEntry(8, "Switch to Page Layout", null, "", Material.PISTON, "switch", 1, 2));
		LOCATIONS_TREE.add(new TeleportEntry(0, "Back to Main Menu", null, "", Material.OBSERVER, "", 1, 2));
		LOCATIONS_TREE.add(new TeleportEntry(4, "Docks", null, "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, "tp @S -2456.0 56.5 1104.0 90 0", 1, 2));
		LOCATIONS_TREE.add(new TeleportEntry(29, "Market", null, "Click to teleport!", Material.BARREL, "execute as @S run function monumenta:mechanisms/teleporters/enter_new_market", 1, 2));
		LOCATIONS_TREE.add(new TeleportEntry(31, "Personal Plot", null, "Click to teleport!", Material.GRASS_BLOCK, "execute as @S run function plot:plot/home", 1, 2));
		LOCATIONS_TREE.add(new TeleportEntry(33, "Guild Plot", null, "Click to teleport!", Material.YELLOW_BANNER, "teleportguild @S", 1, 2));

		LOCATIONS_TREE.add(new TeleportEntry(8, "Switch to Page Layout", null, "", Material.PISTON, "switch", 1, 3));
		LOCATIONS_TREE.add(new TeleportEntry(0, "Back to Main Menu", null, "", Material.OBSERVER, "", 1, 3));
		LOCATIONS_TREE.add(new TeleportEntry(4, "Sierhaven", null, "Click to teleport!", Material.GREEN_CONCRETE, "transferserver region_1 -2456.0 56.5 1104.0 90 0", 1, 3));
		LOCATIONS_TREE.add(new TeleportEntry(23, "Labs", "D0Access", "Click to teleport!", Material.GLASS_BOTTLE, "execute as @S run function monumenta:lobbies/send_one/d0", 1, 3));
		LOCATIONS_TREE.add(new TeleportEntry(20, "White", "D1Access", "Click to teleport!", Material.WHITE_WOOL, "execute as @S run function monumenta:lobbies/send_one/d1", 1, 3));
		LOCATIONS_TREE.add(new TeleportEntry(21, "Orange", "D2Access", "Click to teleport!", Material.ORANGE_WOOL, "execute as @S run function monumenta:lobbies/send_one/d2", 1, 3));
		LOCATIONS_TREE.add(new TeleportEntry(29, "Magenta", "D3Access", "Click to teleport!", Material.MAGENTA_WOOL, "execute as @S run function monumenta:lobbies/send_one/d3", 1, 3));
		LOCATIONS_TREE.add(new TeleportEntry(30, "Light Blue", "D4Access", "Click to teleport!", Material.LIGHT_BLUE_WOOL, "execute as @S run function monumenta:lobbies/send_one/d4", 1, 3));
		LOCATIONS_TREE.add(new TeleportEntry(38, "Yellow", "D5Access", "Click to teleport!", Material.YELLOW_WOOL, "execute as @S run function monumenta:lobbies/send_one/d5", 1, 3));
		LOCATIONS_TREE.add(new TeleportEntry(24, "Willows", "DB1Access", "Click to teleport!", Material.JUNGLE_LEAVES, "execute as @S run function monumenta:lobbies/send_one/db1", 1, 3));
		LOCATIONS_TREE.add(new TeleportEntry(32, "Reverie", "DCAccess", "Click to teleport!", Material.FIRE_CORAL, "execute as @S run function monumenta:lobbies/send_one/dc", 1, 3));
		LOCATIONS_TREE.add(new TeleportEntry(33, "Sanctum", "DS1Access", "Click to teleport!", Material.GRASS_BLOCK, "execute as @S run function monumenta:lobbies/send_one/ds1", 1, 3));

		LOCATIONS_TREE.add(new TeleportEntry(8, "Switch to Page Layout", null, "", Material.PISTON, "switch", 1, 4));
		LOCATIONS_TREE.add(new TeleportEntry(0, "Back to Main Menu", null, "", Material.OBSERVER, "", 1, 4));
		LOCATIONS_TREE.add(new TeleportEntry(4, "Mistport", "Quest101", "Click to teleport!", Material.SAND, "transferserver region_2 -2456.0 56.5 1104.0 90 0", 12, 4));
		LOCATIONS_TREE.add(new TeleportEntry(20, "Lime", "D6Access", "Click to teleport!", Material.LIME_WOOL, "execute as @S run function monumenta:lobbies/send_one/d6", 1, 4));
		LOCATIONS_TREE.add(new TeleportEntry(21, "Pink", "D7Access", "Click to teleport!", Material.PINK_WOOL, "execute as @S run function monumenta:lobbies/send_one/d7", 1, 4));
		LOCATIONS_TREE.add(new TeleportEntry(29, "Gray", "D8Access", "Click to teleport!", Material.GRAY_WOOL, "execute as @S run function monumenta:lobbies/send_one/d8", 1, 4));
		LOCATIONS_TREE.add(new TeleportEntry(30, "Light Gray", "D9Access", "Click to teleport!", Material.LIGHT_GRAY_WOOL, "execute as @S run function monumenta:lobbies/send_one/d9", 1, 4));
		LOCATIONS_TREE.add(new TeleportEntry(38, "Cyan", "D10Access", "Click to teleport!", Material.CYAN_WOOL, "execute as @S run function monumenta:lobbies/send_one/d10", 1, 4));
		LOCATIONS_TREE.add(new TeleportEntry(39, "Purple", "D11Access", "Click to teleport!", Material.PURPLE_WOOL, "execute as @S run function monumenta:lobbies/send_one/d11", 1, 4));
		LOCATIONS_TREE.add(new TeleportEntry(23, "Teal", "DTLAccess", "Click to teleport!", Material.CYAN_CONCRETE_POWDER, "execute as @S run function monumenta:lobbies/send_one/dtl", 1, 4));
		LOCATIONS_TREE.add(new TeleportEntry(24, "Shifting City", "DRL2Access", "Click to teleport!", Material.PRISMARINE_BRICKS, "execute as @S run function monumenta:lobbies/send_one/drl2", 1, 4));
		LOCATIONS_TREE.add(new TeleportEntry(32, "The Fallen Forum", "DFFAccess", "Click to teleport!", Material.BOOKSHELF, "execute as @S run function monumenta:lobbies/send_one/dff", 1, 4));
		
		//LOCATIONS_TREE.add(new TeleportEntry(8, "Switch to Page Layout", null, "", Material.PISTON, "switch", 1, 5));
		//LOCATIONS_TREE.add(new TeleportEntry(0, "Back to Main Menu", null, "", Material.OBSERVER, "", 1, 5));
		//LOCATIONS_TREE.add(new TeleportEntry(31, "Bih you thought", null, "Click to teleport!", Material.BARRIER, "nice try", 1, 5));
	}
	


	public OrinSinglePageGUI(Player player, String[] args) {
		super(player, args);
	}

	/*
	 * You MUST call this method from the SinglePageGUIManager. The example
	 * call for this example GUI is there for reference.
	 */
	@Override
	public void registerCommand() {
		registerCommand("openteleportergui");
	}

	@Override
	public SinglePageGUI constructGUI(Player player, String[] args) {
		return new OrinSinglePageGUI(player, args);
	}

	@Override
	public Inventory getInventory(Player player, String[] args) {
		Inventory baseInventory;
		
		//in case of someone messing with the score, this should fix it to the correct init values
		int scoreboardFixer = ScoreboardUtils.getScoreboardValue(player, "OrinPage");
		switch (scoreboardFixer) {
		case 0:
		case 1:
			break;
		case 2:
		case 3:
		case 4:
		case 5:
			ScoreboardUtils.setScoreboardValue(player, "OrinPage", 1);
			break;
		default:
			ScoreboardUtils.setScoreboardValue(player, "OrinPage", 0);
		}
		int pageChoice = ScoreboardUtils.getScoreboardValue(player, "OrinPage");


		//Create inventory and call the builder

		baseInventory = Bukkit.createInventory(null, ROWS*COLUMNS, "Teleportation Choices");

		setLayout(pageChoice, player, baseInventory);

		return baseInventory;
	}

	@Override
	public void processClick(InventoryClickEvent event) {
		Player player = null;
		Inventory inventory = event.getClickedInventory();
		if (event.getWhoClicked() instanceof Player) {
			player = (Player) event.getWhoClicked();
		} else {
			return;
		}
		ItemStack clickedItem = event.getCurrentItem();
		if (event.getClickedInventory() != mInventory) {
			return;
		}
		int activeTreeInv = ScoreboardUtils.getScoreboardValue(player, "OrinPage");

		if (clickedItem != null && clickedItem.getType() != Material.BLACK_STAINED_GLASS_PANE && !event.isShiftClick()) {
			int chosenSlot = event.getSlot();
			if (activeTreeInv == 0) {
				for (TeleportEntry location : LOCATIONS_PAGE) {
					if (location.mSlot == chosenSlot) {
						if (location.mCommand == "switch") {
							setLayout(1, player, inventory);
							return;
						} else {
							completeCommand(player, location.mCommand);
							player.closeInventory();
							return;
						}
					}
				}
			}
			if (activeTreeInv >= 1 && activeTreeInv <= 5) {
				for (TeleportEntry location : LOCATIONS_TREE) {
					if (location.mSlot == chosenSlot && location.mPage == activeTreeInv && location.mPage == 1) {
						if (location.mCommand == "switch") {
							setLayout(0, player, inventory);
							return;
						}
						setTreeInventory(Integer.parseInt(location.mCommand), player, inventory);
						return;
					}

					if (location.mPage == activeTreeInv) {
						if (location.mSlot == chosenSlot) {
							if (location.mSlot == 0) {
								setTreeInventory(1, player, inventory);
								return;
							} else if (location.mCommand == "switch") {
								setLayout(0, player, inventory);
							} else {
								completeCommand(player, location.mCommand);
								player.closeInventory();
								com.playmonumenta.plugins.utils.ScoreboardUtils.setScoreboardValue(player, "OrinPage", 1);
								return;
							}
						}

					}
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
			String finalCommand = command.replace("@S", player.getDisplayName());
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
		} else {
			String finalCommand = command.replace("@S", player.getUniqueId().toString());
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
		}
	}

	public ItemStack createCustomItem(TeleportEntry location) {
		ItemStack newItem = new ItemStack(location.mType, 1);
		ItemMeta meta = newItem.getItemMeta();
		meta.setDisplayName(ChatColor.GOLD + location.mName);
		ArrayList<String> lore = new ArrayList<String>();
		if (location.mLore != "") {
			lore.add(location.mLore);
		}
		meta.setLore(lore);
		newItem.setItemMeta(meta);
		return newItem;
	}
	
	public Inventory setLayout(int layout, Player player, Inventory inventory) {
		if (layout != 0) {
			setTreeInventory(1, player, inventory);
		} else {
			inventory.clear();
			for (TeleportEntry location : LOCATIONS_PAGE) {
				if (location.mScoreboard == null || ScoreboardUtils.getScoreboardValue(player, location.mScoreboard) >= location.mScoreRequired) {
					ItemStack newItem = createCustomItem(location);
					inventory.setItem(location.mSlot, newItem);
				}
			}

			for (int i = 0; i < (ROWS*COLUMNS); i++) {
				if (inventory.getItem(i) == null) {
					inventory.setItem(i,new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1));
				}
			}
			com.playmonumenta.plugins.utils.ScoreboardUtils.setScoreboardValue(player, "OrinPage", 0);
		}
		return inventory;
	}

	public Inventory setTreeInventory(int page, Player player, Inventory inventory) {
		//wipe existing items and replace with new active inventory
		inventory.clear();
		for (TeleportEntry location : LOCATIONS_TREE) {
			if (location.mScoreboard == null || ScoreboardUtils.getScoreboardValue(player, location.mScoreboard) >= location.mScoreRequired) {
				if (location.mPage == page) {
					ItemStack newItem = createCustomItem(location);
					inventory.setItem(location.mSlot, newItem);
				}
			}
		}
		for (int i = 0; i < (ROWS*COLUMNS); i++) {
			if (inventory.getItem(i) == null) {
				inventory.setItem(i,new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1));
			}
		}
		com.playmonumenta.plugins.utils.ScoreboardUtils.setScoreboardValue(player, "OrinPage", page);
		return inventory;
	}
	

}
