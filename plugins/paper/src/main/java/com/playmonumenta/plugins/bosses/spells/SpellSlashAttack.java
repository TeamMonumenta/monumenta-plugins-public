package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellSlashAttack extends Spell {

	public final Plugin mPlugin;
	public final LivingEntity mBoss;
	public final int mCooldown;
	public final double mDamage;
	public final int mTelegraphDuration;
	public final double mRadius;
	public final double mMinAngle;
	public final double mMaxAngle;
	public final String mAttackName;
	public final int mRings;
	public final double mStartAngle;
	public final double mEndAngle;
	public final double mSpacing;
	public final Color mStartColor;
	public final Color mMidColor;
	public final Color mEndColor;
	public final boolean mXSlash;
	public final boolean mHorizontalColor;
	public final boolean mKnockAway;
	public final double mKbrEffectiveness;
	public final boolean mFollowCaster;
	public final double mHitboxSize;
	public final double mForcedParticleSize;
	public final DamageEvent.DamageType mDamageType;

	public Vector mKnockback;

	public double mCurrAngleProgress = 0;
	public boolean mSwitchedColor = false;
	public Map<Player, Integer> mLastKnockbackTick = new HashMap<>();

	public static final int KNOCKBACK_IFRAMES = 5;

	public SpellSlashAttack(Plugin plugin, LivingEntity boss, int cooldown, double damage, int telegraphDuration,
							double radius, double minAngle, double maxAngle, String attackName, int rings, double startAngle,
							double endAngle, double spacing, String startColorHex, String midColorHex, String endColorHex,
							String xSlash, String horizontalColor, Vector knockback, String knockAway, double kbrEffectiveness,
							String followCaster, double hitboxSize, double forcedParticleSize, DamageEvent.DamageType damageType) {
		mPlugin = plugin;
		mBoss = boss;
		mCooldown = cooldown;
		mDamage = damage;
		mTelegraphDuration = telegraphDuration;
		mRadius = radius;
		mMinAngle = minAngle;
		mMaxAngle = maxAngle;
		mAttackName = attackName;
		mRings = rings;
		mStartAngle = startAngle;
		mEndAngle = endAngle;
		mSpacing = spacing;
		mStartColor = Color.fromRGB(Integer.parseInt(startColorHex, 16));
		mMidColor = Color.fromRGB(Integer.parseInt(midColorHex, 16));
		mEndColor = Color.fromRGB(Integer.parseInt(endColorHex, 16));
		mXSlash = Boolean.parseBoolean(xSlash);
		mHorizontalColor = Boolean.parseBoolean(horizontalColor);
		mKnockback = knockback;
		mKnockAway = Boolean.parseBoolean(knockAway);
		mKbrEffectiveness = kbrEffectiveness;
		mFollowCaster = Boolean.parseBoolean(followCaster);
		mHitboxSize = hitboxSize;
		mForcedParticleSize = forcedParticleSize;
		mDamageType = damageType;
	}

	@Override
	public void run() {
		double selectedAngle = Math.random() * (mMaxAngle - mMinAngle) + mMinAngle;

		if (mTelegraphDuration == 0) {
			// Instantly attack
			doSlash(selectedAngle);
		} else {
			// Cast telegraph first, then attack
			telegraphSLash(selectedAngle);
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					if (EntityUtils.shouldCancelSpells(mBoss)) {
						this.cancel();
						return;
					}
					if (mTicks >= mTelegraphDuration) {
						doSlash(selectedAngle);
						this.cancel();
					}
					mTicks++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

	private void doSlash(double selectedAngle) {
		Location startLoc = mBoss.getLocation().clone();
		startLoc.add(0, mBoss.getHeight() / 2, 0);
		double maxAngleProgress = Math.abs(mEndAngle - mStartAngle) / 2;
		mCurrAngleProgress = 0;
		mSwitchedColor = false;

		ParticleUtils.drawHalfArc(startLoc, mRadius, selectedAngle, mStartAngle, mEndAngle, mRings, mSpacing,
				(Location l, int ring) -> {
					Location finalLoc = l.clone();
					if (mFollowCaster) {
						finalLoc.add(mBoss.getLocation().toVector().subtract(startLoc.toVector()));
					}
					Particle.DustOptions data = calculateColorProgress(ring, maxAngleProgress);
					new PartialParticle(Particle.REDSTONE, finalLoc, 1).extra(0)
							.data(data).minimumMultiplier(false).spawnAsEntityActive(mBoss);
					Hitbox hitbox = new Hitbox.AABBHitbox(mBoss.getWorld(), BoundingBox.of(finalLoc, mHitboxSize, mHitboxSize, mHitboxSize));
					List<Player> targets = hitbox.getHitPlayers(true);
					for (Player target : targets) {
						DamageUtils.damage(mBoss, target, mDamageType, mDamage, null, false, false, mAttackName);
						applyKnockback(target);
					}
					mCurrAngleProgress += 5 / (double) mRings;
				}
		);
		if (mXSlash) {
			ParticleUtils.drawHalfArc(startLoc, mRadius, 360 - selectedAngle, mStartAngle, mEndAngle, mRings, mSpacing,
					(Location l, int ring) -> {
						Location finalLoc = l.clone();
						if (mFollowCaster) {
							finalLoc.add(mBoss.getLocation().toVector().subtract(startLoc.toVector()));
						}
						Particle.DustOptions data = calculateColorProgress(ring, maxAngleProgress);
						new PartialParticle(Particle.REDSTONE, finalLoc, 1).extra(0)
								.data(data).minimumMultiplier(false).spawnAsEntityActive(mBoss);
						Hitbox hitbox = new Hitbox.AABBHitbox(mBoss.getWorld(), BoundingBox.of(finalLoc, mHitboxSize, mHitboxSize, mHitboxSize));
						List<Player> targets = hitbox.getHitPlayers(true);
						for (Player target : targets) {
							DamageUtils.damage(mBoss, target, mDamageType, mDamage, null, false, false, mAttackName);
							applyKnockback(target);
						}
					}
			);
		}
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
	}

	private void applyKnockback(Player target) {
		if (mKnockback.lengthSquared() == 0) {
			return;
		}

		// Limit knockback to once every 5 ticks, otherwise every single dot that hit the player will
		// apply the knockback to the player
		if (mLastKnockbackTick.containsKey(target) && Bukkit.getCurrentTick() - mLastKnockbackTick.get(target) < KNOCKBACK_IFRAMES) {
			mLastKnockbackTick.replace(target, Bukkit.getCurrentTick());
			return;
		}

		double kbMultiplier = 1 - EntityUtils.getAttributeOrDefault(target, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0) * mKbrEffectiveness;
		if (mKnockAway) {
			MovementUtils.knockAwayRealistic(mBoss.getLocation(), target, (float) (mKnockback.getX() * kbMultiplier), (float) (mKnockback.getY() * kbMultiplier), false);
		} else {
			target.setVelocity(mKnockback.multiply(kbMultiplier));
		}
		mLastKnockbackTick.put(target, Bukkit.getCurrentTick());
	}

	private void telegraphSLash(double selectedAngle) {
		Location startLoc = mBoss.getLocation().clone();
		startLoc.add(0, mBoss.getHeight() / 2, 0);
		ParticleUtils.drawHalfArc(startLoc, mRadius, selectedAngle, mStartAngle, mEndAngle, mRings, mSpacing,
				(Location l, int ring) -> {
					new PartialParticle(Particle.REDSTONE, l, 1).extra(0)
							.data(new Particle.DustOptions(Color.WHITE, (mForcedParticleSize > 0) ? (float) mForcedParticleSize : 0.6f + (ring * 0.1f)))
							.minimumMultiplier(false).spawnAsEntityActive(mBoss);
				}
		);
		if (mXSlash) {
			ParticleUtils.drawHalfArc(startLoc, mRadius, 360 - selectedAngle, mStartAngle, mEndAngle, mRings, mSpacing,
					(Location l, int ring) -> {
						new PartialParticle(Particle.REDSTONE, l, 1).extra(0)
								.data(new Particle.DustOptions(Color.WHITE, (mForcedParticleSize > 0) ? (float) mForcedParticleSize : 0.6f + (ring * 0.1f)))
								.minimumMultiplier(false).spawnAsEntityActive(mBoss);
					}
			);
		}
	}

	Particle.DustOptions calculateColorProgress(int ring, double maxAngleProgress) {
		Particle.DustOptions data;
		if (!mHorizontalColor) {
			int halfRings = mRings / 2;
			if (ring < halfRings) {
				// Transition from start to mid
				data = new Particle.DustOptions(
						ParticleUtils.getTransition(mStartColor, mMidColor, Math.min(ring / (double) halfRings, 1)),
						(mForcedParticleSize > 0) ? (float) mForcedParticleSize : 0.6f + (ring * 0.1f)
				);
			} else {
				// Transition from mid to end
				data = new Particle.DustOptions(
						ParticleUtils.getTransition(mMidColor, mEndColor, Math.min((ring - halfRings) / (double) halfRings, 1)),
						(mForcedParticleSize > 0) ? (float) mForcedParticleSize : 0.6f + (ring * 0.1f)
				);
			}
		} else {
			double progress = mCurrAngleProgress / maxAngleProgress;
			if (!mSwitchedColor) {
				data = new Particle.DustOptions(
						ParticleUtils.getTransition(mStartColor, mMidColor, Math.min(progress, 1)),
						(mForcedParticleSize > 0) ? (float) mForcedParticleSize : 0.6f + (ring * 0.1f)
				);
				if (mCurrAngleProgress >= maxAngleProgress) {
					mCurrAngleProgress = 0;
					mSwitchedColor = true;
				}
			} else {
				data = new Particle.DustOptions(
						ParticleUtils.getTransition(mMidColor, mEndColor, Math.min(progress, 1)),
						(mForcedParticleSize > 0) ? (float) mForcedParticleSize : 0.6f + (ring * 0.1f)
				);
			}
		}
		return data;
	}
}
