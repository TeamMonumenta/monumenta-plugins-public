package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellDuelist extends Spell {

	private static final Particle.DustOptions SWORD_COLOR = new Particle.DustOptions(Color.fromRGB(200, 200, 200), 1.0f);

	private static final int RANGE = 5;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final int mCooldown;
	private final int mDamage;

	public SpellDuelist(Plugin plugin, LivingEntity boss, int cooldown, int damage) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mCooldown = cooldown;
		mDamage = damage;
	}

	@Override
	public void run() {
		if (!(mBoss instanceof Mob)) {
			return;
		}

		Location loc = mBoss.getLocation();
		LivingEntity target = ((Mob) mBoss).getTarget();
		if (!(target instanceof Player && LocationUtils.hasLineOfSight(mBoss, target))) {
			List<Player> players = PlayerUtils.playersInRange(loc, RANGE, false);
			for (Player player : players) {
				if (LocationUtils.hasLineOfSight(mBoss, player) && loc.distance(player.getLocation()) < RANGE) {
					target = player;
					break;
				}
			}
		}

		if (target == null) {
			return;
		}

		((Mob) mBoss).setTarget(target);


		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_VINDICATOR_HURT, 1f, 0.5f);
		int random = FastUtils.RANDOM.nextInt(3);

		if (random == 0) {
			Vector direction = mBoss.getLocation().getDirection().setY(0).normalize();
			Vector sideways = new Vector(direction.getZ() / 2, 0, -direction.getX() / 2);
			Location locParticle = mBoss.getLocation().add(0, 1.75, 0).subtract(sideways.clone().multiply(10));
			for (int i = 0; i <= 20; i++) {
				new PartialParticle(Particle.REDSTONE, locParticle, 5, 0.2, 0.2, 0.2, 0, SWORD_COLOR).spawnAsEntityActive(mBoss);
				locParticle.add(sideways);
			}

			BukkitRunnable attack = new BukkitRunnable() {
				List<Player> mPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), RANGE * 4, true);
				int mTime = 0;

				@Override
				public void run() {
					Vector forwards = mBoss.getLocation().getDirection().setY(0).normalize();
					Vector sideways = new Vector(forwards.getZ(), 0, -forwards.getX());

					Vector shift1 = new Vector(0, 0, 0).add(forwards.clone().multiply(Math.sin(Math.PI * mTime / 8))).add(sideways.clone().multiply(Math.cos(Math.PI * mTime / 8)));
					Location loc1 = mBoss.getLocation().add(0, 2, 0);
					Vector shift2 = new Vector(0, 0, 0).add(forwards.clone().multiply(Math.sin(Math.PI * mTime / 8))).add(sideways.clone().multiply(-Math.cos(Math.PI * mTime / 8)));
					Location loc2 = mBoss.getLocation().add(0, 2, 0);
					BoundingBox hitbox1 = new BoundingBox().shift(loc1).expand(1, 0.25, 1);
					BoundingBox hitbox2 = new BoundingBox().shift(loc2).expand(1, 0.25, 1);
					mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.3f, 1f);

					for (int i = 0; i < RANGE; i++) {
						loc1.add(shift1);
						hitbox1.shift(shift1);
						loc2.add(shift2);
						hitbox2.shift(shift2);
						new PartialParticle(Particle.SWEEP_ATTACK, loc1, 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.SWEEP_ATTACK, loc2, 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);

						Iterator<Player> iter = mPlayers.iterator();
						while (iter.hasNext()) {
							Player player = iter.next();
							BoundingBox box = player.getBoundingBox();
							if (box.overlaps(hitbox1) || box.overlaps(hitbox2)) {
								BossUtils.blockableDamage(mBoss, player, DamageType.MELEE, mDamage);
								iter.remove();
							}
						}
					}

					mTime++;
					if (mTime >= 5) {
						this.cancel();
					}
				}
			};

			attack.runTaskTimer(mPlugin, 10, 1);
			mActiveRunnables.add(attack);
		} else if (random == 1) {
			Vector direction = target.getLocation().subtract(mBoss.getLocation()).toVector().setY(0).normalize();
			Location locParticle = mBoss.getEyeLocation();
			for (int i = 0; i < 10; i++) {
				new PartialParticle(Particle.REDSTONE, locParticle, 5, 0.2, 0.2, 0.2, 0, SWORD_COLOR).spawnAsEntityActive(mBoss);
				locParticle.add(0, 0.5, 0);
			}

			BukkitRunnable attack = new BukkitRunnable() {
				List<Player> mPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), RANGE * 4, true);
				int mTime = 0;
				Vector mDirection = direction;

				@Override
				public void run() {
					Vector upwards = new Vector(0, 1, 0);
					Vector shift = new Vector(0, 0, 0).add(mDirection.clone().multiply(Math.sin(Math.PI * mTime / 8))).add(upwards.clone().multiply(Math.cos(Math.PI * mTime / 8)));
					Location loc = mBoss.getLocation();
					BoundingBox hitbox = new BoundingBox().shift(loc).expand(1);
					mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.3f, 1f);
					for (int i = 0; i < RANGE; i++) {
						loc.add(shift);
						hitbox.shift(shift);
						new PartialParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);

						Iterator<Player> iter = mPlayers.iterator();
						while (iter.hasNext()) {
							Player player = iter.next();
							if (player.getBoundingBox().overlaps(hitbox)) {
								BossUtils.blockableDamage(mBoss, player, DamageType.MELEE, mDamage);
								iter.remove();
							}
						}
					}

					mTime++;
					if (mTime >= 5) {
						this.cancel();
					}
				}
			};

			attack.runTaskTimer(mPlugin, 10, 1);
			mActiveRunnables.add(attack);
		} else {
			Vector direction = mBoss.getLocation().getDirection().setY(0).normalize();
			Vector sideways = new Vector(direction.getZ() / 2, 0, -direction.getX() / 2);
			Location locParticle = mBoss.getLocation().subtract(sideways.clone().multiply(10));
			for (int i = 0; i <= 20; i++) {
				new PartialParticle(Particle.REDSTONE, locParticle, 5, 0.2, 0.2, 0.2, 0, SWORD_COLOR).spawnAsEntityActive(mBoss);
				locParticle.add(sideways);
			}

			BukkitRunnable attack = new BukkitRunnable() {
				List<Player> mPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), RANGE * 4, true);
				int mTime = 0;

				@Override
				public void run() {
					Vector forwards = mBoss.getLocation().getDirection().setY(0).normalize();
					Vector sideways = new Vector(forwards.getZ(), 0, -forwards.getX());

					Vector shift1 = new Vector(0, 0, 0).add(forwards.clone().multiply(Math.sin(Math.PI * mTime / 8))).add(sideways.clone().multiply(Math.cos(Math.PI * mTime / 8)));
					Location loc1 = mBoss.getLocation();
					Vector shift2 = new Vector(0, 0, 0).add(forwards.clone().multiply(Math.sin(Math.PI * mTime / 8))).add(sideways.clone().multiply(-Math.cos(Math.PI * mTime / 8)));
					Location loc2 = mBoss.getLocation();
					BoundingBox hitbox1 = new BoundingBox().shift(loc1).expand(1, 0.25, 1);
					BoundingBox hitbox2 = new BoundingBox().shift(loc2).expand(1, 0.25, 1);
					mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.3f, 1f);

					for (int i = 0; i < RANGE; i++) {
						loc1.add(shift1);
						hitbox1.shift(shift1);
						loc2.add(shift2);
						hitbox2.shift(shift2);
						new PartialParticle(Particle.SWEEP_ATTACK, loc1, 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.SWEEP_ATTACK, loc2, 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);

						Iterator<Player> iter = mPlayers.iterator();
						while (iter.hasNext()) {
							Player player = iter.next();
							BoundingBox box = player.getBoundingBox();
							if (box.overlaps(hitbox1) || box.overlaps(hitbox2)) {
								BossUtils.blockableDamage(mBoss, player, DamageType.MELEE, mDamage);
								iter.remove();
							}
						}
					}

					mTime++;
					if (mTime >= 5) {
						this.cancel();
					}
				}
			};

			attack.runTaskTimer(mPlugin, 10, 1);
			mActiveRunnables.add(attack);
		}
	}

	@Override
	public boolean canRun() {
		Location loc = mBoss.getLocation();
		List<Player> players = PlayerUtils.playersInRange(loc, RANGE, false);
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
	public int cooldownTicks() {
		return mCooldown;
	}

}
