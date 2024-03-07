package com.playmonumenta.plugins.abilities.warrior.guardian;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.guardian.ShieldWallCS;
import com.playmonumenta.plugins.effects.OnHitTimerEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ShieldWall extends Ability implements AbilityWithDuration {

	private static final int SHIELD_WALL_1_DURATION = 6 * 20;
	private static final int SHIELD_WALL_2_DURATION = 10 * 20;
	private static final int SHIELD_WALL_DAMAGE = 3;
	private static final int SHIELD_WALL_1_COOLDOWN = 20 * 25;
	private static final int SHIELD_WALL_2_COOLDOWN = 20 * 18;
	private static final int SHIELD_WALL_ANGLE = 180;
	private static final float SHIELD_WALL_KNOCKBACK = 0.3f;
	private static final double SHIELD_WALL_RADIUS = 4.0;
	private static final int SHIELD_WALL_HEIGHT = 5;
	private static final String ON_HIT_EFFECT = "ShieldWallHitCooldownEffect";

	public static final String CHARM_DURATION = "Shield Wall Duration";
	public static final String CHARM_DAMAGE = "Shield Wall Damage";
	public static final String CHARM_COOLDOWN = "Shield Wall Cooldown";
	public static final String CHARM_ANGLE = "Shield Wall Angle";
	public static final String CHARM_KNOCKBACK = "Shield Wall Knockback";
	public static final String CHARM_HEIGHT = "Shield Wall Height";
	public static final String CHARM_RADIUS = "Shield Wall Radius";

	public static final AbilityInfo<ShieldWall> INFO =
			new AbilityInfo<>(ShieldWall.class, "Shield Wall", ShieldWall::new)
					.linkedSpell(ClassAbility.SHIELD_WALL)
					.scoreboardId("ShieldWall")
					.shorthandName("SW")
					.descriptions(
							String.format("Press the swap key while holding a shield in either hand to create a %s degree arc of particles from 1 block below to %s blocks above the user's location and with a %s block radius in front of the user. " +
											"Enemies that pass through the wall are dealt %s melee damage and knocked back. The wall also blocks all enemy projectiles such as arrows or fireballs. The wall lasts %s seconds. The wall moves along with the user. Triggering again while active keeps the wall at the location where it is for the remainder of the duration. Cooldown: %ss.",
									SHIELD_WALL_ANGLE,
									SHIELD_WALL_HEIGHT,
									(int) SHIELD_WALL_RADIUS,
									SHIELD_WALL_DAMAGE,
									StringUtils.ticksToSeconds(SHIELD_WALL_1_DURATION),
									StringUtils.ticksToSeconds(SHIELD_WALL_1_COOLDOWN)
							),
							String.format("The shield lasts %s seconds instead. Cooldown: %ss.",
									StringUtils.ticksToSeconds(SHIELD_WALL_2_DURATION),
									StringUtils.ticksToSeconds(SHIELD_WALL_2_COOLDOWN)
							)
					)
					.simpleDescription("Deploy a wall that can block projectiles and mobs from entering.")
					.cooldown(SHIELD_WALL_1_COOLDOWN, SHIELD_WALL_2_COOLDOWN, CHARM_COOLDOWN)
					.addTrigger(new AbilityTriggerInfo<>("cast", "cast", shieldWall -> shieldWall.cast(false), new AbilityTrigger(AbilityTrigger.Key.SWAP),
							new AbilityTriggerInfo.TriggerRestriction("holding a shield in either hand",
									player -> player.getInventory().getItemInMainHand().getType() == Material.SHIELD || player.getInventory().getItemInOffHand().getType() == Material.SHIELD)))
					.addTrigger(new AbilityTriggerInfo<>("caststationary", "cast stationary", shieldWall -> shieldWall.cast(true), new AbilityTrigger(AbilityTrigger.Key.SWAP).enabled(false),
							new AbilityTriggerInfo.TriggerRestriction("holding a shield in either hand",
								player -> player.getInventory().getItemInMainHand().getType() == Material.SHIELD || player.getInventory().getItemInOffHand().getType() == Material.SHIELD)))
					.displayItem(Material.STONE_BRICK_WALL);

	private final int mDuration;
	private final int mHeight;
	private final float mKnockback;
	private final double mDamage;
	private final double mAngle;
	private final double mRadius;
	private final ShieldWallCS mCosmetic;

	private int mCurrDuration = -1;

	private boolean mDeposited = false;

	public ShieldWall(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, (isLevelOne() ? SHIELD_WALL_1_DURATION : SHIELD_WALL_2_DURATION));
		mHeight = SHIELD_WALL_HEIGHT + (int) CharmManager.getLevel(mPlayer, CHARM_HEIGHT);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, SHIELD_WALL_KNOCKBACK);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, SHIELD_WALL_DAMAGE);
		mAngle = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ANGLE, SHIELD_WALL_ANGLE);
		mRadius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RADIUS, SHIELD_WALL_RADIUS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ShieldWallCS());
	}

	public boolean cast(boolean deposit) {
		if (isOnCooldown()) {
			mDeposited = true;
			return true;
		}
		mDeposited = deposit;

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		mCosmetic.shieldStartEffect(world, mPlayer, loc, SHIELD_WALL_RADIUS);
		putOnCooldown();

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		mCurrDuration = 0;
		cancelOnDeath(new BukkitRunnable() {
			final Set<LivingEntity> mMobsAlreadyHit = new HashSet<>();
			Location mLoc = loc;

			@Override
			public void run() {
				mCurrDuration++;
				Vector vec;

				if (!mDeposited) {
					double lastY = mLoc.getY();
					mLoc = mPlayer.getLocation();
					if (!PlayerUtils.isOnGround(mPlayer)) {
						mLoc.setY(lastY);
					}
				}

				Hitbox hitbox = Hitbox.approximateHollowCylinderSegment(mLoc.clone().add(0, -1, 0), mHeight + 1, mRadius - 0.4, mRadius + 0.4, Math.toRadians(mAngle) / 2);

				for (double degree = 0; degree < mAngle; degree += 9) {
					double radian1 = Math.toRadians(degree - 0.5 * mAngle);
					vec = new Vector(-FastUtils.sin(radian1) * mRadius, -1, FastUtils.cos(radian1) * mRadius);
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());
					Location l = mLoc.clone().add(vec);
					for (double y = 0; y < mHeight + 1; y += 0.75) {
						l.add(0, 1, 0);
						mCosmetic.shieldWallDot(mPlayer, l, degree, mAngle, y, mHeight);
					}
				}

				List<Projectile> projectiles = hitbox.getHitEntitiesByClass(Projectile.class);
				for (Projectile proj : projectiles) {
					if (proj.getShooter() instanceof LivingEntity shooter && !(shooter instanceof Player)) {
						proj.remove();
						mCosmetic.shieldOnBlock(world, proj.getLocation(), mPlayer);
					}
				}

				List<LivingEntity> entities = hitbox.getHitMobs();
				for (LivingEntity le : entities) {
					boolean enteredWall = !mMobsAlreadyHit.contains(le);
					if (enteredWall) {
						DamageUtils.damage(mPlayer, le, new DamageEvent.Metadata(DamageEvent.DamageType.MELEE_SKILL, mInfo.getLinkedSpell(), playerItemStats), mDamage, false, true, false);
					}

					if (mKnockback > 0 && !EntityUtils.isCCImmuneMob(le) && !mPlugin.mEffectManager.hasEffect(le, ON_HIT_EFFECT + mPlayer.getName())) {
						float y = 0.4f;
						if (!le.isOnGround()) {
							y -= 0.2f;
						}
						if (!enteredWall) {
							y -= 0.15f;
						}
						mCosmetic.shieldOnHit(world, le.getLocation(), mPlayer);
						MovementUtils.knockAway(mLoc, le, mKnockback, y, true);
						mPlugin.mEffectManager.addEffect(le, ON_HIT_EFFECT + mPlayer.getName(), new OnHitTimerEffect(5, 0));
					}

					mMobsAlreadyHit.add(le);
				}

				mMobsAlreadyHit.removeIf(mob -> !entities.contains(mob));

				if (mCurrDuration >= mDuration) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				mCurrDuration = -1;
				ClientModHandler.updateAbility(mPlayer, ShieldWall.this);
			}
		}.runTaskTimer(mPlugin, 0, 1));

		return true;
	}

	@Override
	public int getInitialAbilityDuration() {
		return mDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 ? getInitialAbilityDuration() - this.mCurrDuration : 0;
	}
}
