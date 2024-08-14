package com.playmonumenta.plugins.market;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.playmonumenta.plugins.inventories.WalletManager;
import com.playmonumenta.plugins.itemstats.enums.ItemType;
import com.playmonumenta.plugins.itemstats.enums.Location;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.market.gui.MarketGuiTab;
import com.playmonumenta.plugins.market.gui.TabBazaarBrowser;
import com.playmonumenta.plugins.market.gui.TabModeratorBrowser;
import com.playmonumenta.plugins.market.gui.TabPlayerListings;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.WalletUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

	private final MarketListingType mListingType;

	// the item to be sold
	private final long mItemToSellID;

	// amount of trades left to be sold
	private int mAmountToSellRemaining;

	// amount of items per trade
	private final int mBundleSize;

	// item (currency) used to buy the item to be sold
	private final long mCurrencyToBuyID;

	// amount of item (currency) needed to buy the item
	private final int mAmountToBuy;

	// the seller may not be there when another player buys this listing
	// thus, we need to store the amount that were sold, in order to give the money to the seller when they ask
	// this represents the amount of items that were sold, but not yet money-claimed
	private int mAmountToClaim;

	// date of creation of the listing
	private @Nullable
	final String mListingCreationDate;

	// date of expected expiration
	private final long mExpirationDate;

	// Display name of the seller
	private @Nullable String mOwnerName;

	// UUID of the owner
	private @Nullable String mOwnerUUID;

	// listing is locked, by item-balance-update, or by moderator action
	private boolean mLocked;

	// listing is expired, by time passing, or by moderator action
	private boolean mExpired;

	// the region of the sold item. used for indexes/filters
	private @Nullable Region mRegion;

	// the location (sub-region, dungeon, etc...) of the sold item. used for indexes/filters
	private @Nullable Location mLocation;

	private @Nullable String mItemName;

	private @Nullable String mCurrencyName;

	private @Nullable ItemType mItemType;

	private @Nullable Tier mItemTier;

	private @Nullable String mEditLocked;

	public MarketListing(long listingID, MarketListingType type, long itemToSellID, int amountToSell, int bundleSize, int pricePerTradeAmount, long currencyToBuyID, Player owner) {
		this.mId = listingID;
		this.mListingType = type;
		this.mItemToSellID = itemToSellID;
		this.mAmountToSellRemaining = amountToSell;
		this.mAmountToBuy = pricePerTradeAmount;
		this.mBundleSize = bundleSize;
		this.mCurrencyToBuyID = currencyToBuyID;
		this.mAmountToClaim = 0;
		this.mListingCreationDate = DateUtils.trueUtcDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
		this.mExpirationDate = DateUtils.trueUtcDateTime().plusDays(30).toInstant(ZoneOffset.UTC).getEpochSecond();
		this.mOwnerName = owner.getName();
		this.mOwnerUUID = owner.getUniqueId().toString();
		this.mLocked = false;
		this.mExpired = false;
		this.mEditLocked = null;

		this.recalculateListingIndexValues();
	}

	public void recalculateListingIndexValues() {

		ItemStack itemToSell = RedisItemDatabase.getItemStackFromID(this.mItemToSellID);
		this.mRegion = ItemStatUtils.getRegion(itemToSell);
		this.mLocation = ItemStatUtils.getLocation(itemToSell);
		this.mItemName = ItemUtils.getPlainNameOrDefault(itemToSell);
		this.mItemType = ItemUtils.getItemType(itemToSell);
		this.mItemTier = ItemStatUtils.getTier(itemToSell);

		ItemStack currencyToBuy = RedisItemDatabase.getItemStackFromID(this.mCurrencyToBuyID);
		this.mCurrencyName = convertCurrencyItemStackToSmallestCurrencyName(currencyToBuy);

	}

	public MarketListing(Long listingID) {
		this.mId = listingID;
		this.mListingType = MarketListingType.BAZAAR;
		this.mItemToSellID = 0;
		this.mItemName = "";
		this.mAmountToSellRemaining = 0;
		this.mAmountToBuy = 0;
		this.mBundleSize = 0;
		this.mCurrencyToBuyID = 0;
		this.mAmountToClaim = 0;
		this.mListingCreationDate = null;
		this.mExpirationDate = 0;
		this.mOwnerName = null;
		this.mLocked = false;
		this.mExpired = false;
		this.mRegion = null;
		this.mLocation = null;
		this.mEditLocked = null;
		this.mCurrencyName = null;
		this.mItemType = null;
		this.mItemTier = null;
	}

	public MarketListing(MarketListing other) {
		this.mId = other.mId;
		this.mListingType = other.mListingType;
		this.mItemToSellID = other.mItemToSellID;
		this.mItemName = other.mItemName;
		this.mAmountToSellRemaining = other.mAmountToSellRemaining;
		this.mBundleSize = other.mBundleSize;
		this.mCurrencyToBuyID = other.mCurrencyToBuyID;
		this.mAmountToBuy = other.mAmountToBuy;
		this.mAmountToClaim = other.mAmountToClaim;
		this.mListingCreationDate = other.mListingCreationDate;
		this.mExpirationDate = other.mExpirationDate;
		this.mOwnerName = other.mOwnerName;
		this.mOwnerUUID = other.mOwnerUUID;
		this.mLocked = other.mLocked;
		this.mExpired = other.mExpired;
		this.mRegion = other.mRegion;
		this.mLocation = other.mLocation;
		this.mEditLocked = other.mEditLocked;
		this.mCurrencyName = other.mCurrencyName;
		this.mItemType = other.mItemType;
		this.mItemTier = other.mItemTier;
	}

	public boolean isSimilar(MarketListing listing) {
		return mId == listing.mId
			&& mListingType == listing.mListingType
			&& mAmountToSellRemaining == listing.mAmountToSellRemaining
			&& mBundleSize == listing.mBundleSize
			&& mAmountToBuy == listing.mAmountToBuy
			&& mAmountToClaim == listing.mAmountToClaim
			&& mLocked == listing.mLocked
			&& mExpired == listing.mExpired
			&& mItemToSellID == listing.mItemToSellID
			&& mCurrencyToBuyID == listing.mCurrencyToBuyID
			&& Objects.equals(mListingCreationDate, listing.mListingCreationDate)
			&& mExpirationDate == listing.mExpirationDate
			&& Objects.equals(mOwnerName, listing.mOwnerName)
			&& mRegion == listing.mRegion
			&& mLocation == listing.mLocation
			&& Objects.equals(mEditLocked, listing.mEditLocked)
			&& Objects.equals(mItemName, listing.mItemName)
			&& Objects.equals(mCurrencyName, listing.mCurrencyName)
			&& mItemType == listing.mItemType
			&& mItemTier == listing.mItemTier
			;
	}

	public long getId() {
		return mId;
	}

	public ItemStack getItemToSell() {
		return RedisItemDatabase.getItemStackFromID(this.mItemToSellID);
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

	public ItemStack getListingDisplayItemStack(Player player, @Nullable MarketGuiTab environment) {

		if (isNotUsable()) {
			// fields needed are null, something is wrong.
			// display an error Item, with the raw contents of the listing
			String jsonString = MarketRedisManager.getListingRaw(this.getId());
			JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String prettyJson = gson.toJson(json);
			List<Component> compList = new ArrayList<>();
			compList.add(Component.text("THIS LISTING IS BROKEN. CONTACT A MODERATOR FOR A REFUND", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			compList.add(this.getDisplayComponentId());
			for (String str : prettyJson.split("\n")) {
				compList.add(Component.text(str, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			}
			compList.add(this.getDisplayComponentId());
			compList.add(Component.text("THIS LISTING IS BROKEN. CONTACT A MODERATOR FOR A REFUND", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			return GUIUtils.createExclamation(compList);
		}

		List<Component> newLore = getListingDisplayLore(player, environment);

		// Add to previous lore (if any):

		ItemStack item = getItemToSell();
		ItemMeta itemMeta = item.getItemMeta();

		List<Component> prevLore = null;
		if (itemMeta.hasLore()) {
			prevLore = itemMeta.lore();
		}
		if (prevLore == null) {
			prevLore = new ArrayList<>();
		}
		prevLore.addAll(newLore);
		itemMeta.lore(prevLore);
		if (getBundleSize() > 1) {
			itemMeta.displayName(Component.text(getBundleSize() + " * ", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).append(ItemUtils.getDisplayName(getItemToSell())));
		}

		item.setItemMeta(itemMeta);

		item.setAmount(getBundleSize());

		ItemUtils.setPlainName(item, ItemUtils.getPlainName(getItemToSell()));

		return item;
	}

	private List<Component> getListingDisplayLore(Player player, @Nullable MarketGuiTab environment) {

		List<Component> newLore = new ArrayList<>();
		newLore.add(Component.text("-".repeat(30), NamedTextColor.DARK_GRAY));
		if (environment != null) {
			if (environment instanceof TabBazaarBrowser || environment instanceof TabModeratorBrowser) {
				newLore.addAll(getListingDisplayLoreListingsBrowser(player));
			} else if (environment instanceof TabPlayerListings) {
				newLore.addAll(getListingDisplayLorePlayerListings());
			}
		}
		newLore.add(this.getDisplayComponentId());

		return newLore;
	}

	private List<Component> getListingDisplayLorePlayerListings() {

		List<Component> newLore = new ArrayList<>();

		if (isExpired()) {
			newLore.add(Component.text("This listing is ", NamedTextColor.GRAY).append(Component.text("expired", NamedTextColor.RED)).append(Component.text(".", NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false));
		} else {
			newLore.add(Component.text("This listing is ", NamedTextColor.GRAY).append(
					isLocked() ? Component.text("invisible", NamedTextColor.RED) : Component.text("visible", NamedTextColor.DARK_GREEN))
				.append(Component.text(".", NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false));
			newLore.add(Component.text("Expires in ", NamedTextColor.GRAY).append(Component.text(getTimeUntilExpirationAsDisplayableString(), NamedTextColor.RED)).decoration(TextDecoration.ITALIC, false));
		}

	    // stock
		newLore.add(Component.empty());
		newLore.add(Component.text("Remaining in stock: ", NamedTextColor.GRAY).append(Component.text(this.getAmountToSellRemaining(), (this.getAmountToSellRemaining() > 0 ? NamedTextColor.GREEN : NamedTextColor.RED))).decoration(TextDecoration.ITALIC, false));
		// price
		newLore.add(Component.text("Price per: ", NamedTextColor.GRAY)
			.append(Component.text(this.getAmountToBuy() + " " + ItemUtils.getPlainName(this.getCurrencyToBuy()), NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false));

		if (this.getAmountToClaim() > 0) {
			newLore.add(Component.empty());
			newLore.add(Component.text("Right click to claim money!", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
			newLore.add(Component.text("Claimable: ", NamedTextColor.GRAY)
				.append(Component.text(Integer.toString(this.getAmountToClaim() * this.getAmountToBuy()), NamedTextColor.GOLD))
				.append(Component.text(" " + ItemUtils.getPlainName(this.getCurrencyToBuy()), NamedTextColor.WHITE))
				.decoration(TextDecoration.ITALIC, false)
			);
			newLore.add(Component.text("Stock sold: ", NamedTextColor.GRAY).append(Component.text(Integer.toString(this.getAmountToClaim()), NamedTextColor.GREEN)).decoration(TextDecoration.ITALIC, false));
		} else if (this.isExpired() || (this.getAmountToSellRemaining() == 0 && this.getAmountToClaim() == 0)) {
			newLore.add(Component.empty());
			newLore.add(Component.text("No stock left, and nothing to claim!", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
			newLore.add(Component.text("Right click to delete this listing.", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
		}

		newLore.add(Component.empty());
		newLore.add(Component.text("Left click for more options.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));

		return newLore;
	}

	public List<Component> getFullPriceTag(Player player) {

		ItemStack currency = this.getCurrencyToBuy().clone();
		ItemStack baseCurrency = currency;
		int uncompressedAmount = this.getAmountToBuy();
		WalletManager.CompressionInfo info = WalletManager.getCompressionInfo(currency);
		if (info != null) {
			uncompressedAmount *= info.mAmount;
			baseCurrency = info.mBase;
		}

		WalletUtils.Debt debt = WalletUtils.calculateInventoryAndWalletDebt(baseCurrency.asQuantity(uncompressedAmount), player, true);

		Component shortenedDisplayComp = switch (ItemUtils.getPlainName(baseCurrency)) {
			case "Experience Bottle" -> calcShortenedDisplayCompForXP(uncompressedAmount);
			case "Crystalline Shard" -> calcShortenedDisplayCompForCS(uncompressedAmount);
			case "Archos Ring" -> calcShortenedDisplayCompForAR(uncompressedAmount);
			default -> throw new IllegalStateException("Unexpected value: " + ItemUtils.getPlainName(baseCurrency));
		};

		List<Component> out = new ArrayList<>();

		out.add(Component.text("Price:", NamedTextColor.YELLOW).append(shortenedDisplayComp).append(Component.text(debt.mMeetsRequirement ? " ✓" : " ✗", (debt.mMeetsRequirement ? NamedTextColor.GREEN : NamedTextColor.RED))).decoration(TextDecoration.ITALIC, false));
		out.add(Component.text("(" + uncompressedAmount + " " + ItemUtils.getPlainName(baseCurrency) + (debt.mWalletDebt > 0 ? ", " + debt.mNumInWallet + " in wallet)" : ")"), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

		return out;
	}

	private Component calcShortenedDisplayCompForXP(int uncompressedAmount) {
		int xp = uncompressedAmount % 8;
		uncompressedAmount = uncompressedAmount / 8;
		int cxp = uncompressedAmount % 64;
		int hxp = uncompressedAmount / 64;

		Component out = Component.text("", NamedTextColor.GRAY);
		if (hxp > 0) {
			out = out.append(Component.text(" " + hxp, NamedTextColor.GRAY)).append(Component.text("Hxp", NamedTextColor.GOLD));
		}
		if (cxp > 0) {
			out = out.append(Component.text(" " + cxp, NamedTextColor.GRAY)).append(Component.text("Cxp", NamedTextColor.GOLD));
		}
		if (xp > 0) {
			out = out.append(Component.text(" " + xp, NamedTextColor.GRAY)).append(Component.text("xp", NamedTextColor.GOLD));
		}
		return out;
	}

	private Component calcShortenedDisplayCompForCS(int uncompressedAmount) {
		int cs = uncompressedAmount % 8;
		uncompressedAmount = uncompressedAmount / 8;
		int ccs = uncompressedAmount % 64;
		int hcs = uncompressedAmount / 64;

		Component out = Component.text("", NamedTextColor.GRAY);
		if (hcs > 0) {
			out = out.append(Component.text(" " + hcs, NamedTextColor.GRAY)).append(Component.text("Hcs", NamedTextColor.AQUA));
		}
		if (ccs > 0) {
			out = out.append(Component.text(" " + ccs, NamedTextColor.GRAY)).append(Component.text("Ccs", NamedTextColor.AQUA));
		}
		if (cs > 0) {
			out = out.append(Component.text(" " + cs, NamedTextColor.GRAY)).append(Component.text("cs", NamedTextColor.AQUA));
		}
		return out;
	}

	private Component calcShortenedDisplayCompForAR(int uncompressedAmount) {
		int ar = uncompressedAmount % 64;
		int har = uncompressedAmount / 64;

		Component out = Component.text("", NamedTextColor.GRAY);
		if (har > 0) {
			out = out.append(Component.text(" " + har, NamedTextColor.GRAY)).append(Component.text("Har", NamedTextColor.WHITE));
		}
		if (ar > 0) {
			out = out.append(Component.text(" " + ar, NamedTextColor.GRAY)).append(Component.text("ar", NamedTextColor.WHITE));
		}
		return out;
	}

	public List<Component> getListingDisplayLoreListingsBrowser(Player player) {
		List<Component> newLore = new ArrayList<>();

		// requirement (itemToBuy)
		ItemStack currency = this.getCurrencyToBuy().clone();
		currency.setAmount(this.getAmountToBuy());
		newLore.addAll(getFullPriceTag(player));

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

	public ItemStack getCurrencyToBuy() {
		return RedisItemDatabase.getItemStackFromID(mCurrencyToBuyID);
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

	public Component getVisibilityAsDisplayableComponent() {
		return (this.isLocked() ? Component.text("Invisible", NamedTextColor.RED) : Component.text("Visible", NamedTextColor.GREEN)).decoration(TextDecoration.ITALIC, false);
	}

	public LocalDateTime getExpirationDateTime() {
		return LocalDateTime.ofInstant(Instant.ofEpochSecond(getExpirationTimestamp()), ZoneId.of("UTC"));
	}

	public long getExpirationTimestamp() {
		if (mExpirationDate == 0) {
			return 1714521600L;
			// Default value for listings that are older than the expiration system.
			// it represents the date of Wednesday, May 1, 2024 12:00:00 AM
			// which is approximately 30 days after expiration system goes live on play.
			// this default *should* be able to be removed past that date, as all listings will have a proper expiration date, or be removed
		}
		return mExpirationDate;
	}

	public boolean isExpirationDateInPast() {
		return getSecondsUntilExpiration() < 0;
	}

	public long getSecondsUntilExpiration() {
		return getExpirationTimestamp() - DateUtils.trueUtcDateTime().toInstant(ZoneOffset.UTC).getEpochSecond();
	}

	public String getTimeUntilExpirationAsDisplayableString() {
		long allSeconds = getSecondsUntilExpiration();
		long allMinutes = allSeconds / 60;
		long minutes = allMinutes % 60;
		long allHours = allMinutes / 60;
		long hours = allHours % 24;
		long days = allHours / 24;

		if (days > 0) {
			return days + " Days" + (hours != 0 ? ", " + hours + " Hours" : "");
		} else if (hours > 0) {
			return hours + " Hours" + (minutes != 0 ? ", " + minutes + " Minutes" : "");
		} else if (minutes > 0) {
			return minutes + " Minutes";
		} else {
			return "<1 Minute";
		}
	}

	public Component getDisplayComponentId() {
		return Component.text("Market Listing: ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
			.append(Component.text("#" + this.getDisplayId(), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
	}

	public String getDisplayId() {
		return this.getListingType().toString().substring(0, 1) + this.getId();
	}

	public MarketListingType getListingType() {
		if (this.mListingType == null) {
			return MarketListingType.BAZAAR;
		}
		return mListingType;
	}

	public String getItemName() {
		if (this.mItemName == null) {
			return ItemUtils.getPlainNameOrDefault(getItemToSell());
		}
		return mItemName;
	}

	private String convertCurrencyItemStackToSmallestCurrencyName(ItemStack currencyToBuy) {
		WalletManager.CompressionInfo info = WalletManager.getCompressionInfo(currencyToBuy);
		if (info != null) {
			return ItemUtils.getPlainNameOrDefault(info.mBase);
		}
		// at this point, we expect the item to be a currency.
		if (WalletManager.isCurrency(currencyToBuy)) {
			return ItemUtils.getPlainNameOrDefault(currencyToBuy);
		}
		return "ERROR";
	}

	public String getCurrencyName() {
		if (mCurrencyName == null || mCurrencyName.equals("ERROR")) {
			return convertCurrencyItemStackToSmallestCurrencyName(this.getCurrencyToBuy());
		}
		return mCurrencyName;
	}

	public ItemType getItemType() {
		if (mItemType == null) {
			return ItemUtils.getItemType(this.getItemToSell());
		}
		return mItemType;
	}

	public Tier getItemTier() {
		if (mItemTier == null) {
			return ItemStatUtils.getTier(this.getItemToSell());
		}
		return mItemTier;
	}

	public int getBundleSize() {
		if (mBundleSize == 0) {
			return 1;
		}
		return mBundleSize;
	}
}
