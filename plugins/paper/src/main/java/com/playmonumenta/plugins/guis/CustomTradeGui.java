package com.playmonumenta.plugins.guis;

import com.playmonumenta.plugins.inventories.Wallet;
import com.playmonumenta.plugins.inventories.WalletManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import com.playmonumenta.plugins.utils.WalletUtils;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.trades.TradeWindowOpenEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
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
import org.bukkit.event.inventory.ClickType;
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
	// Variables:
	//region <SCOREBOARDS>
	public static final String MAIN = "peb_tradeGUI_main";
	public static final String LOCKED = "peb_tradeGUI_locked";
	public static final String THEME = "peb_tradeGUI_theme";
	public static final String SPACING = "peb_tradeGUI_spacing";
	public static final String PREVIEWDISPLAY = "peb_tradeGUI_previewDisplay";
	public static final String TRADEORG = "peb_tradeGUI_tradeOrg";
	public static final String CONFIRM = "peb_tradeGUI_confirm";
	public static final String QUICKBUY = "peb_tradeGUI_quickBuy";
	public static final String SUCCESS = "peb_tradeGUI_success";
	public static final String PARTICLES = "peb_tradeGUI_particles";
	public static final String SOUNDS = "peb_tradeGUI_sounds";
	public static final String WALLET = "peb_tradeGUI_wallet";
	public static final String PERMISSION = "monumenta.customtradegui";

	// Options:
	// Note: mPeb_tradeGUI_main & mPeb_tradeGUI_locked are also scoreboards, but not tested in here.
	// THEME: 0: classic. 1: sleek.
	private final int mPebTradeGUITheme = ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.THEME).orElse(0);
	// SPACING: 0: auto. 1: force 16. 2: force 28.
	private final int mPebTradeGUISpacing = ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.SPACING).orElse(0);
	// PREVIEWDISPLAY: 0: display price on preview. 1: dont.
	private final int mPebTradeGUIPreviewDisplay = ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.PREVIEWDISPLAY).orElse(0);
	// TRADEORG: 0: split trades by type. 1: dont.
	private final int mPebTradeGUITradeOrg = ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.TRADEORG).orElse(0);
	// CONFIRM: 0: bring up confirm menu. 1: dont.
	private final int mPebTradeGUIConfirm = ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.CONFIRM).orElse(0);
	// QUICKBUY: 0: shift click on preview trade to buy 1. 1: disabled.
	private final int mPebTradeGUIQuickBuy = ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.QUICKBUY).orElse(0);
	// SUCCESS: 0: return to preview upon successful trade. 1: close gui. 2: do nothing.
	private final int mPebTradeGUISuccess = ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.SUCCESS).orElse(2);
	// PARTICLES: 0: particles on. 1: off.
	private final int mPebTradeGUIParticles = ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.PARTICLES).orElse(0);
	// SOUNDS: 0: sounds on. 1: off.
	private final int mPebTradeGUISounds = ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.SOUNDS).orElse(0);
	// WALLET: 0: enabled, prioritize inventory. 1: disabled. 2: enabled, prioritize wallet.
	private final int mPebTradeGUIWallet = ScoreboardUtils.getScoreboardValue(mPlayer, CustomTradeGui.WALLET).orElse(0);
	//endregion

	//region <FINAL_VARS>
	private final List<TradeWindowOpenEvent.Trade> mTrades;
	private final @Nullable Villager mVillager;
	private final String mTitle;
	private final String mCustomTagKey = "trade_preview";
	private final NamespacedKey mCustomTagNamespacedKey = new NamespacedKey(mPlugin, mCustomTagKey);
	private final boolean mGuiTagsActive = GUIUtils.getGuiTextureObjective(mPlayer);
	private final ItemStack mBorderPane =
		(mPebTradeGUITheme == 0) ?
			GUIUtils.setGuiNbtTag(
				GUIUtils.createBasicItem(Material.BROWN_STAINED_GLASS_PANE, "", NamedTextColor.GRAY, false,
					"", NamedTextColor.GRAY, 0), "texture", "gui_trade_filler", mGuiTagsActive
			) :
			GUIUtils.FILLER;
	private static final int GUI_ID_LOC_L = 0;
	private static final int GUI_ID_LOC_R = 8;
	//endregion

	//region <CACHED_VARS>
	private final List<TradeType> mDisplayTradeTypes = new ArrayList<>();
	private final List<TradeWindowOpenEvent.Trade> mTradeMisc = new ArrayList<>();
	private final List<TradeWindowOpenEvent.Trade> mTradeWeapon = new ArrayList<>();
	private final List<TradeWindowOpenEvent.Trade> mTradeOffhand = new ArrayList<>();
	private final List<TradeWindowOpenEvent.Trade> mTradeArmor = new ArrayList<>();
	private final List<TradeWindowOpenEvent.Trade> mTradeCharm = new ArrayList<>();
	private final Map<List<ItemStack>, TradeStatusWrapper> mSavedBaseRequirements = new HashMap<>(); // Save the result of base requirement checks as they can be intensive.
	//endregion

	//region <GUI_VARS>
	private @Nullable TradeWindowOpenEvent.Trade mSelectedTrade = null;
	private int mSelectedTradeMultiplier = 1;
	private int mSelectedTradeMaxMultiplier = 64;
	private @Nullable TradeType mCurrentTab = null;
	private int mCurrentPage = 1;
	private boolean mShowAllTrades = true; // toggle between showing all trades versus the ones you can currently buy.
	//endregion

	//region <SUBCLASSES>
	private enum TradeType {
		WEAPON("Weapons", "trade_menu_weapons"), ARMOR("Armor", "trade_menu_armor"), OFFHAND("Offhands", "trade_menu_offhands"), CHARM("Charms", "trade_menu_charms"), MISC("Misc", "trade_menu_misc"), GENERAL("Trades", "trade_menu_trade");

		public final String mName;
		public final String mTag;

		TradeType(String name, String tag) {
			this.mName = name;
			this.mTag = tag;
		}
	}

	private enum SoundType {
		TAB_SELECT, PAGE_FLIP, CONFIRM_TRADE, TRADE_ERROR, TRADE_SUCCESS, TRADE_MULTIPLIER
	}

	private class TradeReq {
		// This class holds the status of the player's requirements for a specific trade.
		private List<Component> mLore = new ArrayList<>();
		private final List<ItemStack> mRequirements = new ArrayList<>();
		private final int mNumRequirements;
		private final int mMultiplier;
		private boolean mHasRequirements = true;
		private final TradeWindowOpenEvent.Trade mTrade;

		public TradeReq(Player player, TradeWindowOpenEvent.Trade trade, int multiplier, boolean useCache) {
			// Store trade:
			mTrade = trade;
			mMultiplier = multiplier;
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
				// Check for locked trades:
				mHasRequirements = mHasRequirements && handleMaxUses(trade, multiplier, mLore);
				return;
			}
			// Copy of player's inventory and wallet (if enabled):
			ItemStack[] inventoryShallowClone = player.getInventory().getStorageContents().clone();
			Wallet walletClone = (mPebTradeGUIWallet == 1) ? null : WalletManager.getWallet(player).deepClone();
			// Check each requirement, constructing lore and updating mHasRequirements:
			for (ItemStack requirement : mRequirements) {
				// Calculate amount to remove:
				WalletUtils.Debt debt = WalletUtils.calculateInventoryAndWalletDebt(requirement, inventoryShallowClone, walletClone, mPebTradeGUIWallet != 0);
				int inventoryDebt = debt.mInventoryDebt;
				int walletDebt = debt.mWalletDebt;
				long numInWallet = debt.mNumInWallet;
				boolean meetsRequirement = debt.mMeetsRequirement;
				// Remove from inventory and wallet clones:
				if (meetsRequirement && inventoryDebt > 0) {
					InventoryUtils.removeItemFromArray(inventoryShallowClone, requirement.asQuantity(inventoryDebt));
				}
				if (meetsRequirement && walletDebt > 0 && walletClone != null) {
					walletClone.remove(player, requirement.asQuantity(walletDebt));
				}
				// Construct lore:
				String plainName = ItemUtils.getPlainNameOrDefault(requirement);
				mLore.add(
					Component.text(requirement.getAmount() + " " + plainName + " ", NamedTextColor.WHITE).append(
						Component.text(meetsRequirement ? "✓" : "✗", (meetsRequirement ? NamedTextColor.GREEN : NamedTextColor.RED))).append(
						Component.text(walletDebt > 0 ? " (" + numInWallet + " in wallet)" : "", NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false));
				// Update total requirement status:
				mHasRequirements = mHasRequirements && meetsRequirement;
			}
			// Save the base requirement to our map:
			boolean tradeAvailable = handleMaxUses(trade, multiplier, mLore);
			if (multiplier == 1 && tradeAvailable) {
				mSavedBaseRequirements.put(mRequirements, new TradeStatusWrapper(mHasRequirements, mLore));
			}
			// Check for locked trades:
			mHasRequirements = mHasRequirements && tradeAvailable;
		}

		public List<Component> lore() {
			return mLore;
		}

		public List<ItemStack> requirements() {
			return mRequirements;
		}

		public List<ItemStack> requirementsAsBase() {
			// Returns a list of requirements as if multiplier was 1:
			if (mMultiplier == 1) {
				return mRequirements;
			}
			return mRequirements.stream().map((ItemStack requirement) -> {
				ItemStack clone = requirement.clone();
				clone.setAmount(requirement.getAmount() / mMultiplier);
				return clone;
			}).toList();
		}

		public int size() {
			return mNumRequirements;
		}

		public boolean status() {
			return mHasRequirements;
		}

		public TradeWindowOpenEvent.Trade getTrade() {
			return mTrade;
		}

		public void removeRequirements() {
			// Remove requirements from actual inventory and wallet:
			Inventory inventory = mPlayer.getInventory();
			Wallet wallet = (mPebTradeGUIWallet == 1) ? null : WalletManager.getWallet(mPlayer);
			for (ItemStack requirement : requirements()) {
				// Calculate amount to remove:
				WalletUtils.Debt debt = WalletUtils.calculateInventoryAndWalletDebt(requirement, inventory.getStorageContents(), wallet, mPebTradeGUIWallet != 0);
				int inventoryDebt = debt.mInventoryDebt;
				int walletDebt = debt.mWalletDebt;
				boolean meetsRequirement = debt.mMeetsRequirement;

				// Remove from inventory and wallet:
				if (meetsRequirement && inventoryDebt > 0) {
					inventory.removeItem(requirement.asQuantity(inventoryDebt));
				}
				if (meetsRequirement && walletDebt > 0 && wallet != null) {
					wallet.remove(mPlayer, requirement.asQuantity(walletDebt));
				}
				// Notify player:
				WalletUtils.notifyRemovalFromWallet(debt, mPlayer);

				// Check for errors:
				if (!meetsRequirement) {
					mPlayer.sendMessage("We're sorry - there was a problem verifying a requirement: '" +
						                    ItemUtils.getPlainNameOrDefault(requirement) +
						                    "'. Please contact a moderator if a refund is needed.");
					MMLog.warning("Custom Trade GUI: requirement - removal mismatch @buyNow: " + mTitle);
					close();
					return;
				}
			}
		}

		private static boolean handleMaxUses(TradeWindowOpenEvent.Trade trade, int multiplier, List<Component> mLore) {
			// Returns if trade is available or not:
			if (trade.getRecipe().getMaxUses() < (trade.getRecipe().getUses() + multiplier)) {
				mLore.add(0, Component.text("Out of Stock", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
				for (int i = 1; i < mLore.size(); i++) {
					mLore.set(i, mLore.get(i).color(NamedTextColor.GRAY).decoration(TextDecoration.STRIKETHROUGH, true));
				}
				return false;
			}
			return true;
		}
	}

	private static class TradeStatusWrapper {
		private final boolean mHasRequirements;
		private final List<Component> mLore;


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
	//endregion

	// Functions:
	//region <SETUP>
	public CustomTradeGui(Player player, @Nullable Villager villager, Component title, List<TradeWindowOpenEvent.Trade> trades) {
		super(player, 6 * 9, title);
		mVillager = villager;
		mTrades = trades;
		mTitle = MessagingUtils.plainText(title);
		// Setup Trade Lists:
		organizeTrades();
	}

	@Override
	protected void setup() {
		// Border panes:
		if (mPebTradeGUITheme == 0) {
			for (int i = 0; i < 9; i++) {
				setItem(0, i, mBorderPane);
				setItem(5, i, mBorderPane);
			}
			for (int i = 1; i < 5; i++) {
				setItem(i, 0, mBorderPane);
				setItem(i, 8, mBorderPane);
			}
		}
		// Background panes:
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
	//endregion

	//region <TABS>
	/*
	 * Overview: there are 3 states this GUI can be in:
	 *   1: Locked (openLockedTradeView): if a player has the 'locked' score. (currently unused).
	 *   2: Preview (openGeneralTradeView): the default, occurs when there is no selected trade (mSelectedTrade == null).
	 *   3: Confirm (openConfirmTradeView): confirmation screen for a specific trade (mSelectedTrade != null).
	 * */
	private void openLockedTradeView() {
		setItem(0, 4, GUIUtils.createBasicItem(Material.OAK_SIGN, "Error: ", NamedTextColor.BLUE, false,
			"You don't meet the requirements to view these trades!", NamedTextColor.GRAY, 20));
		ItemStack chain = GUIUtils.createBasicItem(Material.IRON_BARS, "", NamedTextColor.GRAY, false,
			"", NamedTextColor.GRAY, 0);
		// Amogus:
		setItem(1, 3, chain);
		setItem(1, 4, chain);
		setItem(1, 5, chain);
		setItem(2, 5, chain);
		setItem(2, 6, chain);
		setItem(3, 3, chain);
		setItem(3, 4, chain);
		setItem(3, 5, chain);
		setItem(3, 6, chain);
		setItem(4, 3, chain);
		setItem(4, 5, chain);
	}

	private void openGeneralTradeView() {
		// Initialize the current tab:
		if (mCurrentTab == null) {
			mCurrentTab = mDisplayTradeTypes.get(0);
		}

		// Load trades for the current tab:
		int numTrades = showTrades(mCurrentTab);
		int pageCount = getMaxPages(numTrades);
		// Display header and icons for all tabs:
		setItem(0, 4,
			GUIUtils.setGuiNbtTag(
				GUIUtils.createBasicItem(Material.OAK_SIGN, "Viewing: ", NamedTextColor.BLUE, false,
					"Tab: " + ((mCurrentTab != null) ? mCurrentTab.mName : "") + "\nPage: " + mCurrentPage + "/" + pageCount, NamedTextColor.GRAY, 20),
				"texture", "trade_menu_help", mGuiTagsActive));
		int guiCol = 1;
		for (TradeType tradeType : mDisplayTradeTypes) {
			boolean isSelected = (mCurrentTab == tradeType);
			String name = tradeType.mName + (isSelected ? " (Selected)" : "");
			String tag = tradeType.mTag + (isSelected ? "_selected" : "");
			setItem(5, guiCol,
				GUIUtils.setGuiNbtTag(
					GUIUtils.createBasicItem(
						(isSelected ? Material.BLUE_STAINED_GLASS_PANE : Material.CYAN_STAINED_GLASS_PANE),
						name, NamedTextColor.YELLOW, false, "", NamedTextColor.GRAY, 0), "texture", tag, mGuiTagsActive))
				.onLeftClick(() -> {
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
		// Display page icons:
		if (mCurrentPage > pageCount) {
			// Failsafe for a glitch or something:
			mCurrentPage = pageCount;
		}
		if (mCurrentPage < pageCount) {
			// NEXT PAGE BUTTON
			setItem(5, 7,
				GUIUtils.setGuiNbtTag(GUIUtils.createBasicItem(Material.ARROW, "Next Page (" + (mCurrentPage + 1) + ")", NamedTextColor.YELLOW, false,
					"", NamedTextColor.GRAY, 0), "texture", "next_page", mGuiTagsActive)
				).onLeftClick(() -> {
				// Page Flip:
				mCurrentPage++;
				playSound(mPlayer.getLocation(), SoundType.PAGE_FLIP);
				update();
			});
		}
		if (mCurrentPage > 1) {
			// PREV PAGE BUTTON
			setItem(5, 6,
				GUIUtils.setGuiNbtTag(GUIUtils.createBasicItem(Material.ARROW, "Previous Page (" + (mCurrentPage - 1) + ")", NamedTextColor.YELLOW, false,
					"", NamedTextColor.GRAY, 0), "texture", "prev_page", mGuiTagsActive)
				).onLeftClick(() -> {
				// Page Flip:
				mCurrentPage--;
				playSound(mPlayer.getLocation(), SoundType.PAGE_FLIP);
				update();
			});
		}
		// Button to toggle between showing all trades or trade you can buy:
		Material material = mShowAllTrades ? Material.AMETHYST_CLUSTER : Material.MEDIUM_AMETHYST_BUD;
		String name = mShowAllTrades ? "Showing: All Trades" : "Showing: Trades You Can Buy";
		String tag = mShowAllTrades ? "trade_menu_show_all" : "trade_menu_show_affordable";
		setItem(5, 8,
			GUIUtils.setGuiNbtTag(
				GUIUtils.createBasicItem(material, name, NamedTextColor.YELLOW, true,
					"Click to toggle. ", NamedTextColor.GRAY, 20), "texture", tag, mGuiTagsActive))
			.onLeftClick(() -> {
			// Page Flip:
			mShowAllTrades = !mShowAllTrades;
			mCurrentPage = 1;
			playSound(mPlayer.getLocation(), SoundType.PAGE_FLIP);
			update();
		});
		// RP Support: gui identifiers.
		setGuiIdentifiers();
	}

	private void openConfirmTradeView() {
		if (mSelectedTrade == null) {
			// Note: these error messages are similar to the message you get if you access a village UI you aren't supposed to -
			// None will pretty much never happen, but just adding a failsafe + letting the player know.
			mPlayer.sendMessage("Something went wrong - if this keeps happening, please report it!");
			MMLog.warning("Custom Trade GUI: null trade at: " + mTitle);
			close();
			return;
		}
		// Vars:
		MerchantRecipe recipe = mSelectedTrade.getRecipe();
		String itemName = ItemUtils.getPlainNameOrDefault(recipe.getResult());
		TradeReq tradeReq = new TradeReq(mPlayer, mSelectedTrade, mSelectedTradeMultiplier, true);
		// Header:
		setItem(0, 4,
			GUIUtils.setGuiNbtTag(
				GUIUtils.createBasicItem(Material.OAK_SIGN, "Viewing: ", NamedTextColor.BLUE, false,
					itemName, NamedTextColor.GRAY, 20),
				"texture", "trade_confirm_help", mGuiTagsActive));
		// Custom Multipliers: display the base trade (multiplier of 1):
		boolean displayAsBase = (mSelectedTradeMultiplier > mSelectedTradeMaxMultiplier);
		int displayMultiplier = displayAsBase ? 1 : mSelectedTradeMultiplier;
		// Trade result:
		setItem(2, 6, createTradePreviewGuiItem(recipe, tradeReq, false, displayMultiplier));
		// Trade requirement(s):
		int numRequirements = tradeReq.size();
		if (numRequirements <= 0) {
			mPlayer.sendMessage("Something went wrong - if this keeps happening, please report it!");
			MMLog.warning("Custom Trade GUI: trade with no requirements at: " + mTitle + ", " + mSelectedTrade);
			close();
			return;
		}
		List<ItemStack> requirements = displayAsBase ? tradeReq.requirementsAsBase() : tradeReq.requirements();
		if (numRequirements == 1) {
			setItem(2, 2, createTradePreviewGuiItem(requirements.get(0)));
		} else if (numRequirements == 2) {
			setItem(2, 3, createTradePreviewGuiItem(requirements.get(1)));
			setItem(2, 2, createTradePreviewGuiItem(requirements.get(0)));
		} else {
			mPlayer.sendMessage("Something went wrong - if this keeps happening, please report it!");
			MMLog.warning("Custom Trade GUI: trade with too many requirements at: " + mTitle + ", " + mSelectedTrade);
			close();
			return;
		}
		// Trade Multiplier Button:
		setItem(2, 4, createTradeMultiplierButton())
			.onClick((inventoryClickEvent) -> {
				int offset = 1;
				if (inventoryClickEvent.isShiftClick()) {
					offset = 8;
				}
				if (inventoryClickEvent.isLeftClick()) {
					// Left click, decrease multiplier:
					modifyTradeMultiplier(-offset);
				} else if (inventoryClickEvent.isRightClick()) {
					// Right click, increase multiplier:
					modifyTradeMultiplier(offset);
				} else if (inventoryClickEvent.getClick() == ClickType.SWAP_OFFHAND) {
					// Swap, custom multiplier:
					openTradeMultiplierSignMenu((Integer multiplier) -> {
						// onSuccess:
						if (multiplier == 0) {
							playSound(mPlayer.getLocation(), SoundType.TAB_SELECT);
						} else {
							mSelectedTradeMultiplier = multiplier;
							playSound(mPlayer.getLocation(), SoundType.TRADE_MULTIPLIER);
						}
						open();
					});
				}
				update();
			});
		// Back Button:
		ItemStack backButton = createBackButton(tradeReq.status());
		setItem(4, 3, backButton).onLeftClick(this::navToGeneralView);
		// Confirm/Deny Button:
		if (tradeReq.status()) {
			setItem(4, 5, createConfirmButton(mSelectedTrade, recipe, tradeReq)).onLeftClick(() -> {
				buyNow(mSelectedTrade, mSelectedTradeMultiplier);
			});
		} else {
			setItem(4, 5, createConfirmButton(mSelectedTrade, recipe, tradeReq));
		}
		// RP Support: gui identifiers.
		setGuiIdentifiers();
	}
	//endregion

	//region <NAVIGATION>
	/*
	* Overview: logic bundles for transferring between each state (see TABS overview).
	* */

	private void navToConfirmView(TradeWindowOpenEvent.Trade trade) {
		mSelectedTrade = trade;
		mSelectedTradeMaxMultiplier = getMaxTradeMultiplier(trade);
		playSound(mPlayer.getLocation(), SoundType.CONFIRM_TRADE);
		update();
	}

	private void navToGeneralView() {
		mSelectedTrade = null;
		mSelectedTradeMultiplier = 1;
		update();
	}
	//endregion

	//region <ACTIONS>
	/*
	* Overview: logic bundles for common actions:
	* */
	private void buyNow(@Nullable TradeWindowOpenEvent.Trade trade, int multiplier) {
		if (trade == null) {
			mPlayer.sendMessage("Something went wrong - if this keeps happening, please report it!");
			MMLog.warning("Custom Trade GUI: null trade @buyNow: " + mTitle);
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
		tradeReq.removeRequirements();
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
			navToGeneralView();
		} else {
			update();
		}
	}

	private void clearIllegalItems() {
		Inventory inventory = mPlayer.getInventory();
		for (ItemStack itemStack : inventory.getContents()) {
			if (itemStack != null) {
				ItemMeta itemMeta = itemStack.getItemMeta();
				// Check if the custom tag exists on the item meta
				if (itemMeta.getPersistentDataContainer().has(mCustomTagNamespacedKey, PersistentDataType.INTEGER)) {
					// Custom tag exists, clear the item
					inventory.remove(itemStack);
				}
			}
		}
	}

	private void modifyTradeMultiplier(int offset) {
		// Max trade multiplier:
		if (mSelectedTradeMultiplier == 1 && offset == -1) {
			mSelectedTradeMultiplier = calculateTradeMultiplierFromInventory();
			if (mSelectedTradeMultiplier == 0) {
				mSelectedTradeMultiplier = 1;
				playSound(mPlayer.getLocation(), SoundType.TRADE_ERROR);
			} else {
				playSound(mPlayer.getLocation(), SoundType.TRADE_MULTIPLIER);
			}
			return;
		}
		if (mSelectedTradeMultiplier > mSelectedTradeMaxMultiplier) {
			mSelectedTradeMultiplier = 1;
			playSound(mPlayer.getLocation(), SoundType.PAGE_FLIP);
			return;
		}

		// Normal trade multiplier:
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
		if (mSelectedTradeMultiplier == 1 && offset > 1) {
			// This way, mSelectedTradeMultiplier stays a multiple of 'offset' on the first click.
			mSelectedTradeMultiplier += offset - 1;
		} else {
			mSelectedTradeMultiplier += offset;
		}
		playSound(mPlayer.getLocation(), SoundType.PAGE_FLIP);
	}

	private void openTradeMultiplierSignMenu(Consumer<Integer> onSuccess) {
		close();
		SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter your", "desired multiplier"))
			.response((player, lines) -> {
				// Retrieves and returns a custom trade multiplier as per player input.
				// Calls onSuccess with a valid multiplier, or 0 if canceled.

				// Cancel with no input:
				if (lines[0].isEmpty()) {
					onSuccess.accept(0);
					return true;
				}
				// Parse input:
				double retrievedMultiplier;
				try {
					retrievedMultiplier = WalletManager.parseDoubleOrCalculation(lines[0]);
				} catch (NumberFormatException e) {
					player.sendMessage(Component.text("Please enter a valid number.", NamedTextColor.RED));
					return false;
				}
				int multiplier = (int) retrievedMultiplier;
				if (multiplier * 64 < 0) {
					// Prevent possible overflow of (base requirement amount * multiplier):
					multiplier = Integer.MAX_VALUE / 64;
				}
				if (multiplier <= 0) {
					player.sendMessage(Component.text("Please enter a positive number.", NamedTextColor.RED));
					return false;
				}

				// Return new multiplier:
				onSuccess.accept(multiplier);
				return true;
			})
			.reopenIfFail(true)
			.open(mPlayer);
	}

	private void playSound(Location location, SoundType type) {
		if (mPebTradeGUISounds == 1) {
			return;
		}
		if (type == SoundType.TAB_SELECT) {
			Sound sound = Sound.BLOCK_WOODEN_BUTTON_CLICK_ON;
			float volume = 1.0f;
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
		} else if (type == SoundType.TRADE_MULTIPLIER) {
			Sound sound = Sound.BLOCK_NOTE_BLOCK_COW_BELL;
			float volume = 1.0f;
			float pitch = 1.0f;
			mPlayer.playSound(location, sound, SoundCategory.NEUTRAL, volume, pitch);
		}
	}
	//endregion

	//region <SETUP_UTILS>
	/*
	* Overview: functions called during GUI setup.
	* */
	private void organizeTrades() {
		// Decide to display trades together or to organize into categories.
		// Store the results in mDisplayTradeTypes and the contents of each mTrade List.
		if (mPebTradeGUITradeOrg == 1) {
			// disabled in peb:
			mDisplayTradeTypes.add(TradeType.GENERAL);
			return;
		}
		if (mTrades.size() <= 28) {
			// too little trades:
			mDisplayTradeTypes.add(TradeType.GENERAL);
			return;
		}
		// Split mTrades -> mTrade Categories:
		for (TradeWindowOpenEvent.Trade trade : mTrades) {
			// Loop through each trade and assign it to a sub-list:
			List<TradeWindowOpenEvent.Trade> tradeList = tradeTypeToMemberList(getTradeType(trade));
			tradeList.add(trade);
		}
		// If there is more than 1 empty list, opt for displaying together:
		int emptyListCount = 0;
		if (mTradeWeapon.isEmpty()) {
			emptyListCount++;
		}
		if (mTradeOffhand.isEmpty()) {
			emptyListCount++;
		}
		if (mTradeArmor.isEmpty()) {
			emptyListCount++;
		}
		if (emptyListCount > 1) {
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

	private int showTrades(TradeType type) {
		// Handle the displaying of trades, return the number of trades displayed.

		// Find the right trades list:
		List<TradeWindowOpenEvent.Trade> tradeList = tradeTypeToMemberList(type);
		// Create a list of filtered trade (requirement objects):
		List<TradeReq> trades =
			tradeList.stream()
				.map(trade -> new TradeReq(mPlayer, trade, 1, true))
				.filter(tradeReq -> mShowAllTrades || tradeReq.status())
				.toList();
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
					MMLog.warning("Custom Trade GUI: overflow trades: " + mTitle + ", " + trades.get(i));
					close();
					return 0;
				} else {
					guiRow++;
					guiCol = 1;
				}
			}
			// Trade preview logic:
			TradeReq tradeReq = trades.get(i);
			TradeWindowOpenEvent.Trade trade = tradeReq.getTrade();
			setItem(guiRow, guiCol, createTradePreviewGuiItem(trade.getRecipe(), tradeReq, true, 1))
				.onClick((inventoryClickEvent) -> {
					// Buy now:
					if ((inventoryClickEvent.isLeftClick() && mPebTradeGUIConfirm != 0) || (inventoryClickEvent.isShiftClick() && mPebTradeGUIQuickBuy == 0)) {
						if (tradeReq.status()) {
							buyNow(trade, 1);
						} else {
							playSound(mPlayer.getLocation(), SoundType.TRADE_ERROR);
						}
					} else if (inventoryClickEvent.getClick() == ClickType.SWAP_OFFHAND) {
						// Buy w/ custom multiplier:
						openTradeMultiplierSignMenu((Integer multiplier) -> {
							// onSuccess:
							if (multiplier == 0) {
								playSound(mPlayer.getLocation(), SoundType.TAB_SELECT);
							} else {
								mSelectedTrade = trade;
								mSelectedTradeMultiplier = multiplier;
								playSound(mPlayer.getLocation(), SoundType.TRADE_MULTIPLIER);
							}
							open();
						});
					} else {
						// Default: Open confirm screen:
						navToConfirmView(trade);
					}
					update();
				});
			guiCol += spacer;
		}
		return numTrades;
	}

	private int getMaxPages(int tradeCount) {
		int maxTradesPerPage = (mPebTradeGUISpacing == 1 ? 16 : 28); // 0 and 2 are auto & 28, 1 is 16.
		return (int) Math.ceil((double) tradeCount / maxTradesPerPage);
	}

	private int getMaxTradeMultiplier(TradeWindowOpenEvent.Trade trade) {
		// Return a trade's maximum multiplier the confirm menu can display normally.
		// This means that (each of the two requirement slots) and (the trade result slot) cannot exceed 64.
		// For example, if a trade is 16 cxp -> 2 dirt, this function returns a multiplier of 4.

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

	private int calculateTradeMultiplierFromInventory() {
		// Returns the maximum multiplier the player can afford (based on their inventory only).

		if (mSelectedTrade == null) {
			return 0;
		}
		// Calculate:
		int maxTradeMultiplier = 0;
		ItemStack[] inventoryContents = mPlayer.getInventory().getStorageContents();
		List<ItemStack> tradeItemList = new TradeReq(mPlayer, mSelectedTrade, 1, true).requirements();
		// Combine similar requirements when checking:
		ItemStack req1 = tradeItemList.get(0);
		if (tradeItemList.size() == 2 && tradeItemList.get(1).isSimilar(req1)) {
			req1.setAmount(req1.getAmount() + tradeItemList.get(1).getAmount());
			tradeItemList.remove(1);
		}
		//
		for (ItemStack tradeReq : tradeItemList) {
			int reqAmount = tradeReq.getAmount();
			int numInInventory = InventoryUtils.numInInventory(inventoryContents, tradeReq);
			int reqMultiplier = numInInventory / reqAmount;
			if (maxTradeMultiplier == 0 || maxTradeMultiplier > reqMultiplier) {
				maxTradeMultiplier = reqMultiplier;
			}
		}
		return maxTradeMultiplier;
	}
	//endregion

	//region <UI_UTILS>
	/*
	 * Overview: functions to display commonly used GUI items.
	 * */
	private ItemStack createTradeMultiplierButton() {
		if (mSelectedTradeMultiplier > mSelectedTradeMaxMultiplier) {
			// Custom trade multiplier button:
			ItemStack book = new ItemStack(Material.KNOWLEDGE_BOOK);
			ItemMeta itemMeta = book.getItemMeta();
			// Change name and lore:
			itemMeta.displayName(Component.text("Trade Multiplier: (" + mSelectedTradeMultiplier + "x)", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			Component[] lore = {
				Component.text("A custom multiplier.", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
				Component.empty(),
				Component.text("Swap to enter a custom multiplier.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text("Click to return.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)};
			itemMeta.lore(Arrays.asList(lore));
			// Finalize:
			book.setItemMeta(itemMeta);
			GUIUtils.setGuiNbtTag(book, "texture", "trade_confirm_multiplier", mGuiTagsActive);
			return book;
		}
		// Regular trade multiplier button:
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
			List<Component> lore = new ArrayList<>();
			if (mSelectedTradeMultiplier == 1) {
				lore.add(Component.text("Left click to calculate your max multiplier.", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
				lore.add(Component.empty());
				lore.add(Component.text("Right click to increase.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

			} else {
				lore.add(Component.empty());
				lore.add(Component.text("Left click to decrease, right click to increase.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			}
			lore.add(Component.text("Hold shift to offset by 8.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("Swap to enter a custom multiplier.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

			bannerMeta.lore(lore);
			// Hide patterns:
			bannerMeta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS); // banner patterns are actually the same 'data' as potion effects, lmao
			// Finalize:
			banner.setItemMeta(bannerMeta);
			GUIUtils.setGuiNbtTag(banner, "texture", "trade_confirm_multiplier", mGuiTagsActive);
		}
		return banner;
	}

	private ItemStack createConfirmButton(TradeWindowOpenEvent.Trade trade, MerchantRecipe recipe, TradeReq tradeReq) {
		// Create lore:
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
		// Set item material and name:
		boolean canAfford = tradeReq.status();
		String tag = canAfford ? "trade_confirm_confirm" : "trade_confirm_unaffordable";
		Material material = canAfford ? Material.GREEN_STAINED_GLASS_PANE : Material.BARRIER;
		Component name = Component.text((canAfford ? "Confirm" : "Missing material(s)"), (canAfford ? NamedTextColor.GREEN : NamedTextColor.RED)).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true);
		return GUIUtils.setGuiNbtTag(
			GUIUtils.createBasicItem(material, 1, name, confirmLore, false, null), "texture", tag, mGuiTagsActive);
	}

	private ItemStack createBackButton(boolean canAfford) {
		// Set item material and name:
		String tag = canAfford ? "trade_confirm_cancel" : "trade_confirm_back";
		Material material = canAfford ? Material.ORANGE_STAINED_GLASS_PANE : Material.ARROW;
		String name = canAfford ? "Cancel" : "Back";
		return GUIUtils.setGuiNbtTag(
			GUIUtils.createBasicItem(material, name, NamedTextColor.GRAY, false,
				"Return to the previous page.", NamedTextColor.GRAY, 40), "texture", tag, mGuiTagsActive);
	}

	private ItemStack createTradePreviewGuiItem(MerchantRecipe recipe, TradeReq tradeReq, boolean includePriceInLore, int multiplier) {
		// Create our item:
		ItemStack output = ItemUtils.clone(recipe.getResult());
		ItemMeta itemMeta = output.getItemMeta();
		// Add our custom NBT tag:
		itemMeta.getPersistentDataContainer().set(mCustomTagNamespacedKey, PersistentDataType.INTEGER, 1);
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
				prevLore.addAll(Objects.requireNonNull(itemMeta.lore()));
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
		itemMeta.getPersistentDataContainer().set(mCustomTagNamespacedKey, PersistentDataType.INTEGER, 1);
		// Finalize and return:
		item.setItemMeta(itemMeta);
		return item;
	}

	private void setGuiIdentifiers() {
		// Sets filler with tag for rp gui support.
		// Different tags depending on if in general trade view or confirm trade view.
		// Different base item depending on theme.
		boolean isGeneral = (mSelectedTrade == null);
		String tagL = isGeneral ? "gui_trade_1_l" : "gui_trade_2_l";
		String tagR = isGeneral ? "gui_trade_1_r" : "gui_trade_2_r";
		setItem(GUI_ID_LOC_L, GUIUtils.createGuiIdentifierItem(mBorderPane, tagL, mGuiTagsActive));
		setItem(GUI_ID_LOC_R, GUIUtils.createGuiIdentifierItem(mBorderPane, tagR, mGuiTagsActive));
	}
	//endregion

	//region <OTHER_UTILS>
	/*
	* Overview: commonly used functions.
	* */
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
	//endregion
}
