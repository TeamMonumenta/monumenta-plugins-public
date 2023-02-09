package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.player.PlayerData;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
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
		Function<PEBCustomInventory, TextComponent> mLore;
		Material mType;
		@Nullable BiConsumer<PEBCustomInventory, InventoryClickEvent> mAction;
		boolean mCloseAfter;

		public PebItem(int slot, String name, String lore, TextColor color, Material type, boolean closeAfter) {
			this(slot, name, Component.text(lore, color), type, closeAfter);
		}

		public PebItem(int slot, String name, TextComponent lore, Material type, boolean closeAfter) {
			this(slot, gui -> name, gui -> lore, type, closeAfter);
		}

		public PebItem(int slot, Function<PEBCustomInventory, String> name, Function<PEBCustomInventory, String> lore, TextColor color, Material type, boolean closeAfter) {
			this(slot, name, lore.andThen(s -> Component.text(s, color)), type, closeAfter);
		}

		public PebItem(int slot, Function<PEBCustomInventory, String> name, Function<PEBCustomInventory, TextComponent> lore, Material type, boolean closeAfter) {
			mSlot = slot;
			mName = name;
			mLore = lore;
			mType = type;
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
			new PebItem(0, "Back to Main Menu", "Returns you to page 1.", NamedTextColor.GOLD, Material.OBSERVER, false).switchToPage(PebPage.MAIN),
			new PebItem(8, "Exit PEB", "Exits this menu.", NamedTextColor.GOLD, Material.RED_CONCRETE, false).action((gui, event) -> gui.mPlayer.closeInventory()),
			new PebItem(45, "Delete P.E.B.s \u2717",
				"Click to remove P.E.B.s from your inventory.", NamedTextColor.LIGHT_PURPLE,
				Material.FLINT_AND_STEEL, true).playerCommand("clickable peb_delete")
		);

		// main menu
		definePage(PebPage.MAIN,
			new PebItem(0, "", "", NamedTextColor.LIGHT_PURPLE, FILLER, false),
			new PebItem(4, "Main Menu",
				"A list of commonly used options, along with menu buttons to reach the full lists.", NamedTextColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, false),
			new PebItem(20, "Filtered Pickup and Disabled Drop",
				"Click to choose your pickup and disabled drop preferences.", NamedTextColor.LIGHT_PURPLE,
				Material.DIRT, false).switchToPage(PebPage.PICKUP_AND_DISABLE_DROP),
			new PebItem(21, "Toggle Darksight",
				"Click to toggle whether Darksight provides Night Vision", NamedTextColor.LIGHT_PURPLE,
				Material.LANTERN, false).serverCommand("execute as @S run function monumenta:mechanisms/darksight_toggle"),
			new PebItem(23, "Dailies",
				"Click to see what daily content you have and haven't done today.", NamedTextColor.LIGHT_PURPLE,
				Material.ACACIA_BOAT, true).playerCommand("clickable peb_dailies"),
			new PebItem(24, "Dungeon Instances",
				"Click to view what dungeon instances you have open, and how old they are.", NamedTextColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, true).playerCommand("clickable peb_dungeoninfo"),
			new PebItem(37, "Player Information",
				"Details about your Class, Dailies, and other player-focused options.", NamedTextColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, false).switchToPage(PebPage.PLAYER_INFO),
			new PebItem(39, "Toggle-able Options",
				"Inventory Sort, Filtered Pickup, and more toggleable choices.", NamedTextColor.LIGHT_PURPLE,
				Material.LEVER, false).switchToPage(PebPage.TOGGLEABLE_OPTIONS),
			new PebItem(41, "Server Information",
				"Information such as how to use the PEB and random tips.", NamedTextColor.LIGHT_PURPLE,
				Material.DISPENSER, false).switchToPage(PebPage.SERVER_INFO),
			new PebItem(43, "Book Skins",
				"Change the color of the cover on your P.E.B.", NamedTextColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, false).switchToPage(PebPage.BOOK_SKINS)
		);

		//Player Info
		definePage(PebPage.PLAYER_INFO,
			new PebItem(4, "Player Information",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, false),
			new PebItem(20, "Class",
				"Click to view your class and skills.", NamedTextColor.LIGHT_PURPLE,
				Material.STONE_SWORD, true).playerCommand("clickable peb_class"),
			new PebItem(22, "Dailies",
				"Click to see what daily content you have and haven't done today.", NamedTextColor.LIGHT_PURPLE,
				Material.ACACIA_BOAT, true).playerCommand("clickable peb_dailies"),
			new PebItem(24, "Dungeon Instances",
				"Click to view what dungeon instances you have open, and how old they are.", NamedTextColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, true).playerCommand("clickable peb_dungeoninfo"),
			new PebItem(38, "Patron",
				"Click to view patron information. Use /help donate to learn about donating.", NamedTextColor.LIGHT_PURPLE,
				Material.GLOWSTONE_DUST, true).playerCommand("clickable peb_patroninfo"),
			new PebItem(42, "Item Stats",
				"Click to view your current item stats and compare items.", NamedTextColor.LIGHT_PURPLE,
				Material.KNOWLEDGE_BOOK, true).playerCommand("playerstats")
		);

		// Toggle-able Options
		definePage(PebPage.TOGGLEABLE_OPTIONS,
			new PebItem(4, "Toggleable Options",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.LEVER, false),
			new PebItem(19, "Particle Options",
				"Click to choose how many particles will be shown for different categories.", NamedTextColor.LIGHT_PURPLE,
				Material.NETHER_STAR, false).switchToPage(PebPage.PARTIAL_PARTICLES),
			new PebItem(20, "Glowing options",
				"Click to choose your preferences for the \"glowing\" effect.", NamedTextColor.LIGHT_PURPLE,
				Material.SPECTRAL_ARROW, false).switchToPage(PebPage.GLOWING),
			new PebItem(21, "Show name on patron buff announcement.",
				Component.text("Toggles whether the player has their IGN in the buff announcement when they activate ", NamedTextColor.LIGHT_PURPLE)
					.append(Component.text("Patreon ", NamedTextColor.GOLD))
					.append(Component.text("buffs.", NamedTextColor.LIGHT_PURPLE)),
				Material.GLOWSTONE, false).playerCommand("clickable toggle_patron_buff_thank"),
			new PebItem(23, "Inventory Drink",
				"Click to toggle drinking potions with a right click in any inventory.", NamedTextColor.LIGHT_PURPLE,
				Material.GLASS_BOTTLE, false).playerCommand("clickable peb_tid"),
			new PebItem(24, "Filtered Pickup and Disabled Drop",
				"Click to choose your pickup and disabled drop preferences.", NamedTextColor.LIGHT_PURPLE,
				Material.DIRT, false).switchToPage(PebPage.PICKUP_AND_DISABLE_DROP),
			new PebItem(25, "Compass Particles",
				"Click to toggle a trail of guiding particles when following the quest compass.", NamedTextColor.LIGHT_PURPLE,
				Material.COMPASS, false).playerCommand("clickable peb_comp_particles"),
			new PebItem(29, "Rocket Jump",
				"Click to enable or disable Rocket Jump", NamedTextColor.LIGHT_PURPLE,
				Material.FIREWORK_ROCKET, false).switchToPage(PebPage.ROCKET_JUMP),
			new PebItem(32, "Auto-abandon completed dungeons",
				"Click to disable or enable automatically abandoning completed dungeon instances when a new week starts.", NamedTextColor.LIGHT_PURPLE,
				Material.DAYLIGHT_DETECTOR, false).serverCommand("execute as @S run function monumenta:mechanisms/auto_dungeon_abandon_toggle"),
			new PebItem(33, "Spawner Equipment",
				"Click to toggle whether mob equipment is displayed in spawners (significantly decreases FPS in many areas)", NamedTextColor.LIGHT_PURPLE,
				Material.SPAWNER, false).playerCommand("clickable peb_spawnerequipment"),
			new PebItem(34, "Virtual Firmament",
				"Click to toggle Virtual Firmament, which visually turns your Firmament into a stack of blocks for faster placement.", NamedTextColor.LIGHT_PURPLE,
				Material.PRISMARINE, false).playerCommand("virtualfirmament"),
			new PebItem(37, "Spoof World Names",
				"Click to enable or disable spoofing of shard-specific world names. This is helpful for world map mods to be able to detect worlds better.", NamedTextColor.LIGHT_PURPLE,
				Material.CARTOGRAPHY_TABLE, false).playerCommand("toggleworldnames"),
			new PebItem(38, "Toggle Darksight",
				"Click to toggle whether Darksight provides Night Vision", NamedTextColor.LIGHT_PURPLE,
				Material.LANTERN, false).serverCommand("execute as @S run function monumenta:mechanisms/darksight_toggle"),
			new PebItem(39, "Toggle Radiant",
				"Click to toggle whether Radiant provides Night Vision.", NamedTextColor.LIGHT_PURPLE,
				Material.SOUL_LANTERN, false).serverCommand("execute as @S run function monumenta:mechanisms/radiant_toggle"),
			new PebItem(41, "Block Interactions",
				"Click to disable or enable interactions with blocks (looms, crafting tables, beds, etc.)", NamedTextColor.LIGHT_PURPLE,
				Material.LOOM, false).playerCommand("blockinteractions"),
			new PebItem(42, "Offhand Swapping",
				"Click to toggle whether pressing your swap key will be fully cancelled or only cancelled when a spellcast does so", NamedTextColor.LIGHT_PURPLE,
				Material.SHIELD, false).playerCommand("toggleswap"),
			new PebItem(43, "Offhand Swapping in Inventory",
				"Click to toggle whether pressing your swap key in an inventory will perform its vanilla action", NamedTextColor.LIGHT_PURPLE,
				Material.SHIELD, false).playerCommand("toggleinventoryswap")
		);

		// Server Info
		definePage(PebPage.SERVER_INFO,
			new PebItem(4, "Server Information",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.DISPENSER, false),
			new PebItem(20, "P.E.B. Introduction",
				"Click to hear the P.E.B. Introduction.", NamedTextColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, true).playerCommand("clickable peb_intro"),
			new PebItem(24, "Get a random tip!",
				"Click to get a random tip!", NamedTextColor.LIGHT_PURPLE,
				Material.REDSTONE_TORCH, true).playerCommand("clickable peb_tip")
		);

		// Book Skins
		definePage(PebPage.BOOK_SKINS,
			new PebItem(4, "Book Skins",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, false),
			new PebItem(40, "Wool Colors",
				"Click to jump to a page of wool colors.", NamedTextColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, false).switchToPage(PebPage.WOOL_BOOK_SKINS),
			new PebItem(19, "Enchanted Book",
				"Click to change skin to Enchanted Book. (Default)", NamedTextColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, true).playerCommand("clickable peb_skin_enchantedbook"),
			new PebItem(21, "Regal",
				"Click to change skin to Regal.", NamedTextColor.LIGHT_PURPLE,
				Material.YELLOW_CONCRETE, true).playerCommand("clickable peb_skin_regal"),
			new PebItem(23, "Crimson King",
				"Upon the ancient powers creep...", NamedTextColor.DARK_RED,
				Material.RED_TERRACOTTA, true).playerCommand("clickable peb_skin_ck"),
			new PebItem(25, "Rose",
				"Red like roses!", NamedTextColor.RED,
				Material.RED_CONCRETE, true).playerCommand("clickable peb_skin_rose")
		);

		// Wool book skins
		definePage(PebPage.WOOL_BOOK_SKINS,
			new PebItem(9, "Back to Book Skins",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, false).switchToPage(PebPage.BOOK_SKINS),
			new PebItem(4, "Wool Skins",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, false),
			new PebItem(11, "White",
				"Click to change skin to White.", NamedTextColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, true).playerCommand("clickable peb_skin_white"),
			new PebItem(12, "Orange",
				"Click to change skin to Orange.", NamedTextColor.LIGHT_PURPLE,
				Material.ORANGE_WOOL, true).playerCommand("clickable peb_skin_orange"),
			new PebItem(20, "Magenta",
				"Click to change skin to Magenta.", NamedTextColor.LIGHT_PURPLE,
				Material.MAGENTA_WOOL, true).playerCommand("clickable peb_skin_magenta"),
			new PebItem(21, "Light Blue",
				"Click to change skin to Light Blue.", NamedTextColor.LIGHT_PURPLE,
				Material.LIGHT_BLUE_WOOL, true).playerCommand("clickable peb_skin_lightblue"),
			new PebItem(29, "Yellow",
				"Click to change skin to Yellow.", NamedTextColor.LIGHT_PURPLE,
				Material.YELLOW_WOOL, true).playerCommand("clickable peb_skin_yellow"),
			new PebItem(30, "Lime",
				"Click to change skin to Lime.", NamedTextColor.LIGHT_PURPLE,
				Material.LIME_WOOL, true).playerCommand("clickable peb_skin_lime"),
			new PebItem(38, "Pink",
				"Click to change skin to Pink.", NamedTextColor.LIGHT_PURPLE,
				Material.PINK_WOOL, true).playerCommand("clickable peb_skin_pink"),
			new PebItem(39, "Gray",
				"Click to change skin to Gray.", NamedTextColor.LIGHT_PURPLE,
				Material.GRAY_WOOL, true).playerCommand("clickable peb_skin_gray"),
			new PebItem(14, "Light Gray",
				"Click to change skin to Light Gray.", NamedTextColor.LIGHT_PURPLE,
				Material.LIGHT_GRAY_WOOL, true).playerCommand("clickable peb_skin_lightgray"),
			new PebItem(15, "Cyan",
				"Click to change skin to Cyan.", NamedTextColor.LIGHT_PURPLE,
				Material.CYAN_WOOL, true).playerCommand("clickable peb_skin_cyan"),
			new PebItem(23, "Purple",
				"Click to change skin to Purple.", NamedTextColor.LIGHT_PURPLE,
				Material.PURPLE_WOOL, true).playerCommand("clickable peb_skin_purple"),
			new PebItem(24, "Blue",
				"Click to change skin to Blue.", NamedTextColor.LIGHT_PURPLE,
				Material.BLUE_WOOL, true).playerCommand("clickable peb_skin_blue"),
			new PebItem(32, "Brown",
				"Click to change skin to Brown.", NamedTextColor.LIGHT_PURPLE,
				Material.BROWN_WOOL, true).playerCommand("clickable peb_skin_brown"),
			new PebItem(33, "Green",
				"Click to change skin to Green.", NamedTextColor.LIGHT_PURPLE,
				Material.GREEN_WOOL, true).playerCommand("clickable peb_skin_green"),
			new PebItem(41, "Red",
				"Click to change skin to Red.", NamedTextColor.LIGHT_PURPLE,
				Material.RED_WOOL, true).playerCommand("clickable peb_skin_red"),
			new PebItem(42, "Black",
				"Click to change skin to Black.", NamedTextColor.LIGHT_PURPLE,
				Material.BLACK_WOOL, true).playerCommand("clickable peb_skin_black")
		);

		// Pickup and Disable Drop
		definePage(PebPage.PICKUP_AND_DISABLE_DROP,
			new PebItem(0, "Back to Toggleable Options",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.OBSERVER, false).switchToPage(PebPage.TOGGLEABLE_OPTIONS),
			new PebItem(4, "Pickup and Disable Drop Settings",
				"Choose the appropriate level of pickup filter and drop filter below.", NamedTextColor.LIGHT_PURPLE,
				Material.PRISMARINE_CRYSTALS, false),
			new PebItem(11, "Disable Drop:",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.BLACK_CONCRETE, false),
			new PebItem(19, "None",
				"Disable no drops, the vanilla drop behavior.", NamedTextColor.LIGHT_PURPLE,
				Material.BARRIER, false).playerCommand("disabledrop none"),
			new PebItem(20, "Equipped",
				"Disable dropping of only equipped items.", NamedTextColor.LIGHT_PURPLE,
				Material.LEATHER_HELMET, false).playerCommand("disabledrop equipped"),
			new PebItem(21, "Tiered",
				"Disable dropping of tiered items.", NamedTextColor.LIGHT_PURPLE,
				Material.OAK_STAIRS, false).playerCommand("disabledrop tiered"),
			new PebItem(28, "Lore",
				"Disable the drop of items with custom lore.", NamedTextColor.LIGHT_PURPLE,
				Material.LECTERN, false).playerCommand("disabledrop lore"),
			new PebItem(29, "Interesting",
				"Disable the dropping of anything that matches the default pickup filter of interesting items.", NamedTextColor.LIGHT_PURPLE,
				Material.GOLD_NUGGET, false).playerCommand("disabledrop interesting"),
			new PebItem(30, "All",
				"Disable all drops.", NamedTextColor.LIGHT_PURPLE,
				Material.DIRT, false).playerCommand("disabledrop all"),
			new PebItem(15, "Pickup Filter:",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.WHITE_CONCRETE, false),
			new PebItem(23, "Tiered",
				"Only pick up items that have a tier.", NamedTextColor.LIGHT_PURPLE,
				Material.OAK_STAIRS, false).playerCommand("pickup tiered"),
			new PebItem(24, "Lore",
				"Only pick up items that have custom lore.", NamedTextColor.LIGHT_PURPLE,
				Material.LECTERN, false).playerCommand("pickup lore"),
			new PebItem(25, "Interesting",
				"Only pick up items are of interest for the adventuring player, like arrows, torches, and anything with custom lore.", NamedTextColor.LIGHT_PURPLE,
				Material.GOLD_NUGGET, false).playerCommand("pickup interesting"),
			new PebItem(41, "All",
				"Pick up anything and everything, matching vanilla functionality.", NamedTextColor.LIGHT_PURPLE,
				Material.DIRT, false).playerCommand("pickup all"),
			new PebItem(43, "Threshold",
				"Set the minimum size of a stack of uninteresting items to pick up.", NamedTextColor.LIGHT_PURPLE,
				Material.OAK_SIGN, false)
				.action((gui, event) -> {
					gui.mPlayer.closeInventory();
					gui.openPickupThresholdSignUI();
				})
		);

		// Glowing options
		definePage(PebPage.GLOWING,
			new PebItem(0, "Back to Toggleable Options",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.OBSERVER, false).switchToPage(PebPage.TOGGLEABLE_OPTIONS),
			new PebItem(4, "Glowing Settings",
				"Choose for which entity types the glowing effect may be shown. " +
					"If an entity fits into more than one category (e.g. a boss matches both 'mobs' and 'bosses'), it will glow if any of the matching options are enabled.", NamedTextColor.LIGHT_PURPLE,
				Material.SPECTRAL_ARROW, false),
			new PebItem(22, "Enable All",
				"Enable glowing for all entities (default).", NamedTextColor.LIGHT_PURPLE,
				Material.GOLD_INGOT, false).playerCommand("glowing enable all"),
			new PebItem(28, "Other Players",
				"Toggle glowing for other players.", NamedTextColor.LIGHT_PURPLE,
				Material.PLAYER_WALL_HEAD, false).playerCommand("glowing toggle other_players"),
			new PebItem(29, "Yourself",
				"Toggle glowing for yourself (visible in third-person). Disable this if glowing causes rendering issues.", NamedTextColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, false).playerCommand("glowing toggle self"),
			new PebItem(30, "Mobs",
				"Toggle glowing for mobs.", NamedTextColor.LIGHT_PURPLE,
				Material.ZOMBIE_HEAD, false).playerCommand("glowing toggle mobs"),
			new PebItem(31, "Bosses",
				"Toggle glowing for bosses. Note that pretty much all bosses are mobs, so are affected by that option as well.", NamedTextColor.LIGHT_PURPLE,
				Material.DRAGON_HEAD, false).playerCommand("glowing toggle bosses"),
			new PebItem(32, "Invisible Entities",
				"Toggle glowing for invisible entities.", NamedTextColor.LIGHT_PURPLE,
				Material.GLASS, false).playerCommand("glowing toggle invisible"),
			new PebItem(33, "Experience Orbs",
				"Toggle glowing for experience orbs.", NamedTextColor.LIGHT_PURPLE,
				Material.EXPERIENCE_BOTTLE, false).playerCommand("glowing toggle experience_orbs"),
			new PebItem(34, "Miscellaneous",
				"Toggle glowing for miscellaneous entities, i.e. entities that don't fit into any other category.", NamedTextColor.LIGHT_PURPLE,
				Material.IRON_NUGGET, false).playerCommand("glowing toggle misc"),
			new PebItem(40, "Disable All",
				"Disable glowing for all entities.", NamedTextColor.LIGHT_PURPLE,
				Material.DIRT, false).playerCommand("glowing disable all")
		);

		// Rocket Jump Option
		definePage(PebPage.ROCKET_JUMP,
			new PebItem(0, "Back to Toggleable Options",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.OBSERVER, false).switchToPage(PebPage.TOGGLEABLE_OPTIONS),
			new PebItem(4, "Rocket Jump Settings",
				"Choose how Unstable Amalgam should interact with you.", NamedTextColor.LIGHT_PURPLE,
				Material.FIREWORK_ROCKET, false),
			new PebItem(20, "Enable All",
				"Enable to rocket jump from ANY Unstable Amalgam.", NamedTextColor.LIGHT_PURPLE,
				Material.FIREWORK_STAR, false).serverCommand("scoreboard players set @S RocketJumper 100"),
			new PebItem(22, "Enable your",
				"Enable to rocket jump only from YOUR Unstable Amalgam.", NamedTextColor.LIGHT_PURPLE,
				Material.CLAY_BALL, false).serverCommand("scoreboard players set @S RocketJumper 1"),
			new PebItem(24, "Disable all",
				"Disable to rocket jump from ANY Unstable Amalgam.", NamedTextColor.LIGHT_PURPLE,
				Material.SKELETON_SKULL, false).serverCommand("scoreboard players set @S RocketJumper 0")
		);

		// Partial particle settings
		definePage(PebPage.PARTIAL_PARTICLES,
			new PebItem(0, "Back to Toggleable Options",
				"", NamedTextColor.GRAY,
				Material.OBSERVER, false).switchToPage(PebPage.TOGGLEABLE_OPTIONS),
			new PebItem(4, "Particle Settings",
				"Choose how many particles are shown for abilities of various categories. These settings can also be changed using the /particles command.", NamedTextColor.GRAY,
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
		String objectiveName = Objects.requireNonNull(category.mObjectiveName);
		return new PebItem(slot, gui -> name + ": " + ScoreboardUtils.getScoreboardValue(gui.mPlayer, objectiveName).orElse(100) + "%",
				gui -> description + ". Left click to increase, right click to decrease. Hold shift to increase/decrease in smaller steps.", NamedTextColor.GRAY,
				material, false).switchToPage(PebPage.TOGGLEABLE_OPTIONS)
				.action((gui, event) -> {
					int value = ScoreboardUtils.getScoreboardValue(gui.mPlayer, objectiveName).orElse(100);
					value += (event.isLeftClick() ? 1 : -1) * (event.isShiftClick() ? 5 : 20);
					value = Math.max(0, Math.min(value, PlayerData.MAX_PARTIAL_PARTICLE_VALUE));
					ScoreboardUtils.setScoreboardValue(gui.mPlayer, objectiveName, value);
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
			for (PebItem item : PEB_ITEMS.getOrDefault(mCurrentPage, List.of())) {
				if (item.mSlot == chosenSlot) {
					if (item.mAction != null) {
						item.mAction.accept(this, event);
					}
					return;
				}
			}
			for (PebItem item : PEB_ITEMS.getOrDefault(PebPage.COMMON, List.of())) {
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
				Arrays.asList("", "~~~~~~~~~~~", "Input a number", "from 1-65 above."))
			                      .reopenIfFail(false)
			                      .response((player, strings) -> {
				                      int inputVal;
				                      try {
					                      inputVal = Integer.parseInt(strings[0]);
				                      } catch (Exception e) {
					                      player.sendMessage("Input is not an integer.");
					                      return false;
				                      }
				                      if (inputVal >= 1 && inputVal <= 65) {
					                      player.performCommand("pickup threshold " + strings[0]);
					                      return false;
				                      } else {
					                      player.sendMessage("Input is not with the bounds of 1 - 65.");
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
		TextComponent lore = item.mLore.apply(this);
		GUIUtils.splitLoreLine(meta, lore, 30, true);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		newItem.setItemMeta(meta);
		ItemUtils.setPlainName(newItem);
		return newItem;
	}

	private void setLayout(PebPage page) {
		mCurrentPage = page;

		mInventory.clear();
		for (PebItem item : PEB_ITEMS.getOrDefault(mCurrentPage, List.of())) {
			mInventory.setItem(item.mSlot, createCustomItem(item, mPlayer));
		}
		for (PebItem item : PEB_ITEMS.getOrDefault(PebPage.COMMON, List.of())) {
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
