package com.playmonumenta.plugins.bosses.spells.xenotopsis;

import com.playmonumenta.plugins.bosses.bosses.Xenotopsis;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DeathlyBombs extends Spell {
	// the base cooldown of the attack, which is modified by the cooldown ticks provided by the boss
	private static final int BASE_COOLDOWN = 20;

	// the total amount of bomb groups to be shot
	private static final int BOMB_AMOUNT = 3;

	// the longest time a bomb can exist before exploding, in ticks
	private static final int BOMB_MAX_DURATION = 20 * 8;

	// the delay between each bomb being shot, in ticks
	private static final int DELAY_BETWEEN_BOMBS = 25;

	// the radius of the attack of each bomb on impact
	private static final double BOMB_RADIUS = 3.75;

	// the attack and death damage of each bomb on impact
	private static final int BOMB_ATTACK_DAMAGE = 130;
	private static final int BOMB_DEATH_DAMAGE = 12;

	// the attack and death damage of each shrapnel
	private static final int SHRAPNEL_ATTACK_DAMAGE = 65;
	private static final int SHRAPNEL_DEATH_DAMAGE = 6;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final Xenotopsis mXenotopsis;
	private final int mCooldownTicks;

	private final int mBombTargets;
	private final int mShrapnelShots;

	public DeathlyBombs(Plugin plugin, LivingEntity boss, Xenotopsis xenotopsis, int cooldownTicks, int bombTargets, int shrapnelShots) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mCooldownTicks = cooldownTicks;
		mXenotopsis = xenotopsis;

		mBombTargets = bombTargets;
		mShrapnelShots = shrapnelShots;
	}

	@Override
	public boolean canRun() {
		return mXenotopsis.canRunSpell(this);
	}

	@Override
	public void run() {
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks % DELAY_BETWEEN_BOMBS == 0) {
					List<Location> locations = new ArrayList<>();
					for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), Xenotopsis.DETECTION_RANGE, true)) {
						locations.add(player.getLocation());
					}
					locations.sort((o1, o2) -> {
						double distance = o1.distance(o2);
						return distance > 0 ? 1 : distance < 0 ? -1 : 0;
					});
					for (int i = 0; i < Math.min(mBombTargets, locations.size()); i++) {
						launchBomb(locations.get(i));
					}

					// play sound
					mWorld.playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 2.2f, 0.85f);
				}

				mTicks++;
				if (mTicks > DELAY_BETWEEN_BOMBS * (BOMB_AMOUNT - 1) || mBoss.isDead()) {
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	// largely taken from SpellBaseGrenadeLauncher
	private void launchBomb(Location destination) {
		FallingBlock fallingBlock = mWorld.spawnFallingBlock(mBoss.getEyeLocation().add(0, 1, 0), Material.POLISHED_DIORITE.createBlockData());
		fallingBlock.setDropItem(false);
		EntityUtils.disableBlockPlacement(fallingBlock);
		Location tLoc = fallingBlock.getLocation();

		// h = 0.5 * g * t^2
		// t^2 = 0.5 * g / h
		// t = sqrt(0.5 * g / h)
		// h = 5.8 blocks with a 0.7 y velocity component
		double timeOfFlight = Math.sqrt(0.5 * 16 / 5.8);
		Location endPoint = destination.clone();
		endPoint.setY(tLoc.getY());
		double distance = endPoint.distance(tLoc);
		double velocity = distance * timeOfFlight;

		// Divide the actual velocity by 32 (speed at which things fall in minecraft; don't ask me why, but it works)
		Vector vel = new Vector(destination.getX() - tLoc.getX(), 0, destination.getZ() - tLoc.getZ()).normalize().multiply(velocity / 32);
		vel.setY(0.7f);

		if (!Double.isFinite(vel.getX())) {
			vel = new Vector(0, 1, 0);
		}
		fallingBlock.setVelocity(vel);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (fallingBlock.isOnGround() || mTicks == BOMB_MAX_DURATION) {
					Location finalLocation = fallingBlock.getLocation().clone();

					// visual & sound effects for landing
					mWorld.strikeLightningEffect(finalLocation);
					new PPExplosion(Particle.CLOUD, finalLocation)
						.count(80)
						.extra(0.5)
						.speed(0.5)
						.spawnAsBoss();
					new PPCircle(Particle.REDSTONE, finalLocation, BOMB_RADIUS)
						.ringMode(true)
						.count(90)
						.data(new Particle.DustOptions(Color.fromRGB(224, 222, 227), 1.4f))
						.delta(0.08)
						.spawnAsBoss();

					mWorld.playSound(finalLocation, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.3f, 0.93f);

					Hitbox hitbox = new Hitbox.UprightCylinderHitbox(finalLocation, 3, BOMB_RADIUS);
					for (Player player : hitbox.getHitPlayers(true)) {
						BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.PROJECTILE, mXenotopsis.scaleDamage(BOMB_ATTACK_DAMAGE), "Deathly Bombs", finalLocation, Xenotopsis.SHIELD_STUN_TIME * 2);
						mXenotopsis.changePlayerDeathValue(player, BOMB_DEATH_DAMAGE, false);
						//MovementUtils.knockAwayRealistic(finalLocation, player, 0.3f, 0.35f, true);
					}

					// summon shrapnel
					for (int i = 0; i < mShrapnelShots; i++) {
						Location shrapnelLocation = finalLocation.clone();
						shrapnelLocation.setY(Math.floor(finalLocation.getY()));
						double r = FastUtils.randomDoubleInRange(((double) i / mShrapnelShots) * Math.PI * 2, ((double) (i + 1) / mShrapnelShots) * Math.PI * 2);
						Vector direction = new Vector(FastUtils.cos(r), 0, FastUtils.sin(r));

						launchShrapnel(shrapnelLocation, direction, 9, -6);
					}

					fallingBlock.remove();
					this.cancel();
					return;
				}

				new PartialParticle(Particle.REDSTONE, fallingBlock.getLocation())
					.count(5)
					.extra(0.2)
					.data(new Particle.DustOptions(Color.fromRGB(224, 222, 227), 1.7f))
					.delta(0.02, 0.02, 0.02)
					.spawnAsBoss();

				mTicks++;
				if (mTicks > BOMB_MAX_DURATION || mBoss.isDead()) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				fallingBlock.remove();
				super.cancel();
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private void launchShrapnel(Location location, Vector direction, double speed, double acceleration) {
		BlockDisplay bullet = mWorld.spawn(location.clone().add(-0.3125, -0.3125, -0.3125), BlockDisplay.class);
		bullet.setBlock(Material.POLISHED_DIORITE.createBlockData());
		bullet.setBrightness(new Display.Brightness(15, 15));
		bullet.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.625f, 0.625f, 0.625f), new Quaternionf()));
		bullet.setInterpolationDuration(2);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			Location mAccurateLocation = bullet.getLocation();
			final Vector mTotalChange = new Vector(0, 0, 0);

			@Override
			public void run() {
				// move the bullet
				double speedThisTick = (speed + (acceleration / 20) * mTicks) / 20;
				Vector change = direction.clone().multiply(speedThisTick);
				bullet.setTransformation(new Transformation(new Vector3f((float)(mTotalChange.getX() + change.getX() * 2), (float)(mTotalChange.getY() + change.getY() * 2), (float)(mTotalChange.getZ() + change.getZ() * 2)), bullet.getTransformation().getLeftRotation(), bullet.getTransformation().getScale(), bullet.getTransformation().getRightRotation()));
				bullet.setInterpolationDelay(-1);

				mTotalChange.add(change);
				mAccurateLocation = bullet.getLocation().clone().add(mTotalChange);

				// check collisions
				Hitbox hitbox = new Hitbox.AABBHitbox(mWorld, BoundingBox.of(mAccurateLocation.clone().add(0, 0.3125, 0), 0.3125, 0.3125, 0.3125));
				for (Player player : hitbox.getHitPlayers(true)) {
					DamageUtils.damage(mBoss, player, DamageEvent.DamageType.PROJECTILE, mXenotopsis.scaleDamage(SHRAPNEL_ATTACK_DAMAGE), null, false, true, "Deathly Shrapnel");
					mXenotopsis.changePlayerDeathValue(player, SHRAPNEL_DEATH_DAMAGE, false);
					//MovementUtils.knockAwayRealistic(bullet.getLocation(), player, 0.3f, 0.35f, true);

					// particle effect
					new PPExplosion(Particle.SMOKE_NORMAL, mAccurateLocation.clone().add(0, 0.45, 0))
						.count(20)
						.extra(0.5)
						.speed(0.35)
						.spawnAsBoss();

					bullet.remove();
					this.cancel();
					return;
				}

				mTicks++;
				if (mTicks > 100 || speedThisTick < 0 || mBoss.isDead()) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				bullet.remove();
				super.cancel();
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
