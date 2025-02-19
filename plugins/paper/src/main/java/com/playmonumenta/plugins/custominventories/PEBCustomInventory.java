package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.abilities.AbilityHotbar;
import com.playmonumenta.plugins.abilities.scout.EagleEye;
import com.playmonumenta.plugins.effects.RespawnStasis;
import com.playmonumenta.plugins.guis.CustomTradeGui;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.itemstats.enchantments.Multitool;
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.particle.ParticleManager;
import com.playmonumenta.plugins.protocollib.EntityEquipmentReplacer;
import com.playmonumenta.plugins.protocollib.RecipeBookGUIOpener;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
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
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class PEBCustomInventory extends CustomInventory {
	private static final Material FILLER = GUIUtils.FILLER_MATERIAL;

	public enum PebPage {
		COMMON,
		MAIN,
		PLAYER_INFO,
		GAMEPLAY_OPTIONS,
		TECHNICAL_OPTIONS,
		TRADE_GUI,
		INTERACTABLE_OPTIONS,
		SERVER_INFO,
		BOOK_SKINS,
		WOOL_BOOK_SKINS,
		PICKUP_AND_DISABLE_DROP,
		GLOWING,
		ROCKET_JUMP,
		PARTIAL_PARTICLES,
		SOUND_CONTROLS,
		SOUND_CATEGORIES,
		SOUND_OVERWORLD_PLOTS,
		SOUND_DELAYS,
		SOUND_TP_CUTOFFS,
		FINISHER_VISIBILTY,
	}

	private static class PebItem {
		int mSlot;
		Function<PEBCustomInventory, String> mName;
		Function<PEBCustomInventory, TextComponent> mLore;
		Material mType;
		ArrayList<BiConsumer<PEBCustomInventory, InventoryClickEvent>> mActions;
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
			mActions = new ArrayList<>();
		}

		public PebItem playerCommand(String command) {
			mActions.add((gui, event) -> {
				if (mCloseAfter) {
					gui.mPlayer.closeInventory();
				}
				gui.mPlayer.performCommand(command);
			});
			return this;
		}

		public PebItem playerMessage(String message) {
			mActions.add((gui, event) -> {
				gui.mPlayer.sendMessage(message);
			});
			return this;
		}

		public PebItem serverCommand(String command) {
			mActions.add((gui, event) -> {
				String finalCommand = command.replace("@S", gui.mPlayer.getName());
				NmsUtils.getVersionAdapter().runConsoleCommandSilently(finalCommand);
				if (mCloseAfter) {
					gui.mPlayer.closeInventory();
				}
			});
			return this;
		}

		public PebItem toggleTag(String tag, Component giveTagMessage, Component removeTagMessage) {
			mActions.add((gui, event) -> {
				HumanEntity player = event.getWhoClicked();
				if (ScoreboardUtils.toggleTag(player, tag)) {
					player.sendMessage(giveTagMessage);
				} else {
					player.sendMessage(removeTagMessage);
				}
			});
			return this;
		}

		public PebItem action(BiConsumer<PEBCustomInventory, InventoryClickEvent> action) {
			mActions.add(action);
			return this;
		}

		public PebItem switchToPage(PebPage page) {
			mActions.add((gui, event) -> gui.setLayout(page));
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
			new PebItem(0, "", "", NamedTextColor.LIGHT_PURPLE, FILLER, false), // TODO: why is this needed? we manually override it in setLayout
			new PebItem(4, "Main Menu",
				"A list of commonly used options, along with menu buttons to reach the full lists.", NamedTextColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, false),
			new PebItem(19, "Filtered Pickup and Disabled Drop",
				"Click to choose your pickup and disabled drop preferences.", NamedTextColor.LIGHT_PURPLE,
				Material.DIRT, false).switchToPage(PebPage.PICKUP_AND_DISABLE_DROP),
			new PebItem(20, "Particle Options",
				"Click to choose how many particles will be shown for different categories. Also available under Gameplay/Combat options.", NamedTextColor.LIGHT_PURPLE,
				Material.NETHER_STAR, false).switchToPage(PebPage.PARTIAL_PARTICLES),
			new PebItem(21, "Glowing options",
				"Click to choose your preferences for the \"glowing\" effect. Also available under Gameplay/Combat options.", NamedTextColor.LIGHT_PURPLE,
				Material.SPECTRAL_ARROW, false).switchToPage(PebPage.GLOWING),

			new PebItem(23, "Music Options",
				"Click to choose your preferences across a wide variety of music", NamedTextColor.LIGHT_PURPLE,
				Material.JUKEBOX, false).switchToPage(PebPage.SOUND_CONTROLS),
			new PebItem(24, "Dailies",
				"Click to see what daily content you have and haven't done today.", NamedTextColor.LIGHT_PURPLE,
				Material.ACACIA_BOAT, true).playerCommand("clickable peb_dailies"),
			new PebItem(25, "Dungeon Instances",
				"Click to view what dungeon instances you have open, and how old they are.", NamedTextColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, true).playerCommand("clickable peb_dungeoninfo"),

			new PebItem(37, "Gameplay/Combat Options",
				"Particle options, skill-related toggles, and other toggle related to combat.", NamedTextColor.LIGHT_PURPLE,
				Material.DIAMOND_SWORD, false).switchToPage(PebPage.GAMEPLAY_OPTIONS),
			new PebItem(38, "Technical Options",
				"Dungeon auto-abandon, world name spoofing, GUI options, and other technical enhancements.", NamedTextColor.LIGHT_PURPLE,
				Material.COMPARATOR, false).switchToPage(PebPage.TECHNICAL_OPTIONS),
			new PebItem(39, "Trigger/Interactable Options",
				"Offhand Swap, Filtered Pickup, and more options to change or disable triggers.", NamedTextColor.LIGHT_PURPLE,
				Material.SHIELD, false).switchToPage(PebPage.INTERACTABLE_OPTIONS),

			new PebItem(41, "Player Information",
				"Details about your Class, Dailies, and other player-focused options.", NamedTextColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, false).switchToPage(PebPage.PLAYER_INFO),
			new PebItem(42, "Server Information",
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
				Material.STONE_SWORD, true).action((peb, event) -> {
				peb.mInventory.close();
				new ClassDisplayCustomInventory(peb.mPlayer).open();
			}),
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

		definePage(PebPage.GAMEPLAY_OPTIONS,
			new PebItem(4, "Gameplay Options",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.DIAMOND_SWORD, false),
			new PebItem(20, "Particle Options",
				"Click to choose how many particles will be shown for different categories.", NamedTextColor.LIGHT_PURPLE,
				Material.NETHER_STAR, false).switchToPage(PebPage.PARTIAL_PARTICLES),
			new PebItem(21, "Glowing Options",
				"Click to choose your preferences for the \"glowing\" effect.", NamedTextColor.LIGHT_PURPLE,
				Material.SPECTRAL_ARROW, false).switchToPage(PebPage.GLOWING),
			new PebItem(22, "Music Options",
				"Click to choose your preferences across a wide variety of music", NamedTextColor.LIGHT_PURPLE,
				Material.JUKEBOX, false).switchToPage(PebPage.SOUND_CONTROLS),
			new PebItem(23, "Passive ability sounds",
				"Click to toggle whether some sounds from long-lasting ability effects and enchantments are played.", NamedTextColor.LIGHT_PURPLE,
				Material.NOTE_BLOCK, false).action((pebCustomInventory, event) -> {
				boolean disabled = ScoreboardUtils.toggleTag((Player) event.getWhoClicked(), AbilityUtils.PASSIVE_SOUNDS_DISABLED_TAG);
				event.getWhoClicked().sendMessage(Component.text("Passive ability sounds are now " + (disabled ? "disabled" : "enabled"), NamedTextColor.GOLD, TextDecoration.BOLD));
			}),
			new PebItem(24, "Inventory Drink",
				"Click to toggle drinking potions with a right click in any inventory.", NamedTextColor.LIGHT_PURPLE,
				Material.GLASS_BOTTLE, false).playerCommand("clickable peb_tid"),
			new PebItem(29, "Ability Hotbar",
				"Click to toggle ability HUD.", NamedTextColor.LIGHT_PURPLE,
				Material.PAPER, false).action((pebCustomInventory, event) -> {
				boolean enabled = ScoreboardUtils.toggleTag((Player) event.getWhoClicked(), AbilityHotbar.ABILITY_HOTBAR_TAG);
				event.getWhoClicked().sendMessage(Component.text("Ability Hotbar is now " + (enabled ? "enabled" : "disabled"), NamedTextColor.GOLD, TextDecoration.BOLD));
			}),
			new PebItem(30, "Toggle Darksight",
				"Click to toggle whether Darksight provides Night Vision", NamedTextColor.LIGHT_PURPLE,
				Material.LANTERN, false).serverCommand("execute as @S run function monumenta:mechanisms/darksight_toggle"),
			new PebItem(31, "Toggle Radiant",
				"Click to toggle whether Radiant provides Night Vision.", NamedTextColor.LIGHT_PURPLE,
				Material.SOUL_LANTERN, false).serverCommand("execute as @S run function monumenta:mechanisms/radiant_toggle"),
			new PebItem(32, "Rocket Jump",
				"Click to enable or disable Rocket Jump", NamedTextColor.LIGHT_PURPLE,
				Material.FIREWORK_ROCKET, false).switchToPage(PebPage.ROCKET_JUMP),
			new PebItem(33, "Cloned Finisher Elites Visibility",
				"Click to toggle whether cloned elites in finishers glow and are visible.", NamedTextColor.LIGHT_PURPLE,
				Material.ZOMBIE_HEAD, false).switchToPage(PebPage.FINISHER_VISIBILTY),
			new PebItem(4 * 9 + 3, "Toggle Spectating After Death",
				"Click to toggle whether you spectate the area in which you die for 3 seconds after dying.", NamedTextColor.LIGHT_PURPLE,
				Material.ENDER_EYE, false).toggleTag(RespawnStasis.SPECTATE_DISABLE_TAG,
				Component.text("Spectating after death is now disabled.", NamedTextColor.GOLD, TextDecoration.BOLD),
				Component.text("Spectating after death is now enabled.", NamedTextColor.GOLD, TextDecoration.BOLD)
			),
			new PebItem(4 * 9 + 4, "Disable Depth Strider while Riptiding",
				"Click to toggle whether depth strider is disabled constantly while holding a riptide trident, or only while riptiding.", NamedTextColor.LIGHT_PURPLE,
				Material.TRIDENT, false).action((pebCustomInventory, event) -> {
				boolean onlyDuringRiptide = ScoreboardUtils.toggleTag((Player) event.getWhoClicked(), Constants.Tags.DEPTH_STRIDER_DISABLED_ONLY_WHILE_RIPTIDING);
				event.getWhoClicked().sendMessage(Component.text("Depth Strider is now " + (onlyDuringRiptide ? "only disabled while riptiding." : "constantly disabled while holding a Riptide trident."), NamedTextColor.GOLD, TextDecoration.BOLD));
			})
		);

		definePage(PebPage.TECHNICAL_OPTIONS,
			new PebItem(4, "Technical Options",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.COMPARATOR, false),
			new PebItem(20, gui -> "Toggle display other players' gear (" +
				(ScoreboardUtils.getScoreboardValue(gui.mPlayer, EntityEquipmentReplacer.SCOREBOARD).orElse(1) == 1 ? "Enabled)" : "Disabled)"),
				gui -> Component.text("Toggles if you want to see the gear on other players, which may improve performance ", NamedTextColor.LIGHT_PURPLE),
				Material.LEATHER_HELMET, false).action((peb, action) -> {
					if (ScoreboardUtils.toggleBinaryScoreboard(peb.mPlayer, EntityEquipmentReplacer.SCOREBOARD, 1)) {
						peb.mPlayer.sendMessage(Component.text("Displaying other players' gear has been enabled.", NamedTextColor.GOLD));
					} else {
						peb.mPlayer.sendMessage(Component.text("Displaying other players' gear has been disabled.", NamedTextColor.GOLD));
					}
					peb.refresh();
				}),
			new PebItem(21, "Spawner Equipment",
				"Click to toggle whether mob equipment is displayed in spawners (significantly decreases FPS in many areas)", NamedTextColor.LIGHT_PURPLE,
				Material.SPAWNER, false).playerCommand("clickable peb_spawnerequipment"),
			new PebItem(22, "Trading GUI Options",
				"Click to choose your NPC trading preferences.", NamedTextColor.LIGHT_PURPLE,
				Material.DEEPSLATE_EMERALD_ORE, false).switchToPage(PebPage.TRADE_GUI),
			new PebItem(23, "Compass Particles",
				"Click to toggle a trail of guiding particles when following the quest compass.", NamedTextColor.LIGHT_PURPLE,
				Material.COMPASS, false).playerCommand("clickable peb_comp_particles"),
			new PebItem(24, "Show name on patron buff announcement",
				Component.text("Toggles whether your IGN is in the announcement when they activate ", NamedTextColor.LIGHT_PURPLE)
					.append(Component.text("Patreon ", NamedTextColor.GOLD))
					.append(Component.text("buffs.", NamedTextColor.LIGHT_PURPLE)),
				Material.GLOWSTONE, false).playerCommand("clickable toggle_patron_buff_thank"),
			new PebItem(29, "Shattered and Region Scaling Messages", "Click to toggle whether you receive actionbar messages when you have equipment that is shattered or debuffed based on region scaling.", NamedTextColor.LIGHT_PURPLE,
				Material.DAMAGED_ANVIL, false)
				.action((peb, event) -> {
					if (ScoreboardUtils.toggleTag(peb.mPlayer, Shattered.MESSAGE_DISABLE_TAG)) {
						peb.mPlayer.sendMessage(Component.text("Shattered and Region Scaling actionbar messages have been disabled!", NamedTextColor.GOLD));
					} else {
						peb.mPlayer.sendMessage(Component.text("Shattered and Region Scaling actionbar messages have been enabled!", NamedTextColor.GOLD));
					}
				}),
			new PebItem(30, "Show Overworld POI Titles", "Click to enable or disable seeing " +
				                                             "titles appear upon entering certain " +
				                                             "Overworld Points of Interest.", NamedTextColor.LIGHT_PURPLE,
				Material.BIRCH_SIGN, false)
				.action((peb, event) -> {
					if (ScoreboardUtils.toggleBinaryScoreboard(peb.mPlayer, "POITitles")) {
						peb.mPlayer.sendMessage(Component.text("Overworld POI Titles have been disabled!", NamedTextColor.GOLD));
					} else {
						peb.mPlayer.sendMessage(Component.text("Overworld POI Titles have been enabled!", NamedTextColor.GOLD));
					}
				}),
			new PebItem(31, "Auto-Abandon Completed Dungeons",
				"Click to disable or enable automatically abandoning completed dungeon instances when a new week starts.", NamedTextColor.LIGHT_PURPLE,
				Material.DAYLIGHT_DETECTOR, false).serverCommand("execute as @S run function monumenta:mechanisms/auto_dungeon_abandon_toggle"),
			new PebItem(32, "Recipe Book Opening Player Details",
				"Click here to disable or enable clicking on the recipe book opening the Player Details GUI", NamedTextColor.LIGHT_PURPLE,
				Material.KNOWLEDGE_BOOK, false).action((peb, event) -> {
					if (ScoreboardUtils.toggleTag(peb.mPlayer, RecipeBookGUIOpener.DISABLE_TAG)) {
						peb.mPlayer.sendMessage(Component.text("The recipe book will now open the vanilla recipes menu.", NamedTextColor.GOLD));
					} else {
						peb.mPlayer.sendMessage(Component.text("The recipe book will now open the Player Details GUI.", NamedTextColor.GOLD));
					}
			}),
			new PebItem(33, "Virtual Firmament",
				"Click to toggle Virtual Firmament, which visually turns your Firmament into a stack of blocks for faster placement.", NamedTextColor.LIGHT_PURPLE,
				Material.PRISMARINE, false).playerCommand("virtualfirmament"),
			new PebItem(39, "Spoof World Names",
				"Click to enable or disable spoofing of shard-specific world names. This is helpful for world map mods to be able to detect worlds better.", NamedTextColor.LIGHT_PURPLE,
				Material.CARTOGRAPHY_TABLE, false).playerCommand("toggleworldnames"),
			new PebItem(40, "Resource Pack GUI Textures",
				"Click to enable or disable the Monumenta Resource Pack applying background textures to various GUIs. (Requires v5.0.0 or higher to work when enabled.)", NamedTextColor.LIGHT_PURPLE,
				Material.BEEHIVE, false).playerCommand("guitextures"),
			new PebItem(41, "Simplified Tab List",
				"Click to enable or disable the simplified tab list, removing custom effects from the tab list."
				+ " Recommended if using an up-to-date version of the Unofficial Monumenta Mod (1.9.8+).", NamedTextColor.LIGHT_PURPLE,
				Material.FLOWER_BANNER_PATTERN, false)
				.action((peb, event) -> {
					if (peb.mPlayer.hasPermission("monumenta.tablist.simplified")) {
						LuckPermsIntegration.setPermission(peb.mPlayer, "monumenta.tablist.simplified", false);
						peb.mPlayer.sendMessage(Component.text("Simplified Tab List has been disabled!", NamedTextColor.GOLD));
					} else {
						LuckPermsIntegration.setPermission(peb.mPlayer, "monumenta.tablist.simplified", true);
						peb.mPlayer.sendMessage(Component.text("Simplified Tab List has been enabled!", NamedTextColor.GOLD));
					}
				})
		);

		// Trade GUI Options
		definePage(PebPage.TRADE_GUI,
			new PebItem(4, "Trading GUI Options",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.DEEPSLATE_EMERALD_ORE, false),
			new PebItem(10, gui -> "General: ",
				gui -> Component.text("", NamedTextColor.LIGHT_PURPLE),
				Material.BIRCH_SIGN, false),
			new PebItem(19, gui -> "Custom Trade GUI: ",
				gui -> Component.text("Toggles between vanilla UI and custom GUI. \n\nCurrent: " + (ScoreboardUtils.getScoreboardValue(gui.mPlayer, CustomTradeGui.MAIN).orElse(1) == 1 ? "Custom." : "Vanilla."), NamedTextColor.LIGHT_PURPLE),
				Material.LOOM, false).action((inventory, action) -> {
				int oldValue = ScoreboardUtils.getScoreboardValue(inventory.mPlayer, CustomTradeGui.MAIN).orElse(1);
				ScoreboardUtils.setScoreboardValue(inventory.mPlayer, CustomTradeGui.MAIN, oldValue == 0 ? 1 : 0);
				inventory.setLayout(inventory.mCurrentPage);
			}),
			new PebItem(20, gui -> "Theme: ",
				gui -> Component.text("Toggles between classic and sleek theme. \n\nCurrent: " + (ScoreboardUtils.getScoreboardValue(gui.mPlayer, CustomTradeGui.THEME).orElse(0) == 1 ? "Sleek." : "Classic."), NamedTextColor.LIGHT_PURPLE),
				Material.CARTOGRAPHY_TABLE, false).action((inventory, action) -> {
				int oldValue = ScoreboardUtils.getScoreboardValue(inventory.mPlayer, CustomTradeGui.THEME).orElse(0);
				ScoreboardUtils.setScoreboardValue(inventory.mPlayer, CustomTradeGui.THEME, oldValue == 0 ? 1 : 0);
				inventory.setLayout(inventory.mCurrentPage);
			}),

			new PebItem(14, gui -> "Trade Preview Page: ",
				gui -> Component.text("", NamedTextColor.LIGHT_PURPLE),
				Material.BIRCH_SIGN, false),
			new PebItem(23, gui -> "Trade Spacing: ",
				gui -> Component.text("Toggles between auto, 16, and 28 items per page. \n\nCurrent: " + (ScoreboardUtils.getScoreboardValue(gui.mPlayer, CustomTradeGui.SPACING).orElse(0) == 0 ? "Auto." :
					                                                                                          ScoreboardUtils.getScoreboardValue(gui.mPlayer, CustomTradeGui.SPACING).orElse(0) == 1 ? "16 per page." : "28 per page."), NamedTextColor.LIGHT_PURPLE),
				Material.TRIPWIRE_HOOK, false).action((inventory, action) -> {
				int oldValue = ScoreboardUtils.getScoreboardValue(inventory.mPlayer, CustomTradeGui.SPACING).orElse(0);
				ScoreboardUtils.setScoreboardValue(inventory.mPlayer, CustomTradeGui.SPACING, oldValue == 2 ? 0 : oldValue + 1); // cycles from 0, 1, 2 -> 0
				inventory.setLayout(inventory.mCurrentPage);
			}),

			new PebItem(24, gui -> "Display Price on Preview Items: ",
				gui -> Component.text("Toggles displaying the price of each item on the preview page. \n\nCurrent: " + (ScoreboardUtils.getScoreboardValue(gui.mPlayer, CustomTradeGui.PREVIEWDISPLAY).orElse(0) == 0 ? "True." : "False."), NamedTextColor.LIGHT_PURPLE),
				Material.EMERALD, false).action((inventory, action) -> {
				int oldValue = ScoreboardUtils.getScoreboardValue(inventory.mPlayer, CustomTradeGui.PREVIEWDISPLAY).orElse(0);
				ScoreboardUtils.setScoreboardValue(inventory.mPlayer, CustomTradeGui.PREVIEWDISPLAY, oldValue == 0 ? 1 : 0);
				inventory.setLayout(inventory.mCurrentPage);
			}),
			new PebItem(25, gui -> "Trade organization: ",
				gui -> Component.text("Toggles sorting trades by category or displaying them together. Trade sorting will only occur for rare traders/traders with a large number of items. \n\nCurrent: " + (ScoreboardUtils.getScoreboardValue(gui.mPlayer, CustomTradeGui.TRADEORG).orElse(0) == 0 ? "Split trades by type." : "Display trades together."), NamedTextColor.LIGHT_PURPLE),
				Material.BOOKSHELF, false).action((inventory, action) -> {
				int oldValue = ScoreboardUtils.getScoreboardValue(inventory.mPlayer, CustomTradeGui.TRADEORG).orElse(0);
				ScoreboardUtils.setScoreboardValue(inventory.mPlayer, CustomTradeGui.TRADEORG, oldValue == 0 ? 1 : 0);
				inventory.setLayout(inventory.mCurrentPage);
			}),

			new PebItem(28, gui -> "Trade Logistics: ",
				gui -> Component.text("", NamedTextColor.LIGHT_PURPLE),
				Material.BIRCH_SIGN, false),
			new PebItem(37, gui -> "Confirm page on left-click: ",
					gui -> Component.text("Toggles whether to show a confirm page when left-clicking on a trade preview. Right-clicking will always bring up a confirm page. \n\nCurrent: " + (ScoreboardUtils.getScoreboardValue(gui.mPlayer, CustomTradeGui.CONFIRM).orElse(0) == 0 ? "Enabled." : "Disabled."), NamedTextColor.LIGHT_PURPLE),
				Material.ANVIL, false).action((inventory, action) -> {
				int oldValue = ScoreboardUtils.getScoreboardValue(inventory.mPlayer, CustomTradeGui.CONFIRM).orElse(0);
				ScoreboardUtils.setScoreboardValue(inventory.mPlayer, CustomTradeGui.CONFIRM, oldValue == 0 ? 1 : 0);
				inventory.setLayout(inventory.mCurrentPage);
			}),
			new PebItem(38, gui -> "Quick-buy on shift-click: ",
				gui -> Component.text("Toggles whether to instantly buy 1 when shift-clicking on a trade preview. \n\nCurrent: " + (ScoreboardUtils.getScoreboardValue(gui.mPlayer, CustomTradeGui.QUICKBUY).orElse(0) == 0 ? "Enabled." : "Disabled."), NamedTextColor.LIGHT_PURPLE),
				Material.LIGHTNING_ROD, false).action((inventory, action) -> {
				int oldValue = ScoreboardUtils.getScoreboardValue(inventory.mPlayer, CustomTradeGui.QUICKBUY).orElse(0);
				ScoreboardUtils.setScoreboardValue(inventory.mPlayer, CustomTradeGui.QUICKBUY, oldValue == 0 ? 1 : 0);
				inventory.setLayout(inventory.mCurrentPage);
			}),
			new PebItem(39, gui -> "Upon successful trade: ",
				gui -> Component.text("Toggles between: return to preview page, close GUI, or do nothing. \n\nCurrent: " +
					                      (ScoreboardUtils.getScoreboardValue(gui.mPlayer, CustomTradeGui.SUCCESS).orElse(2) == 0 ? "Return to preview." :
						                       ScoreboardUtils.getScoreboardValue(gui.mPlayer, CustomTradeGui.SUCCESS).orElse(2) == 1 ? "Close GUI." : "Do nothing."), NamedTextColor.LIGHT_PURPLE),
				Material.GLOW_ITEM_FRAME, false).action((inventory, action) -> {
				int oldValue = ScoreboardUtils.getScoreboardValue(inventory.mPlayer, CustomTradeGui.SUCCESS).orElse(2);
				ScoreboardUtils.setScoreboardValue(inventory.mPlayer, CustomTradeGui.SUCCESS, oldValue == 2 ? 0 : oldValue + 1); // cycles from 0, 1, 2 -> 0
				inventory.setLayout(inventory.mCurrentPage);
			}),

			new PebItem(32, gui -> "Misc: ",
				gui -> Component.text("", NamedTextColor.LIGHT_PURPLE),
				Material.BIRCH_SIGN, false),
			new PebItem(41, gui -> "Trade particle effects: ",
				gui -> Component.text("Toggles particle effects upon successful trade. \n\nCurrent: " + (ScoreboardUtils.getScoreboardValue(gui.mPlayer, CustomTradeGui.PARTICLES).orElse(0) == 0 ? "Enabled." : "Disabled."), NamedTextColor.LIGHT_PURPLE),
				Material.FIREWORK_ROCKET, false).action((inventory, action) -> {
				int oldValue = ScoreboardUtils.getScoreboardValue(inventory.mPlayer, CustomTradeGui.PARTICLES).orElse(0);
				ScoreboardUtils.setScoreboardValue(inventory.mPlayer, CustomTradeGui.PARTICLES, oldValue == 0 ? 1 : 0);
				inventory.setLayout(inventory.mCurrentPage);
			}),
			new PebItem(42, gui -> "Trade sound effects: ",
				gui -> Component.text("Toggles sound effects upon successful trade. \n\nCurrent: " + (ScoreboardUtils.getScoreboardValue(gui.mPlayer, CustomTradeGui.SOUNDS).orElse(0) == 0 ? "Enabled." : "Disabled."), NamedTextColor.LIGHT_PURPLE),
				Material.BELL, false).action((inventory, action) -> {
				int oldValue = ScoreboardUtils.getScoreboardValue(inventory.mPlayer, CustomTradeGui.SOUNDS).orElse(0);
				ScoreboardUtils.setScoreboardValue(inventory.mPlayer, CustomTradeGui.SOUNDS, oldValue == 0 ? 1 : 0);
				inventory.setLayout(inventory.mCurrentPage);
			}),
			new PebItem(43, gui -> "Wallet integration: ",
				gui -> Component.text("Toggles whether to take currency directly from your wallet. \n\nCurrent: " +
					                      (ScoreboardUtils.getScoreboardValue(gui.mPlayer, CustomTradeGui.WALLET).orElse(0) == 0 ? "Enabled, prioritize inventory." :
						                       ScoreboardUtils.getScoreboardValue(gui.mPlayer, CustomTradeGui.WALLET).orElse(0) == 1 ? "Disabled." : "Enabled, prioritize wallet."), NamedTextColor.LIGHT_PURPLE),
				Material.FLOWER_POT, false).action((inventory, action) -> {
				int oldValue = ScoreboardUtils.getScoreboardValue(inventory.mPlayer, CustomTradeGui.WALLET).orElse(0);
				ScoreboardUtils.setScoreboardValue(inventory.mPlayer, CustomTradeGui.WALLET, oldValue == 2 ? 0 : oldValue + 1);
				inventory.setLayout(inventory.mCurrentPage);
			})
		);

		// Toggle-able Options
		definePage(PebPage.INTERACTABLE_OPTIONS,
			new PebItem(4, "Trigger/Interact-able Options",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.LEVER, false),
			new PebItem(29, "Filtered Pickup and Disabled Drop",
				"Click to choose your pickup and disabled drop preferences.", NamedTextColor.LIGHT_PURPLE,
				Material.DIRT, false).switchToPage(PebPage.PICKUP_AND_DISABLE_DROP),
			new PebItem(30, "Block Interactions",
				"Click to disable or enable interactions with blocks (looms, crafting tables, beds, etc.)", NamedTextColor.LIGHT_PURPLE,
				Material.LOOM, false).playerCommand("blockinteractions"),
			new PebItem(31, "Multitool Trigger",
				"""
					Click to change the trigger of swapping a held Multitool item between one of the following possible values:
					- Right Click
					- Swap Hands
					- Drop
					- Disabled

					Note that you can always swap Multitool items via the swap key in your inventory.""", NamedTextColor.LIGHT_PURPLE,
				Material.IRON_PICKAXE, false)
				.action((peb, event) -> {
					int value = ScoreboardUtils.getScoreboardValue(peb.mPlayer, Multitool.MULTITOOL_TRIGGER_OPTION_SCORE).orElse(0);
					int newValue = (value + 1) % 4;
					ScoreboardUtils.setScoreboardValue(peb.mPlayer, Multitool.MULTITOOL_TRIGGER_OPTION_SCORE, newValue);
					String key = switch (newValue) {
						case Multitool.TRIGGER_OPTION_RIGHT_CLICK -> "right click";
						case Multitool.TRIGGER_OPTION_SWAP -> "swap";
						case Multitool.TRIGGER_OPTION_DROP -> "drop";
						default -> null;
					};
					event.getWhoClicked().sendMessage(Component.text(key != null ? "Multitool now swaps when pressing " + key : "Multitool swapping is now disabled in the main hand", NamedTextColor.GOLD, TextDecoration.BOLD));
				}),
			new PebItem(32, "Offhand Swapping",
				"Click to toggle whether pressing your swap key will be fully cancelled or only cancelled when a spellcast does so", NamedTextColor.LIGHT_PURPLE,
				Material.SHIELD, false).playerCommand("toggleswap"),
			new PebItem(33, "Offhand Swapping in Inventory",
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
				Material.OBSERVER, false).switchToPage(PebPage.INTERACTABLE_OPTIONS),
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
				Material.OBSERVER, false).switchToPage(PebPage.GAMEPLAY_OPTIONS),
			new PebItem(4, "Glowing Settings",
				"Choose for which entity types the glowing effect may be shown. " +
					"If an entity fits into more than one category (e.g. a boss matches both 'mobs' and 'bosses'), it will glow if any of the matching options are enabled.", NamedTextColor.LIGHT_PURPLE,
				Material.SPECTRAL_ARROW, false),
			new PebItem(2 * 9 + 1, "Other Players",
				"Toggle glowing for other players.", NamedTextColor.LIGHT_PURPLE,
				Material.PLAYER_WALL_HEAD, false).playerCommand("glowing toggle other_players"),
			new PebItem(2 * 9 + 2, "Yourself",
				"Toggle glowing for yourself (visible in third-person). Disable this if glowing causes rendering issues.", NamedTextColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, false).playerCommand("glowing toggle self"),
			new PebItem(2 * 9 + 3, "Mobs",
				"Toggle glowing for mobs.", NamedTextColor.LIGHT_PURPLE,
				Material.ZOMBIE_HEAD, false).playerCommand("glowing toggle mobs"),
			new PebItem(2 * 9 + 4, "Elite Mobs",
				"Toggle glowing for elite mobs.", NamedTextColor.LIGHT_PURPLE,
				Material.WITHER_SKELETON_SKULL, false).playerCommand("glowing toggle elites"),
			new PebItem(2 * 9 + 5, "Bosses",
				"Toggle glowing for bosses. Note that pretty much all bosses are mobs, so are affected by that option as well.", NamedTextColor.LIGHT_PURPLE,
				Material.DRAGON_HEAD, false).playerCommand("glowing toggle bosses"),
			new PebItem(2 * 9 + 6, "Invisible Entities",
				"Toggle glowing for invisible entities.", NamedTextColor.LIGHT_PURPLE,
				Material.GLASS, false).playerCommand("glowing toggle invisible"),
			new PebItem(2 * 9 + 7, "Items",
				"Toggle glowing for items (including items from abilities like Iron Tincture).", NamedTextColor.LIGHT_PURPLE,
				Material.IRON_INGOT, false).playerCommand("glowing toggle items"),
			new PebItem(2 * 9 + 8, "Miscellaneous",
				"Toggle glowing for miscellaneous entities, i.e. entities that don't fit into any other category.", NamedTextColor.LIGHT_PURPLE,
				Material.IRON_NUGGET, false).playerCommand("glowing toggle misc"),
			new PebItem(3 * 9 + 3, "Enable All",
				"Enable glowing for all entities (default).", NamedTextColor.LIGHT_PURPLE,
				Material.GOLD_INGOT, false).playerCommand("glowing enable all"),
			new PebItem(3 * 9 + 5, "Disable All",
				"Disable glowing for all entities.", NamedTextColor.LIGHT_PURPLE,
				Material.DIRT, false).playerCommand("glowing disable all"),
			new PebItem(4 * 9 + 4, "Eagle Eye",
				"Cycle through Eagle Eye options. These options are in addition to any general options set above.", NamedTextColor.LIGHT_PURPLE,
				Material.BOW, false).action((inv, event) -> {
				if (event.getWhoClicked() instanceof Player player) {
					int option = ScoreboardUtils.getScoreboardValue(player, EagleEye.GLOWING_OPTION_SCOREBOARD_NAME).orElse(0);
					if (event.isLeftClick()) {
						option++;
					} else {
						option--;
					}
					option = (option + EagleEye.GlowingOption.values().length) % EagleEye.GlowingOption.values().length;
					ScoreboardUtils.setScoreboardValue(player, EagleEye.GLOWING_OPTION_SCOREBOARD_NAME, option);
					player.sendMessage(Component.text(EagleEye.GlowingOption.values()[option].mDescription, NamedTextColor.GOLD));
				}
			})
		);

		// Rocket Jump Option
		definePage(PebPage.ROCKET_JUMP,
			new PebItem(0, "Back to Toggleable Options",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.OBSERVER, false).switchToPage(PebPage.GAMEPLAY_OPTIONS),
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

		// Elite Finisher Visibility Options
		definePage(PebPage.FINISHER_VISIBILTY,
			new PebItem(0, "Back to Toggleable Options",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.OBSERVER, false).switchToPage(PebPage.GAMEPLAY_OPTIONS),
			new PebItem(4, "Elite Finisher Visibility Settings",
				"Choose how cloned mobs in your elite finishers are visible.", NamedTextColor.LIGHT_PURPLE,
				Material.ZOMBIE_HEAD, false),
			new PebItem(19, "Show and Glow",
				"Cloned Elites from your elite finisher are visible and glow.", NamedTextColor.LIGHT_PURPLE,
				Material.GLOW_INK_SAC, false).serverCommand("execute as @S run function monumenta:mechanisms/finisher_show_glow"),
			new PebItem(21, "Hide and Glow",
				"Cloned Elites from your elite finisher are hidden but still glow.", NamedTextColor.LIGHT_PURPLE,
				Material.GLOWSTONE_DUST, false).serverCommand("execute as @S run function monumenta:mechanisms/finisher_glow"),
			new PebItem(23, "Show and Don't Glow",
				"Cloned Elites from your elite finisher are visible and do not glow.", NamedTextColor.LIGHT_PURPLE,
				Material.SKELETON_SKULL, false).serverCommand("execute as @S run function monumenta:mechanisms/finisher_show"),
			new PebItem(25, "Hide and Don't Glow",
				"Cloned Elites from your elite finisher are hidden and do not glow.", NamedTextColor.LIGHT_PURPLE,
				Material.BARRIER, false).serverCommand("execute as @S run function monumenta:mechanisms/finisher_hide")
		);

		// Partial particle settings
		definePage(PebPage.PARTIAL_PARTICLES,
			new PebItem(0, "Back to Toggleable Options",
				"", NamedTextColor.GRAY,
				Material.OBSERVER, false).switchToPage(PebPage.GAMEPLAY_OPTIONS),
			new PebItem(4, "Particle Settings",
				"Choose how many particles are shown for abilities of various categories. These settings can also be changed using the /particles command.", NamedTextColor.GRAY,
				Material.NETHER_STAR, false),
			makePartialParticlePebItem(11, "Particle multiplier for your own emojis", Material.GLOW_INK_SAC, ParticleCategory.OWN_EMOJI),
			makePartialParticlePebItem(15, "Particle multiplier for other players' emojis", Material.GLOW_INK_SAC, ParticleCategory.OTHER_EMOJI),
			makePartialParticlePebItem(19, "Particle multiplier for your own active abilities", Material.PLAYER_HEAD, ParticleCategory.OWN_ACTIVE),
			makePartialParticlePebItem(20, "Particle multiplier for your own passive abilities", Material.FIREWORK_STAR, ParticleCategory.OWN_PASSIVE),
			makePartialParticlePebItem(21, "Particle multiplier for active effects on you, e.g. the Defensive Line buff", Material.ENDER_PEARL, ParticleCategory.OWN_BUFF),
			makePartialParticlePebItem(23, "Particle multiplier for other players' active abilities", Material.PLAYER_WALL_HEAD, ParticleCategory.OTHER_ACTIVE),
			makePartialParticlePebItem(24, "Particle multiplier for other players' passive abilities", Material.FIREWORK_STAR, ParticleCategory.OTHER_PASSIVE),
			makePartialParticlePebItem(25, "Particle multiplier for active effects on other players, e.g. the Defensive Line buff", Material.ENDER_PEARL, ParticleCategory.OTHER_BUFF),
			makePartialParticlePebItem(39, "Particle multiplier for bosses' abilities", Material.DRAGON_HEAD, ParticleCategory.BOSS),
			makePartialParticlePebItem(40, "Particle multiplier for active effects on enemies, e.g. Spellshock's Static", Material.ENDER_PEARL, ParticleCategory.ENEMY_BUFF),
			makePartialParticlePebItem(41, "Particle multiplier for non-boss enemies' abilities", Material.ZOMBIE_HEAD, ParticleCategory.ENEMY)
		);

		// Sound Controls
		definePage(PebPage.SOUND_CONTROLS,
			new PebItem(4, "Sound Options",
				"Use the menus and options below to customize your audio experience within Monumenta.", NamedTextColor.LIGHT_PURPLE,
				Material.JUKEBOX, false),
			new PebItem(20, "Sound Categories",
				"Control the settings related to categories such as boss music, strike music, and city music.", NamedTextColor.LIGHT_PURPLE,
				Material.CHEST, false).switchToPage(PebPage.SOUND_CATEGORIES),
			new PebItem(22, "Overworld and Plots",
				"Toggle between original, custom, and disabled music for the overworlds and plots.", NamedTextColor.LIGHT_PURPLE,
				Material.COMPASS, false).switchToPage(PebPage.SOUND_OVERWORLD_PLOTS),
			new PebItem(24, "Sound Delays",
				"Toggle whether music constantly plays in certain areas.", NamedTextColor.LIGHT_PURPLE,
				Material.CLOCK, true).switchToPage(PebPage.SOUND_DELAYS),

			new PebItem(40, "Enable Teleporter Cutoffs",
				"Using teleporters will stop the currently playing song.", NamedTextColor.LIGHT_PURPLE,
				Material.ENDER_PEARL, false).serverCommand("scoreboard players set @S MusicStopResetTpToggle 0").playerMessage("Using teleporters will now stop the currently playing song."),
			new PebItem(49, "Disable Teleporter Cutoffs",
				"Using teleporters will not stop the currently playing song.", NamedTextColor.LIGHT_PURPLE,
				Material.BARRIER, false).serverCommand("scoreboard players set @S MusicStopResetTpToggle 1").playerMessage("Using teleporters will no longer stop the currently playing song.")

		);

		// Sound Categories
		definePage(PebPage.SOUND_CATEGORIES,
			new PebItem(4, "Sound Categories",
				"Use the options below to choose settings for certain categories of music.", NamedTextColor.LIGHT_PURPLE,
				Material.CHEST, false),
			new PebItem(20, "City Music",
				"Enable or disable music while in cities.", NamedTextColor.LIGHT_PURPLE,
				Material.STONE_BRICKS, false),
			new PebItem(29, "Enable",
				"Cities' themes will play when inside their borders.", NamedTextColor.LIGHT_PURPLE,
				Material.CAMPFIRE, false).serverCommand("scoreboard players set @S MusicCity 0").playerMessage("Cities' themes now play when inside their borders."),
			new PebItem(38, "Disable",
				"City music will not play.", NamedTextColor.LIGHT_PURPLE,
				Material.BARRIER, false).serverCommand("scoreboard players set @S MusicCity 1").playerMessage("City music will now not play."),

			new PebItem(21, "Boss Music",
				"Enable or disable boss music.", NamedTextColor.LIGHT_PURPLE,
				Material.DIAMOND_SWORD, false),
			new PebItem(30, "Enable",
				"A boss’ theme will play during the battle with them.", NamedTextColor.LIGHT_PURPLE,
				Material.WITHER_SKELETON_SKULL, false).serverCommand("scoreboard players set @S MusicBoss 0").playerMessage("A boss’ theme will now play during the battle with them."),
			new PebItem(39, "Disable",
				"Boss music will not play.", NamedTextColor.LIGHT_PURPLE,
				Material.BARRIER, false).serverCommand("scoreboard players set @S MusicBoss 1").playerMessage("Boss music will now not play."),

			new PebItem(22, "Dungeon Music",
				"Enable or disable dungeon music.", NamedTextColor.LIGHT_PURPLE,
				Material.WOODEN_PICKAXE, false),
			new PebItem(31, "Enable",
				"Ambient dungeon themes will play when inside the instance.", NamedTextColor.LIGHT_PURPLE,
				Material.LANTERN, false).serverCommand("scoreboard players set @S MusicDungeon 0").playerMessage("Ambient dungeon themes will now play when inside the instance."),
			new PebItem(40, "Disable",
				"Dungeon music will not play aside from boss fights and certain cinematic moments.", NamedTextColor.LIGHT_PURPLE,
				Material.BARRIER, false).serverCommand("scoreboard players set @S MusicDungeon 1").playerMessage("Dungeon music will now not play aside from boss fights and certain cinematic moments."),

			new PebItem(23, "Strike Music",
				"Enable or disable strike music.", NamedTextColor.LIGHT_PURPLE,
				Material.IRON_AXE, false),
			new PebItem(32, "Enable",
				"Strike music will play when inside the instance.", NamedTextColor.LIGHT_PURPLE,
				Material.IRON_SWORD, false).serverCommand("scoreboard players set @S MusicStrike 0").playerMessage("Strike music will now play when inside the instance."),
			new PebItem(41, "Disable",
				"Strike music will not play aside from boss fights and certain cinematic moments.", NamedTextColor.LIGHT_PURPLE,
				Material.BARRIER, false).serverCommand("scoreboard players set @S MusicStrike 1").playerMessage("Strike music will now not play aside from boss fights and certain cinematic moments."),

			new PebItem(24, "Miscellaneous Music",
				"Enable or disable miscellaneous music.", NamedTextColor.LIGHT_PURPLE,
				Material.ITEM_FRAME, false),
			new PebItem(33, "Enable",
				"Music that does not fit the other categories, (such as minigames and certain cinematic moments) will play.", NamedTextColor.LIGHT_PURPLE,
				Material.SHEEP_SPAWN_EGG, false).serverCommand("scoreboard players set @S MusicMisc 0").playerMessage("Miscellaneous music will now play."),
			new PebItem(42, "Disable",
				"Miscellaneous music will not play.", NamedTextColor.LIGHT_PURPLE,
				Material.BARRIER, false).serverCommand("scoreboard players set @S MusicMisc 1").playerMessage("Miscellaneous music will no longer play.")
		);

		// Sound Overworld/Plots
		definePage(PebPage.SOUND_OVERWORLD_PLOTS,
			new PebItem(4, "Overworld and Plots",
				"Use the options below to choose settings for certain categories of music.", NamedTextColor.LIGHT_PURPLE,
				Material.COMPASS, false),
			new PebItem(11, "King’s Valley Music",
				"Choose what sounds to play in the King's Valley", NamedTextColor.LIGHT_PURPLE,
				Material.JUNGLE_SAPLING, false),
			new PebItem(20, "Official Theme",
				"The official theme of the King’s Valley will play when in the wilderness of that region.", NamedTextColor.LIGHT_PURPLE,
				Material.GOLD_INGOT, false).serverCommand("scoreboard players set @S MusicOverworldValley 0").playerMessage("The official theme of the King's Valley will play in wilderness."),
			new PebItem(29, "Custom Theme",
				"The \"customoverworldvalley.ogg\" file in your custom music resource pack will be played in the wilderness of the King’s Valley.", NamedTextColor.LIGHT_PURPLE,
				Material.IRON_INGOT, false).serverCommand("scoreboard players set @S MusicOverworldValley 1").playerMessage("The custom music file you set will now play in the King's Valley wilderness."),
			new PebItem(38, "No Theme",
				"No music will be played in the wilderness of the King’s Valley.", NamedTextColor.LIGHT_PURPLE,
				Material.BARRIER, false).serverCommand("scoreboard players set @S MusicOverworldValley 2").playerMessage("No music will be played in the wilderness of King's Valley."),

			new PebItem(12, "Celsian Isles Music",
				"Choose what sounds to play in the Celsian Isles", NamedTextColor.LIGHT_PURPLE,
				Material.SAND, false),
			new PebItem(21, "Official Theme",
				"The official theme of false Celsian Isles will play when in the wilderness of that region.", NamedTextColor.LIGHT_PURPLE,
				Material.EMERALD, false).serverCommand("scoreboard players set @S MusicOverworldIsles 0").playerMessage("The official theme of the Celsian Isles will play in wilderness."),
			new PebItem(30, "Custom Theme",
				"The \"customoverworldisles.ogg\" file in your custom music resource pack will be played in the wilderness of the Celsian Isles.", NamedTextColor.LIGHT_PURPLE,
				Material.AMETHYST_SHARD, false).serverCommand("scoreboard players set @S MusicOverworldIsles 1").playerMessage("The custom music file you set will now play in the Celsian Isles wilderness."),
			new PebItem(39, "No Theme",
				"No music will be played in the wilderness of the Celsian Isles", NamedTextColor.LIGHT_PURPLE,
				Material.BARRIER, false).serverCommand("scoreboard players set @S MusicOverworldIsles 2").playerMessage("No music will be played in the wilderness of Celsian Isles."),

			new PebItem(13, "Silvaria Music",
				"Choose what sounds to play in Silvaria", NamedTextColor.LIGHT_PURPLE,
				Material.RED_MUSHROOM_BLOCK, false),
			new PebItem(22, "Official Theme",
				"The official theme of Silvaria will play when in the wilderness of that region.", NamedTextColor.LIGHT_PURPLE,
				Material.DIAMOND, false).serverCommand("scoreboard players set @S MusicOverworldRing 0").playerMessage("The official theme of Silvaria will play in wilderness."),
			new PebItem(31, "Custom Theme",
				"The \"customoverworldring.ogg\" file in your custom music resource pack will be played in the wilderness of Silvaria.", NamedTextColor.LIGHT_PURPLE,
				Material.NETHER_STAR, false).serverCommand("scoreboard players set @S MusicOverworldRing 1").playerMessage("The custom music file you set will now play in Silvaria wilderness."),
			new PebItem(40, "No Theme",
				"No music will be played in the wilderness of Silvaria.", NamedTextColor.LIGHT_PURPLE,
				Material.BARRIER, false).serverCommand("scoreboard players set @S MusicOverworldRing 2").playerMessage("No music will be played in the wilderness of Silvaria."),

			new PebItem(14, "Star Point Music",
				"Choose what sounds to play in Star Point", NamedTextColor.LIGHT_PURPLE,
				Material.AMETHYST_BLOCK, false),
			new PebItem(23, "Official Theme",
				"The official theme of the Star Point will play when in the wilderness of that region.", NamedTextColor.LIGHT_PURPLE,
				Material.ENDER_EYE, false).serverCommand("scoreboard players set @S MusicOverworldStarpoint 0").playerMessage("The official theme of the Star Point will play in wilderness."),
			new PebItem(32, "Custom Theme",
				"The \"customoverworldstarpoint.ogg\" file in your custom music resource pack will be played in the wilderness of the Star Point.", NamedTextColor.LIGHT_PURPLE,
				Material.ENDER_PEARL, false).serverCommand("scoreboard players set @S MusicOverworldStarpoint 1").playerMessage("The custom music file you set will now play in the Star Point wilderness."),
			new PebItem(41, "No Theme",
				"No music will be played in the wilderness of the Star Point.", NamedTextColor.LIGHT_PURPLE,
				Material.BARRIER, false).serverCommand("scoreboard players set @S MusicOverworldStarpoint 2").playerMessage("No music will be played in the wilderness of Star Point."),

			new PebItem(15, "Plots and Playerplots Music",
				"Enable or disable plots/playerplots music.", NamedTextColor.LIGHT_PURPLE,
				Material.CRAFTING_TABLE, false),
			new PebItem(24, "Official Theme",
				"The official Plots theme will be played when on the Plots and Playerplots shards.", NamedTextColor.LIGHT_PURPLE,
				Material.RED_BED, false).serverCommand("scoreboard players set @S MusicPlots 0").playerMessage("The official Plots theme will now play on the Plots and Playerplots shards."),
			new PebItem(33, "Shuffled Themes",
				"A pre-selected playlist of peaceful music from Monumenta’s soundtrack will be played on shuffle when on the Plots and Playerplots shards", NamedTextColor.LIGHT_PURPLE,
				Material.WHITE_BED, false).serverCommand("scoreboard players set @S MusicPlots 1").playerMessage("Pre-selected peaceful music from Monumenta's soundtrack will now be played on shuffle."),
			new PebItem(42, "Custom Theme",
				"The ‘customplots.ogg’ file in your custom music resource pack will be played when on the Plots and Playerplots shards.", NamedTextColor.LIGHT_PURPLE,
				Material.BLUE_BED, false).serverCommand("scoreboard players set @S MusicPlots 2").playerMessage("Your custom music file will now be played while on Plots and Playerplots shards."),
			new PebItem(51, "No Theme",
				"No music will be played when on the Plots and Playerplots shards", NamedTextColor.LIGHT_PURPLE,
				Material.BARRIER, false).serverCommand("scoreboard players set @S MusicPlots 3").playerMessage("No music will now be played on Plots and Playerplots shards."),

			new PebItem(16, "Lobby Music",
				"Choose what to play in lobbies", NamedTextColor.LIGHT_PURPLE,
				Material.REDSTONE_LAMP, false),
			new PebItem(25, "Monument Theme",
				"The official Monument theme will play in lobbies", NamedTextColor.LIGHT_PURPLE,
				Material.LANTERN, false).serverCommand("scoreboard players set @S MusicLobby 0").playerMessage("The official Monument theme will now play in lobbies."),
			new PebItem(34, "Custom Lobby Music",
				"The \"customoverworldlobby.ogg\" file in your music pack will play in lobbies.", NamedTextColor.LIGHT_PURPLE,
				Material.SOUL_LANTERN, false).serverCommand("scoreboard players set @S MusicLobby 1").playerMessage("The custom music file you set will now play in lobbies."),
			new PebItem(43, "No Theme",
				"No Lobby Music", NamedTextColor.LIGHT_PURPLE,
				Material.BARRIER, false).serverCommand("scoreboard players set @S MusicLobby 2").playerMessage("No music will be played in lobbies.")
		);

		// Sound Delays
		definePage(PebPage.SOUND_DELAYS,
			new PebItem(4, "Sound Delays",
				"Toggle whether music constantly plays in certain areas.", NamedTextColor.LIGHT_PURPLE,
				Material.CLOCK, false),

			new PebItem(21, "Overworld Music Delay",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.BRICKS, false),
			new PebItem(30, "Delay",
				"There will be 10 minutes between the start of each play of overworld music.", NamedTextColor.LIGHT_PURPLE,
				Material.GRASS_BLOCK, false).serverCommand("scoreboard players set @S MusicOverworldDelay 0").playerMessage("There will now be a delay at the start of each play of overworld music."),
			new PebItem(39, "No Delay",
				"There will be no delay between loops of the official theme. There will be 4 minutes between the start of each play of a custom theme.", NamedTextColor.LIGHT_PURPLE,
				Material.BARRIER, false).serverCommand("scoreboard players set @S MusicOverworldDelay 1").playerMessage("There will be no delay between loops of the official theme."),
			new PebItem(22, "Dungeon Music Delay",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.WOODEN_PICKAXE, false),
			new PebItem(31, "Enable",
				"There will be 10 minutes between the start of each play of dungeon music.", NamedTextColor.LIGHT_PURPLE,
				Material.LANTERN, false).serverCommand("scoreboard players set @S MusicDungeonDelay 0").playerMessage("There will now be 10 minutes between the start of each play of dungeon music."),
			new PebItem(40, "Disable",
				"There will be no delay between dungeon music tracks.", NamedTextColor.LIGHT_PURPLE,
				Material.BARRIER, false).serverCommand("scoreboard players set @S MusicDungeonDelay 1").playerMessage("There will now be no delay between dungeon music tracks."),

			new PebItem(23, "Plots Music Delay",
				"", NamedTextColor.LIGHT_PURPLE,
				Material.CRAFTING_TABLE, false),
			new PebItem(32, "Delay",
				"There will be 10 minutes between the start of each play of plots music.", NamedTextColor.LIGHT_PURPLE,
				Material.JUKEBOX, false).serverCommand("scoreboard players set @S MusicPlotsDelay 0").playerMessage("There will now be 10 minutes between the start of each play of plots music."),
			new PebItem(41, "No Delay",
				"There will be no delay between loops of the official track. There will be 4 minutes between the start of each play of a custom theme. ", NamedTextColor.LIGHT_PURPLE,
				Material.BARRIER, false).serverCommand("scoreboard players set @S MusicPlotsDelay 1").playerMessage("There will now be no delay between loops of the plots music.")
		);
	}

	private static boolean isEmojiCategory(ParticleCategory category) {
		return category == ParticleCategory.OWN_EMOJI || category == ParticleCategory.OTHER_EMOJI;
	}

	private static PebItem makePartialParticlePebItem(int slot, String description, Material material, ParticleCategory category) {
		String objectiveName = Objects.requireNonNull(category.mObjectiveName);
		return new PebItem(slot, gui -> category.mDisplayName + ": " + ScoreboardUtils.getScoreboardValue(gui.mPlayer, objectiveName).orElse(100) + "%",
			gui -> description + ". Left click to increase, right click to decrease." + (isEmojiCategory(category) ? "" : " Hold shift to increase/decrease in smaller steps."), NamedTextColor.GRAY,
			material, false).switchToPage(PebPage.PARTIAL_PARTICLES)
			.action((gui, event) -> {
				int value = ScoreboardUtils.getScoreboardValue(gui.mPlayer, objectiveName).orElse(100);
				if (isEmojiCategory(category)) {
					// Only 4 options: 100%, 50%, 25%, 0% (1x, 0.5x, 0.25x, 0x resolution)
					value = (int) (value * (event.isLeftClick() ? 2.0 : 0.5));
					// Handle 0
					if (value == 0 && event.isLeftClick()) {
						value = 25;
					} else if (value < 25) {
						value = 0;
					}
					value = Math.min(100, value);
				} else {
					value += (event.isLeftClick() ? 1 : -1) * (event.isShiftClick() ? 5 : 20);
					value = Math.max(0, Math.min(value, ParticleManager.MAX_PARTIAL_PARTICLE_VALUE));
				}
				ScoreboardUtils.setScoreboardValue(gui.mPlayer, objectiveName, value);
				ParticleManager.updateParticleSettings(gui.mPlayer);
				gui.setLayout(gui.mCurrentPage); // refresh GUI
			});
	}

	private final Player mPlayer;
	private PebPage mCurrentPage;


	public PEBCustomInventory(Player player) {
		this(player, PebPage.MAIN);
	}

	public PEBCustomInventory(Player player, PebPage page) {
		super(player, 54, player.getName() + "'s P.E.B");

		mPlayer = player;
		mCurrentPage = page;

		setLayout(mCurrentPage);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);
		if (event.getWhoClicked() != mPlayer
			    || event.getClickedInventory() != mInventory) {
			return;
		}
		ItemStack clickedItem = event.getCurrentItem();
		if (clickedItem != null && clickedItem.getType() != FILLER) {
			int chosenSlot = event.getSlot();
			for (PebItem item : PEB_ITEMS.getOrDefault(mCurrentPage, List.of())) {
				if (item.mSlot == chosenSlot) {
					for (BiConsumer<PEBCustomInventory, InventoryClickEvent> action : item.mActions) {
						action.accept(this, event);
					}
					return;
				}
			}
			for (PebItem item : PEB_ITEMS.getOrDefault(PebPage.COMMON, List.of())) {
				if (item.mSlot == chosenSlot) {
					for (BiConsumer<PEBCustomInventory, InventoryClickEvent> action : item.mActions) {
						action.accept(this, event);
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
		Material type = item.mType == Material.PLAYER_WALL_HEAD ? Material.PLAYER_HEAD : item.mType;
		ItemStack newItem = GUIUtils.createBasicItem(type, 1, item.mName.apply(this), NamedTextColor.WHITE, false, item.mLore.apply(this), 30, true);
		if (item.mType == Material.PLAYER_HEAD) {
			GUIUtils.setSkullOwner(newItem, player);
		}
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

		GUIUtils.fillWithFiller(mInventory);
		if (page.equals(PebPage.MAIN)) {
			mInventory.setItem(0, GUIUtils.FILLER);
		}
	}

	public void refresh() {
		setLayout(mCurrentPage);
	}
}
