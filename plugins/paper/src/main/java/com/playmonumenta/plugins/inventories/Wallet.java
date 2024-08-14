package com.playmonumenta.plugins.inventories;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class Wallet {

	public final UUID mOwner;
	public final ArrayList<WalletItem> mItems = new ArrayList<>();

	Wallet(UUID owner) {
		mOwner = owner;
	}

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

	private @Nullable Wallet.WalletItem find(ItemStack currency) {
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

		if (!player.getUniqueId().equals(mOwner)) {
			AuditListener.log("+AddItemToWallet: " + player.getName() + " added to wallet of "
				                  + MonumentaRedisSyncIntegration.cachedUuidToName(mOwner) + ": " + AuditListener.getItemLogString(currency));
		}

		for (WalletItem walletItem : mItems) {
			if (walletItem.mItem.isSimilar(currency)) {
				walletItem.mAmount += currency.getAmount();
				currency.setAmount(0);
				return;
			}
		}

		ItemStack clone = ItemUtils.clone(currency);
		clone.setAmount(1);
		mItems.add(new WalletItem(clone, currency.getAmount()));
		currency.setAmount(0);
	}

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
					if (currency.getAmount() > 0 && !player.getUniqueId().equals(mOwner)) {
						AuditListener.log("-RemoveItemFromWallet: " + player.getName() + " removed from wallet of "
							                  + MonumentaRedisSyncIntegration.cachedUuidToName(mOwner) + ": " + AuditListener.getItemLogString(currency));
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
				if (currency.getAmount() > 0 && !player.getUniqueId().equals(mOwner)) {
					AuditListener.log("-RemoveItemFromWallet: " + player.getName() + " removed from wallet of "
						                  + MonumentaRedisSyncIntegration.cachedUuidToName(mOwner) + ": " + AuditListener.getItemLogString(currency));
				}
				return;
			}
		}

		currency.setAmount(0);
	}

	/**
	 * Create a clone of this wallet. The returned wallet can freely be modified without affecting the real wallet.
	 * Care should be taken to perform operations with the correct player, as audit messages can be logged otherwise still.
	 */
	public Wallet deepClone() {
		Wallet clone = new Wallet(mOwner);
		mItems.stream()
			.map(item -> new WalletItem(ItemUtils.clone(item.mItem), item.mAmount))
			.forEach(clone.mItems::add);
		return clone;
	}

	JsonObject serialize() {
		JsonObject json = new JsonObject();
		JsonArray itemsArray = new JsonArray();
		for (WalletItem item : mItems) {
			itemsArray.add(item.serialize());
		}
		json.add("items", itemsArray);
		return json;
	}

	static Wallet deserialize(Player player, JsonObject json) {
		Wallet wallet = new Wallet(player.getUniqueId());
		for (JsonElement item : json.getAsJsonArray("items")) {
			// code to combine duplicate bag of hoarding items together
			WalletItem walletItem = WalletItem.deserialize(item.getAsJsonObject());
			if (ItemUtils.isNullOrAir(walletItem.mItem) || walletItem.mAmount <= 0) { // item has been removed from the game
				continue;
			}
			// add duplicate compressed items differently
			WalletManager.CompressionInfo info = WalletManager.getCompressionInfo(walletItem.mItem);
			if (info != null) {
				boolean found = false;
				for (Iterator<WalletItem> it = wallet.mItems.iterator(); it.hasNext(); ) {
					WalletItem otherWalletItem = it.next();
					if (otherWalletItem.mItem.isSimilar(info.mBase)) {
						otherWalletItem.mAmount += walletItem.mAmount * info.mAmount;
						found = true;
						break;
					}
				}
				// if found, skip creating an item
				if (found) {
					continue;
				} else {
					walletItem = new WalletItem(info.mBase, walletItem.mAmount * info.mAmount);
				}
			} else {
				// perform basic deduplication
				for (Iterator<WalletItem> it = wallet.mItems.iterator(); it.hasNext(); ) {
					WalletItem otherWalletItem = it.next();
					if (otherWalletItem.mItem.isSimilar(walletItem.mItem)) {
						walletItem.mAmount += otherWalletItem.mAmount;
						it.remove();
					}
				}
			}
			wallet.mItems.add(walletItem);
		}
		return wallet;
	}

}
