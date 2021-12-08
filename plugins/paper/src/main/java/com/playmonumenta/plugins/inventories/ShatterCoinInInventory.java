package com.playmonumenta.plugins.inventories;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;

import net.md_5.bungee.api.ChatColor;

public class ShatterCoinInInventory implements Listener {
	private Plugin mPlugin;

	public ShatterCoinInInventory(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		Player player = (Player)event.getWhoClicked();

		if (!event.getClick().equals(ClickType.RIGHT)) {
			return;
		}

		ItemStack coin = event.getCursor();

		if (coin == null || !InventoryUtils.testForItemWithName(coin, ChatColor.GOLD + "" + ChatColor.BOLD + "Shattered Denarius")) {
			return;
		}

		ItemStack item = event.getCurrentItem();

		if (item == null || item.getType().equals(Material.AIR)
				|| InventoryUtils.testForItemWithName(item, ChatColor.GOLD + "" + ChatColor.BOLD + "Shattered Denarius")) {
			return;
		}

		if ((item != null && ItemUtils.isItemShattered(item) && item.getAmount() == 1)) {
			ItemUtils.reforgeItem(item);
			coin.subtract();
			World world = player.getWorld();

			world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.45f, 0.8f);
			new BukkitRunnable() {
				int mTicks = 0;
				Location mParticleLoc = player.getLocation().add(0, 1, 0);
				@Override
				public void run() {
					mTicks++;
					if (mTicks >= 3) {
						this.cancel();
						world.playSound(mParticleLoc, Sound.BLOCK_BELL_USE, 3.0f, 1.25f);
						world.playSound(mParticleLoc, Sound.BLOCK_BELL_USE, 3.0f, 1.25f);
					}
				}

			}.runTaskTimer(mPlugin, 0, 7);
			player.updateInventory();
			event.setCancelled(true);
		} else if (item != null && item.getAmount() > 1) {
			player.sendMessage(ChatColor.RED + "Cannot reforge stacks of items!");
			event.setCancelled(true);
		} else {
			player.sendMessage(ChatColor.RED + "This item cannot be reforged!");
			event.setCancelled(true);
		}
	}
}
