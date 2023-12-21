package com.playmonumenta.plugins.depths.bosses.spells.callicarpa;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class LeafNova extends Spell {

	public static final int DURATION = 4 * 20;
	public static final int DURATION_A15_DECREASE = 20;
	public static final int COOLDOWN = 10 * 20;
	public static final int MAX_RANGE = 10;
	public static final Particle.DustOptions LEAF_COLOR = new Particle.DustOptions(Color.fromRGB(14, 123, 8), 1.0f);
	public static final Particle.DustOptions PROJ_COLOR = new Particle.DustOptions(Color.fromRGB(79, 59, 18), 1.0f);
	public static final int RADIUS = 5;
	public static final int NOVA_DAMAGE = 50;
	public static final int PROJECTILE_DAMAGE = 25;
	public static final Sound CHARGE_SOUND = Sound.BLOCK_BAMBOO_BREAK;

	private final LivingEntity mBoss;
	private final int mFinalCooldown;
	private final int mFinalDuration;

	public LeafNova(LivingEntity boss, @Nullable DepthsParty party) {
		mBoss = boss;
		mFinalCooldown = DepthsParty.getAscensionEightCooldown(COOLDOWN, party);
		mFinalDuration = getDuration(party);
	}

	@Override
	public void run() {
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, mFinalDuration, 20));
		new BukkitRunnable() {
			int mTicks = 0;
			Location mOldLoc = mBoss.getLocation();
			double mCurrentRadius = RADIUS;

			@Override public void run() {
				Location mBossLoc = mBoss.getLocation();
				// Spiraling Bullets
				for (int i = 0; i < 4; i++) {
					Location mLoc = new Location(mBoss.getWorld(), mBossLoc.getX() + calculateX(mTicks + 1, i * Math.PI / 2.0), mBossLoc.getY() + 0.25, mBossLoc.getZ() + calculateZ(mTicks + 1, i * Math.PI / 2.0));
					Vector vec = new Vector(calculateX(mTicks - 1, i * Math.PI / 2.0) - calculateX(mTicks, i * Math.PI / 2.0), 0, calculateZ(mTicks - 1, i * Math.PI / 2.0) - calculateZ(mTicks, i * Math.PI / 2.0));
					new PartialParticle(Particle.REDSTONE, mLoc, 6).data(PROJ_COLOR).spawnAsBoss();
					checkForCollisions(BoundingBox.of(mLoc, 0.25, 0.5, 0.25), vec);
				}

				// Nova
				new PartialParticle(Particle.REDSTONE, mBossLoc.clone().add(0, 1, 0), 1, RADIUS / 2.0, RADIUS / 2.0, RADIUS / 2.0, LEAF_COLOR).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.COMPOSTER, mBossLoc.clone().add(0, 1, 0), 1, RADIUS / 2.0, RADIUS / 2.0, RADIUS / 2.0, 0.05).spawnAsEntityActive(mBoss);
				if (mTicks <= mFinalDuration - 5) {
					mBoss.getWorld().playSound(mBoss.getLocation(), CHARGE_SOUND, SoundCategory.HOSTILE, 1f, 0.25f + ((float) mTicks / 100));
				}
				for (double i = 0; i < 360; i += 30) {
					double radian1 = Math.toRadians(i);
					mBossLoc.add(FastUtils.cos(radian1) * mCurrentRadius, 0, FastUtils.sin(radian1) * mCurrentRadius);
					new PartialParticle(Particle.REDSTONE, mBossLoc, 1, 0.25, 0.25, 0.25, LEAF_COLOR).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.REDSTONE, mBossLoc.clone().add(0, 2, 0), 1, 0.25, 0.25, 0.25, LEAF_COLOR).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.COMPOSTER, mBossLoc, 1, 0.25, 0.25, 0.25, 0.1).spawnAsEntityActive(mBoss);
					mBossLoc.subtract(FastUtils.cos(radian1) * mCurrentRadius, 0, FastUtils.sin(radian1) * mCurrentRadius);
				}
				mCurrentRadius -= (RADIUS / ((double) mFinalDuration));
				if (mCurrentRadius <= 0) {
					dealDamageAction(mBossLoc);
					new BukkitRunnable() {
						final Location mLoc = mBoss.getLocation();
						double mBurstRadius = 0;

						@Override
						public void run() {
							for (int j = 0; j < 2; j++) {
								mBurstRadius += 1.5;
								for (double i = 0; i < 360; i += 15) {
									double radian1 = Math.toRadians(i);
									mLoc.add(FastUtils.cos(radian1) * mBurstRadius, 0, FastUtils.sin(radian1) * mBurstRadius);
									new PartialParticle(Particle.COMPOSTER, mLoc, 1, 0.1, 0.1, 0.1, 0.3).spawnAsEntityActive(mBoss);
									new PartialParticle(Particle.SLIME, mLoc, 2, 0.25, 0.25, 0.25, 0.1).spawnAsEntityActive(mBoss);
									mLoc.subtract(FastUtils.cos(radian1) * mBurstRadius, 0, FastUtils.sin(radian1) * mBurstRadius);
								}
							}
							if (mBurstRadius >= RADIUS) {
								this.cancel();
							}
						}

					}.runTaskTimer(Plugin.getInstance(), 0, 1);
				}
				//end condition
				if (mTicks >= mFinalDuration) {
					this.cancel();
				}
				mTicks += 1;
				mOldLoc = mBoss.getLocation();
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private double calculateX(double t, double angle) {
		return 1 / 10.0 * t * FastUtils.cos(angle + t / 8.0);
	}

	private double calculateZ(int t, double angle) {
		return 1 / 10.0 * t * FastUtils.sin(angle + t / 8.0);
	}

	@Override
	public int cooldownTicks() {
		return mFinalCooldown;
	}

	private void checkForCollisions(BoundingBox box, Vector vec) {
		BoundingBox mBox = box.clone();
		for (int j = 0; j < 2; j++) {
			mBox.shift(vec.clone().multiply(0.5));
			for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), MAX_RANGE, true)) {
				if (player.getBoundingBox().overlaps(mBox)) {
					BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MAGIC, PROJECTILE_DAMAGE, "Leaf Nova", mBoss.getLocation());
					new PPExplosion(Particle.SLIME, box.clone().getCenter().toLocation(mBoss.getWorld()));
					World world = mBoss.getWorld();
					world.playSound(mBox.getCenter().toLocation(world), Sound.BLOCK_COMPOSTER_FILL, SoundCategory.HOSTILE, 1.0f, 1.0f);
				}
			}
		}
	}

	// Causes damage for the nova part of leafNova.
	private void dealDamageAction(Location loc) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2.0f, 0.65F);
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), RADIUS, true)) {
			DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, NOVA_DAMAGE, null, false, true, "Trembling Roots");
			player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 20 * 6, 4));
		}
	}

	private int getDuration(@Nullable DepthsParty party) {
		int duration = DURATION;
		if (party != null && party.getAscension() >= 15) {
			duration -= DURATION_A15_DECREASE;
		}
		return duration;
	}
}
