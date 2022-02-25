package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;

public class PEBCustomInventory extends CustomInventory {
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

	public static class PebItem {
		int mPage;
		int mSlot;
		String mName;
		String mLore;
		Material mType;
		String mCommand;
		ChatColor mChatColor;
		Boolean mCloseAfter;

		public PebItem(int pg, int sl, String n, String l, ChatColor cc, Material t, String cmd, Boolean closeAfter) {
			mSlot = sl;
			mName = n;
			mLore = l;
			mType = t;
			mCommand = cmd;
			mPage = pg;
			mChatColor = cc;
			mCloseAfter = closeAfter;
		}

	}

	private static final ArrayList<PebItem> PEB_ITEMS = new ArrayList<>();

	static {
		//If the command is internal to the GUI, closeAfter is ignored. Otherwise, the GUI abides by that boolean.

		//Common items for all but main menu are "page 0"
		//Page 1 is the top level menu, 2-9 saved for the next level of menus.
		//Pages 10 and beyond are used for implementation of specialized menus.

		PEB_ITEMS.add(new PebItem(0, 0, "Back to Main Menu", "Returns you to page 1.", ChatColor.GOLD, Material.OBSERVER, "page 1", false));
		PEB_ITEMS.add(new PebItem(0, 8, "Exit PEB", "Exits this menu.", ChatColor.GOLD, Material.RED_CONCRETE, "exit", false));
		PEB_ITEMS.add(new PebItem(0, 45, "Delete P.E.B.s ✗",
				"Click to remove P.E.B.s from your inventory.", ChatColor.LIGHT_PURPLE,
				Material.FLINT_AND_STEEL, "clickable peb_delete", true));

		//page 1: main menu
		PEB_ITEMS.add(new PebItem(1, 0, "", "", ChatColor.LIGHT_PURPLE, FILLER, "", false));
		PEB_ITEMS.add(new PebItem(1, 4, "Main Menu",
				"A list of commonly used options, along with menu buttons to reach the full lists.", ChatColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, "", false));
		PEB_ITEMS.add(new PebItem(1, 20, "Filtered Pickup and Disabled Drop",
				"Click to choose your pickup and disabled drop preferences.", ChatColor.LIGHT_PURPLE,
				Material.DIRT, "page 20", false));
		PEB_ITEMS.add(new PebItem(1, 21, "Toggle Darksight",
				"Click to toggle whether Darksight provides Night Vision", ChatColor.LIGHT_PURPLE,
				Material.LANTERN, "execute as @S run function monumenta:mechanisms/darksight_toggle", false));
		PEB_ITEMS.add(new PebItem(1, 23, "Dailies",
				"Click to see what daily content you have and haven't done today.", ChatColor.LIGHT_PURPLE,
				Material.ACACIA_BOAT, "clickable peb_dailies", true));
		PEB_ITEMS.add(new PebItem(1, 24, "Dungeon Instances",
				"Click to view what dungeon instances you have open, and how old they are.", ChatColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, "clickable peb_dungeoninfo", true));

		PEB_ITEMS.add(new PebItem(1, 37, "Player Information",
				"Details about Housing, Dailies, and other player-focused options.", ChatColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, "page 2", false));
		PEB_ITEMS.add(new PebItem(1, 39, "Toggle-able Options",
				"Inventory Sort, Filtered Pickup, and more toggleable choices.", ChatColor.LIGHT_PURPLE,
				Material.LEVER, "page 3", false));
		PEB_ITEMS.add(new PebItem(1, 41, "Server Information",
				"Information such as how to use the PEB and random tips.", ChatColor.LIGHT_PURPLE,
				Material.DISPENSER, "page 4", false));
		PEB_ITEMS.add(new PebItem(1, 43, "Book Skins",
				"Change the color of the cover on your P.E.B.", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, "page 5", false));


		//page 2: Player Info
		PEB_ITEMS.add(new PebItem(2, 4, "Player Information",
				"", ChatColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, "", false));
		PEB_ITEMS.add(new PebItem(2, 20, "Housing",
				"Click to view housing information.", ChatColor.LIGHT_PURPLE,
				Material.OAK_DOOR, "clickable peb_housing", true));
		PEB_ITEMS.add(new PebItem(2, 22, "Class",
				"Click to view your class and skills.", ChatColor.LIGHT_PURPLE,
				Material.STONE_SWORD, "clickable peb_class", true));
		PEB_ITEMS.add(new PebItem(2, 24, "Dungeon Instances",
				"Click to view what dungeon instances you have open, and how old they are.", ChatColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, "clickable peb_dungeoninfo", true));
		PEB_ITEMS.add(new PebItem(2, 38, "Patron",
				"Click to view patron information. Use /donate to learn about donating.", ChatColor.LIGHT_PURPLE,
				Material.GLOWSTONE_DUST, "clickable peb_patroninfo", true));
		PEB_ITEMS.add(new PebItem(2, 40, "Dailies",
				"Click to see what daily content you have and haven't done today.", ChatColor.LIGHT_PURPLE,
				Material.ACACIA_BOAT, "clickable peb_dailies", true));
		PEB_ITEMS.add(new PebItem(2, 42, "Item Stats",
				"Click to view your current item stats and compare items.", ChatColor.LIGHT_PURPLE,
				Material.KNOWLEDGE_BOOK, "playerstats", true));

		//page 3: Toggle-able Options
		PEB_ITEMS.add(new PebItem(3, 4, "Toggleable Options",
				"", ChatColor.LIGHT_PURPLE,
				Material.LEVER, "", false));
		PEB_ITEMS.add(new PebItem(3, 19, "Self Particles",
				"Click to toggle self particles.", ChatColor.LIGHT_PURPLE,
				Material.FIREWORK_STAR, "clickable peb_selfparticles", false));
		PEB_ITEMS.add(new PebItem(3, 20, "Glowing options",
				"Click to choose your preferences for the \"glowing\" effect.", ChatColor.LIGHT_PURPLE,
				Material.SPECTRAL_ARROW, "page 30", false));
		PEB_ITEMS.add(new PebItem(3, 21, "Show name on patron buff announcement.",
				"Toggles whether the player has their IGN in the buff announcement when they"
						+ " activate " + ChatColor.GOLD + "Patreon " + ChatColor.LIGHT_PURPLE + "buffs.", ChatColor.LIGHT_PURPLE,
				Material.GLOWSTONE, "clickable toggle_patron_buff_thank", false));
		PEB_ITEMS.add(new PebItem(3, 23, "Inventory Drink",
				"Click to toggle drinking potions with a right click in any inventory.", ChatColor.LIGHT_PURPLE,
				Material.GLASS_BOTTLE, "clickable peb_tid", false));
		PEB_ITEMS.add(new PebItem(3, 24, "Filtered Pickup and Disabled Drop",
				"Click to choose your pickup and disabled drop preferences.", ChatColor.LIGHT_PURPLE,
				Material.DIRT, "page 20", false));
		PEB_ITEMS.add(new PebItem(3, 25, "Compass Particles",
				"Click to toggle a trail of guiding particles when following the quest compass.", ChatColor.LIGHT_PURPLE,
				Material.COMPASS, "clickable peb_comp_particles", false));
		PEB_ITEMS.add(new PebItem(3, 29, "Rocket Jump",
				"Click to enable or disable Rocket Jump", ChatColor.LIGHT_PURPLE,
				Material.FIREWORK_ROCKET, "page 31", false));
		PEB_ITEMS.add(new PebItem(3, 33, "Block Interactions",
				"Click to disable or enable interactions with blocks (looms, crafting tables, beds, etc.)", ChatColor.LIGHT_PURPLE,
				Material.LOOM, "blockinteractions", false));
		PEB_ITEMS.add(new PebItem(3, 37, "Death Sort",
				"Click to toggle death sorting, which attempts to return items dropped on death to the slot they were in prior to death.", ChatColor.LIGHT_PURPLE,
				Material.CHEST, "clickable peb_toggle_dso", false));
		PEB_ITEMS.add(new PebItem(3, 38, "Toggle Darksight",
				"Click to toggle whether Darksight provides Night Vision", ChatColor.LIGHT_PURPLE,
				Material.LANTERN, "execute as @S run function monumenta:mechanisms/darksight_toggle", false));
		PEB_ITEMS.add(new PebItem(3, 39, "Toggle Radiant",
				"Click to toggle whether Radiant provides Night Vision.", ChatColor.LIGHT_PURPLE,
				Material.SOUL_LANTERN, "execute as @S run function monumenta:mechanisms/radiant_toggle", false));
		PEB_ITEMS.add(new PebItem(3, 41, "Offhand Swapping",
				"Click to toggle whether pressing your swap key will be fully cancelled or only cancelled when a spellcast does so", ChatColor.LIGHT_PURPLE,
				Material.SHIELD, "toggleswap", false));
		PEB_ITEMS.add(new PebItem(3, 42, "Spawner Equipment",
				"Click to toggle whether mob equipment is displayed in spawners (significantly decreases FPS in many areas)", ChatColor.LIGHT_PURPLE,
				Material.IRON_CHESTPLATE, "clickable peb_spawnerequipment", false));
		PEB_ITEMS.add(new PebItem(3, 43, "Virtual Firmament",
				"Click to toggle Virtual Firmament, which visually turns your Firmament into a stack of blocks for faster placement.", ChatColor.LIGHT_PURPLE,
				Material.PRISMARINE, "virtualfirmament", false));

		//page 4: Server Info
		PEB_ITEMS.add(new PebItem(4, 4, "Server Information",
				"", ChatColor.LIGHT_PURPLE,
				Material.DISPENSER, "", false));
		PEB_ITEMS.add(new PebItem(4, 20, "P.E.B. Introduction",
				"Click to hear the P.E.B. Introduction.", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, "clickable peb_intro", true));
		PEB_ITEMS.add(new PebItem(4, 24, "Get a random tip!",
				"Click to get a random tip!", ChatColor.LIGHT_PURPLE,
				Material.REDSTONE_TORCH, "clickable peb_tip", true));

		//page 5: Book Skins
		PEB_ITEMS.add(new PebItem(5, 4, "Book Skins",
				"", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, "", false));
		PEB_ITEMS.add(new PebItem(5, 40, "Wool Colors",
				"Click to jump to a page of wool colors.", ChatColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, "page 10", false));
		PEB_ITEMS.add(new PebItem(5, 19, "Enchanted Book",
				"Click to change skin to Enchanted Book. (Default)", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, "clickable peb_skin_enchantedbook", true));
		PEB_ITEMS.add(new PebItem(5, 21, "Regal",
				"Click to change skin to Regal.", ChatColor.LIGHT_PURPLE,
				Material.YELLOW_CONCRETE, "clickable peb_skin_regal", true));
		PEB_ITEMS.add(new PebItem(5, 23, "Crimson King",
				"Upon the ancient powers creep...", ChatColor.DARK_RED,
				Material.RED_TERRACOTTA, "clickable peb_skin_ck", true));
		PEB_ITEMS.add(new PebItem(5, 25, "Rose",
				"Red like roses!", ChatColor.RED,
				Material.RED_CONCRETE, "clickable peb_skin_rose", true));

		//page 10: Wool book skins
		PEB_ITEMS.add(new PebItem(10, 9, "Back to Book Skins",
				"", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, "page 5", false));
		PEB_ITEMS.add(new PebItem(10, 4, "Wool Skins",
				"", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, "", false));
		PEB_ITEMS.add(new PebItem(10, 11, "White",
				"Click to change skin to White.", ChatColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, "clickable peb_skin_white", true));
		PEB_ITEMS.add(new PebItem(10, 12, "Orange",
				"Click to change skin to Orange.", ChatColor.LIGHT_PURPLE,
				Material.ORANGE_WOOL, "clickable peb_skin_orange", true));
		PEB_ITEMS.add(new PebItem(10, 20, "Magenta",
				"Click to change skin to Magenta.", ChatColor.LIGHT_PURPLE,
				Material.MAGENTA_WOOL, "clickable peb_skin_magenta", true));
		PEB_ITEMS.add(new PebItem(10, 21, "Light Blue",
				"Click to change skin to Light Blue.", ChatColor.LIGHT_PURPLE,
				Material.LIGHT_BLUE_WOOL, "clickable peb_skin_lightblue", true));
		PEB_ITEMS.add(new PebItem(10, 29, "Yellow",
				"Click to change skin to Yellow.", ChatColor.LIGHT_PURPLE,
				Material.YELLOW_WOOL, "clickable peb_skin_yellow", true));
		PEB_ITEMS.add(new PebItem(10, 30, "Lime",
				"Click to change skin to Lime.", ChatColor.LIGHT_PURPLE,
				Material.LIME_WOOL, "clickable peb_skin_lime", true));
		PEB_ITEMS.add(new PebItem(10, 38, "Pink",
				"Click to change skin to Pink.", ChatColor.LIGHT_PURPLE,
				Material.PINK_WOOL, "clickable peb_skin_pink", true));
		PEB_ITEMS.add(new PebItem(10, 39, "Gray",
				"Click to change skin to Gray.", ChatColor.LIGHT_PURPLE,
				Material.GRAY_WOOL, "clickable peb_skin_gray", true));
		PEB_ITEMS.add(new PebItem(10, 14, "Light Gray",
				"Click to change skin to Light Gray.", ChatColor.LIGHT_PURPLE,
				Material.LIGHT_GRAY_WOOL, "clickable peb_skin_lightgray", true));
		PEB_ITEMS.add(new PebItem(10, 15, "Cyan",
				"Click to change skin to Cyan.", ChatColor.LIGHT_PURPLE,
				Material.CYAN_WOOL, "clickable peb_skin_cyan", true));
		PEB_ITEMS.add(new PebItem(10, 23, "Purple",
				"Click to change skin to Purple.", ChatColor.LIGHT_PURPLE,
				Material.PURPLE_WOOL, "clickable peb_skin_purple", true));
		PEB_ITEMS.add(new PebItem(10, 24, "Blue",
				"Click to change skin to Blue.", ChatColor.LIGHT_PURPLE,
				Material.BLUE_WOOL, "clickable peb_skin_blue", true));
		PEB_ITEMS.add(new PebItem(10, 32, "Brown",
				"Click to change skin to Brown.", ChatColor.LIGHT_PURPLE,
				Material.BROWN_WOOL, "clickable peb_skin_brown", true));
		PEB_ITEMS.add(new PebItem(10, 33, "Green",
				"Click to change skin to Green.", ChatColor.LIGHT_PURPLE,
				Material.GREEN_WOOL, "clickable peb_skin_green", true));
		PEB_ITEMS.add(new PebItem(10, 41, "Red",
				"Click to change skin to Red.", ChatColor.LIGHT_PURPLE,
				Material.RED_WOOL, "clickable peb_skin_red", true));
		PEB_ITEMS.add(new PebItem(10, 42, "Black",
				"Click to change skin to Black.", ChatColor.LIGHT_PURPLE,
				Material.BLACK_WOOL, "clickable peb_skin_black", true));

		//page 20: Pickup and Disable Drop
		PEB_ITEMS.add(new PebItem(20, 0, "Back to Toggleable Options",
				"", ChatColor.LIGHT_PURPLE,
				Material.OBSERVER, "page 3", false));
		PEB_ITEMS.add(new PebItem(20, 4, "Pickup and Disable Drop Settings",
				"Choose the appropriate level of pickup filter and drop filter below.", ChatColor.LIGHT_PURPLE,
				Material.PRISMARINE_CRYSTALS, "", false));
		PEB_ITEMS.add(new PebItem(20, 11, "Disable Drop:",
				"", ChatColor.LIGHT_PURPLE,
				Material.BLACK_CONCRETE, "", false));
		PEB_ITEMS.add(new PebItem(20, 19, "None",
				"Disable no drops, the vanilla drop behavior.", ChatColor.LIGHT_PURPLE,
				Material.BARRIER, "disabledrop none", false));
		PEB_ITEMS.add(new PebItem(20, 20, "Holding",
				"Disable dropping of only held items.", ChatColor.LIGHT_PURPLE,
				Material.WOODEN_PICKAXE, "disabledrop holding", false));
		PEB_ITEMS.add(new PebItem(20, 21, "Equipped",
				"Disable dropping of only equipped items.", ChatColor.LIGHT_PURPLE,
				Material.LEATHER_HELMET, "disabledrop equipped", false));
		PEB_ITEMS.add(new PebItem(20, 28, "Tiered",
				"Disable dropping of tiered items.", ChatColor.LIGHT_PURPLE,
				Material.OAK_STAIRS, "disabledrop tiered", false));
		PEB_ITEMS.add(new PebItem(20, 29, "Lore",
				"Disable the drop of items with custom lore.", ChatColor.LIGHT_PURPLE,
				Material.LECTERN, "disabledrop lore", false));
		PEB_ITEMS.add(new PebItem(20, 30, "Interesting",
				"Disable the dropping of anything that matches the default pickup filter of interesting items.", ChatColor.LIGHT_PURPLE,
				Material.GOLD_NUGGET, "disabledrop interesting", false));
		PEB_ITEMS.add(new PebItem(20, 38, "All",
				"Disable all drops.", ChatColor.LIGHT_PURPLE,
				Material.DIRT, "disabledrop all", false));

		PEB_ITEMS.add(new PebItem(20, 15, "Pickup Filter:",
				"", ChatColor.LIGHT_PURPLE,
				Material.WHITE_CONCRETE, "", false));
		PEB_ITEMS.add(new PebItem(20, 23, "Lore",
				"Only pick up items that have custom lore.", ChatColor.LIGHT_PURPLE,
				Material.LECTERN, "pickup lore", false));
		PEB_ITEMS.add(new PebItem(20, 25, "Interesting",
				"Only pick up items are of interest for the adventuring player, like arrows, torches, and anything with custom lore.", ChatColor.LIGHT_PURPLE,
				Material.GOLD_NUGGET, "pickup interesting", false));
		PEB_ITEMS.add(new PebItem(20, 41, "All",
				"Pick up anything and everything, matching vanilla functionality.", ChatColor.LIGHT_PURPLE,
				Material.DIRT, "pickup all", false));
		PEB_ITEMS.add(new PebItem(20, 43, "Threshold",
				"Set the minimum size of a stack of uninteresting items to pick up.", ChatColor.LIGHT_PURPLE,
				Material.OAK_SIGN, "threshold", false));

		//page 30: Glowing options
		PEB_ITEMS.add(new PebItem(30, 0, "Back to Toggleable Options",
				"", ChatColor.LIGHT_PURPLE,
				Material.OBSERVER, "page 3", false));
		PEB_ITEMS.add(new PebItem(30, 4, "Glowing Settings",
				"Choose for which entity types the glowing effect may be shown. " +
						"If an entity fits into more than one category (e.g. a boss matches both 'mobs' and 'bosses'), it will glow if any of the matching options are enabled.", ChatColor.LIGHT_PURPLE,
				Material.SPECTRAL_ARROW, "", false));
		PEB_ITEMS.add(new PebItem(30, 22, "Enable All",
				"Enable glowing for all entities (default).", ChatColor.LIGHT_PURPLE,
				Material.GOLD_INGOT, "glowing enable all", false));
		PEB_ITEMS.add(new PebItem(30, 28, "Players",
				"Toggle glowing for players.", ChatColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, "glowing toggle players", false));
		PEB_ITEMS.add(new PebItem(30, 29, "Mobs",
				"Toggle glowing for mobs.", ChatColor.LIGHT_PURPLE,
				Material.ZOMBIE_HEAD, "glowing toggle mobs", false));
		PEB_ITEMS.add(new PebItem(30, 30, "Bosses",
				"Toggle glowing for bosses. Note that pretty much all bosses are mobs, soa re affected by that option as well.", ChatColor.LIGHT_PURPLE,
				Material.DRAGON_HEAD, "glowing toggle bosses", false));
		PEB_ITEMS.add(new PebItem(30, 32, "Invisible Entities",
				"Toggle glowing for invisible entities.", ChatColor.LIGHT_PURPLE,
				Material.GLASS, "glowing toggle invisible", false));
		PEB_ITEMS.add(new PebItem(30, 33, "Experience Orbs",
				"Toggle glowing for experience orbs.", ChatColor.LIGHT_PURPLE,
				Material.EXPERIENCE_BOTTLE, "glowing toggle experience_orbs", false));
		PEB_ITEMS.add(new PebItem(30, 34, "Miscellaneous",
				"Toggle glowing for miscellaneous entities, i.e. entities that don't fit into any other category.", ChatColor.LIGHT_PURPLE,
				Material.IRON_NUGGET, "glowing toggle misc", false));
		PEB_ITEMS.add(new PebItem(30, 40, "Disable All",
				"Disable glowing for all entities.", ChatColor.LIGHT_PURPLE,
				Material.DIRT, "glowing disable all", false));

		//page 39: Rocket Jump Option
		PEB_ITEMS.add(new PebItem(31, 0, "Back to Toggleable Options",
			"", ChatColor.LIGHT_PURPLE,
			Material.OBSERVER, "page 3", false));
		PEB_ITEMS.add(new PebItem(31, 4, "Rocket Jump Settings",
			"Choose how Unstable Amalgam should interact with you.", ChatColor.LIGHT_PURPLE,
			Material.FIREWORK_ROCKET, "", false));
		PEB_ITEMS.add(new PebItem(31, 20, "Enable All",
			"Enable to rocket jump from ANY Unstable Amalgam.", ChatColor.LIGHT_PURPLE,
			Material.FIREWORK_STAR, "scoreboard players set @S RocketJumper 100", false));
		PEB_ITEMS.add(new PebItem(31, 22, "Enable your",
			"Enable to rocket jump only from YOUR Unstable Amalgam.", ChatColor.LIGHT_PURPLE,
			Material.CLAY_BALL, "scoreboard players set @S RocketJumper 1", false));
		PEB_ITEMS.add(new PebItem(31, 24, "Disable all",
			"Disable to rocket jump from ANY Unstable Amalgam.", ChatColor.LIGHT_PURPLE,
			Material.SKELETON_SKULL, "scoreboard players set @S RocketJumper 0", false));


	}

