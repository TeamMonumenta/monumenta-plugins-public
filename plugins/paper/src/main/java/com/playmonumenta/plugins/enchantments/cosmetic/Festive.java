package com.playmonumenta.plugins.enchantments.cosmetic;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
import com.playmonumenta.plugins.enchantments.BaseSpawnableItemEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class Festive implements BaseSpawnableItemEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Festive";
	private static final int TICK_PERIOD = 5;
	private static final Particle.DustOptions FESTIVE_RED_COLOR = new Particle.DustOptions(Color.fromRGB(255, 98, 71), 1.0f);
	private static final Particle.DustOptions FESTIVE_GREEN_COLOR = new Particle.DustOptions(Color.fromRGB(75, 200, 0), 1.0f);
	private static final Set<UUID> NO_SELF_PARTICLES = new HashSet<>();

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean isMultiLevel() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.INVENTORY);
	}

	@Override
	public int getPlayerItemLevel(ItemStack itemStack, Player player, ItemSlot itemSlot) {
		if (player.getScoreboardTags().contains("noSelfParticles")) {
			NO_SELF_PARTICLES.add(player.getUniqueId());
		} else {
			NO_SELF_PARTICLES.remove(player.getUniqueId());
		}
		return BaseSpawnableItemEnchantment.super.getPlayerItemLevel(itemStack, player, itemSlot);
	}

	@Override
	public void tick(Plugin plugin, Player player, int level) {
		final Location loc = player.getLocation().add(0, 1, 0);
		if (NO_SELF_PARTICLES.contains(player.getUniqueId())) {
			for (Player other : PlayerUtils.otherPlayersInRange(player, 30, true)) {
				other.spawnParticle(Particle.REDSTONE, loc, Math.max(6, 2 + level), 0.4, 0.4, 0.4, FESTIVE_RED_COLOR);
				other.spawnParticle(Particle.REDSTONE, loc, Math.max(6, 2 + level), 0.4, 0.4, 0.4, FESTIVE_GREEN_COLOR);
				other.spawnParticle(Particle.SNOWBALL, loc, Math.max(3, level), 0.4, 0.4, 0.4, 0);
			}
		} else {
			World world = player.getWorld();
			world.spawnParticle(Particle.REDSTONE, loc, Math.max(6, 2 + level), 0.4, 0.4, 0.4, FESTIVE_RED_COLOR);
			world.spawnParticle(Particle.REDSTONE, loc, Math.max(6, 2 + level), 0.4, 0.4, 0.4, FESTIVE_GREEN_COLOR);
			world.spawnParticle(Particle.SNOWBALL, loc, Math.max(3, level), 0.4, 0.4, 0.4, 0);
		}
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, int level) {
		new BukkitRunnable() {
			int mTicks = 0;
			World mWorld = item.getWorld();
			@Override
			public void run() {
				if (item == null || item.isDead() || !item.isValid()) {
					this.cancel();
				}

				final Location loc = item.getLocation().add(0, 0.15, 0);
				mWorld.spawnParticle(Particle.REDSTONE, loc, 3, 0.2, 0.2, 0.2, FESTIVE_RED_COLOR);
				mWorld.spawnParticle(Particle.REDSTONE, loc, 3, 0.2, 0.2, 0.2, FESTIVE_GREEN_COLOR);
				mWorld.spawnParticle(Particle.SNOWBALL, loc, 3, 0.2, 0.2, 0.2, 0);

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
