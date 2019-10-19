package com.playmonumenta.plugins.enchantments;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;

public class PestilenceTesseract implements BaseEnchantment {
	private static final String PROPERTY_NAME = "PestilenceTesseract";
	private static final int TICK_PERIOD = 20;
	private static final int MAX_LIFETIME_SECONDS = 60;
	private static final double EFFECT_RADIUS = 5;
	private static final double PARTICLE_RING_HEIGHT = 1.0;

	private static final Collection<Map.Entry<Double, SpawnParticleAction>> PARTICLES =
		Arrays.asList(new AbstractMap.SimpleEntry<Double, SpawnParticleAction>(0.4, (Location loc) -> loc.getWorld().spawnParticle(Particle.CRIT, loc, 1, 0, 0, 0, 0)));

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.NONE);
	}

	@Override
	public boolean hasOnSpawn() {
		return true;
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

				ParticleUtils.explodingRingEffect(plugin, item.getLocation().add(0, 0.5, 0), EFFECT_RADIUS, PARTICLE_RING_HEIGHT, TICK_PERIOD, PARTICLES);

				for (LivingEntity mob : EntityUtils.getNearbyMobs(item.getLocation(), EFFECT_RADIUS)) {
					mob.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 600, 2, false, true));
					mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 600, 1, false, true));
				}

				numTicks++;

				if (numTicks >= MAX_LIFETIME_SECONDS * Constants.TICKS_PER_SECOND / TICK_PERIOD) {
					item.remove();
					this.cancel();
				}

				// Very infrequently check if the item is still actually there
				if (numTicks % 100 == 0) {
					if (!EntityUtils.isStillLoaded(item)) {
						this.cancel();
					}
				}
			}
		}.runTaskTimer(plugin, 0, TICK_PERIOD);
	}
}
