package com.playmonumenta.plugins.custominventories;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.goncalomb.bukkit.mylib.utils.CustomInventory;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;

public class PEBCustomInventory extends CustomInventory {
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

	public static class PebItem {
		int mPage = 1;
		int mSlot;
		String mName;
		String mLore;
		Material mType;
		String mCommand;
		ChatColor mChatColor = null;

		public PebItem(int pg, int sl, String n, String l, ChatColor cc, Material t, String cmd) {
			mSlot = sl;
			mName = n;
			mLore = l;
			mType = t;
			mCommand = cmd;
			mPage = pg;
			mChatColor = cc;
		}

	}

	private static ArrayList<PebItem> PEB_ITEMS = new ArrayList<>();

	static {
		//Common items for all but main menu are "page 0"
		//Page 1 is the top level menu, 2-9 saved for the next level of menus.
		//Pages 10 and beyond are used for implementation of specialized menus.

		PEB_ITEMS.add(new PebItem(0, 0, "Back to Main Menu", "Returns you to page 1.", ChatColor.GOLD, Material.OBSERVER, "page 1"));
		PEB_ITEMS.add(new PebItem(0, 8, "Exit PEB", "Exits this menu.", ChatColor.GOLD, Material.RED_CONCRETE, "exit"));
		PEB_ITEMS.add(new PebItem(0, 45, "Delete P.E.B.s ✗",
				"Click to remove P.E.B.s from your inventory.", ChatColor.LIGHT_PURPLE,
				Material.FLINT_AND_STEEL, "clickable peb_delete"));

		//page 1: main menu
		PEB_ITEMS.add(new PebItem(1, 0, "", "", ChatColor.LIGHT_PURPLE, FILLER, ""));
		PEB_ITEMS.add(new PebItem(1, 11, "Player Information",
				"Details about Housing, Prestige, and other player-focused options.", ChatColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, "page 2"));
		PEB_ITEMS.add(new PebItem(1, 15, "Toggle-able Options",
				"Inventory Sort, Filtered Pickup, and more toggleable choices.", ChatColor.LIGHT_PURPLE,
				Material.LEVER, "page 3"));
		PEB_ITEMS.add(new PebItem(1, 38, "Server Information",
				"Information such as how to use the PEB, current shrine buffs, and random tips.", ChatColor.LIGHT_PURPLE,
				Material.DISPENSER, "page 4"));
		PEB_ITEMS.add(new PebItem(1, 42, "Book Skins",
				"Change the color of the cover on your P.E.B.", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, "page 5"));


		//page 2: Player Info
		PEB_ITEMS.add(new PebItem(2, 4, "Player Information",
				"", ChatColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, ""));
		PEB_ITEMS.add(new PebItem(2, 19, "Housing",
				"Click to view housing information.", ChatColor.LIGHT_PURPLE,
				Material.OAK_DOOR, "clickable peb_housing"));
		PEB_ITEMS.add(new PebItem(2, 21, "Prestige",
				"Click to view prestige and related unlocks.", ChatColor.LIGHT_PURPLE,
				Material.BRICK, "clickable peb_prestige"));
		PEB_ITEMS.add(new PebItem(2, 23, "Class",
				"Click to view your class and skills.", ChatColor.LIGHT_PURPLE,
				Material.STONE_SWORD, "clickable peb_class"));
		PEB_ITEMS.add(new PebItem(2, 25, "Dungeon Instances",
				"Click to view what dungeon instances you have open, and how old they are.", ChatColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, "clickable peb_dungeoninfo"));
		PEB_ITEMS.add(new PebItem(2, 37, "Patreon Shrine",
				"Click to view what buffs are active on the " + ChatColor.GOLD + "Patreon Shrine" + ChatColor.LIGHT_PURPLE + " and your settings.", ChatColor.LIGHT_PURPLE,
				Material.LANTERN, "clickable peb_shrineinfo"));
		PEB_ITEMS.add(new PebItem(2, 39, "Patron",
				"Click to view patron information. Use /donate to learn about donating.", ChatColor.LIGHT_PURPLE,
				Material.GLOWSTONE_DUST, "clickable peb_patroninfo"));
		PEB_ITEMS.add(new PebItem(2, 41, "Dailies",
				"Click to see what daily content you have and haven't done today.", ChatColor.LIGHT_PURPLE,
				Material.ACACIA_BOAT, "clickable peb_dailies"));

		//page 3: Toggle-able Options
		PEB_ITEMS.add(new PebItem(3, 4, "Toggleable Options",
				"", ChatColor.LIGHT_PURPLE,
				Material.LEVER, ""));
		PEB_ITEMS.add(new PebItem(3, 20, "Self Particles",
				"Click to toggle self particles.", ChatColor.LIGHT_PURPLE,
				Material.FIREWORK_STAR, "clickable peb_selfparticles"));
		PEB_ITEMS.add(new PebItem(3, 21, "UA Rocket Jumping",
				"Click to toggle rocket jumping with Unstable Arrows.", ChatColor.LIGHT_PURPLE,
				Material.FIREWORK_ROCKET, "clickable peb_uarj"));
		PEB_ITEMS.add(new PebItem(3, 22, "Show name on patron buff announcement.",
				"Toggles whether the player has their IGN in the buff announcement when they"
				+ " activate " + ChatColor.GOLD + "Patreon " + ChatColor.LIGHT_PURPLE + "buffs.", ChatColor.LIGHT_PURPLE,
				Material.GLOWSTONE, "clickable toggle_patron_buff_thank"));
		PEB_ITEMS.add(new PebItem(3, 23, "Inventory Drink",
				"Click to toggle drinking potions with a right click in any inventory.", ChatColor.LIGHT_PURPLE,
				Material.GLASS_BOTTLE, "clickable peb_tid"));
		PEB_ITEMS.add(new PebItem(3, 24, "Filtered Pickup",
				"Click to toggle the pickup of uninteresting items.", ChatColor.LIGHT_PURPLE,
				Material.DIRT, "pickup"));
		PEB_ITEMS.add(new PebItem(3, 25, "Filtered Pickup information",
				"Click to explain filtered pickup.", ChatColor.LIGHT_PURPLE,
				Material.BOOK, "clickable peb_filteredinfo"));
		PEB_ITEMS.add(new PebItem(3, 38, "Compass Particles",
				"Click to toggle a trail of guiding particles when following the quest compass.", ChatColor.LIGHT_PURPLE,
				Material.COMPASS, "clickable peb_comp_particles"));
		PEB_ITEMS.add(new PebItem(3, 39, "Death Sort",
				"Click to toggle death sorting, which attempts to return items dropped on death to the slot they were in prior to death.", ChatColor.LIGHT_PURPLE,
				Material.CHEST, "clickable peb_toggle_dso"));
		PEB_ITEMS.add(new PebItem(3, 40, "Toggle Darksight",
				"Click to toggle whether Darksight provides Night Vision", ChatColor.LIGHT_PURPLE,
				Material.LANTERN, "execute as @S run function monumenta:mechanisms/darksight_toggle"));
		PEB_ITEMS.add(new PebItem(3, 41, "Toggle Radiant",
				"Click to toggle whether Radiant provides Night Vision.", ChatColor.LIGHT_PURPLE,
				Material.SOUL_LANTERN, "execute as @S run function monumenta:mechanisms/radiant_toggle"));
		PEB_ITEMS.add(new PebItem(3, 42, "Offhand Swapping",
				"Click to toggle whether pressing your swap key will be fully cancelled or only cancelled when a spellcast does so", ChatColor.LIGHT_PURPLE,
				Material.SHIELD, "toggleswap"));
		PEB_ITEMS.add(new PebItem(3, 43, "Spawner Equipment",
				"Click to toggle whether mob equipment is displayed in spawners (significantly decreases FPS in many areas)", ChatColor.LIGHT_PURPLE,
				Material.IRON_CHESTPLATE, "clickable peb_spawnerequipment"));

		//page 4: Server Info
		PEB_ITEMS.add(new PebItem(4, 4, "Server Information",
				"", ChatColor.LIGHT_PURPLE,
				Material.DISPENSER, ""));
		PEB_ITEMS.add(new PebItem(4, 20, "P.E.B. Introduction",
				"Click to hear the P.E.B. Introduction.", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, "clickable peb_intro"));
		PEB_ITEMS.add(new PebItem(4, 24, "Get a random tip!",
				"Click to get a random tip!", ChatColor.LIGHT_PURPLE,
				Material.REDSTONE_TORCH, "clickable peb_tip"));

		//page 5: Book Skins
		PEB_ITEMS.add(new PebItem(5, 4, "Book Skins",
				"", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, ""));
		PEB_ITEMS.add(new PebItem(5, 40, "Wool Colors",
				"Click to jump to a page of wool colors.", ChatColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, "page 10"));
		PEB_ITEMS.add(new PebItem(5, 19, "Enchanted Book",
				"Click to change skin to Enchanted Book. (Default)", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, "clickable peb_skin_enchantedbook"));
		PEB_ITEMS.add(new PebItem(5, 21, "Regal",
				"Click to change skin to Regal.", ChatColor.LIGHT_PURPLE,
				Material.YELLOW_CONCRETE, "clickable peb_skin_regal"));
		PEB_ITEMS.add(new PebItem(5, 23, "Crimson King",
				"Upon the ancient powers creep...", ChatColor.DARK_RED,
				Material.RED_TERRACOTTA, "clickable peb_skin_ck"));
		PEB_ITEMS.add(new PebItem(5, 25, "Rose",
				"Red like roses!", ChatColor.RED,
				Material.RED_CONCRETE, "clickable peb_skin_rose"));

		//page 10
		PEB_ITEMS.add(new PebItem(10, 9, "Back to Book Skins",
				"", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, "page 5"));
		PEB_ITEMS.add(new PebItem(10, 4, "Wool Skins",
				"", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, ""));
		PEB_ITEMS.add(new PebItem(10, 11, "White",
				"Click to change skin to White.", ChatColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, "clickable peb_skin_white"));
		PEB_ITEMS.add(new PebItem(10, 12, "Orange",
				"Click to change skin to Orange.", ChatColor.LIGHT_PURPLE,
				Material.ORANGE_WOOL, "clickable peb_skin_orange"));
		PEB_ITEMS.add(new PebItem(10, 20, "Magenta",
				"Click to change skin to Magenta.", ChatColor.LIGHT_PURPLE,
				Material.MAGENTA_WOOL, "clickable peb_skin_magenta"));
		PEB_ITEMS.add(new PebItem(10, 21, "Light Blue",
				"Click to change skin to Light Blue.", ChatColor.LIGHT_PURPLE,
				Material.LIGHT_BLUE_WOOL, "clickable peb_skin_lightblue"));
		PEB_ITEMS.add(new PebItem(10, 29, "Yellow",
				"Click to change skin to Yellow.", ChatColor.LIGHT_PURPLE,
				Material.YELLOW_WOOL, "clickable peb_skin_yellow"));
		PEB_ITEMS.add(new PebItem(10, 30, "Lime",
				"Click to change skin to Lime.", ChatColor.LIGHT_PURPLE,
				Material.LIME_WOOL, "clickable peb_skin_lime"));
		PEB_ITEMS.add(new PebItem(10, 38, "Pink",
				"Click to change skin to Pink.", ChatColor.LIGHT_PURPLE,
				Material.PINK_WOOL, "clickable peb_skin_pink"));
		PEB_ITEMS.add(new PebItem(10, 39, "Gray",
				"Click to change skin to Gray.", ChatColor.LIGHT_PURPLE,
				Material.GRAY_WOOL, "clickable peb_skin_gray"));
		PEB_ITEMS.add(new PebItem(10, 14, "Light Gray",
				"Click to change skin to Light Gray.", ChatColor.LIGHT_PURPLE,
				Material.LIGHT_GRAY_WOOL, "clickable peb_skin_lightgray"));
		PEB_ITEMS.add(new PebItem(10, 15, "Cyan",
				"Click to change skin to Cyan.", ChatColor.LIGHT_PURPLE,
				Material.CYAN_WOOL, "clickable peb_skin_cyan"));
		PEB_ITEMS.add(new PebItem(10, 23, "Purple",
				"Click to change skin to Purple.", ChatColor.LIGHT_PURPLE,
				Material.PURPLE_WOOL, "clickable peb_skin_purple"));
		PEB_ITEMS.add(new PebItem(10, 24, "Blue",
				"Click to change skin to Blue.", ChatColor.LIGHT_PURPLE,
				Material.BLUE_WOOL, "clickable peb_skin_blue"));
		PEB_ITEMS.add(new PebItem(10, 32, "Brown",
				"Click to change skin to Brown.", ChatColor.LIGHT_PURPLE,
				Material.BROWN_WOOL, "clickable peb_skin_brown"));
		PEB_ITEMS.add(new PebItem(10, 33, "Green",
				"Click to change skin to Green.", ChatColor.LIGHT_PURPLE,
				Material.GREEN_WOOL, "clickable peb_skin_green"));
		PEB_ITEMS.add(new PebItem(10, 41, "Red",
				"Click to change skin to Red.", ChatColor.LIGHT_PURPLE,
				Material.RED_WOOL, "clickable peb_skin_red"));
		PEB_ITEMS.add(new PebItem(10, 42, "Black",
				"Click to change skin to Black.", ChatColor.LIGHT_PURPLE,
				Material.BLACK_WOOL, "clickable peb_skin_black"));
	}

