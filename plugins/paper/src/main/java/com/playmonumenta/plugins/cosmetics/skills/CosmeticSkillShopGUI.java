package com.playmonumenta.plugins.cosmetics.skills;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.cosmetics.gui.CosmeticsGUI;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
	private static final String CHALLENGE_POINTS_SCOREBOARD = "ChallengePoints";

	//Theme constants
	//Depths
	private static final List<TextComponent> DEPTH_INTRO;

	private static final ImmutableList<String> DEPTH_THEME = CosmeticSkills.getDepthsNames();
	private static final ImmutableList<DepthsCS> DEPTHS_CS = CosmeticSkills.getDepthsSkins();
	//Delves
	private static final List<TextComponent> DELVE_INTRO;

	private static final ImmutableList<String> DELVE_THEME = CosmeticSkills.getDelvesNames();
	//Prestige
	private static final List<TextComponent> PRESTIGE_INTRO;


	private static final ImmutableList<String> PRESTIGE_THEME = CosmeticSkills.getPrestigeNames();
	private static final ImmutableList<PrestigeCS> PRESTIGE_CS = CosmeticSkills.getPrestigeSkins();
	//Sanguine
	private static final List<TextComponent> GALLERY_INTRO;


	private static final ImmutableList<String> GALLERY_THEME = CosmeticSkills.getGalleryNames();
	private static final ImmutableList<GalleryCS> GALLERY_CS = CosmeticSkills.getGallerySkins();

	//GUI constants
	private static final Material FILLER = GUIUtils.FILLER_MATERIAL;
	private static final Material LOCKED = Material.BARRIER;
	private static final int LINE = 5;
	private static final int INTRO_LOC = 4;
	private static final int ENTRY_START = 9;
	private static final int ENTRY_PER_LINE = 8;
	private static final int ENTRY_PER_PAGE = ENTRY_PER_LINE * (LINE - 2);
	private static final int[] ENTRY_COLUMNS = {0, 1, 2, 3, 5, 6, 7, 8};
	private static final int BACK_LOC = (LINE - 1) * 9 + 4;
	private static final int PREV_PAGE_LOC = (LINE - 1) * 9;
	private static final int NEXT_PAGE_LOC = (LINE - 1) * 9 + 8;

	private static final String R1MONUMENT_SCB = "R1Complete";
	private static final String DEPTHS_SCB = "Depths";
	private static final String GALLERY_SCB = "DGLobby";
	private static final TextColor DEPTH_COLOR = TextColor.fromHexString("#5D2D87");
	private static final TextColor DELVE_COLOR = TextColor.fromHexString("#B47028");
	public static final TextColor PRESTIGE_COLOR = TextColor.fromHexString("#FEDC10");
	private static final TextColor GALLERY_COLOR = TextColor.fromHexString("#39B14E");
	private static final int DEPTH_ENTRY_LOC = 20;
	private static final int DELVE_ENTRY_LOC = 21;
	private static final int PRESTIGE_ENTRY_LOC = 22;
	private static final int GALLERY_ENTRY_LOC = 23;

	private final Plugin mPlugin;
	private CSGUIPage mCurrentPage = CSGUIPage.HOME;
	private int mPageNumber = 1;
	private boolean mCosmeticSkillChanged = false;

	private enum CSGUIPage {
		HOME,
		DEPTHS,
		DELVE,
		PRESTIGE,
		SANGUINE,
		OTHER
	}

	public CosmeticSkillShopGUI(Plugin plugin, Player player) {
		super(player, 9 * LINE, Component.text("Cosmetic Skill Shop", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		mPlugin = plugin;
		loadPage(CSGUIPage.HOME, player);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);
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
				player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 1);
				player.sendMessage(Component.text("You don't match the requirement to unlock this content!", NamedTextColor.RED));
				return;
			}

			// General: if not home page, back to shop home
			if (slot == BACK_LOC) {
				if (mCurrentPage != CSGUIPage.HOME) {
					loadPage(CSGUIPage.HOME, player);
				} else {
					close();
					new CosmeticsGUI(mPlugin, player).openInventory(player, mPlugin);
				}
				return;
			}
			switch (mCurrentPage) {
				case DEPTHS -> {
					int entry = slotToEntryNum(slot);

					// Clicked on a cosmetic. Check for buying
					if (entry >= 0 && entry < DEPTH_THEME.size()) {
						String skin = DEPTH_THEME.get(entry);
						if (!CosmeticsManager.getInstance().playerHasCosmetic(player, CosmeticType.COSMETIC_SKILL, skin)) {
							// Try to buy
							if (player.getGameMode() == GameMode.CREATIVE) {
								buyCosmetic(player, skin);
								player.sendMessage(Component.text("Because you are in creative mode, this is free!", NamedTextColor.GREEN));
								return;
							}

							boolean isExtraGeodes = DEPTHS_CS.get(entry).getToken().equals(DepthsCS.EXTRA_GEODES);
							if (isExtraGeodes) {
								// Check costs
								ItemStack mGeode = InventoryUtils.getItemFromLootTable(player, GEODE_LOOTTABLE);
								ItemStack mPigment = InventoryUtils.getItemFromLootTable(player, PIGMENT_LOOTTABLE);
								if (!player.getInventory().containsAtLeast(mPigment, PIGMENT_PER_SKIN) ||
									!player.getInventory().containsAtLeast(mGeode, GEODE_PER_DEPTH_SKIN + 64)) {
									player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 1);
									player.sendMessage(Component.text("You don't have enough items to buy this cosmetic skill!", NamedTextColor.RED));
									return;
								}
								// Remove items
								mGeode.setAmount(GEODE_PER_DEPTH_SKIN + 64);
								mPigment.setAmount(PIGMENT_PER_SKIN);
								if (buyCosmetic(player, skin)) {
									player.getInventory().removeItem(mGeode);
									player.getInventory().removeItem(mPigment);
								}
							} else {
								NamespacedKey talismanLoot = NamespacedKeyUtils.fromString(TALISMAN_LOOTTABLE_FOLDER + DEPTHS_CS.get(entry).getToken());
								int talismanPerDepthSkin = TALISMAN_PER_DEPTH_SKIN;
								if (talismanLoot == null) {
									// Shouldn't be here! But leave it as a handler to avoid typo in code.
									player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 0.5f);
									player.sendMessage(Component.text("EX[" + skin + "]1: An exception occurred when buying cosmetic skill. Contact a moder or dev with this message to report if you believe this is a bug.", NamedTextColor.DARK_RED));
									close();
									return;
								}
								// Check costs
								ItemStack mGeode = InventoryUtils.getItemFromLootTable(player, GEODE_LOOTTABLE);
								ItemStack mPigment = InventoryUtils.getItemFromLootTable(player, PIGMENT_LOOTTABLE);
								ItemStack mTalisman = InventoryUtils.getItemFromLootTable(player, talismanLoot);
								if (!player.getInventory().containsAtLeast(mPigment, PIGMENT_PER_SKIN) ||
									!player.getInventory().containsAtLeast(mTalisman, talismanPerDepthSkin) ||
									!player.getInventory().containsAtLeast(mGeode, GEODE_PER_DEPTH_SKIN)) {
									player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 1);
									player.sendMessage(Component.text("You don't have enough items to buy this cosmetic skill!", NamedTextColor.RED));
									return;
								}
								// Remove items
								mGeode.setAmount(GEODE_PER_DEPTH_SKIN);
								mPigment.setAmount(PIGMENT_PER_SKIN);
								mTalisman.setAmount(talismanPerDepthSkin);
								if (buyCosmetic(player, skin)) {
									player.getInventory().removeItem(mGeode);
									player.getInventory().removeItem(mPigment);
									player.getInventory().removeItem(mTalisman);
								}
							}
							return;

						} else {
							// Already bought
							player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 1);
							player.sendMessage(Component.text("You already have this cosmetic skill. Go to Cosmetic Manager to equip it!", NamedTextColor.RED));
							return;
						}
					}

					// Changing page
					if (slot == PREV_PAGE_LOC) {
						mPageNumber--;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.5f, 1f);
						loadDepthPage(player);
					} else if (slot == NEXT_PAGE_LOC) {
						mPageNumber++;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.5f, 1f);
						loadDepthPage(player);
					}
				}
				case DELVE -> {
					int entry = slotToEntryNum(slot);

					// Clicked on a cosmetic. Check for buying
					if (entry >= 0 && entry < DELVE_THEME.size()) {
						String skin = DELVE_THEME.get(entry);
						if (!CosmeticsManager.getInstance().playerHasCosmetic(player, CosmeticType.COSMETIC_SKILL, skin)) {
							// Try to buy
							if (player.getGameMode() == GameMode.CREATIVE) {
								buyCosmetic(player, skin);
								player.sendMessage(Component.text("Because you are in creative mode, this is free!", NamedTextColor.GREEN));
								return;
							}

							// Check costs
							ItemStack mStrand = InventoryUtils.getItemFromLootTable(player, STRAND_LOOTTABLE);
							ItemStack mPigment = InventoryUtils.getItemFromLootTable(player, PIGMENT_LOOTTABLE);
							if (!player.getInventory().containsAtLeast(mPigment, PIGMENT_PER_SKIN) ||
									!player.getInventory().containsAtLeast(mStrand, STRAND_PER_DELVE_SKIN)) {
								player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 1);
								player.sendMessage(Component.text("You don't have enough items to buy this cosmetic skill!", NamedTextColor.RED));
								return;
							}
							// Remove items
							mStrand.setAmount(STRAND_PER_DELVE_SKIN);
							mPigment.setAmount(PIGMENT_PER_SKIN);
							if (buyCosmetic(player, skin)) {
								player.getInventory().removeItem(mStrand);
								player.getInventory().removeItem(mPigment);
							}
							return;
						} else {
							// Already bought
							player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 1);
							player.sendMessage(Component.text("You already have this cosmetic skill. Go to Cosmetic Manager to equip it!", NamedTextColor.RED));
							return;
						}
					}

					// Changing page
					if (slot == PREV_PAGE_LOC) {
						mPageNumber--;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.5f, 1f);
						loadDepthPage(player);
					} else if (slot == NEXT_PAGE_LOC) {
						mPageNumber++;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.5f, 1f);
						loadDepthPage(player);
					}
				}
				case PRESTIGE -> {
					int entry = slotToEntryNum(slot);

					// Clicked on a cosmetic. Check for buying
					if (entry >= 0 && entry < PRESTIGE_THEME.size()) {
						String skin = PRESTIGE_THEME.get(entry);
						if (!CosmeticsManager.getInstance().playerHasCosmetic(player, CosmeticType.COSMETIC_SKILL, skin)) {
							// Try to buy
							if (player.getGameMode() == GameMode.CREATIVE) {
								buyCosmetic(player, skin);
								player.sendMessage(Component.text("Because you are in creative mode, this is free!", NamedTextColor.GREEN));
								return;
							}

							// Check costs
							int score = ScoreboardUtils.getScoreboardValue(player, CHALLENGE_POINTS_SCOREBOARD).orElse(0);
							int priceNum = PRESTIGE_CS.get(entry).getPrice();
							if (score < priceNum) {
								player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 1);
								player.sendMessage(Component.text("You don't have enough Challenge Points to buy this cosmetic skill!", NamedTextColor.RED));
							} else {
								if (buyCosmetic(player, skin)) {
									ScoreboardUtils.setScoreboardValue(player, CHALLENGE_POINTS_SCOREBOARD, score - priceNum);
									reloadPage(player);
								}
							}
						} else {
							// Already bought
							player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 1);
							player.sendMessage(Component.text("You already have this cosmetic skill. Go to Cosmetic Manager to equip it!", NamedTextColor.RED));
						}
						return;
					}

					// Changing page
					if (slot == PREV_PAGE_LOC) {
						mPageNumber--;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.5f, 1f);
						loadDepthPage(player);
					} else if (slot == NEXT_PAGE_LOC) {
						mPageNumber++;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.5f, 1f);
						loadDepthPage(player);
					}
				}
				case SANGUINE -> {
					int entry = slotToEntryNum(slot);

					// Clicked on a cosmetic. Check for buying
					if (entry >= 0 && entry < GALLERY_THEME.size()) {
						String skin = GALLERY_THEME.get(entry);
						if (!CosmeticsManager.getInstance().playerHasCosmetic(player, CosmeticType.COSMETIC_SKILL, skin)) {
							// Try to buy
							if (player.getGameMode() == GameMode.CREATIVE) {
								buyCosmetic(player, skin);
								player.sendMessage(Component.text("Because you are in creative mode, this is free!", NamedTextColor.GREEN));
								return;
							}

							// Check costs
							ItemStack mPigment = InventoryUtils.getItemFromLootTable(player, PIGMENT_LOOTTABLE);
							ItemStack mCanvas = InventoryUtils.getItemFromLootTable(player, CANVAS_LOOTTABLE);
							if (!player.getInventory().containsAtLeast(mPigment, PIGMENT_PER_SKIN) ||
								!player.getInventory().containsAtLeast(mCanvas, CANVAS_PER_GALLERY_SKIN)) {
								player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 1);
								player.sendMessage(Component.text("You don't have enough items to buy this cosmetic skill!", NamedTextColor.RED));
								return;
							}
							// Remove items
							mPigment.setAmount(PIGMENT_PER_SKIN);
							mCanvas.setAmount(CANVAS_PER_GALLERY_SKIN);
							if (buyCosmetic(player, skin)) {
								player.getInventory().removeItem(mPigment);
								player.getInventory().removeItem(mCanvas);
							}
							return;
						} else {
							// Already bought
							player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 1);
							player.sendMessage(Component.text("You already have this cosmetic skill. Go to Cosmetic Manager to equip it!", NamedTextColor.RED));
							return;
						}
					}

					// Changing page
					if (slot == PREV_PAGE_LOC) {
						mPageNumber--;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.5f, 1f);
						loadGalleryPage(player);
					} else if (slot == NEXT_PAGE_LOC) {
						mPageNumber++;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.5f, 1f);
						loadGalleryPage(player);
					}
				}
				default -> {
					//Reject: related content not discovered
					if (item.getType() == LOCKED) {
						return;
					}

					//Home page, choose skin set
					if (slot == DEPTH_ENTRY_LOC) {
						mPageNumber = 1;
						loadPage(CSGUIPage.DEPTHS, player);
					} else if (slot == DELVE_ENTRY_LOC) {
						mPageNumber = 1;
						loadPage(CSGUIPage.DELVE, player);
					} else if (slot == PRESTIGE_ENTRY_LOC) {
						mPageNumber = 1;
						loadPage(CSGUIPage.PRESTIGE, player);
					} else if (slot == GALLERY_ENTRY_LOC) {
						mPageNumber = 1;
						loadPage(CSGUIPage.SANGUINE, player);
					}
				}
			}
		}
	}

	private boolean buyCosmetic(Player player, String skin) {
		if (CosmeticsManager.getInstance().addCosmetic(player, CosmeticType.COSMETIC_SKILL, skin, true)) {
			player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1, 1.5f);
			player.sendMessage(Component.text("You successfully bought " + skin + "! It has been automatically equipped.", NamedTextColor.GREEN));
			reloadPage(player);
			mCosmeticSkillChanged = true;
			return true;
		} else {
			// Shouldn't be here! But leave it as a handler to avoid typo in code.
			player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 0.5f);
			player.sendMessage(Component.text("EX[" + skin + "]2: An exception occurred when buying cosmetic skill. Contact a moder or dev with this message to report if you believe this is a bug.", NamedTextColor.DARK_RED));
			close();
		}
		return false;
	}

	private void reloadPage(Player player) {
		loadPage(mCurrentPage, player);
	}

	private void loadPage(CSGUIPage page, Player player) {
		mCurrentPage = page;
		player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.5f, 1f);
		// Set filler to start
		GUIUtils.fillWithFiller(mInventory, true);

		switch (mCurrentPage) {
			case DEPTHS -> loadDepthPage(player);
			case DELVE -> loadDelvePage(player);
			case PRESTIGE -> loadPrestigePage(player);
			case SANGUINE -> loadGalleryPage(player);
			default -> {
				// Intro item
				ItemStack introItem = createPageIcon(Material.RED_GLAZED_TERRACOTTA, Component.text("Theme Selection", NamedTextColor.RED), List.of(Component.text("Select a theme to buy", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false), Component.text("cosmetic skills!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
				mInventory.setItem(INTRO_LOC, introItem);

				// Depth theme entry
				setPageIcon(DEPTH_ENTRY_LOC, Material.BLACKSTONE, Component.text("Darkest Depths", DEPTH_COLOR), Component.text("Da").append(Component.text("rkest Dep").decorate(TextDecoration.OBFUSCATED)).append(Component.text("ths")), DEPTH_INTRO, List.of(Component.text("Complete ").append(Component.text("Darke").decorate(TextDecoration.OBFUSCATED)).append(Component.text("st D").append(Component.text("epth").decorate(TextDecoration.OBFUSCATED).append(Component.text("s")))), Component.text("to unlock this theme!")), DEPTHS_SCB, player);

				// Delve theme entry
				setPageIcon(DELVE_ENTRY_LOC, Material.NETHERITE_BLOCK, Component.text("Dungeon Delves", DELVE_COLOR), Component.text("Dungeon D", DELVE_COLOR).decorate(TextDecoration.OBFUSCATED).append(Component.text("elves", DELVE_COLOR)), DELVE_INTRO, List.of(Component.text("Complete Monument of King's Valley"), Component.text("to unlock this theme!")), R1MONUMENT_SCB, player);

				// Prestige theme entry
				setPageIcon(PRESTIGE_ENTRY_LOC, Material.GOLD_BLOCK, Component.text("Challenge Delves", PRESTIGE_COLOR), Component.text("Chall", PRESTIGE_COLOR).decorate(TextDecoration.OBFUSCATED).append(Component.text("enge Delves", PRESTIGE_COLOR)), PRESTIGE_INTRO, List.of(Component.text("Complete Monument of King's Valley"), Component.text("to unlock this theme!")), R1MONUMENT_SCB, player);

				// Gallery theme entry
				setPageIcon(GALLERY_ENTRY_LOC, Material.WAXED_OXIDIZED_COPPER, Component.text("Gallery of Fear", GALLERY_COLOR), Component.text("MzkCaerulaArbor", GALLERY_COLOR).decorate(TextDecoration.OBFUSCATED), GALLERY_INTRO, List.of(Component.text("Reveal the secret b").append(Component.text("eneth the ocean").decorate(TextDecoration.OBFUSCATED)).append(Component.text("s")), Component.text("to unlock this theme!")), GALLERY_SCB, player);

				// Back item
				setBackItem("Back to Cosmetic Manager");
			}
		}
	}

	private void loadDepthPage(Player player) {
		// Intro item
		ItemStack introItem = createPageIcon(Material.BLACKSTONE,
				Component.text("Darkest Depths", DEPTH_COLOR), DEPTH_INTRO);
		mInventory.setItem(INTRO_LOC, introItem);

		// Skin items
		int numPages = (DEPTH_THEME.size() - 1) / ENTRY_PER_PAGE + 1;
		mPageNumber = Math.min(numPages, Math.max(1, mPageNumber));
		// Paging
		for (int i = (mPageNumber - 1) * ENTRY_PER_PAGE; i < DEPTHS_CS.size(); ) {
			int slot = ENTRY_START + ENTRY_COLUMNS[i % ENTRY_PER_LINE] + i / ENTRY_PER_LINE;
			String skin = DEPTH_THEME.get(i);
			boolean isExtraGeodes = DEPTHS_CS.get(i).getToken().equals(DepthsCS.EXTRA_GEODES);
			if (isExtraGeodes) {
				List<String> price = List.of(
					PIGMENT_PER_SKIN + " Twisted Pigments and",
					GEODE_PER_DEPTH_SKIN + 64 + " Voidstained Geodes");
				ItemStack item = createSkillIcon(skin, DEPTH_COLOR, player, price);
				mInventory.setItem(slot, item);
			} else {
				String talismanName = StringUtils.capitalizeWords(DEPTHS_CS.get(i).getToken().replace('_', ' '));
				List<String> price = List.of(
					PIGMENT_PER_SKIN + " Twisted Pigments,",
					TALISMAN_PER_DEPTH_SKIN + " " + talismanName + " and",
					GEODE_PER_DEPTH_SKIN + " Voidstained Geodes");
				ItemStack item = createSkillIcon(skin, DEPTH_COLOR, player, price);
				mInventory.setItem(slot, item);
			}

			if (++i % ENTRY_PER_PAGE == 0) {
				// End of current page number
				break;
			}
		}

		// Prev and next page buttons
		setPagingItems(numPages);

		// Back item
		setBackItem("Back to Overview");
	}

	private void loadDelvePage(Player player) {
		// Intro item
		ItemStack introItem = createPageIcon(Material.NETHERITE_BLOCK,
				Component.text("Dungeon Delves", DELVE_COLOR), DELVE_INTRO);
		mInventory.setItem(INTRO_LOC, introItem);

		// Skin items
		int numPages = (DELVE_THEME.size() - 1) / ENTRY_PER_PAGE + 1;
		mPageNumber = Math.min(numPages, Math.max(1, mPageNumber));
		// Paging
		List<String> price = List.of(
				PIGMENT_PER_SKIN + " Twisted Pigments and",
				STRAND_PER_DELVE_SKIN + " Twisted Strands");
		for (int i = (mPageNumber - 1) * ENTRY_PER_PAGE; i < DELVE_THEME.size(); ) {
			int slot = ENTRY_START + ENTRY_COLUMNS[i % ENTRY_PER_LINE] + i / ENTRY_PER_LINE;
			String skin = DELVE_THEME.get(i);
			ItemStack item = createSkillIcon(skin, DELVE_COLOR, player, price);
			mInventory.setItem(slot, item);

			if (++i % ENTRY_PER_PAGE == 0) {
				// End of current page number
				break;
			}
		}

		// Prev and next page buttons
		setPagingItems(numPages);

		// Back item
		setBackItem("Back to Overview");
	}

	private void loadPrestigePage(Player player) {
		// Intro item
		List<TextComponent> desc = new ArrayList<>(PRESTIGE_INTRO);
		desc.add(Component.text("Challenge Points: " + ScoreboardUtils.getScoreboardValue(player, CHALLENGE_POINTS_SCOREBOARD).orElse(0)));
		ItemStack introItem = createPageIcon(Material.GOLD_BLOCK, Component.text("Prestige Hall", PRESTIGE_COLOR), desc);
		mInventory.setItem(INTRO_LOC, introItem);

		// Skin items
		int numPages = (PRESTIGE_THEME.size() - 1) / ENTRY_PER_PAGE + 1;
		mPageNumber = Math.min(numPages, Math.max(1, mPageNumber));
		// Paging
		for (int i = (mPageNumber - 1) * ENTRY_PER_PAGE; i < PRESTIGE_THEME.size();) {
			int iMod = i % ENTRY_PER_PAGE;
			int slot = ENTRY_START + iMod + (iMod + 4) / 8;
			String skin = PRESTIGE_THEME.get(i);
			int priceNum = PRESTIGE_CS.get(i).getPrice();
			List<String> price = List.of(priceNum + " Challenge Points");
			ItemStack item = createSkillIcon(skin, PRESTIGE_COLOR, player, price);
			mInventory.setItem(slot, item);

			if (++i % ENTRY_PER_PAGE == 0) {
				// End of current page number
				break;
			}
		}

		// Prev and next page buttons
		setPagingItems(numPages);

		// Back item
		setBackItem("Back to Overview");
	}

	private void loadGalleryPage(Player player) {
		// Intro item
		ItemStack introItem = createPageIcon(Material.WAXED_OXIDIZED_COPPER,
			Component.text("Gallery of Fear", GALLERY_COLOR), GALLERY_INTRO);
		mInventory.setItem(INTRO_LOC, introItem);

		// Skin items
		int numPages = (GALLERY_THEME.size() - 1) / ENTRY_PER_PAGE + 1;
		mPageNumber = Math.min(numPages, Math.max(1, mPageNumber));
		// Paging
		List<String> price = List.of(
			PIGMENT_PER_SKIN + " Twisted Pigments and",
			CANVAS_PER_GALLERY_SKIN + " Torn Canvases");
		for (int i = (mPageNumber - 1) * ENTRY_PER_PAGE; i < GALLERY_THEME.size(); ) {
			int slot = ENTRY_START + ENTRY_COLUMNS[i % ENTRY_PER_LINE] + i / ENTRY_PER_LINE;
			String skin = GALLERY_THEME.get(i);
			ItemStack item = createSkillIcon(skin, GALLERY_CS.get(i).getMap().mColor, player, price);
			mInventory.setItem(slot, item);

			if (++i % ENTRY_PER_PAGE == 0) {
				// End of current page number
				break;
			}
		}

		// Prev and next page buttons
		setPagingItems(numPages);

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

	private void setPageIcon(int location, Material unlockedIcon, Component unlockedName, Component lockedName, List<TextComponent> unlockedLore, List<TextComponent> lockedLore, String scoreboard, Player player) {
		ItemStack item;
		if (ScoreboardUtils.getScoreboardValue(player, scoreboard).orElse(0) > 0 || player.getGameMode() == GameMode.CREATIVE) {
			item = createPageIcon(unlockedIcon, unlockedName, unlockedLore);
		} else {
			item = createPageIcon(LOCKED, lockedName, lockedLore);
		}
		mInventory.setItem(location, item);
	}

	private ItemStack createPageIcon(Material icon, Component name, List<TextComponent> desc) {
		return createBasicItem(icon, name, desc);
	}

	private ItemStack createSkillIcon(String skin, TextColor color, Player player, List<String> price) {
		CosmeticSkill skill = CosmeticSkills.getCosmeticSkill(skin);
		if (skill == null) {
			return new ItemStack(Material.BARRIER);
		}
		List<String> desc = new ArrayList<>();
		desc.add("Cosmetic " + skill.getAbility().getName());
		if (skill instanceof LockableCS lockable && !lockable.isUnlocked(player)) {
			// Locked skin, show lock description to give info
			return createBasicItem(LOCKED, skin, color, desc, NamedTextColor.RED, lockable.getLockDesc());
		}

		Cosmetic cosmetic = CosmeticSkills.getCosmeticByName(skin);
		String[] extraLore = new String[0];
		// Unlocked skin, show extra lore
		if (cosmetic != null && cosmetic.getDescription() != null) {
			extraLore = cosmetic.getDescription();
		}
		Cosmetic skillCosmetic = skill.getCosmetic();
		if (skillCosmetic != null && CosmeticsManager.getInstance().playerHasCosmetic(player, CosmeticType.COSMETIC_SKILL, skillCosmetic.getName())) {
			// attach
			desc.add("Owned");
		} else {
			// attach price
			desc.addAll(price);
		}
		return createBasicItem(skill.getDisplayItem(), skin, color, desc, extraLore);
	}

	private ItemStack createBasicItem(Material mat, String name, TextColor nameColor, List<String> desc, String... extraLore) {
		return createBasicItem(mat, name, nameColor, desc, NamedTextColor.DARK_GRAY, extraLore);
	}

	private ItemStack createBasicItem(Material mat, String name, TextColor nameColor, List<String> desc, TextColor extraColor, String... extraLore) {
		return createBasicItem(mat, Component.text(name, nameColor), desc, extraColor, extraLore);
	}

	private ItemStack createBasicItem(Material mat, Component name, List<String> desc, TextColor extraColor, String... extraLore) {
		return createBasicItem(mat, name,
				desc.stream().map(s -> Component.text(s, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)).toList(),
				Arrays.stream(extraLore).map(s -> Component.text(s, extraColor).decoration(TextDecoration.ITALIC, false)).toList().toArray(new TextComponent[0]));
	}

	private ItemStack createBasicItem(Material mat, Component name, List<TextComponent> desc, Component... extraLore) {
		ItemStack item = new ItemStack(mat, 1);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(name
				.decoration(TextDecoration.ITALIC, false)
				.decoration(TextDecoration.BOLD, true));
		List<Component> lore = new ArrayList<>(desc);
		lore.addAll(List.of(extraLore));
		meta.lore(lore);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
		item.setItemMeta(meta);
		return item;
	}

	//May fail if changed relative constants

	private int slotToEntryNum(int slot) {
		int line = slot / 9 + 1;
		if (line <= 1 || line >= LINE || (slot % 9 + 5) % 9 == 0) {
			return -1;
		}
		int temp = slot - 9 * (line - 2) - ((slot % 9 + 5) / 9);
		if (temp >= ENTRY_START && temp <= ENTRY_START + ENTRY_PER_LINE) {
			return temp - ENTRY_START + ENTRY_PER_LINE * (line - 2) + ENTRY_PER_PAGE * (mPageNumber - 1);
		}
		return -1;
	}

	@Override
	protected void inventoryClose(InventoryCloseEvent event) {
		if (event.getPlayer() instanceof Player player) {
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				if (mCosmeticSkillChanged) {
					mPlugin.mAbilityManager.updatePlayerAbilities(player, false);
				}
			}, 2);
		}
	}

	private static final Style INTRO_STYLE = Style.style(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false);

	/**
	 * "Styles" a list of components for use in the intros.
	 *
	 * @param components The list of components.
	 * @return The styled components.
	 */
	private static List<TextComponent> applyIntroStyle(String... components) {
		List<TextComponent> comps = new ArrayList<>();
		for (String s : components) {
			comps.add(Component.text(s, INTRO_STYLE));
		}
		return ImmutableList.copyOf(comps);
	}

	static {
		DEPTH_INTRO = applyIntroStyle(
				"Attuned with powers from",
				"Darkest Depths trees."
		);
		DELVE_INTRO = applyIntroStyle(
				"Essences of the twisted contingency,",
				"rewards for heroic adventurers."
		);
		PRESTIGE_INTRO = applyIntroStyle(
				"Every single step here is",
				"a witness of your prestige and glory."
		);
		GALLERY_INTRO = applyIntroStyle(
			"The nightmare was never meant for life.",
			"Banish the dream.",
			"End this nightmare!"
		);

	}


}