	public PEBCustomInventory(Player player) {
		super(player, 54, player.getName() + "'s P.E.B");

		ScoreboardUtils.setScoreboardValue(player, "PEBPage", 1);

		setLayout(mInventory, 1, player);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		Player player;
		if (event.getWhoClicked() instanceof Player) {
			player = (Player) event.getWhoClicked();
		} else {
			return;
		}
		ItemStack clickedItem = event.getCurrentItem();
		if (event.getClickedInventory() != mInventory) {
			return;
		}
		int currentPage = ScoreboardUtils.getScoreboardValue(player, "PEBPage");
		if (clickedItem != null && clickedItem.getType() != FILLER && !event.isShiftClick()) {
			int chosenSlot = event.getSlot();
			for (PebItem item : PEB_ITEMS) {
				if (item.mSlot == chosenSlot && item.mPage == currentPage) {
					completeCommand(player, item);
				}
				if (item.mSlot == chosenSlot && item.mPage == 0) {
					completeCommand(player, item);
				}
			}
		}
	}

	public Boolean isInternalCommand(String command) {
		return command.equals("exit") || command.startsWith("page") || command.equals("threshold");
	}

	public Boolean isPlayerCommand(String command) {
		return command.startsWith("clickable") ||
				       command.startsWith("pickup") ||
				       command.equals("toggleswap") ||
				       command.equals("virtualfirmament") ||
				       command.startsWith("glowing") ||
				       command.startsWith("disabledrop") ||
				       command.startsWith("playerstats") ||
				       command.equals("blockinteractions");
	}

