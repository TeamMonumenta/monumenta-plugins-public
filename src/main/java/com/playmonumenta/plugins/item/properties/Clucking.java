package com.playmonumenta.plugins.item.properties;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.item.properties.ItemPropertyManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;

/*
 * Plays chicken clucking sounds when in inventory or dropped
 */
public class Clucking implements ItemProperty {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Clucking";
	private static final int droppedTickPeriod = 60;

	/* This is shared by all instances */
	private static int staticTicks = 0;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.INVENTORY);
	}

	@Override
	public boolean hasOnSpawn() {
		return true;
	}

	@Override
	public void tick(Plugin plugin, World world, Player player, int level) {
		staticTicks++;

		/*
		 * Max - 60 items or more is every 3s (60 ticks)
		 * Min - 1 item is every 33s (650 ticks)
		 */
		if (level > 60) {
			level = 60;
		}

		int modulo = 60 + (600 - (level * 10));

		// Since this is only called once per second
		modulo = modulo / 20;

		if (staticTicks % modulo == 0) {
			world.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_AMBIENT, SoundCategory.HOSTILE, 1.0f, 0.8f);
		}
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, int level) {
		new BukkitRunnable() {
			int numTicks = 0;

			@Override
			public void run() {
				if (item == null || item.isDead()) {
					this.cancel();
				}

				item.getWorld().playSound(item.getLocation(), Sound.ENTITY_CHICKEN_AMBIENT, SoundCategory.HOSTILE, 1.0f, 0.8f);

				// Very infrequently check if the item is still actually there
				numTicks++;
				if (numTicks > 200) {
					numTicks = 0;
					if (!EntityUtils.isStillLoaded(item)) {
						this.cancel();
					}
				}
			}
		}.runTaskTimer(plugin, 10, droppedTickPeriod);
	}
}
