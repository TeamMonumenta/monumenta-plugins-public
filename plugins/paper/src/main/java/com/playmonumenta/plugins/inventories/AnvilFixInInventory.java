package com.playmonumenta.plugins.inventories;

import org.bukkit.Bukkit;
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
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;

import net.md_5.bungee.api.ChatColor;

public class AnvilFixInInventory implements Listener {
	private static final String REPAIR_OBJECTIVE = "RepairT";
	private Plugin mPlugin;

	public AnvilFixInInventory(Plugin plugin) {
		mPlugin = plugin;
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.NORMAL)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (!event.getClick().equals(ClickType.RIGHT)) {
			return;
		}

		ItemStack anvil = event.getCursor();
		if (anvil == null || !anvil.getType().equals(Material.ANVIL)) {
			return;
		}

		ItemStack item = event.getCurrentItem();

		if (item == null || item.getType().equals(Material.AIR) || ItemUtils.isShulkerBox(item.getType()) || item.getType().equals(Material.ANVIL)) {
			return;
		}

		Player player = (Player)event.getWhoClicked();
		if ((item != null && item.getDurability() > 0 && !item.getType().isBlock()
		    && (!item.hasItemMeta() || !item.getItemMeta().hasLore()
		        || (!InventoryUtils.testForItemWithLore(item, "* Irreparable *")
		            && !InventoryUtils.testForItemWithLore(item, "Curse of Irreparability"))))) {
			item.setDurability((short) 0);
			anvil.subtract();
			World world = player.getWorld();

			world.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.0f);
			new BukkitRunnable() {
				int mTicks = 0;
				Location mParticleLoc = player.getLocation().add(0, 1, 0);
				@Override
				public void run() {
					mTicks++;
					if (mTicks >= 3) {
						this.cancel();
						world.playSound(mParticleLoc, Sound.BLOCK_STONE_BREAK, 1, 0.75f);
						world.playSound(mParticleLoc, Sound.BLOCK_STONE_BREAK, 1, 0.75f);
					}
				}

			}.runTaskTimer(mPlugin, 0, 7);

			int repCount = ScoreboardUtils.getScoreboardValue(player, REPAIR_OBJECTIVE).orElse(0) + 1;
			ScoreboardUtils.setScoreboardValue(player, REPAIR_OBJECTIVE, repCount);

			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
			                       "execute as " + player.getName() + " run function monumenta:mechanisms/item_repair/grant_repair_advancement");
			player.updateInventory();
			event.setCancelled(true);
		} else {
			player.sendMessage(ChatColor.RED + "This is not a valid item to repair!");
			event.setCancelled(true);
		}
	}
}
