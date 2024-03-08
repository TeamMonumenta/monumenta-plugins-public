package com.playmonumenta.plugins.market;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.playmonumenta.plugins.itemstats.enums.Location;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.WalletUtils;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class MarketListing {

	// unique numeric identifier of the listing
	// two listings should never have the same ID
	// to ensure that, it is more than advised to use the redis incremListingId method,
	// if you do not know the id of your listing
	private final long mId;

	// the item to be sold
	private final long mItemToSellID;

	// amount of items left to be sold
	private int mAmountToSellRemaining;

	// item (currency) used to buy the item to be sold
	private final long mCurrencyToBuyID;

	// amount of item (currency) needed to buy the item
	private final int mAmountToBuy;

	// the seller may not be there when another player buys this listing
	// thus, we need to store the amount that were sold, in order to give the money to the seller when they ask
	// this represents the amount of items that were sold, but not yet money-claimed
	private int mAmountToClaim;

	// date of creation of the listing
	private @Nullable final String mListingCreationDate;

	// Display name of the seller
	private @Nullable String mOwnerName;

	// UUID of the owner
	private @Nullable String mOwnerUUID;

	// listing is locked, by item-balance-update, or by moderator action
	private boolean mLocked;

	// listing is expired, by time passing, or by moderator action
	private boolean mExpired;

	// the region of the sold item. used for indexes/filters
	private @Nullable final Region mRegion;

	// the location (sub-region, dungeon, etc...) of the sold item. used for indexes/filters
	private @Nullable final Location mLocation;

	private @Nullable String mEditLocked;

	public MarketListing(long listingID, long itemToSellID, int amountToSell, int pricePerItemAmount, long currencyToBuyID, Player owner) {
		this.mId = listingID;
		this.mItemToSellID = itemToSellID;
		this.mAmountToSellRemaining = amountToSell;
		this.mAmountToBuy = pricePerItemAmount;
		this.mCurrencyToBuyID = currencyToBuyID;
		this.mAmountToClaim = 0;
		this.mListingCreationDate = DateUtils.localDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
		this.mOwnerName = owner.getName();
		this.mOwnerUUID = owner.getUniqueId().toString();
		this.mLocked = false;
		this.mExpired = false;
		this.mEditLocked = null;

		ItemStack itemToSell = MarketItemDatabase.getItemStackFromID(itemToSellID);
		this.mRegion = ItemStatUtils.getRegion(itemToSell);
		this.mLocation = ItemStatUtils.getLocation(itemToSell);
	}

	public MarketListing(Long listingID) {
		this.mId = listingID;
		this.mItemToSellID = 0;
		this.mAmountToSellRemaining = 0;
		this.mAmountToBuy = 0;
		this.mCurrencyToBuyID = 0;
		this.mAmountToClaim = 0;
		this.mListingCreationDate = null;
		this.mOwnerName = null;
		this.mLocked = false;
		this.mExpired = false;
		this.mRegion = null;
		this.mLocation = null;
		this.mEditLocked = null;
	}

	public MarketListing(MarketListing other) {
		this.mId = other.mId;
		this.mItemToSellID = other.mItemToSellID;
		this.mAmountToSellRemaining = other.mAmountToSellRemaining;
		this.mCurrencyToBuyID = other.mCurrencyToBuyID;
		this.mAmountToBuy = other.mAmountToBuy;
		this.mAmountToClaim = other.mAmountToClaim;
		this.mListingCreationDate = other.mListingCreationDate;
		this.mOwnerName = other.mOwnerName;
		this.mOwnerUUID = other.mOwnerUUID;
		this.mLocked = other.mLocked;
		this.mExpired = other.mExpired;
		this.mRegion = other.mRegion;
		this.mLocation = other.mLocation;
		this.mEditLocked = other.mEditLocked;
	}

	public long getId() {
		return mId;
	}

	public ItemStack getItemToSell() {
		MMLog.info("GET " + this.mItemToSellID);
		return MarketItemDatabase.getItemStackFromID(this.mItemToSellID);
	}

	public int getAmountToSellRemaining() {
		return mAmountToSellRemaining;
	}

	public void setAmountToSellRemaining(int amountToSellRemaining) {
		this.mAmountToSellRemaining = amountToSellRemaining;
	}

	public boolean isNotUsable() {
		return mItemToSellID == 0
			|| mCurrencyToBuyID == 0
			|| mId == 0
			|| mListingCreationDate == null;
	}

	public ItemStack getListingDisplayItemStack(Player player, MarketGUI.MarketGuiTab environment) {

		if (isNotUsable()) {
			// fields needed are null, something is wrong.
			// display an error Item, with the raw contents of the listing
			String jsonString = MarketRedisManager.getListingRaw(this.getId());
			JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String prettyJson = gson.toJson(json);
			List<Component> compList = new ArrayList<>();
			compList.add(Component.text("THIS LISTING IS BROKEN. CONTACT A MODERATOR FOR A REFUND", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			compList.add(Component.text("Market Listing: ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text("#" + this.getId(), NamedTextColor.DARK_GRAY)));
			for (String str : prettyJson.split("\n")) {
				compList.add(Component.text(str, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			}
			compList.add(Component.text("Market Listing: ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text("#" + this.getId(), NamedTextColor.DARK_GRAY)));
			compList.add(Component.text("THIS LISTING IS BROKEN. CONTACT A MODERATOR FOR A REFUND", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			return GUIUtils.createExclamation(compList);
		}

		List<Component> newLore = getListingDisplayLore(player, environment);

		// Add to previous lore (if any):

		ItemStack item = getItemToSell();
		ItemMeta itemMeta = item.getItemMeta();

		List<Component> prevLore = new ArrayList<>();
		if (itemMeta.hasLore()) {
			prevLore = itemMeta.lore();
		}
		prevLore.addAll(newLore);
		itemMeta.lore(prevLore);

		item.setItemMeta(itemMeta);

		return item;
	}

	private List<Component> getListingDisplayLore(Player player, MarketGUI.MarketGuiTab environment) {

		List<Component> newLore = new ArrayList<>();
		newLore.add(Component.empty());
		newLore.add(Component.text("Market Listing: ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text("#" + this.getId(), NamedTextColor.DARK_GRAY)));

		switch (environment) {
			case LISTINGS_BROWSER:
			case ACTIVE_LISTINGS_BROWSER:
				newLore.addAll(getListingDisplayLoreListingsBrowser(player));
				break;
			case PLAYER_LISTINGS:
				newLore.addAll(getListingDisplayLorePlayerListings());
				break;
			default:
				newLore.add(Component.text("IMPLEMENT GETLISTINGDISPLAYLORE FOR " + environment, NamedTextColor.RED));
		}

		return newLore;
	}

	private Collection<? extends Component> getListingDisplayLorePlayerListings() {

		List<Component> newLore = new ArrayList<>();
		newLore.add(Component.empty());

		if (this.isExpired()) {
			newLore.add(Component.text("This listing has expired", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
			newLore.add(Component.text("and will not show up in the browser ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
		} else if (this.getAmountToSellRemaining() == 0) {
			newLore.add(Component.text("This listing ran out of stock", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
			newLore.add(Component.text("and will not show up in the browser ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
		} else {

			if (this.isLocked()) {
				newLore.add(Component.text("This listing is ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
					.append(Component.text("LOCKED", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));
				newLore.add(Component.text("and will not show up in the browser ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
				newLore.add(Component.text("Right click", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
					.append(Component.text(" to unlock the listing", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
			} else {
				newLore.add(Component.text("This listing is ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
					.append(Component.text("UNLOCKED", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
				newLore.add(Component.text("Right click", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
					.append(Component.text(" to lock the listing", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
			}

			newLore.add(Component.empty());
			newLore.add(Component.text("Remaining in stock: ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
				.append(Component.text(this.getAmountToSellRemaining(), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));

			if (this.getAmountToClaim() > 0) {
				newLore.add(Component.text("Amount to claim: ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
					.append(Component.text(this.getAmountToClaim(), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
				newLore.add(Component.text("Left click", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
					.append(Component.text(" to claim:", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
				newLore.add(Component.text("  " + this.getAmountToClaim() * this.getAmountToBuy(), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
					.append(Component.text(" " + ItemUtils.getPlainName(this.getItemToBuy()), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
			}

		}

		newLore.add(Component.empty());
		newLore.add(Component.text("Press ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
			.append(Component.keybind("key.swapOffhand", NamedTextColor.WHITE))
			.append(Component.text(" To claim everything back").decoration(TextDecoration.ITALIC, false)));
		newLore.add(Component.text("and remove the listing.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));

		return newLore;
	}

	public List<Component> getListingDisplayLoreListingsBrowser(Player player) {
		List<Component> newLore = new ArrayList<>();

		// requirement (itemToBuy)
		ItemStack currency = this.getItemToBuy().clone();
		currency.setAmount(this.getAmountToBuy());
		WalletUtils.Debt debt = WalletUtils.calculateInventoryAndWalletDebt(currency, player, true);
		newLore.add(Component.text(this.getAmountToBuy() + " " + ItemUtils.getPlainName(currency) + " ", NamedTextColor.WHITE)
			.append(Component.text(debt.mMeetsRequirement ? "\u2713" : "\u2717", (debt.mMeetsRequirement ? NamedTextColor.GREEN : NamedTextColor.RED)))
			.append(Component.text(debt.mWalletDebt > 0 ? " (" + debt.mNumInWallet + " in wallet)" : "", NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false));

		// listing locked
		if (this.isLocked()) {
			newLore.add(Component.text("Listing is locked.", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
		}

		if (this.isExpired()) {
			newLore.add(Component.text("Listing is expired.", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
		}

		// items in stock
		newLore.add(Component.text("In Stock: ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
			.append(Component.text("" + this.getAmountToSellRemaining(), this.getAmountToSellRemaining() > 0 ? NamedTextColor.GREEN : NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false)));

		// sold by author +
		newLore.add(Component.text("Sold by: ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
			.append(Component.text(this.getOwnerName(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));

		return newLore;
	}

	public ItemStack getItemToBuy() {
		return MarketItemDatabase.getItemStackFromID(mCurrencyToBuyID);
	}

	public int getAmountToBuy() {
		return mAmountToBuy;
	}

	public int getAmountToClaim() {
		return mAmountToClaim;
	}

	public void setAmountToClaim(int amountToClaim) {
		this.mAmountToClaim = amountToClaim;
	}

	public @Nullable String getListingCreationDate() {
		return mListingCreationDate;
	}

	public String toJsonString() {
		return new Gson().toJson(this);
	}

	public String toBeautifiedJsonString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson(this);
	}

	public Component toPlayerReadableComponent(Player player) {

		ItemStack item = this.getListingDisplayItemStack(player, MarketGUI.MarketGuiTab.LISTINGS_BROWSER);
		ItemStack itemToBuy = this.getItemToBuy();

		return Component.text("MarketListing - ID: " + this.mId)
			.append(Component.text("\n  itemToSell=")).append(item.displayName().hoverEvent(item))
			.append(Component.text("\n amountToSellRemaining=" + mAmountToSellRemaining))
			.append(Component.text("\n itemToBuy=")).append(itemToBuy.displayName().hoverEvent(itemToBuy))
			.append(Component.text("\n amountToBuy=" + mAmountToBuy))
			.append(Component.text("\n amountToClaim=" + mAmountToClaim))
			.append(Component.text("\n listingCreationDate='" + mListingCreationDate + '\''))
			.append(Component.text("\n ownerName='" + mOwnerName + '\''))
			.append(Component.text("\n locked='" + mLocked + '\''))
			.append(Component.text("\n expired='" + mExpired + '\''))
			.append(Component.text("\n region='" + mRegion + '\''))
			.append(Component.text("\n location='" + mLocation + '\''));
	}

	public @Nullable String getOwnerName() {
		return mOwnerName;
	}

	public void setOwnerName(@Nullable String ownerName) {
		this.mOwnerName = ownerName;
	}

	public boolean isLocked() {
		return mLocked;
	}

	public void setLocked(boolean locked) {
		this.mLocked = locked;
	}

	public boolean isExpired() {
		return mExpired;
	}

	public void setExpired(boolean expired) {
		this.mExpired = expired;
	}

	public @Nullable Region getRegion() {
		return mRegion;
	}

	public @Nullable Location getLocation() {
		return mLocation;
	}

	public MarketListingStatus getPurchasableStatus(int stockNeeded) {

		if (this.isLocked()) {
			return MarketListingStatus.LOCKED;
		}

		if (this.isExpired()) {
			return MarketListingStatus.EXPIRED;
		}

		if (this.getAmountToSellRemaining() < stockNeeded) {
			return MarketListingStatus.NOT_ENOUGH_REMAINING;
		}

		return MarketListingStatus.IS_PURCHASABLE;
	}


	public boolean isSimilar(MarketListing listing) {
		return mId == listing.mId
			&& mAmountToSellRemaining == listing.mAmountToSellRemaining
			&& mAmountToBuy == listing.mAmountToBuy
			&& mAmountToClaim == listing.mAmountToClaim
			&& mLocked == listing.mLocked
			&& mExpired == listing.mExpired
			&& mItemToSellID == listing.mItemToSellID
			&& mCurrencyToBuyID == listing.mCurrencyToBuyID
			&& Objects.equals(mListingCreationDate, listing.mListingCreationDate)
			&& Objects.equals(mOwnerName, listing.mOwnerName)
			&& mRegion == listing.mRegion
			&& mLocation == listing.mLocation
			&& Objects.equals(mEditLocked, listing.mEditLocked)
			;
	}

	public @Nullable String getEditLocked() {
		return mEditLocked;
	}

	public void setEditLocked(String checkKey) {
		this.mEditLocked = checkKey;
	}

	public @Nullable String getOwnerUUID() {
		return mOwnerUUID;
	}

	public void setOwnerUUID(String mOwnerUUID) {
		this.mOwnerUUID = mOwnerUUID;
	}
}
