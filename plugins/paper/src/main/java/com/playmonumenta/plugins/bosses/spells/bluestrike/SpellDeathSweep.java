package com.playmonumenta.plugins.bosses.spells.bluestrike;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellDeathSweep extends Spell {
	private static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f);

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private PartialParticle mPRed;
	private PartialParticle mPHit;
	private PartialParticle mPHitCone1;
	private PartialParticle mPHitCone2;
	private final int mDelay;
	private final int mPhase;
	private boolean mCooldown;

	// Attack in a 90 degree fan, front and back, knocking player back about 5 blocks.
	// Phase 1-4: 65 Damage, Vuln +20% 5s, Faster Casts
	public SpellDeathSweep(Plugin plugin, LivingEntity boss, int phase) {
		mPlugin = plugin;
		mBoss = boss;
		mDelay = getCastTime();
		mPhase = phase;

		mPRed = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 1, 0.1, 0.1, 0.1, 0.1, RED);
		mPHitCone1 = new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation(), 1, 0.1, 0.1, 0.1, 0.1);
		mPHitCone2 = new PartialParticle(Particle.CRIT_MAGIC, mBoss.getLocation(), 2, 0.1, 0.1, 0.1, 0.1);
		mPHit = new PartialParticle(Particle.CRIT_MAGIC, mBoss.getLocation(), 20, 0.25, 0.25, 0.25, 0.25);
	}

	@Override
	public void run() {
		mCooldown = true;
		new BukkitRunnable() {

			@Override
			public void run() {
				mCooldown = false;
			}

		}.runTaskLater(mPlugin, cooldownTicks() + 20);

		mBoss.setAI(false);

		List<Player> targets = EntityUtils.getNearestPlayers(mBoss.getLocation(), 100);
		Collections.shuffle(targets);
		Player target = targets.get(0);
		Vector dir = LocationUtils.getDirectionTo(target.getLocation(), mBoss.getLocation());
		Location tloc = mBoss.getLocation().setDirection(dir);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					return;
				}

				if (mT % 2 == 0) {
					Vector v;
					for (double r = 0; r <= 15; r += 0.5) {
						double resolution = 90.0 / (r + 1.0);

						for (double degree = -45; degree <= 45; degree += resolution) {
							double radian1 = Math.toRadians(degree);
							v = new Vector(Math.cos(radian1) * r, 0.8, Math.sin(radian1) * r);
							v = VectorUtils.rotateXAxis(v, 0);
							v = VectorUtils.rotateYAxis(v, tloc.getYaw() + 90);

							Location loc = mBoss.getLocation().clone().add(v);
							if (loc.getBlock().getType() == Material.AIR) {
								mPRed.location(loc).spawnAsBoss();
							} else {
								mPRed.location(loc.add(0, 0.5, 0)).spawnAsBoss();
							}

							Location loc2 = mBoss.getLocation().clone().subtract(v.clone().setY(-0.8));
							if (loc2.getBlock().getType() == Material.AIR) {
								mPRed.location(loc2).spawnAsBoss();
							} else {
								mPRed.location(loc2.add(0, 0.5, 0)).spawnAsBoss();
							}

						}
					}
				}

				if (mT % (getCastTime() / 4) == 0) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 5, 1);
				}

				if (mT > mDelay) {
					mBoss.setAI(true);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 5, 0.7f);

					Vector vec;
					for (double r1 = 0; r1 <= 15; r1 += 0.5) {
						for (double degree1 = -45; degree1 < 45; degree1 += 10) {
							double radian2 = Math.toRadians(degree1);
							vec = new Vector(Math.cos(radian2) * r1, 0.8, Math.sin(radian2) * r1);
							vec = VectorUtils.rotateXAxis(vec, 0);
							vec = VectorUtils.rotateYAxis(vec, tloc.getYaw() + 90);

							Location l1 = mBoss.getLocation().clone().add(vec);
							mPHitCone1.location(l1).spawnAsBoss();
							mPHitCone2.location(l1).spawnAsBoss();
							BoundingBox box1 = BoundingBox.of(l1, 0.4, 4, 0.4);

							Location l2 = mBoss.getLocation().clone().subtract(vec.clone().setY(-0.8));
							mPHitCone1.location(l2).spawnAsBoss();
							mPHitCone2.location(l2).spawnAsBoss();
							BoundingBox box2 = BoundingBox.of(l2, 0.4, 4, 0.4);

							for (Player player : targets) {
								if (player.getBoundingBox().overlaps(box1) || player.getBoundingBox().overlaps(box2)) {
									// Damage + KB in the function
									mPHit.location(player.getLocation()).spawnAsBoss();
									hit(player);
								}
							}
						}
					}
					this.cancel();
					return;
				}

				mT++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		if (mPhase <= 3) {
			return 10 * 20;
		} else {
			return 5 * 20;
		}
	}

	private int getCastTime() {
		if (mPhase <= 3) {
			return 40;
		} else {
			return 24;
		}
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

	public void hit(Player player) {
		String cause = "Death Sweep";
		AbilityUtils.increaseDamageRecievedPlayer(player, 20 * 8, 0.2, cause);
		BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MELEE, 65, cause, mBoss.getLocation());
		MovementUtils.knockAway(mBoss, player, 1f, 0.5f);
	}
}
