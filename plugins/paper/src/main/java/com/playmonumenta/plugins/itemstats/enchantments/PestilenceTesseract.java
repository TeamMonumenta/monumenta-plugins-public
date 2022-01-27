package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class PestilenceTesseract implements Enchantment {

	private static final int TICK_PERIOD = 20;
	private static final int MAX_LIFETIME_SECONDS = 60;
	private static final double EFFECT_RADIUS = 5;
	private static final double PARTICLE_RING_HEIGHT = 1.0;
	private static final Collection<Map.Entry<Double, ParticleUtils.SpawnParticleAction>> PARTICLES = Arrays.asList(new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(0.4, (Location loc) -> loc.getWorld().spawnParticle(Particle.CRIT, loc, 1, 0, 0, 0, 0)));

	@Override
	public String getName() {
		return "PestilenceTesseract";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.PESTILENCE_TESSERACT;
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, double value) {
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (!item.getLocation().isChunkLoaded() || item.isDead() || !item.isValid()) {
					this.cancel();
					return;
				}

				ParticleUtils.explodingRingEffect(plugin, item.getLocation().add(0, 0.5, 0), EFFECT_RADIUS, PARTICLE_RING_HEIGHT, TICK_PERIOD, PARTICLES);

				for (LivingEntity mob : EntityUtils.getNearbyMobs(item.getLocation(), EFFECT_RADIUS)) {
					if (!(mob instanceof Player) && !ZoneUtils.hasZoneProperty(mob.getLocation(), ZoneUtils.ZoneProperty.RESIST_5)) {
						plugin.mEffectManager.addEffect(mob, Decay.DOT_EFFECT_NAME, new CustomDamageOverTime(600, 1, 10, null, null, Particle.SQUID_INK));
						EntityUtils.applySlow(plugin, 600, 0.2, mob);
					}
				}

				mTicks++;

				if (mTicks >= MAX_LIFETIME_SECONDS * Constants.TICKS_PER_SECOND / TICK_PERIOD) {
					item.remove();
					this.cancel();
				}

				// Very infrequently check if the item is still actually there
				if (mTicks % 100 == 0) {
					if (!EntityUtils.isStillLoaded(item)) {
						this.cancel();
					}
				}
			}
		}.runTaskTimer(plugin, 0, TICK_PERIOD);
	}
}
