package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class AmethystShards extends Spell {

	// The block that the bullets are shown as
	private static final Material BULLET_MATERIAL = Material.AMETHYST_BLOCK;

	// Lifetime of the windup portion of the spell in ticks
	private static final int WINDUP_DURATION = 2 * 20;

	// The hitbox diameter of each bullet
	private static final double HITBOX_SIZE = 0.625;

	// The number of streams of bullets that are shot out from the center, equally spaced
	private static final int BULLET_STREAMS = 3;

	// The number of rotations per second each stream makes
	private static final double ROTATION_SPEED = 0.35;

	// The delay in ticks between each stream shooting a bullet
	private static final int BULLET_DELAY = 3;

	// The velocity of each bullet in blocks per second
	private static final double BULLET_SPEED = 15;

	// The constant acceleration of each bullet in blocks per second
	private static final double BULLET_ACCELERATION = -10;

	// The lifetime of the bullet in ticks
	private static final int BULLET_LIFETIME = (int) (1.5 * 20);

	// The lifetime of the main part of the spell in ticks
	private static final int MAIN_DURATION = 5 * 20;

	// Attack damage
	private static final double ATTACK_DAMAGE = 70;


	public final LivingEntity mBoss;
	public final Plugin mPlugin;
	public final World mWorld;

	public String mAttackName;

	public AmethystShards(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();

		mAttackName = "Amethyst Shards";
	}

	@Override
	public void run() {
		final double TRIANGLE_SIZE = 3.0;

		final double PI_2_3 = Math.PI * 2 / 3;
		final double PI_4_3 = Math.PI * 4 / 3;
		final double SQRT_3 = Math.sqrt(3);
		final double SIDE_LENGTH = SQRT_3 * 2;

		double offsetAngle = FastUtils.randomDoubleInRange(0, 360);

		// Triangle telegraph
		new PPParametric(Particle.WAX_OFF, mBoss.getLocation().clone().add(0, 0.2, 0),
			(param, builder) -> {
				if (param <= 1.0 / 3) {
					double p = param * 3;
					Vector d = new Vector(1, 0, 0);
					Vector s = new Vector(-SQRT_3, 0, -1);

					Vector pos = s.add(d.multiply(SIDE_LENGTH * p)).multiply(TRIANGLE_SIZE);

					pos.rotateAroundY(offsetAngle);

					builder.offset(-pos.getX(), 0, -pos.getZ());
					builder.location(mBoss.getLocation().clone().add(pos).add(0, 0.2, 0));
				} else if (param <= 1.0 / 3 * 2) {
					double p = (param - 1.0 / 3) * 3;
					Vector d = new Vector(FastUtils.cos(PI_2_3), 0, FastUtils.sin(PI_2_3));
					Vector s = new Vector(SQRT_3, 0, -1);

					Vector pos = s.add(d.multiply(SIDE_LENGTH * p)).multiply(TRIANGLE_SIZE);

					pos.rotateAroundY(offsetAngle);

					builder.offset(-pos.getX(), 0, -pos.getZ());
					builder.location(mBoss.getLocation().clone().add(pos).add(0, 0.2, 0));
				} else {
					double p = (param - 1.0 / 3 * 2) * 3;
					Vector d = new Vector(FastUtils.cos(PI_4_3), 0, FastUtils.sin(PI_4_3));
					Vector s = new Vector(0, 0, 2);

					Vector pos = s.add(d.multiply(SIDE_LENGTH * p)).multiply(TRIANGLE_SIZE);

					pos.rotateAroundY(offsetAngle);

					builder.offset(-pos.getX(), 0, -pos.getZ());
					builder.location(mBoss.getLocation().clone().add(pos).add(0, 0.2, 0));
				}
			})
			.count(160)
			.directionalMode(true)
			.extra(1.2)
			.spawnAsBoss();

		// Runnable for the attack
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks < WINDUP_DURATION) {
					if (mTicks % 3 == 0) {
						mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.HOSTILE, 15, 0.4f + ((float) mTicks / WINDUP_DURATION * 0.5f));
					}
				} else {
					// If bullets should be shot this tick
					if (mTicks % BULLET_DELAY == 0) {
						// Primary stream angle, used to calculate other stream's angles later
						double primaryAngle = ((Math.PI * 2 * ROTATION_SPEED) * ((double) mTicks / 20)) % (Math.PI * 2);

						mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.HOSTILE, 5, (float) (mTicks - WINDUP_DURATION) / MAIN_DURATION * 1.6f);

						for (int i = 0; i < BULLET_STREAMS; i++) {
							// The angle of this stream in radians
							double streamAngle = (primaryAngle + ((Math.PI * 2) / BULLET_STREAMS * i)) % (Math.PI * 2);

							Vector direction = new Vector(Math.cos(streamAngle), 0, Math.sin(streamAngle));

							launchAcceleratingBullet(mBoss.getLocation(), direction);
						}
					}
				}

				mTicks++;

				if (mTicks >= WINDUP_DURATION + MAIN_DURATION || mBoss.isDead()) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void launchAcceleratingBullet(Location startLocation, Vector direction) {
		BlockDisplay bullet = mWorld.spawn(startLocation.clone().add(-0.3125, -0.3125, -0.3125), BlockDisplay.class);
		bullet.setBlock(BULLET_MATERIAL.createBlockData());
		bullet.setBrightness(new Display.Brightness(15, 15));
		bullet.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.625f, 0.625f, 0.625f), new Quaternionf()));
		bullet.setInterpolationDuration(2);
		EntityUtils.setRemoveEntityOnUnload(bullet);

		new BukkitRunnable() {
			int mTicks = 0;
			Location mAccurateLocation = bullet.getLocation();
			final Vector mTotalChange = new Vector(0, 0, 0);
			int mTpCooldown = 0;

			@Override
			public void run() {
				double speedThisTick = (BULLET_SPEED + (BULLET_ACCELERATION / 20) * mTicks) / 20;
				Vector change = direction.clone().multiply(speedThisTick);
				bullet.setTransformation(new Transformation(new Vector3f((float) (mTotalChange.getX() + change.getX() * 2), (float) (mTotalChange.getY() + change.getY() * 2), (float) (mTotalChange.getZ() + change.getZ() * 2)), bullet.getTransformation().getLeftRotation(), bullet.getTransformation().getScale(), bullet.getTransformation().getRightRotation()));
				bullet.setInterpolationDelay(-1);

				mTotalChange.add(change);
				mAccurateLocation = bullet.getLocation().clone().add(mTotalChange);

				// adjust for terrain height
				if (mTicks > 5 && mTpCooldown == 0) {
					Location position = mAccurateLocation.clone().add(0, HITBOX_SIZE / 2 + 0.5, 0);
					if (position.getBlock().getType() != Material.AIR) {
						bullet.teleport(bullet.getLocation().clone().add(0, 1, 0));
						mTpCooldown = 3;
					} else if (position.add(0, -1, 0).getBlock().getType() == Material.AIR) {
						bullet.teleport(bullet.getLocation().clone().add(0, -1, 0));
						mTpCooldown = 3;
					}
				}

				// check collision
				Hitbox hitbox = new Hitbox.AABBHitbox(mWorld, BoundingBox.of(mAccurateLocation.clone().add(0, HITBOX_SIZE / 2, 0), HITBOX_SIZE / 2, HITBOX_SIZE / 2, HITBOX_SIZE / 2));
				for (Player player : hitbox.getHitPlayers(true)) {
					DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, ATTACK_DAMAGE, null, false, true, "Amethyst Shards");

					mWorld.playSound(player.getLocation(), Sound.BLOCK_BASALT_BREAK, SoundCategory.HOSTILE, 1.8f, 0);
					new PartialParticle(Particle.REVERSE_PORTAL, player.getLocation(), 15, 0, 0, 0, 0.175).spawnAsBoss();

					bullet.remove();
					this.cancel();
				}

				if (mTpCooldown > 0) {
					mTpCooldown--;
				}
				mTicks++;
				if (mTicks >= BULLET_LIFETIME) {
					bullet.remove();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return WINDUP_DURATION + MAIN_DURATION + 30;
	}
}
