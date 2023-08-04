package com.playmonumenta.plugins.guis;

import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.trades.TradeWindowOpenEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;


public class CustomTradeGui extends Gui {
	// Static scoreboard objective names:
	public static final String MAIN = "peb_tradeGUI_main";
	public static final String LOCKED = "peb_tradeGUI_locked";
	public static final String THEME = "peb_tradeGUI_theme";
	public static final String SPACING = "peb_tradeGUI_spacing";
	public static final String PREVIEWDISPLAY = "peb_tradeGUI_previewDisplay";
	public static final String TRADEORG = "peb_tradeGUI_tradeOrg";
	public static final String CONFIRM = "peb_tradeGUI_confirm";
	public static final String SUCCESS = "peb_tradeGUI_success";
	public static final String PARTICLES = "peb_tradeGUI_particles";
	public static final String SOUNDS = "peb_tradeGUI_sounds";

	public static final String PERMISSION = "monumenta.customtradegui";

	// Final:
	private final List<TradeWindowOpenEvent.Trade> mTrades;
	private final Villager mVillager;
	private final String mCustomTagKey = "trade_preview";

	// Setup:
	private final List<TradeType> mDisplayTradeTypes = new ArrayList<>();
	private final List<TradeWindowOpenEvent.Trade> mTradeMisc = new ArrayList<>();
	private final List<TradeWindowOpenEvent.Trade> mTradeWeapon = new ArrayList<>();
	private final List<TradeWindowOpenEvent.Trade> mTradeOffhand = new ArrayList<>();
	private final List<TradeWindowOpenEvent.Trade> mTradeArmor = new ArrayList<>();
	private final List<TradeWindowOpenEvent.Trade> mTradeCharm = new ArrayList<>();
	private final Map<List<ItemStack>, TradeStatusWrapper> mSavedBaseRequirements = new HashMap<>(); // Save the result of base requirement checks as they can be intensive.

	// GUI Vars:
	private @Nullable TradeWindowOpenEvent.Trade mSelectedTrade = null;
	private int mSelectedTradeMultiplier = 1;
	private int mSelectedTradeMaxMultiplier = 64;
	private @Nullable TradeType mCurrentTab = null;
	private int mCurrentPage = 1;

	// Misc:
	private ItemStack mBackgroundPane = GUIUtils.createBasicItem(Material.BROWN_STAINED_GLASS_PANE, "", NamedTextColor.GRAY, false,
		"", NamedTextColor.GRAY, 0);
	private final ItemStack mBackgroundChain = GUIUtils.createBasicItem(Material.IRON_BARS, "", NamedTextColor.GRAY, false,
		"", NamedTextColor.GRAY, 0);

	// Subclasses/Enums:
	private enum TradeType {
		WEAPON("Weapons"), ARMOR("Armor"), OFFHAND("Offhands"), CHARM("Charms"), MISC("Misc"), GENERAL("Trades");

		private final String mName;

		TradeType(String name) {
			this.mName = name;
		}

		@Override
		public String toString() {
			return mName;
		}
	}

	private enum SoundType {
		TAB_SELECT, PAGE_FLIP, CONFIRM_TRADE, TRADE_ERROR, TRADE_SUCCESS
	}

	private class TradeReq {
		// This class holds the status of the player's requirements for a specific trade.
		private List<Component> mLore = new ArrayList<>();
		private final List<ItemStack> mRequirements = new ArrayList<>();
		private final int mNumRequirements;
		private boolean mHasRequirements = true;

