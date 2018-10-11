package com.playmonumenta.bossfights.spells;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

import com.playmonumenta.bossfights.utils.Utils;

public class SpellFrostNova extends Spell {
	private Plugin mPlugin;
	private Entity mLauncher;
	private int mRadius;
	private int mPower;
	private Random mRand = new Random();
	private int w;

	public SpellFrostNova(Plugin plugin, Entity launcher, int radius, int power) {
		mPlugin = plugin;
		mLauncher = launcher;
		mRadius = radius;
		mPower = power;
	}

	@Override
	public void run() {
		w = -80;
		animation();
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
				for (Player player : Utils.playersInRange(mLauncher.getLocation(), mRadius)) {
					double distance = player.getLocation().distance(mLauncher.getLocation());
					int pot_pow = (int)((double)mPower * (((double)mRadius - distance) / (double)mRadius));
					player.addPotionEffect(new PotionEffect(PotionEffectType.HARM, 1, pot_pow));
					player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 30, 2));
				}
			}
		};
		scheduler.scheduleSyncDelayedTask(mPlugin, dealer, 80L);
	}

	private void animation() {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable anim_loop = new Runnable() {
			@Override
			public void run() {
				Location lloc = mLauncher.getLocation();
				int  n = mRand.nextInt(50) + 100;
				double precision = n;
				double increment = (2 * Math.PI) / precision;
				Location particleLoc = new Location(lloc.getWorld(), 0, lloc.getY() + 1.5, 0);
				double rad = mRadius * (w < 0 ? (double)w / 80 : (double)w / 5);
				double angle = 0;
				for (int j = 0; j < precision; j++) {
					angle = (double)j * increment;
					particleLoc.setX(lloc.getX() + (rad * Math.cos(angle)));
					particleLoc.setZ(lloc.getZ() + (rad * Math.sin(angle)));
					particleLoc.setY(lloc.getY() + 1.5);
					particleLoc.getWorld().spawnParticle(Particle.SNOWBALL, particleLoc, 1, 0.02, 1.5 * rad, 0.02, 0);
				}
				if (w < -20 && w % 2 == 0) {
					particleLoc.getWorld().playSound(particleLoc, Sound.ENTITY_SNOWBALL_THROW, (float)mRadius / 7, (float)(0.5 + ((float)(w + 60) / 100)));
				} else if (w == -1) {
					particleLoc.getWorld().playSound(particleLoc, Sound.BLOCK_GLASS_BREAK, (float)mRadius / 7, 0.77F);
					particleLoc.getWorld().playSound(particleLoc, Sound.BLOCK_GLASS_BREAK, (float)mRadius / 7, 0.5F);
					particleLoc.getWorld().playSound(particleLoc, Sound.BLOCK_GLASS_BREAK, (float)mRadius / 7, 0.65F);
				}
				w++;
			}
		};

		for (int i = -80; i < 5; i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, anim_loop, 1L * (i + 81));
		}
	}
}