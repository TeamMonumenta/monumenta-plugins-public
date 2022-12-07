package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SundialSlash extends Spell {
	private static final double RADIUS = 9;
	private static final double DAMAGE = 60;
	private static final int DEGREES = 30;
	private static final int TIME_PER = 20;
	private static final int PERIOD = 2;

	private final LivingEntity mBoss;
	private final int mCooldownTicks;

	public SundialSlash(LivingEntity boss, int cooldownTicks) {
		mBoss = boss;
		mCooldownTicks = cooldownTicks;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		float yaw = mBoss.getLocation().getYaw();

		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;
			int mDeg = 25;

			@Override
			public void run() {
				Location bossLoc = mBoss.getLocation();

				List<BoundingBox> boxes = new ArrayList<>();
				for (double r = 0; r < RADIUS; r++) {
					for (double degree = 0; degree < DEGREES + 10; degree += 5) {
						boxes.addAll(cone(bossLoc, yaw, degree, mDeg, r));
						boxes.addAll(cone(bossLoc, yaw, degree, mDeg, -r));
					}
				}

				if (mT % TIME_PER == 0 && mT > 0) {
					for (Player player : PlayerUtils.playersInRange(bossLoc, RADIUS, true)) {
						for (BoundingBox box : boxes) {
							if (box.overlaps(player.getBoundingBox())) {
								DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MELEE, DAMAGE, null, false, true, "Sundial Slash");
								break;
							}
						}
					}

					mDeg += DEGREES;

					world.playSound(bossLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.75f);
				}

				if (mT % TIME_PER == 2) {
					world.playSound(bossLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.5f);
				}

				if (mT >= TIME_PER * 4) {
					this.cancel();
					return;
				}

				mT += PERIOD;
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(com.playmonumenta.plugins.Plugin.getInstance(), 0, PERIOD);
	}

	private List<BoundingBox> cone(Location bossLoc, double yaw, double degree, double mDeg, double r) {
		double radian = Math.toRadians(degree + mDeg);
		Vector vec = new Vector(FastUtils.cos(radian) * r, 0, FastUtils.sin(radian) * r);
		vec = VectorUtils.rotateYAxis(vec, yaw);

		List<BoundingBox> boxes = new ArrayList<>();
		Location l = bossLoc.clone().add(vec);
		BoundingBox box = BoundingBox.of(l, 0.55, 3, 0.55);
		boxes.add(box);

		Color color;
		if (FastUtils.RANDOM.nextBoolean()) {
			color = Color.fromRGB(235, 195, 52);
		} else {
			color = Color.fromRGB(255, 255, 71);
		}
		new PartialParticle(Particle.REDSTONE, l.clone().add(0, 1, 0), 1, 0.1, 0.1, 0.1, 0.1, new Particle.DustOptions(color, 1)).spawnAsEntityActive(mBoss);

		return boxes;
	}

	@Override
	public boolean canRun() {
		return !PlayerUtils.playersInRange(mBoss.getLocation(), RADIUS + 2, true).isEmpty();
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}
}