		public TradeReq(Player player, TradeWindowOpenEvent.Trade trade, int multiplier, boolean useCache) {
			// Obtain the requirements for this trade:
			for (ItemStack requirement : trade.getRecipe().getIngredients()) {
				// Check for air or null:
				if (requirement == null || requirement.getType() == Material.AIR) {
					continue;
				}
				// Update requirement with multiplier:
				requirement.setAmount(requirement.getAmount() * multiplier);
				mRequirements.add(requirement);
			}
			// Update mNumRequirements:
			mNumRequirements = mRequirements.size();
			// See if we should check our saved map:
			if (useCache && multiplier == 1 && mSavedBaseRequirements.containsKey(mRequirements)) {
				// useCache is true: usually true, but if we are in buyNow(), we should recheck the player's inventory.
				// multiplier == 1: our map only saves the base requirement, without any trade multipliers.
				TradeStatusWrapper wrapper = mSavedBaseRequirements.get(mRequirements);
				mHasRequirements = wrapper.status();
				mLore = wrapper.lore();
				return;
			}
			// Shallow copy of player's inventory:
			ItemStack[] itemStacks = player.getInventory().getStorageContents().clone();
			// Check each requirement, constructing lore and updating mHasRequirements:
			for (ItemStack requirement : mRequirements) {
				boolean meetsRequirement;
				// Obtain the status of this requirement:
				meetsRequirement = InventoryUtils.inventoryContainsItemOrMore(itemStacks, requirement);
				mHasRequirements = mHasRequirements && meetsRequirement;
				if (meetsRequirement) {
					InventoryUtils.removeItemFromArray(itemStacks, requirement);
				}
				// Construct lore:
				int numItems = requirement.getAmount();
				String plainName = ItemUtils.getPlainNameOrDefault(requirement);
				mLore.add(
					Component.text(numItems + " " + plainName + " ", NamedTextColor.WHITE).append(
						Component.text(meetsRequirement ? "\u2713" : "\u2717", (meetsRequirement ? NamedTextColor.GREEN : NamedTextColor.RED))).decoration(TextDecoration.ITALIC, false));
			}
			// Finally, save the base requirement to our map:
			if (multiplier == 1) {
				mSavedBaseRequirements.put(mRequirements, new TradeStatusWrapper(mHasRequirements, mLore));
			}
		}

		public List<Component> lore() {
			return mLore;
		}

		public List<ItemStack> requirements() {
			return mRequirements;
		}

		public int size() {
			return mNumRequirements;
		}

		public boolean status() {
			return mHasRequirements;
		}
	}

	private static class TradeStatusWrapper {
		private boolean mHasRequirements;
		private List<Component> mLore;


		public TradeStatusWrapper(boolean hasRequirements, List<Component> lore) {
			this.mHasRequirements = hasRequirements;
			this.mLore = lore;
		}


		public boolean status() {
			return this.mHasRequirements;
		}


		public List<Component> lore() {
			return this.mLore;
		}
	}


	// Options:
		// Note: mPeb_tradeGUI_main & mPeb_tradeGUI_locked are also scoreboards, but not tested in here.
	private final int mPebTradeGUITheme; // 0: classic. 1: sleek.
	private final int mPebTradeGUISpacing; // 0: auto. 1: force 16. 2: force 28.
	private final int mPebTradeGUIPreviewDisplay; // 0: display price on preview. 1: dont.
	private final int mPebTradeGUITradeOrg; // 0: split trades by type. 1: dont.
	private final int mPebTradeGUIConfirm; // 0: bring up confirm menu. 1: dont.
	private final int mPebTradeGUISuccess; // 0: return to preview upon successful trade. 1: close gui. 2: do nothing.
	private final int mPebTradeGUIParticles; // 0: particles on. 1: off.
	private final int mPebTradeGUISounds; // 0: sounds on. 1: off.

