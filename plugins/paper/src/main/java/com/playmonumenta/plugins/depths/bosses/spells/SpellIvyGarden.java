package com.playmonumenta.plugins.depths.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAoE.ChargeAuraAction;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAoE.ChargeCircleAction;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAoE.CircleOutburstAction;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAoE.DealDamageAction;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAoE.OutburstAction;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

public class SpellIvyGarden extends Spell {

	public static final int DAMAGE = 40;
	public static final int RADIUS = 5;
	public static final int DURATION = 80;

	public Plugin mPlugin;
	public int mRadius;
	public int mTime;
	public int mCooldownTicks;
	public Map<Location, LivingEntity> mPlants;

	public SpellIvyGarden(Plugin plugin, int cooldown, Map<Location, LivingEntity> plants) {
		mPlugin = plugin;
		mRadius = RADIUS;
		mTime = DURATION;
		mCooldownTicks = cooldown;
		mPlants = plants;
	}

	@Override
	public boolean canRun() {
		return mPlants.values().size() > 0;
	}

	@Override
	public void run() {

		for (LivingEntity le : mPlants.values()) {
			if (le != null && !le.isDead()) {
				runForce(mPlugin, le, mRadius, mTime, mCooldownTicks, false, false, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 1,
						(Location loc) -> {
							World world = loc.getWorld();
							world.spawnParticle(Particle.SMOKE_LARGE, loc, 1, ((double) mRadius) / 2, ((double) mRadius) / 2, ((double) mRadius) / 2, 0.05);
						},
						(Location loc) -> {
							World world = loc.getWorld();
							world.spawnParticle(Particle.CRIT_MAGIC, loc, 1, 0.25, 0.25, 0.25, 0.1);
						},
						(Location loc) -> {
							World world = loc.getWorld();
							world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.65f);
							world.playSound(loc, Sound.ENTITY_GHAST_SHOOT, 1f, 0.5f);
							world.playSound(loc, Sound.ENTITY_GUARDIAN_HURT, 1f, 0.8f);
							world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 0.5, 0), 100, 0.5, 0, 0.5, 0.8f);
						},
						(Location loc) -> {
							World world = loc.getWorld();
							world.spawnParticle(Particle.SMOKE_LARGE, loc, 1, 0.1, 0.1, 0.1, 0.3);
							world.spawnParticle(Particle.SMOKE_NORMAL, loc, 2, 0.25, 0.25, 0.25, 0.1);
						},
						(Location loc) -> {
							for (Player player : PlayerUtils.playersInRange(le.getLocation(), mRadius, true)) {

								double distance = player.getLocation().distance(loc);
								if (distance < mRadius / 3.0) {
									player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 2));
									MovementUtils.knockAway(le, player, 3.0f, false);
								} else if (distance < (mRadius * 2.0) / 3.0) {
									player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 1));
									MovementUtils.knockAway(le, player, 2.1f, false);
								} else if (distance < mRadius) {
									player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 0));
									MovementUtils.knockAway(le, player, 1.2f, false);
								}
								BossUtils.blockableDamage(le, player, DamageType.MAGIC, DAMAGE, "Ivy Garden", le.getLocation());

								player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, player.getLocation().clone().add(0, 1, 0), 4, 0.25, 0.5, 0.25, 0);
							}
						});
				//Resistance
				le.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 2));
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}


	public void runForce(Plugin plugin, Entity launcher, int radius, int duration, int cooldown, boolean canMoveWhileCasting, boolean needLineOfSight,
			Sound chargeSound, float soundVolume, int soundDensity, ChargeAuraAction chargeAuraAction, ChargeCircleAction chargeCircleAction,
			OutburstAction outburstAction, CircleOutburstAction circleOutburstAction, DealDamageAction dealDamageAction) {
		if (needLineOfSight) {
			// Don't cast if no player in sight, e.g. should not initiate cast through a wall
			boolean hasLineOfSight = false;
			for (Player player : PlayerUtils.playersInRange(launcher.getLocation(), mRadius * 4, true)) {
				if (LocationUtils.hasLineOfSight(launcher, player)) {
					hasLineOfSight = true;
					break;
				}
			}
			if (!hasLineOfSight) {
				return;
			}
		}

		if (!canMoveWhileCasting) {
			((LivingEntity) launcher).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 20));
		}

		new BukkitRunnable() {
			float mTicks = 0;
			double mCurrentRadius = mRadius;
			World mWorld = launcher.getWorld();

			@Override
			public void run() {
				Location loc = launcher.getLocation();

				if (launcher.isDead() || !launcher.isValid() || EntityUtils.isStunned(launcher) || EntityUtils.isSilenced(launcher)) {
					if (launcher instanceof LivingEntity) {
						((LivingEntity) launcher).setAI(true);
					}
					this.cancel();
					return;
				}
				mTicks++;
				chargeAuraAction.run(loc.clone().add(0, 1, 0));
				if (mTicks <= (duration - 5) && mTicks % soundDensity == 0) {
					mWorld.playSound(launcher.getLocation(), chargeSound, SoundCategory.HOSTILE, soundVolume, 0.25f + (mTicks / 100));
				}
				for (double i = 0; i < 360; i += 30) {
					double radian1 = Math.toRadians(i);
					loc.add(FastUtils.cos(radian1) * mCurrentRadius, 0, FastUtils.sin(radian1) * mCurrentRadius);
					chargeCircleAction.run(loc);
					loc.subtract(FastUtils.cos(radian1) * mCurrentRadius, 0, FastUtils.sin(radian1) * mCurrentRadius);
				}
				mCurrentRadius -= (mRadius / ((double) duration));
				if (mCurrentRadius <= 0) {
					this.cancel();
					dealDamageAction.run(loc);
					outburstAction.run(loc);

					new BukkitRunnable() {
						Location mLoc = launcher.getLocation();
						double mBurstRadius = 0;
						@Override
						public void run() {
							for (int j = 0; j < 2; j++) {
								mBurstRadius += 1.5;
								for (double i = 0; i < 360; i += 15) {
									double radian1 = Math.toRadians(i);
									mLoc.add(FastUtils.cos(radian1) * mBurstRadius, 0, FastUtils.sin(radian1) * mBurstRadius);
									circleOutburstAction.run(mLoc);
									mLoc.subtract(FastUtils.cos(radian1) * mBurstRadius, 0, FastUtils.sin(radian1) * mBurstRadius);
								}
							}
							if (mBurstRadius >= mRadius) {
								this.cancel();
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}
}
