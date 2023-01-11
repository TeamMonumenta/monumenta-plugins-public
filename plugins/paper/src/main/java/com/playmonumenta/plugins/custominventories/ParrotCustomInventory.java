package com.playmonumenta.plugins.custominventories;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.libraryofsouls.SoulEntry;
import com.playmonumenta.libraryofsouls.SoulsDatabase;
import com.playmonumenta.libraryofsouls.bestiary.BestiaryManager;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.parrots.ParrotManager;
import com.playmonumenta.plugins.parrots.ParrotManager.ParrotVariant;
import com.playmonumenta.plugins.parrots.ParrotManager.PlayerShoulder;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import de.tr7zw.nbtapi.NBTItem;
import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public final class ParrotCustomInventory extends CustomInventory {

	private enum ParrotGUIPage {
		R1(0),
		R2(1),
		SPECIAL(2),
		OTHERS(99);

		private final int mNum;

		ParrotGUIPage(int num) {
			mNum = num;
		}

		public static @Nullable ParrotGUIPage valueOfPage(int pagenum) {
			for (ParrotGUIPage page : ParrotGUIPage.values()) {
				if (page.mNum == pagenum) {
					return page;
				}
			}
			return null;
		}
	}

	private enum ParrotAction {
		NONE,
		SET_LEFT_SHOULDER,
		SET_RIGHT_SHOULDER,
		PLACE
	}

	private static final int ROWS = 4;
	private static final int COLUMNS = 9;

	private static final ItemStack JUNK_BORDER_ITEM = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
	private static final ItemStack JUNK_INTERIOR_ITEM = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

	//this item is the same as the JUNK_BORDER_ITEM but with different PlainName()
	private static final Map<ParrotGUIPage, ItemStack> BORDER_TOPLEFT_MAP = new HashMap<>();

	private static final Map<ParrotGUIPage, ItemStack> SIGN_MAP = new HashMap<>();

	static {
		ItemMeta metaB = JUNK_BORDER_ITEM.getItemMeta();
		metaB.displayName(Component.empty());
		JUNK_BORDER_ITEM.setItemMeta(metaB);

		ItemUtils.setPlainName(JUNK_BORDER_ITEM, "gui_blank");

		ItemMeta metaI = JUNK_INTERIOR_ITEM.getItemMeta();
		metaI.displayName(Component.empty());
		JUNK_INTERIOR_ITEM.setItemMeta(metaI);

		ItemUtils.setPlainName(JUNK_INTERIOR_ITEM, "gui_blank");

		List<Component> lore = new ArrayList<>();

		ItemStack signValley = new ItemStack(Material.OAK_SIGN);
		ItemMeta metaValley = signValley.getItemMeta();
		metaValley.displayName(Component.text("King's Valley Parrots", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text("Purchase and select your", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text("parrots from King's Valley!", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		metaValley.lore(lore);
		signValley.setItemMeta(metaValley);
		ItemUtils.setPlainName(signValley, "King's Valley Parrots");

		SIGN_MAP.put(ParrotGUIPage.R1, signValley);

		lore.clear();
		ItemStack signIsles = new ItemStack(Material.OAK_SIGN);
		ItemMeta metaIsles = signIsles.getItemMeta();
		metaIsles.displayName(Component.text("Celsian Isles Parrots", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text("Purchase and select your", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text("parrots from Celsian Isles!", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		metaIsles.lore(lore);
		signIsles.setItemMeta(metaIsles);
		ItemUtils.setPlainName(signIsles, "Celsian Isles Parrots");

		SIGN_MAP.put(ParrotGUIPage.R2, signIsles);

		lore.clear();
		ItemStack signSpecial = new ItemStack(Material.OAK_SIGN);
		ItemMeta metaSpecial = signSpecial.getItemMeta();
		metaSpecial.displayName(Component.text("Special Parrots", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text("Purchase and select", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text("your Special Parrots", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		metaSpecial.lore(lore);
		signSpecial.setItemMeta(metaSpecial);
		ItemUtils.setPlainName(signSpecial, "Special Parrots");

		SIGN_MAP.put(ParrotGUIPage.SPECIAL, signSpecial);


		ItemStack junkValley = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
		ItemMeta metaJunkValley = junkValley.getItemMeta();
		metaJunkValley.displayName(Component.empty());
		junkValley.setItemMeta(metaJunkValley);

		ItemUtils.setPlainName(junkValley, "ParrotGUIOverlay1");
		BORDER_TOPLEFT_MAP.put(ParrotGUIPage.R1, junkValley);

		ItemStack junkIsles = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
		ItemMeta metaJunkIsles = junkIsles.getItemMeta();
		metaJunkIsles.displayName(Component.empty());
		junkIsles.setItemMeta(metaJunkIsles);

		ItemUtils.setPlainName(junkIsles, "ParrotGUIOverlay2");
		BORDER_TOPLEFT_MAP.put(ParrotGUIPage.R2, junkIsles);

		ItemStack junkSpecial = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
		ItemMeta metaJunkSpecial = junkSpecial.getItemMeta();
		metaJunkSpecial.displayName(Component.empty());
		junkSpecial.setItemMeta(metaJunkSpecial);

		ItemUtils.setPlainName(junkSpecial, "ParrotGUIOverlay99");
		BORDER_TOPLEFT_MAP.put(ParrotGUIPage.SPECIAL, junkSpecial);
	}


	private static final String SCOREBOARD_BOUGHT_SHOULDERS = "ParrotBoth";
	//0 if the player can't have 2 parrot at the same time, otherwise the time of when he bought it


	private final ArrayList<ParrotGuiItem> GUI_ITEMS = new ArrayList<>();
	private final Map<Integer, ParrotGuiItem> mInvMapping = new HashMap<>();
	private ParrotAction mSelectedAction = ParrotAction.NONE;
	private ParrotGUIPage mCurrentPage = ParrotGUIPage.R1;

	private ItemStack loadItemTable(Player playerLoad, String path) throws Exception {
		ItemStack item = InventoryUtils.getItemFromLootTable(playerLoad, NamespacedKeyUtils.fromString(path));
		if (item == null) {
			throw new Exception("Failed to load item '" + path + "' from loot tables");
		}
		return item;
	}

	private void loadItem(Player playerLoad) throws Exception {
		//getting the currencies from loottable
		ItemStack hcs = loadItemTable(playerLoad, "epic:r2/items/currency/hyper_crystalline_shard");
		ItemStack hxp = loadItemTable(playerLoad, "epic:r1/items/currency/hyper_experience");
		ItemStack pulsatingGold = loadItemTable(playerLoad, "epic:r1/items/currency/pulsating_gold");
		ItemStack pulsatingEmerald = loadItemTable(playerLoad, "epic:r2/items/currency/pulsating_emerald");
		ItemStack shardOfTheMantle = loadItemTable(playerLoad, "epic:r1/kaul/crownshard");
		ItemStack titanicKnowledge = loadItemTable(playerLoad, "epic:r2/eldrask/materials/epic_material");
		ItemStack ancestralEffigy = loadItemTable(playerLoad, "epic:r2/lich/materials/ancestral_effigy");
		ItemStack voidstainedGeode = loadItemTable(playerLoad, "epic:r2/depths/loot/voidstained_geode");
		ItemStack persistentParchment = loadItemTable(playerLoad, "epic:r1/delves/rogue/persistent_parchment");
		ItemStack unicornPuke = loadItemTable(playerLoad, "epic:r1/dungeons/4/static_uncommons/unicorn_puke");
		ItemStack blitzDoubloon = loadItemTable(playerLoad, "epic:r1/blitz/blitz_doubloon");

		//==================================================================================================
		//                                     FUNCTIONAL ITEMS
		//                                 DONT modify these items
		//==================================================================================================
		ItemStack shoulderLeft = buildItem(Material.COOKIE, "Shoulder Left", List.of());
		GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 6, shoulderLeft,
			(player, inv) -> mSelectedAction != ParrotAction.SET_LEFT_SHOULDER,
			(player, inv) -> {
				mSelectedAction = ParrotAction.SET_LEFT_SHOULDER;
				return true;
			}));

		ItemStack shoulderRight = buildItem(Material.COOKIE, "Shoulder Right", List.of());
		GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 4, shoulderRight,
			(player, inv) -> mSelectedAction != ParrotAction.SET_RIGHT_SHOULDER,
			(player, inv) -> {
				mSelectedAction = ParrotAction.SET_RIGHT_SHOULDER;
				return true;
			}));

		ItemStack leftShoulder = buildItem(Material.BRICK, "Left Shoulder Selected", List.of());
		GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 6, leftShoulder,
			(player, inv) -> mSelectedAction == ParrotAction.SET_LEFT_SHOULDER,
			(player, inv) -> {
				mSelectedAction = ParrotAction.NONE;
				return true;
			}));

		ItemStack rightShoulder = buildItem(Material.BRICK, "Right Shoulder Selected", List.of());
		GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 4, rightShoulder,
			(player, inv) -> mSelectedAction == ParrotAction.SET_RIGHT_SHOULDER,
			(player, inv) -> {
				mSelectedAction = ParrotAction.NONE;
				return true;
			}));

		ItemStack removeParrots = buildItem(Material.FEATHER, "Remove Parrots", List.of());
		GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 8, removeParrots,
			(player, inv) -> ParrotManager.hasParrotOnShoulders(player),
			(player, inv) -> {
				ParrotManager.clearParrots(player);
				return true;
			}));

		ItemStack visibleParrots = buildItem(Material.SADDLE, "Set parrots visible", List.of());
		GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 2, visibleParrots,
			(player, inv) -> ParrotManager.hasParrotOnShoulders(player) && !ParrotManager.areParrotsVisible(player),
			(player, inv) -> {
				ParrotManager.setParrotVisible(player, true);
				return true;
			}));

		ItemStack invisibleParrots = buildItem(Material.SADDLE, "Set parrots invisible", List.of());
		GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 2, invisibleParrots,
			(player, inv) -> ParrotManager.hasParrotOnShoulders(player) && ParrotManager.areParrotsVisible(player),
			(player, inv) -> {
				ParrotManager.setParrotVisible(player, false);
				return true;
			}));

		ItemStack spawnParrot = buildItem(Material.ARMOR_STAND, "Place Parrots", List.of());
		GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 3, spawnParrot,
			(player, inv) -> ZoneUtils.hasZoneProperty(player.getLocation(), ZoneUtils.ZoneProperty.PLOT)
				                 && ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.CURRENT_PLOT).orElse(-1) == ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.OWN_PLOT).orElse(-2),
			(player, inv) -> {
				mSelectedAction = ParrotAction.PLACE;
				return true;
			}));

		ItemStack spawningParrot = buildItem(Material.ARMOR_STAND, "Placing Parrots", List.of());
		GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 3, spawningParrot,
			(player, inv) -> mSelectedAction == ParrotAction.PLACE,
			(player, inv) -> {
				mSelectedAction = ParrotAction.NONE;
				return true;
			}));

		ItemStack bothShoulders = buildItem(Material.INK_SAC, "Buy Both Shoulders", List.of("Click to buy!", "64HCS"));
		GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 5, bothShoulders, ImmutableMap.of(hcs, 64),
			(player, inv) -> !ParrotManager.hasDoubleShoulders(player),
			(player, inv) -> {
				ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_BOUGHT_SHOULDERS, (int) Instant.now().getEpochSecond());
				return true;
			}));

		ItemStack boughtShoulders = buildItem(Material.INK_SAC, "Both Shoulders",
			List.of("Owned", new Date((long) ScoreboardUtils.getScoreboardValue(playerLoad, SCOREBOARD_BOUGHT_SHOULDERS).orElse(0) * 1000).toString()));
		GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 5, boughtShoulders,
			(player, inv) -> ParrotManager.hasDoubleShoulders(player)));


		ItemStack turnRight = buildItem(Material.ARROW, "Turn Page ->", List.of());
		GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 1, turnRight,
			(player, inv) -> mCurrentPage != ParrotGUIPage.SPECIAL,
			(player, inv) -> {
				if (mCurrentPage == ParrotGUIPage.R1) {
					mCurrentPage = ParrotGUIPage.R2;
				} else if (mCurrentPage == ParrotGUIPage.R2) {
					mCurrentPage = ParrotGUIPage.SPECIAL;
				}
				return true;
			}));

		ItemStack turnLeft = buildItem(Material.ARROW, "<- Turn Page", List.of());
		GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 9, turnLeft,
			(player, inv) -> mCurrentPage != ParrotGUIPage.R1,
			(player, inv) -> {
				if (mCurrentPage == ParrotGUIPage.R2) {
					mCurrentPage = ParrotGUIPage.R1;
				} else if (mCurrentPage == ParrotGUIPage.SPECIAL) {
					mCurrentPage = ParrotGUIPage.R2;
				}
				return true;
			}));

		ItemStack lockboxSwapEnabled = buildItem(Material.GRAY_SHULKER_BOX, "Swapping Parrots with Lockboxes: Enabled", List.of());
		ItemUtils.setPlainName(lockboxSwapEnabled, "Loadout Lockbox");
		ItemStatUtils.addPlayerModified(new NBTItem(lockboxSwapEnabled, true))
			.setString(ItemStatUtils.CUSTOM_SKIN_KEY, "Alchemist");
		GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 7, lockboxSwapEnabled,
			(player, inv) -> player.getScoreboardTags().contains(ParrotManager.PARROT_LOCKBOX_SWAP_TAG),
			(player, inv) -> {
				player.getScoreboardTags().remove(ParrotManager.PARROT_LOCKBOX_SWAP_TAG);
				return true;
			}));

		ItemStack lockboxSwapDisabled = buildItem(Material.GRAY_SHULKER_BOX, "Swapping Parrots with Lockboxes: Disabled", List.of());
		ItemUtils.setPlainName(lockboxSwapDisabled, "Loadout Lockbox");
		ItemStatUtils.addPlayerModified(new NBTItem(lockboxSwapDisabled, true))
			.setString(ItemStatUtils.CUSTOM_SKIN_KEY, "Warrior");
		GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 7, lockboxSwapDisabled,
			(player, inv) -> !player.getScoreboardTags().contains(ParrotManager.PARROT_LOCKBOX_SWAP_TAG),
			(player, inv) -> {
				player.getScoreboardTags().add(ParrotManager.PARROT_LOCKBOX_SWAP_TAG);
				return true;
			}));

		//==================================================================================================
		//                                     FUNCTIONAL ITEMS end
		//==================================================================================================


		//from now on there are only separate parrots per page
		//in pairs, first the one to buy then the one bought


		//==================================================================================================
		//                                         R1 parrots
		//==================================================================================================

		//GREEN
		createParrotItems(playerLoad, ParrotVariant.GREEN, ParrotGUIPage.R1, 10, Material.GREEN_WOOL, "ParrotBought4",
			ImmutableMap.of(hxp, 48));

		//GRAY
		createParrotItems(playerLoad, ParrotVariant.GRAY, ParrotGUIPage.R1, 11, Material.LIGHT_GRAY_WOOL, "ParrotBought5",
			ImmutableMap.of(hxp, 48));

		//GOLD
		createParrotItems(playerLoad, ParrotVariant.PULSATING_GOLD, ParrotGUIPage.R1, 12, Material.YELLOW_CONCRETE, "ParrotBought7",
			ImmutableMap.of(pulsatingGold, 64));

		//BEE Parrot!
		createParrotItems(playerLoad, ParrotVariant.BEE, ParrotGUIPage.R1, 13, Material.HONEYCOMB_BLOCK, "ParrotBought16",
			ImmutableMap.of(hxp, 80));

		//Radiant
		createParrotItems(playerLoad, ParrotVariant.RADIANT, ParrotGUIPage.R1, 14, Material.GLOWSTONE, "ParrotBought17",
			List.of("Defeat Arena of Terth to learn more about this parrot"),
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, "Arena").orElse(0) != 0,
			ImmutableMap.of(hxp, 48));

		//Kaul!
		createParrotItems(playerLoad, ParrotVariant.KAUL, ParrotGUIPage.R1, 15, Material.JUNGLE_LEAVES, "ParrotBought10",
			List.of("Requires 50 Kaul wins to buy", "You still need " + (50 - ScoreboardUtils.getScoreboardValue(playerLoad, "KaulWins").orElse(0)) + " wins"),
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, "KaulWins").orElse(0) >= 50,
			ImmutableMap.of(shardOfTheMantle, 80));

		//Plunderer's Blitz
		createParrotItems(playerLoad, ParrotVariant.BLITZ, ParrotGUIPage.R1, 16, Material.RED_WOOL, "ParrotBought19",
			List.of("Requires having beaten the 50th round in Plunderer's Blitz",
				"You have reached round " + (ScoreboardUtils.getScoreboardValue(playerLoad, "Blitz").orElse(1) - 1)),
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, "Blitz").orElse(0) > 50,
			ImmutableMap.of(blitzDoubloon, 128));

		int corridorsScore = ScoreboardUtils.getScoreboardValue(playerLoad, "RogEndless").orElse(0);
		createParrotItems(playerLoad, ParrotVariant.CORRIDORS, ParrotGUIPage.R1, 19, Material.NETHER_WART_BLOCK, "ParrotBought20",
			List.of("Requires clearing floor 12 from Ephemeral Corridors", "You have cleared floor " + corridorsScore),
			(player, inv) -> ScoreboardUtils.getScoreboardValue(playerLoad, "RogEndless").orElse(0) > 12,
			ImmutableMap.of(persistentParchment, 24));

		//==================================================================================================
		//                                         R1 parrots end
		//==================================================================================================

		//==================================================================================================
		//                                         R2 parrots
		//==================================================================================================

		//RED PARROT
		createParrotItems(playerLoad, ParrotVariant.RED, ParrotGUIPage.R2, 10, Material.RED_WOOL, "ParrotBought1",
			ImmutableMap.of(hcs, 48));

		//BLUE PARROT
		createParrotItems(playerLoad, ParrotVariant.BLUE, ParrotGUIPage.R2, 11, Material.BLUE_WOOL, "ParrotBought2",
			ImmutableMap.of(hcs, 48));

		//BLUE-YELLOW
		createParrotItems(playerLoad, ParrotVariant.CYAN, ParrotGUIPage.R2, 12, Material.LIGHT_BLUE_WOOL, "ParrotBought3",
			ImmutableMap.of(hcs, 48));

		//EMERALD
		createParrotItems(playerLoad, ParrotVariant.PULSATING_EMERALD, ParrotGUIPage.R2, 13, Material.GREEN_CONCRETE, "ParrotBought8",
			ImmutableMap.of(pulsatingEmerald, 64));

		//PIRATE
		createParrotItems(playerLoad, ParrotVariant.PIRATE, ParrotGUIPage.R2, 14, Material.PURPLE_WOOL, "ParrotBought9",
			ImmutableMap.of(hcs, 80));

		//Snowy
		createParrotItems(playerLoad, ParrotVariant.SNOWY, ParrotGUIPage.R2, 15, Material.SNOW_BLOCK, "ParrotBought13",
			ImmutableMap.of(hcs, 80));

		//Eldrask
		createParrotItems(playerLoad, ParrotVariant.ELDRASK, ParrotGUIPage.R2, 16, Material.BLUE_ICE, "ParrotBought11",
			List.of("Requires 50 Eldrask wins to buy", "You still need " + (50 - ScoreboardUtils.getScoreboardValue(playerLoad, "FGWins").orElse(0)) + " wins"),
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, "FGWins").orElse(0) >= 50,
			ImmutableMap.of(titanicKnowledge, 80));

		//Hekawt - Lich
		createParrotItems(playerLoad, ParrotVariant.HEKAWT, ParrotGUIPage.R2, 19, Material.MAGMA_BLOCK, "ParrotBought18",
			List.of("Requires 50 Hekawt wins to buy", "You still need " + (50 - ScoreboardUtils.getScoreboardValue(playerLoad, "LichWins").orElse(0)) + " wins"),
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, "LichWins").orElse(0) >= 50,
			ImmutableMap.of(ancestralEffigy, 80));

		//Depths
		int depthsScore = ScoreboardUtils.getScoreboardValue(playerLoad, "DepthsEndless").orElse(0);
		createParrotItems(playerLoad, ParrotVariant.DEPTHS, ParrotGUIPage.R2, 20, Material.RED_GLAZED_TERRACOTTA, "ParrotBought14",
			List.of("Requires clearing floor 9 from Darkest Depths", "You have cleared floor " + ((depthsScore - 1) / 10)),
			(player, inv) -> ScoreboardUtils.getScoreboardValue(playerLoad, "DepthsEndless").orElse(0) >= 91,
			ImmutableMap.of(voidstainedGeode, 64),
			// unlock requirements hidden (and not purchasable) until having beaten Depths at least once
			List.of("Defeat Darkest Depths to learn more about this parrot"),
			(player, inv) -> ScoreboardUtils.getScoreboardValue(playerLoad, "Depths").orElse(0) > 0);

		//Depths Upgrade
		createParrotItems(playerLoad, ParrotVariant.DEPTHS_UPGRADE, ParrotGUIPage.R2, 21, Material.CRYING_OBSIDIAN, "ParrotBought15",
			List.of("Requires clearing floor 12 from Darkest Depths", "You have cleared floor " + ((depthsScore - 1) / 10)),
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, "DepthsEndless").orElse(0) >= 121,
			ImmutableMap.of(voidstainedGeode, 96),
			// unlock requirements hidden (and not purchasable) until the previous depths parrot has been bought
			List.of("You have to unlock Otherworldly Myiopsitta", "to learn more about this parrot"),
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, "ParrotBought14").orElse(0) > 0);

		//==================================================================================================
		//                                         R2 parrots end
		//==================================================================================================

		//==================================================================================================
		//                                         Specials parrots
		//==================================================================================================

		// Parteon parrot
		// This one cannot be bought in-game. This also means it has no purchase date.
		ItemStack buyPatreon = buildItem(Material.ORANGE_WOOL, "Patreon Parakeet", List.of("Become a Tier 1 patreon to unlock"));
		GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.SPECIAL.mNum, 10, buyPatreon,
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.PATREON_DOLLARS).orElse(0) < Constants.PATREON_TIER_1 && mSelectedAction == ParrotAction.NONE));

		ItemStack boughtPatreon = buildItem(Material.ORANGE_WOOL, "Patreon Parakeet", List.of("Owned"));
		GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.SPECIAL.mNum, 10, boughtPatreon,
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.PATREON_DOLLARS).orElse(0) >= Constants.PATREON_TIER_1 && mSelectedAction == ParrotAction.NONE));
		GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.SPECIAL.mNum, 10, boughtPatreon,
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.PATREON_DOLLARS).orElse(0) >= Constants.PATREON_TIER_1 && mSelectedAction != ParrotAction.NONE,
			(player, inv) -> {
				selectParrot(player, ParrotVariant.PATREON);
				return true;
			}));


		//PRIDE PARROT!
		List<String> rainbowCost = new ArrayList<>();
		rainbowCost.add("You have to unlock these parrots");
		rainbowCost.add("before being able to purchase this one:");
		if (ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought1").orElse(0) == 0) {
			rainbowCost.add("Scarlet Macaw");
		} else {
			rainbowCost.add("##Scarlet Macaw");
		}
		if (ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought2").orElse(0) == 0) {
			rainbowCost.add("Hyacinth Macaw");
		} else {
			rainbowCost.add("##Hyacinth Macaw");
		}
		if (ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought3").orElse(0) == 0) {
			rainbowCost.add("Blue-Yellow Macaw");
		} else {
			rainbowCost.add("##Blue-Yellow Macaw");
		}
		if (ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought4").orElse(0) == 0) {
			rainbowCost.add("Green Parakeet");
		} else {
			rainbowCost.add("##Green Parakeet");
		}
		if (ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought5").orElse(0) == 0) {
			rainbowCost.add("Gray Cockatiel");
		} else {
			rainbowCost.add("##Gray Cockatiel");
		}
		createParrotItems(playerLoad, ParrotVariant.RAINBOW, ParrotGUIPage.SPECIAL, 11, Material.YELLOW_GLAZED_TERRACOTTA,
			"ParrotBought12", rainbowCost,
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, "ParrotBought1").orElse(0) > 0 &&
				                 ScoreboardUtils.getScoreboardValue(player, "ParrotBought2").orElse(0) > 0 &&
				                 ScoreboardUtils.getScoreboardValue(player, "ParrotBought3").orElse(0) > 0 &&
				                 ScoreboardUtils.getScoreboardValue(player, "ParrotBought4").orElse(0) > 0 &&
				                 ScoreboardUtils.getScoreboardValue(player, "ParrotBought5").orElse(0) > 0,
			ImmutableMap.of(
				hcs, 32,
				hxp, 32,
				unicornPuke, 1));

		List<String> twistedMobsKilled = List.of("AlricLordofFrostedWinds", "YeigarLastEmperor", "SalazarArchitectofViridia", "XenoShatteredScalllawag", "IsadoratheBloodiedQueen", "CTelsketCrimsonConqueror", "AesirLightbringer");
		boolean twistedUnlocked = true;

		for (String twistedMobName : twistedMobsKilled) {
			SoulEntry entry = SoulsDatabase.getInstance().getSoul(twistedMobName);
			if (entry != null) {
				int kills = BestiaryManager.getKillsForMob(playerLoad, entry);
				if (kills <= 0) {
					twistedUnlocked = false;
					break;
				}
			}
		}

		if (twistedUnlocked) {
			createParrotItems(playerLoad, ParrotVariant.TWISTED, ParrotGUIPage.SPECIAL, 12, Material.BEDROCK, "ParrotBought21", ImmutableMap.of());
		} else {
			ItemStack stack = buildItem(Material.BEDROCK, ChatColor.MAGIC + "Twisted?????", List.of());
			ItemUtils.setPlainName(stack, ParrotVariant.TWISTED.getName());
			GUI_ITEMS.add(new ParrotGuiItem(ParrotGUIPage.SPECIAL.mNum, 12, stack, null, null, null));
		}

		//==================================================================================================
		//                                         Special parrots end
		//==================================================================================================


	}

	private void createParrotItems(Player playerLoad, ParrotVariant variant, ParrotGUIPage page, int slot, Material itemMaterial, String scoreboard,
	                               ImmutableMap<ItemStack, Integer> cost) {
		createParrotItems(playerLoad, variant, page, slot, itemMaterial, scoreboard, null, null, cost, null, null);
	}

	private void createParrotItems(Player playerLoad, ParrotVariant variant, ParrotGUIPage page, int slot, Material itemMaterial, String scoreboard,
	                               List<String> requirementsLore, BiPredicate<Player, Inventory> requirements, ImmutableMap<ItemStack, Integer> cost) {
		createParrotItems(playerLoad, variant, page, slot, itemMaterial, scoreboard, requirementsLore, requirements, cost, null, null);
	}

	private void createParrotItems(Player playerLoad, ParrotVariant variant, ParrotGUIPage page, int slot, Material itemMaterial, String scoreboard,
	                               @Nullable List<String> requirementsLore, @Nullable BiPredicate<Player, Inventory> requirements, ImmutableMap<ItemStack, Integer> cost,
	                               @Nullable List<String> hiddenUnlockLore, @Nullable BiPredicate<Player, Inventory> showUnlockRequirements) {

		// "hidden" parrot (cannot be bought and unlock condition is hidden)
		if (hiddenUnlockLore != null && showUnlockRequirements != null) {
			ItemStack hiddenDisplayItem = buildItem(itemMaterial, variant.getName(), hiddenUnlockLore);
			GUI_ITEMS.add(new ParrotGuiItem(page.mNum, slot, hiddenDisplayItem,
				(player, inv) -> mSelectedAction == ParrotAction.NONE
					                 && ScoreboardUtils.getScoreboardValue(player, scoreboard).orElse(0) == 0
					                 && !showUnlockRequirements.test(player, inv)));
		}

		// locked parrot (cannot be bought yet)
		if (requirementsLore != null && requirements != null) {
			ItemStack lockedDisplayItem = buildItem(itemMaterial, variant.getName(), requirementsLore);
			GUI_ITEMS.add(new ParrotGuiItem(page.mNum, slot, lockedDisplayItem,
				(player, inv) -> mSelectedAction == ParrotAction.NONE
					                 && ScoreboardUtils.getScoreboardValue(player, scoreboard).orElse(0) == 0
					                 && !requirements.test(player, inv)
					                 && (showUnlockRequirements == null || showUnlockRequirements.test(player, inv))));
		}

		// buyable parrot
		List<String> costLore = new ArrayList<>();
		costLore.add("Click to buy!");
		for (Map.Entry<ItemStack, Integer> entry : cost.entrySet()) {
			costLore.add(entry.getValue() + " " + ItemUtils.getPlainName(entry.getKey()));
		}
		ItemStack buyDisplayItem = buildItem(itemMaterial, "Buy " + variant.getName(), costLore);
		GUI_ITEMS.add(new ParrotGuiItem(page.mNum, slot, buyDisplayItem, cost,
			(player, inv) -> mSelectedAction == ParrotAction.NONE
				                 && ScoreboardUtils.getScoreboardValue(player, scoreboard).orElse(0) == 0
				                 && (requirements == null || requirements.test(player, inv))
				                 && (showUnlockRequirements == null || showUnlockRequirements.test(player, inv)),
			(player, inv) -> {
				ScoreboardUtils.setScoreboardValue(player, scoreboard, (int) Instant.now().getEpochSecond());
				return true;
			}));

		// unclickable bought parrot
		ItemStack boughtDisplayItem = buildItem(itemMaterial, variant.getName(),
			List.of("Owned", new Date((long) ScoreboardUtils.getScoreboardValue(playerLoad, scoreboard).orElse(0) * 1000).toString()));
		GUI_ITEMS.add(new ParrotGuiItem(page.mNum, slot, boughtDisplayItem,
			(player, inv) -> mSelectedAction == ParrotAction.NONE && ScoreboardUtils.getScoreboardValue(player, scoreboard).orElse(0) > 0));

		// clickable bought parrot
		GUI_ITEMS.add(new ParrotGuiItem(page.mNum, slot, boughtDisplayItem,
			(player, inv) -> mSelectedAction != ParrotAction.NONE && ScoreboardUtils.getScoreboardValue(player, scoreboard).orElse(0) > 0,
			(player, inv) -> {
				selectParrot(player, variant);
				return true;
			}));

	}

	private void selectParrot(Player player, ParrotVariant variant) {
		switch (mSelectedAction) {
			case SET_LEFT_SHOULDER -> ParrotManager.selectParrot(player, variant, PlayerShoulder.LEFT);
			case SET_RIGHT_SHOULDER -> ParrotManager.selectParrot(player, variant, PlayerShoulder.RIGHT);
			case PLACE -> ParrotManager.spawnParrot(player, variant);
			default -> {
			}
		}
	}

	public ItemStack buildItem(Material mat, String name, List<String> lore) {
		ItemStack newItem = new ItemStack(mat, 1);
		ItemMeta meta = newItem.getItemMeta();

		Component cName = Component.text(name, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false);

		if (name.endsWith("(u)")) {
			cName = cName.decoration(TextDecoration.OBFUSCATED, true);
		}

		meta.displayName(cName);

		if (!lore.isEmpty()) {
			List<Component> mLore = new ArrayList<>();
			for (String sLore : lore) {
				if (!sLore.contains("1970")) {
					if (sLore.startsWith("##")) {
						mLore.add(Component.text(sLore.substring(2), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.STRIKETHROUGH, true));
					} else {
						mLore.add(Component.text(sLore, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
					}
				}
			}
			meta.lore(mLore);
		}

		newItem.setItemMeta(meta);

		ItemUtils.setPlainName(newItem, name);
		return newItem;
	}

/**------------Parrot GUI layout-------------
 *
 * ==================== R1 - R2 - Special ==============================
 * |   X  |   X  |   X  |   X  |   X  |   X  |   X  |   X  |   X  |
 * |   X  |   p  |   p  |   p  |   p  |   p  |   p  |   p  |   X  |
 * |   X  |   p  |   p  |   p  |   p  |   p  |   p  |   p  |   X  |
 * |   X  |   p  |   p  |   p  |   p  |   p  |   p  |   p  |   X  |
 * |   X  |   p  |   p  |   p  |   p  |   p  |   p  |   p  |   X  |
 * |  <-  |Remove|   X  |  SL  |double|  SR  |   X  |   V  |  ->  |
 * =====================================================================
 *
 * Where:
 *  X -> black stained glass pane (Junk border item)
 *  p -> parrot
 *  SL -> Shoulder Left
 *  SR -> Shoulder Right
 *  Double -> make you buy the option to have 2 different parrot
 *  remove -> clear currents parrot
 *  V -> SetParrotVisible true or false
 *  arrow to swap pages
 *
 * Parrots slots = {10-16, 19-25, 28-34, 37-43}
 */
	public ParrotCustomInventory(Player owner) throws Exception {
		super(owner, ROWS * COLUMNS, "Parrots");
		loadItem(owner);
		owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.NEUTRAL, 3f, 1.2f);

		new BukkitRunnable() {
			@Override
			public void run() {
				updateInventory(owner);
			}
		}.runTaskLater(Plugin.getInstance(), 2);
	}

	public void updateInventory(Player player) {
		mInventory.clear();
		mInvMapping.clear();

		if (SIGN_MAP.get(mCurrentPage) != null) {
			mInventory.setItem(4, SIGN_MAP.get(mCurrentPage));
		}

		for (ParrotGuiItem gItem : GUI_ITEMS) {
			ParrotGUIPage itemPage = ParrotGUIPage.valueOfPage(gItem.getPage());
			if (itemPage != null && (itemPage == mCurrentPage || itemPage == ParrotGUIPage.OTHERS) && gItem.isVisible(player, mInventory)) {
				mInventory.setItem(gItem.getSlot(), gItem.getShowedItem());
				mInvMapping.put(gItem.getSlot(), gItem);
			}
		}

		fillWithJunk(mInventory);

	}

	public void fillWithJunk(Inventory inventory) {
		if (BORDER_TOPLEFT_MAP.get(mCurrentPage) != null) {
			inventory.setItem(0, BORDER_TOPLEFT_MAP.get(mCurrentPage));
		}

		for (int i = 0; i < (ROWS*COLUMNS); i++) {
			if (inventory.getItem(i) == null) {
				if (i < COLUMNS || i > ((ROWS - 1)*COLUMNS) || (i % COLUMNS == 0) || (i % COLUMNS == COLUMNS - 1)) {
					inventory.setItem(i, JUNK_BORDER_ITEM);
				} else {
					inventory.setItem(i, JUNK_INTERIOR_ITEM);
				}
			}
		}
	}



	@Override
	public void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);

		if (event.isShiftClick()) {
			return;
		}

		if (!mInventory.equals(event.getClickedInventory())) {
			return;
		}

		ItemStack currentItem = event.getCurrentItem();

		if (currentItem == null
				|| currentItem.getType() == JUNK_BORDER_ITEM.getType()
				|| currentItem.getType() == JUNK_INTERIOR_ITEM.getType()) {
			//if the player press the junk item nothing happen    // Magikarp use SPLASH!
			return;
		}

		if (currentItem.getType() == Material.OAK_SIGN) {
			return;
		}

		int slotClicked = event.getSlot();
		ParrotGuiItem gItem = mInvMapping.get(slotClicked);
		Player whoClicked = (Player) event.getWhoClicked();
		Inventory inventory = event.getClickedInventory();

		if (gItem != null && gItem.doesSomethingOnClick()) {
			if (gItem.canPurchase(whoClicked)) {
				if (gItem.purchase(whoClicked)) {
					whoClicked.playSound(whoClicked.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundCategory.NEUTRAL, 10f, 1.3f);
					gItem.afterClick(whoClicked, inventory);
					updateInventory(whoClicked);
				} else {
					whoClicked.sendMessage(Component.text("[SYSTEM]", NamedTextColor.RED).decoration(TextDecoration.BOLD, true)
							                       .append(Component.text(" Error! please contact a mod! fail with purchasing.", NamedTextColor.RED).decoration(TextDecoration.BOLD, false)));
				}
			} else {
				whoClicked.sendMessage(Component.text("You don't have enough currency to pay for this item.", NamedTextColor.RED));
			}
		}
	}


}
