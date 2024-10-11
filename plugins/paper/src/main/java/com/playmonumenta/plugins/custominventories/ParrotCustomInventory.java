package com.playmonumenta.plugins.custominventories;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.libraryofsouls.SoulEntry;
import com.playmonumenta.libraryofsouls.SoulsDatabase;
import com.playmonumenta.libraryofsouls.bestiary.BestiaryManager;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.parrots.ParrotManager;
import com.playmonumenta.plugins.parrots.ParrotManager.ParrotVariant;
import com.playmonumenta.plugins.parrots.ParrotManager.PlayerShoulder;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import de.tr7zw.nbtapi.NBT;
import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ParrotCustomInventory extends Gui {

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
			for (ParrotGUIPage page : values()) {
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

	private static final ItemStack JUNK_BORDER_ITEM = GUIUtils.createBasicItem(Material.BLACK_STAINED_GLASS_PANE, "", NamedTextColor.BLACK);
	private static final ItemStack JUNK_INTERIOR_ITEM = GUIUtils.createBasicItem(Material.GRAY_STAINED_GLASS_PANE, "", NamedTextColor.BLACK);

	//this item is the same as the JUNK_BORDER_ITEM but with different PlainName()
	private static final Map<ParrotGUIPage, ItemStack> BORDER_TOPLEFT_MAP = new HashMap<>();

	private static final Map<ParrotGUIPage, ItemStack> SIGN_MAP = new HashMap<>();

	static {
		ItemUtils.setPlainName(JUNK_BORDER_ITEM, "gui_blank");
		ItemUtils.setPlainName(JUNK_INTERIOR_ITEM, "gui_blank");

		ItemStack signValley = GUIUtils.createBasicItem(Material.OAK_SIGN, "King's Valley Parrots", NamedTextColor.GRAY, true, "Purchase and select your parrots from King's Valley!", NamedTextColor.DARK_GRAY);
		SIGN_MAP.put(ParrotGUIPage.R1, signValley);

		ItemStack signIsles = GUIUtils.createBasicItem(Material.OAK_SIGN, "Celsian Isles Parrots", NamedTextColor.GRAY, true, "Purchase and select your parrots from Celsian Isles!", NamedTextColor.DARK_GRAY);
		SIGN_MAP.put(ParrotGUIPage.R2, signIsles);

		ItemStack signSpecial = GUIUtils.createBasicItem(Material.OAK_SIGN, "Special Parrots", NamedTextColor.GRAY, true, "Purchase and select your Special Parrots!", NamedTextColor.DARK_GRAY);
		SIGN_MAP.put(ParrotGUIPage.SPECIAL, signSpecial);


		ItemStack junkValley = GUIUtils.createBasicItem(Material.BLACK_STAINED_GLASS_PANE, "", NamedTextColor.BLACK);
		ItemUtils.setPlainName(junkValley, "ParrotGUIOverlay1");
		BORDER_TOPLEFT_MAP.put(ParrotGUIPage.R1, junkValley);

		ItemStack junkIsles = GUIUtils.createBasicItem(Material.BLACK_STAINED_GLASS_PANE, "", NamedTextColor.BLACK);
		ItemUtils.setPlainName(junkIsles, "ParrotGUIOverlay2");
		BORDER_TOPLEFT_MAP.put(ParrotGUIPage.R2, junkIsles);

		ItemStack junkSpecial = GUIUtils.createBasicItem(Material.BLACK_STAINED_GLASS_PANE, "", NamedTextColor.BLACK);
		ItemUtils.setPlainName(junkSpecial, "ParrotGUIOverlay99");
		BORDER_TOPLEFT_MAP.put(ParrotGUIPage.SPECIAL, junkSpecial);
	}


	private static final String SCOREBOARD_BOUGHT_SHOULDERS = "ParrotBoth";
	//0 if the player can't have 2 parrot at the same time, otherwise the time of when he bought it


	private final List<ParrotGuiItem> GUI_ITEMS = new ArrayList<>();
	private ParrotAction mSelectedAction = ParrotAction.NONE;
	private ParrotGUIPage mCurrentPage = ParrotGUIPage.R1;

	private ItemStack loadItemTable(String path) throws Exception {
		ItemStack item = InventoryUtils.getItemFromLootTable(mPlayer, NamespacedKeyUtils.fromString(path));
		if (item == null) {
			throw new Exception("Failed to load item '" + path + "' from loot tables");
		}
		return item;
	}

	private void loadItems() throws Exception {
		//getting the currencies from loottable
		ItemStack hcs = loadItemTable("epic:r2/items/currency/hyper_crystalline_shard");
		ItemStack hxp = loadItemTable("epic:r1/items/currency/hyper_experience");
		ItemStack pulsatingGold = loadItemTable("epic:r1/items/currency/pulsating_gold");
		ItemStack pulsatingEmerald = loadItemTable("epic:r2/items/currency/pulsating_emerald");
		ItemStack shardOfTheMantle = loadItemTable("epic:r1/items/currency/shard_of_the_mantle");
		ItemStack titanicKnowledge = loadItemTable("epic:r2/eldrask/materials/epic_material");
		ItemStack ancestralEffigy = loadItemTable("epic:r2/lich/materials/ancestral_effigy");
		ItemStack voidstainedGeode = loadItemTable("epic:r2/depths/loot/voidstained_geode");
		ItemStack persistentParchment = loadItemTable("epic:r1/delves/rogue/persistent_parchment");
		ItemStack unicornPuke = loadItemTable("epic:r1/items/uncommons/lightblue/unicorn_puke");
		ItemStack blitzDoubloon = loadItemTable("epic:r1/blitz/blitz_doubloon");

		//==================================================================================================
		//                                     FUNCTIONAL ITEMS
		//                                 DONT modify these items
		//==================================================================================================
		ItemStack shoulderLeft = buildItem(Material.COOKIE, "Shoulder Left", List.of(), false);
		GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 6, shoulderLeft,
			(player, inv) -> mSelectedAction != ParrotAction.SET_LEFT_SHOULDER,
			(player, inv) -> {
				mSelectedAction = ParrotAction.SET_LEFT_SHOULDER;
				return true;
			}));

		ItemStack shoulderRight = buildItem(Material.COOKIE, "Shoulder Right", List.of(), false);
		GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 4, shoulderRight,
			(player, inv) -> mSelectedAction != ParrotAction.SET_RIGHT_SHOULDER,
			(player, inv) -> {
				mSelectedAction = ParrotAction.SET_RIGHT_SHOULDER;
				return true;
			}));

		ItemStack leftShoulder = buildItem(Material.BRICK, "Left Shoulder Selected", List.of(), false);
		GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 6, leftShoulder,
			(player, inv) -> mSelectedAction == ParrotAction.SET_LEFT_SHOULDER,
			(player, inv) -> {
				mSelectedAction = ParrotAction.NONE;
				return true;
			}));

		ItemStack rightShoulder = buildItem(Material.BRICK, "Right Shoulder Selected", List.of(), false);
		GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 4, rightShoulder,
			(player, inv) -> mSelectedAction == ParrotAction.SET_RIGHT_SHOULDER,
			(player, inv) -> {
				mSelectedAction = ParrotAction.NONE;
				return true;
			}));

		ItemStack removeParrots = buildItem(Material.FEATHER, "Remove Parrots", List.of(), false);
		GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 8, removeParrots,
			(player, inv) -> ParrotManager.hasParrotOnShoulders(player),
			(player, inv) -> {
				ParrotManager.clearParrots(player);
				return true;
			}));

		ItemStack visibleParrots = buildItem(Material.SADDLE, "Set parrots visible", List.of(), false);
		GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 2, visibleParrots,
			(player, inv) -> ParrotManager.hasParrotOnShoulders(player) && !ParrotManager.areParrotsVisible(player),
			(player, inv) -> {
				ParrotManager.setParrotVisible(player, true);
				return true;
			}));

		ItemStack invisibleParrots = buildItem(Material.SADDLE, "Set parrots invisible", List.of(), false);
		GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 2, invisibleParrots,
			(player, inv) -> ParrotManager.hasParrotOnShoulders(player) && ParrotManager.areParrotsVisible(player),
			(player, inv) -> {
				ParrotManager.setParrotVisible(player, false);
				return true;
			}));

		ItemStack spawnParrot = buildItem(Material.ARMOR_STAND, "Place Parrots", List.of(), false);
		GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 3, spawnParrot,
			(player, inv) -> ZoneUtils.hasZoneProperty(player.getLocation(), ZoneUtils.ZoneProperty.PLOT)
				                 && ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.CURRENT_PLOT).orElse(-1) == ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.OWN_PLOT).orElse(-2),
			(player, inv) -> {
				mSelectedAction = ParrotAction.PLACE;
				return true;
			}));

		ItemStack spawningParrot = buildItem(Material.ARMOR_STAND, "Placing Parrots", List.of(), false);
		GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 3, spawningParrot,
			(player, inv) -> mSelectedAction == ParrotAction.PLACE,
			(player, inv) -> {
				mSelectedAction = ParrotAction.NONE;
				return true;
			}));

		ItemStack bothShoulders = buildItem(Material.INK_SAC, "Buy Both Shoulders", List.of("Click to buy!", "64HCS"), false);
		GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 5, bothShoulders, ImmutableMap.of(hcs, 64),
			(player, inv) -> !ParrotManager.hasDoubleShoulders(player),
			(player, inv) -> {
				ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_BOUGHT_SHOULDERS, (int) Instant.now().getEpochSecond());
				return true;
			}));

		ItemStack boughtShoulders = buildItem(Material.INK_SAC, "Both Shoulders",
			List.of("Owned", new Date((long) ScoreboardUtils.getScoreboardValue(mPlayer, SCOREBOARD_BOUGHT_SHOULDERS).orElse(0) * 1000).toString()), false);
		GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 5, boughtShoulders,
			(player, inv) -> ParrotManager.hasDoubleShoulders(player)));


		ItemStack turnRight = buildItem(Material.ARROW, "Turn Page ->", List.of(), false);
		GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 1, turnRight,
			(player, inv) -> mCurrentPage != ParrotGUIPage.SPECIAL,
			(player, inv) -> {
				if (mCurrentPage == ParrotGUIPage.R1) {
					mCurrentPage = ParrotGUIPage.R2;
				} else if (mCurrentPage == ParrotGUIPage.R2) {
					mCurrentPage = ParrotGUIPage.SPECIAL;
				}
				return true;
			}));

		ItemStack turnLeft = buildItem(Material.ARROW, "<- Turn Page", List.of(), false);
		GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 9, turnLeft,
			(player, inv) -> mCurrentPage != ParrotGUIPage.R1,
			(player, inv) -> {
				if (mCurrentPage == ParrotGUIPage.R2) {
					mCurrentPage = ParrotGUIPage.R1;
				} else if (mCurrentPage == ParrotGUIPage.SPECIAL) {
					mCurrentPage = ParrotGUIPage.R2;
				}
				return true;
			}));

		ItemStack lockboxSwapEnabled = buildItem(Material.GRAY_SHULKER_BOX, "Swapping Parrots with Lockboxes: Enabled", List.of(), false);
		ItemUtils.setPlainName(lockboxSwapEnabled, "Loadout Lockbox");
		NBT.modify(lockboxSwapEnabled, nbt -> {
			ItemStatUtils.addPlayerModified(nbt).setString(ItemStatUtils.CUSTOM_SKIN_KEY, "Alchemist");
		});
		GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 7, lockboxSwapEnabled,
			(player, inv) -> player.getScoreboardTags().contains(ParrotManager.PARROT_LOCKBOX_SWAP_TAG),
			(player, inv) -> {
				player.getScoreboardTags().remove(ParrotManager.PARROT_LOCKBOX_SWAP_TAG);
				return true;
			}));

		ItemStack lockboxSwapDisabled = buildItem(Material.GRAY_SHULKER_BOX, "Swapping Parrots with Lockboxes: Disabled", List.of(), false);
		ItemUtils.setPlainName(lockboxSwapDisabled, "Loadout Lockbox");
		NBT.modify(lockboxSwapDisabled, nbt -> {
			ItemStatUtils.addPlayerModified(nbt).setString(ItemStatUtils.CUSTOM_SKIN_KEY, "Warrior");
		});
		GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 7, lockboxSwapDisabled,
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
		createParrotItems(ParrotVariant.GREEN, ParrotGUIPage.R1, 10,
			ImmutableMap.of(hxp, 48));

		//GRAY
		createParrotItems(ParrotVariant.GRAY, ParrotGUIPage.R1, 11,
			ImmutableMap.of(hxp, 48));

		//GOLD
		createParrotItems(ParrotVariant.PULSATING_GOLD, ParrotGUIPage.R1, 12,
			ImmutableMap.of(pulsatingGold, 64));

		//BEE Parrot!
		createParrotItems(ParrotVariant.BEE, ParrotGUIPage.R1, 13,
			ImmutableMap.of(hxp, 80));

		//Radiant
		createParrotItems(ParrotVariant.RADIANT, ParrotGUIPage.R1, 14,
			List.of("Defeat Arena of Terth to learn more about this parrot"),
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, "Arena").orElse(0) != 0,
			ImmutableMap.of(hxp, 48));

		//Kaul!
		createParrotItems(ParrotVariant.KAUL, ParrotGUIPage.R1, 15,
			List.of("Requires 50 Kaul wins to buy", "You still need " + (50 - ScoreboardUtils.getScoreboardValue(mPlayer, "KaulWins").orElse(0)) + " wins"),
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, "KaulWins").orElse(0) >= 50,
			ImmutableMap.of(shardOfTheMantle, 80));

		//Plunderer's Blitz
		createParrotItems(ParrotVariant.BLITZ, ParrotGUIPage.R1, 16,
			List.of("Requires having beaten the 50th round in Plunderer's Blitz",
				"You have reached round " + (ScoreboardUtils.getScoreboardValue(mPlayer, "Blitz").orElse(1) - 1)),
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, "Blitz").orElse(0) > 50,
			ImmutableMap.of(blitzDoubloon, 128));

		int corridorsScore = ScoreboardUtils.getScoreboardValue(mPlayer, "RogEndless").orElse(0);
		createParrotItems(ParrotVariant.CORRIDORS, ParrotGUIPage.R1, 19,
			List.of("Requires clearing floor 12 from Ephemeral Corridors", "You have cleared floor " + corridorsScore),
			(player, inv) -> ScoreboardUtils.getScoreboardValue(mPlayer, "RogEndless").orElse(0) > 12,
			ImmutableMap.of(persistentParchment, 24));

		//==================================================================================================
		//                                         R1 parrots end
		//==================================================================================================

		//==================================================================================================
		//                                         R2 parrots
		//==================================================================================================

		//RED PARROT
		createParrotItems(ParrotVariant.RED, ParrotGUIPage.R2, 10,
			ImmutableMap.of(hcs, 48));

		//BLUE PARROT
		createParrotItems(ParrotVariant.BLUE, ParrotGUIPage.R2, 11,
			ImmutableMap.of(hcs, 48));

		//BLUE-YELLOW
		createParrotItems(ParrotVariant.CYAN, ParrotGUIPage.R2, 12,
			ImmutableMap.of(hcs, 48));

		//EMERALD
		createParrotItems(ParrotVariant.PULSATING_EMERALD, ParrotGUIPage.R2, 13,
			ImmutableMap.of(pulsatingEmerald, 64));

		//PIRATE
		createParrotItems(ParrotVariant.PIRATE, ParrotGUIPage.R2, 14,
			ImmutableMap.of(hcs, 80));

		//Snowy
		createParrotItems(ParrotVariant.SNOWY, ParrotGUIPage.R2, 15,
			ImmutableMap.of(hcs, 80));

		//Eldrask
		createParrotItems(ParrotVariant.ELDRASK, ParrotGUIPage.R2, 16,
			List.of("Requires 50 Eldrask wins to buy", "You still need " + (50 - ScoreboardUtils.getScoreboardValue(mPlayer, "FGWins").orElse(0)) + " wins"),
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, "FGWins").orElse(0) >= 50,
			ImmutableMap.of(titanicKnowledge, 80));

		//Hekawt - Lich
		createParrotItems(ParrotVariant.HEKAWT, ParrotGUIPage.R2, 19,
			List.of("Requires 50 Hekawt wins to buy", "You still need " + (50 - ScoreboardUtils.getScoreboardValue(mPlayer, "LichWins").orElse(0)) + " wins"),
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, "LichWins").orElse(0) >= 50,
			ImmutableMap.of(ancestralEffigy, 80));

		//Depths
		int depthsScore = ScoreboardUtils.getScoreboardValue(mPlayer, "DepthsEndless").orElse(0);
		createParrotItems(ParrotVariant.DEPTHS, ParrotGUIPage.R2, 20,
			List.of("Requires clearing floor 9 from Darkest Depths", "You have cleared floor " + ((depthsScore - 1) / 10)),
			(player, inv) -> ScoreboardUtils.getScoreboardValue(mPlayer, "DepthsEndless").orElse(0) >= 91,
			ImmutableMap.of(voidstainedGeode, 64),
			// unlock requirements hidden (and not purchasable) until having beaten Depths at least once
			List.of("Defeat Darkest Depths to learn more about this parrot"),
			(player, inv) -> ScoreboardUtils.getScoreboardValue(mPlayer, "Depths").orElse(0) > 0);

		//Depths Upgrade
		createParrotItems(ParrotVariant.DEPTHS_UPGRADE, ParrotGUIPage.R2, 21,
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
		ItemStack buyPatreon = buildItem(Material.ORANGE_WOOL, "Patreon Parakeet", List.of("Become a Tier 1 patreon to unlock"), false);
		GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.SPECIAL.mNum, 10, buyPatreon,
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.PATREON_DOLLARS).orElse(0) < Constants.PATREON_TIER_1 && mSelectedAction == ParrotAction.NONE));

		ItemStack boughtPatreon = buildItem(Material.ORANGE_WOOL, "Patreon Parakeet", List.of("Owned"), false);
		GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.SPECIAL.mNum, 10, boughtPatreon,
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.PATREON_DOLLARS).orElse(0) >= Constants.PATREON_TIER_1 && mSelectedAction == ParrotAction.NONE));
		GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.SPECIAL.mNum, 10, boughtPatreon,
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.PATREON_DOLLARS).orElse(0) >= Constants.PATREON_TIER_1 && mSelectedAction != ParrotAction.NONE,
			(player, inv) -> {
				selectParrot(ParrotVariant.PATREON);
				return true;
			}));


		//PRIDE PARROT!
		List<Component> rainbowCost = new ArrayList<>();
		rainbowCost.add(Component.text("You have to unlock these parrots"));
		rainbowCost.add(Component.text("before being able to purchase this one:"));

		for (ParrotVariant variant : EnumSet.of(ParrotVariant.RED, ParrotVariant.BLUE, ParrotVariant.CYAN, ParrotVariant.GREEN, ParrotVariant.GRAY)) {
			rainbowCost.add(Component.text(variant.getName()).decoration(TextDecoration.STRIKETHROUGH, variant.hasUnlocked(mPlayer)));
		}

		createParrotItemsFinal(ParrotVariant.RAINBOW, ParrotGUIPage.SPECIAL, 11,
			rainbowCost,
			(player, inv) -> ScoreboardUtils.getScoreboardValue(player, "ParrotBought1").orElse(0) > 0 &&
				                 ScoreboardUtils.getScoreboardValue(player, "ParrotBought2").orElse(0) > 0 &&
				                 ScoreboardUtils.getScoreboardValue(player, "ParrotBought3").orElse(0) > 0 &&
				                 ScoreboardUtils.getScoreboardValue(player, "ParrotBought4").orElse(0) > 0 &&
				                 ScoreboardUtils.getScoreboardValue(player, "ParrotBought5").orElse(0) > 0,
			ImmutableMap.of(
				hcs, 32,
				hxp, 32,
				unicornPuke, 1),
			null, null);

		List<String> twistedMobsKilled = List.of("AlricLordofFrostedWinds", "YeigarLastEmperor", "SalazarArchitectofViridia", "XenoShatteredScalllawag", "IsadoratheBloodiedQueen", "CTelsketCrimsonConqueror", "AesirLightbringer", "CZaniltheSoulcrusher");
		boolean twistedUnlocked = true;

		for (String twistedMobName : twistedMobsKilled) {
			SoulEntry entry = SoulsDatabase.getInstance().getSoul(twistedMobName);
			if (entry != null) {
				int kills = BestiaryManager.getKillsForMob(mPlayer, entry);
				if (kills <= 0) {
					twistedUnlocked = false;
					break;
				}
			}
		}

		if (twistedUnlocked) {
			createParrotItems(ParrotVariant.TWISTED, ParrotGUIPage.SPECIAL, 12, ImmutableMap.of());
		} else {
			ItemStack stack = buildItem(Material.BEDROCK, "Twisted?????", List.of(), true);
			ItemUtils.setPlainName(stack, ParrotVariant.TWISTED.getName());
			GUI_ITEMS.add(new ParrotGuiItem(this, ParrotGUIPage.SPECIAL.mNum, 12, stack, null, (player, inv) -> mSelectedAction == ParrotAction.NONE, null));
		}

		//==================================================================================================
		//                                         Special parrots end
		//==================================================================================================


	}

	private void createParrotItems(ParrotVariant variant, ParrotGUIPage page, int slot,
	                               ImmutableMap<ItemStack, Integer> cost) {
		createParrotItems(variant, page, slot, null, null, cost, null, null);
	}

	private void createParrotItems(ParrotVariant variant, ParrotGUIPage page, int slot,
	                               List<String> requirementsLore, BiPredicate<Player, Inventory> requirements, ImmutableMap<ItemStack, Integer> cost) {
		createParrotItems(variant, page, slot, requirementsLore, requirements, cost, null, null);
	}

	private void createParrotItems(ParrotVariant variant, ParrotGUIPage page, int slot,
	                               @Nullable List<String> requirementsLore, @Nullable BiPredicate<Player, Inventory> requirements, ImmutableMap<ItemStack, Integer> cost,
	                               @Nullable List<String> hiddenUnlockLore, @Nullable BiPredicate<Player, Inventory> showUnlockRequirements) {
		createParrotItemsFinal(variant, page, slot, requirementsLore == null ? null : requirementsLore.stream().map(Component::text).toList(), requirements, cost, hiddenUnlockLore, showUnlockRequirements);
	}

	private void createParrotItemsFinal(ParrotVariant variant, ParrotGUIPage page, int slot,
	                                    @Nullable List<? extends Component> requirementsLore, @Nullable BiPredicate<Player, Inventory> requirements, ImmutableMap<ItemStack, Integer> cost,
	                                    @Nullable List<String> hiddenUnlockLore, @Nullable BiPredicate<Player, Inventory> showUnlockRequirements) {
		String scoreboard = variant.getScoreboard();

		// "hidden" parrot (cannot be bought and unlock condition is hidden)
		if (hiddenUnlockLore != null && showUnlockRequirements != null) {
			ItemStack hiddenDisplayItem = buildItem(variant.getDisplayitem(), variant.getName(), hiddenUnlockLore, variant == ParrotVariant.DEPTHS_UPGRADE);
			GUI_ITEMS.add(new ParrotGuiItem(this, page.mNum, slot, hiddenDisplayItem,
				(player, inv) -> mSelectedAction == ParrotAction.NONE
					                 && ScoreboardUtils.getScoreboardValue(player, scoreboard).orElse(0) == 0
					                 && !showUnlockRequirements.test(player, inv)));
		}

		// locked parrot (cannot be bought yet)
		if (requirementsLore != null && requirements != null) {
			ItemStack lockedDisplayItem = buildItemComponents(variant.getDisplayitem(), variant.getName(), requirementsLore, variant == ParrotVariant.DEPTHS_UPGRADE);
			GUI_ITEMS.add(new ParrotGuiItem(this, page.mNum, slot, lockedDisplayItem,
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
		ItemStack buyDisplayItem = buildItem(variant.getDisplayitem(), "Buy " + variant.getName(), costLore, variant == ParrotVariant.DEPTHS_UPGRADE);
		GUI_ITEMS.add(new ParrotGuiItem(this, page.mNum, slot, buyDisplayItem, cost,
			(player, inv) -> mSelectedAction == ParrotAction.NONE
				                 && ScoreboardUtils.getScoreboardValue(player, scoreboard).orElse(0) == 0
				                 && (requirements == null || requirements.test(player, inv))
				                 && (showUnlockRequirements == null || showUnlockRequirements.test(player, inv)),
			(player, inv) -> {
				ScoreboardUtils.setScoreboardValue(player, scoreboard, (int) Instant.now().getEpochSecond());
				return true;
			}));

		// unclickable bought parrot
		ItemStack boughtDisplayItem = buildItem(variant.getDisplayitem(), variant.getName(),
			List.of("Owned", new Date((long) ScoreboardUtils.getScoreboardValue(mPlayer, scoreboard).orElse(0) * 1000).toString()), variant == ParrotVariant.DEPTHS_UPGRADE);
		GUI_ITEMS.add(new ParrotGuiItem(this, page.mNum, slot, boughtDisplayItem,
			(player, inv) -> mSelectedAction == ParrotAction.NONE && ScoreboardUtils.getScoreboardValue(player, scoreboard).orElse(0) > 0));

		// clickable bought parrot
		GUI_ITEMS.add(new ParrotGuiItem(this, page.mNum, slot, boughtDisplayItem,
			(player, inv) -> mSelectedAction != ParrotAction.NONE && ScoreboardUtils.getScoreboardValue(player, scoreboard).orElse(0) > 0,
			(player, inv) -> {
				selectParrot(variant);
				return true;
			}));

	}

	private void selectParrot(ParrotVariant variant) {
		switch (mSelectedAction) {
			case SET_LEFT_SHOULDER -> ParrotManager.selectParrot(mPlayer, variant, PlayerShoulder.LEFT);
			case SET_RIGHT_SHOULDER -> ParrotManager.selectParrot(mPlayer, variant, PlayerShoulder.RIGHT);
			case PLACE -> ParrotManager.spawnParrot(mPlayer, variant);
			default -> {
			}
		}
	}

	public ItemStack buildItem(Material mat, String name, List<String> lore, boolean nameObfuscated) {
		return buildItemComponents(mat, name, lore.stream().map(Component::text).toList(), nameObfuscated);
	}

	public ItemStack buildItemComponents(Material mat, String name, List<? extends Component> lore, boolean nameObfuscated) {
		Component cName = Component.text(name, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.OBFUSCATED, nameObfuscated);
		List<Component> finalLore = lore.stream().map(line -> line.colorIfAbsent(NamedTextColor.AQUA)).toList();
		return GUIUtils.createBasicItem(mat, 1, cName, finalLore, true);
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
		loadItems();
		owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.NEUTRAL, 3f, 1.2f);
	}

	@Override
	public void setup() {
		if (SIGN_MAP.get(mCurrentPage) != null) {
			setItem(4, SIGN_MAP.get(mCurrentPage));
		}

		for (ParrotGuiItem gItem : GUI_ITEMS) {
			ParrotGUIPage itemPage = ParrotGUIPage.valueOfPage(gItem.getPage());
			if (itemPage != null && (itemPage == mCurrentPage || itemPage == ParrotGUIPage.OTHERS) && gItem.isVisible(mPlayer, getInventory())) {
				setItem(gItem.getSlot(), gItem);
			}
		}

		fillWithJunk();
	}

	public void fillWithJunk() {
		if (BORDER_TOPLEFT_MAP.get(mCurrentPage) != null) {
			setItem(0, BORDER_TOPLEFT_MAP.get(mCurrentPage));
		}

		for (int i = 0; i < (ROWS * COLUMNS); i++) {
			if (getItem(i) == null) {
				if (i < COLUMNS || i > ((ROWS - 1) * COLUMNS) || (i % COLUMNS == 0) || (i % COLUMNS == COLUMNS - 1)) {
					setItem(i, JUNK_BORDER_ITEM);
				} else {
					setItem(i, JUNK_INTERIOR_ITEM);
				}
			}
		}
	}

	public void refreshItems() {
		try {
			GUI_ITEMS.clear();
			loadItems();
		} catch (Exception e) {
			MMLog.warning("Caught exception refreshing Parrot GUI");
			e.printStackTrace();
		}
		update();
	}

}
