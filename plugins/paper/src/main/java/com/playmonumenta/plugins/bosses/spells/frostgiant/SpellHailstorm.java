package com.playmonumenta.plugins.bosses.spells.frostgiant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 Hailstorm - Creates a snowstorm in a circle that is 24 blocks and beyond that passively
 deals 5% max health damage every half second to players are in it and giving them slowness
 3 for 2 seconds.
 */
public class SpellHailstorm extends Spell {

	//Used when the boss teleports, prevent from doing damage
	private boolean mDoDamage = true;

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mStartLoc;
	private boolean mAttack = false;
	private double mRadius;
	private List<Player> mWarned = new ArrayList<Player>();
	private Map<Player, BukkitRunnable> mDamage = new HashMap<>();

	private static final Particle.DustOptions BLUE_COLOR = new Particle.DustOptions(Color.fromRGB(66, 185, 245), 1.0f);
	private static final Particle.DustOptions LIGHT_BLUE_COLOR = new Particle.DustOptions(Color.fromRGB(0, 255, 247), 1.0f);

	public SpellHailstorm(Plugin plugin, LivingEntity boss, double radius, Location start) {
		mBoss = boss;
		mRadius = radius;
		mPlugin = plugin;
		mStartLoc = start;

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
				mWarned.clear();
			}
		}.runTaskTimer(mPlugin, 0, 20 * 10);
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation();
		World world = mBoss.getWorld();
		for (double degree = 0; degree < 360; degree += 8) {
			double radian = Math.toRadians(degree);
			loc.add(FastUtils.cos(radian) * (mRadius + 5), 2.5, FastUtils.sin(radian) * (mRadius + 5));
			world.spawnParticle(Particle.CLOUD, loc, 2, 2, 1, 3, 0.075);
			world.spawnParticle(Particle.CLOUD, loc, 2, 2, 4, 3, 0.075);
			world.spawnParticle(Particle.REDSTONE, loc, 3, 2, 4, 3, 0.075, BLUE_COLOR);
			world.spawnParticle(Particle.REDSTONE, loc, 3, 2, 1, 3, 0.075, BLUE_COLOR);
			loc.subtract(FastUtils.cos(radian) * (mRadius + 5), 2.5, FastUtils.sin(radian) * (mRadius + 5));
		}

		for (double degree = 0; degree < 360; degree++) {
			if (FastUtils.RANDOM.nextDouble() < 0.4) {
				double radian = Math.toRadians(degree);
				loc.add(FastUtils.cos(radian) * (mRadius + 2), 0.5, FastUtils.sin(radian) * (mRadius + 2));
				world.spawnParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, LIGHT_BLUE_COLOR);
				loc.subtract(FastUtils.cos(radian) * (mRadius + 2), 0.5, FastUtils.sin(radian) * (mRadius + 2));
			}
		}

		if (!mAttack) {
			mAttack = true;
			return;
		}

		for (Player player : PlayerUtils.playersInRange(loc, FrostGiant.fighterRange)) {
			if (mDoDamage && !player.getLocation().toVector().isInSphere(mBoss.getLocation().toVector(), mRadius) && player.getGameMode() != GameMode.CREATIVE && !mDamage.containsKey(player) && mStartLoc.distance(player.getLocation()) <= FrostGiant.fighterRange) {
				BukkitRunnable runnable = new BukkitRunnable() {
					int mTicks = 0;
					float mPitch = 1;
					@Override
					public void run() {
						if (mTicks <= 10) {
							player.playSound(player.getLocation(), Sound.ENTITY_SNOW_GOLEM_HURT, SoundCategory.HOSTILE, 1, mPitch);
						}

						if (mTicks % 10 == 0) {
							world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1, 1);
							world.spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation().add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.15);
							world.spawnParticle(Particle.SPIT, player.getLocation().add(0, 1, 0), 6, 0.4, 0.4, 0.4, 0.2);

						}

						if (mTicks >= 10 && mTicks % 10 == 0) {
							if (player.isDead() || mBoss.isDead() || !mBoss.isValid() || player.getGameMode() == GameMode.CREATIVE) {
								mDamage.remove(player);
								this.cancel();
							}

							if (!player.getLocation().toVector().isInSphere(mBoss.getLocation().toVector(), mRadius) && mDoDamage) {
								Vector vel = player.getVelocity();
								BossUtils.bossDamagePercent(mBoss, player, 0.1, loc);
								player.setVelocity(vel);
								player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 2, 0));
							} else {
								mDamage.remove(player);
								this.cancel();
							}
						}
						mTicks += 2;
						mPitch += 0.025;
					}
				};
				runnable.runTaskTimer(mPlugin, 0, 2);
				mDamage.put(player, runnable);

				if (!mWarned.contains(player)) {
					player.sendMessage(ChatColor.DARK_RED + "The Hailstorm is freezing! Move closer to the Giant!");
					mWarned.add(player);
				}
			}
		}
		mAttack = false;
	}

	public void delayDamage() {
		mDoDamage = false;
		new BukkitRunnable() {
			@Override
			public void run() {
				mDoDamage = true;
			}
		}.runTaskLater(mPlugin, 30);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
