package com.playmonumenta.plugins.cosmetics.gui;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.classes.PlayerSpec;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkillGUIConfig;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkillShopGUI;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;


public class CosmeticsGUI extends CustomInventory {

	private static final int PREV_PAGE_LOC = 45;
	private static final int NEXT_PAGE_LOC = 53;
	static final int BACK_LOC = 49;
	private static final int TITLE_LOC = 2 * 9 + 1;
	private static final int ELITE_FINISHER_LOC = 2 * 9 + 3;
	private static final int COSMETIC_SKILL_LOC = 2 * 9 + 5;
	private static final int VANITY_LOC = 2 * 9 + 7;
	private static final int UNLOCKED_VANITY_LOC = 3 * 9 + 7;
	private static final int SUMMARY_LOC = 0;
	private static final int SKILL_ICON_LOC = 8;
	private static final int COSMETICS_START = 9;
	private static final int COSMETICS_PER_PAGE = 36;

	static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

	private final Plugin mPlugin;
	private @Nullable CosmeticType mDisplayPage = null;
	private int mPageNumber = 1;
	private boolean mCosmeticSkillChanged = false;

	// for cosmetic skill paging
	private @Nullable PlayerClass mCurrentClass = null;
	private @Nullable PlayerSpec mCurrentSpec = null;
	private @Nullable AbilityInfo<?> mCurrentAbility = null;

