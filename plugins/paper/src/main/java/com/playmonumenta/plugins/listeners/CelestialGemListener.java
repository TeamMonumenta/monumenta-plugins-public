package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.depths.charmfactory.CharmFactory;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class CelestialGemListener implements Listener {
	public static final String ITEM_NAME = "Celestial Gem";
	private static final Material ITEM_MATERIAL = Material.AMETHYST_SHARD;
	public static final String HAS_USED_KEY = "CELESTIAL_GEM_USED";

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}

		ItemStack item = event.getCurrentItem();
		if (ItemUtils.isNullOrAir(item)) {
			return;
		}
		if (event.getClick() != ClickType.RIGHT) {
			return;
		}
		// only run if the item is in the player's inventory
		if (!(event.getClickedInventory() instanceof PlayerInventory)) {
			return;
		}


		ItemStack cursor = event.getCursor();
		if (!isCelestialGem(cursor)) {
			return;
		}
		if (!ItemStatUtils.isZenithCharm(item)) {
			player.sendMessage(Component.text("You can only use " + ITEM_NAME + "s on Zenith Charms!", NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.2f);
			event.setCancelled(true);
			return;
		}
		if (cursor.getAmount() > 1) {
			player.sendMessage(Component.text("You cannot use multiple " + ITEM_NAME + "s at once!", NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.2f);
			event.setCancelled(true);
			return;
		}
		if (item.getAmount() > 1) {
			player.sendMessage(Component.text("You cannot upgrade multiple charms at once!", NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.2f);
			event.setCancelled(true);
			return;
		}
		if (CharmFactory.getZenithCharmRarity(item) >= 5) {
			player.sendMessage(Component.text("You cannot use a " + ITEM_NAME + " on a legendary charm!", NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.2f);
			event.setCancelled(true);
			return;
		}
		if (hasUsedBefore(item)) {
			player.sendMessage(Component.text("You cannot use a " + ITEM_NAME + " twice on the same charm!", NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.2f);
			event.setCancelled(true);
			return;
		}

		player.setItemOnCursor(null);

		ItemStack newCharm = CharmFactory.upgradeCharm(item);
		if (newCharm != null) {
			item.setItemMeta(newCharm.getItemMeta());
		}

		Location loc = player.getLocation();
		player.playSound(loc, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 0.35f, 2.0f);
		player.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.25f, 2.0f);
		player.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.0f, 0.5f);
		player.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
		player.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.0f, 1.5f);
		player.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.0f, 2.0f);

		event.setCancelled(true);
	}

	public static boolean isCelestialGem(ItemStack item) {
		if (item == null || item.getType() != ITEM_MATERIAL) {
			return false;
		}
		return ItemUtils.getPlainNameIfExists(item).equals(ITEM_NAME);
	}

	public static boolean hasUsedBefore(ItemStack item) {
		if (!ItemStatUtils.isZenithCharm(item)) {
			return false;
		}

		NBTItem nbt = new NBTItem(item);
		ReadWriteNBT playerModified = ItemStatUtils.getPlayerModified(nbt);
		if (playerModified == null) {
			return false;
		}
		return playerModified.getBoolean(HAS_USED_KEY);
	}
}
