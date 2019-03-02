package com.playmonumenta.bossfights.spells;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.bossfights.utils.Utils;

public class SpellAxtalWitherAoe extends Spell {
	private Plugin mPlugin;
	private Entity mLauncher;
	private int mRadius;
	private int mPower;

	public SpellAxtalWitherAoe(Plugin plugin, Entity launcher, int radius, int power) {
		mPlugin = plugin;
		mLauncher = launcher;
		mRadius = radius;
		mPower = power;
	}

	@Override
	public void run() {
		World world = mLauncher.getWorld();
		new BukkitRunnable() {
			float j = 0;
			double radius = mRadius;

			@Override
			public void run() {
				Location loc = mLauncher.getLocation();
				j++;
				world.spawnParticle(Particle.SPELL_WITCH, mLauncher.getLocation().add(0, 1, 0), 25, 6, 3, 6);
				if (j <= 75) {
					world.playSound(mLauncher.getLocation(), Sound.ENTITY_CAT_HISS, 1.5f, 0.25f + (j / 100));
				}
				for (double i = 0; i < 360; i += 12) {
					double radian1 = Math.toRadians(i);
					loc.add(Math.cos(radian1) * radius, 0, Math.sin(radian1) * radius);
					world.spawnParticle(Particle.SMOKE_LARGE, loc, 1, 0.25, 0.25, 0.25, 0, null, true);
					loc.subtract(Math.cos(radian1) * radius, 0, Math.sin(radian1) * radius);
				}
				radius -= 0.1625;
				if (radius <= 0) {
					this.cancel();
					deal_damage();
					world.playSound(mLauncher.getLocation(), Sound.ENTITY_WITHER_SHOOT, 3, 0.5f);
					world.playSound(mLauncher.getLocation(), Sound.ENTITY_WITHER_SHOOT, 3, 1f);
					world.playSound(mLauncher.getLocation(), Sound.ENTITY_WITHER_SHOOT, 3, 1.5f);
					world.spawnParticle(Particle.SMOKE_LARGE, loc, 125, 0, 0, 0, 0.5, null, true);

					new BukkitRunnable() {
						Location loc = mLauncher.getLocation();
						double radius = 0;
						@Override
						public void run() {
							for (int j = 0; j < 2; j++) {
								radius += 1.5;
								for (double i = 0; i < 360; i += 15) {
									double radian1 = Math.toRadians(i);
									loc.add(Math.cos(radian1) * radius, 0, Math.sin(radian1) * radius);
									world.spawnParticle(Particle.SMOKE_NORMAL, loc, 4, 0.25, 0.25, 0.25, 0.35, null, true);
									loc.subtract(Math.cos(radian1) * radius, 0, Math.sin(radian1) * radius);
								}
							}
							if (radius >= 13) {
								this.cancel();
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

	}

	@Override
	public int duration() {
		return 160; // 8 seconds
	}

	private void deal_damage() {
		for (Player player : Utils.playersInRange(mLauncher.getLocation(), mRadius)) {
			double distance = player.getLocation().distance(mLauncher.getLocation());
			int pot_pow = (int)(mPower * ((mRadius - distance) / mRadius));
			player.addPotionEffect(new PotionEffect(PotionEffectType.HARM, 1, pot_pow));
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 1));
		}
	}
}