	public CosmeticsGUI(Plugin plugin, Player player) {
		super(player, 6 * 9, Component.text("Cosmetics Manager", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		mPlugin = plugin;
		setUpCosmetics(player);
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
		if (event.getClickedInventory() != mInventory) {
			return;
		}
		//Attempt to switch page if clicked page
		// Main page filtering
		int slot = event.getSlot();
		if (mDisplayPage == null && slot == TITLE_LOC) {
			mDisplayPage = CosmeticType.TITLE;
			playBookSound(player);
			setUpCosmetics(player);
			return;
		} else if (mDisplayPage == null && slot == ELITE_FINISHER_LOC) {
			mDisplayPage = CosmeticType.ELITE_FINISHER;
			playBookSound(player);
			setUpCosmetics(player);
			return;
		} else if (mDisplayPage == null && slot == VANITY_LOC) {
			new VanityGUI(player).open();
			return;
		} else if (mDisplayPage == null && slot == UNLOCKED_VANITY_LOC) {
			mDisplayPage = CosmeticType.VANITY;
			playBookSound(player);
			setUpCosmetics(player);
			return;
		} else if (mDisplayPage == null && slot == COSMETIC_SKILL_LOC) {
			mDisplayPage = CosmeticType.COSMETIC_SKILL;
			playBookSound(player);
			setUpClassSelectionPage();
			return;
		}

		// Cosmetic skill page layers
		if (mDisplayPage == CosmeticType.COSMETIC_SKILL) {
			// Choose class
			if (mCurrentAbility == null) {
				if (mCurrentClass == null) {
					// Choose class or shop
					if (slot == CosmeticSkillGUIConfig.SHOP_LOC) {
						close();
						new CosmeticSkillShopGUI(mPlugin, player).openInventory(player, mPlugin);
						return;
					}
					final MonumentaClasses mClasses = new MonumentaClasses();
					for (int i = 0; i < CosmeticSkillGUIConfig.CLASS_LOCS.length; i++) {
						if (slot == CosmeticSkillGUIConfig.CLASS_LOCS[i]) {
							mCurrentClass = mClasses.mClasses.get(i);
							playBookSound(player);
							setUpClassPage();
							return;
						}
					}
				} else if (mCurrentSpec == null) {
					// Choose skill
					for (int i = 0; i < CosmeticSkillGUIConfig.SKILL_LOCS.length; i++) {
						if (slot == CosmeticSkillGUIConfig.SKILL_LOCS[i]) {
							mCurrentAbility = mCurrentClass.mAbilities.get(i);
							playBookSound(player);
							setUpCosmetics(player);
							return;
						}
					}

					// Choose spec
					if (slot == CosmeticSkillGUIConfig.SPEC_ONE_LOC) {
						mCurrentSpec = mCurrentClass.mSpecOne;
						playBookSound(player);
						setUpSpecPage();
						return;
					} else if (slot == CosmeticSkillGUIConfig.SPEC_TWO_LOC) {
						mCurrentSpec = mCurrentClass.mSpecTwo;
						playBookSound(player);
						setUpSpecPage();
						return;
					}

					// Return to class selection
					if (slot == BACK_LOC) {
						mCurrentClass = null;
						playBookSound(player);
						setUpClassSelectionPage();
						return;
					}
				} else {
					// Choose spec skills
					for (int i = 0; i < CosmeticSkillGUIConfig.SPEC_SKILL_LOCS.length; i++) {
						if (slot == CosmeticSkillGUIConfig.SPEC_SKILL_LOCS[i]) {
							mCurrentAbility = mCurrentSpec.mAbilities.get(i);
							playBookSound(player);
							setUpCosmetics(player);
							return;
						}
					}

					// Return to class page
					if (slot == BACK_LOC) {
						mCurrentSpec = null;
						playBookSound(player);
						setUpClassPage();
						return;
					}
				}
			} else {
				// Chosen ability, override back to go to previous page
				if (slot == BACK_LOC) {
					mCurrentAbility = null;
					playBookSound(player);
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
			playBookSound(player);
			mPageNumber++;
			setUpCosmetics(player);
			return;
		}

		if (slot == PREV_PAGE_LOC && item.getType() == Material.ARROW) {
			playBookSound(player);
			mPageNumber--;
			setUpCosmetics(player);
			return;
		}

		if (slot == BACK_LOC && item.getType() == Material.REDSTONE_BLOCK && mCurrentAbility == null) {
			playBookSound(player);
			mDisplayPage = null;
			mCurrentClass = null;
			mCurrentSpec = null;
			mCurrentAbility = null;
			setUpCosmetics(player);
			return;
		}

		if (mDisplayPage != null && item.getType() != FILLER && mDisplayPage.isEquippable() && slot >= COSMETICS_START) {
			int index = (slot - COSMETICS_START) + (COSMETICS_PER_PAGE * (mPageNumber - 1));
			toggleCosmetic(player, mDisplayPage, mCurrentAbility, index);

			if (mDisplayPage == CosmeticType.TITLE) {
				MonumentaNetworkChatIntegration.refreshPlayer(player);
			}
			if (mDisplayPage == CosmeticType.COSMETIC_SKILL && mCurrentAbility != null) {
				mCosmeticSkillChanged = true;
			}
			setUpCosmetics(player);
		}
	}

	public static void toggleCosmetic(Player player, Cosmetic cosmetic) {
		List<Cosmetic> playerCosmetics = CosmeticsManager.getInstance().getCosmeticsOfTypeAlphabetical(player, cosmetic.mType, cosmetic.mAbility);
		toggleCosmetic(player, playerCosmetics, cosmetic);
	}

	public static void toggleCosmetic(Player player, CosmeticType type, @Nullable AbilityInfo<?> ability, int index) {
		List<Cosmetic> playerCosmetics = CosmeticsManager.getInstance().getCosmeticsOfTypeAlphabetical(player, type, ability);
		toggleCosmetic(player, playerCosmetics, playerCosmetics.get(index));
	}

	public static void toggleCosmetic(Player player, List<Cosmetic> playerCosmetics, Cosmetic cosmetic) {
		if (!cosmetic.getType().canEquipMultiple()) {
			playerCosmetics.stream().filter(c -> c != cosmetic).forEach(c -> c.mEquipped = false);
		}
		if (cosmetic.isEquipped()) {
			player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.8f, 1f);
			cosmetic.mEquipped = false;
		} else {
			player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.8f, 1f);
			cosmetic.mEquipped = true;
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
			setUpHomePage();
			return;
		}

		// Get list of cosmetics
		List<Cosmetic> playerCosmetics = mCurrentAbility != null && mCurrentAbility.getLinkedSpell() == null ? List.of()
			                                 : CosmeticsManager.getInstance().getCosmeticsOfTypeAlphabetical(targetPlayer, mDisplayPage, mCurrentAbility);
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
				List<Cosmetic> activeCosmetics = CosmeticsManager.getInstance().getActiveCosmetics(targetPlayer, mDisplayPage, mCurrentAbility == null ? null : mCurrentAbility.getLinkedSpell());
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
		ItemStack pageItem = GUIUtils.createBasicItem(Material.REDSTONE_BLOCK, "Back to Overview", NamedTextColor.RED, true);
		mInventory.setItem(BACK_LOC, pageItem);
	}

	/**
	 * Sets up the display page for the home screen
	 * with items to click to browse certain cosmetic types
	 */
	private void setUpHomePage() {
		mCurrentClass = null;
		mCurrentSpec = null;
		mCurrentAbility = null;

		ItemStack titleItem = GUIUtils.createBasicItem(CosmeticType.TITLE.getDisplayItem(null), "Titles", NamedTextColor.GOLD, true, "Select a title to be displayed above your head for other players", NamedTextColor.GRAY);
		mInventory.setItem(TITLE_LOC, titleItem);

		ItemStack eliteItem = GUIUtils.createBasicItem(CosmeticType.ELITE_FINISHER.getDisplayItem(null), "Elite Finishers", NamedTextColor.GOLD, true, "Select an effect to play when you kill an elite mob.", NamedTextColor.GRAY);
		mInventory.setItem(ELITE_FINISHER_LOC, eliteItem);

		ItemStack cosmeticSkillItem = GUIUtils.createBasicItem(CosmeticType.COSMETIC_SKILL.getDisplayItem(null), "Cosmetic Skills", NamedTextColor.GOLD, true, "Select cosmetic effects for ability casts.", NamedTextColor.GRAY);
		mInventory.setItem(COSMETIC_SKILL_LOC, cosmeticSkillItem);

		ItemStack vanityItem = GUIUtils.createBasicItem(CosmeticType.VANITY.getDisplayItem(null), "Vanity Manager", NamedTextColor.GOLD, true, "Control your equipped vanity items.", NamedTextColor.GRAY);
		mInventory.setItem(VANITY_LOC, vanityItem);

		ItemStack unlockedVanityItem = GUIUtils.createBasicItem(Material.LEATHER_CHESTPLATE, "Show Unlocked Vanity", NamedTextColor.GOLD, true, "Shows all unlocked vanity items.", NamedTextColor.GRAY);
		mInventory.setItem(UNLOCKED_VANITY_LOC, unlockedVanityItem);
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
			final MonumentaClasses mClasses = new MonumentaClasses();
			for (int i = 0; i < CosmeticSkillGUIConfig.CLASS_LOCS.length; i++) {
				ItemStack item = createClassItem(mClasses.mClasses.get(i));
				mInventory.setItem(CosmeticSkillGUIConfig.CLASS_LOCS[i], item);
			}
		}

		// Display shop item
		ItemStack shopItem = GUIUtils.createBasicItem(Material.EMERALD, "Cosmetic Skill Shop", NamedTextColor.LIGHT_PURPLE, true, "View cosmetic skills for sale!", NamedTextColor.GOLD);
		mInventory.setItem(CosmeticSkillGUIConfig.SHOP_LOC, shopItem);

		// Display go back button
		ItemStack pageItem = GUIUtils.createBasicItem(Material.REDSTONE_BLOCK, "Back to Overview", NamedTextColor.RED, true);
		mInventory.setItem(BACK_LOC, pageItem);
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

		Objects.requireNonNull(mCurrentClass);

		//Display intro items
		{
			ItemStack pageItem = GUIUtils.createBasicItem(mCurrentClass.mDisplayItem, mCurrentClass.mClassName, mCurrentClass.mClassColor,
				true, "Click to edit cosmetics for a skill!", NamedTextColor.YELLOW);
			mInventory.setItem(CosmeticSkillGUIConfig.INTRO_LOC, pageItem);
		}

		// Display skill items
		for (int i = 0; i < CosmeticSkillGUIConfig.SKILL_LOCS.length; i++) {
			ItemStack abilityItem = createSkillItem(mCurrentClass.mAbilities.get(i));
			mInventory.setItem(CosmeticSkillGUIConfig.SKILL_LOCS[i], abilityItem);
		}

		// Display spec items
		{
			ItemStack pageItem = GUIUtils.createBasicItem(mCurrentClass.mSpecOne.mDisplayItem, mCurrentClass.mSpecOne.mSpecName, mCurrentClass.mClassColor,
				true, "Click to edit cosmetics for " + mCurrentClass.mSpecOne.mSpecName + "!", NamedTextColor.GRAY);
			mInventory.setItem(CosmeticSkillGUIConfig.SPEC_ONE_LOC, pageItem);
		}

		{
			ItemStack pageItem = GUIUtils.createBasicItem(mCurrentClass.mSpecTwo.mDisplayItem, mCurrentClass.mSpecTwo.mSpecName, mCurrentClass.mClassColor,
				true, "Click to edit cosmetics for " + mCurrentClass.mSpecTwo.mSpecName + "!", NamedTextColor.GRAY);
			mInventory.setItem(CosmeticSkillGUIConfig.SPEC_TWO_LOC, pageItem);
		}

		// Display go back button
		ItemStack pageItem = GUIUtils.createBasicItem(Material.REDSTONE_BLOCK, "Back to Class Selection", NamedTextColor.RED, true);
		mInventory.setItem(BACK_LOC, pageItem);
	}

	/**
	 * Sets up spec page of cosmetic sill system
	 * with items to click to choose a skill for editing
	 */
	private void setUpSpecPage() {
		// Set inventory to filler to start
		mInventory.clear();
		GUIUtils.fillWithFiller(mInventory, FILLER);

		Objects.requireNonNull(mCurrentSpec);
		Objects.requireNonNull(mCurrentClass);

		//Display intro items
		{
			ItemStack pageItem = GUIUtils.createBasicItem(mCurrentSpec.mDisplayItem, mCurrentSpec.mSpecName, mCurrentClass.mClassColor,
				true, "Click to edit cosmetics for a skill!", NamedTextColor.YELLOW);
			mInventory.setItem(CosmeticSkillGUIConfig.INTRO_LOC, pageItem);
		}

		// Display skill items
		for (int i = 0; i < CosmeticSkillGUIConfig.SPEC_SKILL_LOCS.length; i++) {
			ItemStack abilityItem = createSkillItem(mCurrentSpec.mAbilities.get(i));
			mInventory.setItem(CosmeticSkillGUIConfig.SPEC_SKILL_LOCS[i], abilityItem);
		}

		// Display go back button
		ItemStack pageItem = GUIUtils.createBasicItem(Material.REDSTONE_BLOCK, "Back to Class Page", NamedTextColor.RED, true);
		mInventory.setItem(BACK_LOC, pageItem);
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

	private ItemStack createClassItem(PlayerClass classToItemize) {
		return GUIUtils.createBasicItem(classToItemize.mDisplayItem, classToItemize.mClassName, classToItemize.mClassColor,
			true, "Click to choose cosmetics for " + classToItemize.mClassName + "!", NamedTextColor.GRAY);
	}

	private ItemStack createSkillItem(AbilityInfo<?> abilityToItemize) {
		return createItem(abilityToItemize, "View cosmetics of this skill!", NamedTextColor.GRAY);
	}

	private ItemStack createSkillItemForCosmetic(AbilityInfo<?> abilityToItemize) {
		return createItem(abilityToItemize, "Current skill", NamedTextColor.YELLOW);
	}

	private ItemStack createItem(AbilityInfo<?> abilityToItemize, String desc, NamedTextColor loreColor) {
		Objects.requireNonNull(mCurrentClass);
		Material baseItem = Objects.requireNonNull(abilityToItemize.getDisplayItem());
		return GUIUtils.createBasicItem(baseItem, Objects.requireNonNull(abilityToItemize.getDisplayName()), mCurrentClass.mClassColor,
			true, desc, loreColor);
	}

	private void playBookSound(Player player) {
		player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.5f, 1f);
	}

}
