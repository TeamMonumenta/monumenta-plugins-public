package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
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

/*
 Hailstorm - Creates a snowstorm in a circle that is 24 blocks and beyond that passively
 deals 5% max health damage every half second to players are in it and giving them slowness
 3 for 2 seconds.
 */
public class SpellHailstorm extends Spell {

	//Used when the boss teleports, prevent from doing damage
	private boolean mDoDamage = true;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mStartLoc;
	private boolean mAttack = false;
	private final double mRadius;
	private final List<Player> mWarned = new ArrayList<Player>();
	private final Map<Player, BukkitRunnable> mDamage = new HashMap<>();
	private final PPCircle mInnerCircle;
	private final PPCircle mOuterCircle;

	private @Nullable BukkitRunnable mDelay;

	private static final Particle.DustOptions LIGHT_BLUE_COLOR = new Particle.DustOptions(Color.fromRGB(0, 255, 247), 1.0f);

	public SpellHailstorm(Plugin plugin, LivingEntity boss, double radius, Location start) {
		mBoss = boss;
		mRadius = radius;
		mPlugin = plugin;
		mStartLoc = start;

		Location loc = boss.getLocation();
		mInnerCircle = new PPCircle(Particle.REDSTONE, loc, radius - 0.75).ringMode(true).count(60).delta(0.1).extra(1).data(LIGHT_BLUE_COLOR);
		mOuterCircle = new PPCircle(Particle.CLOUD, loc, radius + 5).ringMode(true).count(30).delta(2).extra(0.075);

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

		if (mDoDamage) {
			Location offsetLoc = loc.clone().add(0, 2, 0);
			mOuterCircle.location(offsetLoc);
			mOuterCircle.spawnAsBoss();
		}

		Location offsetLoc = loc.clone().add(0, 0.2, 0);
		mInnerCircle.location(offsetLoc);
		mInnerCircle.spawnAsBoss();

		if (!mAttack) {
			mAttack = true;
			return;
		}

		for (Player player : PlayerUtils.playersInRange(loc, FrostGiant.detectionRange, true)) {

			//This location sets player's y location to the mBoss' y location to compute location ignoring the y level
			//Essentially distance in terms of x and z only
			Location pLocY = player.getLocation();
			pLocY.setY(loc.getY());

			if (mDoDamage && pLocY.distanceSquared(loc) > mRadius * mRadius && player.getGameMode() != GameMode.CREATIVE && !mDamage.containsKey(player) && mStartLoc.distance(player.getLocation()) <= FrostGiant.fighterRange && player.getLocation().getY() - mStartLoc.getY() <= 45) {
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

							Location loc = mBoss.getLocation();
							Location pLocY = player.getLocation();
							pLocY.setY(loc.getY());

							if (pLocY.distanceSquared(loc) > mRadius * mRadius && mDoDamage) {
								Vector vel = player.getVelocity();
								BossUtils.bossDamagePercent(mBoss, player, 0.15, loc, "Hailstorm");
								player.setVelocity(vel);
								player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 2, 0));
							} else {
								mDamage.remove(player);
								this.cancel();
							}
						}
						mTicks += 2;
						mPitch += 0.025f;
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
		if (mDelay != null && !mDelay.isCancelled()) {
			mDelay.cancel();
		}

		mDoDamage = false;
		mDelay = new BukkitRunnable() {
			@Override
			public void run() {
				mDoDamage = true;
			}
		};
		mDelay.runTaskLater(mPlugin, 20 * 4);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
