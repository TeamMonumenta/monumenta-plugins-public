package com.playmonumenta.plugins.gallery.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.gallery.GalleryPlayer;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

public class GalleryFallingWrathEffect extends GalleryConsumableEffect {
	/**
	 * - After damaging an enemy with a projectile weapon, a """trident""" will fall from the sky at that location,
	 *  bypassing iframes and dealing 20% of that attack’s damage in a 3 block radius - last 3 wave
	 */

	private static final double EXPLOSION_RADIUS = 3;
	private static final double DAMAGE = 0.2;

	boolean mHasDoneDamageThisTick = false;

	public GalleryFallingWrathEffect() {
		super(GalleryEffectType.FALLING_WRATH);
	}

	@Override public void tick(GalleryPlayer player, boolean oneSecond, boolean twoHertz, int ticks) {
		super.tick(player, oneSecond, twoHertz, ticks);
		mHasDoneDamageThisTick = false;
	}

	@Override public void onPlayerDamage(GalleryPlayer player, DamageEvent event, LivingEntity entity) {
		if (event.getType() == DamageEvent.DamageType.PROJECTILE && !mHasDoneDamageThisTick) {
			mHasDoneDamageThisTick = true;
			summonExplosion(player, event, entity.getLocation());
		}
	}

	protected void summonExplosion(GalleryPlayer player, DamageEvent event, Location loc) {
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc).delta(EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS).count(10).spawnAsPlayer(player.getPlayer(), ParticleCategory.OWN_ACTIVE, ParticleCategory.OTHER_ACTIVE);
		loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2, 10);
		for (LivingEntity entity : EntityUtils.getNearbyMobs(loc, EXPLOSION_RADIUS)) {
			DamageUtils.damage(player.getPlayer(), entity, DamageEvent.DamageType.OTHER, event.getDamage() * DAMAGE);
		}
	}
}
