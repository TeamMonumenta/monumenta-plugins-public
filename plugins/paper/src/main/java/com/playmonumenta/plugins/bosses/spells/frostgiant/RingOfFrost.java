package com.playmonumenta.plugins.bosses.spells.frostgiant;

import org.bukkit.ChatColor;
import org.bukkit.Color;
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
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 Ring of Frost: Fast(er) cast speed. All players 18 blocks away or
 greater take 28 damage and are thrown towards the Frost Giant. Afterwards
 they are given slowness 3 for 8 seconds. (lowish CD)
 */

public class RingOfFrost extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private double mRadius;
	private Location mStartLoc;

	public RingOfFrost(Plugin plugin, LivingEntity boss, double radius, Location loc) {
		mPlugin = plugin;
		mBoss = boss;
		mRadius = radius;
		mStartLoc = loc;
	}

	private static final Particle.DustOptions GREEN_COLOR = new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.0f);

	@Override
	public void run() {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 3, 1.5f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 5, 0);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 3, 0);
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), FrostGiant.detectionRange)) {
			player.sendMessage(ChatColor.DARK_PURPLE + "The air away from the giant starts to freeze!");
		}
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			double mCurrentRadius = mRadius;
			float mPitch = 0.5f;

			@Override
			public void run() {
				Location loc = mBoss.getLocation();
				mTicks += 2;

				for (double degree = 0; degree < 360; degree += 4) {
					double radian = Math.toRadians(degree);
					double cos = FastUtils.cos(radian);
					double sin = FastUtils.sin(radian);
					loc.add(cos * mCurrentRadius, 0.1, sin * mCurrentRadius);
					world.spawnParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, GREEN_COLOR);
					loc.subtract(cos * mCurrentRadius, 0.1, sin * mCurrentRadius);

					if (degree % 16 == 0) {
						for (int r = 24; r > mCurrentRadius + 1; r -= 4) {
							Location l = mBoss.getLocation();
							l.add(cos * r + FastUtils.randomDoubleInRange(0, 2), 0.1, sin * r + + FastUtils.randomDoubleInRange(0, 2));
							world.spawnParticle(Particle.DAMAGE_INDICATOR, l, 1, 1, 0.1, 1, 0.05);
							world.spawnParticle(Particle.DRAGON_BREATH, l, 1, 1, 0.1, 1, 0.05);
						}
					}
				}

				world.playSound(mBoss.getLocation(), Sound.ENTITY_SNOW_GOLEM_SHOOT, SoundCategory.HOSTILE, 3, mPitch);
				mPitch += 0.025f;

				world.spawnParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0, 2, 0), 20, mCurrentRadius - 2, 2, mCurrentRadius - 2, 0);

				//After 4 seconds, damage everyone outside the green circle and pull them in
				if (mTicks >= 20 * 4) {
					this.cancel();
					for (double degree = 0; degree < 360; degree += 5) {
						double radian = Math.toRadians(degree);
						double cos = FastUtils.cos(radian);
						double sin = FastUtils.sin(radian);
						loc.add(cos * mCurrentRadius, 2, FastUtils.sin(radian) * mCurrentRadius);
						world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 10, 1, 3, 1, 0.45);
						loc.subtract(sin * mCurrentRadius, 2, FastUtils.sin(radian) * mCurrentRadius);
					}
					world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 3, 1.5f);
					world.playSound(mBoss.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 3, 0.65f);
					Vector vec = loc.toVector();
					for (Player player : PlayerUtils.playersInRange(loc, 50)) {
						if (!player.getLocation().toVector().isInSphere(vec, mCurrentRadius) && mStartLoc.distance(player.getLocation()) <= FrostGiant.fighterRange) {
							BossUtils.bossDamage(mBoss, player, 40, null);
							world.spawnParticle(Particle.EXPLOSION_NORMAL, player.getLocation().add(0, 1, 0), 30, 0.25, 0.45, 0.25, 0.2);
							MovementUtils.knockAway(loc, player, -2.75f);
							player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 10, 3));
						}
					}
				}
			}

		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);

	}

	@Override
	public int duration() {
		return 20 * 6;
	}
}
