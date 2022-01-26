package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.parrots.ParrotManager;
import com.playmonumenta.plugins.parrots.ParrotManager.ParrotVariant;
import com.playmonumenta.plugins.parrots.ParrotManager.PlayerShoulder;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import javax.annotation.Nullable;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


	private ArrayList<GuiItem> GUI_ITEMS = new ArrayList<>();
	private Map<Integer, GuiItem> mInvMapping = new HashMap<>();
	private PlayerShoulder mShoulderSelected = PlayerShoulder.NONE;
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
		ItemStack mHCS = loadItemTable(playerLoad, "epic:r2/items/currency/hyper_crystalline_shard");
		ItemStack mHXP = loadItemTable(playerLoad, "epic:r1/items/currency/hyper_experience");
		ItemStack mPGo = loadItemTable(playerLoad, "epic:r1/items/currency/pulsating_gold");
		ItemStack mPPe = loadItemTable(playerLoad, "epic:r2/items/currency/pulsating_emerald");
		ItemStack mKS = loadItemTable(playerLoad, "epic:r1/kaul/crownshard");
		ItemStack mFG = loadItemTable(playerLoad, "epic:r2/eldrask/materials/epic_material");
		ItemStack mDM = loadItemTable(playerLoad, "epic:r2/depths/loot/voidstained_geode");
		ItemStack mUP = loadItemTable(playerLoad, "epic:r1/dungeons/4/static_uncommons/unicorn_puke");
		ItemStack mLi = loadItemTable(playerLoad, "epic:r2/lich/materials/ancestral_effigy");

		List<String> lore = new ArrayList<>();
		Map<ItemStack, Integer> cost = new HashMap<>();

		//==================================================================================================
		//                                     FUNCTIONAL ITEMS
		//                                 DONT modify these items
		//==================================================================================================
		ItemStack shoulderLeft = buildItem(Material.COOKIE, "Shoulder Left", new ArrayList<>());
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 6, shoulderLeft, (player, inv) -> {
						return !mShoulderSelected.equals(PlayerShoulder.LEFT);
					}, (player, inv) -> {
						mShoulderSelected = PlayerShoulder.LEFT;
						return true; }));

		ItemStack shoulderRight = buildItem(Material.COOKIE, "Shoulder Right", new ArrayList<>());
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 4, shoulderRight, (player, inv) -> {
						return !mShoulderSelected.equals(PlayerShoulder.RIGHT);
					}, (player, inv) -> {
						mShoulderSelected = PlayerShoulder.RIGHT;
						return true; }));

		ItemStack leftShoulder = buildItem(Material.BRICK, "Left Shoulder Selected", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 6, leftShoulder, (player, inv) -> {
						return mShoulderSelected.equals(PlayerShoulder.LEFT);
					}, (player, inv) -> {
						mShoulderSelected = PlayerShoulder.NONE;
						return true;
					}));

		ItemStack rightShoulder = buildItem(Material.BRICK, "Right Shoulder Selected", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 4, rightShoulder, (player, inv) -> {
					return mShoulderSelected.equals(PlayerShoulder.RIGHT);
				}, (player, inv) -> {
					mShoulderSelected = PlayerShoulder.NONE;
					return true;
				}));

		ItemStack removeParrots = buildItem(Material.FEATHER, "Remove Parrots", new ArrayList<>());
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 8, removeParrots, (player, inv) -> {
						return ParrotManager.hasParrotOnShoulders(player); },
							(player, inv) -> {
								ParrotManager.clearParrots(player);
								return true; }));

		ItemStack visibleParrots = buildItem(Material.SADDLE, "Set parrots visible", new ArrayList<>());
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 2, visibleParrots, (player, inv) -> {
			return ParrotManager.hasParrotOnShoulders(player) && !ParrotManager.areParrotsVisible(player);
		},
		                          (player, inv) -> {
			                          ParrotManager.setParrotVisible(player, true);
			                          return true;
		                          }));

		ItemStack invisibleParrots = buildItem(Material.SADDLE, "Set parrots invisible", new ArrayList<>());
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 2, invisibleParrots, (player, inv) -> {
			return ParrotManager.hasParrotOnShoulders(player) && ParrotManager.areParrotsVisible(player);
		},
		                          (player, inv) -> {
			                          ParrotManager.setParrotVisible(player, false);
			                          return true;
		                          }));

		lore.clear();
		lore.add("Click to buy!");
		lore.add("64HCS");
		cost.clear();
		cost.put(mHCS, 64);
		ItemStack bothShoulders = buildItem(Material.INK_SAC, "Buy Both Shoulders", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 5, bothShoulders, new HashMap<>(cost), (player, inv) -> {
						return !ParrotManager.hasDoubleShoulders(player); },
							(player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_BOUGHT_SHOULDERS, (int) Instant.now().getEpochSecond());
								return true; }));

		lore.clear();
		lore.add("Owned");
		lore.add(new Date((long)ScoreboardUtils.getScoreboardValue(playerLoad, SCOREBOARD_BOUGHT_SHOULDERS).orElse(0)*1000).toString());
		ItemStack boughtShoulders = buildItem(Material.INK_SAC, "Both Shoulders", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 5, boughtShoulders, (player, inv) -> {
						return ParrotManager.hasDoubleShoulders(player); }));


		lore.clear();
		ItemStack turnRight = buildItem(Material.ARROW, "Turn Page ->", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 1, turnRight, (player, inv) -> {
						return mCurrentPage != ParrotGUIPage.SPECIAL; },
							(player, inv) -> {
								if (mCurrentPage == ParrotGUIPage.R1) {
									mCurrentPage = ParrotGUIPage.R2;
								} else if (mCurrentPage == ParrotGUIPage.R2) {
									mCurrentPage = ParrotGUIPage.SPECIAL;
								}
								return true; }));

		ItemStack turnLeft = buildItem(Material.ARROW, "<- Turn Page", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.OTHERS.mNum, ROWS * COLUMNS - 9, turnLeft, (player, inv) -> {
						return mCurrentPage != ParrotGUIPage.R1; },
							(player, inv) -> {
								if (mCurrentPage == ParrotGUIPage.R2) {
									mCurrentPage = ParrotGUIPage.R1;
								} else if (mCurrentPage == ParrotGUIPage.SPECIAL) {
									mCurrentPage = ParrotGUIPage.R2;
								}
								return true; }));

		//==================================================================================================
		//                                     FUNCTIONAL ITEMS end
		//==================================================================================================



		//from now on there are only separate parrots per page
		//in pairs, first the one to buy then the one bought


		//==================================================================================================
		//                                         R1 parrots
		//==================================================================================================
		lore.clear();
		lore.add("48HXP");
		cost.clear();
		cost.put(mHXP, 48);

		//GREEN
		ItemStack buyGreen = buildItem(Material.GREEN_WOOL, "Buy Green Parakeet", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 10, buyGreen, new HashMap<>(cost), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought4").orElse(0) == 0 && mShoulderSelected == PlayerShoulder.NONE; },
							(player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, "ParrotBought4", (int) Instant.now().getEpochSecond());
								return true; }));

		lore.clear();
		lore.add("Owned");
		lore.add(new Date((long)ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought4").orElse(0)*1000).toString());
		ItemStack boughGreen = buildItem(Material.GREEN_WOOL, "Green Parakeet", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 10, boughGreen, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought4").orElse(0) > 0 && mShoulderSelected == PlayerShoulder.NONE; }));
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 10, boughGreen, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought4").orElse(0) > 0 && mShoulderSelected != PlayerShoulder.NONE; },
				(player, inv) -> {
					ParrotManager.selectParrot(player, ParrotVariant.GREEN, mShoulderSelected);
					return true;
				}));

		//GRAY
		lore.clear();
		lore.add("48HXP");
		ItemStack buyGray = buildItem(Material.LIGHT_GRAY_WOOL, "Buy Gray Cockatiel", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 11, buyGray, new HashMap<>(cost), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought5").orElse(0) == 0 && mShoulderSelected == PlayerShoulder.NONE; },
							(player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, "ParrotBought5", (int) Instant.now().getEpochSecond());
								return true; }));

		lore.clear();
		lore.add("Owned");
		lore.add(new Date((long)ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought5").orElse(0)*1000).toString());
		ItemStack boughGray = buildItem(Material.LIGHT_GRAY_WOOL, "Gray Cockatiel", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 11, boughGray, (player, inv) -> {
							return ScoreboardUtils.getScoreboardValue(player, "ParrotBought5").orElse(0) > 0 && mShoulderSelected == PlayerShoulder.NONE; }));
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 11, boughGray, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought5").orElse(0) > 0 && mShoulderSelected != PlayerShoulder.NONE; },
							(player, inv) -> {
								ParrotManager.selectParrot(player, ParrotVariant.GRAY, mShoulderSelected);
								return true; }));

		//GOLD
		lore.clear();
		lore.add("Click to buy!");
		lore.add("64 Pulsating Gold");
		cost.clear();
		cost.put(mPGo, 64);
		ItemStack buyGolden = buildItem(Material.YELLOW_CONCRETE, "Buy Golden Conure", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 12, buyGolden, new HashMap<>(cost), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought7").orElse(0) == 0 && mShoulderSelected == PlayerShoulder.NONE; },
							(player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, "ParrotBought7", (int) Instant.now().getEpochSecond());
								return true; }));

		lore.clear();
		lore.add("Owned");
		lore.add(new Date((long)ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought7").orElse(0)*1000).toString());
		ItemStack boughGolden = buildItem(Material.YELLOW_CONCRETE, "Golden Conure", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 12, boughGolden, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought7").orElse(0) > 0 && mShoulderSelected == PlayerShoulder.NONE; }));
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 12, boughGolden, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought7").orElse(0) > 0 && mShoulderSelected != PlayerShoulder.NONE; },
							(player, inv) -> {
								ParrotManager.selectParrot(player, ParrotVariant.PULSATING_GOLD, mShoulderSelected);
								return true; }));

		//BEE Parrot!
		lore.clear();
		lore.add("Click to buy!");
		lore.add("80 HXP");
		cost.clear();
		cost.put(mHXP, 80);
		ItemStack buyBee = buildItem(Material.HONEYCOMB_BLOCK, "Buy Bee Conure", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 13, buyBee, new HashMap<>(cost), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought16").orElse(0) == 0 && mShoulderSelected == PlayerShoulder.NONE;
						}, (player, inv) -> {
							ScoreboardUtils.setScoreboardValue(player, "ParrotBought16", (int) Instant.now().getEpochSecond());
							return true; }));

		lore.clear();
		lore.add("Owned");
		lore.add(new Date((long)ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought16").orElse(0)*1000).toString());
		ItemStack boughtBee = buildItem(Material.HONEYCOMB_BLOCK, "Bee Conure", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 13, boughtBee, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought16").orElse(0) > 0 && mShoulderSelected == PlayerShoulder.NONE;
						}));
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 13, boughtBee, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought16").orElse(0) > 0 && mShoulderSelected != PlayerShoulder.NONE;
							}, (player, inv) -> {
			ParrotManager.selectParrot(player, ParrotVariant.BEE, mShoulderSelected);
								return true;
							}));

		//Radiant
		lore.clear();
		lore.add("Defeat Arena of Terth to learn more about this parrot");
		ItemStack canBuyRadiant = buildItem(Material.GLOWSTONE, "Radiant Conure", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 14, canBuyRadiant, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought17").orElse(0) == 0 &&
							ScoreboardUtils.getScoreboardValue(player, "Arena").orElse(0) == 0 &&
							mShoulderSelected == PlayerShoulder.NONE;
							}));

		lore.clear();
		lore.add("Click to buy!");
		lore.add("48 HXP");
		cost.clear();
		cost.put(mHXP, 48);
		ItemStack buyRadiant = buildItem(Material.GLOWSTONE, "Buy Radiant Conure", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 14, buyRadiant, new HashMap<>(cost), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought17").orElse(0) == 0 &&
							ScoreboardUtils.getScoreboardValue(player, "Arena").orElse(0) != 0 &&
							mShoulderSelected == PlayerShoulder.NONE;
							}, (player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, "ParrotBought17", (int) Instant.now().getEpochSecond());
								return true;
							}));

		lore.clear();
		lore.add("Owned");
		lore.add(new Date((long)ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought17").orElse(0)*1000).toString());
		ItemStack boughtRadiant = buildItem(Material.GLOWSTONE, "Radiant Conure", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 14, boughtRadiant, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought17").orElse(0) > 0 && mShoulderSelected == PlayerShoulder.NONE;
						}));
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 14, boughtRadiant, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought17").orElse(0) > 0 && mShoulderSelected != PlayerShoulder.NONE;
							}, (player, inv) -> {
			ParrotManager.selectParrot(player, ParrotVariant.RADIANT, mShoulderSelected);
								return true;
							}));

		//Kaul!
		lore.clear();
		lore.add("Requires 50 Kaul wins to buy");
		int currentWins = ScoreboardUtils.getScoreboardValue(playerLoad, "KaulWins").orElse(0);
		lore.add("You still need " + (50 - currentWins) + " wins");
		ItemStack winKaul = buildItem(Material.JUNGLE_LEAVES, "Blackroot Kakapo", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 15, winKaul, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "KaulWins").orElse(0) < 50 && mShoulderSelected == PlayerShoulder.NONE;
		}));

		lore.clear();
		lore.add("Click to buy!");
		lore.add("80 Shard of the Mantle");
		cost.clear();
		cost.put(mKS, 80);
		ItemStack buyKaul = buildItem(Material.JUNGLE_LEAVES, "Blackroot Kakapo", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 15, buyKaul, new HashMap<>(cost), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "KaulWins").orElse(0) >= 50 && mShoulderSelected == PlayerShoulder.NONE;
							}, (player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, "ParrotBought10", (int) Instant.now().getEpochSecond());
								return true;
							}));

		lore.clear();
		lore.add("Owned");
		lore.add(new Date((long)ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought10").orElse(0)*1000).toString());
		ItemStack boughtKaul = buildItem(Material.JUNGLE_LEAVES, "Blackroot Kakapo", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 15, boughtKaul, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought10").orElse(0) > 0 && mShoulderSelected == PlayerShoulder.NONE; }));
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R1.mNum, 15, boughtKaul, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought10").orElse(0) > 0 && mShoulderSelected != PlayerShoulder.NONE;
							}, (player, inv) -> {
			ParrotManager.selectParrot(player, ParrotVariant.KAUL, mShoulderSelected);
								return true;
							}));

		//==================================================================================================
		//                                         R1 parrots end
		//==================================================================================================

		//==================================================================================================
		//                                         R2 parrots
		//==================================================================================================

		//RED PARROT
		lore.clear();
		lore.add("Click to buy!");
		lore.add("48HCS");
		cost.clear();
		cost.put(mHCS, 48);
		ItemStack buyRed = buildItem(Material.RED_WOOL, "Buy Scarlet Macaw", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 10, buyRed, new HashMap<>(cost), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought1").orElse(0) == 0 && mShoulderSelected == PlayerShoulder.NONE;
							}, (player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, "ParrotBought1", (int) Instant.now().getEpochSecond());
								return true;
							}));
		lore.clear();
		lore.add("Owned");
		lore.add(new Date((long)ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought1").orElse(0)*1000).toString());
		ItemStack boughtRed = buildItem(Material.RED_WOOL, "Scarlet Macaw", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 10, boughtRed, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought1").orElse(0) > 0 && mShoulderSelected == PlayerShoulder.NONE; }));
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 10, boughtRed, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought1").orElse(0) > 0 && mShoulderSelected != PlayerShoulder.NONE; },
							(player, inv) -> {
								ParrotManager.selectParrot(player, ParrotVariant.RED, mShoulderSelected);
								return true; }));


		//BLUE PARROT
		lore.clear();
		lore.add("Click to buy!");
		lore.add("48HCS");
		cost.clear();
		cost.put(mHCS, 48);
		ItemStack buyBlue = buildItem(Material.BLUE_WOOL, "Buy Hyacinth Macaw", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 11, buyBlue, new HashMap<>(cost), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought2").orElse(0) == 0 && mShoulderSelected == PlayerShoulder.NONE;
							}, (player, inv) -> {
									ScoreboardUtils.setScoreboardValue(player, "ParrotBought2", (int) Instant.now().getEpochSecond());
									return true; }));
		lore.clear();
		lore.add("Owned");
		lore.add(new Date((long)ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought2").orElse(0)*1000).toString());
		ItemStack boughtBlue = buildItem(Material.BLUE_WOOL, "Hyacinth Macaw", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 11, boughtBlue, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought2").orElse(0) > 0 && mShoulderSelected == PlayerShoulder.NONE; }));
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 11, boughtBlue, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought2").orElse(0) > 0 && mShoulderSelected != PlayerShoulder.NONE;
							}, (player, inv) -> {
			ParrotManager.selectParrot(player, ParrotVariant.BLUE, mShoulderSelected);
								return true; }));


		//BLUE-YELLOW
		lore.clear();
		lore.add("Click to buy!");
		lore.add("48HCS");
		ItemStack buyYellow = buildItem(Material.LIGHT_BLUE_WOOL, "Buy Blue-Yellow Macaw", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 12, buyYellow, new HashMap<>(cost), (player, inv) -> {
							return ScoreboardUtils.getScoreboardValue(player, "ParrotBought3").orElse(0) == 0 && mShoulderSelected == PlayerShoulder.NONE; },
								(player, inv) -> {
									ScoreboardUtils.setScoreboardValue(player, "ParrotBought3", (int) Instant.now().getEpochSecond());
									return true; }));
		lore.clear();
		lore.add("Owned");
		lore.add(new Date((long)ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought3").orElse(0)*1000).toString());
		ItemStack boughtYellow = buildItem(Material.LIGHT_BLUE_WOOL, "Blue-Yellow Macaw", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 12, boughtYellow, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought3").orElse(0) > 0 && mShoulderSelected == PlayerShoulder.NONE; }));
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 12, boughtYellow, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought3").orElse(0) > 0 && mShoulderSelected != PlayerShoulder.NONE; },
							(player, inv) -> {
								ParrotManager.selectParrot(player, ParrotVariant.CYAN, mShoulderSelected);
								return true; }));

		//EMERALD
		lore.clear();
		lore.add("Click to buy!");
		lore.add("64 Pulsating Emerald");
		cost.clear();
		cost.put(mPPe, 64);
		ItemStack buyEmerald = buildItem(Material.GREEN_CONCRETE, "Buy Emerald Conure", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 13, buyEmerald, new HashMap<>(cost), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought8").orElse(0) == 0 && mShoulderSelected == PlayerShoulder.NONE; },
							(player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, "ParrotBought8", (int) Instant.now().getEpochSecond());
								return true; }));

		lore.clear();
		lore.add("Owned");
		lore.add(new Date((long)ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought8").orElse(0)*1000).toString());
		ItemStack boughEmerald = buildItem(Material.GREEN_CONCRETE, "Emerald Conure", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 13, boughEmerald, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought8").orElse(0) > 0 && mShoulderSelected == PlayerShoulder.NONE; }));
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 13, boughEmerald, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought8").orElse(0) > 0 && mShoulderSelected != PlayerShoulder.NONE; },
							(player, inv) -> {
								ParrotManager.selectParrot(player, ParrotVariant.PULSATING_EMERALD, mShoulderSelected);
								return true; }));

		//PIRATE
		lore.clear();
		lore.add("Click to buy!");
		lore.add("80HCS");
		cost.clear();
		cost.put(mHCS, 80);
		ItemStack buyPirate = buildItem(Material.PURPLE_WOOL, "Buy Scoundrel Macaw", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 14, buyPirate, new HashMap<>(cost), (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought9").orElse(0) == 0 && mShoulderSelected == PlayerShoulder.NONE; },
						(player, inv) -> {
								ScoreboardUtils.setScoreboardValue(player, "ParrotBought9", (int) Instant.now().getEpochSecond());
								return true; }));

		lore.clear();
		lore.add("Owned");
		lore.add(new Date((long)ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought9").orElse(0)*1000).toString());
		ItemStack boughPirate = buildItem(Material.PURPLE_WOOL, "Scoundrel Macaw", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 14, boughPirate, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought9").orElse(0) > 0 && mShoulderSelected == PlayerShoulder.NONE; }));
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 14, boughPirate, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "ParrotBought9").orElse(0) > 0 && mShoulderSelected != PlayerShoulder.NONE; },
							(player, inv) -> {
								ParrotManager.selectParrot(player, ParrotVariant.PIRATE, mShoulderSelected);
								return true; }));

		//Snowy
		lore.clear();
		lore.add("Click to buy!");
		lore.add("80HCS");
		ItemStack buySnowy = buildItem(Material.SNOW_BLOCK, "Buy Snowy Cockatoo", lore);
		cost.clear();
		cost.put(mHCS, 80);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 15, buySnowy, new HashMap<>(cost), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought13").orElse(0) == 0 && mShoulderSelected == PlayerShoulder.NONE;
		}, (player, inv) -> {
			ScoreboardUtils.setScoreboardValue(player, "ParrotBought13", (int) Instant.now().getEpochSecond());
			return true;
		}));

		lore.clear();
		lore.add("Owned");
		lore.add(new Date((long)ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought13").orElse(0)*1000).toString());
		ItemStack boughtSnowy = buildItem(Material.SNOW_BLOCK, "Snowy Cockatoo", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 15, boughtSnowy, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought13").orElse(0) > 0 && mShoulderSelected == PlayerShoulder.NONE;
		}));

		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 15, boughtSnowy, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought13").orElse(0) > 0 && mShoulderSelected != PlayerShoulder.NONE;
		}, (player, inv) -> {
			ParrotManager.selectParrot(player, ParrotVariant.SNOWY, mShoulderSelected);
			return true;
		}));

		//Eldrask
		lore.clear();
		lore.add("Requires 50 Eldrask wins to buy");
		currentWins = ScoreboardUtils.getScoreboardValue(playerLoad, "FGWins").orElse(0);
		lore.add("You still need " + (50 - currentWins) + " wins");
		ItemStack winEldrask = buildItem(Material.BLUE_ICE, "Permafrost Kea", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 16, winEldrask, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "EldraskWins").orElse(0) < 50 && mShoulderSelected == PlayerShoulder.NONE;
		}));

		lore.clear();
		lore.add("Click to buy!");
		lore.add("80 Titanic Knowledge");
		cost.clear();
		cost.put(mFG, 80);
		ItemStack buyEldrask = buildItem(Material.BLUE_ICE, "Permafrost Kea", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 16, buyEldrask, new HashMap<>(cost), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "FGWins").orElse(0) >= 50 && mShoulderSelected == PlayerShoulder.NONE;
		}, (player, inv) -> {
			ScoreboardUtils.setScoreboardValue(player, "ParrotBought11", (int) Instant.now().getEpochSecond());
			return true;
		}));

		lore.clear();
		lore.add("Owned");
		lore.add(new Date((long)ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought11").orElse(0)*1000).toString());
		ItemStack boughtEldrask = buildItem(Material.BLUE_ICE, "Permafrost Kea", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 16, boughtEldrask, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought11").orElse(0) > 0 && mShoulderSelected == PlayerShoulder.NONE;
		}));
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 16, boughtEldrask, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought11").orElse(0) > 0 && mShoulderSelected != PlayerShoulder.NONE;
		}, (player, inv) -> {
			ParrotManager.selectParrot(player, ParrotVariant.ELDRASK, mShoulderSelected);
			return true;
		}));

		//Hekawt - Lich
		lore.clear();
		lore.add("Requires 50 Hekawt wins to buy");
		currentWins = ScoreboardUtils.getScoreboardValue(playerLoad, "LichWins").orElse(0);
		lore.add("You still need " + (50 - currentWins) + " wins");
		ItemStack winHekawt = buildItem(Material.MAGMA_BLOCK, "Veil Electus", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 19, winHekawt, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "LichWins").orElse(0) < 50 && mShoulderSelected == PlayerShoulder.NONE;
		}));

		lore.clear();
		lore.add("Click to buy!");
		lore.add("80 Ancestral Effigy");
		cost.clear();
		cost.put(mLi, 80);
		ItemStack buyHekawt = buildItem(Material.MAGMA_BLOCK, "Buy Veil Electus", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 19, buyHekawt, new HashMap<>(cost), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "LichWins").orElse(0) >= 50 && mShoulderSelected == PlayerShoulder.NONE;
		}, (player, inv) -> {
			ScoreboardUtils.setScoreboardValue(player, "ParrotBought18", (int) Instant.now().getEpochSecond());
			return true;
		}));

		lore.clear();
		lore.add("Owned");
		lore.add(new Date((long)ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought18").orElse(0)*1000).toString());
		ItemStack boughtHekawt = buildItem(Material.MAGMA_BLOCK, "Veil Electus", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 19, boughtHekawt, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought18").orElse(0) > 0 && mShoulderSelected == PlayerShoulder.NONE;
		}));
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 19, boughtHekawt, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought18").orElse(0) > 0 && mShoulderSelected != PlayerShoulder.NONE;
		}, (player, inv) -> {
			ParrotManager.selectParrot(player, ParrotVariant.HEKAWT, mShoulderSelected);
			return true;
		}));

		//Depths
		lore.clear();
		lore.add("Defeat Darkest Depths to learn more about this parrot");
		ItemStack buyDepths1 = buildItem(Material.RED_GLAZED_TERRACOTTA, "Buy Otherworldly Myiopsitta", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 20, buyDepths1, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought14").orElse(0) == 0 &&
			ScoreboardUtils.getScoreboardValue(playerLoad, "DepthsEndless").orElse(0) < 61 &&
			ScoreboardUtils.getScoreboardValue(playerLoad, "Depths").orElse(0) == 0 &&
			mShoulderSelected == PlayerShoulder.NONE;
		}));

		lore.clear();
		lore.add("Requires clearing floor 9 from Darkest Depths");
		int depthsScore = ScoreboardUtils.getScoreboardValue(playerLoad, "DepthsEndless").orElse(0);
		lore.add("You have cleared floor " + ((depthsScore - 1) / 10));
		ItemStack buyDepths = buildItem(Material.RED_GLAZED_TERRACOTTA, "Buy Otherworldly Myiopsitta", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 20, buyDepths, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought14").orElse(0) == 0 &&
			ScoreboardUtils.getScoreboardValue(playerLoad, "DepthsEndless").orElse(0) < 91 &&
			ScoreboardUtils.getScoreboardValue(playerLoad, "Depths").orElse(0) > 0 &&
			mShoulderSelected == PlayerShoulder.NONE;
		}));

		cost.clear();
		cost.put(mDM, 64);
		lore.clear();
		lore.add("Click to buy!");
		lore.add("64 Voidstained Geode");
		ItemStack canBuyDepths = buildItem(Material.RED_GLAZED_TERRACOTTA, "Otherworldly Myiopsitta", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 20, canBuyDepths, new HashMap<>(cost), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought14").orElse(0) == 0 &&
			ScoreboardUtils.getScoreboardValue(playerLoad, "DepthsEndless").orElse(0) >= 91 &&
			mShoulderSelected == PlayerShoulder.NONE;
		}, (player, inv) -> {
			ScoreboardUtils.setScoreboardValue(player, "ParrotBought14", (int) Instant.now().getEpochSecond());
			return true;
		}));

		lore.clear();
		lore.add("Owned");
		lore.add(new Date((long)ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought14").orElse(0)*1000).toString());
		ItemStack boughtDepths = buildItem(Material.RED_GLAZED_TERRACOTTA, "Otherworldly Myiopsitta", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 20, boughtDepths, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought14").orElse(0) > 0 && mShoulderSelected == PlayerShoulder.NONE;
		}));
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 20, boughtDepths, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought14").orElse(0) > 0 && mShoulderSelected != PlayerShoulder.NONE;
		}, (player, inv) -> {
			ParrotManager.selectParrot(player, ParrotVariant.DEPTHS, mShoulderSelected);
			return true;
		}));

		//Depths Upgrade
		lore.clear();
		lore.add("You have to unlock Otherworldly Myiopsitta");
		lore.add("to learn more about this parrot");
		ItemStack canBuyDepthsU = buildItem(Material.CRYING_OBSIDIAN, "Otherworldly Myiopsitta (u)", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 21, canBuyDepthsU, (player, inv) -> {
							return ScoreboardUtils.getScoreboardValue(player, "ParrotBought14").orElse(0) == 0 &&
							mShoulderSelected == PlayerShoulder.NONE;
								}));

		lore.clear();
		lore.add("Requires clearing floor 15 from Darkest Depths");
		lore.add("You have cleared floor " + ((depthsScore - 1) / 10));
		ItemStack canBuyDepthsU2 = buildItem(Material.CRYING_OBSIDIAN, "Otherworldly Myiopsitta (u)", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 21, canBuyDepthsU2, (player, inv) -> {
							return ScoreboardUtils.getScoreboardValue(player, "ParrotBought14").orElse(0) > 0 &&
							ScoreboardUtils.getScoreboardValue(playerLoad, "DepthsEndless").orElse(0) < 151 &&
							ScoreboardUtils.getScoreboardValue(player, "ParrotBought15").orElse(0) == 0 &&
							mShoulderSelected == PlayerShoulder.NONE;
								}));

		cost.clear();
		cost.put(mDM, 96);
		lore.clear();
		lore.add("Click to buy!");
		lore.add("96 Voidstained Geode");
		ItemStack buyDepthsU = buildItem(Material.CRYING_OBSIDIAN, "Otherworldly Myiopsitta (u)", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 21, buyDepthsU, new HashMap<>(cost), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought15").orElse(0) == 0 &&
			ScoreboardUtils.getScoreboardValue(playerLoad, "DepthsEndless").orElse(0) >= 151 &&
			ScoreboardUtils.getScoreboardValue(player, "ParrotBought14").orElse(0) > 0 &&
			mShoulderSelected == PlayerShoulder.NONE;
		}, (player, inv) -> {
			ScoreboardUtils.setScoreboardValue(player, "ParrotBought15", (int) Instant.now().getEpochSecond());
			return true;
		}));

		lore.clear();
		lore.add("Owned");
		lore.add(new Date((long)ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought15").orElse(0)*1000).toString());
		ItemStack boughtDepthsU = buildItem(Material.CRYING_OBSIDIAN, "Otherworldly Myiopsitta (u)", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 21, boughtDepthsU, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought15").orElse(0) > 0 && mShoulderSelected == PlayerShoulder.NONE;
		}));
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.R2.mNum, 21, boughtDepthsU, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought15").orElse(0) > 0 && mShoulderSelected != PlayerShoulder.NONE;
		}, (player, inv) -> {
			ParrotManager.selectParrot(player, ParrotVariant.DEPTHS_UPGRADE, mShoulderSelected);
			return true;
		}));

		//==================================================================================================
		//                                         R2 parrots end
		//==================================================================================================

		//==================================================================================================
		//                                         Specials parrots
		//==================================================================================================

		//setting lore for patreon
		lore.clear();
		lore.add("Become a Tier 2 patreon to unlock");
		ItemStack buyPatreon = buildItem(Material.ORANGE_WOOL, "Patron Parakeet", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.SPECIAL.mNum, 10, buyPatreon, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "Patreon").orElse(0) < 5 && mShoulderSelected == PlayerShoulder.NONE; }));

		lore.clear();
		lore.add("Owned");
		//the patreon parrot item don't have a date
		ItemStack boughPatreon = buildItem(Material.ORANGE_WOOL, "Patreon Parakeet", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.SPECIAL.mNum, 10, boughPatreon, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "Patreon").orElse(0) >= 5 && mShoulderSelected == PlayerShoulder.NONE; }));
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.SPECIAL.mNum, 10, boughPatreon, (player, inv) -> {
						return ScoreboardUtils.getScoreboardValue(player, "Patreon").orElse(0) >= 5 && mShoulderSelected != PlayerShoulder.NONE;
							}, (player, inv) -> {
			ParrotManager.selectParrot(player, ParrotVariant.PATREON, mShoulderSelected);
								return true; }));

		//PRIDE PARROT!
		lore.clear();
		lore.add("You have to unlock these parrots");
		lore.add("before being able to purchase this one:");
		if (ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought1").orElse(0) == 0) {
			lore.add("Scarlet Macaw");
		} else {
			lore.add("##Scarlet Macaw");
		}
		if (ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought2").orElse(0) == 0) {
			lore.add("Hyacinth Macaw");
		} else {
			lore.add("##Hyacinth Macaw");
		}
		if (ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought3").orElse(0) == 0) {
			lore.add("Blue-Yellow Macaw");
		} else {
			lore.add("##Blue-Yellow Macaw");
		}
		if (ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought4").orElse(0) == 0) {
			lore.add("Green Parakeet");
		} else {
			lore.add("##Green Parakeet");
		}
		if (ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought5").orElse(0) == 0) {
			lore.add("Gray Cockatiel");
		} else {
			lore.add("##Gray Cockatiel");
		}
		ItemStack prideParrot = buildItem(Material.YELLOW_GLAZED_TERRACOTTA, "Rainbow Parrot", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.SPECIAL.mNum, 11, prideParrot, (player, inv) -> {
			return mShoulderSelected == PlayerShoulder.NONE && (
			ScoreboardUtils.getScoreboardValue(player, "ParrotBought1").orElse(0) == 0 ||
			ScoreboardUtils.getScoreboardValue(player, "ParrotBought2").orElse(0) == 0 ||
			ScoreboardUtils.getScoreboardValue(player, "ParrotBought3").orElse(0) == 0 ||
			ScoreboardUtils.getScoreboardValue(player, "ParrotBought4").orElse(0) == 0 ||
			ScoreboardUtils.getScoreboardValue(player, "ParrotBought5").orElse(0) == 0);
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
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.SPECIAL.mNum, 11, buyPrideParrot, new HashMap<>(cost), (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought1").orElse(0) > 0 &&
			ScoreboardUtils.getScoreboardValue(player, "ParrotBought2").orElse(0) > 0 &&
			ScoreboardUtils.getScoreboardValue(player, "ParrotBought3").orElse(0) > 0 &&
			ScoreboardUtils.getScoreboardValue(player, "ParrotBought4").orElse(0) > 0 &&
			ScoreboardUtils.getScoreboardValue(player, "ParrotBought5").orElse(0) > 0 &&
			mShoulderSelected == PlayerShoulder.NONE;
		}, (player, inv) -> {
			ScoreboardUtils.setScoreboardValue(player, "ParrotBought12", (int) Instant.now().getEpochSecond());
			return true;
		}));

		lore.clear();
		lore.add("Owned");
		lore.add(new Date((long)ScoreboardUtils.getScoreboardValue(playerLoad, "ParrotBought12").orElse(0)*1000).toString());
		ItemStack boughtPrideParrot = buildItem(Material.YELLOW_GLAZED_TERRACOTTA, "Rainbow Parrot", lore);
		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.SPECIAL.mNum, 11, boughtPrideParrot, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought12").orElse(0) > 0 && mShoulderSelected == PlayerShoulder.NONE;
		}));

		GUI_ITEMS.add(new GuiItem(ParrotGUIPage.SPECIAL.mNum, 11, boughtPrideParrot, (player, inv) -> {
			return ScoreboardUtils.getScoreboardValue(player, "ParrotBought12").orElse(0) > 0 && mShoulderSelected != PlayerShoulder.NONE;
		}, (player, inv) -> {
			ParrotManager.selectParrot(player, ParrotVariant.RAINBOW, mShoulderSelected);
			return true;
		}));

		//==================================================================================================
		//                                         Special parrots end
		//==================================================================================================


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
		owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 3f, 1.2f);

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

		for (GuiItem gItem : GUI_ITEMS) {
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
		GuiItem gItem = mInvMapping.get(slotClicked);
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
