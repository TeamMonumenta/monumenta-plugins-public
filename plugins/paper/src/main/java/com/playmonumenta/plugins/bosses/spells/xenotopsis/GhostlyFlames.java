package com.playmonumenta.plugins.bosses.spells.xenotopsis;

import com.playmonumenta.plugins.bosses.bosses.Xenotopsis;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class GhostlyFlames extends Spell {
	// the duration of the entire attack, in ticks
	private static final int DURATION = 20 * 8;

	// the base cooldown of the attack, in ticks
	private static final int BASE_COOLDOWN = 20;

	// the duration of the windup portion of the attack, in ticks
	private static final int WINDUP_DURATION = 20 * 2;

	// the attack and death damage of the attack
	private static final int ATTACK_DAMAGE = 80;
	private static final int DEATH_DAMAGE = 15;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final Xenotopsis mXenotopsis;
	private final int mCooldownTicks;

	private final double mFlameRadius;

	public GhostlyFlames(Plugin plugin, LivingEntity boss, Xenotopsis xenotopsis, int cooldownTicks, double flameRadius) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mCooldownTicks = cooldownTicks;
		mXenotopsis = xenotopsis;

		mFlameRadius = flameRadius;
	}

	@Override
	public boolean canRun() {
		return mXenotopsis.canRunSpell(this);
	}

	@Override
	public void run() {
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			final Location mLocation = mBoss.getLocation().clone();
			final Map<Player, Integer> mLastHit = new HashMap<>();

			@Override
			public void run() {
				// change the speed of the boss based on the point in the attack and play sounds
				if (mTicks == 0) {
					EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, 0.0);

					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.HOSTILE, 4f, 0.5f);
				}
				if (mTicks == WINDUP_DURATION) {
					EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, mXenotopsis.getMovementSpeed());

					mWorld.playSound(mBoss.getLocation(), Sound.ITEM_FIRECHARGE_USE, SoundCategory.HOSTILE, 2.4f, 0.8f);
				}

				if (mTicks < WINDUP_DURATION && mTicks % 4 == 0) {
					new PPParametric(Particle.ASH, mBoss.getLocation(), (parameter, builder) -> {
						double r = FastUtils.randomDoubleInRange(0, Math.PI * 2);
						builder.offset(FastUtils.cos(r), 0, FastUtils.sin(r));
					})
						.directionalMode(true)
						.count(20)
						.extra(0.5)
						.spawnAsBoss();

					new PPCircle(Particle.REDSTONE, mLocation, mFlameRadius)
						.ringMode(true)
						.data(new Particle.DustOptions(Color.fromRGB(19, 19, 28), 1.2f))
						.delta(0.3 * ((double) mTicks / WINDUP_DURATION))
						.count((int) ((12 * mFlameRadius) * ((double) mTicks / WINDUP_DURATION)))
						.spawnAsBoss();
					new PPCircle(Particle.REDSTONE, mLocation, mFlameRadius)
						.ringMode(true)
						.data(new Particle.DustOptions(Color.fromRGB(220, 222, 228), 1.2f))
						.delta(0.3 * ((double) mTicks / WINDUP_DURATION))
						.count((int) ((12 * mFlameRadius) * ((double) mTicks / WINDUP_DURATION)))
						.spawnAsBoss();
				}

				if (mTicks > WINDUP_DURATION) {
					// particle effect
					if (mTicks % 4 == 0) {
						new PPCircle(Particle.SPELL_MOB, mLocation, mFlameRadius)
							.ringMode(true)
							.delta(0.3)
							.count((int) (10 * mFlameRadius))
							.spawnAsBoss();
						new PPCircle(Particle.SPELL, mLocation, mFlameRadius)
							.ringMode(true)
							.delta(0.3)
							.count((int) (2 * mFlameRadius))
							.spawnAsBoss();
						new PPCircle(Particle.REDSTONE, mLocation, mFlameRadius)
							.ringMode(true)
							.data(new Particle.DustOptions(Color.fromRGB(19, 19, 28), 2.1f))
							.delta(0.3)
							.count((int) (5 * mFlameRadius))
							.spawnAsBoss();
						new PPCircle(Particle.REDSTONE, mLocation, mFlameRadius)
							.ringMode(true)
							.data(new Particle.DustOptions(Color.fromRGB(220, 222, 228), 2.1f))
							.delta(0.3)
							.count((int) (4 * mFlameRadius))
							.spawnAsBoss();
						new PPCircle(Particle.SMOKE_LARGE, mLocation, mFlameRadius)
							.ringMode(true)
							.delta(0.3)
							.count((int) (11 * mFlameRadius))
							.spawnAsBoss();
						new PPParametric(Particle.FIREWORKS_SPARK, mLocation, (parameter, builder) -> {
							double theta = (parameter + FastUtils.randomDoubleInRange(0, 1.0 / 6)) * Math.PI * 2;
							Vector diff = new Vector(FastUtils.cos(theta) * mFlameRadius, 0.3, FastUtils.sin(theta) * mFlameRadius);

							builder.location(mLocation.clone().add(diff));
							builder.offset(0, FastUtils.randomDoubleInRange(0.15, 0.25), 0);
						})
							.directionalMode(true)
							.extra(1)
							.count(6)
							.spawnAsBoss();
					}

					// sound effect
					if (mTicks % 12 == 0) {
						mWorld.playSound(mLocation, Sound.BLOCK_FIRE_AMBIENT, SoundCategory.HOSTILE, 5f, 0.76f);
					}
					if (mTicks % 6 == 0) {
						mWorld.playSound(mLocation, Sound.PARTICLE_SOUL_ESCAPE, SoundCategory.HOSTILE, 5f, 1.32f);
					}

					Hitbox hitbox = Hitbox.approximateHollowCylinderSegment(mLocation, 2, mFlameRadius - 0.3, mFlameRadius + 0.3, Math.PI);
					for (Player player : hitbox.getHitPlayers(true)) {
						if (mLastHit.containsKey(player) && mTicks - mLastHit.get(player) < 10) {
							continue;
						}
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, mXenotopsis.scaleDamage(ATTACK_DAMAGE), null, false, true, "Ghostly Flames");
						mXenotopsis.changePlayerDeathValue(player, DEATH_DAMAGE, false);
						//MovementUtils.knockAwayRealistic(mLocation, player, 0.3f, 0.35f, true);

						if (mLastHit.containsKey(player)) {
							mLastHit.replace(player, mTicks);
						} else {
							mLastHit.put(player, mTicks);
						}

						mWorld.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.HOSTILE, 2.8f, 0.85f);
						mWorld.playSound(player.getLocation(), Sound.ENTITY_STRAY_AMBIENT, SoundCategory.HOSTILE, 4f, 0.65f);
					}
				}

				mTicks++;
				if (mTicks > DURATION || mBoss.isDead()) {
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return BASE_COOLDOWN + mCooldownTicks;
	}
}
