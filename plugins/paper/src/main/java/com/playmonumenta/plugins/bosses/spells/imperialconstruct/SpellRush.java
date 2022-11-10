package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.destroystokyo.paper.entity.Pathfinder;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.*;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;




public class SpellRush extends Spell {

	private final EnumSet<Material> mIgnoredMats = EnumSet.of(
		Material.AIR,
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.BEDROCK,
		Material.BARRIER,
		Material.SPAWNER,
		Material.LIGHT
	);

	//private static final int MAX_DISTANCE = 15;
	private static final float VELOCITY = 0.9f;
	private Plugin mPlugin;
	private LivingEntity mBoss;
	//private Location mCurrentLoc;

	private PassiveRushBlockBreak mBlockBreak;

	private static final int RESPAWN_DURATION = 20 * 10;

	public Location mStartLoc;
	private int mRange;

	public SpellRush(Plugin plugin, LivingEntity boss, Location startLoc, int range) {
		mPlugin = plugin;
		mBoss = boss;
		mStartLoc = startLoc;
		mRange = range;
		//mCurrentLoc = middleLoc.clone();

		mBlockBreak = new PassiveRushBlockBreak(mBoss, 5, 5, 5);
	}

	@Override
	public void run() {
		mBlockBreak.mIsActive = true;
		List<Player> players = PlayerUtils.playersInRange(mStartLoc, mRange, true);
		if (players.isEmpty()) {
			return;
		}
		Location endLoc = players.get(FastUtils.RANDOM.nextInt(players.size())).getLocation();
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 100f, 1f);
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				Creature c = (Creature) mBoss;
				Pathfinder pathfinder = c.getPathfinder();

				c.setTarget(null);
				pathfinder.stopPathfinding();

				endLoc.setY(mBoss.getLocation().getY() + 1);

				if (mTicks >= 20 * 7 || endLoc.distance(mBoss.getLocation()) < 3) {
					c.setTarget(EntityUtils.getNearestPlayer(c.getLocation(), 15));
					mBlockBreak.mIsActive = false;
					this.cancel();
					return;
				}

				if (mTicks % 10 == 0) {
					new PPLine(Particle.WAX_ON, mBoss.getLocation(), endLoc).countPerMeter(3).delta(0.5).extra(0.025).spawnAsBoss();
					new PPLine(Particle.ELECTRIC_SPARK, mBoss.getLocation(), endLoc).countPerMeter(3).delta(0.5).extra(0.025).spawnAsBoss();
				}

				if (mTicks <= 20 * 2) {
					Vector dir = endLoc.clone().toVector().subtract(mBoss.getLocation().toVector().clone());
					dir.normalize();
					dir.multiply(VELOCITY);
				}

				if (mTicks > 20 * 2) {
					List<Location> blocksToRegen = new ArrayList<>();
					Location loc = mBoss.getLocation();
					Location tempLoc = mBoss.getLocation();
					for (int y = -2; y <= 2; y++) {
						for (int x = -2; x <= 2; x++) {
							for (int z = -2; z <= 2; z++) {
								tempLoc.set(loc.getX() + x, loc.getY() + y, loc.getZ() + z);
								if (!mIgnoredMats.contains(tempLoc.getBlock().getType())) {
									blocksToRegen.add(tempLoc.clone());
									tempLoc.getBlock().setType(Material.AIR);
								}
							}
						}
					}

					runBlockRespawn(blocksToRegen);

					for (Player p : PlayerUtils.playersInRange(loc, 5, true)) {
						DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, 90, null, false, true, "Metallic Rush");
						MovementUtils.knockAway(loc, p, 1f, 0.25f, false);
					}

					Vector dir = endLoc.toVector().clone().subtract(loc.clone().toVector());
					dir.normalize();
					dir.multiply(VELOCITY);
					mBoss.setVelocity(dir);
					if (mTicks % 10 == 0) {
						world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.3f, 0.9f);
						world.spawnParticle(Particle.EXPLOSION_HUGE, loc, 1, 2, 2, 2);
					}

				}
				mTicks++;
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	private void runBlockRespawn(List<Location> blocksToRegen) {
		if (blocksToRegen.size() > 0) {
			BukkitRunnable runnable = new BukkitRunnable() {
				int mTicks = 0;
				int mCount = 0;
				@Override
				public void run() {
					/* Replace a slice of blocks every tick, so that all blocks are replaced over the duration */
					for (; mCount < (((blocksToRegen.size() - 1) * mTicks) / RESPAWN_DURATION) + 1; mCount++) {
						blocksToRegen.get(mCount).getBlock().setType(Material.COBBLESTONE);
					}
					if (mCount >= blocksToRegen.size()) {
						this.cancel();
						return;
					}
					mTicks++;
				}
			};
			runnable.runTaskTimer(mPlugin, 20 * 2, 1);
			mActiveRunnables.add(runnable);
		}
	}

	public void setLocation(Location loc) {
		//mCurrentLoc = loc;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 5;
	}
}
