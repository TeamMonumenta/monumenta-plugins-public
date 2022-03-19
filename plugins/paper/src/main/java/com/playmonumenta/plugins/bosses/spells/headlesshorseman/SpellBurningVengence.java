package com.playmonumenta.plugins.bosses.spells.headlesshorseman;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/*
 * Burning Vengeance - The horseman after 0.5 seconds summons a ring of flames at the edge of a 16 block
radius circle that travels inwards towards the boss, this ring travels over 1 block tall blocks.
Players hit by the ring take 8/15 damage, ignited for 5 seconds, and knocked towards the boss. Players
can be hit multiple times. After the ring reaches the horseman the fire erupts, dealing 20/30 damage
in a 5 block radius and knocking them away from the boss.
 */
public class SpellBurningVengence extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mCenter;
	private int mRange;
	private int mCooldownTicks;
	private double mDamage;

	public SpellBurningVengence(Plugin plugin, LivingEntity entity, int cooldown, Location center, int range, double damage) {
		mPlugin = plugin;
		mBoss = entity;
		mCenter = center;
		mRange = range;
		mCooldownTicks = cooldown;
		mDamage = damage;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		if (mBoss.getVehicle() != null) {
			if (mBoss.getVehicle() instanceof Horse horse) {
				horse.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 65, 2));
			}
		}

		if (mBoss.getVehicle() != null) {
			if (mBoss.getVehicle() instanceof LivingEntity horse) {
				PPCircle outerFlameCircle = new PPCircle(Particle.FLAME, horse.getLocation(), 0).ringMode(true).count(48).delta(0.07).extra(0.01);

				BukkitRunnable run = new BukkitRunnable() {
					double mRadius = 16;
					int mTicks = 0;

					@Override
					public void run() {

						if (mTicks % 2 == 0) {
							world.playSound(mBoss.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 3, 0.5f + mTicks / 32f);
						}
						mTicks++;
						Location loc = horse.getLocation();

						outerFlameCircle.radius(mRadius).location(loc).spawnAsBoss();

						for (double i = 0; i < 360; i += 7.5) {
							double radian1 = Math.toRadians(i);
							loc.add(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);

							for (Player player : PlayerUtils.playersInRange(loc, 0.75, true)) {
								if (mCenter.distance(player.getLocation()) < mRange) {
									DamageUtils.damage(mBoss, player, DamageType.FIRE, 4, null, false, true, "Burning Vengence");
									player.setFireTicks(20 * 5);
									MovementUtils.pullTowardsByUnit(mBoss, player, (float)0.5);
								}
							}
							loc.subtract(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
						}

						mRadius -= 0.25;

						if (mRadius <= 0) {
							this.cancel();
							horse.removePotionEffect(PotionEffectType.SLOW);
							world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3, 1.25f);
							world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3, 0.85f);
							new PartialParticle(Particle.FLAME, loc, 100, 0, 0, 0, 0.125).spawnAsBoss();
							new PartialParticle(Particle.SMOKE_LARGE, loc, 25, 0, 0, 0, 0.1).spawnAsBoss();
							new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 15, 0, 0, 0, 0.125).spawnAsBoss();
							for (Player player : PlayerUtils.playersInRange(loc, 5, true)) {
								if (mCenter.distance(player.getLocation()) < mRange) {
									BossUtils.bossDamagePercent(mBoss, player, mDamage, "Burning Vengence");
									MovementUtils.knockAway(loc, player, 3.0f, false);
								}
							}
						}
					}

				};
				run.runTaskTimer(mPlugin, 0, 1);
				mActiveRunnables.add(run);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}
}
