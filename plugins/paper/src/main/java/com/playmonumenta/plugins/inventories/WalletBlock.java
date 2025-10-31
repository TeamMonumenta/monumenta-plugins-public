package com.playmonumenta.plugins.inventories;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.integrations.luckperms.GuildPermission;
import com.playmonumenta.plugins.integrations.luckperms.GuildPlotUtils;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.mail.recipient.GuildRecipient;
import com.playmonumenta.plugins.mail.recipient.MailDirection;
import com.playmonumenta.plugins.mail.recipient.Recipient;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.io.File;
import java.util.Iterator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class WalletBlock extends BaseWallet {
	public boolean mIsOwnerLoaded = true;
	public @Nullable Recipient mOwner;
	public final String mWorldName;
	public final int mX;
	public final int mY;
	public final int mZ;

	public static File getFile(BlockState blockState) {
		return FileUtils.getBlockMonumentaFile(blockState, "wallet_block.", ".json");
	}

	public WalletBlock(BlockState blockState) {
		Location loc = blockState.getLocation();
		mWorldName = loc.getWorld().getName();
		mX = loc.getBlockX();
		mY = loc.getBlockY();
		mZ = loc.getBlockZ();
	}

	private WalletBlock(
		String worldName,
		int x,
		int y,
		int z
	) {
		mWorldName = worldName;
		mX = x;
		mY = y;
		mZ = z;
	}

	public @Nullable Location getLocation() {
		World world = Bukkit.getWorld(mWorldName);
		if (world == null) {
			return null;
		}
		return new Location(world, mX, mY, mZ);
	}

	@Override
	public ItemStack ownerIcon() {
		return ownerIcon(mOwner);
	}

	@Override
	public ItemStack ownerIcon(@Nullable Recipient possibleOwner) {
		if (possibleOwner == null) {
			return GUIUtils.createBasicItem(Material.BARRIER, Component.text("No owner", NamedTextColor.GREEN)
				.decoration(TextDecoration.ITALIC, false));
		}
		ItemStack result = possibleOwner.icon(MailDirection.DEFAULT);
		ItemMeta meta = result.getItemMeta();
		Component name = meta.displayName();
		if (name == null) {
			// This shouldn't happen
			name = Component.text("Unknown?", NamedTextColor.RED);
		}
		name = Component.text("Owned by ", NamedTextColor.GREEN)
			.decoration(TextDecoration.ITALIC, false)
			.append(name);
		meta.displayName(name);
		result.setItemMeta(meta);
		return result;
	}

	@Override
	public boolean canNotChangeOwner(Player player) {
		if (GuildPlotUtils.guildPlotChangeVaultOwnerBlocked(player)) {
			return true;
		}

		if (mOwner == null) {
			return false;
		}

		return mOwner.nonMemberCheck(player, GuildPermission.EDIT_VAULT_OWNERSHIP);
	}

	@Override
	public boolean canNotAccess(Player player) {
		if (GuildPlotUtils.guildPlotUseVaultBlocked(player)) {
			return true;
		}

		if (mOwner == null) {
			return false;
		}
		return mOwner.nonMemberCheck(player, GuildPermission.USE_VAULT);
	}

	@Override
	public void setOwner(@Nullable Recipient recipient) {
		mOwner = recipient;
		mIsOwnerLoaded = true;
		onUpdate();
	}

	@Override
	public boolean isLoaded() {
		if (!mIsOwnerLoaded) {
			return false;
		}

		Location loc = getLocation();
		if (loc == null) {
			return false;
		}
		if (!loc.isChunkLoaded()) {
			return false;
		}
		Block block = loc.getBlock();
		return SharedVaultManager.isSharedVault(block);
	}

	@Override
	public void logAddItem(Player player, ItemStack currency) {
		AuditListener.logPlayer("+AddItemToWalletBlock: " + player.getName() + " added to wallet at "
			+ this + "`: " + AuditListener.getItemLogString(currency));
	}

	@Override
	public void logRemoveItem(Player player, ItemStack currency) {
		AuditListener.logPlayer("-RemoveItemFromWalletBlock: " + player.getName() + " added to wallet at"
			+ this + "`: " + AuditListener.getItemLogString(currency));
	}

	@Override
	public WalletBlock deepClone() {
		return new WalletBlock(
			mWorldName,
			mX,
			mY,
			mZ
		);
	}

	@Override
	public JsonObject serialize() {
		JsonObject json = super.serialize();

		if (mOwner != null) {
			json.add("owner", mOwner.toJson());
		}

		if (!mIsOwnerLoaded) {
			// Don't save recipient back to the block if it's not loaded successfully
			return json;
		}

		Location loc = getLocation();
		if (loc == null) {
			return json;
		}

		BlockState blockState = loc.getBlock().getState();
		File walletBlockFile = getFile(blockState);
		String walletBlockPath = walletBlockFile.getPath();
		try {
			FileUtils.writeJsonSafely(walletBlockPath, json, false);
		} catch (Exception ex) {
			String errorMessage = "Failed to write wallet data at " + walletBlockPath + ": " + ex;
			MMLog.severe(errorMessage, ex);
			MonumentaNetworkRelayIntegration.sendAdminMessage(errorMessage);
			// Effectively prevents further damage
			mIsOwnerLoaded = false;
		}

		return json;
	}

	public static WalletBlock deserialize(BlockState blockState) {
		WalletBlock wallet = new WalletBlock(blockState);

		JsonObject json = null;
		File walletBlockFile = getFile(blockState);
		if (walletBlockFile.isFile()) {
			String walletBlockPath = walletBlockFile.getPath();
			try {
				json = FileUtils.readJson(walletBlockPath);
			} catch (Exception ex) {
				String errorMessage = "Failed to load wallet data at " + walletBlockPath + ": " + ex;
				MMLog.warning(errorMessage, ex);
				MonumentaNetworkRelayIntegration.sendAdminMessage(errorMessage);
				// Effectively prevents further damage
				wallet.mIsOwnerLoaded = false;
			}
		}

		if (json == null) {
			return wallet;
		}

		JsonElement ownerElement = json.get("owner");
		if (ownerElement instanceof JsonObject ownerObject) {
			wallet.mIsOwnerLoaded = false;
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				Recipient recipient;
				try {
					recipient = Recipient.fromJson(ownerObject).join();
				} catch (Throwable throwable) {
					MMLog.warning("Unable to get owner of wallet block:");
					MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), throwable);

					recipient = new GuildRecipient(GuildRecipient.DUMMY_ID_NO_GUILD, null);
				}
				Recipient finalRecipient = recipient;
				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
					wallet.mOwner = finalRecipient;
					wallet.mIsOwnerLoaded = true;
				});
			});
		}

		if (!(json.get("items") instanceof JsonArray itemsJsonArray)) {
			return wallet;
		}
		for (JsonElement item : itemsJsonArray) {
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
	public void onUpdate() {
		super.onUpdate();
		serialize();
	}

	@Override
	public String toString() {
		return "`/world " + mWorldName + "` `/tp @s " + mX + " " + mY + " " + mZ;
	}

	@Override
	public int hashCode() {
		int result = mWorldName.hashCode();
		result = result * 31 + mX;
		result = result * 31 + mY;
		result = result * 31 + mZ;
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof WalletBlock other)) {
			return false;
		}
		return mWorldName.equals(other.mWorldName)
			&& mX == other.mX
			&& mY == other.mY
			&& mZ == other.mZ;
	}
}
