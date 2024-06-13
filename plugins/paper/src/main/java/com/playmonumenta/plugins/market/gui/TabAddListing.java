package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.market.MarketManager;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.WalletUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class TabAddListing implements MarketGuiTab {

	MarketGui mGui;

	static final Component TAB_TITLE = Component.text("New Bazaar Listing");
	static final int TAB_SIZE = 4 * 9;

	Player mPlayer;

	// listings owned by the player
	List<Long> mPlayerListingsIds;

	// represents the item that is going to be sold
	@Nullable ItemStack mItemToSell = null;

	// represents the amount of trades the lisitng is able to do
	int mAmountOfTrades = 1;
	// represents the amount of items traded per trade
	int mItemsPerTrade = 1;
	// mAmountOfTrades * mItemsPerTrade = total items in the listing

	// represents the currency of the new listing
	ItemStack mCurrencyItem;

	// represents the price per item
	int mPricePerTradeAmount = 1;

	public TabAddListing(MarketGui marketGUI) {
		this.mGui = marketGUI;
		this.mPlayer = mGui.mPlayer;
		this.mCurrencyItem = InventoryUtils.getItemFromLootTableOrThrow(mPlayer.getLocation(), NamespacedKeyUtils.fromString("epic:r1/items/currency/concentrated_experience"));
	}

	@Override
	public void setup() {
		mPlayerListingsIds = MarketManager.getInstance().getListingsOfPlayer(mPlayer);
		if (mPlayerListingsIds.size() >= MarketManager.getConfig().mAmountOfPlayerListingsSlots) {
			mPlayer.sendMessage(Component.text("You do not have enough slots to add a new listing", NamedTextColor.RED));
			mGui.switchToTab(mGui.TAB_PLAYER_LISTINGS);
			return;
		}

		// Minimum value of price per item is 1
		if (mPricePerTradeAmount < 1) {
			mPricePerTradeAmount = 1;
		}
		// Minimum items per trade is 1
		if (mItemsPerTrade < 1) {
			mItemsPerTrade = 1;
		}
		// Minimum amount of trades is 1
		if (mAmountOfTrades < 1) {
			mAmountOfTrades = 1;
		}

		// Clamp the amount to between 1 and number of items in player inventory
		clampAmountToSellToAmountInInventory();


		mGui.setItem(0, 0, MarketGuiIcons.BACK_TO_MAIN_MENU).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_MAIN_MENU));


		mGui.setItem(1, 1, new GuiItem(buildAddListingItemToBeSoldIcon(), false))
			.onClick((clickEvent) -> {
				mItemToSell = null;
				mGui.update();
			});


		// items per trade amount multiplier
		mGui.setItem(2, 1, buildAddListingItemsPerTradeMultiplierIcon())
			.onClick(this::changeItemsPerTradeAction);

		// trade amount icon
		mGui.setItem(1, 3, buildAddListingTradeAmountIcon())
			.onClick(this::changeAmountOfTradesAction);

		// trade amount multiplier
		mGui.setItem(2, 3, buildAddListingTradeAmountMultiplierIcon())
			.onClick(this::changeAmountOfTradesAction);

		// Change currency item
		mGui.setItem(1, 5, new GuiItem(buildChangeCurrencyIcon(), false))
			.onLeftClick(() -> mGui.switchToTab(mGui.TAB_CHOOSE_CURRENCY));

		mGui.setItem(2, 5, buildAddListingPricePerTradeMultiplierIcon())
			.onClick(this::changePricePerTradeAction);

		// small middleman check
		if (mItemToSell != null && mCurrencyItem != null) {
			List<String> errorMessages = MarketManager.itemIsSellable(mGui.mPlayer, mItemToSell, mCurrencyItem);
			if (!errorMessages.isEmpty()) {
				for (String message : errorMessages) {
					mGui.mPlayer.sendMessage(Component.text(message, NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
				}
				mGui.mPlayer.playSound(mGui.mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1, 1);
				mItemToSell = null;
				mGui.update();
				return;
			}
		}

		// confirm button
		boolean enoughItemsInInventory = false;
		if (mItemToSell != null) {
			enoughItemsInInventory = InventoryUtils.numInInventory(mPlayer.getInventory(), mItemToSell) >= mItemsPerTrade * mAmountOfTrades;
		}
		// exclude exhorbitant prices
		// a bazaar listing should not be worth more than 200 stacks of HXX
		boolean priceCorrect = mPricePerTradeAmount * mAmountOfTrades < 200*/*stacks*/64*/*of HXP*/64*8;
		// tax calculation
		WalletUtils.Debt taxDebt = MarketManager.getInstance().calculateTaxDebt(mPlayer, mCurrencyItem, mPricePerTradeAmount * mAmountOfTrades);

		GuiItem guiItem = mGui.setItem(1, 7, new GuiItem(buildAddListingConfirmWithTaxStatusIcon(enoughItemsInInventory, priceCorrect, taxDebt), false));
		if (enoughItemsInInventory && priceCorrect && taxDebt.mMeetsRequirement) {
			guiItem.onClick((clickEvent) -> {
				if (MarketGui.initiatePlayerAction(mGui.mPlayer)) {
					return;
				}
				if (mItemToSell != null && mCurrencyItem != null) {
					MarketManager.getInstance().addNewListing(mPlayer, mItemToSell, mItemsPerTrade, mAmountOfTrades, mPricePerTradeAmount, mCurrencyItem, taxDebt);
					MarketGui.endPlayerAction(mPlayer);
					mGui.switchToTab(mGui.TAB_PLAYER_LISTINGS);
				}
			});
		}
	}

	private ItemStack buildAddListingConfirmWithTaxStatusIcon(boolean enoughItemsInInventory, boolean priceCorrect, WalletUtils.Debt taxDebt) {

		ArrayList<Component> lore = new ArrayList<>();
		boolean error = false;

		if (mItemToSell != null) {

			if (!priceCorrect) {
				lore.add(Component.text("The value of this listing", NamedTextColor.RED));
				lore.add(Component.text("seem to be too high.", NamedTextColor.RED));
				lore.add(Component.text("You should reduce the price,", NamedTextColor.RED));
				lore.add(Component.text("the amount of items per trade,", NamedTextColor.RED));
				lore.add(Component.text("or the amount of trades.", NamedTextColor.RED));
				lore.add(Component.empty());
				error = true;
			}

			lore.add(Component.text("You will need to pay a tax of:", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text(taxDebt.mTotalRequiredAmount + " " + ItemUtils.getPlainName(taxDebt.mItem) + " ", NamedTextColor.WHITE)
				.append(MarketGuiIcons.getCheckboxOrXmark(taxDebt.mMeetsRequirement))
				.append(Component.text(taxDebt.mWalletDebt > 0 ? " (" + taxDebt.mNumInWallet + " in wallet)" : "", NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text(String.format("Tax Rate: %.1f%%", MarketManager.getConfig().mBazaarTaxRate * 100), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			if (!taxDebt.mMeetsRequirement) {
				lore.add(Component.text("There is not enough money", NamedTextColor.RED));
				lore.add(Component.text("in your inventory or wallet", NamedTextColor.RED));
				lore.add(Component.text("to pay the tax.", NamedTextColor.RED));
				error = true;
			}
			lore.add(Component.empty());


			lore.add(Component.text((mItemsPerTrade * mAmountOfTrades) + ItemUtils.getPlainName(mItemToSell), NamedTextColor.WHITE)
				.append(MarketGuiIcons.getCheckboxOrXmark(enoughItemsInInventory)).decoration(TextDecoration.ITALIC, false));
			if (!enoughItemsInInventory) {
				lore.add(Component.text("You do not possess enough", NamedTextColor.RED));
				lore.add(Component.text("of those items", NamedTextColor.RED));
				lore.add(Component.text("in your inventory.", NamedTextColor.RED));
				error = true;
			}
		} else {
			lore.add(Component.text("Put an item first!", NamedTextColor.GOLD));
			error = true;
		}

		if (error) {
			return GUIUtils.createBasicItem(Material.GOLD_INGOT, 1, GUIUtils.formatName("Wrong parameters!", NamedTextColor.RED, true),
				lore, true, "gui_exclamation_mark");
		}
		return GUIUtils.createBasicItem(Material.GREEN_STAINED_GLASS_PANE, 1, GUIUtils.formatName("Confirm the Creation", NamedTextColor.GREEN, true),
			lore, true, "gui_checkmark");

	}

	private ItemStack buildAddListingItemToBeSoldIcon() {
		if (ItemUtils.isNullOrAir(mItemToSell)) {
			return MarketGuiIcons.MISSING_ITEM_SELECTION;
		}


		List<Component> sellItemLore = new ArrayList<>();
		sellItemLore.add(mItemToSell.displayName());
		List<Component> lore = mItemToSell.lore();
		if (lore != null) {
			sellItemLore.addAll(lore);
		}

		ItemStack icon = GUIUtils.createBasicItem(mItemToSell.getType(), mItemsPerTrade, "Item to be sold:", NamedTextColor.GOLD, true, sellItemLore, true);
		ItemUtils.setPlainName(icon, ItemUtils.getPlainName(mItemToSell));
		return icon;
	}

	public ItemStack buildChangeCurrencyIcon() {
		if (mCurrencyItem == null) {
			return GUIUtils.createBasicItem(
				Material.STRUCTURE_VOID, 1,
				GUIUtils.formatName("Current Currency: NONE", NamedTextColor.GOLD, true),
				List.of(Component.text("Left click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to select the currency", NamedTextColor.WHITE))),
				false, null);
		}
		ItemStack icon = mCurrencyItem.asQuantity(mPricePerTradeAmount);
		ItemMeta meta = icon.getItemMeta();
		meta.displayName(GUIUtils.formatName("Current Currency: " + StringUtils.getCurrencyShortForm(mCurrencyItem), NamedTextColor.GOLD, true));
		meta.lore(List.of(
			Component.text("Left click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
				.append(Component.text(" to change the currency", NamedTextColor.WHITE))
		));
		icon.setItemMeta(meta);
		ItemUtils.setPlainName(icon, ItemUtils.getPlainName(mCurrencyItem));
		return icon;
	}

	private ItemStack buildAddListingPricePerTradeMultiplierIcon() {
		ItemStack icon = MarketGuiIcons.ADDLISTING_MULTIPLIER_MISSING_ITEM.asQuantity(1);

		if (ItemUtils.isNullOrAir(mCurrencyItem)) {
			return icon;
		}

		ItemMeta meta = icon.getItemMeta();

		meta.displayName(Component.text("Price per Trade: ", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false).append(Component.text(mPricePerTradeAmount, NamedTextColor.GOLD, TextDecoration.BOLD)));
		meta.lore(List.of(
			Component.text("Left click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to decrease by 1", NamedTextColor.WHITE)),
			Component.text("Right click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to increase by 1", NamedTextColor.WHITE)),
			Component.text("Shift click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to offset by 64 instead", NamedTextColor.WHITE)),
			Component.keybind("key.swapOffhand", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to specify the amount", NamedTextColor.WHITE))
		));

		icon.setItemMeta(meta);

		return icon;
	}

	private GuiItem buildAddListingTradeAmountIcon() {
		ItemStack icon = GUIUtils.createBasicItem(Material.CHEST, mAmountOfTrades, "Amount of Trades: " + mAmountOfTrades, NamedTextColor.GOLD, true, new ArrayList<>(), false);
		return new GuiItem(icon, false);
	}

	public ItemStack buildAddListingTradeAmountMultiplierIcon() {

		ItemStack icon = MarketGuiIcons.ADDLISTING_MULTIPLIER_MISSING_ITEM.asQuantity(1);

		if (ItemUtils.isNullOrAir(mItemToSell)) {
			return icon;
		}

		ItemMeta meta = icon.getItemMeta();

		meta.displayName(Component.text("Amount of Trades: ", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false).append(Component.text(mAmountOfTrades, NamedTextColor.GOLD, TextDecoration.BOLD)));
		meta.lore(List.of(
			Component.text("Left click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to decrease by 1", NamedTextColor.WHITE)),
			Component.text("Right click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to increase by 1", NamedTextColor.WHITE)),
			Component.text("Shift click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to offset by 64 instead", NamedTextColor.WHITE)),
			Component.keybind("key.swapOffhand", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to specify the amount", NamedTextColor.WHITE))
		));

		icon.setItemMeta(meta);

		return icon;
	}

	public ItemStack buildAddListingItemsPerTradeMultiplierIcon() {

		ItemStack icon = MarketGuiIcons.ADDLISTING_MULTIPLIER_MISSING_ITEM.asQuantity(1);

		if (ItemUtils.isNullOrAir(mItemToSell)) {
			return icon;
		}

		ItemMeta meta = icon.getItemMeta();

		meta.displayName(Component.text("Items per trade: ", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false).append(Component.text(mItemsPerTrade, NamedTextColor.GOLD, TextDecoration.BOLD)));
		meta.lore(List.of(
			Component.text("Left click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to divide by 2", NamedTextColor.WHITE)),
			Component.text("Right click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to multiply by 2", NamedTextColor.WHITE)),
			Component.keybind("key.swapOffhand", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to specify the amount", NamedTextColor.WHITE)),
			Component.text("The amount is required to be a power of 2,", NamedTextColor.WHITE),
			Component.text("with a maximum value of 64", NamedTextColor.WHITE)
			));

		icon.setItemMeta(meta);

		return icon;
	}

	private void changePricePerTradeAction(InventoryClickEvent clickEvent) {
		int changeMultiplier = 1;
		if (!ItemUtils.isNullOrAir(mItemToSell)) {
			switch (clickEvent.getClick()) {
				case SHIFT_LEFT:
					changeMultiplier = 64;
					// fall through to actually change the amount
				case LEFT:
					mPricePerTradeAmount -= changeMultiplier;
					mGui.update();
					break;
				case SHIFT_RIGHT:
					changeMultiplier = 64;
					// fall through to actually change the amount
				case RIGHT:
					mPricePerTradeAmount += changeMultiplier;
					mGui.update();
					break;
				case SWAP_OFFHAND:
					mGui.close();
					SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter price", "per trade"))
						.reopenIfFail(false)
						.response((player, lines) -> {
							try {
								mPricePerTradeAmount = Integer.parseInt(lines[0]);
								mGui.open();
								return true;
							} catch (NumberFormatException e) {
								player.sendMessage(Component.text("Please enter a valid number.", NamedTextColor.RED));
								mGui.open();
								return false;
							}
						})
						.open(mPlayer);
					break;
				default:
					// Do nothing
			}
			mGui.update();
		}
	}

	private void changeItemsPerTradeAction(InventoryClickEvent clickEvent) {
		if (!ItemUtils.isNullOrAir(mItemToSell)) {
			switch (clickEvent.getClick()) {
				case LEFT:
					mItemsPerTrade /= 2;
					if (mItemsPerTrade < 1) {
						mItemsPerTrade = 64;
					}
					mGui.update();
					break;
				case RIGHT:
					mItemsPerTrade *= 2;
					if (mItemsPerTrade > 64) {
						mItemsPerTrade = 1;
					}
					mGui.update();
					break;
				case SWAP_OFFHAND:
					mGui.close();
					SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter amount of", "items per trade"))
						.reopenIfFail(false)
						.response((player, lines) -> {
							try {
								mItemsPerTrade = Integer.parseInt(lines[0]);
								mGui.open();
								return true;
							} catch (NumberFormatException e) {
								player.sendMessage(Component.text("Please enter a valid number.", NamedTextColor.RED));
								mGui.open();
								return false;
							}
						})
						.open(mPlayer);
					break;
				default:
					// Do nothing
			}
			mGui.update();
		}
	}

	private void changeAmountOfTradesAction(InventoryClickEvent clickEvent) {
		int changeMultiplier = 1;
		int maxInInventory = 0;
		if (mItemToSell != null) {
			maxInInventory = InventoryUtils.numInInventory(mPlayer.getInventory(), mItemToSell) / mItemsPerTrade;
		}
		if (!ItemUtils.isNullOrAir(mItemToSell)) {
			switch (clickEvent.getClick()) {
				case SHIFT_LEFT:
					changeMultiplier = 64;
					// fall through to actually change the amount
				case LEFT:
					mAmountOfTrades -= changeMultiplier;
					if (mAmountOfTrades < 1) {
						mAmountOfTrades = maxInInventory;
					}
					mGui.update();
					break;
				case SHIFT_RIGHT:
					changeMultiplier = 64;
					// fall through to actually change the amount
				case RIGHT:
					mAmountOfTrades += changeMultiplier;
					if (mAmountOfTrades > maxInInventory) {
						mAmountOfTrades = 1;
					}
					mGui.update();
					break;
				case SWAP_OFFHAND:
					mGui.close();
					SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter amount", "of trades"))
						.reopenIfFail(false)
						.response((player, lines) -> {
							try {
								mAmountOfTrades = Integer.parseInt(lines[0]);
								mGui.open();
								return true;
							} catch (NumberFormatException e) {
								player.sendMessage(Component.text("Please enter a valid number.", NamedTextColor.RED));
								mGui.open();
								return false;
							}
						})
						.open(mPlayer);
					break;
				default:
					// Do nothing
			}
			mGui.update();
		}
	}

	private void clampAmountToSellToAmountInInventory() {

		int amountRequired = mAmountOfTrades * mItemsPerTrade;
		int amountInInv = 0;
		if (mItemToSell != null) {
			amountInInv = InventoryUtils.numInInventory(mPlayer.getInventory(), mItemToSell);
		}

		if (amountInInv == 0) {
			// the player will never have this item, even if we bring the wanted amount to 1.
			// reset the values to default
			mItemToSell = null;
		} else {
			// amount may be negative when changing the amount using the GuiItem
			int amountPossible = Math.min(Math.max(amountRequired, 1), amountInInv);
			// change the amount of trades if the possible amount is not enough
			mAmountOfTrades = Math.max(amountPossible / mItemsPerTrade, 1);
		}
	}

	@Override
	public void onSwitch() {
		mGui.setTitle(TAB_TITLE);
		mGui.setSize(TAB_SIZE);
	}

	@Override
	public void onLeave() {

	}

	protected void onPlayerInventoryClickEvt(InventoryClickEvent event) {
		ItemStack item = event.getCurrentItem();
		if (!ItemUtils.isNullOrAir(item) && mCurrencyItem != null) {

			List<String> errorMessages = MarketManager.itemIsSellable(mPlayer, event.getCurrentItem(), mCurrencyItem);
			if (!errorMessages.isEmpty()) {
				for (String message : errorMessages) {
					mPlayer.sendMessage(Component.text(message, NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
				}
				mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1, 1);
			} else {
				// Change the item to be sold, when the "Add listing" tab is open
				int amountInInv = InventoryUtils.numInInventory(mPlayer.getInventory(), item);
				mAmountOfTrades = amountInInv / mItemsPerTrade;
				if (mAmountOfTrades == 0) {
					mItemsPerTrade = 1;
					mAmountOfTrades = amountInInv;
				}
				mItemToSell = item.asQuantity(1);
				mGui.update();
			}
		}
	}

	public void setCurrency(ItemStack currency) {
		this.mCurrencyItem = currency;
	}
}
