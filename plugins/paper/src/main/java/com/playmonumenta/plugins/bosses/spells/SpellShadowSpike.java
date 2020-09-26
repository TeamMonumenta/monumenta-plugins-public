package com.playmonumenta.plugins.bosses.spells;

import java.util.Iterator;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class SpellShadowSpike extends Spell {

	private static final Particle.DustOptions ATTACK_COLOR = new Particle.DustOptions(Color.fromRGB(100, 0, 0), 1.0f);

	private static final int RANGE = 20;
	private static final int BLOCK_HIT_RANGE = 3;
	private static final int SLOWNESS_DURATION = 20 * 5;
	private static final int SLOWNESS_AMPLIFIER = 2;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final int mCooldown;
	private final int mDamage;

	public SpellShadowSpike(Plugin plugin, LivingEntity boss, int cooldown, int damage) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mCooldown = cooldown;
		mDamage = damage;
	}

	@Override
	public void run() {
		LivingEntity entity = null;
		if (mBoss instanceof Mob) {
			entity = ((Mob) mBoss).getTarget();
		}

		Location loc = mBoss.getLocation();
		if (!(entity instanceof Player && LocationUtils.hasLineOfSight(mBoss, entity))) {
			List<Player> players = EntityUtils.getNearestPlayers(loc, RANGE);
			for (Player player : players) {
				if (LocationUtils.hasLineOfSight(mBoss, player)) {
					entity = player;
					break;
				}
			}
		}

		if (entity != null) {
			Player target = (Player) entity;

			mWorld.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1, 0.5f);

			BukkitRunnable preparation = new BukkitRunnable() {
				int mTicks = 1;

				@Override
				public void run() {
					Location loc = mBoss.getLocation();
					for (double r = 0; r < Math.PI * 2; r += Math.PI / mTicks / 3) {
						double dx = Math.sin(r) * mTicks / 3;
						double dz = Math.cos(r) * mTicks / 3;
						loc.add(dx, 0, dz);
						mWorld.spawnParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0, ATTACK_COLOR);
						loc.subtract(dx, 0, dz);
					}

					mTicks++;
					if (mTicks > 10) {
						this.cancel();
					}
				}
			};

			preparation.runTaskTimer(mPlugin, 0, 1);
			mActiveRunnables.add(preparation);


			BukkitRunnable attack = new BukkitRunnable() {
				List<Player> mPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), RANGE * 2);
				Player mTarget = target;
				Location mTargetLocation = target.getLocation();
				int mTicks = -10;

				@Override
				public void run() {
					if (mTicks == -10) {
						Location loc = mBoss.getLocation();
						Vector offset = mTarget.getLocation().subtract(mTargetLocation).toVector().setY(0);
						mTargetLocation.add(offset.multiply(2.5));
						Vector shift = mTargetLocation.subtract(loc).toVector().normalize().multiply(RANGE);
						mTargetLocation = loc.add(shift);
					} else if (mTicks >= 0) {
						Location loc = mBoss.getLocation().add(0, 1, 0);
						BoundingBox hitbox = new BoundingBox().shift(loc).expand(0.5);
						mTargetLocation.setY(mTarget.getLocation().getY() + 1);
						Vector shift = mTargetLocation.clone().subtract(loc).toVector().multiply(1.0 / RANGE);
						for (int i = 0; i < RANGE - 2 * Math.abs(mTicks - RANGE / 2); i++) {
							loc.add(shift);
							hitbox.shift(shift);
							mWorld.spawnParticle(Particle.SQUID_INK, loc, 2, 0.3, 0.3, 0.3, 0);
							mWorld.spawnParticle(Particle.REDSTONE, loc, 1, 0.6, 0.6, 0.6, 0, ATTACK_COLOR);
							
							Iterator<Player> iter = mPlayers.iterator();
							while (iter.hasNext()) {
								Player player = iter.next();
								if (player.getBoundingBox().overlaps(hitbox)) {
									BossUtils.bossDamage(mBoss, player, mDamage);
									PotionUtils.applyPotion(mBoss, player, new PotionEffect(PotionEffectType.SLOW, SLOWNESS_DURATION, SLOWNESS_AMPLIFIER, false, true));
									iter.remove();
								}
							}
							
							if (!loc.getBlock().isPassable()) {
								mWorld.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);
								mWorld.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.5f, 1f);
								for (Player player : PlayerUtils.playersInRange(loc, BLOCK_HIT_RANGE)) {
									BossUtils.bossDamage(mBoss, player, mDamage / 2);
								}
								
								this.cancel();
								break;
							}
						}
					} else if (mTicks >= RANGE) {
						this.cancel();
					}

					mTicks++;
				}
			};

			attack.runTaskTimer(mPlugin, 10, 1);
			mActiveRunnables.add(attack);
		}
	}

	@Override
	public boolean canRun() {
		Location loc = mBoss.getLocation();
		List<Player> players = PlayerUtils.playersInRange(loc, RANGE);
		if (!players.isEmpty()) {
			for (Player player : players) {
				if (LocationUtils.hasLineOfSight(mBoss, player)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public int duration() {
		return mCooldown;
	}

}