	public PEBCustomInventory(Player player) {
		super(player, 54, player.getName() + "'s P.E.B");

		ScoreboardUtils.setScoreboardValue(player, "PEBPage", 1);

		setLayout(1, player);
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
		int newPageValue;
		int currentPage = ScoreboardUtils.getScoreboardValue(player, "PEBPage");
		if (clickedItem != null && clickedItem.getType() != FILLER && !event.isShiftClick()) {

			int chosenSlot = event.getSlot();
			for (PebItem item : PEB_ITEMS) {
				if (item.mSlot == chosenSlot && item.mPage == currentPage) {
					if (item.mCommand == "") {
						return;
					}
					if (item.mCommand.startsWith("page")) {
						newPageValue = Integer.parseInt(item.mCommand.split(" ")[1]);
						setLayout(newPageValue, player);
						return;
					} else if (item.mCommand.startsWith("exit")) {
						player.closeInventory();
						return;
					} else {
						completeCommand(player, item.mCommand);
						player.closeInventory();
						return;
					}
				}
				if (item.mSlot == chosenSlot && item.mPage == 0) {
					if (item.mCommand == "") {
						return;
					}
					if (item.mCommand.startsWith("page")) {
						newPageValue = Integer.parseInt(item.mCommand.split(" ")[1]);
						setLayout(newPageValue, player);
						return;
					} else {
						completeCommand(player, item.mCommand);
						player.closeInventory();
						return;
					}
				}
			}
		}
	}

