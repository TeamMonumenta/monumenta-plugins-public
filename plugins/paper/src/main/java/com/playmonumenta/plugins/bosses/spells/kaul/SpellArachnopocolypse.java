package com.playmonumenta.plugins.bosses.spells.kaul;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellArachnopocolypse extends Spell {
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mLoc;
	private double mDetectRange;
	private boolean mCooldown = false;

	public SpellArachnopocolypse(Plugin plugin, LivingEntity boss, Location loc, double detectRange) {
		mPlugin = plugin;
		mBoss = boss;
		mLoc = loc;
		mDetectRange = detectRange;
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		mCooldown = true;
		new BukkitRunnable() {

			@Override
			public void run() {
				mCooldown = false;
			}

		}.runTaskLater(mPlugin, 20 * 60);
		new BukkitRunnable() {

			@Override
			public void run() {
				List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mDetectRange);
				players.removeIf(p -> p.getLocation().getY() >= 61);
				int amount = 10 + (5 * (players.size()));
				if (players.size() == 1) {
					amount = 18;
				}
				int a = amount;
				world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 10, 1);
				world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 50, 0.5, 0.25, 0.5, 0);
				new BukkitRunnable() {
					int mTicks = 0;
					@Override
					public void run() {
						mTicks++;
						riseSpider(getRandomLocation(mLoc, 32));
						if (mTicks >= a) {
							this.cancel();
						}
					}

				}.runTaskTimer(mPlugin, 0, 2);
			}

		}.runTaskLater(mPlugin, 30);

	}

	private static final String[] SPIDER_SUMMONS = {
	        "BlackrootBroodmother",
	        "BlackrootArachnid",
	        "ShieldbreakerSpider",
	        "BlackrootMonster"
	};

	public void riseSpider(Location loc) {
	    new BukkitRunnable() {
	        int mTicks = 0;
	        @Override
	        public void run() {
	            mTicks++;
	            World world = loc.getWorld();
	            world.spawnParticle(Particle.BLOCK_DUST, loc, 2, 0.4, 0.1, 0.4, 0.25, Material.DIRT.createBlockData());
	            if (mTicks >= 20) {
	                this.cancel();
	                world.playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 1, 1f);
	                world.spawnParticle(Particle.BLOCK_DUST, loc, 16, 0.25, 0.1, 0.25, 0.25, Material.DIRT.createBlockData());
	                LibraryOfSoulsIntegration.summon(loc.clone().add(0, 1, 0), SPIDER_SUMMONS[FastUtils.RANDOM.nextInt(SPIDER_SUMMONS.length)]);
	            }
	        }
	    }.runTaskTimer(mPlugin, 0, 1);
	}

	private Location getRandomLocation(Location origin, double range) {
		Location loc = origin.clone().add(FastUtils.randomDoubleInRange(-range, range), 0, FastUtils.randomDoubleInRange(-range, range));
		while (loc.getBlock().getType().isSolid()) {
			loc = origin.clone().add(FastUtils.randomDoubleInRange(-range, range), 0, FastUtils.randomDoubleInRange(-range, range));
		}
		return loc;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 20;
	}

	@Override
	public int castTicks() {
		return 20 * 5;
	}

}
