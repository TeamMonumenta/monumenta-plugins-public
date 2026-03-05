package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.SporousAmalgam;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SporeShower extends Spell {
	private static final int COOLDOWN = 20 * 10;
	private static final int EXPLOSION_TELEGRAPH_DURATION = 20 * 2;
	private static final int EXPLOSION_LINGERING_DURATION = 20 * 5;
	private static final double EXPLOSION_RADIUS = 4;
	private static final double TELEGRAPH_RADIUS_SHIFT = (EXPLOSION_RADIUS - 0.1) / EXPLOSION_TELEGRAPH_DURATION;

	private static final float PITCH_PER_TICK = 0.8f / EXPLOSION_LINGERING_DURATION;

	private static final int RADIAL_EXPLOSIONS = 6;
	private static final int RANDOM_EXPLOSIONS_AMOUNT = 4;

	private static final double DAMAGE_AMOUNT = 90;
	private static final float EXPLOSION_SPORE_AMOUNT = 2.5f;
	private static final float LINGERING_SPORE_AMOUNT = 1f;

	private static final Particle.DustTransition TELEGRAPH_DATA = new Particle.DustTransition(Color.fromRGB(196, 21, 212), Color.fromRGB(101, 56, 115), 1.25f);
	private static final Particle.DustTransition LINGERING_DATA = new Particle.DustTransition(Color.fromRGB(0, 0, 255), Color.fromRGB(0, 71, 171), 1.25f);

	private final PPCircle mOuterCircle;
	private final PPCircle mInnerCircle;
	private final HashSet<Player> mSporedByExplosion;

	private final Plugin mPlugin;
	private final SporousAmalgam mSporeBeast;
	private final LivingEntity mBoss;

	public SporeShower(Plugin plugin, SporousAmalgam sporeBeast) {
		mPlugin = plugin;
		mSporeBeast = sporeBeast;
		mBoss = sporeBeast.mBoss;
		mSporedByExplosion = new HashSet<>();
		mOuterCircle = new PPCircle(Particle.DUST_COLOR_TRANSITION, mBoss.getLocation(), EXPLOSION_RADIUS).data(TELEGRAPH_DATA).count(12);

		mInnerCircle = new PPCircle(Particle.REDSTONE, mBoss.getLocation(), EXPLOSION_RADIUS)
			.data(new Particle.DustOptions(Color.fromRGB(184, 22, 114), 1f)).countPerMeter(0.40).delta(0.2).ringMode(false);
	}


	@Override
	public void run() {
		mOuterCircle.data(TELEGRAPH_DATA);
		 new BukkitRunnable() {
			int mTicks = 0;
			final Set<Player> mPlayersHitBySpore = new HashSet<>();
			final List<Location> mExplosionHitboxLocations = generateHitboxLocations();
			@Nullable List<Hitbox.UprightCylinderHitbox> mExplosionHitboxes;

			@Override
			public void run() {

				if (mTicks >= 0 && mTicks < EXPLOSION_TELEGRAPH_DURATION) {
					mInnerCircle.radius(mTicks * TELEGRAPH_RADIUS_SHIFT);
					for (Location loc : mExplosionHitboxLocations) {
						mBoss.getWorld().playSound(loc, Sound.BLOCK_SPORE_BLOSSOM_PLACE, 1f, 1f - PITCH_PER_TICK * mTicks);
						mOuterCircle.location(loc).spawnAsBoss();
						mInnerCircle.location(loc).spawnAsBoss();
					}
				} else if (mTicks == EXPLOSION_TELEGRAPH_DURATION) {
					mInnerCircle.ringMode(false);
					for (Location loc : mExplosionHitboxLocations) {
						new PartialParticle(Particle.EXPLOSION_HUGE, loc.clone().add(0, 0.1, 0), 1).spawnAsBoss();
						mBoss.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.9f);
					}
					mExplosionHitboxes = dealDamage(mExplosionHitboxLocations);
				} else if (mTicks > EXPLOSION_TELEGRAPH_DURATION && mTicks < EXPLOSION_TELEGRAPH_DURATION + EXPLOSION_LINGERING_DURATION) {
					for (Location loc : mExplosionHitboxLocations) {
						mOuterCircle.location(loc).spawnAsBoss();
						mInnerCircle.location(loc).spawnAsBoss();
					}

					if (mExplosionHitboxes != null) {
						for (Hitbox.UprightCylinderHitbox h : mExplosionHitboxes) {
							for (Player p : h.getHitPlayers(true)) {
								EffectManager.getInstance().addEffect(p, "Spore Shower - 1", new PercentSpeed(20 * 5, -0.3, "Spore slow"));
								EffectManager.getInstance().addEffect(p, "Spore Shower - 2", new PercentDamageDealt(20 * 5, -0.3));
								if (mPlayersHitBySpore.add(p)) {
									mSporeBeast.addSpores(p, LINGERING_SPORE_AMOUNT);
								}
							}
						}
					}
				}

				if (mBoss.isDead() || mTicks == EXPLOSION_TELEGRAPH_DURATION + EXPLOSION_LINGERING_DURATION) {
					this.cancel();
				}
				mTicks += 1;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private List<Hitbox.UprightCylinderHitbox> dealDamage(List<Location> mExplosionHitboxLocations) {
		List<Hitbox.UprightCylinderHitbox> hitboxes = new ArrayList<>();
		for (Location l : mExplosionHitboxLocations) {
			Hitbox.UprightCylinderHitbox hitbox = new Hitbox.UprightCylinderHitbox(l, 0.5, EXPLOSION_RADIUS);
			for (Player p : new Hitbox.UprightCylinderHitbox(l, 10, EXPLOSION_RADIUS).getHitPlayers(true)) {
				double damage = mSporeBeast.isLastPhase() ? DAMAGE_AMOUNT * 0.66 : DAMAGE_AMOUNT;
				DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, damage, null, false, true, "Spore Shower");
				if (mSporedByExplosion.add(p)) {
					mSporeBeast.addSpores(p, EXPLOSION_SPORE_AMOUNT);
				}
			}
			hitboxes.add(hitbox);
		}
		mSporedByExplosion.clear();
		mOuterCircle.data(LINGERING_DATA);
		return hitboxes;
	}

	private List<Location> generateHitboxLocations() {
		List<Location> hitboxLocations = new ArrayList<>();
		Location location;
		double angle = 0;
		double anglePerExplosion = 360.0 / RADIAL_EXPLOSIONS;
		for (int i = 0; i < RADIAL_EXPLOSIONS; i++) {
			angle += anglePerExplosion;
			location = mSporeBeast.getRandomLocationGivenAngle(EXPLOSION_RADIUS * 2, EXPLOSION_RADIUS - 1, 0, angle);
			hitboxLocations.add(location);
		}
		for (int i = 0; i < RANDOM_EXPLOSIONS_AMOUNT; i++) {
			location = mSporeBeast.getRandomLocationInArena(EXPLOSION_RADIUS, EXPLOSION_RADIUS - 1, 0);
			hitboxLocations.add(location);
		}
		return hitboxLocations;
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	@Override
	public boolean canRun() {
		if (!mSporeBeast.canRunUproot()) {
			return mSporeBeast.canRunSpell(this);
		}
		return false;
	}
}
