package com.playmonumenta.plugins.bosses.spells.falsespirit;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
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

import com.playmonumenta.plugins.bosses.bosses.FalseSpirit;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

public class PassiveLongRangeLaser extends Spell {

	private int mCooldown = 0;
	private LivingEntity mBoss;
	private Plugin mPlugin;
	private int mTimer;

	private static final Particle.DustOptions ENCHANTED_ARROW_COLOR = new Particle.DustOptions(Color.fromRGB(225, 255, 219), 2.0f);
	private static final Particle.DustOptions ENCHANTED_ARROW_FRINGE_COLOR = new Particle.DustOptions(Color.fromRGB(168, 255, 252), 2.0f);

	public PassiveLongRangeLaser(Plugin plugin, LivingEntity boss, int time) {
		mBoss = boss;
		mPlugin = plugin;
		mTimer = time;
	}

	@Override
	public void run() {
		mCooldown -= 5;
		if (mCooldown <= 0) {
			mCooldown = mTimer;

			//List is sorted with nearest players earlier in the list, and farthest players at the end
			List<Player> players = EntityUtils.getNearestPlayers(mBoss.getLocation(), FalseSpirit.detectionRange);
			players.removeIf(p -> p.getGameMode() == GameMode.SPECTATOR || mBoss.getLocation().distance(p.getLocation()) < FalseSpirit.meleeRange);

			if (players.size() <= 0) {
				return;
			}

			Player target = players.get(FastUtils.RANDOM.nextInt(players.size()));

			Location launLoc = mBoss.getLocation().add(0, 1.6f, 0);
			Location tarLoc = target.getLocation();

			Vector baseVect = new Vector(tarLoc.getX() - launLoc.getX(), tarLoc.getY() - launLoc.getY(), tarLoc.getZ() - launLoc.getZ()).normalize().multiply(0.5);

			target.playSound(tarLoc, Sound.AMBIENT_UNDERWATER_ENTER, SoundCategory.HOSTILE, 1f, 0);

			BukkitRunnable runnable = new BukkitRunnable() {
				int mTicks = 0;
				Location mEndLoc = launLoc;
				@Override
				public void run() {

					BoundingBox box = BoundingBox.of(launLoc, 0.5, 0.5, 0.5);

					for (int i = 0; i < 75; i++) {
						box.shift(baseVect);
						mEndLoc = box.getCenter().toLocation(mBoss.getWorld());

						if (FastUtils.RANDOM.nextInt(3) == 0) {
							mEndLoc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, mEndLoc, 1, 0.02, 0.02, 0.02, 0);
						}
					}


					if (mTicks >= 20 * 3) {
						box = BoundingBox.of(launLoc, 1.5, 1.5, 1.5);

						World world = tarLoc.getWorld();
						Vector dir = new Vector(tarLoc.getX() - launLoc.getX(), tarLoc.getY() - launLoc.getY(), tarLoc.getZ() - launLoc.getZ()).normalize();
						Vector pVec = new Vector(launLoc.getDirection().getX(), launLoc.getDirection().getY(), launLoc.getDirection().getZ());
						pVec = pVec.normalize();

						for (int i = 0; i < 75; i++) {
							box.shift(baseVect);
							mEndLoc = box.getCenter().toLocation(mBoss.getWorld());
							world.spawnParticle(Particle.SMOKE_NORMAL, mEndLoc, 5, 0.1, 0.1, 0.1, 0.2);
							world.spawnParticle(Particle.FALLING_DUST, mEndLoc, 5, 0.1, 0.1, 0.1, 0, Bukkit.createBlockData("light_gray_glazed_terracotta"));
							world.spawnParticle(Particle.SPELL_INSTANT, mEndLoc, 6, 0.2, 0.2, 0.2, 0.15);
							world.spawnParticle(Particle.SMOKE_NORMAL, mEndLoc, 3, 0.1, 0.1, 0.1, 0);
							world.spawnParticle(Particle.REDSTONE, mEndLoc, 3, 0.1, 0.1, 0.1, 0, ENCHANTED_ARROW_COLOR);
							world.spawnParticle(Particle.CRIT, mEndLoc, 5, 0.15, 0.15, 0.15, 0.4);
							world.spawnParticle(Particle.REDSTONE, mEndLoc.clone().add(pVec), 1, 0, 0, 0, 0, ENCHANTED_ARROW_FRINGE_COLOR);
							world.spawnParticle(Particle.END_ROD, mEndLoc.clone().add(pVec), 1, 0, 0, 0, 0.02);
							world.spawnParticle(Particle.REDSTONE, mEndLoc.clone().add(dir.clone().multiply(0.5)).add(pVec.clone().rotateAroundAxis(dir, Math.PI / 12)), 1, 0, 0, 0, 0, ENCHANTED_ARROW_FRINGE_COLOR);

							if (target.getBoundingBox().overlaps(box)) {
								BossUtils.bossDamage(mBoss, target, 10);
								world.createExplosion(mBoss, tarLoc, 2, false, true);

								world.playSound(mEndLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.85f);
								this.cancel();
								return;
							}

							pVec.rotateAroundAxis(dir, Math.PI / 6);
						}

						this.cancel();

					}

					mTicks += 2;
				}
			};

			runnable.runTaskTimer(mPlugin, 0, 2);
			mActiveRunnables.add(runnable);
		}

	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
