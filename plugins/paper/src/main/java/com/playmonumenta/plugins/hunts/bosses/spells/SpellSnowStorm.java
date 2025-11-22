package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.AlocAcoc;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellSnowStorm extends Spell {
	public static final int COOLDOWN = 2 * 20;
	private static final int DURATION = 7 * 20;
	private static final int DELTA = 5;
	private static final float SPEED = 1.5f;
	private static final int DAMAGE = 70;

	private final LivingEntity mBoss;
	private final PassivePolarAura mAura;
	private final Plugin mPlugin;
	private final AlocAcoc mAlocAcoc;
	private int mBulletCount;

	public SpellSnowStorm(LivingEntity boss, Plugin plugin, AlocAcoc alocAcoc, PassivePolarAura aura) {
		mBoss = boss;
		mPlugin = plugin;
		mAlocAcoc = alocAcoc;
		mAura = aura;
	}

	@Override
	public boolean canRun() {
		return mAlocAcoc.canRunSpell(this);
	}

	@Override
	public void run() {
		List<Location> landLocations = new ArrayList<>();
		int count = mAura.mInnerAura ? 8 : 10;
		for (int radian = 0; radian < count; radian++) {
			Vector vec = new Vector(FastUtils.cos((radian * 2 * Math.PI) / count) * (mAura.mCurrentRadius * 0.7) + FastUtils.randomFloatInRange(0, DELTA), 5, FastUtils.sin((radian * 2 * Math.PI) / count) * (mAura.mCurrentRadius * 0.7) + FastUtils.randomFloatInRange(0, DELTA));
			Location mLandingLocation = mBoss.getLocation().clone().add(vec);
			mLandingLocation = LocationUtils.fallToGround(mLandingLocation, 0);
			landLocations.add(mLandingLocation);
		}

		// 2.5 or 3.5
		double radius = 1 + 2.5 * (mAura.mCurrentRadius / PassivePolarAura.OUTER_RADIUS);
		for (Location loc : landLocations) {
			mBulletCount++;
			bullet(loc, radius);
		}
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks == 0) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_DROWNED_SHOOT, 2, 1);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 2, 0.5f);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ITEM_BUCKET_EMPTY_POWDER_SNOW, 2, 0.6f);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_HORSE_BREATHE, 2, 2);
				}
				if (mBulletCount == 0 || mTicks > 32) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	private void bullet(Location target, double radius) {
		Location bulletLoc = mBoss.getLocation().clone();
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				Vector vec;
				if (mTicks < 10) {
					vec = new Vector(0, 2.5, 0);
				} else {
					vec = LocationUtils.getDirectionTo(target, bulletLoc).multiply(SPEED);
					if (mTicks <= 20) {
						vec.add(new Vector(0, -0.15 * (mTicks - 9) + 1.5, 0));
					}
				}
				bulletLoc.add(vec);
				new PartialParticle(Particle.CLOUD, bulletLoc, 10).delta(0.1).spawnAsBoss();

				//Delaying the circle so people have to react a bit more
				if (mTicks >= 5 && mTicks % 3 == 0) {
					new PPCircle(Particle.REDSTONE, target, radius - 0.5f).ringMode(false).count((int) (radius * radius * 5)).data(new Particle.DustOptions(Color.fromRGB(80, 128, 150), 1.2f)).spawnAsBoss();
					new PPCircle(Particle.END_ROD, target.clone().add(0, 0.05, 0), radius).ringMode(true).count((int) (radius * 6)).spawnAsBoss();
				}
				if (LocationUtils.getVectorTo(bulletLoc, target).length() <= 1) {
					new PPExplosion(Particle.SNOWFLAKE, target).spawnAsBoss();
					for (Player p : PlayerUtils.playersInRange(target, radius, true, true)) {
						BossUtils.blockableDamage(mBoss, p, DamageEvent.DamageType.BLAST, DAMAGE, "Snow Storm", target);
						mAura.addFrostbite(p, 0.175f);
						p.playSound(p, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.HOSTILE, 2, 0.62f);
					}
					mBulletCount--;
					mBoss.getWorld().playSound(bulletLoc, Sound.BLOCK_SNOW_STEP, SoundCategory.HOSTILE, 1.5f, 1.6f);
					mBoss.getWorld().playSound(bulletLoc, Sound.ENTITY_HORSE_SADDLE, SoundCategory.HOSTILE, 1.5f, 0.97f);
					this.cancel();
				}
				if (mBoss.isDead() || mTicks >= DURATION) {
					mBulletCount--;
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}
}
