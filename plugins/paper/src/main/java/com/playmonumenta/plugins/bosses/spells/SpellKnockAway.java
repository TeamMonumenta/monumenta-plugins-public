package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class SpellKnockAway extends Spell {
	private static final String KNOCK_AWAY_SLOWNESS_SOURCE = "KnockAwaySlownessEffect";
	private static final String KNOCK_AWAY_WEAKNESS_SOURCE = "KnockAwayWeaknessEffect";
	private static final String KNOCK_AWAY_SPEEDUP_SOURCE = "KnockAwaySpeedupCasterEffect";
	private final Plugin mPlugin;
	private final LivingEntity mLauncher;
	// These are declared as final until this spell is refactored to be usable with boss params
	private final double mDamage = 9.0;
	private final int mRadius;
	private final int mSlownessDuration = 20 * 4;
	private final double mSlownessPotency = -0.75;
	private final int mWeaknessDuration = 20 * 10;
	private final double mWeaknessPotency = -0.2;
	private final double mSpeedupPotency = 0.8;
	private final int mSpeedupDuration = 20 * 10;
	private final int mTime;
	private final float mSpeed;
	private int mWidth;

	public SpellKnockAway(Plugin plugin, LivingEntity launcher, int radius, int time, float speed) {
		mPlugin = plugin;
		mLauncher = launcher;
		mRadius = radius;
		mTime = time;
		mSpeed = speed;
	}

	@Override
	public void run() {
		mWidth = 0;
		animation(mLauncher.getLocation());
		deal_damage();
	}

	@Override
	public int cooldownTicks() {
		return 20; // 1 second
	}

	private void deal_damage() {
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			for (Player player : PlayerUtils.playersInRange(mLauncher.getLocation(), mRadius, true)) {
				if (!mLauncher.isDead() || mLauncher.isValid()) {
					BossUtils.blockableDamage(mLauncher, player, DamageType.MELEE, mDamage);
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, KNOCK_AWAY_SLOWNESS_SOURCE, new PercentSpeed(mSlownessDuration, mSlownessPotency, KNOCK_AWAY_SLOWNESS_SOURCE));
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, KNOCK_AWAY_WEAKNESS_SOURCE, new PercentDamageDealt(mWeaknessDuration, mWeaknessPotency, DamageType.getScalableDamageType()));
					Vector dir = player.getLocation().subtract(mLauncher.getLocation().toVector()).toVector().multiply(mSpeed);
					dir.setY(0.5f);
					player.setVelocity(dir);
				}
			}
		}, mTime);
	}

	private void animation(Location loc) {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		World world = loc.getWorld();
		Runnable animLoop = () -> {
			if (mLauncher.isDead() || !mLauncher.isValid()) {
				this.cancel();
				return;
			}
			Location centerLoc = loc.clone().add(0, 1, 0);
			EntityUtils.teleportStack(mLauncher, loc);
			world.playSound(centerLoc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.HOSTILE, (float) mRadius / 7,
				(float) (0.5 + FastUtils.RANDOM.nextInt(150) / 100.0));
			new PartialParticle(Particle.CRIT, centerLoc, 10, 1, 1, 1, 0.01).spawnAsEntityActive(mLauncher);
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mLauncher, KNOCK_AWAY_SPEEDUP_SOURCE,
				new PercentSpeed(mSpeedupDuration, mSpeedupPotency, KNOCK_AWAY_SPEEDUP_SOURCE));
		};

		Runnable animLoop2 = () -> {
			if (mLauncher.isDead() || !mLauncher.isValid()) {
				this.cancel();
				return;
			}
			Location lloc = mLauncher.getLocation();
			double precision = FastUtils.RANDOM.nextInt(50) + 100;
			double increment = (2 * Math.PI) / precision;
			Location particleLoc = new Location(lloc.getWorld(), 0, lloc.getY() + 1.5, 0);
			double rad = (double) (mRadius * mWidth) / 5;
			for (int j = 0; j < precision; j++) {
				double angle = j * increment;
				particleLoc.setX(lloc.getX() + (rad * FastUtils.cos(angle)));
				particleLoc.setZ(lloc.getZ() + (rad * FastUtils.sin(angle)));
				particleLoc.setY(lloc.getY() + 1.5);
				new PartialParticle(Particle.CRIT, particleLoc, 1, 0.02, 1.5 * rad, 0.02, 0).spawnAsEntityActive(mLauncher);
			}
			if (mWidth == 0) {
				world.playSound(particleLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, (float) mRadius / 7, 0.77F);
				world.playSound(particleLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, (float) mRadius / 7, 0.5F);
				world.playSound(particleLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, (float) mRadius / 7, 0.65F);
			}
			mWidth++;
		};

		for (int i = 0; i < mTime; i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, animLoop, i);
		}
		for (int i = 0; i < 6; i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, animLoop2, i + mTime);
		}
	}
}