	public void runInternalCommand(Player player, PebItem item) {
		if (item.mCommand.startsWith("page")) {
			int newPageValue = Integer.parseInt(item.mCommand.split(" ")[1]);
			setLayout(mInventory, newPageValue, player);
		} else if (item.mCommand.startsWith("exit")) {
			player.closeInventory();
		} else if (item.mCommand.equals("threshold")) {
			player.closeInventory();
			callSignUI(player);
		}
	}

	public void completeCommand(Player player, PebItem item) {
		if (item.mCommand.isEmpty()) {
			return;
		}
		if (isInternalCommand(item.mCommand)) {
			runInternalCommand(player, item);
		} else if (isPlayerCommand(item.mCommand)) {
			if (item.mCloseAfter) {
				player.closeInventory();
			}
			player.performCommand(item.mCommand);
		} else {
			String finalCommand = item.mCommand.replace("@S", player.getName());
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
			if (item.mCloseAfter) {
				player.closeInventory();
			}
		}
	}

	public void callSignUI(Player target) {
		SignUtils.Menu menu = SignUtils.newMenu(
				new ArrayList<>(Arrays.asList("", "~~~~~~~~~~~", "Input a number", "from 1-65 above.")))
	            .reopenIfFail(false)
	            .response((player, strings) -> {
					int inputVal;
					try {
						inputVal = Integer.parseInt(strings[0]);
					} catch (Exception e) {
						new BukkitRunnable() {
							@Override
							public void run() {
								player.sendMessage("Input is not an integer.");
							}
						}.runTaskLater(Plugin.getInstance(), 2);
						return false;
					}
					if (inputVal >= 1 && inputVal <= 65) {
						new BukkitRunnable() {
							@Override
							public void run() {
								player.performCommand("pickup threshold " + strings[0]);
							}
						}.runTaskLater(Plugin.getInstance(), 2);
					    return false;
					} else {
						new BukkitRunnable() {
							@Override
							public void run() {
								player.sendMessage("Input is not with the bounds of 1 - 65.");
							}
						}.runTaskLater(Plugin.getInstance(), 2);
					}
					return true;
	            });

	    menu.open(target);
	}

