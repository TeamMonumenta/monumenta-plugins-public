package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellSmokeBomb extends Spell {
	private Plugin mPlugin;
	private LivingEntity mLauncher;
	private int mRadius;
	private int mTime;
	private int mW;

	public SpellSmokeBomb(Plugin plugin, LivingEntity launcher, int radius, int time) {
		mPlugin = plugin;
		mLauncher = launcher;
		mRadius = radius;
		mTime = time;
	}

	@Override
	public void run() {
		mW = 0;
		animation(mLauncher.getLocation());
		deal_damage();
	}

	@Override
	public int duration() {
		return 160; // 8 seconds
	}

	private void deal_damage() {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable dealer = new Runnable() {
			@Override
			public void run() {
				for (Player player : PlayerUtils.playersInRange(mLauncher.getLocation(), mRadius)) {
					BossUtils.bossDamage(mLauncher, player, 2);
					player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
					player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
				}
			}
		};
		scheduler.scheduleSyncDelayedTask(mPlugin, dealer, mTime);
	}

	private void animation(Location loc) {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();

		Runnable animLoop = new Runnable() {
			@Override
			public void run() {
				Location centerLoc = new Location(loc.getWorld(), loc.getX(), loc.getY() + 1, loc.getZ());
				mLauncher.teleport(new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ()));
				centerLoc.getWorld().playSound(centerLoc, Sound.UI_TOAST_IN, (float)mRadius / 7, (float)(0.5 + FastUtils.RANDOM.nextInt(150) / 100));
				centerLoc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, centerLoc, 10, 1, 1, 1, 0.01);
			}
		};

		Runnable animLoop2 = new Runnable() {
			@Override
			public void run() {
				Location lloc = mLauncher.getLocation();
				double precision = FastUtils.RANDOM.nextInt(50) + 100;
				double increment = (2 * Math.PI) / precision;
				Location particleLoc = new Location(lloc.getWorld(), 0, lloc.getY() + 1.5, 0);
				double rad = (double)(mRadius * mW) / 5;
				double angle = 0;
				for (int j = 0; j < precision; j++) {
					angle = j * increment;
					particleLoc.setX(lloc.getX() + (rad * FastUtils.cos(angle)));
					particleLoc.setZ(lloc.getZ() + (rad * FastUtils.sin(angle)));
					particleLoc.setY(lloc.getY() + 1.5);
					particleLoc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, particleLoc, 1, 0.02, 1.5 * rad, 0.02, 0);
				}
				if (mW == 0) {
					particleLoc.getWorld().playSound(particleLoc, Sound.ENTITY_WITHER_SHOOT, (float)mRadius / 7, 0.77F);
					particleLoc.getWorld().playSound(particleLoc, Sound.ENTITY_WITHER_SHOOT, (float)mRadius / 7, 0.5F);
					particleLoc.getWorld().playSound(particleLoc, Sound.ENTITY_WITHER_SHOOT, (float)mRadius / 7, 0.65F);
				}
				mW++;
			}
		};

		for (int i = 0; i < mTime; i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, animLoop, i);
		}
		for (int i = 0; i < 6; i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, animLoop2, i + mTime);
		}
	}
}
