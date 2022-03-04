package com.playmonumenta.plugins.bosses.spells.falsespirit;

import com.playmonumenta.plugins.bosses.bosses.FalseSpirit;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
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

public class TriplicateSlash extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;

	//direction is 0 or 180
	private int mDirection = 0;

	public TriplicateSlash(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		mDirection = 0;

		Vector dir = mBoss.getLocation().getDirection();

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				Location loc = mBoss.getLocation();
				loc.setDirection(dir);

				if (mTicks > 0 && mTicks % 30 == 0) {
					Vector vec;
					List<BoundingBox> boxes = new ArrayList<>();

					world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 3, 0.5f);

					//Final particle show
					for (double r = 0; r < 7; r++) {
						for (int dir = mDirection; dir <= 270 + mDirection; dir += 120) {
							for (double degree = 60; degree < 120; degree += 5) {
								double radian1 = Math.toRadians(degree);
								vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
								vec = VectorUtils.rotateYAxis(vec, loc.getYaw() + dir);

								Location l = loc.clone().add(vec);
								//1.5 -> 15
								BoundingBox box = BoundingBox.of(l, 0.65, 3, 0.65);
								boxes.add(box);

								world.spawnParticle(Particle.SWEEP_ATTACK, l, 1, 0.1, 0.2, 0.1, 0.1);
							}
						}
					}

					for (Player player : PlayerUtils.playersInRange(loc, 10, true)) {
						for (BoundingBox box : boxes) {
							if (player.getBoundingBox().overlaps(box)) {
								DamageUtils.damage(mBoss, player, DamageType.MAGIC, 30, null, false, true, "Triplicate Slash");
							}
						}
					}

					if (mTicks >= 90) {
						mDirection = 0;
						this.cancel();
					} else if (mDirection == 0) {
						mDirection = 180;
					} else {
						mDirection = 0;
					}
				} else {
					world.playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, 2, 2f);

					for (int dir = mDirection; dir <= 270 + mDirection; dir += 120) {
						Vector vec;
						//The degree range is 60 degrees for 30 blocks radius
						for (double degree = 60; degree < 120; degree += 5) {
							for (double r = 0; r < 7; r++) {
								double radian1 = Math.toRadians(degree);
								vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
								vec = VectorUtils.rotateYAxis(vec, loc.getYaw() + dir);

								//Spawns particles
								Location l = loc.clone().add(vec);
								world.spawnParticle(Particle.CRIT, l, 1, 0.1, 0.2, 0.1, 0);
							}
						}
					}
				}
				mTicks += 2;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return 6 * 20;
	}

	@Override
	public boolean canRun() {
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), FalseSpirit.detectionRange, true)) {
			if (mBoss.getLocation().distance(player.getLocation()) < FalseSpirit.meleeRange) {
				return true;
			}
		}
		return false;
	}

}
