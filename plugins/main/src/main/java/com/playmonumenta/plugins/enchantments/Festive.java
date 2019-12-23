package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class Festive implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Festive";
	private static final int tickPeriod = 5;
	private static final Particle.DustOptions FESTIVE_RED_COLOR = new Particle.DustOptions(Color.fromRGB(255, 98, 71), 1.0f);
	private static final Particle.DustOptions FESTIVE_GREEN_COLOR = new Particle.DustOptions(Color.fromRGB(75, 200, 0), 1.0f);
	private static final Set<Player> NO_SELF_PARTICLES = new HashSet<Player>();

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
	public int getLevelFromItem(ItemStack item, Player player) {
		if (player.getScoreboardTags().contains("noSelfParticles")) {
			NO_SELF_PARTICLES.add(player);
		} else {
			NO_SELF_PARTICLES.remove(player);
		}
		return getLevelFromItem(item);
	}

	@Override
	public void tick(Plugin plugin, World world, Player player, int level) {
		final Location loc = player.getLocation().add(0, 1, 0);
		if (NO_SELF_PARTICLES.contains(player)) {
			for (Player other : PlayerUtils.playersInRange(player, 30, false)) {
				other.spawnParticle(Particle.REDSTONE, loc, Math.max(6, 2 + level), 0.4, 0.4, 0.4, FESTIVE_RED_COLOR);
				other.spawnParticle(Particle.REDSTONE, loc, Math.max(6, 2 + level), 0.4, 0.4, 0.4, FESTIVE_GREEN_COLOR);
				other.spawnParticle(Particle.SNOWBALL, loc, Math.max(3, level), 0.4, 0.4, 0.4, 0);
			}
		} else {
			world.spawnParticle(Particle.REDSTONE, loc, Math.max(6, 2 + level), 0.4, 0.4, 0.4, FESTIVE_RED_COLOR);
			world.spawnParticle(Particle.REDSTONE, loc, Math.max(6, 2 + level), 0.4, 0.4, 0.4, FESTIVE_GREEN_COLOR);
			world.spawnParticle(Particle.SNOWBALL, loc, Math.max(3, level), 0.4, 0.4, 0.4, 0);
		}
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

				final Location loc = item.getLocation().add(0, 0.15, 0);
				world.spawnParticle(Particle.REDSTONE, loc, 3, 0.2, 0.2, 0.2, FESTIVE_RED_COLOR);
				world.spawnParticle(Particle.REDSTONE, loc, 3, 0.2, 0.2, 0.2, FESTIVE_GREEN_COLOR);
				world.spawnParticle(Particle.SNOWBALL, loc, 3, 0.2, 0.2, 0.2, 0);

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
