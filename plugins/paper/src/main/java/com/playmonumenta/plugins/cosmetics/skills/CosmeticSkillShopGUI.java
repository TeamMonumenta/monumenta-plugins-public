package com.playmonumenta.plugins.cosmetics.skills;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.cosmetics.gui.CosmeticsGUI;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class CosmeticSkillShopGUI extends CustomInventory {
	//Price constants
	private static final int PIGMENT_PER_SKIN = 3;
	private static final int TALISMAN_PER_DEPTH_SKIN = 1;
	private static final int GEODE_PER_DEPTH_SKIN = 2 * 64;
	private static final int STRAND_PER_DELVE_SKIN = 64;
	private static final int CANVAS_PER_GALLERY_SKIN = 2 * 64;
	private static final NamespacedKey PIGMENT_LOOTTABLE = NamespacedKeyUtils.fromString("epic:r2/delves/items/twisted_pigment");
	private static final String TALISMAN_LOOTTABLE_FOLDER = "epic:r2/depths/utility/";
	private static final NamespacedKey GEODE_LOOTTABLE = NamespacedKeyUtils.fromString("epic:r2/depths/loot/voidstained_geode");
	private static final NamespacedKey STRAND_LOOTTABLE = NamespacedKeyUtils.fromString("epic:r2/delves/items/twisted_strand");
	private static final NamespacedKey CANVAS_LOOTTABLE = NamespacedKeyUtils.fromString("epic:r3/gallery/items/torn_canvas");
	private static final NamespacedKey PRESTIGE_LOOTTABLE = NamespacedKeyUtils.fromString("epic:r2/delves/items/reverial_roots");

	//Theme constants
	//Depths
	private static final List<String> DEPTH_INTRO = List.of(
		"Attuned with powers from",
		"Darkest Depths trees."
	);
	private static final ImmutableList<String> DEPTH_THEME = CosmeticSkills.getDepthsNames();
	private static final ImmutableList<DepthsCS> DEPTHS_CS = CosmeticSkills.getDepthsSkins();
	//Delves
	private static final List<String> DELVE_INTRO = List.of(
		"Essences of the twisted contingency,",
		"rewards for heroic adventurers."
	);
	private static final ImmutableList<String> DELVE_THEME = CosmeticSkills.getDelvesNames();
	//Prestige
	private static final List<String> PRESTIGE_INTRO = List.of(
		"Every single step here is",
		"a witness of your prestige and glory."
	);
	private static final ImmutableList<String> PRESTIGE_THEME = CosmeticSkills.getPrestigeNames();
	private static final ImmutableList<PrestigeCS> PRESTIGE_CS = CosmeticSkills.getPrestigeSkins();
	//Sanguine
	private static final List<String> GALLERY_INTRO = List.of(
		"The nightmare was never meant for life.",
		"Banish the dream.",
		"End this nightmare!"
	);
	private static final ImmutableList<String> GALLERY_THEME = CosmeticSkills.getGalleryNames();
	private static final ImmutableList<GalleryCS> GALLERY_CS = CosmeticSkills.getGallerySkins();

	//GUI constants
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private static final Material LOCKED = Material.BARRIER;
	private static final int LINE = 5;
	private static final int INTRO_LOC = 4;
	private static final int ENTRY_START = 10;
	private static final int ENTRY_PER_LINE = 7;
	private static final int ENTRY_PER_PAGE = ENTRY_PER_LINE * (LINE - 2);
	private static final int BACK_LOC = (LINE - 1) * 9 + 4;
	private static final int PREV_PAGE_LOC = (LINE - 1) * 9 + 0;
	private static final int NEXT_PAGE_LOC = (LINE - 1) * 9 + 8;

	private static final String R1MONUMENT_SCB = "R1Complete";
	private static final String DEPTHS_SCB = "Depths";
	private static final String GALLERY_SCB = "DGLobby";
	private static final String PRESTIGE_SCB = ""; //TODO: scb for unlocking hall
	private static final TextColor DEPTH_COLOR = TextColor.fromHexString("#5D2D87");
	private static final TextColor DELVE_COLOR = TextColor.fromHexString("#B47028");
	private static final TextColor PRESTIGE_COLOR = TextColor.fromHexString("#FEDC10");
	private static final TextColor GALLERY_COLOR = TextColor.fromHexString("#39B14E");
	private static final int DEPTH_ENTRY_LOC = 20;
	private static final int DELVE_ENTRY_LOC = 21;
	private static final int PRESTIGE_ENTRY_LOC = 22;
	private static final int GALLERY_ENTRY_LOC = 23;

	private final Plugin mPlugin;
	private CSGUIPage mCurrentPage = CSGUIPage.Home;
	private int mPageNumber = 1;

	private enum CSGUIPage {
		Home(0),
		Depth(1),
		Delve(2),
		Prestige(3),
		Gallery(4),
		Other(114);

		private final int mNum;

		CSGUIPage(int num) {
			mNum = num;
		}

		public static @Nullable
		CosmeticSkillShopGUI.CSGUIPage valueOfPage(int num) {
			for (CosmeticSkillShopGUI.CSGUIPage page : CosmeticSkillShopGUI.CSGUIPage.values()) {
				if (page.mNum == num) {
					return page;
				}
			}
			return null;
		}
	}

	public CosmeticSkillShopGUI(Plugin plugin, Player player) {
		super(player, 9*LINE, Component.text("Cosmetic Skill Shop", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		mPlugin = plugin;
		loadPage(CSGUIPage.Home, player);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		ItemStack item = event.getCurrentItem();
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		Player player = (Player) event.getWhoClicked();

		if (event.getClickedInventory() == mInventory) {
			int slot = event.getSlot();
			if (slot < 9 || item.getType() == FILLER) {
				// Reject: useless slot or locked content
				return;
			}
			if (item.getType() == LOCKED) {
				player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
				player.sendMessage(Component.text("You don't match the requirement to unlock this content!", NamedTextColor.RED));
				return;
			}

			// General: if not home page, back to shop home
			if (slot == BACK_LOC) {
				if (mCurrentPage != CSGUIPage.Home) {
					loadPage(CSGUIPage.Home, player);
					return;
				} else {
					close();
					new CosmeticsGUI(mPlugin, player).openInventory(player, mPlugin);
					return;
				}
			}
			switch (mCurrentPage) {
				case Depth: {
					int entry = slotToEntryNum(slot);

					// Clicked on a cosmetic. Check for buying
					if (entry >= 0 && entry < DEPTH_THEME.size()) {
						String skin = DEPTH_THEME.get(entry);
						if (!CosmeticsManager.getInstance().playerHasCosmetic(player, CosmeticType.COSMETIC_SKILL, skin)) {
							// Try to buy
							if (player.getGameMode() == GameMode.CREATIVE) {
								CosmeticsManager.getInstance().addCosmetic(player, CosmeticType.COSMETIC_SKILL, skin);
								player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.5f);
								player.sendMessage(Component.text("Because you are in creative mode, this is free!", NamedTextColor.GREEN));
								reloadPage(player);
								return;
							}

							NamespacedKey talismanLoot = NamespacedKeyUtils.fromString(TALISMAN_LOOTTABLE_FOLDER + DEPTHS_CS.get(entry).getToken());
							if (talismanLoot == null) {
								// Shouldn't be here! But leave it as a handler to avoid typo in code.
								player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 0.5f);
								player.sendMessage(Component.text("EX[" + skin + "]1: An exception occurred when buying cosmetic skill. Contact a moder or dev with this message to report if you believe this is a bug.", NamedTextColor.DARK_RED));
								close();
								return;
							}
							// Check costs
							ItemStack mGeode = InventoryUtils.getItemFromLootTable(player, GEODE_LOOTTABLE);
							ItemStack mPigment = InventoryUtils.getItemFromLootTable(player, PIGMENT_LOOTTABLE);
							ItemStack mTalisman = InventoryUtils.getItemFromLootTable(player, talismanLoot);
							if (!player.getInventory().containsAtLeast(mPigment, PIGMENT_PER_SKIN) ||
								!player.getInventory().containsAtLeast(mTalisman, TALISMAN_PER_DEPTH_SKIN) ||
								!player.getInventory().containsAtLeast(mGeode, GEODE_PER_DEPTH_SKIN)) {
								player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
								player.sendMessage(Component.text("You don't have enough items to buy this cosmetic skill!", NamedTextColor.RED));
								return;
							}
							// Remove items
							mGeode.setAmount(GEODE_PER_DEPTH_SKIN);
							mPigment.setAmount(PIGMENT_PER_SKIN);
							mTalisman.setAmount(TALISMAN_PER_DEPTH_SKIN);
							if (CosmeticsManager.getInstance().addCosmetic(player, CosmeticType.COSMETIC_SKILL, skin)) {
								player.getInventory().removeItem(mGeode);
								player.getInventory().removeItem(mPigment);
								player.getInventory().removeItem(mTalisman);
								player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.5f);
								player.sendMessage(Component.text("You successfully bought " + skin + "! Go to Cosmetic Manager to equip it!", NamedTextColor.GREEN));
								reloadPage(player);
								return;
							} else {
								// Shouldn't be here! But leave it as a handler to avoid typo in code.
								player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 0.5f);
								player.sendMessage(Component.text("EX[" + skin + "]2: An exception occurred when buying cosmetic skill. Contact a moder or dev with this message to report if you believe this is a bug.", NamedTextColor.DARK_RED));
								close();
								return;
							}
						} else {
							// Already bought
							player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
							player.sendMessage(Component.text("You already have this cosmetic skill. Go to Cosmetic Manager to equip it!", NamedTextColor.RED));
							return;
						}
					}

					// Changing page
					if (slot == PREV_PAGE_LOC) {
						mPageNumber--;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
						loadDepthPage(player);
						return;
					} else if (slot == NEXT_PAGE_LOC) {
						mPageNumber++;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
						loadDepthPage(player);
						return;
					}
				}
				break;
				case Delve: {
					int entry = slotToEntryNum(slot);

					// Clicked on a cosmetic. Check for buying
					if (entry >= 0 && entry < DELVE_THEME.size()) {
						String skin = DELVE_THEME.get(entry);
						if (!CosmeticsManager.getInstance().playerHasCosmetic(player, CosmeticType.COSMETIC_SKILL, skin)) {
							// Try to buy
							if (player.getGameMode() == GameMode.CREATIVE) {
								CosmeticsManager.getInstance().addCosmetic(player, CosmeticType.COSMETIC_SKILL, skin);
								player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.5f);
								player.sendMessage(Component.text("Because you are in creative mode, this is free!", NamedTextColor.GREEN));
								reloadPage(player);
								return;
							}

							// Check costs
							ItemStack mStrand = InventoryUtils.getItemFromLootTable(player, STRAND_LOOTTABLE);
							ItemStack mPigment = InventoryUtils.getItemFromLootTable(player, PIGMENT_LOOTTABLE);
							if (!player.getInventory().containsAtLeast(mPigment, PIGMENT_PER_SKIN) ||
								!player.getInventory().containsAtLeast(mStrand, STRAND_PER_DELVE_SKIN)) {
								player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
								player.sendMessage(Component.text("You don't have enough items to buy this cosmetic skill!", NamedTextColor.RED));
								return;
							}
							// Remove items
							mStrand.setAmount(STRAND_PER_DELVE_SKIN);
							mPigment.setAmount(PIGMENT_PER_SKIN);
							if (CosmeticsManager.getInstance().addCosmetic(player, CosmeticType.COSMETIC_SKILL, skin)) {
								player.getInventory().removeItem(mStrand);
								player.getInventory().removeItem(mPigment);
								player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.5f);
								player.sendMessage(Component.text("You successfully bought " + skin + "! Go to Cosmetic Manager to equip it!", NamedTextColor.GREEN));
								reloadPage(player);
								return;
							} else {
								// Shouldn't be here! But leave it as a handler to avoid typo in code.
								player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 0.5f);
								player.sendMessage(Component.text("EX[" + skin + "]2: An exception occurred when buying cosmetic skill. Contact a moder or dev with this message to report if you believe this is a bug.", NamedTextColor.DARK_RED));
								close();
								return;
							}
						} else {
							// Already bought
							player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
							player.sendMessage(Component.text("You already have this cosmetic skill. Go to Cosmetic Manager to equip it!", NamedTextColor.RED));
							return;
						}
					}

					// Changing page
					if (slot == PREV_PAGE_LOC) {
						mPageNumber--;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
						loadDepthPage(player);
						return;
					} else if (slot == NEXT_PAGE_LOC) {
						mPageNumber++;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
						loadDepthPage(player);
						return;
					}
				}
				break;
				case Prestige: {
					int entry = slotToEntryNum(slot);

					// Clicked on a cosmetic. Check for buying
					if (entry >= 0 && entry < PRESTIGE_THEME.size()) {
						String skin = PRESTIGE_THEME.get(entry);
						if (!CosmeticsManager.getInstance().playerHasCosmetic(player, CosmeticType.COSMETIC_SKILL, skin)) {
							// Try to buy
							if (player.getGameMode() == GameMode.CREATIVE) {
								CosmeticsManager.getInstance().addCosmetic(player, CosmeticType.COSMETIC_SKILL, skin);
								player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.5f);
								player.sendMessage(Component.text("Because you are in creative mode, this is free!", NamedTextColor.GREEN));
								reloadPage(player);
								return;
							}

							// Check costs
							ItemStack mStar = InventoryUtils.getItemFromLootTable(player, PRESTIGE_LOOTTABLE);
							int priceNum = PRESTIGE_CS.get(entry).getPrice();
							int removed = InventoryUtils.removeSoulboundItemFromInventory(player.getInventory(), ItemUtils.getPlainName(mStar), priceNum, player);
							if (removed == 0) {
								player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
								player.sendMessage(Component.text("You don't have enough items to buy this cosmetic skill!", NamedTextColor.RED));
							} else if (removed == priceNum) {
								// Successfully removed items
								if (CosmeticsManager.getInstance().addCosmetic(player, CosmeticType.COSMETIC_SKILL, skin)) {
									player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.5f);
									player.sendMessage(Component.text("You successfully bought " + skin + "! Go to Cosmetic Manager to equip it!", NamedTextColor.GREEN));
									reloadPage(player);
								} else {
									// Shouldn't be here! But leave it as a handler to avoid typo in code.
									player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 0.5f);
									player.sendMessage(Component.text("EX[" + skin + "]2: An exception occurred when buying cosmetic skill. Contact a moder or dev with this message to report if you believe this is a bug.", NamedTextColor.DARK_RED));
									close();
								}
							} else {
								// Shouldn't be here! But leave it as a handler to avoid typo in code.
								player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 0.5f);
								player.sendMessage(Component.text("EX[" + skin + "]3: An exception occurred when consuming Soulbound tokens. Contact a moder if you lose items.", NamedTextColor.DARK_RED));
							}
						} else {
							// Already bought
							player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
							player.sendMessage(Component.text("You already have this cosmetic skill. Go to Cosmetic Manager to equip it!", NamedTextColor.RED));
						}
						return;
					}

					// Changing page
					if (slot == PREV_PAGE_LOC) {
						mPageNumber--;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
						loadDepthPage(player);
						return;
					} else if (slot == NEXT_PAGE_LOC) {
						mPageNumber++;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
						loadDepthPage(player);
						return;
					}
				}
				break;
				case Gallery: {
					int entry = slotToEntryNum(slot);

					// Clicked on a cosmetic. Check for buying
					if (entry >= 0 && entry < GALLERY_THEME.size()) {
						String skin = GALLERY_THEME.get(entry);
						if (!CosmeticsManager.getInstance().playerHasCosmetic(player, CosmeticType.COSMETIC_SKILL, skin)) {
							// Try to buy
							if (player.getGameMode() == GameMode.CREATIVE) {
								CosmeticsManager.getInstance().addCosmetic(player, CosmeticType.COSMETIC_SKILL, skin);
								player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.5f);
								player.sendMessage(Component.text("Because you are in creative mode, this is free!", NamedTextColor.GREEN));
								reloadPage(player);
								return;
							}

							// Check costs
							ItemStack mPigment = InventoryUtils.getItemFromLootTable(player, PIGMENT_LOOTTABLE);
							ItemStack mCanvas = InventoryUtils.getItemFromLootTable(player, CANVAS_LOOTTABLE);
							if (!player.getInventory().containsAtLeast(mPigment, PIGMENT_PER_SKIN) ||
								!player.getInventory().containsAtLeast(mCanvas, CANVAS_PER_GALLERY_SKIN)) {
								player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
								player.sendMessage(Component.text("You don't have enough items to buy this cosmetic skill!", NamedTextColor.RED));
								return;
							}
							// Remove items
							mPigment.setAmount(PIGMENT_PER_SKIN);
							mCanvas.setAmount(CANVAS_PER_GALLERY_SKIN);
							if (CosmeticsManager.getInstance().addCosmetic(player, CosmeticType.COSMETIC_SKILL, skin)) {
								player.getInventory().removeItem(mPigment);
								player.getInventory().removeItem(mCanvas);
								player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.5f);
								player.sendMessage(Component.text("You successfully bought " + skin + "! Go to Cosmetic Manager to equip it!", NamedTextColor.GREEN));
								reloadPage(player);
								return;
							} else {
								// Shouldn't be here! But leave it as a handler to avoid typo in code.
								player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 0.5f);
								player.sendMessage(Component.text("EX[" + skin + "]2: An exception occurred when buying cosmetic skill. Contact a moder or dev with this message to report if you believe this is a bug.", NamedTextColor.DARK_RED));
								close();
								return;
							}
						} else {
							// Already bought
							player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
							player.sendMessage(Component.text("You already have this cosmetic skill. Go to Cosmetic Manager to equip it!", NamedTextColor.RED));
							return;
						}
					}

					// Changing page
					if (slot == PREV_PAGE_LOC) {
						mPageNumber--;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
						loadGalleryPage(player);
						return;
					} else if (slot == NEXT_PAGE_LOC) {
						mPageNumber++;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
						loadGalleryPage(player);
						return;
					}
				}
				break;
				default:
					//Reject: related content not discovered
					if (item.getType() == LOCKED) {
						return;
					}

					//Home page, choose skin set
					if (slot == DEPTH_ENTRY_LOC) {
						mPageNumber = 1;
						loadPage(CSGUIPage.Depth, player);
						return;
					} else if (slot == DELVE_ENTRY_LOC) {
						mPageNumber = 1;
						loadPage(CSGUIPage.Delve, player);
						return;
					} else if (slot == PRESTIGE_ENTRY_LOC) {
						mPageNumber = 1;
						loadPage(CSGUIPage.Prestige, player);
						return;
					} else if (slot == GALLERY_ENTRY_LOC) {
						mPageNumber = 1;
						loadPage(CSGUIPage.Gallery, player);
						return;
					}
			}
		}
	}

	private void reloadPage(Player player) {
		loadPage(mCurrentPage, player);
	}

	private void loadPage(CSGUIPage page, Player player) {
		mCurrentPage = page;
		player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
		// Set filler to start
		mInventory.clear();
		GUIUtils.fillWithFiller(mInventory, FILLER);

		switch (mCurrentPage) {
			case Depth:
				loadDepthPage(player);
				break;
			case Delve:
				loadDelvePage(player);
				break;
			case Prestige:
				loadPrestigePage(player);
				break;
			case Gallery:
				loadGalleryPage(player);
				break;
			default:
				// Intro item
				ItemStack introItem = createPageIcon(Material.RED_GLAZED_TERRACOTTA, "Theme Selection", NamedTextColor.RED, List.of("Select a theme to buy", "cosmetic skills!"));
				mInventory.setItem(INTRO_LOC, introItem);

				// Depth theme entry
				if (ScoreboardUtils.getScoreboardValue(player, DEPTHS_SCB).orElse(0) > 0 || player.getGameMode() == GameMode.CREATIVE) {
					ItemStack depthItem = createPageIcon(Material.BLACKSTONE, "Darkest Depths", DEPTH_COLOR, DEPTH_INTRO);
					mInventory.setItem(DEPTH_ENTRY_LOC, depthItem);
				} else {
					ItemStack depthItem = createPageIcon(LOCKED, "Da" + ChatColor.MAGIC + "rkest Dep" + ChatColor.RESET + "ths", DEPTH_COLOR,
						List.of("Complete " + ChatColor.MAGIC + "Darke" + ChatColor.RESET + "st D" + ChatColor.MAGIC + "epth" + ChatColor.RESET + "s",
							"to unlock this theme!"));
					mInventory.setItem(DEPTH_ENTRY_LOC, depthItem);
				}

				// Delve theme entry
				if (ScoreboardUtils.getScoreboardValue(player, R1MONUMENT_SCB).orElse(0) > 0 || player.getGameMode() == GameMode.CREATIVE) {
					ItemStack delveItem = createPageIcon(Material.NETHERITE_BLOCK, "Dungeon Delves", DELVE_COLOR, DELVE_INTRO);
					mInventory.setItem(DELVE_ENTRY_LOC, delveItem);
				} else {
					ItemStack delveItem = createPageIcon(LOCKED, ChatColor.MAGIC + "Dungeon D" + ChatColor.RESET + "elves", DELVE_COLOR,
						List.of("Complete Monument of King's Valley",
							"to unlock this theme!"));
					mInventory.setItem(DELVE_ENTRY_LOC, delveItem);
				}

				// Prestige theme entry
				final boolean prestige_enable = false; //TODO: Remove this awful restriction when launching prestige hall
				if (prestige_enable) {
					if (ScoreboardUtils.getScoreboardValue(player, PRESTIGE_SCB).orElse(0) > 0 || player.getGameMode() == GameMode.CREATIVE) { //Testing!
						ItemStack prestigeItem = createPageIcon(Material.GOLD_BLOCK, "Prestige Hall", PRESTIGE_COLOR, PRESTIGE_INTRO);
						mInventory.setItem(PRESTIGE_ENTRY_LOC, prestigeItem);
					} else {
						ItemStack prestigeItem = createPageIcon(LOCKED, "???", PRESTIGE_COLOR,
							List.of("Coming Soon!")); //TODO: pre hall condition
						mInventory.setItem(PRESTIGE_ENTRY_LOC, prestigeItem);
					}
				}

				// Gallery theme entry
				if (ScoreboardUtils.getScoreboardValue(player, GALLERY_SCB).orElse(0) > 0 || player.getGameMode() == GameMode.CREATIVE) {
					ItemStack galleryItem = createPageIcon(Material.WAXED_OXIDIZED_COPPER, "Gallery of Fear", GALLERY_COLOR, GALLERY_INTRO);
					mInventory.setItem(GALLERY_ENTRY_LOC, galleryItem);
				} else {
					ItemStack galleryItem = createPageIcon(LOCKED, ChatColor.MAGIC + "MzkCaerulaArbor" + ChatColor.RESET, GALLERY_COLOR,
						List.of("Reveal the secret b" + ChatColor.MAGIC + "eneth the ocean" + ChatColor.RESET + "s",
							"to unlock this theme!"));
					mInventory.setItem(GALLERY_ENTRY_LOC, galleryItem);
				}

				// Back item
				setBackItem("Back to Cosmetic Manager");
		}
	}

	private void loadDepthPage(Player player) {
		// Intro item
		{
			ItemStack introItem = createPageIcon(Material.BLACKSTONE, "Darkest Depths", DEPTH_COLOR, DEPTH_INTRO);
			mInventory.setItem(INTRO_LOC, introItem);
		}

		// Skin items
		{
			int numPages = (DEPTH_THEME.size() - 1) / ENTRY_PER_PAGE + 1;
			mPageNumber = Math.min(numPages, Math.max(1, mPageNumber));
			int currentSlot = ENTRY_START;
			// Paging
			for (int i = (mPageNumber - 1) * ENTRY_PER_PAGE; i < DEPTHS_CS.size(); ) {
				String skin = DEPTH_THEME.get(i);
				String tokenName = StringUtils.capitalizeWords(DEPTHS_CS.get(i).getToken().replace('_', ' '));
				List<String> price = List.of(
					PIGMENT_PER_SKIN + " Twisted Pigments,",
					TALISMAN_PER_DEPTH_SKIN + " " + tokenName + " and",
					GEODE_PER_DEPTH_SKIN + " Voidstained Geodes");
				ItemStack item = createSkillIcon(skin, DEPTH_COLOR, player, price);
				mInventory.setItem(currentSlot, item);
				if (slotToEntryNum(++currentSlot) < 0) {
					// New line
					currentSlot += (9 - ENTRY_PER_LINE);
				}
				if (++i % ENTRY_PER_PAGE == 0) {
					// End of current page number
					break;
				}
			}

			// Prev and next page buttons
			setPagingItems(numPages);
		}

		// Back item
		setBackItem("Back to Overview");
	}

	private void loadDelvePage(Player player) {
		// Intro item
		{
			ItemStack introItem = createPageIcon(Material.NETHERITE_BLOCK, "Dungeon Delves", DELVE_COLOR, DELVE_INTRO);
			mInventory.setItem(INTRO_LOC, introItem);
		}

		// Skin items
		{
			int numPages = (DELVE_THEME.size() - 1) / ENTRY_PER_PAGE + 1;
			mPageNumber = Math.min(numPages, Math.max(1, mPageNumber));
			int currentSlot = ENTRY_START;
			// Paging
			List<String> price = List.of(
				PIGMENT_PER_SKIN + " Twisted Pigments and",
				STRAND_PER_DELVE_SKIN + " Twisted Strands");
			for (int i = (mPageNumber - 1) * ENTRY_PER_PAGE; i < DELVE_THEME.size();) {
				String skin = DELVE_THEME.get(i);
				ItemStack item = createSkillIcon(skin, DELVE_COLOR, player, price);
				mInventory.setItem(currentSlot, item);
				if (slotToEntryNum(++currentSlot) < 0) {
					// New line
					currentSlot += (9 - ENTRY_PER_LINE);
				}
				if (++i % ENTRY_PER_PAGE == 0) {
					// End of current page number
					break;
				}
			}

			// Prev and next page buttons
			setPagingItems(numPages);
		}

		// Back item
		setBackItem("Back to Overview");
	}

	private void loadPrestigePage(Player player) {
		// Intro item
		{
			ItemStack introItem = createPageIcon(Material.GOLD_BLOCK, "Prestige Hall", PRESTIGE_COLOR, PRESTIGE_INTRO);
			mInventory.setItem(INTRO_LOC, introItem);
		}

		// Skin items
		{
			int numPages = (PRESTIGE_THEME.size() - 1) / ENTRY_PER_PAGE + 1;
			mPageNumber = Math.min(numPages, Math.max(1, mPageNumber));
			int currentSlot = ENTRY_START;
			// Paging
			for (int i = (mPageNumber - 1) * ENTRY_PER_PAGE; i < PRESTIGE_CS.size(); ) {
				String skin = PRESTIGE_THEME.get(i);
				int priceNum = PRESTIGE_CS.get(i).getPrice();
				List<String> price = List.of(priceNum + " Reverial Roots");
				ItemStack item = createSkillIcon(skin, PRESTIGE_COLOR, player, price);
				mInventory.setItem(currentSlot, item);
				if (slotToEntryNum(++currentSlot) < 0) {
					// New line
					currentSlot += (9 - ENTRY_PER_LINE);
				}
				if (++i % ENTRY_PER_PAGE == 0) {
					// End of current page number
					break;
				}
			}

			// Prev and next page buttons
			setPagingItems(numPages);
		}

		// Back item
		setBackItem("Back to Overview");
	}

	private void loadGalleryPage(Player player) {
		// Intro item
		{
			ItemStack introItem = createPageIcon(Material.WAXED_OXIDIZED_COPPER, "Gallery of Fear", GALLERY_COLOR, GALLERY_INTRO);
			mInventory.setItem(INTRO_LOC, introItem);
		}

		// Skin items
		{
			int numPages = (GALLERY_THEME.size() - 1) / ENTRY_PER_PAGE + 1;
			mPageNumber = Math.min(numPages, Math.max(1, mPageNumber));
			int currentSlot = ENTRY_START;
			// Paging
			List<String> price = List.of(
				PIGMENT_PER_SKIN + " Twisted Pigments and",
				CANVAS_PER_GALLERY_SKIN + " Torn Canvases");
			for (int i = (mPageNumber - 1) * ENTRY_PER_PAGE; i < GALLERY_THEME.size();) {
				String skin = GALLERY_THEME.get(i);
				ItemStack item = createSkillIcon(skin, GALLERY_CS.get(i).getMap().mColor, player, price);
				mInventory.setItem(currentSlot, item);
				if (slotToEntryNum(++currentSlot) < 0) {
					// New line
					currentSlot += (9 - ENTRY_PER_LINE);
				}
				if (++i % ENTRY_PER_PAGE == 0) {
					// End of current page number
					break;
				}
			}

			// Prev and next page buttons
			setPagingItems(numPages);
		}

		// Back item
		setBackItem("Back to Overview");
	}

	private void setPagingItems(int numPages) {
		if (mPageNumber > 1) {
			// Display prev page
			ItemStack pageItem = new ItemStack(Material.ARROW, 1);
			ItemMeta meta = pageItem.getItemMeta();
			meta.displayName(Component.text("Previous Page", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			pageItem.setItemMeta(meta);
			mInventory.setItem(PREV_PAGE_LOC, pageItem);
		}
		if (mPageNumber < numPages) {
			// Display next page
			ItemStack pageItem = new ItemStack(Material.ARROW, 1);
			ItemMeta meta = pageItem.getItemMeta();
			meta.displayName(Component.text("Next Page", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			pageItem.setItemMeta(meta);
			mInventory.setItem(NEXT_PAGE_LOC, pageItem);
		}
	}

	private void setBackItem(String itemName) {
		ItemStack item = new ItemStack(Material.REDSTONE_BLOCK, 1);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text(itemName, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		item.setItemMeta(meta);
		mInventory.setItem(BACK_LOC, item);
	}

	private ItemStack createPageIcon(Material icon, String name, TextColor color, List<String> desc) {
		return createBasicItem(icon, name, color, true, desc);
	}

	private ItemStack createSkillIcon(String skin, TextColor color, Player player, List<String> price) {
		CosmeticSkill skill = CosmeticSkills.getCosmeticSkill(skin);
		List<String> desc = new ArrayList<>();
		desc.add("Cosmetic " + skill.getAbilityName().getName());
		if (skill instanceof LockableCS && !((LockableCS) skill).isUnlocked(player)) {
			// Locked skin, show lock description to give info
			return createBasicItem(LOCKED, skin, color, true, NamedTextColor.YELLOW, desc, NamedTextColor.RED, ((LockableCS) skill).getLockDesc());
		}

		Cosmetic cosmetic = CosmeticSkills.getCosmeticByName(skin);
		String[] extraLore = new String[0];
		// Unlocked skin, show extra lore
		if (cosmetic != null && cosmetic.getDescription() != null) {
			extraLore = cosmetic.getDescription();
		}
		if (CosmeticsManager.getInstance().playerHasCosmetic(player, CosmeticType.COSMETIC_SKILL, skill.getCosmetic().getName())) {
			// attach
			desc.add("Owned");
		} else {
			// attach price
			desc.addAll(price);
		}
		return createBasicItem(skill.getDisplayItem(), skin, color, true, desc, extraLore);
	}

	private ItemStack createBasicItem(Material mat, String name, TextColor nameColor, boolean nameBold, List<String> desc, String... extraLore) {
		return createBasicItem(mat, name, nameColor, nameBold, NamedTextColor.YELLOW, desc, NamedTextColor.DARK_GRAY, extraLore);
	}

	private ItemStack createBasicItem(Material mat, String name, TextColor nameColor, boolean nameBold, TextColor descColor, List<String> desc, TextColor extraColor, String... extraLore) {
		ItemStack item = new ItemStack(mat, 1);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text(name, nameColor)
			.decoration(TextDecoration.ITALIC, false)
			.decoration(TextDecoration.BOLD, nameBold));
		List<Component> lore = new ArrayList<>();
		for (String s : desc) {
			lore.add(Component.text(s, descColor).decoration(TextDecoration.ITALIC, false));
		}
		for (String s : extraLore) {
			lore.add(Component.text(s, extraColor).decoration(TextDecoration.ITALIC, false));
		}
		meta.lore(lore);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		item.setItemMeta(meta);
		return item;
	}

	//May fail if changed relative constants
	private int slotToEntryNum(int slot) {
		int line = slot / 9 + 1;
		if (line <= 1 || line >= LINE) {
			return -1;
		}
		int temp = slot - 9 * (line - 2);
		if (temp >= ENTRY_START && temp < ENTRY_START + ENTRY_PER_LINE) {
			return temp - ENTRY_START + ENTRY_PER_LINE * (line - 2) + ENTRY_PER_PAGE * (mPageNumber - 1);
		}
		return -1;
	}
}
