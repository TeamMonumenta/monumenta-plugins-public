package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.Uamiel;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class TerraBlast extends Spell {
	// the time between each time the attack activates, in ticks
	private static final int COOLDOWN = 30;

	// the minimum range from the boss for the attack to target a player
	private static final double MIN_DISTANCE = 20;

	// the duration the attack takes to wind up
	private static final int WINDUP_DURATION = 20;

	// the movement speed of the blast projectile in blocks per tick
	private static final double PROJECTILE_SPEED = 0.75;

	// the turn speed of the projectile, or speed of the projectile orthogonal to the velocity, in blocks per tick
	private static final double TURN_SPEED = 0.028;

	// the radius size of the blast projectile
	private static final double HITBOX_SIZE = 0.3;

	// the damage of the attack
	private static final int ATTACK_DAMAGE = 70;

	private int mTicks = 0;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Uamiel mUamiel;
	private final World mWorld;

	public TerraBlast(Plugin plugin, LivingEntity boss, Uamiel uamiel) {
		mPlugin = plugin;
		mBoss = boss;
		mUamiel = uamiel;
		mWorld = boss.getWorld();
	}

	@Override
	public boolean canRun() {
		return mUamiel.canRunSpell(this);
	}

	@Override
	public void run() {
		mTicks++;

		// display range
		if (mTicks % 4 == 0) {
			Location spawnFromBottom = mBoss.getLocation().clone();
			spawnFromBottom.setY(mUamiel.mCenterLocation.getY() + 0.75);
			Location spawnFromMiddle = mBoss.getLocation().clone();
			spawnFromMiddle.setY(mUamiel.mCenterLocation.getY() + 2.25);
			Location spawnFromTop = mBoss.getLocation().clone();
			spawnFromTop.setY(mUamiel.mCenterLocation.getY() + 3.75);
			new PPCircle(Particle.CRIT, spawnFromBottom, MIN_DISTANCE).count(75).spawnAsBoss();
			new PPCircle(Particle.CRIT, spawnFromMiddle, MIN_DISTANCE).count(75).spawnAsBoss();
			new PPCircle(Particle.CRIT, spawnFromTop, MIN_DISTANCE).count(75).spawnAsBoss();
		}

		if (mTicks >= COOLDOWN) {
			mTicks = 0;

			List<Player> targets = new ArrayList<>();

			Location getFrom = mBoss.getLocation().clone();
			getFrom.setY(mUamiel.mCenterLocation.getY());
			List<Player> nearbyPlayers = PlayerUtils.playersInRange(getFrom, 50, true);
			List<Player> validPlayers = new ArrayList<>();
			nearbyPlayers.forEach(player -> {
				if (mBoss.getLocation().distanceSquared(player.getLocation()) > MIN_DISTANCE * MIN_DISTANCE) {
					validPlayers.add(player);
				}
			});
			for (int i = 0; i < (int) Math.ceil((double) validPlayers.size() / 3.0); i++) {
				targets.add(validPlayers.remove(FastUtils.randomIntInRange(0, validPlayers.size() - 1)));
			}

			for (Player player : targets) {
				mWorld.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 2.3f, 0.73f);
			}

			BukkitRunnable runnable = new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					for (Player player : targets) {
						new PPCircle(Particle.CRIT, player.getLocation().clone().add(0, 1, 0), 1)
							.ringMode(true)
							.count(8)
							.spawnAsBoss();
					}

					if (mTicks == WINDUP_DURATION) {
						for (Player player : targets) {
							launchBlastAtPlayer(player);
						}
					}

					mTicks += 2;
					if (mTicks > WINDUP_DURATION || !mBoss.isValid()) {
						this.cancel();
					}
				}
			};
			runnable.runTaskTimer(mPlugin, 0, 2);
			mActiveRunnables.add(runnable);
		}
	}

	private void launchBlastAtPlayer(Player player) {
		Vector dirToPlayer = player.getLocation().clone().toVector().add(new Vector(0, 1.5, 0)).subtract(mBoss.getLocation().clone().toVector()).normalize();

		Vector crossXZ = dirToPlayer.clone().crossProduct(new Vector(0, 1, 0)).normalize();
		Vector crossY = dirToPlayer.clone().crossProduct(crossXZ).normalize();

		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.HOSTILE, 3f, 1.4f);

		// crit particle effect
		new PPParametric(Particle.CRIT, mBoss.getLocation().clone().add(dirToPlayer.clone().multiply(2.2)).add(0, 1.5, 0), (parameter, builder) -> {
			double angle = parameter * Math.PI * 2;

			Vector launchDir = crossXZ.clone().multiply(FastUtils.cos(angle)).add(crossY.clone().multiply(FastUtils.sin(angle))).multiply(1.8);

			builder.offset(launchDir.getX(), launchDir.getY(), launchDir.getZ());
		}).count(50).directionalMode(true).extra(1).spawnAsBoss();
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			new PPParametric(Particle.CRIT, mBoss.getLocation().clone().add(dirToPlayer.clone().multiply(2.35)).add(0, 1.5, 0), (parameter, builder) -> {
				double angle = parameter * Math.PI * 2;

				Vector launchDir = crossXZ.clone().multiply(FastUtils.cos(angle)).add(crossY.clone().multiply(FastUtils.sin(angle))).multiply(1.1);

				builder.offset(launchDir.getX(), launchDir.getY(), launchDir.getZ());
			}).count(40).directionalMode(true).extra(1).spawnAsBoss();
		}, 4);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			new PPParametric(Particle.CRIT, mBoss.getLocation().clone().add(dirToPlayer.clone().multiply(2.5)).add(0, 1.5, 0), (parameter, builder) -> {
				double angle = parameter * Math.PI * 2;

				Vector launchDir = crossXZ.clone().multiply(FastUtils.cos(angle)).add(crossY.clone().multiply(FastUtils.sin(angle))).multiply(0.4);

				builder.offset(launchDir.getX(), launchDir.getY(), launchDir.getZ());
			}).count(30).directionalMode(true).extra(1).spawnAsBoss();
		}, 8);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			final Location mLocation = mBoss.getLocation().clone().add(0, 1.7, 0);
			final Vector mToPlayer = dirToPlayer.clone().multiply(PROJECTILE_SPEED);

			@Override
			public void run() {
				if (mTicks == 0) {
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_WITCH_DEATH, SoundCategory.HOSTILE, 1.5f, 0.5f);
				}

				// move the projectile
				Vector angleDelta = mToPlayer.clone().crossProduct(mToPlayer.clone().crossProduct(player.getLocation().clone().toVector().add(new Vector(0, 1.5, 0)).subtract(mLocation.clone().toVector()))).normalize().multiply(TURN_SPEED);
				mToPlayer.add(angleDelta.multiply(-1)).normalize().multiply(PROJECTILE_SPEED);
				mLocation.add(mToPlayer);

				// check hitboxes
				Hitbox hitbox = new Hitbox.AABBHitbox(mWorld, BoundingBox.of(mLocation, HITBOX_SIZE, HITBOX_SIZE, HITBOX_SIZE));
				if (hitbox.getHitPlayers(true).contains(player)) {
					BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.PROJECTILE, ATTACK_DAMAGE, "Terra Blast", mLocation, Uamiel.SHIELD_STUN_TIME);

					new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, player.getLocation().clone().add(0, 1, 0))
						.extra(0)
						.delta(0.4, 0.4, 0.4)
						.count(10)
						.spawnAsBoss();

					this.cancel();
				}

				new PartialParticle(Particle.CLOUD, mLocation)
					.delta(0.1)
					.extra(0)
					.count(3)
					.spawnAsBoss();
				new PartialParticle(Particle.EXPLOSION_NORMAL, mLocation)
					.delta(0.1)
					.extra(0)
					.count(10)
					.spawnAsBoss();

				mTicks++;
				if (mTicks > 60 || mBoss.isDead()) {
					this.cancel();
				}
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 12, 1);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
