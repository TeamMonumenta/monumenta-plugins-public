package com.playmonumenta.plugins.item.properties;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.item.properties.ItemPropertyManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Festive implements ItemProperty {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Festive";
	private static final int tickPeriod = 6;

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
		world.spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation().add(0, 0.8, 0), 2, 0.3, 0.5, 0.3, 0);
		world.spawnParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 0.5, 0), 2, 0.3, 0.2, 0.3, 0);
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

				item.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, item.getLocation().add(0, 0.8, 0), 2, 0.1, 0.1, 0.1, 0);
				item.getWorld().spawnParticle(Particle.SPELL_WITCH, item.getLocation().add(0, 0.5, 0), 2, 0.1, 0.1, 0.1, 0);

				// Very infrequently check if the item is still actually there
				numTicks++;
				if (numTicks > 100) {
					numTicks = 0;
					if (!EntityUtils.isStillLoaded(item)) {
						this.cancel();
					}
				}
			}
		}.runTaskTimer(plugin, 10, tickPeriod);
	}
}
