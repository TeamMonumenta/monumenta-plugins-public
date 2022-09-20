package com.playmonumenta.plugins.cosmetics.skills;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsGUI;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.SunriseBrewCS;
import com.playmonumenta.plugins.cosmetics.skills.cleric.DarkPunishmentCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.VolcanicBurstCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.WindStepCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.FireworkStrikeCS;
import com.playmonumenta.plugins.cosmetics.skills.warlock.AvalanchexCS;
import com.playmonumenta.plugins.cosmetics.skills.warrior.BrambleShellCS;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.scriptedquests.internal.com.google.common.collect.ImmutableList;
import com.playmonumenta.scriptedquests.internal.com.google.common.collect.ImmutableMap;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CosmeticSkillShopGUI extends CustomInventory {
	//Price constants
	private static final int PIGMENT_PER_SKIN = 3;
	private static final int TALISMAN_PER_DEPTH_SKIN = 1;
	private static final int GEODE_PER_DEPTH_SKIN = 2 * 64;
	private static final int STRAND_PER_DELVE_SKIN = 64;
	private static final NamespacedKey PIGMENT_LOOTTABLE = NamespacedKeyUtils.fromString("epic:r2/delves/items/twisted_pigment");
	private static final NamespacedKey GEODE_LOOTTABLE = NamespacedKeyUtils.fromString("epic:r2/depths/loot/voidstained_geode");
	private static final NamespacedKey STRAND_LOOTTABLE = NamespacedKeyUtils.fromString("epic:r2/delves/items/twisted_strand");
	//Talisman item paths
	private static final String TALISMAN_LOOTTABLE_FOLDER = "epic:r2/depths/utility/";
	private static final String TALISMAN_FLAME = "flamecaller_talisman";
	private static final String TALISMAN_FROST = "frostborn_talisman";
	private static final String TALISMAN_WIND = "windwalker_talisman";
	private static final String TALISMAN_EARTH = "earthbound_talisman";
	private static final String TALISMAN_DAWN = "dawnbringer_talisman";
	private static final String TALISMAN_SHADOW = "shadowdancer_talisman";
	private static final String TALISMAN_STEEL = "steelsage_talisman";
	//Theme constants
	private static final List<String> DEPTH_INTRO = List.of("Attuned with powers from", "Darkest Depths trees.");
	private static final ImmutableList<String> DEPTH_THEME = CosmeticSkills.getDepthNames();
	private static final ImmutableMap<String, String> DEPTH_TOKEN =
		ImmutableMap.<String, String>builder()
			.put(SunriseBrewCS.NAME, TALISMAN_DAWN)
			.put(DarkPunishmentCS.NAME, TALISMAN_SHADOW)
			.put(VolcanicBurstCS.NAME, TALISMAN_FLAME)
			.put(WindStepCS.NAME, TALISMAN_WIND)
			.put(FireworkStrikeCS.NAME, TALISMAN_STEEL)
			.put(AvalanchexCS.NAME, TALISMAN_FROST)
			.put(BrambleShellCS.NAME, TALISMAN_EARTH)
			.build();
	private static final List<String> DELVE_INTRO = List.of("Essences of the twisted contingency,", "rewards for heroic adventurers.");
	private static final ImmutableList<String> DELVE_THEME = CosmeticSkills.getDelveNames();

	//GUI constants
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private static final int LINE = 5;
	private static final int INTRO_LOC = 4;
	private static final int ENTRY_START = 10;
	private static final int ENTRY_PER_LINE = 7;
	private static final int ENTRY_PER_PAGE = ENTRY_PER_LINE * (LINE - 2);
	private static final int BACK_LOC = (LINE - 1) * 9 + 4;
	private static final int PREV_PAGE_LOC = (LINE - 1) * 9 + 0;
	private static final int NEXT_PAGE_LOC = (LINE - 1) * 9 + 8;
	private static final NamedTextColor DEPTH_COLOR = NamedTextColor.DARK_PURPLE;
	private static final NamedTextColor DELVE_COLOR = NamedTextColor.DARK_RED;
	private static final int DEPTH_ENTRY_LOC = 20;
	private static final int DELVE_ENTRY_LOC = 24;
	private static final int PRESTIGE_ENTRY_LOC = 22;
	private static final int GALLERY_ENTRY_LOC = 23;

	private final Plugin mPlugin;
	private CSGUIPage mCurrentPage;
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
		loadPage(0, player);
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
				//Nope
				return;
			}

			// General: if not home page, back to shop home
			if (slot == BACK_LOC) {
				if (mCurrentPage != CSGUIPage.Home) {
					loadPage(0, player);
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

							NamespacedKey talismanLoot = NamespacedKeyUtils.fromString(TALISMAN_LOOTTABLE_FOLDER + DEPTH_TOKEN.getOrDefault(skin, null));
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
				case Prestige:
					break;
				case Gallery:
					break;
				default:
					//Home page, choose skin set
					if (slot == DEPTH_ENTRY_LOC) {
						mPageNumber = 1;
						loadPage(CSGUIPage.Depth.mNum, player);
						return;
					} else if (slot == DELVE_ENTRY_LOC) {
						mPageNumber = 1;
						loadPage(CSGUIPage.Delve.mNum, player);
						return;
					} else if (slot == PRESTIGE_ENTRY_LOC) {
						mPageNumber = 1;
						loadPage(CSGUIPage.Prestige.mNum, player);
						return;
					} else if (slot == GALLERY_ENTRY_LOC) {
						mPageNumber = 1;
						loadPage(CSGUIPage.Gallery.mNum, player);
						return;
					}
			}
		}
	}

	private void reloadPage(Player player) {
		loadPage(mCurrentPage.mNum, player);
	}

	private void loadPage(int pageNum, Player player) {
		mCurrentPage = CSGUIPage.valueOfPage(pageNum) == null ? CSGUIPage.Home : CSGUIPage.valueOfPage(pageNum);
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
				//loadPrestigePage(player);
				close();
				player.sendMessage(Component.text("Not implemented.", NamedTextColor.DARK_RED));
				break;
			case Gallery:
				//loadGalleryPage(player);
				close();
				player.sendMessage(Component.text("Not implemented.", NamedTextColor.DARK_RED));
				break;
			default:
				// Intro item
				ItemStack introItem = createPageIcon(Material.RED_GLAZED_TERRACOTTA, "Theme Selection", NamedTextColor.RED, List.of("Select a theme to buy", "cosmetic skills!"));
				mInventory.setItem(INTRO_LOC, introItem);

				// Depth theme entry
				ItemStack depthItem = createPageIcon(Material.BLACKSTONE, "Darkest Depths", DEPTH_COLOR, DEPTH_INTRO);
				mInventory.setItem(DEPTH_ENTRY_LOC, depthItem);

				// Delve theme entry
				ItemStack delveItem = createPageIcon(Material.NETHERITE_BLOCK, "Dungeon Delves", DELVE_COLOR, DELVE_INTRO);
				mInventory.setItem(DELVE_ENTRY_LOC, delveItem);

				// Prestige theme entry
				final boolean prestige_enable = false;
				if (prestige_enable) {
					ItemStack prestigeItem = createPageIcon(Material.GOLD_BLOCK, "Prestigious Hall", NamedTextColor.GOLD, List.of("Witness of prestige."));
					mInventory.setItem(PRESTIGE_ENTRY_LOC, prestigeItem);
				}

				// Gallery theme entry
				final boolean gallery_enable = false;
				if (gallery_enable) {
					ItemStack galleryItem = createPageIcon(Material.WAXED_OXIDIZED_COPPER, "Gallery of Fear", NamedTextColor.DARK_AQUA, List.of("Essences of fear", "and nightmare."));
					mInventory.setItem(GALLERY_ENTRY_LOC, galleryItem);
				}

			// Back item
			{
				ItemStack item = new ItemStack(Material.REDSTONE_BLOCK, 1);
				ItemMeta meta = item.getItemMeta();
				meta.displayName(Component.text("Back to Cosmetic Manager", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
				item.setItemMeta(meta);
				mInventory.setItem(BACK_LOC, item);
			}
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
			for (int i = (mPageNumber - 1) * ENTRY_PER_PAGE; i < DEPTH_THEME.size(); ) {
				String skin = DEPTH_THEME.get(i);
				String tokenName = DEPTH_TOKEN.getOrDefault(skin, null).replace('_', ' ');
				List<String> price = List.of(
					PIGMENT_PER_SKIN + " Twisted Pigments,",
					TALISMAN_PER_DEPTH_SKIN + " " + StringUtils.capitalizeWords(tokenName) + " and",
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

		// Back item
		{
			ItemStack item = new ItemStack(Material.REDSTONE_BLOCK, 1);
			ItemMeta meta = item.getItemMeta();
			meta.displayName(Component.text("Back to Overview", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			item.setItemMeta(meta);
			mInventory.setItem(BACK_LOC, item);
		}
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
			for (int i = (mPageNumber - 1) * ENTRY_PER_PAGE; i < DELVE_THEME.size();) {
				String skin = DELVE_THEME.get(i);
				List<String> price = List.of(
					PIGMENT_PER_SKIN + " Twisted Pigments and",
					STRAND_PER_DELVE_SKIN + " Twisted Strands");
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

		// Back item
		{
			ItemStack item = new ItemStack(Material.REDSTONE_BLOCK, 1);
			ItemMeta meta = item.getItemMeta();
			meta.displayName(Component.text("Back to Overview", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			item.setItemMeta(meta);
			mInventory.setItem(BACK_LOC, item);
		}
	}

	/*
	private void loadPrestigePage(Player player) {
		//Not implemented
	}

	private void loadGalleryPage(Player player) {
		//Not implemented
	}
	 */

	private ItemStack createPageIcon(Material icon, String name, NamedTextColor color, List<String> desc) {
		return createBasicItem(icon, name, color, true, desc);
	}

	private ItemStack createSkillIcon(String skin, NamedTextColor color, Player player, List<String> price) {
		CosmeticSkill skill = CosmeticSkills.getCosmeticSkill(skin);
		List<String> desc = new ArrayList<>();
		desc.add("Cosmetic " + skill.getAbilityName().getName());
		if (CosmeticsManager.getInstance().playerHasCosmetic(player, CosmeticType.COSMETIC_SKILL, skill.getCosmetic().getName())) {
			// attach
			desc.add("Owned");
		} else {
			// attach price
			desc.addAll(price);
		}
		return createBasicItem(skill.getDisplayItem(), skin, color,
			true, desc);
	}

	private ItemStack createBasicItem(Material mat, String name, NamedTextColor nameColor, boolean nameBold, List<String> desc) {
		ItemStack item = new ItemStack(mat, 1);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text(name, nameColor)
			.decoration(TextDecoration.ITALIC, false)
			.decoration(TextDecoration.BOLD, nameBold));
		List<Component> lore = new ArrayList<>();
		for (String s : desc) {
			lore.add(Component.text(s, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
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
