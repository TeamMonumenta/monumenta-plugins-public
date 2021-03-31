package com.playmonumenta.plugins.guis.singlepageguis;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import com.playmonumenta.plugins.guis.SinglePageGUI;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class PebGui extends SinglePageGUI {

	private static final int ROWS = 6;
	private static final int COLUMNS = 9;

	public static class PebItem {
		int mPage = 1;
		int mSlot;
		String mName;
		String mLore;
		Material mType;
		String mCommand;

		public PebItem(int pg, int sl, String n, String l, Material t, String cmd) {
			mSlot = sl;
			mName = n;
			mLore = l;
			mType = t;
			mCommand = cmd;
			mPage = pg;
		}

	}

	private static ArrayList<PebItem> PEB_ITEMS = new ArrayList<>();

	static {
		//Common items for all but main menu are "page 0"
		PEB_ITEMS.add(new PebItem(0, 0, "Back to Main Menu", ChatColor.GOLD + "Returns you to page 1.", Material.OBSERVER, "page 1"));
		PEB_ITEMS.add(new PebItem(0, 8, "Exit PEB", ChatColor.GOLD + "Exits this menu.", Material.COD_BUCKET, "exit"));
		PEB_ITEMS.add(new PebItem(0, 45, "Delete P.E.B.s ✗",
				ChatColor.LIGHT_PURPLE + "Click to remove P.E.B.s from your inventory.",
				Material.FLINT_AND_STEEL, "clickable peb_delete"));
		PEB_ITEMS.add(new PebItem(0, 53, "Kick someone!",
				ChatColor.LIGHT_PURPLE + "Kicks someone from the server.",
				Material.RABBIT_FOOT, "kick @S You really thought I'd let you kick someone other than yourself?"));

		//page 1: main menu
		PEB_ITEMS.add(new PebItem(1, 0, "", "", Material.GRAY_STAINED_GLASS_PANE, ""));
		PEB_ITEMS.add(new PebItem(1, 11, "Player Information",
				ChatColor.LIGHT_PURPLE + "Details about Housing, Prestige, and other player-focused options.",
				Material.PLAYER_HEAD, "page 2"));
		PEB_ITEMS.add(new PebItem(1, 15, "Toggle-able Options",
				ChatColor.LIGHT_PURPLE + "Inventory Sort, Filtered Pickup, and more toggleable choices.",
				Material.LEVER, "page 3"));
		PEB_ITEMS.add(new PebItem(1, 38, "Server Information",
				ChatColor.LIGHT_PURPLE + "Information such as how to use the PEB, current shrine buffs, and random tips.",
				Material.DISPENSER, "page 4"));
		PEB_ITEMS.add(new PebItem(1, 42, "Book Skins",
				ChatColor.LIGHT_PURPLE + "Inventory Sort, Filtered Pickup, and more toggleable choices.",
				Material.ENCHANTED_BOOK, "page 5"));


		//page 2: Player Info
		PEB_ITEMS.add(new PebItem(2, 4, "Player Information",
				"",
				Material.PLAYER_HEAD, ""));
		PEB_ITEMS.add(new PebItem(2, 19, "Housing",
				ChatColor.LIGHT_PURPLE + "Click to view housing information.",
				Material.OAK_DOOR, "clickable peb_housing"));
		PEB_ITEMS.add(new PebItem(2, 21, "Prestige",
				ChatColor.LIGHT_PURPLE + "Click to view prestige and related unlocks.",
				Material.BRICK, "clickable peb_prestige"));
		PEB_ITEMS.add(new PebItem(2, 23, "Class",
				ChatColor.LIGHT_PURPLE + "Click to view your class and skills.",
				Material.STONE_SWORD, "clickable peb_class"));
		PEB_ITEMS.add(new PebItem(2, 25, "Dungeon Instances",
				ChatColor.LIGHT_PURPLE + "Click to view what dungeon instances you have open, and how old they are.",
				Material.WHITE_WOOL, "clickable peb_dungeoninfo"));
		PEB_ITEMS.add(new PebItem(2, 37, "Patreon Shrine",
				ChatColor.LIGHT_PURPLE + "Click to view what buffs are active on the " + ChatColor.GOLD + "Patreon Shrine" + ChatColor.LIGHT_PURPLE + " and your settings.",
				Material.LANTERN, "clickable peb_shrineinfo"));
		PEB_ITEMS.add(new PebItem(2, 39, "Patron",
				ChatColor.LIGHT_PURPLE + "Click to view patron information. Use /donate to learn about donating.",
				Material.GLOWSTONE_DUST, "clickable peb_patroninfo"));
		PEB_ITEMS.add(new PebItem(2, 41, "Dailies",
				ChatColor.LIGHT_PURPLE + "Click to see what daily content you have and haven't done today.",
				Material.ACACIA_BOAT, "clickable peb_dailies"));

		//page 3: Toggle-able Options
		PEB_ITEMS.add(new PebItem(3, 4, "Toggleable Options",
				"",
				Material.LEVER, ""));
		PEB_ITEMS.add(new PebItem(3, 19, "Self Particles",
				ChatColor.LIGHT_PURPLE + "Click to toggle self particles.",
				Material.FIREWORK_STAR, "clickable peb_selfparticles"));
		PEB_ITEMS.add(new PebItem(3, 21, "UA Rocket Jumping",
				ChatColor.LIGHT_PURPLE + "Click to toggle rocket jumping with Unstable Arrows.",
				Material.FIREWORK_ROCKET, "clickable peb_uarj"));
		PEB_ITEMS.add(new PebItem(3, 23, "Show name on patron buff announcement.",
				ChatColor.LIGHT_PURPLE + "Toggles whether the player has their IGN in the buff announcement when they activate " + ChatColor.GOLD + "Patreon " + ChatColor.LIGHT_PURPLE + "buffs.",
				Material.LANTERN, "clickable toggle_patron_buff_thank"));
		PEB_ITEMS.add(new PebItem(3, 25, "Filtered Pickup",
				ChatColor.LIGHT_PURPLE + "Click to toggle the pickup of uninteresting items.",
				Material.DIRT, "pickup"));
		PEB_ITEMS.add(new PebItem(3, 26, "Filtered Pickup information",
				ChatColor.LIGHT_PURPLE + "Click to explain filtered pickup.",
				Material.BOOK, "clickable peb_filteredinfo"));
		PEB_ITEMS.add(new PebItem(3, 37, "Inventory Drink",
				ChatColor.LIGHT_PURPLE + "Click to toggle drinking potions with a right click in any inventory.",
				Material.GLASS_BOTTLE, "clickable peb_tid"));
		PEB_ITEMS.add(new PebItem(3, 39, "Compass Particles",
				ChatColor.LIGHT_PURPLE + "Click to toggle a trail of guiding particles when following the quest compass.",
				Material.COMPASS, "clickable peb_comp_particles"));
		PEB_ITEMS.add(new PebItem(3, 41, "Death Sort",
				ChatColor.LIGHT_PURPLE + "Click to toggle death sorting, which attempts to return items dropped on death to the slot they were in prior to death.",
				Material.CHEST, "clickable peb_toggle_dso"));

		//page 4: Server Info
		PEB_ITEMS.add(new PebItem(4, 4, "Server Information",
				"",
				Material.DISPENSER, ""));
		PEB_ITEMS.add(new PebItem(4, 20, "P.E.B. Introduction",
				ChatColor.LIGHT_PURPLE + "Click to hear the P.E.B. Introduction.",
				Material.ENCHANTED_BOOK, "clickable peb_intro"));
		PEB_ITEMS.add(new PebItem(4, 22, "Get Shrine Buffs",
				ChatColor.LIGHT_PURPLE + "Click to recieve buffs from the " + ChatColor.GOLD + "Patreon Shrine" + ChatColor.LIGHT_PURPLE + " if any are active.",
				Material.GLOWSTONE_DUST, "clickable peb_pbuff"));
		PEB_ITEMS.add(new PebItem(4, 24, "Get a random tip!",
				ChatColor.LIGHT_PURPLE + "Click to get a random tip!",
				Material.REDSTONE_TORCH, "clickable peb_tip"));

		//page 5: Book Skins
		PEB_ITEMS.add(new PebItem(5, 4, "Book Skins",
				"",
				Material.ENCHANTED_BOOK, ""));
		PEB_ITEMS.add(new PebItem(5, 40, "Wool Colors",
				ChatColor.LIGHT_PURPLE + "Click to jump to a page of wool colors.",
				Material.WHITE_WOOL, "page 6"));
		PEB_ITEMS.add(new PebItem(5, 19, "Enchanted Book",
				ChatColor.LIGHT_PURPLE + "Click to change skin to Enchanted Book. (Default)",
				Material.ENCHANTED_BOOK, "clickable peb_skin_enchantedbook"));
		PEB_ITEMS.add(new PebItem(5, 21, "Regal",
				ChatColor.LIGHT_PURPLE + "Click to change skin to Regal.",
				Material.YELLOW_CONCRETE, "clickable peb_skin_regal"));
		PEB_ITEMS.add(new PebItem(5, 23, "Crimson King",
				ChatColor.DARK_RED + "Upon the ancient powers creep...",
				Material.RED_TERRACOTTA, "clickable peb_skin_ck"));
		PEB_ITEMS.add(new PebItem(5, 25, "Rose",
				ChatColor.RED + "Red like roses!",
				Material.RED_CONCRETE, "clickable peb_skin_rose"));

		//page 6
		PEB_ITEMS.add(new PebItem(6, 9, "Back to Book Skins",
				"",
				Material.ENCHANTED_BOOK, "page 5"));
		PEB_ITEMS.add(new PebItem(6, 4, "Wool Skins",
				"",
				Material.ENCHANTED_BOOK, ""));
		PEB_ITEMS.add(new PebItem(6, 11, "White",
				ChatColor.LIGHT_PURPLE + "Click to change skin to White.",
				Material.WHITE_WOOL, "clickable peb_skin_white"));
		PEB_ITEMS.add(new PebItem(6, 12, "Orange",
				ChatColor.LIGHT_PURPLE + "Click to change skin to Orange.",
				Material.ORANGE_WOOL, "clickable peb_skin_orange"));
		PEB_ITEMS.add(new PebItem(6, 20, "Magenta",
				ChatColor.LIGHT_PURPLE + "Click to change skin to Magenta.",
				Material.MAGENTA_WOOL, "clickable peb_skin_magenta"));
		PEB_ITEMS.add(new PebItem(6, 21, "Light Blue",
				ChatColor.LIGHT_PURPLE + "Click to change skin to Light Blue.",
				Material.LIGHT_BLUE_WOOL, "clickable peb_skin_lightblue"));
		PEB_ITEMS.add(new PebItem(6, 29, "Yellow",
				ChatColor.LIGHT_PURPLE + "Click to change skin to Yellow.",
				Material.YELLOW_WOOL, "clickable peb_skin_yellow"));
		PEB_ITEMS.add(new PebItem(6, 30, "Lime",
				ChatColor.LIGHT_PURPLE + "Click to change skin to Lime.",
				Material.LIME_WOOL, "clickable peb_skin_lime"));
		PEB_ITEMS.add(new PebItem(6, 38, "Pink",
				ChatColor.LIGHT_PURPLE + "Click to change skin to Pink.",
				Material.PINK_WOOL, "clickable peb_skin_pink"));
		PEB_ITEMS.add(new PebItem(6, 39, "Gray",
				ChatColor.LIGHT_PURPLE + "Click to change skin to Gray.",
				Material.GRAY_WOOL, "clickable peb_skin_gray"));
		PEB_ITEMS.add(new PebItem(6, 14, "Light Gray",
				ChatColor.LIGHT_PURPLE + "Click to change skin to Light Gray.",
				Material.LIGHT_GRAY_WOOL, "clickable peb_skin_lightgray"));
		PEB_ITEMS.add(new PebItem(6, 15, "Cyan",
				ChatColor.LIGHT_PURPLE + "Click to change skin to Cyan.",
				Material.CYAN_WOOL, "clickable peb_skin_cyan"));
		PEB_ITEMS.add(new PebItem(6, 23, "Purple",
				ChatColor.LIGHT_PURPLE + "Click to change skin to Purple.",
				Material.PURPLE_WOOL, "clickable peb_skin_purple"));
		PEB_ITEMS.add(new PebItem(6, 24, "Blue",
				ChatColor.LIGHT_PURPLE + "Click to change skin to Blue.",
				Material.BLUE_WOOL, "clickable peb_skin_blue"));
		PEB_ITEMS.add(new PebItem(6, 32, "Brown",
				ChatColor.LIGHT_PURPLE + "Click to change skin to Brown.",
				Material.BROWN_WOOL, "clickable peb_skin_brown"));
		PEB_ITEMS.add(new PebItem(6, 33, "Green",
				ChatColor.LIGHT_PURPLE + "Click to change skin to Green.",
				Material.GREEN_WOOL, "clickable peb_skin_green"));
		PEB_ITEMS.add(new PebItem(6, 41, "Red",
				ChatColor.LIGHT_PURPLE + "Click to change skin to Red.",
				Material.RED_WOOL, "clickable peb_skin_red"));
		PEB_ITEMS.add(new PebItem(6, 42, "Black",
				ChatColor.LIGHT_PURPLE + "Click to change skin to Black.",
				Material.BLACK_WOOL, "clickable peb_skin_black"));
	}

	public PebGui(Player player, String[] args) {
		super(player, args);
	}

	/*
	 * You MUST call this method from the SinglePageGUIManager. The example
	 * call for this example GUI is there for reference.
	 */
	@Override
	public void registerCommand() {
		registerCommand("openPEB");
	}

	@Override
	public SinglePageGUI constructGUI(Player player, String[] args) {
		return new PebGui(player, args);
	}

	@Override
	public Inventory getInventory(Player player, String[] args) {
		Inventory baseInventory;
		//in case of someone messing with the score, this should fix it to the correct init values

		ScoreboardUtils.setScoreboardValue(player, "PEBPage", 1);

		//Create inventory and call the builder

		baseInventory = Bukkit.createInventory(null, ROWS*COLUMNS, Component.text(player.getName() + "'s P.E.B."));

		setLayout(1, player, baseInventory);

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
		int newPageValue;
		int currentPage = ScoreboardUtils.getScoreboardValue(player, "PEBPage");
		if (clickedItem != null && clickedItem.getType() != Material.BLACK_STAINED_GLASS_PANE && !event.isShiftClick()) {

			int chosenSlot = event.getSlot();
			for (PebItem item : PEB_ITEMS) {
				if (item.mSlot == chosenSlot && item.mPage == currentPage) {
					if (item.mCommand == "") {
						return;
					}
					if (item.mCommand.startsWith("page")) {
						newPageValue = Integer.parseInt(item.mCommand.split(" ")[1]);
						setLayout(newPageValue, player, inventory);
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
						setLayout(newPageValue, player, inventory);
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
		if (command.startsWith("clickable") || command.equals("pickup")) {
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
		meta.displayName(Component.text(item.mName, NamedTextColor.WHITE)
				.decoration(TextDecoration.ITALIC, false));
		if (item.mLore != "") {
			splitLoreLine(meta, item.mLore, 30, ChatColor.LIGHT_PURPLE);
		}
		newItem.setItemMeta(meta);
		return newItem;
	}
	
	public void splitLoreLine(ItemMeta meta, String lore, int maxLength, ChatColor defaultColor) {
		String[] splitLine = lore.split(" ");
		String currentString = "";
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

	public Inventory setLayout(int page, Player player, Inventory inventory) {
		
		inventory.clear();
		for (PebItem item : PEB_ITEMS) {
			if (item.mPage == 0) {
				inventory.setItem(item.mSlot, createCustomItem(item, player));
			} //intentionally not else, so overrides can happen
			if (item.mPage == page) {
				inventory.setItem(item.mSlot, createCustomItem(item, player));
			}
		}

		for (int i = 0; i < (ROWS*COLUMNS); i++) {
			if (inventory.getItem(i) == null) {
				inventory.setItem(i,new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1));
			}
		}
		com.playmonumenta.plugins.utils.ScoreboardUtils.setScoreboardValue(player, "PEBPage", page);
	
		return inventory;
	}

}
