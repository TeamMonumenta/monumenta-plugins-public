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

public class Gilded implements BaseEnchantment {
	private static final Particle.DustOptions GILDED_1_COLOR = new Particle.DustOptions(Color.fromRGB(191, 166, 51), 1.0f);
	private static final Particle.DustOptions GILDED_2_COLOR = new Particle.DustOptions(Color.fromRGB(210, 191, 76), 1.0f);
	private static final Particle.DustOptions GILDED_3_COLOR = new Particle.DustOptions(Color.fromRGB(229, 229, 128), 1.0f);
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Gilded";
	private static final int tickPeriod = 6;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean useEnchantLevels() {
		return false;
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
		switch (level) {
		case 0:
			break;
		case 1:
			world.spawnParticle(Particle.REDSTONE, player.getLocation().add(0, 0.8, 0), 4, 0.3, 0.5, 0.3, GILDED_1_COLOR);
			break;
		case 2:
			world.spawnParticle(Particle.REDSTONE, player.getLocation().add(0, 0.8, 0), 4, 0.3, 0.5, 0.3, GILDED_2_COLOR);
			break;
		case 3:
		default:
			world.spawnParticle(Particle.REDSTONE, player.getLocation().add(0, 0.8, 0), 4, 0.3, 0.5, 0.3, GILDED_3_COLOR);
			break;
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

				item.getWorld().spawnParticle(Particle.REDSTONE, item.getLocation(), 1, 0.1, 0.1, 0.1, GILDED_1_COLOR);

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