	public void completeCommand(Player player, String command) {
		if (command.startsWith("clickable") ||
				command.equals("pickup") ||
				command.equals("toggleswap")) {
			player.performCommand(command);
			return;
		} else {
			String finalCommand = command.replace("@S", player.getName());
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
		}
	}

	public ItemStack createCustomItem(PebItem item, Player player) {
		ItemStack newItem = new ItemStack(item.mType, 1);
		if (item.mType == Material.PLAYER_HEAD) {
			SkullMeta meta = (SkullMeta) newItem.getItemMeta();
			meta.setOwningPlayer(player);
			newItem.setItemMeta(meta);
		}
		ItemMeta meta = newItem.getItemMeta();
		if (item.mName != "") {
			meta.displayName(Component.text(item.mName, NamedTextColor.WHITE)
					.decoration(TextDecoration.ITALIC, false));
		}
		ChatColor defaultColor = (item.mChatColor != null) ? item.mChatColor : ChatColor.LIGHT_PURPLE;
		if (item.mLore != "") {
			splitLoreLine(meta, item.mLore, 30, defaultColor);
		}
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

	public void setLayout(int page, Player player) {

		_inventory.clear();
		for (PebItem item : PEB_ITEMS) {
			if (item.mPage == 0) {
				_inventory.setItem(item.mSlot, createCustomItem(item, player));
			} //intentionally not else, so overrides can happen
			if (item.mPage == page) {
				_inventory.setItem(item.mSlot, createCustomItem(item, player));
			}
		}

		for (int i = 0; i < 54; i++) {
			if (_inventory.getItem(i) == null) {
				_inventory.setItem(i, new ItemStack(FILLER, 1));
			}
		}
		ScoreboardUtils.setScoreboardValue(player, "PEBPage", page);
	}
}