	// Interface:
	public CustomTradeGui(Player player, Villager villager, List<TradeWindowOpenEvent.Trade> trades) {
		super(player, 6 * 9, villager.customName() == null ? Component.text("NPC trader") : villager.customName());
		mTrades = trades;
		mVillager = villager;
		// Setup PEB options:
		mPebTradeGUITheme = ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.THEME).orElse(0);
		mPebTradeGUISpacing = ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.SPACING).orElse(0);
		mPebTradeGUIPreviewDisplay = ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.PREVIEWDISPLAY).orElse(0);
		mPebTradeGUITradeOrg = ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.TRADEORG).orElse(0);
		mPebTradeGUIConfirm = ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.CONFIRM).orElse(0);
		mPebTradeGUISuccess = ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.SUCCESS).orElse(0);
		mPebTradeGUIParticles = ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.PARTICLES).orElse(0);
		mPebTradeGUISounds = ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.SOUNDS).orElse(0);
		// Options:
			// tradeOrg:
		organizeTradesIfApplicable();
			// theme:
		if (mPebTradeGUITheme == 1) {
			mBackgroundPane = GUIUtils.createBasicItem(Material.GRAY_STAINED_GLASS_PANE, "", NamedTextColor.GRAY, false,
				"", NamedTextColor.GRAY, 0);
		}
	}

	@Override
	protected void setup() {
		// Background panes:
		for (int i = 0; i < 9; i++) {
			setItem(0, i, mBackgroundPane);
			setItem(5, i, mBackgroundPane);
		}
		for (int i = 1; i < 5; i++) {
			setItem(i, 0, mBackgroundPane);
			setItem(i, 8, mBackgroundPane);
		}
		if (ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.LOCKED).orElse(0) == 1) {
			// Locked Trades:
			openLockedTradeView();
		} else if (mSelectedTrade == null) {
			// Show Trade Previews:
			openGeneralTradeView();
		} else {
			// Open confirm menu:
			openConfirmTradeView();
		}
	}

	@Override
	protected void onClose(InventoryCloseEvent event) {
		// Clear any gui-tagged items:
		clearIllegalItems();
	}

	// Tabs and navigation:
	private void openLockedTradeView() {
		setItem(0, 4, GUIUtils.createBasicItem(Material.OAK_SIGN, "Error: ", NamedTextColor.BLUE, false,
			"You don't meet the requirements to view these trades!", NamedTextColor.GRAY, 20));
		// Amogus:
		setItem(1, 3, mBackgroundChain);
		setItem(1, 4, mBackgroundChain);
		setItem(1, 5, mBackgroundChain);
		setItem(2, 5, mBackgroundChain);
		setItem(2, 6, mBackgroundChain);
		setItem(3, 3, mBackgroundChain);
		setItem(3, 4, mBackgroundChain);
		setItem(3, 5, mBackgroundChain);
		setItem(3, 6, mBackgroundChain);
		setItem(4, 3, mBackgroundChain);
		setItem(4, 5, mBackgroundChain);
	}

	private void openGeneralTradeView() {
		// Initialize the current tab:
		if (mCurrentTab == null) {
			mCurrentTab = mDisplayTradeTypes.get(0);
		}
		// Display header and icons for all tabs:
		setItem(0, 4, GUIUtils.createBasicItem(Material.OAK_SIGN, "Viewing: ", NamedTextColor.BLUE, false,
			"Tab: " + mCurrentTab.toString() + "\nPage: " + mCurrentPage + "/" + getMaxPages(), NamedTextColor.GRAY, 20));
		int guiCol = 1;
		for (TradeType tradeType : mDisplayTradeTypes) {
			setItem(5, guiCol, GUIUtils.createBasicItem((mCurrentTab == tradeType ? Material.BLUE_STAINED_GLASS_PANE : Material.CYAN_STAINED_GLASS_PANE), tradeType.toString() + (mCurrentTab == tradeType ? " (Selected)" : ""), NamedTextColor.YELLOW, false,
				"", NamedTextColor.GRAY, 0)).onLeftClick(() -> {
					// Select Tab:
				if (mCurrentTab != tradeType) {
					mCurrentTab = tradeType;
					mCurrentPage = 1;
					playSound(mPlayer.getLocation(), SoundType.TAB_SELECT);
					update();
				}
			});
			guiCol++;
		}
		// Display page icons if needed:
		int pageCount = getMaxPages();
		if (mCurrentPage > pageCount) {
			// Failsafe for a glitch or something:
			mCurrentPage = pageCount;
		}
		if (mCurrentPage < pageCount) {
			// EX - we are on page 1 out of 2:
			setItem(5, 7, GUIUtils.createBasicItem(Material.GREEN_STAINED_GLASS_PANE, "Next Page (" + (mCurrentPage + 1) + ")", NamedTextColor.YELLOW, false,
				"", NamedTextColor.GRAY, 0)).onLeftClick(() -> {
					// Page Flip:
				mCurrentPage++;
				playSound(mPlayer.getLocation(), SoundType.PAGE_FLIP);
				update();
			});
		}
		if (mCurrentPage > 1) {
			// EX - we are on page 2:
			setItem(5, 6, GUIUtils.createBasicItem(Material.GREEN_STAINED_GLASS_PANE, "Previous Page (" + (mCurrentPage - 1) + ")", NamedTextColor.YELLOW, false,
				"", NamedTextColor.GRAY, 0)).onLeftClick(() -> {
					// Page Flip:
				mCurrentPage--;
				playSound(mPlayer.getLocation(), SoundType.PAGE_FLIP);
				update();
			});
		}
		// Load trades for the current tab:
		showTrades(mCurrentTab);
	}

	private void openConfirmTradeView() {
		if (mSelectedTrade == null) {
			// Note: these error messages are similar to the message you get if you access a village UI you aren't supposed to -
			// None will pretty much never happen, but just adding a failsafe + letting the player know.
			mPlayer.sendMessage("Something went wrong - if this keeps happening, please report it!");
			MMLog.warning("Custom Trade GUI: null trade at: " + mVillager.getName());
			close();
			return;
		}
		// Vars:
		MerchantRecipe recipe = mSelectedTrade.getRecipe();
		String itemName = ItemUtils.getPlainNameOrDefault(recipe.getResult());
		TradeReq tradeReq = new TradeReq(mPlayer, mSelectedTrade, mSelectedTradeMultiplier, true);
		// Header:
		setItem(0, 4, GUIUtils.createBasicItem(Material.OAK_SIGN, "Viewing: ", NamedTextColor.BLUE, false,
			itemName, NamedTextColor.GRAY, 20));
		// Trade result:
		setItem(2, 5, createTradePreviewGuiItem(recipe, tradeReq, false, mSelectedTradeMultiplier));
		// Trade requirement(s):
		int numRequirements = tradeReq.size();
		if (numRequirements <= 0) {
			mPlayer.sendMessage("Something went wrong - if this keeps happening, please report it!");
			MMLog.warning("Custom Trade GUI: trade with no requirements at: " + mVillager.getName() + ", " + mSelectedTrade);
			close();
			return;
		} else if (numRequirements == 1) {
			setItem(2, 3, createTradePreviewGuiItem(tradeReq.requirements().get(0)));
		} else if (numRequirements == 2) {
			setItem(2, 3, createTradePreviewGuiItem(tradeReq.requirements().get(1)));
			setItem(2, 2, createTradePreviewGuiItem(tradeReq.requirements().get(0)));
		} else {
			mPlayer.sendMessage("Something went wrong - if this keeps happening, please report it!");
			MMLog.warning("Custom Trade GUI: trade with no requirements at: " + mVillager.getName() + ", " + mSelectedTrade);
			close();
			return;
		}
		// Trade Multiplier Button:
		setItem(2, 4, createTradeMultiplierButton())
			.onClick((inventoryClickEvent) -> {
				int offset = 1;
				if (inventoryClickEvent.isShiftClick()) {
					offset = 5;
				}
				if (inventoryClickEvent.isLeftClick()) {
					// Left click, decrease multiplier:
					modifyTradeMultiplier(-offset);
				} else {
					// Right click, increase multiplier:
					modifyTradeMultiplier(offset);
				}
				update();
			});
		// Back Button:
		setItem(4, 3, GUIUtils.createBasicItem(Material.ARROW, "Back", NamedTextColor.GRAY, false,
			"Return to the previous page.", NamedTextColor.GRAY, 40))
			.onLeftClick(this::navReturnToPreview);
		// Confirm/Deny Button:
		if (tradeReq.status()) {
			setItem(4, 5, createConfirmButton(mSelectedTrade, recipe, tradeReq)).onLeftClick(() -> {
				buyNow(mSelectedTrade, mSelectedTradeMultiplier);
			});
		} else {
			setItem(4, 5, createConfirmButton(mSelectedTrade, recipe, tradeReq));
		}
	}

	private void showTrades(TradeType type) {
		// Find the right trades list:
		List<TradeWindowOpenEvent.Trade> trades = tradeTypeToMemberList(type);
		// Loop through and display trades from (1, 1) to (7, 4):
		int numTrades = trades.size();
		// Trade preview spacing:
		int spacer;
		if (mPebTradeGUISpacing == 1) {
			// Force 16 items per page:
			spacer = 2;
		} else if (mPebTradeGUISpacing == 2) {
			// Force 28 items per page:
			spacer = 1;
		} else {
			// Auto:
			if (numTrades > 16) {
				spacer = 1;
			} else {
				spacer = 2;
			}
		}
		// Page Logic:
		int maxTradesPerPage = (mPebTradeGUISpacing == 1 ? 16 : 28); // 0 and 2 are auto & 28, 1 is 16.
		int firstTradeIndex = (mCurrentPage - 1) * maxTradesPerPage; // mCurrentPage starts at 1, but index starts at 0.
		int lastTradeIndex = Math.min(numTrades, (mCurrentPage * maxTradesPerPage)); // stop before the start of the next page or end of trades.
		// Place trades:
		int guiRow = 1;
		int guiCol = 1;
		for (int i = firstTradeIndex; i < lastTradeIndex && guiRow < 5; i++) {
			// Trade position logic:
			if (guiCol > 7) {
				// Sides are off limits:
				if (guiRow == 4) {
					// We should never get to this point:
					mPlayer.sendMessage("Something went wrong - if this keeps happening, please report it!");
					MMLog.warning("Custom Trade GUI: overflow trades: " + mVillager.getName() + ", " + trades.get(i));
					close();
					return;
				} else {
					guiRow++;
					guiCol = 1;
				}
			}
			// Trade preview logic:
			TradeWindowOpenEvent.Trade trade = trades.get(i);
			TradeReq tradeReq = new TradeReq(mPlayer, trade, 1, true);
			setItem(guiRow, guiCol, createTradePreviewGuiItem(trade.getRecipe(), tradeReq, true, 1)).onLeftClick(() -> {
				if (mPebTradeGUIConfirm == 0) {
					// Open confirm menu:
					navGoToConfirm(trade);
				} else {
					// Skip confirm menu, attempt buy if meet requirements:
					if (tradeReq.status()) {
						buyNow(trade, 1);
					}
					// Else do nothing
				}
			}).onRightClick(() -> {
				// Always open confirm menu:
				navGoToConfirm(trade);
			});
			guiCol += spacer;
		}
	}

	private void navGoToConfirm(TradeWindowOpenEvent.Trade trade) {
		mSelectedTrade = trade;
		mSelectedTradeMaxMultiplier = getMaxTradeMultiplier(trade);
		playSound(mPlayer.getLocation(), SoundType.CONFIRM_TRADE);
		update();
	}

	private void navReturnToPreview() {
		mSelectedTrade = null;
		mSelectedTradeMultiplier = 1;
		update();
	}

	// Actions:
	private void buyNow(@Nullable TradeWindowOpenEvent.Trade trade, int multiplier) {
		if (trade == null) {
			mPlayer.sendMessage("Something went wrong - if this keeps happening, please report it!");
			MMLog.warning("Custom Trade GUI: null trade @buyNow: " + mVillager.getName());
			close();
			return;
		}
		// Double check requirements:
		TradeReq tradeReq = new TradeReq(mPlayer, trade, multiplier, false);
		if (!tradeReq.status()) {
			mPlayer.sendMessage("You don't have the required materials for this trade - please reopen the menu!");
			close();
			return;
		}
		// Remove reqs:
		Inventory inventory = mPlayer.getInventory();
		for (ItemStack requirement : tradeReq.requirements()) {
			inventory.removeItem(requirement.clone());
		}
		// Success, give item * multiplier:
		for (int i = 0; i < multiplier; i++) {
			ItemStack result = trade.getOriginalResult();
			int count = trade.getCount();
			if (result == null || count <= 1) {
				InventoryUtils.giveItem(mPlayer, trade.getRecipe().getResult().clone());
			} else {
				// Copied from SQ NpcTradeManager
				int maxStackSize = result.getMaxStackSize();
				List<ItemStack> items = new ArrayList<>();
				while (count > 0) {
					int amount = count;
					if (amount > maxStackSize) {
						amount = maxStackSize;
					}
					ItemStack resultCopy = new ItemStack(result);
					resultCopy.setAmount(amount);
					items.add(resultCopy);

					count -= amount;
				}

				com.playmonumenta.scriptedquests.utils.InventoryUtils.giveItems(mPlayer, items, false);
			}
		}
		mPlayer.updateInventory();
		// Run Quest Actions:
		QuestActions actions = trade.getActions();
		if (actions != null) {
			QuestContext context = new QuestContext(com.playmonumenta.scriptedquests.Plugin.getInstance(), mPlayer, mVillager);
			for (int i = 0; i < multiplier; i++) {
				actions.doActions(context);
			}
		}
		// Sound and Particle Effects:
		Location location = mPlayer.getLocation();
		playSound(location, SoundType.TRADE_SUCCESS);
		if (mPebTradeGUIParticles == 0) {
			EntityUtils.fireworkAnimation(location, List.of(Color.GREEN, Color.LIME, Color.FUCHSIA), FireworkEffect.Type.BALL, 40);
		}
		// Either return to preview, close GUI, or do nothing:
		if (mPebTradeGUISuccess == 1) {
			// close GUI:
			close();
			return;
		}
		// Clear saved reqs (since item was bought, need to recheck):
		mSavedBaseRequirements.clear();
		if (mPebTradeGUISuccess == 0) {
			navReturnToPreview();
		} else {
			update();
		}
	}

	private void clearIllegalItems() {
		Inventory inventory = mPlayer.getInventory();
		NamespacedKey customTagKey = new NamespacedKey(mPlugin, mCustomTagKey);
		for (ItemStack itemStack : inventory.getContents()) {
			if (itemStack != null) {
				ItemMeta itemMeta = itemStack.getItemMeta();
				// Check if the custom tag exists on the item meta
				if (itemMeta.getPersistentDataContainer().has(customTagKey, PersistentDataType.INTEGER)) {
					// Custom tag exists, clear the item
					inventory.remove(itemStack);
				}
			}
		}
	}

	private void modifyTradeMultiplier(int offset) {
		if (mSelectedTradeMultiplier + offset > mSelectedTradeMaxMultiplier) {
			playSound(mPlayer.getLocation(), SoundType.TRADE_ERROR);
			mSelectedTradeMultiplier = mSelectedTradeMaxMultiplier;
			return;
		}
		if (mSelectedTradeMultiplier + offset < 1) {
			playSound(mPlayer.getLocation(), SoundType.TRADE_ERROR);
			mSelectedTradeMultiplier = 1;
			return;
		}
		mSelectedTradeMultiplier += offset;
	}

	private void playSound(Location location, SoundType type) {
		if (mPebTradeGUISounds == 1) {
			return;
		}
		if (type == SoundType.TAB_SELECT) {
			Sound sound = Sound.BLOCK_WOODEN_BUTTON_CLICK_ON;
			float volume = 0.6f;
			float pitch = 1.0f;
			mPlayer.playSound(location, sound, SoundCategory.NEUTRAL, volume, pitch);
		} else if (type == SoundType.PAGE_FLIP) {
			Sound sound = Sound.ITEM_BOOK_PAGE_TURN;
			float volume = 0.6f;
			float pitch = 1.3f;
			mPlayer.playSound(location, sound, SoundCategory.NEUTRAL, volume, pitch);
		} else if (type == SoundType.CONFIRM_TRADE) {
			Sound sound = Sound.ENTITY_VILLAGER_TRADE;
			float volume = 1.0f;
			float pitch = 1.5f;
			mPlayer.playSound(location, sound, SoundCategory.NEUTRAL, volume, pitch);
		} else if (type == SoundType.TRADE_ERROR) {
			Sound sound = Sound.ENTITY_ENDERMAN_TELEPORT;
			float volume = 0.6f;
			float pitch = 0.8f;
			mPlayer.playSound(location, sound, SoundCategory.NEUTRAL, volume, pitch);
		} else if (type == SoundType.TRADE_SUCCESS) {
			Sound sound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
			float volume = 1.0f;
			float pitch = 0.8f;
			mPlayer.playSound(location, sound, SoundCategory.NEUTRAL, volume, pitch);
		}
	}

	// Setup:
	private void organizeTradesIfApplicable() {
		// Decide to display trades together or to organize into categories.
		// Store the results in mDisplayTradeTypes and the contents of each mTrade List.
		if (mPebTradeGUITradeOrg == 1) {
			// disabled in peb:
			mDisplayTradeTypes.add(TradeType.GENERAL);
			return;
		}
		if (mTrades.size() <= 10) {
			// too little trades:
			mDisplayTradeTypes.add(TradeType.GENERAL);
			return;
		}
		// Split mTrades -> mTrade Categories:
		organizeTrades();
		// Final test to decide:
		if (mTradeWeapon.size() + mTradeOffhand.size() + mTradeArmor.size() + mTradeCharm.size() < 6) {
			mDisplayTradeTypes.add(TradeType.GENERAL);
			return;
		}
		// Store relevant categories in mDisplayTradeTypes:
		TradeType[] tradeTypes = TradeType.values();
		for (int i = 0; i < 5; i++) {
			// If the category has at least 1 trade, add it to mDisplayTradeTypes.
			// Priority for tab order is: WEAPON, ARMOR, OFFHAND, CHARM, MISC, according to the enum.
			TradeType type = tradeTypes[i];
			List<TradeWindowOpenEvent.Trade> tradeList = tradeTypeToMemberList(type);
			if (tradeList.size() > 0) {
				mDisplayTradeTypes.add(type);
			}
		}
	}

	private void organizeTrades() {
		for (TradeWindowOpenEvent.Trade trade : mTrades) {
			// Loop through each trade and assign it to a sub-list:
			List<TradeWindowOpenEvent.Trade> tradeList = tradeTypeToMemberList(getTradeType(trade));
			tradeList.add(trade);
		}
	}

	private int getMaxPages() {
		List<TradeWindowOpenEvent.Trade> trades = tradeTypeToMemberList(mCurrentTab);
		int maxTradesPerPage = (mPebTradeGUISpacing == 1 ? 16 : 28); // 0 and 2 are auto & 28, 1 is 16.
		return (int) Math.ceil((double) trades.size() / maxTradesPerPage);
	}

	private int getMaxTradeMultiplier(TradeWindowOpenEvent.Trade trade) {
		if (trade == null) {
			return 0;
		}
		int maxTradeMultiplier = 64; // You can buy up to a stack of items
		// Test if ingredients and result can handle new multiplier:
		List<ItemStack> tradeItemList = new TradeReq(mPlayer, trade, 1, true).requirements();
		tradeItemList.add(trade.getRecipe().getResult());
		for (ItemStack tradeReq : tradeItemList) {
			int itemAmount = tradeReq.getAmount();
			int itemTradeMultipler = 64 / itemAmount;
			if (itemTradeMultipler < maxTradeMultiplier) {
				// Take the min of all the itemTradeMultipliers.
				maxTradeMultiplier = itemTradeMultipler;
			}
		}
		return maxTradeMultiplier;
	}

	private ItemStack createTradeMultiplierButton() {
		ItemStack banner = new ItemStack(Material.LIGHT_BLUE_BANNER);
		ItemMeta itemMeta = banner.getItemMeta();
		if (itemMeta instanceof BannerMeta bannerMeta) {
			// Add patterns for right-arrow:
			Pattern pattern1 = new Pattern(DyeColor.WHITE, PatternType.STRIPE_RIGHT);
			Pattern pattern2 = new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE);
			Pattern pattern3 = new Pattern(DyeColor.LIGHT_BLUE, PatternType.STRIPE_TOP);
			Pattern pattern4 = new Pattern(DyeColor.LIGHT_BLUE, PatternType.STRIPE_BOTTOM);
			Pattern pattern5 = new Pattern(DyeColor.LIGHT_BLUE, PatternType.CURLY_BORDER);
			bannerMeta.addPattern(pattern1);
			bannerMeta.addPattern(pattern2);
			bannerMeta.addPattern(pattern3);
			bannerMeta.addPattern(pattern4);
			bannerMeta.addPattern(pattern5);
			// Change name and lore:
			bannerMeta.displayName(Component.text("Trade Multiplier: (" + mSelectedTradeMultiplier + "x)", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			Component[] lore = {Component.text("Left click to decrease, right click to increase.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false), Component.text("Hold shift to offset by 5.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)};
			bannerMeta.lore(Arrays.asList(lore));
			// Hide patterns:
			bannerMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS); // banner patterns are actually the same 'data' as potion effects, lmao
			// Finalize:
			banner.setItemMeta(bannerMeta);
		}
		return banner;
	}

	private ItemStack createConfirmButton(TradeWindowOpenEvent.Trade trade, MerchantRecipe recipe, TradeReq tradeReq) {
		// Decide to confirm or deny:
		ItemStack itemStack = new ItemStack(Material.LIME_DYE);
		if (!tradeReq.status()) {
			itemStack = new ItemStack(Material.BARRIER);
		}
		// Set item and name:
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.displayName(Component.text("Confirm Purchase", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
		if (!tradeReq.status()) {
			itemMeta.displayName(Component.text("Missing material(s)", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		}
		// Set lore:
		ItemStack result = recipe.getResult();
		ItemStack originalResult = trade.getOriginalResult();
		if (originalResult != null) {
			result = originalResult;
		}
		int numItem = result.getAmount() * mSelectedTradeMultiplier;
		int count = trade.getCount();
		if (count > 1) {
			numItem *= count;
		}
		String itemName = ItemUtils.getPlainNameOrDefault(result);
		Component comp = Component.text("Buy " + numItem + " " + itemName + " for: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
		List<Component> confirmLore = new ArrayList<>();
		confirmLore.add(comp);
		confirmLore.addAll(tradeReq.lore());
		itemMeta.lore(confirmLore);
		// Finalize:
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	private ItemStack createTradePreviewGuiItem(MerchantRecipe recipe, TradeReq tradeReq, boolean includePriceInLore, int multiplier) {
		// Create our item:
		ItemStack output = ItemUtils.clone(recipe.getResult());
		ItemMeta itemMeta = output.getItemMeta();
		// Add our custom NBT tag:
		NamespacedKey customTagKey = new NamespacedKey(mPlugin, mCustomTagKey);
		itemMeta.getPersistentDataContainer().set(customTagKey, PersistentDataType.INTEGER, 1);
		// Add price lore (if includePriceInLore AND PEB options allow):
		if (includePriceInLore && mPebTradeGUIPreviewDisplay == 0) {
			// Construct price tag lore:
			List<Component> newLore = new ArrayList<>();
			newLore.add(Component.empty());
			newLore.add(Component.text("Price: ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			newLore.addAll(tradeReq.lore());
			if (mPebTradeGUIConfirm == 1) {
				newLore.add(Component.text("Left-click to buy, Right-click for details.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
			}
			// Add to previous lore (if any):
			List<Component> prevLore = new ArrayList<>();
			if (itemMeta.hasLore()) {
				prevLore = itemMeta.lore();
			}
			prevLore.addAll(newLore);
			itemMeta.lore(prevLore);
		}
		// Finalize and return:
		output.setItemMeta(itemMeta);
		output.setAmount(output.getAmount() * multiplier);
		return output;
	}

	private ItemStack createTradePreviewGuiItem(ItemStack item) {
		// Create our item:
		ItemMeta itemMeta = ItemUtils.clone(item).getItemMeta();
		// Add custom NBT tag:
		NamespacedKey customTagKey = new NamespacedKey(mPlugin, mCustomTagKey);
		itemMeta.getPersistentDataContainer().set(customTagKey, PersistentDataType.INTEGER, 1);
		// Finalize and return:
		item.setItemMeta(itemMeta);
		return item;
	}

	// Utils:
	private TradeType getTradeType(TradeWindowOpenEvent.Trade trade) {
		// Grab recipe and output:
		MerchantRecipe recipe = trade.getRecipe();
		ItemStack output = recipe.getResult();
		// Test for type:
		if (ItemStatUtils.isCharm(output)) {
			return TradeType.CHARM;
		}
		if (ItemUtils.isArmorOrWearable(output)) {
			return TradeType.ARMOR;
		}
		if (ItemUtils.isWand(output) || ItemUtils.isAxe(output) || ItemUtils.isAlchemistItem(output) || ItemUtils.isBowOrTrident(output) || ItemUtils.isHoe(output)) {
			return TradeType.WEAPON;
		}
		if (ItemStatUtils.hasAttributeInSlot(output, Slot.OFFHAND) || ItemStatUtils.hasEnchantment(output, EnchantmentType.OFFHAND_MAINHAND_DISABLE)) {
			return TradeType.OFFHAND;
		}
		if (ItemStatUtils.hasAttributeInSlot(output, Slot.MAINHAND)) {
			return TradeType.WEAPON;
		}
		return TradeType.MISC;
	}

	private List<TradeWindowOpenEvent.Trade> tradeTypeToMemberList(@Nullable TradeType type) {
		if (type == null) {
			return mTrades;
		}
		return switch (type) {
			case WEAPON -> mTradeWeapon;
			case ARMOR -> mTradeArmor;
			case OFFHAND -> mTradeOffhand;
			case CHARM -> mTradeCharm;
			case MISC -> mTradeMisc;
			default -> mTrades;
		};
	}
}
