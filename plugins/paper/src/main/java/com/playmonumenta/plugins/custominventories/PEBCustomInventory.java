package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.player.PlayerData;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class PEBCustomInventory extends CustomInventory {
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

	private enum PebPage {
		COMMON,
		MAIN,
		PLAYER_INFO,
		TOGGLEABLE_OPTIONS,
		SERVER_INFO,
		BOOK_SKINS,
		WOOL_BOOK_SKINS,
		PICKUP_AND_DISABLE_DROP,
		GLOWING,
		ROCKET_JUMP,
		PARTIAL_PARTICLES,
	}

	private static class PebItem {
		int mSlot;
		Function<PEBCustomInventory, String> mName;
		Function<PEBCustomInventory, String> mLore;
		Material mType;
		@Nullable BiConsumer<PEBCustomInventory, InventoryClickEvent> mAction;
		ChatColor mChatColor;
		boolean mCloseAfter;

		public PebItem(int slot, String name, String lore, ChatColor color, Material type, boolean closeAfter) {
			mSlot = slot;
			mName = gui -> name;
			mLore = gui -> lore;
			mType = type;
			mChatColor = color;
			mCloseAfter = closeAfter;
		}

		public PebItem(int slot, Function<PEBCustomInventory, String> name, Function<PEBCustomInventory, String> lore, ChatColor color, Material type, boolean closeAfter) {
			mSlot = slot;
			mName = name;
			mLore = lore;
			mType = type;
			mChatColor = color;
			mCloseAfter = closeAfter;
		}

		public PebItem playerCommand(String command) {
			mAction = (gui, event) -> {
				if (mCloseAfter) {
					gui.mPlayer.closeInventory();
				}
				gui.mPlayer.performCommand(command);
			};
			return this;
		}

		public PebItem serverCommand(String command) {
			mAction = (gui, event) -> {
				String finalCommand = command.replace("@S", gui.mPlayer.getName());
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
				if (mCloseAfter) {
					gui.mPlayer.closeInventory();
				}
			};
			return this;
		}

		public PebItem action(BiConsumer<PEBCustomInventory, InventoryClickEvent> action) {
			mAction = action;
			return this;
		}

		public PebItem switchToPage(PebPage page) {
			mAction = (gui, event) -> gui.setLayout(page);
			return this;
		}

	}

	private static final Map<PebPage, List<PebItem>> PEB_ITEMS = new EnumMap<>(PebPage.class);

	private static void definePage(PebPage page, PebItem... items) {
		PEB_ITEMS.put(page, Arrays.asList(items));
	}

	static {
		//If the command is internal to the GUI, closeAfter is ignored. Otherwise, the GUI abides by that boolean.

		// Common items for all but main menu
		definePage(PebPage.COMMON,
			new PebItem(0, "Back to Main Menu", "Returns you to page 1.", ChatColor.GOLD, Material.OBSERVER, false).switchToPage(PebPage.MAIN),
			new PebItem(8, "Exit PEB", "Exits this menu.", ChatColor.GOLD, Material.RED_CONCRETE, false).action((gui, event) -> gui.mPlayer.closeInventory()),
			new PebItem(45, "Delete P.E.B.s \u2717",
				"Click to remove P.E.B.s from your inventory.", ChatColor.LIGHT_PURPLE,
				Material.FLINT_AND_STEEL, true).playerCommand("clickable peb_delete")
		);

		// main menu
		definePage(PebPage.MAIN,
			new PebItem(0, "", "", ChatColor.LIGHT_PURPLE, FILLER, false),
			new PebItem(4, "Main Menu",
				"A list of commonly used options, along with menu buttons to reach the full lists.", ChatColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, false),
			new PebItem(20, "Filtered Pickup and Disabled Drop",
				"Click to choose your pickup and disabled drop preferences.", ChatColor.LIGHT_PURPLE,
				Material.DIRT, false).switchToPage(PebPage.PICKUP_AND_DISABLE_DROP),
			new PebItem(21, "Toggle Darksight",
				"Click to toggle whether Darksight provides Night Vision", ChatColor.LIGHT_PURPLE,
				Material.LANTERN, false).serverCommand("execute as @S run function monumenta:mechanisms/darksight_toggle"),
			new PebItem(23, "Dailies",
				"Click to see what daily content you have and haven't done today.", ChatColor.LIGHT_PURPLE,
				Material.ACACIA_BOAT, true).playerCommand("clickable peb_dailies"),
			new PebItem(24, "Dungeon Instances",
				"Click to view what dungeon instances you have open, and how old they are.", ChatColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, true).playerCommand("clickable peb_dungeoninfo"),
			new PebItem(37, "Player Information",
				"Details about Housing, Dailies, and other player-focused options.", ChatColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, false).switchToPage(PebPage.PLAYER_INFO),
			new PebItem(39, "Toggle-able Options",
				"Inventory Sort, Filtered Pickup, and more toggleable choices.", ChatColor.LIGHT_PURPLE,
				Material.LEVER, false).switchToPage(PebPage.TOGGLEABLE_OPTIONS),
			new PebItem(41, "Server Information",
				"Information such as how to use the PEB and random tips.", ChatColor.LIGHT_PURPLE,
				Material.DISPENSER, false).switchToPage(PebPage.SERVER_INFO),
			new PebItem(43, "Book Skins",
				"Change the color of the cover on your P.E.B.", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, false).switchToPage(PebPage.BOOK_SKINS)
		);

		//Player Info
		definePage(PebPage.PLAYER_INFO,
			new PebItem(4, "Player Information",
				"", ChatColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, false),
			new PebItem(20, "Housing",
				"Click to view housing information.", ChatColor.LIGHT_PURPLE,
				Material.OAK_DOOR, true).playerCommand("clickable peb_housing"),
			new PebItem(22, "Class",
				"Click to view your class and skills.", ChatColor.LIGHT_PURPLE,
				Material.STONE_SWORD, true).playerCommand("clickable peb_class"),
			new PebItem(24, "Dungeon Instances",
				"Click to view what dungeon instances you have open, and how old they are.", ChatColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, true).playerCommand("clickable peb_dungeoninfo"),
			new PebItem(38, "Patron",
				"Click to view patron information. Use /donate to learn about donating.", ChatColor.LIGHT_PURPLE,
				Material.GLOWSTONE_DUST, true).playerCommand("clickable peb_patroninfo"),
			new PebItem(40, "Dailies",
				"Click to see what daily content you have and haven't done today.", ChatColor.LIGHT_PURPLE,
				Material.ACACIA_BOAT, true).playerCommand("clickable peb_dailies"),
			new PebItem(42, "Item Stats",
				"Click to view your current item stats and compare items.", ChatColor.LIGHT_PURPLE,
				Material.KNOWLEDGE_BOOK, true).playerCommand("playerstats")
		);

		// Toggle-able Options
		definePage(PebPage.TOGGLEABLE_OPTIONS,
			new PebItem(4, "Toggleable Options",
				"", ChatColor.LIGHT_PURPLE,
				Material.LEVER, false),
			new PebItem(19, "Particle Options",
				"Click to choose how many particles will be shown for different categories.", ChatColor.LIGHT_PURPLE,
				Material.NETHER_STAR, false).switchToPage(PebPage.PARTIAL_PARTICLES),
			new PebItem(20, "Glowing options",
				"Click to choose your preferences for the \"glowing\" effect.", ChatColor.LIGHT_PURPLE,
				Material.SPECTRAL_ARROW, false).switchToPage(PebPage.GLOWING),
			new PebItem(21, "Show name on patron buff announcement.",
				"Toggles whether the player has their IGN in the buff announcement when they"
					+ " activate " + ChatColor.GOLD + "Patreon " + ChatColor.LIGHT_PURPLE + "buffs.", ChatColor.LIGHT_PURPLE,
				Material.GLOWSTONE, false).playerCommand("clickable toggle_patron_buff_thank"),
			new PebItem(23, "Inventory Drink",
				"Click to toggle drinking potions with a right click in any inventory.", ChatColor.LIGHT_PURPLE,
				Material.GLASS_BOTTLE, false).playerCommand("clickable peb_tid"),
			new PebItem(24, "Filtered Pickup and Disabled Drop",
				"Click to choose your pickup and disabled drop preferences.", ChatColor.LIGHT_PURPLE,
				Material.DIRT, false).switchToPage(PebPage.PICKUP_AND_DISABLE_DROP),
			new PebItem(25, "Compass Particles",
				"Click to toggle a trail of guiding particles when following the quest compass.", ChatColor.LIGHT_PURPLE,
				Material.COMPASS, false).playerCommand("clickable peb_comp_particles"),
			new PebItem(29, "Rocket Jump",
				"Click to enable or disable Rocket Jump", ChatColor.LIGHT_PURPLE,
				Material.FIREWORK_ROCKET, false).switchToPage(PebPage.ROCKET_JUMP),
			new PebItem(33, "Block Interactions",
				"Click to disable or enable interactions with blocks (looms, crafting tables, beds, etc.)", ChatColor.LIGHT_PURPLE,
				Material.LOOM, false).playerCommand("blockinteractions"),
			new PebItem(37, "Death Sort",
				"Click to toggle death sorting, which attempts to return items dropped on death to the slot they were in prior to death.", ChatColor.LIGHT_PURPLE,
				Material.CHEST, false).playerCommand("clickable peb_toggle_dso"),
			new PebItem(38, "Toggle Darksight",
				"Click to toggle whether Darksight provides Night Vision", ChatColor.LIGHT_PURPLE,
				Material.LANTERN, false).serverCommand("execute as @S run function monumenta:mechanisms/darksight_toggle"),
			new PebItem(39, "Toggle Radiant",
				"Click to toggle whether Radiant provides Night Vision.", ChatColor.LIGHT_PURPLE,
				Material.SOUL_LANTERN, false).serverCommand("execute as @S run function monumenta:mechanisms/radiant_toggle"),
			new PebItem(41, "Offhand Swapping",
				"Click to toggle whether pressing your swap key will be fully cancelled or only cancelled when a spellcast does so", ChatColor.LIGHT_PURPLE,
				Material.SHIELD, false).playerCommand("toggleswap"),
			new PebItem(42, "Spawner Equipment",
				"Click to toggle whether mob equipment is displayed in spawners (significantly decreases FPS in many areas)", ChatColor.LIGHT_PURPLE,
				Material.IRON_CHESTPLATE, false).playerCommand("clickable peb_spawnerequipment"),
			new PebItem(43, "Virtual Firmament",
				"Click to toggle Virtual Firmament, which visually turns your Firmament into a stack of blocks for faster placement.", ChatColor.LIGHT_PURPLE,
				Material.PRISMARINE, false).playerCommand("virtualfirmament")
		);

		// Server Info
		definePage(PebPage.SERVER_INFO,
			new PebItem(4, "Server Information",
				"", ChatColor.LIGHT_PURPLE,
				Material.DISPENSER, false),
			new PebItem(20, "P.E.B. Introduction",
				"Click to hear the P.E.B. Introduction.", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, true).playerCommand("clickable peb_intro"),
			new PebItem(24, "Get a random tip!",
				"Click to get a random tip!", ChatColor.LIGHT_PURPLE,
				Material.REDSTONE_TORCH, true).playerCommand("clickable peb_tip")
		);

		// Book Skins
		definePage(PebPage.BOOK_SKINS,
			new PebItem(4, "Book Skins",
				"", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, false),
			new PebItem(40, "Wool Colors",
				"Click to jump to a page of wool colors.", ChatColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, false).switchToPage(PebPage.WOOL_BOOK_SKINS),
			new PebItem(19, "Enchanted Book",
				"Click to change skin to Enchanted Book. (Default)", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, true).playerCommand("clickable peb_skin_enchantedbook"),
			new PebItem(21, "Regal",
				"Click to change skin to Regal.", ChatColor.LIGHT_PURPLE,
				Material.YELLOW_CONCRETE, true).playerCommand("clickable peb_skin_regal"),
			new PebItem(23, "Crimson King",
				"Upon the ancient powers creep...", ChatColor.DARK_RED,
				Material.RED_TERRACOTTA, true).playerCommand("clickable peb_skin_ck"),
			new PebItem(25, "Rose",
				"Red like roses!", ChatColor.RED,
				Material.RED_CONCRETE, true).playerCommand("clickable peb_skin_rose")
		);

		// Wool book skins
		definePage(PebPage.WOOL_BOOK_SKINS,
			new PebItem(9, "Back to Book Skins",
				"", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, false).switchToPage(PebPage.BOOK_SKINS),
			new PebItem(4, "Wool Skins",
				"", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, false),
			new PebItem(11, "White",
				"Click to change skin to White.", ChatColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, true).playerCommand("clickable peb_skin_white"),
			new PebItem(12, "Orange",
				"Click to change skin to Orange.", ChatColor.LIGHT_PURPLE,
				Material.ORANGE_WOOL, true).playerCommand("clickable peb_skin_orange"),
			new PebItem(20, "Magenta",
				"Click to change skin to Magenta.", ChatColor.LIGHT_PURPLE,
				Material.MAGENTA_WOOL, true).playerCommand("clickable peb_skin_magenta"),
			new PebItem(21, "Light Blue",
				"Click to change skin to Light Blue.", ChatColor.LIGHT_PURPLE,
				Material.LIGHT_BLUE_WOOL, true).playerCommand("clickable peb_skin_lightblue"),
			new PebItem(29, "Yellow",
				"Click to change skin to Yellow.", ChatColor.LIGHT_PURPLE,
				Material.YELLOW_WOOL, true).playerCommand("clickable peb_skin_yellow"),
			new PebItem(30, "Lime",
				"Click to change skin to Lime.", ChatColor.LIGHT_PURPLE,
				Material.LIME_WOOL, true).playerCommand("clickable peb_skin_lime"),
			new PebItem(38, "Pink",
				"Click to change skin to Pink.", ChatColor.LIGHT_PURPLE,
				Material.PINK_WOOL, true).playerCommand("clickable peb_skin_pink"),
			new PebItem(39, "Gray",
				"Click to change skin to Gray.", ChatColor.LIGHT_PURPLE,
				Material.GRAY_WOOL, true).playerCommand("clickable peb_skin_gray"),
			new PebItem(14, "Light Gray",
				"Click to change skin to Light Gray.", ChatColor.LIGHT_PURPLE,
				Material.LIGHT_GRAY_WOOL, true).playerCommand("clickable peb_skin_lightgray"),
			new PebItem(15, "Cyan",
				"Click to change skin to Cyan.", ChatColor.LIGHT_PURPLE,
				Material.CYAN_WOOL, true).playerCommand("clickable peb_skin_cyan"),
			new PebItem(23, "Purple",
				"Click to change skin to Purple.", ChatColor.LIGHT_PURPLE,
				Material.PURPLE_WOOL, true).playerCommand("clickable peb_skin_purple"),
			new PebItem(24, "Blue",
				"Click to change skin to Blue.", ChatColor.LIGHT_PURPLE,
				Material.BLUE_WOOL, true).playerCommand("clickable peb_skin_blue"),
			new PebItem(32, "Brown",
				"Click to change skin to Brown.", ChatColor.LIGHT_PURPLE,
				Material.BROWN_WOOL, true).playerCommand("clickable peb_skin_brown"),
			new PebItem(33, "Green",
				"Click to change skin to Green.", ChatColor.LIGHT_PURPLE,
				Material.GREEN_WOOL, true).playerCommand("clickable peb_skin_green"),
			new PebItem(41, "Red",
				"Click to change skin to Red.", ChatColor.LIGHT_PURPLE,
				Material.RED_WOOL, true).playerCommand("clickable peb_skin_red"),
			new PebItem(42, "Black",
				"Click to change skin to Black.", ChatColor.LIGHT_PURPLE,
				Material.BLACK_WOOL, true).playerCommand("clickable peb_skin_black")
		);

		// Pickup and Disable Drop
		definePage(PebPage.PICKUP_AND_DISABLE_DROP,
			new PebItem(0, "Back to Toggleable Options",
				"", ChatColor.LIGHT_PURPLE,
				Material.OBSERVER, false).switchToPage(PebPage.TOGGLEABLE_OPTIONS),
			new PebItem(4, "Pickup and Disable Drop Settings",
				"Choose the appropriate level of pickup filter and drop filter below.", ChatColor.LIGHT_PURPLE,
				Material.PRISMARINE_CRYSTALS, false),
			new PebItem(11, "Disable Drop:",
				"", ChatColor.LIGHT_PURPLE,
				Material.BLACK_CONCRETE, false),
			new PebItem(19, "None",
				"Disable no drops, the vanilla drop behavior.", ChatColor.LIGHT_PURPLE,
				Material.BARRIER, false).playerCommand("disabledrop none"),
			new PebItem(20, "Holding",
				"Disable dropping of only held items.", ChatColor.LIGHT_PURPLE,
				Material.WOODEN_PICKAXE, false).playerCommand("disabledrop holding"),
			new PebItem(21, "Equipped",
				"Disable dropping of only equipped items.", ChatColor.LIGHT_PURPLE,
				Material.LEATHER_HELMET, false).playerCommand("disabledrop equipped"),
			new PebItem(28, "Tiered",
				"Disable dropping of tiered items.", ChatColor.LIGHT_PURPLE,
				Material.OAK_STAIRS, false).playerCommand("disabledrop tiered"),
			new PebItem(29, "Lore",
				"Disable the drop of items with custom lore.", ChatColor.LIGHT_PURPLE,
				Material.LECTERN, false).playerCommand("disabledrop lore"),
			new PebItem(30, "Interesting",
				"Disable the dropping of anything that matches the default pickup filter of interesting items.", ChatColor.LIGHT_PURPLE,
				Material.GOLD_NUGGET, false).playerCommand("disabledrop interesting"),
			new PebItem(38, "All",
				"Disable all drops.", ChatColor.LIGHT_PURPLE,
				Material.DIRT, false).playerCommand("disabledrop all"),
			new PebItem(15, "Pickup Filter:",
				"", ChatColor.LIGHT_PURPLE,
				Material.WHITE_CONCRETE, false),
			new PebItem(23, "Lore",
				"Only pick up items that have custom lore.", ChatColor.LIGHT_PURPLE,
				Material.LECTERN, false).playerCommand("pickup lore"),
			new PebItem(25, "Interesting",
				"Only pick up items are of interest for the adventuring player, like arrows, torches, and anything with custom lore.", ChatColor.LIGHT_PURPLE,
				Material.GOLD_NUGGET, false).playerCommand("pickup interesting"),
			new PebItem(41, "All",
				"Pick up anything and everything, matching vanilla functionality.", ChatColor.LIGHT_PURPLE,
				Material.DIRT, false).playerCommand("p.pickup all"),
			new PebItem(43, "Threshold",
				"Set the minimum size of a stack of uninteresting items to pick up.", ChatColor.LIGHT_PURPLE,
				Material.OAK_SIGN, false)
				.action((gui, event) -> {
					gui.mPlayer.closeInventory();
					gui.openPickupThresholdSignUI();
				})
		);

		// Glowing options
		definePage(PebPage.GLOWING,
			new PebItem(0, "Back to Toggleable Options",
				"", ChatColor.LIGHT_PURPLE,
				Material.OBSERVER, false).switchToPage(PebPage.TOGGLEABLE_OPTIONS),
			new PebItem(4, "Glowing Settings",
				"Choose for which entity types the glowing effect may be shown. " +
					"If an entity fits into more than one category (e.g. a boss matches both 'mobs' and 'bosses'), it will glow if any of the matching options are enabled.", ChatColor.LIGHT_PURPLE,
				Material.SPECTRAL_ARROW, false),
			new PebItem(22, "Enable All",
				"Enable glowing for all entities (default).", ChatColor.LIGHT_PURPLE,
				Material.GOLD_INGOT, false).playerCommand("glowing enable all"),
			new PebItem(28, "Other Players",
				"Toggle glowing for other players.", ChatColor.LIGHT_PURPLE,
				Material.PLAYER_WALL_HEAD, false).playerCommand("glowing toggle other_players"),
			new PebItem(28, "Yourself",
				"Toggle glowing for yourself (visible in third-person). Disable this if glowing causes rendering issues..", ChatColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, false).playerCommand("glowing toggle self"),
			new PebItem(29, "Mobs",
				"Toggle glowing for mobs.", ChatColor.LIGHT_PURPLE,
				Material.ZOMBIE_HEAD, false).playerCommand("glowing toggle mobs"),
			new PebItem(30, "Bosses",
				"Toggle glowing for bosses. Note that pretty much all bosses are mobs, soa re affected by that option as well.", ChatColor.LIGHT_PURPLE,
				Material.DRAGON_HEAD, false).playerCommand("glowing toggle bosses"),
			new PebItem(32, "Invisible Entities",
				"Toggle glowing for invisible entities.", ChatColor.LIGHT_PURPLE,
				Material.GLASS, false).playerCommand("glowing toggle invisible"),
			new PebItem(33, "Experience Orbs",
				"Toggle glowing for experience orbs.", ChatColor.LIGHT_PURPLE,
				Material.EXPERIENCE_BOTTLE, false).playerCommand("glowing toggle experience_orbs"),
			new PebItem(34, "Miscellaneous",
				"Toggle glowing for miscellaneous entities, i.e. entities that don't fit into any other category.", ChatColor.LIGHT_PURPLE,
				Material.IRON_NUGGET, false).playerCommand("glowing toggle misc"),
			new PebItem(40, "Disable All",
				"Disable glowing for all entities.", ChatColor.LIGHT_PURPLE,
				Material.DIRT, false).playerCommand("glowing disable all")
		);

		// Rocket Jump Option
		definePage(PebPage.ROCKET_JUMP,
			new PebItem(0, "Back to Toggleable Options",
				"", ChatColor.LIGHT_PURPLE,
				Material.OBSERVER, false).switchToPage(PebPage.TOGGLEABLE_OPTIONS),
			new PebItem(4, "Rocket Jump Settings",
				"Choose how Unstable Amalgam should interact with you.", ChatColor.LIGHT_PURPLE,
				Material.FIREWORK_ROCKET, false),
			new PebItem(20, "Enable All",
				"Enable to rocket jump from ANY Unstable Amalgam.", ChatColor.LIGHT_PURPLE,
				Material.FIREWORK_STAR, false).serverCommand("scoreboard players set @S RocketJumper 100"),
			new PebItem(22, "Enable your",
				"Enable to rocket jump only from YOUR Unstable Amalgam.", ChatColor.LIGHT_PURPLE,
				Material.CLAY_BALL, false).serverCommand("scoreboard players set @S RocketJumper 1"),
			new PebItem(24, "Disable all",
				"Disable to rocket jump from ANY Unstable Amalgam.", ChatColor.LIGHT_PURPLE,
				Material.SKELETON_SKULL, false).serverCommand("scoreboard players set @S RocketJumper 0")
		);

		// Partial particle settings
		definePage(PebPage.PARTIAL_PARTICLES,
			new PebItem(0, "Back to Toggleable Options",
				"", ChatColor.GRAY,
				Material.OBSERVER, false).switchToPage(PebPage.TOGGLEABLE_OPTIONS),
			new PebItem(4, "Particle Settings",
				"Choose how many particles are shown for abilities of various categories. These settings can also be changed using the /particles command.", ChatColor.GRAY,
				Material.NETHER_STAR, false).switchToPage(PebPage.TOGGLEABLE_OPTIONS),
			makePartialParticlePebItem(19, "Own Active Abilities", "Particle multiplier for your own active abilities", Material.PLAYER_HEAD, ParticleCategory.OWN_ACTIVE),
			makePartialParticlePebItem(20, "Own Passive Abilities", "Particle multiplier for your own passive abilities", Material.FIREWORK_STAR, ParticleCategory.OWN_PASSIVE),
			makePartialParticlePebItem(21, "(De)Buffs on yourself", "Particle multiplier for active effects on you, e.g. the Defensive Line buff", Material.ENDER_PEARL, ParticleCategory.OWN_BUFF),
			makePartialParticlePebItem(23, "Others' Active Abilities", "Particle multiplier for other players' active abilities", Material.PLAYER_WALL_HEAD, ParticleCategory.OTHER_ACTIVE),
			makePartialParticlePebItem(24, "Others' Passive Abilities", "Particle multiplier for other players' passive abilities", Material.FIREWORK_STAR, ParticleCategory.OTHER_PASSIVE),
			makePartialParticlePebItem(25, "(De)Buffs on other players", "Particle multiplier for active effects on other players, e.g. the Defensive Line buff", Material.ENDER_PEARL, ParticleCategory.OTHER_BUFF),
			makePartialParticlePebItem(39, "Boss Abilities", "Particle multiplier for bosses' abilities", Material.DRAGON_HEAD, ParticleCategory.BOSS),
			makePartialParticlePebItem(40, "(De)Buffs on Enemies", "Particle multiplier for active effects on enemies, e.g. Spellshock's Static", Material.ENDER_PEARL, ParticleCategory.ENEMY_BUFF),
			makePartialParticlePebItem(41, "Other Enemies' Abilities", "Particle multiplier for non-boss enemies' abilities", Material.ZOMBIE_HEAD, ParticleCategory.ENEMY)
		);

	}

	private static PebItem makePartialParticlePebItem(int slot, String name, String description, Material material, ParticleCategory category) {
		return new PebItem(slot, gui -> name + ": " + ScoreboardUtils.getScoreboardValue(gui.mPlayer, category.mObjectiveName).orElse(100) + "%",
			gui -> description + ". Left click to increase, right click to decrease. Hold shift to increase/decrease in smaller steps.", ChatColor.GRAY,
			material, false).switchToPage(PebPage.TOGGLEABLE_OPTIONS)
			.action((gui, event) -> {
				int value = ScoreboardUtils.getScoreboardValue(gui.mPlayer, category.mObjectiveName).orElse(100);
				value += (event.isLeftClick() ? 1 : -1) * (event.isShiftClick() ? 5 : 20);
				value = Math.max(0, Math.min(value, PlayerData.MAX_PARTIAL_PARTICLE_VALUE));
				ScoreboardUtils.setScoreboardValue(gui.mPlayer, category.mObjectiveName, value);
				gui.setLayout(gui.mCurrentPage); // refresh GUI
			});
	}

	private final Player mPlayer;
	private PebPage mCurrentPage;


	public PEBCustomInventory(Player player) {
		super(player, 54, player.getName() + "'s P.E.B");

		mPlayer = player;
		mCurrentPage = PebPage.MAIN;

		setLayout(mCurrentPage);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.getWhoClicked() != mPlayer
			    || event.getClickedInventory() != mInventory) {
			return;
		}
		ItemStack clickedItem = event.getCurrentItem();
		if (clickedItem != null && clickedItem.getType() != FILLER) {
			int chosenSlot = event.getSlot();
			for (PebItem item : PEB_ITEMS.get(mCurrentPage)) {
				if (item.mSlot == chosenSlot) {
					if (item.mAction != null) {
						item.mAction.accept(this, event);
					}
					return;
				}
			}
			for (PebItem item : PEB_ITEMS.get(PebPage.COMMON)) {
				if (item.mSlot == chosenSlot) {
					if (item.mAction != null) {
						item.mAction.accept(this, event);
					}
					return;
				}
			}
		}
	}

	private void openPickupThresholdSignUI() {
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

		menu.open(mPlayer);
	}

	private ItemStack createCustomItem(PebItem item, Player player) {
		ItemStack newItem = new ItemStack(item.mType == Material.PLAYER_WALL_HEAD ? Material.PLAYER_HEAD : item.mType, 1);
		if (item.mType == Material.PLAYER_HEAD) {
			SkullMeta meta = (SkullMeta) newItem.getItemMeta();
			meta.setOwningPlayer(player);
			newItem.setItemMeta(meta);
		}
		ItemMeta meta = newItem.getItemMeta();
		String name = item.mName.apply(this);
		if (!name.isEmpty()) {
			meta.displayName(Component.text(name, NamedTextColor.WHITE)
				.decoration(TextDecoration.ITALIC, false));
		}
		ChatColor defaultColor = (item.mChatColor != null) ? item.mChatColor : ChatColor.LIGHT_PURPLE;
		String lore = item.mLore.apply(this);
		if (!lore.isEmpty()) {
			GUIUtils.splitLoreLine(meta, lore, 30, defaultColor, true);
		}
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		newItem.setItemMeta(meta);
		ItemUtils.setPlainName(newItem);
		return newItem;
	}

	private void setLayout(PebPage page) {
		mCurrentPage = page;

		mInventory.clear();
		for (PebItem item : PEB_ITEMS.get(mCurrentPage)) {
			mInventory.setItem(item.mSlot, createCustomItem(item, mPlayer));
		}
		for (PebItem item : PEB_ITEMS.get(PebPage.COMMON)) {
			if (mInventory.getItem(item.mSlot) == null) {
				mInventory.setItem(item.mSlot, createCustomItem(item, mPlayer));
			}
		}

		for (int i = 0; i < 54; i++) {
			if (mInventory.getItem(i) == null) {
				mInventory.setItem(i, new ItemStack(FILLER, 1));
			}
		}
	}
}
