package com.playmonumenta.plugins.cosmetics;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.classes.PlayerSpec;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkillGUIConfig;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkillShopGUI;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;



public class CosmeticsGUI extends CustomInventory {

	private static final int STRAND_COST_PER_VANITY_UNLOCK = 16;

	private static final int PREV_PAGE_LOC = 45;
	private static final int NEXT_PAGE_LOC = 53;
	private static final int BACK_LOC = 49;
	private static final int TITLE_LOC = 10;
	private static final int ELITE_FINISHER_LOC = 12;
	private static final int COSMETIC_SKILL_LOC = 14;
	private static final int UNLOCKED_VANITY_LOC = 16;
	private static final int SUMMARY_LOC = 0;
	private static final int SKILL_ICON_LOC = 8;
	private static final int COSMETICS_START = 9;
	private static final int COSMETICS_PER_PAGE = 36;

	private static final ImmutableMap<EquipmentSlot, Integer> VANITY_EQUIPMENT_TITLE_SLOTS = ImmutableMap.of(
		EquipmentSlot.HEAD, 30,
		EquipmentSlot.CHEST, 31,
		EquipmentSlot.LEGS, 32,
		EquipmentSlot.FEET, 33,
		EquipmentSlot.OFF_HAND, 34
	);
	private static final ImmutableMap<EquipmentSlot, Integer> VANITY_EQUIPMENT_ITEM_SLOTS = ImmutableMap.of(
		EquipmentSlot.HEAD, 39,
		EquipmentSlot.CHEST, 40,
		EquipmentSlot.LEGS, 41,
		EquipmentSlot.FEET, 42,
		EquipmentSlot.OFF_HAND, 43
	);
	private static final ImmutableMap<EquipmentSlot, Material> VANITY_EQUIPMENT_TITLE_ITEMS = ImmutableMap.of(
		EquipmentSlot.HEAD, Material.GOLDEN_HELMET,
		EquipmentSlot.CHEST, Material.GOLDEN_CHESTPLATE,
		EquipmentSlot.LEGS, Material.GOLDEN_LEGGINGS,
		EquipmentSlot.FEET, Material.GOLDEN_BOOTS,
		EquipmentSlot.OFF_HAND, Material.SHIELD
	);
	private static final ImmutableMap<EquipmentSlot, String> VANITY_EQUIPMENT_TITLE_NAMES = ImmutableMap.of(
		EquipmentSlot.HEAD, "Helmet",
		EquipmentSlot.CHEST, "Chestplate",
		EquipmentSlot.LEGS, "Leggings",
		EquipmentSlot.FEET, "Boots",
		EquipmentSlot.OFF_HAND, "Offhand"
	);
	private static final int SELF_VANITY_TOGGLE_SLOT = 28;
	private static final int OTHER_VANITY_TOGGLE_SLOT = 37;
	private static final int LOCKBOX_VANITY_TOGGLE_SLOT = 46;

	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

	private final Plugin mPlugin;
	private CosmeticType mDisplayPage = null;
	private int mPageNumber = 1;
	private boolean mVanityChanged = false;
	private final boolean mPreviousOtherVanityEnabled;
	private boolean mCosmeticSkillChanged = false;

	// for cosmetic skill paging
	private PlayerClass mCurrentClass = null;
	private PlayerSpec mCurrentSpec = null;
	private Ability mCurrentAbility = null;

