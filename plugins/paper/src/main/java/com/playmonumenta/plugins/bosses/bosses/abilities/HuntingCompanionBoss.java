package com.playmonumenta.plugins.bosses.bosses.abilities;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.scout.HuntingCompanion;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.scout.HuntingCompanionCS;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NmsUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class HuntingCompanionBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_huntingcompanion";

	private Mob mFox;
	private Player mPlayer;
	private HuntingCompanionCS mCosmetic;
	private boolean mLevelTwo = false;
	private double mDamage = 0;
	private double mPounceDamage = 0;
	private double mPounceRadius = 0;
	private double mHealingPercent = 0;
	private int mPounceCooldown = 0;
	private double mRange = 0;

	private int mLastPounceCast = Bukkit.getCurrentTick();
	private boolean mIsPouncing = false;

	@SuppressWarnings("NullAway.Init")
	public HuntingCompanionBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		boss.setInvulnerable(true);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), 64, null);
	}

	// Initializer, none of the methods (except for the single event method) can be called without spawn initializer
	public void spawn(Player player, boolean isLevelTwo, double damage, double pounceDamage, double pounceRadius,
					  int pounceCooldown, double healing, double range, HuntingCompanionCS cosmetic) {
		mFox = (Mob) mBoss;
		mPlayer = player;
		mLevelTwo = isLevelTwo;
		mDamage = damage;
		mPounceDamage = pounceDamage;
		mPounceRadius = pounceRadius;
		mPounceCooldown = pounceCooldown;
		mHealingPercent = healing;
		mRange = range;
		mCosmetic = cosmetic;

		if (mDamage > 0) {
			try {
				NmsUtils.getVersionAdapter().setHuntingCompanion((Creature) mFox, target -> {
					DamageUtils.damage(mPlayer, target, DamageEvent.DamageType.PROJECTILE_SKILL, mDamage, ClassAbility.HUNTING_COMPANION, true);

					mCosmetic.onAttack(mFox.getWorld(), mFox.getLocation(), mPlayer, mFox);
				}, 3);
			} catch (Exception e) {
				MMLog.warning("Catch an exception while creating " + mFox.getName() + ". Reason: " + e.getMessage());
				e.printStackTrace();
			}
		}

		mFox.teleport(mFox.getLocation().setDirection(mPlayer.getEyeLocation().getDirection().normalize()));

		Attribute attribute = Attribute.GENERIC_MOVEMENT_SPEED;

		EntityUtils.setAttributeBase(mFox, attribute,
			CharmManager.calculateFlatAndPercentValue(mPlayer, HuntingCompanion.CHARM_SPEED,
				EntityUtils.getAttributeBaseOrDefault(mFox, attribute, 0)));
	}

	public void cosmeticTick() {
		LivingEntity target = getTarget();

		mCosmetic.tick(mFox, mPlayer, target, Bukkit.getCurrentTick());
		if (mIsPouncing) {
			mCosmetic.pounceTick(mFox, mPlayer, target, Bukkit.getServer().getCurrentTick());
		}
	}

	public boolean tick() {
		if (mFox.isDead()
			|| !mFox.isValid()
			|| !mPlayer.getWorld().equals(mFox.getWorld())) {
			return false;
		}

		if (mFox.getFireTicks() > 0) {
			mFox.setFireTicks(0);
		}

		boolean isAfk = HuntingCompanion.isAFK(mPlayer);

		LivingEntity target = getTarget();

		// If the companion is targeting a mob, check if it should drop the target
		if (target != null) {
			boolean isTooFar = (!mIsPouncing && isTooFar(target, mPlayer, mRange));
			if (isTooFar || isAfk || !target.isValid() || target.isDead()) {
				mFox.setTarget(null);
			}
			return true;
		}

		// Otherwise, find a new one if the player isn't afk
		if (!isAfk) {
			LivingEntity newTarget = HuntingCompanion.findNearestNonTargetedMob(mFox, mPlayer, mRange);
			if (newTarget != null) {
				mFox.setTarget(newTarget);
				return true;
			}
		}

		// If it doesn't have a target nor can find one, follow the player
		double distanceSquared = mFox.getLocation().distanceSquared(mPlayer.getLocation());
		if (distanceSquared > 16 * 16) {
			teleportCompanion();
		} else if (distanceSquared > 4 * 4) {
			mFox.getPathfinder().moveTo(mPlayer.getLocation(), distanceSquared > 6 * 6 ? 1 : 0.66);
		} else {
			mFox.getPathfinder().stopPathfinding();
		}

		return true;
	}

	// If a stagger occurs, check the foxes to see if any of them CAN pounce.
	public boolean pounce(LivingEntity target, boolean attack) {
		if (Bukkit.getCurrentTick() - mLastPounceCast < mPounceCooldown && isApplicableTarget(target)) {
			return false;
		}
		mLastPounceCast = Bukkit.getCurrentTick();
		mIsPouncing = true;

		World world = mFox.getWorld();
		Location loc = mFox.getLocation();

		Vector velDir = target.getLocation().subtract(loc).toVector().multiply(0.1).setY(0.8);

		mCosmetic.onAggro(world, loc, mPlayer, mFox);
		mFox.teleport(mFox.getLocation().setDirection(velDir));

		mCosmetic.onJump(world, loc, mPlayer, mFox, target);
		mFox.setTarget(target);

		mFox.setVelocity(velDir);

		final boolean canAttack = attack && mPounceDamage > 0;

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				// If the companion doesn't attack in 5s, just do it
				if (mT > 100 || (mFox.isOnGround() && !canAttack)) {
					if (canAttack) {
						pounceAttack();
					}
					this.cancel();
					return;
				}

				// Delay by 1s so it can do the jump effect
				if (mT > Constants.TICKS_PER_SECOND && canAttack) {
					BoundingBox hitbox = mFox.getBoundingBox().expand(0.25);

					boolean canPounce = EntityUtils.getNearbyMobs(mFox.getLocation(), 5)
						.stream()
						.anyMatch(e -> hitbox.overlaps(e.getBoundingBox()));

					if (canPounce) {
						pounceAttack();
						this.cancel();
						return;
					}
				}

				if (!(mFox instanceof Axolotl)) {
					adjustMidairVelocity(mFox, target);
				}

				mT++;
			}

		}.runTaskTimer(mPlugin, 0, 1);

		return true;
	}

	public void teleportCompanion() {
		mCosmetic.onTeleport(mPlayer.getWorld(), mFox.getLocation(), mPlayer, mFox);
		Location tpLoc = LocationUtils.randomLocationInDonut(mPlayer.getLocation(), 1, 2);

		mFox.teleport(tpLoc);
		mFox.setTarget(null);

		mCosmetic.onTeleport(mPlayer.getWorld(), mFox.getLocation(), mPlayer, mFox);
		mCosmetic.onSummon(mPlayer.getWorld(), mFox.getLocation(), mPlayer, mFox);
	}

	public Mob getBoss() {
		return mFox;
	}

	public @Nullable LivingEntity getTarget() {
		return mFox != null ? mFox.getTarget() : null;
	}

	public void remove() {
		mFox.remove();
	}

	// Private methods

	private void pounceAttack() {
		mIsPouncing = false;

		World world = mFox.getWorld();
		Location loc = LocationUtils.getHalfHeightLocation(mFox);

		mCosmetic.onAttack(world, loc, mPlayer, mFox);
		mCosmetic.onPounce(world, loc, mPlayer, mFox, 3);

		List<LivingEntity> mobs = new Hitbox.SphereHitbox(loc, mPounceRadius).getHitMobs();

		for (LivingEntity e : mobs) {
			DamageUtils.damage(mPlayer, e, DamageEvent.DamageType.PROJECTILE_SKILL, mPounceDamage, ClassAbility.HUNTING_COMPANION, true);
		}

		if (mLevelTwo && !mobs.isEmpty()) {
			int duration = 20;
			@Nullable Effect hcHealing = mPlugin.mEffectManager.getActiveEffect(mPlayer, "HuntingCompanionHealing");

			if (hcHealing != null && hcHealing.getDuration() > 0) {
				duration = Math.min(duration + hcHealing.getDuration(), 3 * Constants.TICKS_PER_SECOND);
			}
			mPlugin.mEffectManager.addEffect(mPlayer, "HuntingCompanionHealing",
				new CustomRegeneration(duration, EntityUtils.getMaxHealth(mPlayer) * mHealingPercent * 5 / 20,
					5, mPlayer, true, mPlugin));
		}
	}

	// Events

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		Entity target = event.getTarget();

		if (!isApplicableTarget(target) || isTooFar(target, mPlayer, mRange)) {
			event.setCancelled(true);
		}
	}

	// Static util methods

	private static boolean isApplicableTarget(Entity entity) {
		return entity instanceof LivingEntity mob
			&& EntityUtils.isHostileMob(mob)
			&& !mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)
			&& !DamageUtils.isImmuneToDamage(mob, DamageEvent.DamageType.PROJECTILE_SKILL);
	}

	private static boolean isTooFar(Entity target, Player player, double range) {
		return target instanceof LivingEntity entity
			&& player != null
			&& entity.getLocation().distanceSquared(player.getLocation()) > range * range;
	}


	private static void adjustMidairVelocity(Mob summon, LivingEntity target) {
		Vector targetDir = target.getLocation().subtract(summon.getLocation()).toVector().setY(0).normalize();
		if (!Double.isFinite(targetDir.getX())) {
			targetDir = new Vector(0, summon.getLocation().getY() > target.getLocation().getY() ? -1 : 1, 0);
		}
		Vector originalVelocity = summon.getVelocity();
		double scale = 1;
		Vector newVelocity = new Vector();
		newVelocity.setX((originalVelocity.getX() * 20 + targetDir.getX() * scale) / 20);
		// Use the original mob's vertical velocity, so it doesn't somehow fall faster than gravity
		newVelocity.setY(originalVelocity.getY());
		newVelocity.setZ((originalVelocity.getZ() * 20 + targetDir.getZ() * scale) / 20);
		summon.setVelocity(newVelocity);
	}
}
