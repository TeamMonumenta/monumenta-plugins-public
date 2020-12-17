package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class Gilded implements BaseEnchantment {
	private static final Particle.DustOptions GILDED_1_COLOR = new Particle.DustOptions(Color.fromRGB(191, 166, 51), 1.0f);
	private static final Particle.DustOptions GILDED_2_COLOR = new Particle.DustOptions(Color.fromRGB(210, 191, 76), 1.0f);
	private static final Particle.DustOptions GILDED_3_COLOR = new Particle.DustOptions(Color.fromRGB(229, 229, 128), 1.0f);
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Gilded";
	private static final int TICK_PERIOD = 6;
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
	public void tick(Plugin plugin, Player player, int level) {
		final Particle.DustOptions color;
		final int count;
		switch (level) {
		case 0:
			return;
		case 1:
			color = GILDED_1_COLOR;
			count = 3;
			break;
		case 2:
			count = 4;
			color = GILDED_2_COLOR;
			break;
		case 3:
		default:
			count = 5;
			color = GILDED_3_COLOR;
			break;
		}

		final Location loc = player.getLocation().add(0, 0.8, 0);
		if (NO_SELF_PARTICLES.contains(player)) {
			for (Player other : PlayerUtils.playersInRange(player, 30, false)) {
				other.spawnParticle(Particle.REDSTONE, loc, count, 0.3, 0.5, 0.3, color);
			}
		} else {
			player.getWorld().spawnParticle(Particle.REDSTONE, loc, count, 0.3, 0.5, 0.3, color);
		}
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, int level) {
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (item == null || item.isDead() || !item.isValid()) {
					this.cancel();
				}

				item.getWorld().spawnParticle(Particle.REDSTONE, item.getLocation(), 1, 0.1, 0.1, 0.1, GILDED_1_COLOR);

				// Very infrequently check if the item is still actually there
				mTicks++;
				if (mTicks > 100) {
					mTicks = 0;
					if (!EntityUtils.isStillLoaded(item)) {
						this.cancel();
					}
				}
			}
		}.runTaskTimer(plugin, 10, TICK_PERIOD);
	}
}