	public CosmeticsGUI(Plugin plugin, Player player) {
		super(player, 54, Component.text("Cosmetics Manager", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		mPlugin = plugin;
		mPreviousOtherVanityEnabled = plugin.mVanityManager.getData(player).mOtherVanityEnabled;
		setUpCosmetics(player);
	}

	public CosmeticsGUI(CosmeticsGUI old, Player player) {
		super(player, 54, Component.text("Cosmetics Manager", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		mPlugin = old.mPlugin;
		mDisplayPage = old.mDisplayPage;
		mPageNumber = old.mPageNumber;
		mVanityChanged = old.mVanityChanged;
		mPreviousOtherVanityEnabled = old.mPreviousOtherVanityEnabled;
		setUpCosmetics(player);
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
			//Attempt to switch page if clicked page
			// Main page filtering
			int slot = event.getSlot();
			if (mDisplayPage == null && slot == TITLE_LOC) {
				mDisplayPage = CosmeticType.TITLE;
				player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
				setUpCosmetics(player);
				return;
			} else if (mDisplayPage == null && slot == ELITE_FINISHER_LOC) {
				mDisplayPage = CosmeticType.ELITE_FINISHER;
				player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
				setUpCosmetics(player);
				return;
			} else if (mDisplayPage == null && slot == UNLOCKED_VANITY_LOC) {
				mDisplayPage = CosmeticType.VANITY;
				player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
				setUpCosmetics(player);
				return;
			} else if (mDisplayPage == null && slot == COSMETIC_SKILL_LOC) {
				mDisplayPage = CosmeticType.COSMETIC_SKILL;
				player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
				setUpClassSelectionPage();
				return;
			}

			// Cosmetic skill page layers
			if (mDisplayPage == CosmeticType.COSMETIC_SKILL) {
				// Choose class
				if (mCurrentClass == null && mCurrentAbility == null) {
					// Choose class or shop
					if (slot == CosmeticSkillGUIConfig.SHOP_LOC) {
						close();
						new CosmeticSkillShopGUI(mPlugin, player).openInventory(player, mPlugin);
						return;
					}
					final MonumentaClasses mClasses = new MonumentaClasses(mPlugin, null);
					for (int i = 0; i < CosmeticSkillGUIConfig.CLASS_LOCS.length; i++) {
						if (slot == CosmeticSkillGUIConfig.CLASS_LOCS[i]) {
							mCurrentClass = mClasses.mClasses.get(i);
							player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
							setUpClassPage();
							return;
						}
					}
				} else if (mCurrentSpec == null && mCurrentAbility == null) {
					// Choose skill
					for (int i = 0; i < CosmeticSkillGUIConfig.SKILL_LOCS.length; i++) {
						if (slot == CosmeticSkillGUIConfig.SKILL_LOCS[i]) {
							mCurrentAbility = mCurrentClass.mAbilities.get(i);
							player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
							setUpCosmetics(player);
							return;
						}
					}

					// Choose spec
					if (slot == CosmeticSkillGUIConfig.SPEC_ONE_LOC) {
						mCurrentSpec = mCurrentClass.mSpecOne;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
						setUpSpecPage();
						return;
					} else if (slot == CosmeticSkillGUIConfig.SPEC_TWO_LOC) {
						mCurrentSpec = mCurrentClass.mSpecTwo;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
						setUpSpecPage();
						return;
					}

					// Return to class selection
					if (slot == BACK_LOC) {
						mCurrentClass = null;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
						setUpClassSelectionPage();
						return;
					}
				} else if (mCurrentAbility == null) {
					// Choose spec skills
					for (int i = 0; i < CosmeticSkillGUIConfig.SPEC_SKILL_LOCS.length; i++) {
						if (slot == CosmeticSkillGUIConfig.SPEC_SKILL_LOCS[i]) {
							mCurrentAbility = mCurrentSpec.mAbilities.get(i);
							player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
							setUpCosmetics(player);
							return;
						}
					}

					// Return to class page
					if (slot == BACK_LOC) {
						mCurrentSpec = null;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
						setUpClassPage();
						return;
					}
				} else {
					// Chosen ability, override back to go to previous page
					if (slot == BACK_LOC) {
						mCurrentAbility = null;
						player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
						if (mCurrentSpec == null) {
							setUpClassPage();
						} else {
							setUpSpecPage();
						}
						return;
					}
				}

			}

			//Page control items
			if (slot == NEXT_PAGE_LOC && item.getType() == Material.ARROW) {
				player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
				mPageNumber++;
				setUpCosmetics(player);
				return;
			}

			if (slot == PREV_PAGE_LOC && item.getType() == Material.ARROW) {
				player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
				mPageNumber--;
				setUpCosmetics(player);
				return;
			}

			if (slot == BACK_LOC && item.getType() == Material.REDSTONE_BLOCK && mCurrentAbility == null) {
				player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
				mDisplayPage = null;
				mCurrentClass = null;
				mCurrentSpec = null;
				mCurrentAbility = null;
				setUpCosmetics(player);
				return;
			}

			if (mDisplayPage != null && item.getType() != FILLER && mDisplayPage.isEquippable() && slot >= COSMETICS_START) {
				//Get the list of cosmetics back
				List<Cosmetic> playerCosmetics = CosmeticsManager.getInstance().getCosmeticsOfTypeAlphabetical(player, mDisplayPage, mCurrentAbility);
				if (playerCosmetics != null) {
					int index = (slot - COSMETICS_START) + (COSMETICS_PER_PAGE * (mPageNumber - 1));
					if (mDisplayPage.canEquipMultiple()) {
						if (!playerCosmetics.get(index).mEquipped) {
							player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1f);
							playerCosmetics.get(index).mEquipped = true;
						} else {
							player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1f);
							playerCosmetics.get(index).mEquipped = false;
						}
					} else {
						for (int i = 0; i < playerCosmetics.size(); i++) {
							if (i == index) {
								if (!playerCosmetics.get(i).mEquipped) {
									player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1f);
									playerCosmetics.get(i).mEquipped = true;
								} else {
									player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1f);
									playerCosmetics.get(i).mEquipped = false;
								}
							} else {
								playerCosmetics.get(i).mEquipped = false;
							}
						}
					}
					if (mDisplayPage == CosmeticType.TITLE) {
						MonumentaNetworkChatIntegration.refreshPlayer(player);
					}
					if (mDisplayPage == CosmeticType.COSMETIC_SKILL && mCurrentAbility != null) {
						mCosmeticSkillChanged = true;
					}
					setUpCosmetics(player);
					return;
				}
			}

			// unequip vanity equipment
			if (mDisplayPage == null) {
				for (Map.Entry<EquipmentSlot, Integer> entry : VANITY_EQUIPMENT_ITEM_SLOTS.entrySet()) {
					if (slot == entry.getValue()) {
						VanityManager.VanityData vanityData = mPlugin.mVanityManager.getData(player);
						ItemStack vanityItem = vanityData.getEquipped(entry.getKey());
						if (vanityItem != null) {
							vanityData.equip(entry.getKey(), null);
							mVanityChanged = true;
							setUpCosmetics(player);
							return;
						} else if (VanityManager.hasFreeAccess(player)) {
							vanityData.equip(entry.getKey(), VanityManager.getInvisibleVanityItem(entry.getKey()));
							mVanityChanged = true;
							setUpCosmetics(player);
							return;
						}
					}
				}

				if (slot == SELF_VANITY_TOGGLE_SLOT) {
					mPlugin.mVanityManager.toggleSelfVanity(player);
					setUpCosmetics(player);
					return;
				}
				if (slot == OTHER_VANITY_TOGGLE_SLOT) {
					mPlugin.mVanityManager.toggleOtherVanity(player);
					setUpCosmetics(player);
					return;
				}
				if (slot == LOCKBOX_VANITY_TOGGLE_SLOT) {
					mPlugin.mVanityManager.toggleLockboxSwap(player);
					setUpCosmetics(player);
					return;
				}
			}
		} else {
			// equip vanity equipment from inventory
			if (mDisplayPage == null) {
				if (ItemStatUtils.getInfusionLevel(item, ItemStatUtils.InfusionType.SOULBOUND) > 0
					    && !player.getUniqueId().equals(ItemStatUtils.getInfuser(item, ItemStatUtils.InfusionType.SOULBOUND))) {
					player.sendMessage(Component.text("This item is soulbound to another player!", NamedTextColor.RED));
					player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
					return;
				}
				item = VanityManager.cleanCopyForDisplay(item);
				item.setAmount(1);
				EquipmentSlot slot = ItemUtils.getEquipmentSlot(item);
				if (slot == EquipmentSlot.HAND || event.getClick().isRightClick()) {
					slot = EquipmentSlot.OFF_HAND;
				}
				if (event.getClick().isShiftClick() && VanityManager.hasFreeAccess(player)) {
					slot = EquipmentSlot.HEAD;
				}
				VanityManager.VanityData vanityData = mPlugin.mVanityManager.getData(player);
				if (slot == EquipmentSlot.OFF_HAND && !VanityManager.isValidOffhandVanityItem(item)) {
					player.sendMessage(Component.text("Cannot use items of this type as offhand vanity!", NamedTextColor.RED));
					player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
					return;
				}
				if (VanityManager.hasVanityUnlocked(player, item)) {
					vanityData.equip(slot, item);
					mVanityChanged = true;
					setUpCosmetics(player);
				} else {
					new VanityBuyScreen(player, item, this).openInventory(player, mPlugin);
				}
			}
		}
	}

	/**
	 * Sets up the cosmetic viewing and selecting page
	 * each page is specialized for the specific cosmetic type
	 * currently in the mDisplayPage variable
	 */
	public void setUpCosmetics(Player targetPlayer) {

		// Set inventory to filler to start
		mInventory.clear();
		GUIUtils.fillWithFiller(mInventory, FILLER);

		// Special case: home page
		if (mDisplayPage == null) {
			setUpHomePage(targetPlayer);
			return;
		}

		// Get list of cosmetics
		List<Cosmetic> playerCosmetics = CosmeticsManager.getInstance().getCosmeticsOfTypeAlphabetical(targetPlayer, mDisplayPage, mCurrentAbility);
		if (playerCosmetics == null) {
			mDisplayPage = null;
			setUpCosmetics(targetPlayer);
			return;
		}
		int numPages = (playerCosmetics.size() / COSMETICS_PER_PAGE) + 1;

		// Set up summary
		{
			ItemStack passSummary = new ItemStack(Material.BOOK, 1);
			ItemMeta meta = passSummary.getItemMeta();
			meta.displayName(Component.text("Displaying Unlocked " + mDisplayPage.getDisplayName() + "s", TextColor.color(DepthsUtils.FROSTBORN)).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

			if (mDisplayPage.isEquippable()) {
				List<Component> lore = new ArrayList<>();
				List<Cosmetic> activeCosmetics = CosmeticsManager.getInstance().getActiveCosmetics(targetPlayer, mDisplayPage);
				if (activeCosmetics.size() > 1) {
					lore.add(Component.text("Equipped Cosmetics: multiple (" + activeCosmetics.size() + ")", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
				} else if (!activeCosmetics.isEmpty()) {
					lore.add(Component.text("Equipped Cosmetic: " + activeCosmetics.get(0).getName(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
				} else {
					lore.add(Component.text("No Equipped Cosmetic!", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
				}
				lore.add(Component.text("Click an unlocked cosmetic to equip!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
				meta.lore(lore);
			}
			passSummary.setItemMeta(meta);
			mInventory.setItem(SUMMARY_LOC, passSummary);
		}

		// Set up skill icon if any
		{
			if (mCurrentAbility != null) {
				ItemStack skillIcon = createSkillItemForCosmetic(mCurrentAbility);
				mInventory.setItem(SKILL_ICON_LOC, skillIcon);
			}
		}

		// Individual cosmetic display
		for (int i = (mPageNumber - 1) * COSMETICS_PER_PAGE; (i < mPageNumber * COSMETICS_PER_PAGE && i < playerCosmetics.size()); i++) {
			try {
				Cosmetic c = playerCosmetics.get(i);
				if (c == null) {
					continue;
				}
				mInventory.setItem(COSMETICS_START + (i % COSMETICS_PER_PAGE), c.getDisplayItem());
			} catch (Exception e) {
				e.printStackTrace();
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

		// Display go back button
		{
			ItemStack pageItem = new ItemStack(Material.REDSTONE_BLOCK, 1);
			ItemMeta meta = pageItem.getItemMeta();
			meta.displayName(Component.text("Back to Overview", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			pageItem.setItemMeta(meta);
			mInventory.setItem(BACK_LOC, pageItem);
		}
	}

	/**
	 * Sets up the display page for the home screen
	 * with items to click to browse certain cosmetic types
	 */
	private void setUpHomePage(Player player) {
		mCurrentClass = null;
		mCurrentSpec = null;
		mCurrentAbility = null;
		{
			ItemStack titleItem = new ItemStack(CosmeticType.TITLE.getDisplayItem(null), 1);
			ItemMeta meta = titleItem.getItemMeta();
			meta.displayName(Component.text("Titles", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			meta.lore(List.of(Component.text("Select a title to be displayed", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text("above your head to other players.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			titleItem.setItemMeta(meta);
			mInventory.setItem(TITLE_LOC, titleItem);
		}
		{
			ItemStack eliteItem = new ItemStack(CosmeticType.ELITE_FINISHER.getDisplayItem(null), 1);
			ItemMeta meta = eliteItem.getItemMeta();
			meta.displayName(Component.text("Elite Finishers", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			meta.lore(List.of(Component.text("Select an effect to play", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text("when you kill an elite mob.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			eliteItem.setItemMeta(meta);
			mInventory.setItem(ELITE_FINISHER_LOC, eliteItem);
		}
		{
			ItemStack unlockedVanityItem = new ItemStack(CosmeticType.VANITY.getDisplayItem(null), 1);
			ItemMeta meta = unlockedVanityItem.getItemMeta();
			meta.displayName(Component.text("Show Unlocked Vanity", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			meta.lore(List.of(Component.text("Shows all unlocked vanity items.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text("Use the controls below to change vanity.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			unlockedVanityItem.setItemMeta(meta);
			mInventory.setItem(UNLOCKED_VANITY_LOC, unlockedVanityItem);
		}
		{
			ItemStack cosmeticSkillItem = new ItemStack(CosmeticType.COSMETIC_SKILL.getDisplayItem(null), 1);
			ItemMeta meta = cosmeticSkillItem.getItemMeta();
			meta.displayName(Component.text("Cosmetic Skills", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			meta.lore(List.of(Component.text("Select one skill to modify", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text("cosmetic effects when cast.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			cosmeticSkillItem.setItemMeta(meta);
			mInventory.setItem(COSMETIC_SKILL_LOC, cosmeticSkillItem);
		}

		VanityManager.VanityData vanityData = mPlugin.mVanityManager.getData(player);
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot == EquipmentSlot.HAND) {
				continue;
			}
			{
				ItemStack titleItem = new ItemStack(VANITY_EQUIPMENT_TITLE_ITEMS.get(slot));
				ItemMeta meta = titleItem.getItemMeta();
				meta.displayName(Component.text("Vanity " + VANITY_EQUIPMENT_TITLE_NAMES.get(slot), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
				List<Component> lore = new ArrayList<>(List.of(Component.text("Put an item here to change", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
					Component.text("the appearance of your worn armor.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
				if (slot == EquipmentSlot.OFF_HAND) {
					lore.add(Component.text("Shields, food, bows, etc. can only use", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
					lore.add(Component.text("vanity of the same base item type.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
				}
				lore.add(Component.text("There is a one-time cost of " + STRAND_COST_PER_VANITY_UNLOCK + " Twisted Strands", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
				lore.add(Component.text("per item to unlock that item for vanity use.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
				lore.add(Component.text("Tier 4+ patrons can use any vanity", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
				lore.add(Component.text("item without paying this fee.", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
				meta.lore(lore);
				meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE);
				titleItem.setItemMeta(meta);
				mInventory.setItem(VANITY_EQUIPMENT_TITLE_SLOTS.get(slot), titleItem);
			}
			ItemStack vanityItem = vanityData.getEquipped(slot);
			if (vanityItem == null || vanityItem.getType() == Material.AIR) {
				vanityItem = new ItemStack(Material.ITEM_FRAME);
				ItemMeta meta = vanityItem.getItemMeta();
				meta.displayName(Component.text("Empty Vanity " + VANITY_EQUIPMENT_TITLE_NAMES.get(slot), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
				List<Component> lore = new ArrayList<>(List.of(Component.text("Left-click items in your inventory", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
					Component.text("to equip them in the proper vanity slot.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
					Component.text("Right-click to force equip in the offhand slot.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
				if (VanityManager.hasFreeAccess(player)) {
					lore.add(Component.text("Patron perk: click to hide the equipped item.", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
					if (slot == EquipmentSlot.HEAD) {
						lore.add(Component.text("Patron perk: shift click any item in your", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
						lore.add(Component.text("inventory to equip it as vanity in your head slot.", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
					}
				}
				meta.lore(lore);
				vanityItem.setItemMeta(meta);
			} else {
				vanityItem = ItemUtils.clone(vanityItem);
				ItemMeta meta = vanityItem.getItemMeta();
				List<Component> lore = new ArrayList<>();
				if (VanityManager.isInvisibleVanityItem(vanityItem)) {
					lore.add(Component.text("Hides your equipped " + VANITY_EQUIPMENT_TITLE_NAMES.get(slot).toLowerCase(Locale.ROOT) + ".", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
					if (slot == EquipmentSlot.OFF_HAND) {
						lore.add(Component.text("Does not hide shields, food, bows, etc.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
					}
					lore.add(Component.text("Requires the Resource Pack to be hidden", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
					lore.add(Component.text("for yourself when 'Self Vanity' is enabled.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
					lore.add(Component.text("Exclusive to T4+ patrons.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
				} else {
					lore.add(Component.text("Active vanity skin. Click to remove.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
				}
				meta.lore(lore);
				meta.addItemFlags(ItemFlag.values());
				vanityItem.setItemMeta(meta);
			}
			mInventory.setItem(VANITY_EQUIPMENT_ITEM_SLOTS.get(slot), vanityItem);
		}

		{
			ItemStack selfVanityToggle = new ItemStack(Material.PLAYER_HEAD, 1);
			ItemMeta meta = selfVanityToggle.getItemMeta();
			((SkullMeta) meta).setOwningPlayer(player);
			meta.displayName(Component.text("Self Vanity " + (vanityData.mSelfVanityEnabled ? "Enabled" : "Disabled"), vanityData.mSelfVanityEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			meta.lore(List.of(Component.text("Click to toggle whether your own", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text("vanity is visible to yourself as well.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			selfVanityToggle.setItemMeta(meta);
			ItemUtils.setPlainTag(selfVanityToggle);
			mInventory.setItem(SELF_VANITY_TOGGLE_SLOT, selfVanityToggle);
		}
		{
			ItemStack otherVanityToggle = new ItemStack(Material.PLAYER_HEAD, 1);
			ItemMeta meta = otherVanityToggle.getItemMeta();
			meta.displayName(Component.text("Others' Vanity " + (vanityData.mOtherVanityEnabled ? "Enabled" : "Disabled"), vanityData.mOtherVanityEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			meta.lore(List.of(Component.text("Click to toggle whether you see", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text("other people's vanity equipment.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			otherVanityToggle.setItemMeta(meta);
			ItemUtils.setPlainTag(otherVanityToggle);
			mInventory.setItem(OTHER_VANITY_TOGGLE_SLOT, otherVanityToggle);
		}
		{
			ItemStack lockboxVanityToggle = new ItemStack(Material.GRAY_SHULKER_BOX, 1);
			ItemMeta meta = lockboxVanityToggle.getItemMeta();
			meta.displayName(Component.text("Lockbox Vanity Swap " + (vanityData.mLockboxSwapEnabled ? "Enabled" : "Disabled"), vanityData.mLockboxSwapEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			meta.lore(List.of(Component.text("Click to toggle whether Lockboxes", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text("swap vanity along with equipment.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			lockboxVanityToggle.setItemMeta(meta);
			ItemUtils.setPlainTag(lockboxVanityToggle);
			ItemUtils.setPlainName(lockboxVanityToggle, vanityData.mLockboxSwapEnabled ? "Loadout: Alchemist" : "Loadout: Warrior");
			mInventory.setItem(LOCKBOX_VANITY_TOGGLE_SLOT, lockboxVanityToggle);
		}

	}

	/**
	 * Sets up the dispaly page for the cosmetic skill main page
	 * with items to click to choose a class then browse skills or spec
	 */
	private void setUpClassSelectionPage() {
		// Set inventory to filler to start
		mInventory.clear();
		GUIUtils.fillWithFiller(mInventory, FILLER);

		//Display intro items
		{
			ItemStack introItem = new ItemStack(CosmeticType.COSMETIC_SKILL.getDisplayItem(null), 1);
			ItemMeta meta = introItem.getItemMeta();
			meta.displayName(Component.text("Cosmetic Skills", TextColor.color(DepthsUtils.FROSTBORN)).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

			List<Component> lore = new ArrayList<>();
			lore.add(Component.text("Choose a class to edit cosmetic!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			meta.lore(lore);

			introItem.setItemMeta(meta);
			mInventory.setItem(CosmeticSkillGUIConfig.INTRO_LOC, introItem);
		}

		// Display class items
		{
			final MonumentaClasses mClasses = new MonumentaClasses(mPlugin, null);
			for (int i = 0; i < CosmeticSkillGUIConfig.CLASS_LOCS.length; i++) {
				ItemStack item = createClassItem(mClasses.mClasses.get(i));
				mInventory.setItem(CosmeticSkillGUIConfig.CLASS_LOCS[i], item);
			}
		}

		// Display shop item
		{
			ItemStack pageItem = new ItemStack(Material.RED_GLAZED_TERRACOTTA, 1);
			ItemMeta meta = pageItem.getItemMeta();
			meta.displayName(Component.text("Cosmetic Skill Shop", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			List<Component> lore = new ArrayList<>();
			lore.add(Component.text("View cosmetic skills for sale!", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
			meta.lore(lore);
			pageItem.setItemMeta(meta);
			mInventory.setItem(CosmeticSkillGUIConfig.SHOP_LOC, pageItem);
		}

		// Display go back button
		{
			ItemStack pageItem = new ItemStack(Material.REDSTONE_BLOCK, 1);
			ItemMeta meta = pageItem.getItemMeta();
			meta.displayName(Component.text("Back to Overview", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			pageItem.setItemMeta(meta);
			mInventory.setItem(BACK_LOC, pageItem);
		}
	}

	/**
	 * Sets up class page of cosmetic skill system
	 * with items to click to choose a skill for editing
	 * or choose a spec for spec skills
	 */
	private void setUpClassPage() {
		// Set inventory to filler to start
		mInventory.clear();
		GUIUtils.fillWithFiller(mInventory, FILLER);

		//Display intro items
		{
			ItemStack pageItem = createBasicItem(mCurrentClass.mDisplayItem.getType(), mCurrentClass.mClassName, mCurrentClass.mClassColor,
				true, "Click to edit cosmetics for a skill!", ChatColor.YELLOW);
			mInventory.setItem(CosmeticSkillGUIConfig.INTRO_LOC, pageItem);
		}

		// Display skill items
		for (int i = 0; i < CosmeticSkillGUIConfig.SKILL_LOCS.length; i++) {
			ItemStack abilityItem = createSkillItem(mCurrentClass.mAbilities.get(i));
			mInventory.setItem(CosmeticSkillGUIConfig.SKILL_LOCS[i], abilityItem);
		}

		// Display spec items
		{
			ItemStack pageItem = createBasicItem(mCurrentClass.mSpecOne.mDisplayItem.getType(), mCurrentClass.mSpecOne.mSpecName, NamedTextColor.RED,
				true, "Click to edit cosmetics for " + mCurrentClass.mSpecOne.mSpecName + "!", ChatColor.GRAY);
			mInventory.setItem(CosmeticSkillGUIConfig.SPEC_ONE_LOC, pageItem);
		}

		{
			ItemStack pageItem = createBasicItem(mCurrentClass.mSpecTwo.mDisplayItem.getType(), mCurrentClass.mSpecTwo.mSpecName, NamedTextColor.RED,
				true, "Click to edit cosmetics for " + mCurrentClass.mSpecTwo.mSpecName + "!", ChatColor.GRAY);
			mInventory.setItem(CosmeticSkillGUIConfig.SPEC_TWO_LOC, pageItem);
		}

		// Display go back button
		{
			ItemStack pageItem = new ItemStack(Material.REDSTONE_BLOCK, 1);
			ItemMeta meta = pageItem.getItemMeta();
			meta.displayName(Component.text("Back to class selection", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			pageItem.setItemMeta(meta);
			mInventory.setItem(BACK_LOC, pageItem);
		}

	}

	/**
	 * Sets up spec page of cosmetic sill system
	 * with items to click to choose a skill for editing
	 */
	private void setUpSpecPage() {
		// Set inventory to filler to start
		mInventory.clear();
		GUIUtils.fillWithFiller(mInventory, FILLER);

		//Display intro items
		{
			ItemStack pageItem = createBasicItem(mCurrentSpec.mDisplayItem.getType(), mCurrentSpec.mSpecName, NamedTextColor.RED,
				true, "Click to edit cosmetics for a skill!", ChatColor.YELLOW);
			mInventory.setItem(CosmeticSkillGUIConfig.INTRO_LOC, pageItem);
		}

		// Display skill items
		for (int i = 0; i < CosmeticSkillGUIConfig.SPEC_SKILL_LOCS.length; i++) {
			ItemStack abilityItem = createSkillItem(mCurrentSpec.mAbilities.get(i));
			mInventory.setItem(CosmeticSkillGUIConfig.SPEC_SKILL_LOCS[i], abilityItem);
		}

		// Display go back button
		{
			ItemStack pageItem = new ItemStack(Material.REDSTONE_BLOCK, 1);
			ItemMeta meta = pageItem.getItemMeta();
			meta.displayName(Component.text("Back to class page", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			pageItem.setItemMeta(meta);
			mInventory.setItem(BACK_LOC, pageItem);
		}
	}

	@Override
	protected void inventoryClose(InventoryCloseEvent event) {
		if (event.getPlayer() instanceof Player player) {
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				player.updateInventory();
				if (mVanityChanged) {
					ProtocolLibrary.getProtocolManager().updateEntity(player, ProtocolLibrary.getProtocolManager().getEntityTrackers(player));
				}
				if (mCosmeticSkillChanged) {
					AbilityManager.getManager().updatePlayerAbilities(player, false);
				}
				if (mPreviousOtherVanityEnabled != mPlugin.mVanityManager.getData(player).mOtherVanityEnabled) {
					for (Player otherPlayer : ProtocolLibrary.getProtocolManager().getEntityTrackers(player)) {
						ProtocolLibrary.getProtocolManager().updateEntity(otherPlayer, List.of(player));
					}
				}
			}, 2);
		}
	}

	private static class VanityBuyScreen extends CustomInventory {
		private static final int ITEM_SLOT = 1 * 9 + 4;
		private static final int CONFIRM_SLOT = 2 * 9 + 2;
		private static final int CANCEL_SLOT = 2 * 9 + 6;
		private final ItemStack mItem;
		private final CosmeticsGUI mCosmeticsGUI;

		private static final NamespacedKey TWISTED_STRAND_LOOT_TABLE = NamespacedKeyUtils.fromString("epic:r2/delves/items/twisted_strand");
		private static ItemStack mTwistedStrand;

		public VanityBuyScreen(Player owner, ItemStack item, CosmeticsGUI cosmeticsGUI) {
			super(owner, 4 * 9, Component.text("Unlock Vanity Item?", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			mItem = item;
			mCosmeticsGUI = cosmeticsGUI;
			setupInventory();
		}

		private void setupInventory() {

			{
				ItemStack vanityItem = ItemUtils.clone(mItem);
				ItemMeta meta = vanityItem.getItemMeta();
				String slot = switch (ItemUtils.getEquipmentSlot(mItem)) {
					case HEAD -> "Head";
					case CHEST -> "Chest";
					case LEGS -> "Legs";
					case FEET -> "Feet";
					default -> "Offhand";
				};
				meta.lore(List.of(Component.text("Base material: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
						.append(Component.translatable(vanityItem.getType().getTranslationKey())),
					Component.text("Slot: " + slot, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
				meta.addItemFlags(ItemFlag.values());
				vanityItem.setItemMeta(meta);
				ItemUtils.setPlainTag(vanityItem);
				mInventory.setItem(ITEM_SLOT, vanityItem);
			}

			{
				ItemStack confirm = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
				ItemMeta meta = confirm.getItemMeta();
				meta.displayName(Component.text("Confirm", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
				meta.lore(List.of(Component.text("Pay " + STRAND_COST_PER_VANITY_UNLOCK + " Twisted Strands to unlock", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
					Component.text("this item for vanity use.", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
					Component.text("All items of the same type as this item", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
					Component.text("(base material + name) will permanently be", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
					Component.text("unlocked for you for use as vanity.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
				confirm.setItemMeta(meta);
				ItemUtils.setPlainTag(confirm);
				mInventory.setItem(CONFIRM_SLOT, confirm);
			}
			{
				ItemStack cancel = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
				ItemMeta meta = cancel.getItemMeta();
				meta.displayName(Component.text("Cancel", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
				meta.lore(List.of(Component.text("Do not unlock this item.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
				cancel.setItemMeta(meta);
				ItemUtils.setPlainTag(cancel);
				mInventory.setItem(CANCEL_SLOT, cancel);
			}

			GUIUtils.fillWithFiller(mInventory, FILLER);
		}

		@Override
		protected void inventoryClick(InventoryClickEvent event) {
			event.setCancelled(true);
			Player player = (Player) event.getWhoClicked();
			if (event.getClickedInventory() != mInventory) {
				return;
			}
			if (event.getSlot() == CANCEL_SLOT) {
				close();
				return;
			}
			if (event.getSlot() == CONFIRM_SLOT) {
				if (mTwistedStrand == null) {
					mTwistedStrand = InventoryUtils.getItemFromLootTable(player, TWISTED_STRAND_LOOT_TABLE);
				}
				if (!player.getInventory().containsAtLeast(mTwistedStrand, STRAND_COST_PER_VANITY_UNLOCK)) {
					player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
					player.sendMessage(Component.text("You don't have enough Twisted Strands to pay for this!", NamedTextColor.RED));
					close();
					return;
				}
				mTwistedStrand.setAmount(STRAND_COST_PER_VANITY_UNLOCK);
				player.getInventory().removeItem(mTwistedStrand);
				mCosmeticsGUI.mPlugin.mVanityManager.unlockVanity(player, mItem);
				EquipmentSlot slot = ItemUtils.getEquipmentSlot(mItem);
				if (slot == EquipmentSlot.HAND) {
					slot = EquipmentSlot.OFF_HAND;
				}
				mCosmeticsGUI.mPlugin.mVanityManager.getData(player).equip(slot, mItem);
				close();
				return;
			}
		}

		@Override
		protected void inventoryClose(InventoryCloseEvent event) {
			Player player = (Player) event.getPlayer();
			Bukkit.getScheduler().runTask(mCosmeticsGUI.mPlugin,
				() -> new CosmeticsGUI(mCosmeticsGUI, player).openInventory(player, mCosmeticsGUI.mPlugin));
		}
	}

	private ItemStack createClassItem(PlayerClass classToItemize) {
		return createBasicItem(classToItemize.mDisplayItem.getType(), classToItemize.mClassName, classToItemize.mClassColor,
			true, "Click to choose cosmetics for " + classToItemize.mClassName + "!", ChatColor.GRAY);
	}

	private ItemStack createSkillItem(Ability abilityToItemize) {
		return createBasicItem(abilityToItemize.mDisplayItem.getType(), abilityToItemize.getDisplayName(), mCurrentClass.mClassColor,
			true, "View cosmetics of this skill!", ChatColor.GRAY);
	}

	private ItemStack createSkillItemForCosmetic(Ability abilityToItemize) {
		return createBasicItem(abilityToItemize.mDisplayItem.getType(), abilityToItemize.getDisplayName(), mCurrentClass.mClassColor,
			true, "Current skill", ChatColor.YELLOW);
	}

	// Copy from class GUI
	private ItemStack createBasicItem(Material mat, String name, NamedTextColor nameColor, boolean nameBold, String desc, ChatColor loreColor) {
		ItemStack item = new ItemStack(mat, 1);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text(name, nameColor)
			.decoration(TextDecoration.ITALIC, false)
			.decoration(TextDecoration.BOLD, nameBold));
		GUIUtils.splitLoreLine(meta, desc, 30, loreColor, true);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		item.setItemMeta(meta);
		return item;
	}

}
