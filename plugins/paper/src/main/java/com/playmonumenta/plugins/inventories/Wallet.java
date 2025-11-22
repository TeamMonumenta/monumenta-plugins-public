package com.playmonumenta.plugins.inventories;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.mail.recipient.Recipient;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class Wallet extends BaseWallet {

	public final UUID mOwner;

	Wallet(UUID owner) {
		mOwner = owner;
	}

	@Override
	public ItemStack ownerIcon() {
		return ownerIcon(null);
	}

	@Override
	public ItemStack ownerIcon(@Nullable Recipient possibleOwner) {
		String playerName = MonumentaRedisSyncIntegration.cachedUuidToName(mOwner);
		if (playerName == null) {
			playerName = mOwner.toString().toLowerCase(Locale.ENGLISH);
		}
		ItemStack result = ItemUtils.createPlayerHead(mOwner, playerName);
		ItemMeta meta = result.getItemMeta();
		meta.displayName(Component.text("Owned by " + playerName, NamedTextColor.YELLOW)
			.decoration(TextDecoration.ITALIC, false));
		result.setItemMeta(meta);
		return result;
	}

	@Override
	public boolean canNotChangeOwner(Player player) {
		return true;
	}

	@Override
	public boolean canNotAccess(Player player) {
		return !player.getUniqueId().equals(mOwner);
	}

	@Override
	public boolean isLoaded() {
		return Bukkit.getPlayer(mOwner) != null;
	}

	@Override
	public void logAddItem(Player player, ItemStack currency) {
		if (!player.getUniqueId().equals(mOwner)) {
			AuditListener.log("+AddItemToWallet: " + player.getName() + " added to wallet of "
				+ MonumentaRedisSyncIntegration.cachedUuidToName(mOwner) + ": " + AuditListener.getItemLogString(currency));
		}
	}

	@Override
	public void logRemoveItem(Player player, ItemStack currency) {
		if (!player.getUniqueId().equals(mOwner)) {
			AuditListener.log("-RemoveItemFromWallet: " + player.getName() + " removed from wallet of "
				+ MonumentaRedisSyncIntegration.cachedUuidToName(mOwner) + ": " + AuditListener.getItemLogString(currency));
		}
	}

	/**
	 * Create a clone of this wallet. The returned wallet can freely be modified without affecting the real wallet.
	 * Care should be taken to perform operations with the correct player, as audit messages can be logged otherwise still.
	 */
	@Override
	public Wallet deepClone() {
		Wallet clone = new Wallet(mOwner);
		mItems.stream()
			.map(item -> new WalletItem(ItemUtils.clone(item.mItem), item.mAmount))
			.forEach(clone.mItems::add);
		return clone;
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
				for (WalletItem otherWalletItem : wallet.mItems) {
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

	@Override
	public String toString() {
		String result = MonumentaRedisSyncIntegration.cachedUuidToName(mOwner);
		if (result == null) {
			return mOwner.toString();
		}
		return result;
	}

	@Override
	public int hashCode() {
		return mOwner.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Wallet other)) {
			return false;
		}
		return mOwner.equals(other.mOwner);
	}
}
