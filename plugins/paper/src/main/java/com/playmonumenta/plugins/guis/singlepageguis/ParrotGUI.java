package com.playmonumenta.plugins.guis.singlepageguis;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.guis.SinglePageGUI;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.parrots.ParrotManager;
import com.playmonumenta.plugins.parrots.ParrotManager.ParrotVariant;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class ParrotGUI extends SinglePageGUI {

	private static final int ROWS = 3;
	private static final int COLUMNS = 9;




	private static final Material JUNK_ITEM = Material.BLACK_STAINED_GLASS_PANE;


	private static final String SCOREBOARD_BOUGHT_SHOULDERS = "ParrotBoth";
	//0 if the player can't have 2 parrot at the same time, otherwise the time of when he bought it

	private static final String SCOREBOARD_GUI_PAGE = "ParrotGUIPage";
	//0 default page, 1 right shoulder page, 2 left shoulder page


	private static ArrayList<GuiItem> GUI_ITEMS = new ArrayList<>();
	private static Map<Integer, GuiItem> mInvMapping = new HashMap<>();

	public void loadItem() {

		//getting the currencies from loottable
		ItemStack mHCS = null;
		ItemStack mHXP = null;
		ItemStack mPPe = null;
		ItemStack mPGo = null;
		ItemStack mKS = null;
		ItemStack mFG = null;
		ItemStack mUP = null;

		mHCS = InventoryUtils.getItemFromLootTable(mPlayer, NamespacedKey.fromString("epic:r2/items/currency/hyper_crystalline_shard"));
		mHXP = InventoryUtils.getItemFromLootTable(mPlayer, NamespacedKey.fromString("epic:r1/items/currency/hyper_experience"));
		mPGo = InventoryUtils.getItemFromLootTable(mPlayer, NamespacedKey.fromString("epic:r1/items/currency/pulsating_gold"));
		mPPe = InventoryUtils.getItemFromLootTable(mPlayer, NamespacedKey.fromString("epic:r2/items/currency/pulsating_emerald"));
		mKS = InventoryUtils.getItemFromLootTable(mPlayer, NamespacedKey.fromString("epic:r1/kaul/crownshard"));
		mFG = InventoryUtils.getItemFromLootTable(mPlayer, NamespacedKey.fromString("epic:r2/eldrask/materials/epic_material"));
		mUP = InventoryUtils.getItemFromLootTable(mPlayer, NamespacedKey.fromString("epic:r1/dungeons/4/static_uncommons/unicorn_puke"));
		//we got the currencies, now populating the list



		//setting lore R2
		List<String> lore = new ArrayList<>();
		lore.add("Click to buy!");
		lore.add("48HCS");

		ItemStack buyRed = buildItem(Material.RED_WOOL, "Buy Scarlet Macaw", lore);
		Map<ItemStack, Integer> cost = new HashMap<>();
		cost.put(mHCS, 48);
		GUI_ITEMS.add(new GuiItem(0, 0, buyRed, new HashMap<>(cost), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought1") == 0; }, (player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, "ParrotBought1", (int) Instant.now().getEpochSecond());
								return true; }));

		ItemStack buyBlue = buildItem(Material.BLUE_WOOL, "Buy Hyacinth Macaw", lore);
		GUI_ITEMS.add(new GuiItem(0, 1, buyBlue, new HashMap<>(cost), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought2") == 0; },
								(player, inv) -> {
									ScoreboardUtils.setScoreboardValue(player, "ParrotBought2", (int) Instant.now().getEpochSecond());
									return true; }));

		ItemStack buyYellow = buildItem(Material.LIGHT_BLUE_WOOL, "Buy Blue-Yellow Macaw", lore);
		GUI_ITEMS.add(new GuiItem(0, 2, buyYellow, new HashMap<>(cost), (player, inv) -> {
							return ScoreboardUtils.getScoreboardValue(player, "ParrotBought3") == 0; },
								(player, inv) -> {
									ScoreboardUtils.setScoreboardValue(player, "ParrotBought3", (int) Instant.now().getEpochSecond());
									return true; }));

		//setting lore R1 & cost
		lore.remove("48HCS");
		lore.add("48HXP");
		cost.clear();
		cost.put(mHXP, 48);


		ItemStack buyGreen = buildItem(Material.GREEN_WOOL, "Buy Green Parakeet", lore);
		GUI_ITEMS.add(new GuiItem(0, 3, buyGreen, new HashMap<>(cost), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought4") == 0; },
							(player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, "ParrotBought4", (int) Instant.now().getEpochSecond());
								return true; }));

		ItemStack buyGray = buildItem(Material.LIGHT_GRAY_WOOL, "Buy Gray Cockatiel", lore);
		GUI_ITEMS.add(new GuiItem(0, 4, buyGray, new HashMap<>(cost), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought5") == 0; },
							(player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, "ParrotBought5", (int) Instant.now().getEpochSecond());
								return true; }));

		//setting lore for patreon
		lore.clear();
		lore.add("Become a Tier 2 patreon to unlock");
		ItemStack buyPatreon = buildItem(Material.ORANGE_WOOL, "Patron Parakeet", lore);
		GUI_ITEMS.add(new GuiItem(0, 6, buyPatreon, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "Patreon") < 5; }));

		//setting lore & cost
		lore.clear();
		lore.add("Click to buy!");
		lore.add("64 Pulsating Gold");
		cost.clear();
		cost.put(mPGo, 64);
		ItemStack buyGolden = buildItem(Material.YELLOW_CONCRETE, "Buy Golden Conure", lore);
		GUI_ITEMS.add(new GuiItem(0, 7, buyGolden, new HashMap<>(cost), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought7") == 0; },
							(player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, "ParrotBought7", (int) Instant.now().getEpochSecond());
								return true; }));

		lore.remove("64 Pulsating Gold");
		lore.add("64 Pulsating Emerald");
		cost.clear();
		cost.put(mPPe, 64);
		ItemStack buyEmerald = buildItem(Material.GREEN_CONCRETE, "Buy Emerald Conure", lore);
		GUI_ITEMS.add(new GuiItem(0, 8, buyEmerald, new HashMap<>(cost), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought8") == 0; },
							(player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, "ParrotBought8", (int) Instant.now().getEpochSecond());
								return true; }));

		lore.remove("64 Pulsating Emerald");
		lore.add("80HCS");
		cost.clear();
		cost.put(mHCS, 80);
		ItemStack buyPirate = buildItem(Material.PURPLE_WOOL, "Buy Scoundrel Macaw", lore);
		GUI_ITEMS.add(new GuiItem(0, 9, buyPirate, new HashMap<>(cost), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought9") == 0; },
						(player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, "ParrotBought9", (int) Instant.now().getEpochSecond());
								return true; }));

		//start loading funtional items
		ItemStack shoulderLeft = buildItem(Material.COOKIE, "Shoulder Left", new ArrayList<>());
		GUI_ITEMS.add(new GuiItem(0, 18, shoulderLeft, (player, inv) -> {
						return true; },
							(player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_GUI_PAGE, 1);
								return true; }));

		ItemStack shoulderRight = buildItem(Material.COOKIE, "Shoulder Right", new ArrayList<>());
		GUI_ITEMS.add(new GuiItem(0, 19, shoulderRight, (player, inv) -> {
						return true; },
							(player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_GUI_PAGE, 2);
								return true; }));

		ItemStack removeParrots = buildItem(Material.FEATHER, "Remove Parrots", new ArrayList<>());
		GUI_ITEMS.add(new GuiItem(0, 21, removeParrots, (player, inv) -> {
						return ParrotManager.hasParrotOnShoulders(player); },
							(player, inv) -> {
								ParrotManager.clearParrots(player);
								return true; }));

		ItemStack visibleParrots = buildItem(Material.SADDLE, "Set parrots visible", new ArrayList<>());
		GUI_ITEMS.add(new GuiItem(0, 22, visibleParrots, (player, inv) -> {
						return ParrotManager.hasParrotOnShoulders(player) && !ParrotManager.isParrotsVisible(player); },
							(player, inv) -> {
								ParrotManager.setParrotVisible(player, true);
								return true; }));

		ItemStack invisibleParrots = buildItem(Material.SADDLE, "Set parrots invisible", new ArrayList<>());
		GUI_ITEMS.add(new GuiItem(0, 22, invisibleParrots, (player, inv) -> {
						return ParrotManager.hasParrotOnShoulders(player) && ParrotManager.isParrotsVisible(player); },
							(player, inv) -> {
								ParrotManager.setParrotVisible(player, false);
								return true; }));

		lore.clear();
		lore.add("Click to buy!");
		lore.add("64HCS");
		cost.clear();
		cost.put(mHCS, 64);
		ItemStack bothShoulders = buildItem(Material.INK_SAC, "Buy Both Shoulders", lore);
		GUI_ITEMS.add(new GuiItem(0, 26, bothShoulders, new HashMap<>(cost), (player, inv) -> {
						return !ParrotManager.hasDoubleShoulders(player); },
							(player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_BOUGHT_SHOULDERS, (int) Instant.now().getEpochSecond());
								return true; }));

		lore.clear();
		lore.add("Owned");
		lore.add((new Date((long)ScoreboardUtils.getScoreboardValue(mPlayer, SCOREBOARD_BOUGHT_SHOULDERS)*1000).toString()));
		ItemStack boughtShoulders = buildItem(Material.INK_SAC, "Both Shoulders", lore);
		GUI_ITEMS.add(new GuiItem(0, 26, boughtShoulders, new HashMap<>(), (player, inv) -> {
						return ParrotManager.hasDoubleShoulders(player); }));


		lore.clear();
		ItemStack back = buildItem(Material.ARROW, "Back", lore);
		GUI_ITEMS.add(new GuiItem(1, 26, back, new HashMap<>(), (player, inv) -> {
						return true; },
							(player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_GUI_PAGE, 0);
								return true; }));
		GUI_ITEMS.add(new GuiItem(2, 26, back, new HashMap<>(), (player, inv) -> {
						return true; },
							(player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_GUI_PAGE, 0);
								return true; }));

		ItemStack leftShoulder = buildItem(Material.BRICK, "Left Shoulder Selected", lore);
		GUI_ITEMS.add(new GuiItem(1, 18, leftShoulder, new HashMap<>(), (player, inv) -> {
						return true; },
							(player, inv) -> {
								return true; }));

		ItemStack rightShoulder = buildItem(Material.BRICK, "Right Shoulder Selected", lore);
		GUI_ITEMS.add(new GuiItem(2, 19, rightShoulder, new HashMap<>(), (player, inv) -> {
						return true; },
							(player, inv) -> {
								return true; }));

		//parrots owned

		//RED PARROT
		lore.clear();
		lore.add("Owned");
		lore.add((new Date((long)ScoreboardUtils.getScoreboardValue(mPlayer, "ParrotBought1")*1000).toString()));
		ItemStack boughtRed = buildItem(Material.RED_WOOL, "Scarlet Macaw", lore);
		GUI_ITEMS.add(new GuiItem(0, 0, boughtRed, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought1") > 0; }));
		GUI_ITEMS.add(new GuiItem(1, 0, boughtRed, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought1") > 0; },
							(player, inv) -> {
								ParrotManager.updateParrot(player, ParrotVariant.RED, "LEFT");
								return true; }));
		GUI_ITEMS.add(new GuiItem(2, 0, boughtRed, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought1") > 0; },
							(player, inv) -> {
								ParrotManager.updateParrot(player, ParrotVariant.RED, "RIGHT");
								return true; }));

		//BLUE PARROT
		lore.clear();
		lore.add("Owned");
		lore.add((new Date((long)ScoreboardUtils.getScoreboardValue(mPlayer, "ParrotBought2")*1000).toString()));
		ItemStack boughtBlue = buildItem(Material.BLUE_WOOL, "Hyacinth Macaw", lore);
		GUI_ITEMS.add(new GuiItem(0, 1, boughtBlue, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought2") > 0; }));
		GUI_ITEMS.add(new GuiItem(1, 1, boughtBlue, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought2") > 0; },
							(player, inv) -> {
								ParrotManager.updateParrot(player, ParrotVariant.BLUE, "LEFT");
								return true; }));
		GUI_ITEMS.add(new GuiItem(2, 1, boughtBlue, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought2") > 0; },
							(player, inv) -> {
								ParrotManager.updateParrot(player, ParrotVariant.BLUE, "RIGHT");
								return true; }));

		//BLUE-YELLOW
		lore.clear();
		lore.add("Owned");
		lore.add((new Date((long)ScoreboardUtils.getScoreboardValue(mPlayer, "ParrotBought3")*1000).toString()));
		ItemStack boughtYellow = buildItem(Material.LIGHT_BLUE_WOOL, "Blue-Yellow Macaw", lore);
		GUI_ITEMS.add(new GuiItem(0, 2, boughtYellow, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought3") > 0; }));
		GUI_ITEMS.add(new GuiItem(1, 2, boughtYellow, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought3") > 0; },
							(player, inv) -> {
								ParrotManager.updateParrot(player, ParrotVariant.CYAN, "LEFT");
								return true; }));
		GUI_ITEMS.add(new GuiItem(2, 2, boughtYellow, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought3") > 0; },
							(player, inv) -> {
								ParrotManager.updateParrot(player, ParrotVariant.CYAN, "RIGHT");
								return true; }));

		//GREEN
		lore.clear();
		lore.add("Owned");
		lore.add((new Date((long)ScoreboardUtils.getScoreboardValue(mPlayer, "ParrotBought4")*1000).toString()));
		ItemStack boughGreen = buildItem(Material.GREEN_WOOL, "Green Parakeet", lore);
		GUI_ITEMS.add(new GuiItem(0, 3, boughGreen, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought4") > 0; }));
		GUI_ITEMS.add(new GuiItem(1, 3, boughGreen, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought4") > 0; },
							(player, inv) -> {
								ParrotManager.updateParrot(player, ParrotVariant.GREEN, "LEFT");
								return true; }));
		GUI_ITEMS.add(new GuiItem(2, 3, boughGreen, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought4") > 0; },
							(player, inv) -> {
								ParrotManager.updateParrot(player, ParrotVariant.GREEN, "RIGHT");
								return true; }));

		//GRAY
		lore.clear();
		lore.add("Owned");
		lore.add((new Date((long)ScoreboardUtils.getScoreboardValue(mPlayer, "ParrotBought5")*1000).toString()));
		ItemStack boughGray = buildItem(Material.LIGHT_GRAY_WOOL, "Gray Cockatiel", lore);
		GUI_ITEMS.add(new GuiItem(0, 4, boughGray, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought5") > 0; }));
		GUI_ITEMS.add(new GuiItem(1, 4, boughGray, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought5") > 0; },
							(player, inv) -> {
								ParrotManager.updateParrot(player, ParrotVariant.GRAY, "LEFT");
								return true; }));
		GUI_ITEMS.add(new GuiItem(2, 4, boughGray, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought5") > 0; },
							(player, inv) -> {
								ParrotManager.updateParrot(player, ParrotVariant.GRAY, "RIGHT");
								return true; }));

		//PATREON
		lore.clear();
		lore.add("Owned");
		//lore.add((new Date((long)ScoreboardUtils.getScoreboardValue(mPlayer, "--------")*1000).toString()));   //the patreon parrot item don't have a date
		ItemStack boughPatreon = buildItem(Material.ORANGE_WOOL, "Patreon Parakeet", lore);
		GUI_ITEMS.add(new GuiItem(0, 6, boughPatreon, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "Patreon") >= 5; }));
		GUI_ITEMS.add(new GuiItem(1, 6, boughPatreon, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "Patreon") >= 5; },
							(player, inv) -> {
								ParrotManager.updateParrot(player, ParrotVariant.PATREON, "LEFT");
								return true; }));
		GUI_ITEMS.add(new GuiItem(2, 6, boughPatreon, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "Patreon") >= 5; },
							(player, inv) -> {
								ParrotManager.updateParrot(player, ParrotVariant.PATREON, "RIGHT");
								return true; }));

		//Golden
		lore.clear();
		lore.add("Owned");
		lore.add((new Date((long)ScoreboardUtils.getScoreboardValue(mPlayer, "ParrotBought7")*1000).toString()));
		ItemStack boughGolden = buildItem(Material.YELLOW_CONCRETE, "Golden Conure", lore);
		GUI_ITEMS.add(new GuiItem(0, 7, boughGolden, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought7") > 0; }));
		GUI_ITEMS.add(new GuiItem(1, 7, boughGolden, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought7") > 0; },
							(player, inv) -> {
								ParrotManager.updateParrot(player, ParrotVariant.PULSATING_GOLD, "LEFT");
								return true; }));
		GUI_ITEMS.add(new GuiItem(2, 7, boughGolden, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought7") > 0; },
							(player, inv) -> {
								ParrotManager.updateParrot(player, ParrotVariant.PULSATING_GOLD, "RIGHT");
								return true; }));

		//EMERALD
		lore.clear();
		lore.add("Owned");
		lore.add((new Date((long)ScoreboardUtils.getScoreboardValue(mPlayer, "ParrotBought8")*1000).toString()));
		ItemStack boughEmerald = buildItem(Material.GREEN_CONCRETE, "Emerald Conure", lore);
		GUI_ITEMS.add(new GuiItem(0, 8, boughEmerald, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought8") > 0; }));
		GUI_ITEMS.add(new GuiItem(1, 8, boughEmerald, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought8") > 0; },
							(player, inv) -> {
								ParrotManager.updateParrot(player, ParrotVariant.PULSATING_EMERALD, "LEFT");
								return true; }));
		GUI_ITEMS.add(new GuiItem(2, 8, boughEmerald, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought8") > 0; },
							(player, inv) -> {
								ParrotManager.updateParrot(player, ParrotVariant.PULSATING_EMERALD, "RIGHT");
								return true; }));

		//Pirate
		lore.clear();
		lore.add("Owned");
		lore.add((new Date((long)ScoreboardUtils.getScoreboardValue(mPlayer, "ParrotBought9")*1000).toString()));
		ItemStack boughPirate = buildItem(Material.PURPLE_WOOL, "Scoundrel Macaw", lore);
		GUI_ITEMS.add(new GuiItem(0, 9, boughPirate, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought9") > 0; }));
		GUI_ITEMS.add(new GuiItem(1, 9, boughPirate, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought9") > 0; },
							(player, inv) -> {
								ParrotManager.updateParrot(player, ParrotVariant.PIRATE, "LEFT");
								return true; }));
		GUI_ITEMS.add(new GuiItem(2, 9, boughPirate, new HashMap<>(), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought9") > 0; },
							(player, inv) -> {
								ParrotManager.updateParrot(player, ParrotVariant.PIRATE, "RIGHT");
								return true; }));

		//Kaul!
		lore.clear();
		lore.add("Requires 50 Kaul wins to buy");
		int currentWins = ScoreboardUtils.getScoreboardValue(mPlayer, "KaulWins");
		lore.add("You still need " + (50 - currentWins) + " wins");
		ItemStack winKaul = buildItem(Material.JUNGLE_LEAVES, "Blackroot Kakapo", lore);
		GUI_ITEMS.add(new GuiItem(0, 10, winKaul, new HashMap<>(), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "KaulWins") < 50;
		}));

		lore.clear();
		lore.add("Click to buy!");
		lore.add("80 Shard of the Mantle");
		cost.clear();
		cost.put(mKS, 80);
		ItemStack buyKaul = buildItem(Material.JUNGLE_LEAVES, "Blackroot Kakapo", lore);
		GUI_ITEMS.add(new GuiItem(0, 10, buyKaul, new HashMap<>(cost), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "KaulWins") >= 50;
		}, (player, inv) -> {
			ScoreboardUtils.setScoreboardValue(player, "ParrotBought10", (int) Instant.now().getEpochSecond());
			return true;
		}));

		lore.clear();
		lore.add("Owned");
		lore.add((new Date((long)ScoreboardUtils.getScoreboardValue(mPlayer, "ParrotBought10")*1000).toString()));
		ItemStack boughtKaul = buildItem(Material.JUNGLE_LEAVES, "Blackroot Kakapo", lore);
		GUI_ITEMS.add(new GuiItem(0, 10, boughtKaul, new HashMap<>(), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought10") > 0;
		}));
		GUI_ITEMS.add(new GuiItem(1, 10, boughtKaul, new HashMap<>(), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought10") > 0;
		}, (player, inv) -> {
			ParrotManager.updateParrot(player, ParrotVariant.KAUL, "LEFT");
			return true;
		}));
		GUI_ITEMS.add(new GuiItem(2, 10, boughtKaul, new HashMap<>(), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought10") > 0;
		}, (player, inv) -> {
			ParrotManager.updateParrot(player, ParrotVariant.KAUL, "RIGHT");
			return true;
		}));

		//Eldrask!
		lore.clear();
		lore.add("Requires 50 Eldrask wins to buy");
		currentWins = ScoreboardUtils.getScoreboardValue(mPlayer, "FGWins");
		lore.add("You still need " + (50 - currentWins) + " wins");
		ItemStack winEldrask = buildItem(Material.BLUE_ICE, "Permafrost Kea", lore);
		GUI_ITEMS.add(new GuiItem(0, 11, winEldrask, new HashMap<>(), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "EldraskWins") < 50;
		}));

		lore.clear();
		lore.add("Click to buy!");
		lore.add("80 Titanic Knowledge");
		cost.clear();
		cost.put(mFG, 80);
		ItemStack buyEldrask = buildItem(Material.BLUE_ICE, "Permafrost Kea", lore);
		GUI_ITEMS.add(new GuiItem(0, 11, buyEldrask, new HashMap<>(cost), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "FGWins") >= 50;
		}, (player, inv) -> {
			ScoreboardUtils.setScoreboardValue(player, "ParrotBought11", (int) Instant.now().getEpochSecond());
			return true;
		}));

		lore.clear();
		lore.add("Owned");
		lore.add((new Date((long)ScoreboardUtils.getScoreboardValue(mPlayer, "ParrotBought11")*1000).toString()));
		ItemStack boughtEldrask = buildItem(Material.BLUE_ICE, "Permafrost Kea", lore);
		GUI_ITEMS.add(new GuiItem(0, 11, boughtEldrask, new HashMap<>(), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought11") > 0;
		}));
		GUI_ITEMS.add(new GuiItem(1, 11, boughtEldrask, new HashMap<>(), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought11") > 0;
		}, (player, inv) -> {
			ParrotManager.updateParrot(player, ParrotVariant.ELDRASK, "LEFT");
			return true;
		}));
		GUI_ITEMS.add(new GuiItem(2, 11, boughtEldrask, new HashMap<>(), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought11") > 0;
		}, (player, inv) -> {
			ParrotManager.updateParrot(player, ParrotVariant.ELDRASK, "RIGHT");
			return true;
		}));


		//PRIDE PARROT!
		lore.clear();
		lore.add("You have to unlock these parrots");
		lore.add("before being able to purchase this one:");
		if (ScoreboardUtils.getScoreboardValue(mPlayer, "ParrotBought1") == 0) {
			lore.add("Scarlet Macaw");
		} else {
			lore.add("##Scarlet Macaw");
		}
		if (ScoreboardUtils.getScoreboardValue(mPlayer, "ParrotBought2") == 0) {
			lore.add("Hyacinth Macaw");
		} else {
			lore.add("##Hyacinth Macaw");
		}
		if (ScoreboardUtils.getScoreboardValue(mPlayer, "ParrotBought3") == 0) {
			lore.add("Blue-Yellow Macaw");
		} else {
			lore.add("##Blue-Yellow Macaw");
		}
		if (ScoreboardUtils.getScoreboardValue(mPlayer, "ParrotBought4") == 0) {
			lore.add("Green Parakeet");
		} else {
			lore.add("##Green Parakeet");
		}
		if (ScoreboardUtils.getScoreboardValue(mPlayer, "ParrotBought5") == 0) {
			lore.add("Gray Cockatiel");
		} else {
			lore.add("##Gray Cockatiel");
		}
		ItemStack prideParrot = buildItem(Material.YELLOW_GLAZED_TERRACOTTA, "Rainbow Parrot", lore);
		GUI_ITEMS.add(new GuiItem(0, 5, prideParrot, new HashMap<>(), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought1") == 0 ||
			ScoreboardUtils.getScoreboardValue(player, "ParrotBought2") == 0 ||
			ScoreboardUtils.getScoreboardValue(player, "ParrotBought3") == 0 ||
			ScoreboardUtils.getScoreboardValue(player, "ParrotBought4") == 0 ||
			ScoreboardUtils.getScoreboardValue(player, "ParrotBought5") == 0;
		}, (player, inv) -> {
			return true;
		}));

		lore.clear();
		lore.add("Click to buy!");
		lore.add("32 HXP");
		lore.add("32 HCS");
		lore.add("1 Unicorn Puke");
		cost.clear();
		cost.put(mHCS, 32);
		cost.put(mHXP, 32);
		cost.put(mUP, 1);
		ItemStack buyPrideParrot = buildItem(Material.YELLOW_GLAZED_TERRACOTTA, "Rainbow Parrot", lore);
		GUI_ITEMS.add(new GuiItem(0, 5, buyPrideParrot, new HashMap<>(cost), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought1") > 0 &&
			ScoreboardUtils.getScoreboardValue(player, "ParrotBought2") > 0 &&
			ScoreboardUtils.getScoreboardValue(player, "ParrotBought3") > 0 &&
			ScoreboardUtils.getScoreboardValue(player, "ParrotBought4") > 0 &&
			ScoreboardUtils.getScoreboardValue(player, "ParrotBought5") > 0;
		}, (player, inv) -> {
			ScoreboardUtils.setScoreboardValue(player, "ParrotBought12", (int) Instant.now().getEpochSecond());
			return true;
		}));

		lore.clear();
		lore.add("Owned");
		lore.add((new Date((long)ScoreboardUtils.getScoreboardValue(mPlayer, "ParrotBought12")*1000).toString()));
		ItemStack boughtPrideParrot = buildItem(Material.YELLOW_GLAZED_TERRACOTTA, "Rainbow Parrot", lore);
		GUI_ITEMS.add(new GuiItem(0, 5, boughtPrideParrot, new HashMap<>(), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought12") > 0;
		}, (player, inv) -> {
			return true;
		}));

		GUI_ITEMS.add(new GuiItem(1, 5, boughtPrideParrot, new HashMap<>(), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought12") > 0;
		}, (player, inv) -> {
			ParrotManager.updateParrot(player, ParrotVariant.RAINBOW, "LEFT");
			return true;
		}));

		GUI_ITEMS.add(new GuiItem(2, 5, boughtPrideParrot, new HashMap<>(), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought12") > 0;
		}, (player, inv) -> {
			ParrotManager.updateParrot(player, ParrotVariant.RAINBOW, "RIGHT");
			return true;
		}));

	}


	public ItemStack buildItem(Material mat, String name, List<String> lore) {
		ItemStack newItem = new ItemStack(mat, 1);
		ItemMeta meta = newItem.getItemMeta();

		meta.displayName(Component.text(name, NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false));

		if (!lore.isEmpty()) {
			List<Component> mLore = new ArrayList<Component>();
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
		return newItem;
	   }

/**------------Parrot GUI layout-------------
 *
 * ============================================
 * | Buy! | p | p | p | p | p | p | p |   p   |
 * |  p   | p | p | p | p | p | p | p |   p   |
 * |  SL  |SR |   | C | V |   |   |   | Double|
 * ============================================
 *
 * Where:
 *  p -> parrot
 *  SL -> Shoulder Left
 *  SR -> Shoulder Right
 *  Double -> make you buy the option to have 2 different parrot
 *  C -> clear currents parrot
 *  V -> SetParrotVisible true or false
 *
 */

	public ParrotGUI(Player player, String[] args) {
		super(player, args);
	}

	@Override
	public void registerCommand() {
		registerCommand("openparrotgui");
	}

	@Override
	public SinglePageGUI constructGUI(Player player, String[] args) {
		return new ParrotGUI(player, args);
	}

	@Override
	public Inventory getInventory(Player player, String[] args) {
		Inventory inv = Bukkit.createInventory(null, COLUMNS * ROWS, Component.text("Parrot variants"));
		loadItem();
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_GUI_PAGE, 0);
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 3f, 1.2f);

		return updateInventory(inv);
	}

	public int getCurrentPage() {
		return ScoreboardUtils.getScoreboardValue(mPlayer, "ParrotGUIPage");
	}

	public Inventory updateInventory(Inventory inv) {
		int page = getCurrentPage();
		inv.clear();
		mInvMapping.clear();

		for (GuiItem gItem : GUI_ITEMS) {
			if (page == gItem.getPage() && gItem.isVisible(mPlayer, inv)) {
				inv.setItem(gItem.getSlot(), gItem.getShowedItem());
				mInvMapping.put(gItem.getSlot(), gItem);
			}
		}

		fillWithJunk(inv, JUNK_ITEM);
		return inv;
	}

	public void fillWithJunk(Inventory inventory, Material junkItem) {
		for (int i = 0; i < (ROWS*COLUMNS); i++) {
			if (inventory.getItem(i) == null) {
				inventory.setItem(i, new ItemStack(junkItem, 1));
			}
		}
	}



	@Override
	public void processClick(InventoryClickEvent event) {
		event.setCancelled(true);

		if (event.isShiftClick()) {
			return;
		}

		if (!mInventory.equals(event.getClickedInventory())) {
			return;
		}

		if (event.getCurrentItem().getType().equals(JUNK_ITEM)) {
			//if the player press the junk item nothing happen    // Magikarp use SPLASH!
			return;
		}

		int slotClicked = event.getSlot();
		GuiItem gItem = mInvMapping.get(slotClicked);
		Player whoClicked = (Player) event.getWhoClicked();
		Inventory inventory = event.getClickedInventory();


		if (gItem.canPurcase(whoClicked)) {
			if (gItem.purcase(whoClicked)) {
				whoClicked.playSound(whoClicked.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundCategory.NEUTRAL, 10f, 1.3f);
				gItem.afterClick(whoClicked, inventory);
				updateInventory(inventory);
			} else {
				whoClicked.sendMessage(Component.text("[SYSTEM]", NamedTextColor.RED).decoration(TextDecoration.BOLD, true)
				.append(Component.text(" Error! please contact a mod! fail with purchasing.", NamedTextColor.RED).decoration(TextDecoration.BOLD, false)));
			}
		} else {
			whoClicked.sendMessage(Component.text("You don't have enough currency to pay for this item.", NamedTextColor.RED));
		}

	}


}
