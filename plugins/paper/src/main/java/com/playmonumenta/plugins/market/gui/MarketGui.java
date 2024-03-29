package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.market.MarketListing;
import com.playmonumenta.plugins.market.MarketManager;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class MarketGui extends Gui {
	protected MarketGuiTab mCurrentTab;

	final MarketGuiTab TAB_NOT_IMPLEMENTED = new TabNotImplemented(this);
	final MarketGuiTab TAB_MAIN_MENU = new TabMainMenu(this);
	final MarketGuiTab TAB_MODERATOR_MENU = new TabModeratorMenu(this);
	final MarketGuiTab TAB_CHOOSE_CURRENCY = new TabChooseCurrency(this);
	final MarketGuiTab TAB_ADD_LISTING = new TabAddListing(this);
	final MarketGuiTab TAB_BUY_LISTING = new TabBuyListing(this);
	final MarketGuiTab TAB_BAZAAR_BROWSER = new TabBazaarBrowser(this);
	final MarketGuiTab TAB_MODERATOR_BROWSER = new TabModeratorBrowser(this);
	final MarketGuiTab TAB_PLAYER_LISTINGS = new TabPlayerListings(this);
	final MarketGuiTab TAB_EDIT_LISTING = new TabEditListing(this);

	// represents the item that is going to be sold
	@Nullable ItemStack mItemToSell = null;

	// represents the amount of item to be sold
	int mAmountToSell = 0;

	// represents the price per item
	int mPricePerItemAmount = 1;

	// represents the currency of the new listing
	@Nullable ItemStack mCurrencyItem = InventoryUtils.getItemFromLootTableOrWarn(mPlayer.getLocation(), NamespacedKeyUtils.fromString("epic:r2/items/currency/compressed_crystalline_shard"));

	// status for the ongling data loading
	// 0 -> no data is loaded
	// 1 -> data is loading the list of listings to be displayed
	// 2 -> list loaded
	// 3 -> list loaded, loading the listings data of the page
	// 4 -> data loaded
	int mIsLoadingData = 0;

	List<Long> mPlayerListingsIds;
	@Nullable MarketListing mFocusedListing;
	int mBuyListingMultiplier;

	boolean mIsOp;

	int mPlayerMaxListings;

	private static final HashSet<String> mMarketOngoingPlayerActions = new HashSet<>();

	public MarketGui(Player player) {
		super(player, 6 * 9, Component.text("Player Market"));
		mIsOp = player.hasPermission("monumenta.command.market");
		mCurrentTab = TAB_MAIN_MENU;
		mPlayerListingsIds = MarketManager.getInstance().getListingsOfPlayer(player);
		mPlayerMaxListings = MarketManager.getConfig().mAmountOfPlayerListingsSlots;
		endPlayerAction(mPlayer);
	}

	@Override
	protected void setup() {
		if (mCurrentTab != null) {
			mCurrentTab.setup();
		}
	}

	void switchToTab(MarketGuiTab tabClass) {
		mCurrentTab.onLeave();
		mCurrentTab = tabClass;
		tabClass.onSwitch();
		update();
	}

	int commonMultiplierSelection(InventoryClickEvent event, int current, int max) {

		int offset = 1;
		if (event.isShiftClick()) {
			offset = 16;
		}

		if (event.getClick() == ClickType.RIGHT) {
			current += offset;
		} else if (event.getClick() == ClickType.LEFT) {
			current -= offset;

		}

		if (current > max) {
			current = 1;
		}
		if (current < 1) {
			current = max;
		}

		return current;
	}

	int modifyTradeMultiplier(int baseModifier, int offset, int maxMultiplier) {
		// Normal trade multiplier:
		if (baseModifier + offset > maxMultiplier) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.NEUTRAL, 0.6f, 0.8f);
			return 1;
		}
		if (baseModifier + offset < 1) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.NEUTRAL, 0.6f, 1.2f);
			return maxMultiplier;
		}
		baseModifier += offset;
		mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.NEUTRAL, 0.6f, 1.3f);
		return baseModifier;
	}

	ItemStack createTradeMultiplierButton(int maxMultiplier) {
		// Regular trade multiplier button:
		ItemStack banner = new ItemStack(Material.LIGHT_BLUE_BANNER);
		ItemMeta itemMeta = banner.getItemMeta();
		if (itemMeta instanceof BannerMeta bannerMeta) {
			// Add patterns for right-arrow:
			bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.STRIPE_RIGHT));
			bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
			bannerMeta.addPattern(new Pattern(DyeColor.LIGHT_BLUE, PatternType.STRIPE_TOP));
			bannerMeta.addPattern(new Pattern(DyeColor.LIGHT_BLUE, PatternType.STRIPE_BOTTOM));
			bannerMeta.addPattern(new Pattern(DyeColor.LIGHT_BLUE, PatternType.CURLY_BORDER));
			// Change name and lore:
			bannerMeta.displayName(Component.text("Trade Multiplier: (" + mBuyListingMultiplier + "x)", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			List<Component> lore = new ArrayList<>();
			lore.add(Component.text("Left click to decrease, right click to increase.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("Hold shift to offset by 8.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			if (mBuyListingMultiplier == 1) {
				lore.add(Component.empty());
				lore.add(Component.text("Left click to calculate your max multiplier.", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			}
			if (mBuyListingMultiplier == maxMultiplier) {
				lore.add(Component.empty());
				lore.add(Component.text("right click to go back to multiplier x1.", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			}
			bannerMeta.lore(lore);
			// Hide patterns:
			bannerMeta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS); // banner patterns are actually the same 'data' as potion effects, lmao
			// Finalize:
			banner.setItemMeta(bannerMeta);
		}
		return banner;
	}

	@Override
	protected void onPlayerInventoryClick(InventoryClickEvent event) {
		ItemStack item = event.getCurrentItem();
		if (mCurrentTab == TAB_ADD_LISTING && !ItemUtils.isNullOrAir(item) && mCurrencyItem != null) {

			List<String> errorMessages = MarketManager.itemIsSellable(mPlayer, event.getCurrentItem(), mCurrencyItem);
			if (!errorMessages.isEmpty()) {
				for (String message : errorMessages) {
					mPlayer.sendMessage(Component.text(message, NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
				}
				mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1, 1);
			} else {
				// Change the item to be sold, when the "Add listing" tab is open
				mAmountToSell = item.getAmount();
				mItemToSell = item.asQuantity(1);
				update();
			}
		}
	}

	static boolean initiatePlayerAction(Player player) {
		if (mMarketOngoingPlayerActions.contains(player.getName())) {
			return true;
		}
		mMarketOngoingPlayerActions.add(player.getName());
		return false;
	}

	static void endPlayerAction(Player player) {
		mMarketOngoingPlayerActions.remove(player.getName());
	}

	/*
	//
	// COMMON ICONS
	//
	*/

	public GuiItem buildChangePageIcon(int mCurrentPage, int maxPage) {
		ArrayList<Component> lore = new ArrayList<>();
		lore.add(Component.text("Right Click to go to the next page.", NamedTextColor.GRAY));
		lore.add(Component.text("Left Click to go to the previous page.", NamedTextColor.GRAY));
		// made this way to facilitate translations later on
		String name = "Page %d of %d";
		String[] nameParts = name.split("%d");
		Component nameComp = Component.text(nameParts[0], NamedTextColor.GOLD)
			.append(Component.text(mCurrentPage + 1))
			.append(Component.text(nameParts[1], NamedTextColor.GOLD))
			.append(Component.text(maxPage).decoration(TextDecoration.OBFUSCATED, maxPage == 0));

		return new GuiItem(GUIUtils.createBasicItem(Material.ARROW, 1, nameComp, lore, true, "gui_changePage"));
	}

	/*
	//
	// COMMON ACTIONS
	//
	*/
}