	public static ItemStack createCustomItem(PebItem item, Player player) {
		ItemStack newItem = new ItemStack(item.mType, 1);
		if (item.mType == Material.PLAYER_HEAD) {
			SkullMeta meta = (SkullMeta) newItem.getItemMeta();
			meta.setOwningPlayer(player);
			newItem.setItemMeta(meta);
		}
		ItemMeta meta = newItem.getItemMeta();
		if (!item.mName.isEmpty()) {
			meta.displayName(Component.text(item.mName, NamedTextColor.WHITE)
				                 .decoration(TextDecoration.ITALIC, false));
		}
		ChatColor defaultColor = (item.mChatColor != null) ? item.mChatColor : ChatColor.LIGHT_PURPLE;
		if (!item.mLore.isEmpty()) {
			GUIUtils.splitLoreLine(meta, item.mLore, 30, defaultColor, true);
		}
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		newItem.setItemMeta(meta);
		ItemUtils.setPlainName(newItem);
		return newItem;
	}

	public static void setLayout(Inventory inventory, int page, Player player) {

		inventory.clear();
		for (PebItem item : PEB_ITEMS) {
			if (item.mPage == 0) {
				inventory.setItem(item.mSlot, createCustomItem(item, player));
			} //intentionally not else, so overrides can happen
			if (item.mPage == page) {
				inventory.setItem(item.mSlot, createCustomItem(item, player));
			}
		}

		for (int i = 0; i < 54; i++) {
			if (inventory.getItem(i) == null) {
				inventory.setItem(i, new ItemStack(FILLER, 1));
			}
		}
		ScoreboardUtils.setScoreboardValue(player, "PEBPage", page);
	}
}
