package com.playmonumenta.plugins.bosses.spells.headlesshorseman;

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

import com.playmonumenta.plugins.bosses.bosses.HeadlessHorsemanBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PPGroundCircle;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;



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
	private HeadlessHorsemanBoss mHorseman;
	private int mCooldownTicks;

	public SpellBurningVengence(Plugin plugin, LivingEntity entity, int cooldown, HeadlessHorsemanBoss horseman) {
		mPlugin = plugin;
		mBoss = entity;
		mHorseman = horseman;
		mCooldownTicks = cooldown;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		Horse horse = null;
		if (mBoss.getVehicle() != null) {
			if (mBoss.getVehicle() instanceof Horse) {
				horse = (Horse) mBoss.getVehicle();
				horse.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 65, 2));
			}
		}

		if (mBoss.getVehicle() != null) {
			if (mBoss.getVehicle() instanceof LivingEntity) {
				LivingEntity h = (LivingEntity) mBoss.getVehicle();

				PPGroundCircle outerFlameCircle = new PPGroundCircle(Particle.FLAME, h.getLocation(), 48, 0.07, 0.07, 0.07, 0.01).init(0, true);

				new BukkitRunnable() {
					double mRadius = 16;
					int mTicks = 0;

					@Override
					public void run() {

						if (mTicks % 2 == 0) {
							world.playSound(mBoss.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 3, 0.5f + mTicks / 32f);
						}
						mTicks++;
						Location loc = h.getLocation();

						outerFlameCircle.radius(mRadius).location(loc).spawnAsBoss();

						for (double i = 0; i < 360; i += 7.5) {
							double radian1 = Math.toRadians(i);
							loc.add(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);

							for (Player player : PlayerUtils.playersInRange(loc, 0.75, true)) {
								if (mHorseman.getSpawnLocation().distance(player.getLocation()) < HeadlessHorsemanBoss.detectionRange) {
									BossUtils.bossDamage(mBoss, player, 4, mBoss.getLocation(), "Burning Vengence");
									player.setFireTicks(20 * 5);
									MovementUtils.pullTowardsByUnit(mBoss, player, (float)0.5);
								}
							}
							loc.subtract(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
						}

						mRadius -= 0.25;

						if (mRadius <= 0) {
							this.cancel();
							h.removePotionEffect(PotionEffectType.SLOW);
							world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3, 1.25f);
							world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3, 0.85f);
							new PartialParticle(Particle.FLAME, loc, 100, 0, 0, 0, 0.125).spawnAsBoss();
							new PartialParticle(Particle.SMOKE_LARGE, loc, 25, 0, 0, 0, 0.1).spawnAsBoss();
							new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 15, 0, 0, 0, 0.125).spawnAsBoss();
							for (Player player : PlayerUtils.playersInRange(loc, 5, true)) {
								if (mHorseman.getSpawnLocation().distance(player.getLocation()) < HeadlessHorsemanBoss.detectionRange) {
									BossUtils.bossDamagePercent(mBoss, player, 0.5, "Burning Vengence");
									MovementUtils.knockAway(loc, player, 3.0f);
								}
							}
						}
					}

				}.runTaskTimer(mPlugin, 0, 1);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}
}
