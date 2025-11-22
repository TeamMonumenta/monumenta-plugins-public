package com.playmonumenta.plugins.inventories;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.WalletGui;
import com.playmonumenta.plugins.mail.recipient.Recipient;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.ArrayList;
import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A base wallet class, which can be owned by a player, block, or other object that doesn't require async behavior
 */
public abstract class BaseWallet {

	public final ArrayList<WalletItem> mItems = new ArrayList<>();

	public static class WalletItem {
		public final ItemStack mItem;
		public long mAmount;

		public WalletItem(ItemStack item, long amount) {
			mItem = item;
			mAmount = amount;
		}

		JsonObject serialize() {
			JsonObject json = new JsonObject();
			json.addProperty("item", ItemUtils.serializeItemStack(mItem));
			json.addProperty("amount", mAmount);
			return json;
		}

		static WalletItem deserialize(JsonObject json) {
			return new WalletItem(ItemUtils.parseItemStack(json.get("item").getAsString()), json.get("amount").getAsLong());
		}
	}

	public abstract ItemStack ownerIcon();

	public abstract ItemStack ownerIcon(@Nullable Recipient possibleOwner);

	public abstract boolean canNotChangeOwner(Player player);

	public abstract boolean canNotAccess(Player player);

	public void setOwner(@Nullable Recipient recipient) {
	}

	public abstract boolean isLoaded();

	public @Nullable WalletItem find(ItemStack currency) {
		for (WalletItem walletItem : mItems) {
			if (walletItem.mItem.isSimilar(currency)) {
				return walletItem;
			}
		}
		return null;
	}

	public void add(Player player, ItemStack currency) {
		if (ItemUtils.isNullOrAir(currency)) {
			throw new IllegalArgumentException("Tried to add air to wallet!");
		}

		// Compressed currencies are stored uncompressed
		WalletManager.CompressionInfo compressionInfo = WalletManager.getCompressionInfo(currency);
		if (compressionInfo != null) {
			int compressedAmount = currency.getAmount();
			currency.setAmount(0);
			ItemStack baseCurrency = ItemUtils.clone(compressionInfo.mBase);
			baseCurrency.setAmount(compressedAmount * compressionInfo.mAmount);
			add(player, baseCurrency);
			return;
		}

		logAddItem(player, currency);

		for (WalletItem walletItem : mItems) {
			if (walletItem.mItem.isSimilar(currency)) {
				walletItem.mAmount += currency.getAmount();
				currency.setAmount(0);
				onUpdate();
				return;
			}
		}

		ItemStack clone = ItemUtils.clone(currency);
		clone.setAmount(1);
		mItems.add(new WalletItem(clone, currency.getAmount()));
		currency.setAmount(0);
		onUpdate();
	}

	public abstract void logAddItem(Player player, ItemStack currency);

	/**
	 * Counts the number of items of the given type in the wallet.
	 * If passed a compressed currency, will return the number of whole compressed items (ignoring any uncompressed remainder).
	 */
	public long count(ItemStack currency) {
		WalletManager.CompressionInfo compressionInfo = WalletManager.getCompressionInfo(currency);
		if (compressionInfo != null) {
			return count(compressionInfo.mBase) / compressionInfo.mAmount;
		}
		WalletItem walletItem = find(currency);
		return walletItem != null ? walletItem.mAmount : 0;
	}

	/**
	 * Removes a certain quantity of currency from this wallet. Does not check if there actually is enough currency in the wallet.
	 * <p>
	 * See also {@link com.playmonumenta.plugins.utils.WalletUtils} for some utilities to pay a cost from the player's wallet and inventory.
	 *
	 * @param player The player that removes the items - used to audit mods removing stuff from other players' wallets
	 */
	public void remove(Player player, ItemStack currency) {
		if (ItemUtils.isNullOrAir(currency)) {
			return;
		}

		// If removing a compressed item, we need to actually remove uncompressed items as only those are stored.
		WalletManager.CompressionInfo compressionInfo = WalletManager.getCompressionInfo(currency);
		if (compressionInfo != null) {
			long toRemove = (long) currency.getAmount() * compressionInfo.mAmount;
			for (Iterator<WalletItem> iterator = mItems.iterator(); iterator.hasNext(); ) {
				WalletItem walletItem = iterator.next();
				if (walletItem.mItem.isSimilar(compressionInfo.mBase)) {
					if (walletItem.mAmount > toRemove) {
						walletItem.mAmount -= toRemove;
					} else {
						// Round down to the nearest multiple of compressionInfo.mAmount
						long adjustedContainedAmount = (walletItem.mAmount / compressionInfo.mAmount) * compressionInfo.mAmount;
						if (adjustedContainedAmount == walletItem.mAmount) {
							iterator.remove();
						} else {
							walletItem.mAmount -= adjustedContainedAmount;
						}
						currency.setAmount((int) (adjustedContainedAmount / compressionInfo.mAmount));
					}
					if (currency.getAmount() > 0) {
						logRemoveItem(player, currency);
						onUpdate();
					}
					return;
				}
			}
			// fall through to normal case in case a compression was added later on
		}

		// Normal item case
		for (Iterator<WalletItem> iterator = mItems.iterator(); iterator.hasNext(); ) {
			WalletItem walletItem = iterator.next();
			if (walletItem.mItem.isSimilar(currency)) {
				if (currency.getAmount() >= walletItem.mAmount) {
					currency.setAmount((int) walletItem.mAmount);
					iterator.remove();
				} else {
					walletItem.mAmount -= currency.getAmount();
				}
				if (currency.getAmount() > 0) {
					logRemoveItem(player, currency);
				}
				onUpdate();
				return;
			}
		}

		currency.setAmount(0);
		onUpdate();
	}

	public abstract void logRemoveItem(Player player, ItemStack currency);

	/**
	 * Create a clone of this wallet. The returned wallet can freely be modified without affecting the real wallet.
	 */
	public abstract BaseWallet deepClone();

	JsonObject serialize() {
		JsonObject json = new JsonObject();
		JsonArray itemsArray = new JsonArray();
		for (WalletItem item : mItems) {
			itemsArray.add(item.serialize());
		}
		json.add("items", itemsArray);
		return json;
	}

	// Implement this in your subclass
	//static Wallet deserialize(Player player, JsonObject json);

	public void onUpdate() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (Gui.getOpenGui(player) instanceof WalletGui walletGui) {
				walletGui.updateIfWalletMatches(this);
			}
		}
	}
}
