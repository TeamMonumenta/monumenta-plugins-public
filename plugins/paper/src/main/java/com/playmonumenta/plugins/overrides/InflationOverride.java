package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class InflationOverride extends BaseOverride {
	public static final Material itemMaterial = Material.DIAMOND_SWORD;

	@Override
	public boolean inventoryClickEvent(Plugin plugin, Player player, ItemStack item, InventoryClickEvent event) {
		if (event.getClick() != ClickType.RIGHT) { // We only care about right clicks in this override
			return true;
		}

		int inflationTier = getInflationTier(item);
		if (inflationTier == 0) {
			return true;
		}
		if (!player.getUniqueId().equals(ItemStatUtils.getInfuser(item, ItemStatUtils.InfusionType.SOULBOUND))) {
			return true;
		}

		final String lootTableBase = "epic:r2/items/patreon/inflation/";
		ItemStack cursor = event.getCursor();

		if (isMoneyStack(cursor) && inflationTier < 32) { // Right-click the sword with 64 money to increase its 'Tier' by 1 and consume the money
			// Increase the Inflation Tier
			inflationTier++;
			ItemStack newItem = InventoryUtils.getItemFromLootTable(player.getLocation(), NamespacedKeyUtils.fromString(lootTableBase + (inflationTier <= 9 ? "0" + inflationTier : inflationTier)));
			if (newItem == null) {
				return true;
			}

			// Carry over infusions
			newItem = ItemStatUtils.copyPlayerModified(item, newItem);

			// Set new item, delete money
			event.setCurrentItem(newItem);
			player.setItemOnCursor(null);
			player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 1.6f);

			return false;
		} else if (inflationTier > 1 && ItemUtils.isNullOrAir(cursor)) { // Right-click the sword to extract the money and deduct 1 from its 'Tier'
			// Get the refund amount for later
			ItemStack moneyRefund = InventoryUtils.getItemFromLootTable(player.getLocation(), NamespacedKeyUtils.fromString("epic:r2/items/currency/hyper_crystalline_shard"));
			if (moneyRefund == null) {
				return true;
			}
			moneyRefund.setAmount(64);

			// Make sure the refund can fit in the player's inventory, else cancel with notification
			if (!InventoryUtils.canFitInInventory(moneyRefund, player.getInventory())) {
				player.sendMessage(Component.text("There's not enough room in your inventory!", NamedTextColor.RED));
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.2f);
				return false;
			}

			// Decrease the Inflation Tier
			inflationTier--;
			ItemStack newItem = InventoryUtils.getItemFromLootTable(player.getLocation(), NamespacedKeyUtils.fromString(lootTableBase + (inflationTier <= 9 ? "0" + inflationTier : inflationTier)));
			if (newItem == null) {
				return true;
			}

			// Carry over infusions
			newItem = ItemStatUtils.copyPlayerModified(item, newItem);

			// Set new item, refund money
			event.setCurrentItem(newItem);
			InventoryUtils.giveItem(player, moneyRefund);
			player.playSound(player.getLocation(), Sound.BLOCK_LADDER_PLACE, SoundCategory.PLAYERS, 1.0f, 2.0f);

			return false;
		}
		return true;
	}

	private int getInflationTier(ItemStack item) {
		if (item == null || item.getType() != itemMaterial) {
			return 0;
		}
		String[] name = ItemUtils.getPlainNameIfExists(item).split(" ");
		if (name[0].equals("Inflation") && name.length == 2) {
			return StringUtils.toArabic(name[1]);
		} else if (name[0].equals("Hyperinflation")) {
			return 32;
		}
		return 0;
	}

	private boolean isMoneyStack(ItemStack item) {
		if (item == null || item.getType() != Material.NETHER_STAR) {
			return false;
		} else if (ItemStatUtils.getInfusionLevel(item, ItemStatUtils.InfusionType.SHATTERED) > 0) {
			return false;
		}
		return ItemUtils.getPlainNameIfExists(item).equals("Hyper Crystalline Shard") && item.getAmount() == 64;
	}
}
