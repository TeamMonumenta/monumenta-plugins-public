package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Festive implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Festive";
	private static final int tickPeriod = 5;
	private static final Particle.DustOptions FESTIVE_RED_COLOR = new Particle.DustOptions(Color.fromRGB(255, 98, 71), 1.0f);
	private static final Particle.DustOptions FESTIVE_GREEN_COLOR = new Particle.DustOptions(Color.fromRGB(75, 200, 0), 1.0f);

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
		world.spawnParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 4, 0.4, 0.4, 0.4, FESTIVE_RED_COLOR);
		world.spawnParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 4, 0.4, 0.4, 0.4, FESTIVE_GREEN_COLOR);
		world.spawnParticle(Particle.SNOWBALL, player.getLocation().add(0, 1, 0), 2, 0.4, 0.4, 0.4, 0);
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, int level) {
		new BukkitRunnable() {
			int numTicks = 0;
			World world = item.getWorld();
			@Override
			public void run() {
				if (item == null || item.isDead()) {
					this.cancel();
				}

				world.spawnParticle(Particle.REDSTONE, item.getLocation().add(0, 0.15, 0), 3, 0.2, 0.2, 0.2, FESTIVE_RED_COLOR);
				world.spawnParticle(Particle.REDSTONE, item.getLocation().add(0, 0.15, 0), 3, 0.2, 0.2, 0.2, FESTIVE_GREEN_COLOR);
				world.spawnParticle(Particle.SNOWBALL, item.getLocation().add(0, 0.15, 0), 3, 0.2, 0.2, 0.2, 0);

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
