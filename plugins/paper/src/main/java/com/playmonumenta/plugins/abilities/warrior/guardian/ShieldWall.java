package com.playmonumenta.plugins.abilities.warrior.guardian;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.guardian.ShieldWallCS;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ShieldWall extends Ability {

	private static final int SHIELD_WALL_1_DURATION = 6 * 20;
	private static final int SHIELD_WALL_2_DURATION = 10 * 20;
	private static final int SHIELD_WALL_REPOSITION_PENALTY = 10;
	private static final int SHIELD_WALL_DAMAGE = 3;
	private static final int SHIELD_WALL_1_COOLDOWN = 20 * 30;
	private static final int SHIELD_WALL_2_COOLDOWN = 20 * 18;
	private static final int SHIELD_WALL_ANGLE = 180;
	private static final float SHIELD_WALL_KNOCKBACK = 0.3f;
	private static final double SHIELD_WALL_RADIUS = 4.0;
	private static final int SHIELD_WALL_HEIGHT = 5;

	public static final String CHARM_DURATION = "Shield Wall Duration";
	public static final String CHARM_DAMAGE = "Shield Wall Damage";
	public static final String CHARM_COOLDOWN = "Shield Wall Cooldown";
	public static final String CHARM_ANGLE = "Shield Wall Angle";
	public static final String CHARM_KNOCKBACK = "Shield Wall Knockback";
	public static final String CHARM_HEIGHT = "Shield Wall Height";

	public static final AbilityInfo<ShieldWall> INFO =
		new AbilityInfo<>(ShieldWall.class, "Shield Wall", ShieldWall::new)
			.linkedSpell(ClassAbility.SHIELD_WALL)
			.scoreboardId("ShieldWall")
			.shorthandName("SW")
			.descriptions(
				String.format("Press the swap key while holding a shield in either hand to create a %s degree arc of particles from 1 block below to %s blocks above the user's location and with a %s block radius in front of the user. " +
					"Enemies that pass through the wall are dealt %s melee damage and knocked back. The wall also blocks all enemy projectiles such as arrows or fireballs. The wall lasts %s seconds. Triggering while active will reposition the wall with the remaining duration minus %ss. Cooldown: %ss.",
					SHIELD_WALL_ANGLE,
					SHIELD_WALL_HEIGHT,
					(int) SHIELD_WALL_RADIUS,
					SHIELD_WALL_DAMAGE,
					StringUtils.ticksToSeconds(SHIELD_WALL_1_DURATION),
					StringUtils.ticksToSeconds(SHIELD_WALL_REPOSITION_PENALTY),
					StringUtils.ticksToSeconds(SHIELD_WALL_1_COOLDOWN)
				),
				String.format("The shield lasts %s seconds instead. Cooldown: %ss.",
					StringUtils.ticksToSeconds(SHIELD_WALL_2_DURATION),
					StringUtils.ticksToSeconds(SHIELD_WALL_2_COOLDOWN)
				)
			)
			.simpleDescription("Deploy a wall that can block projectiles and mobs from entering.")
			.cooldown(SHIELD_WALL_1_COOLDOWN, SHIELD_WALL_2_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ShieldWall::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP),
				new AbilityTriggerInfo.TriggerRestriction("holding a shield in either hand",
					player -> player.getInventory().getItemInMainHand().getType() == Material.SHIELD || player.getInventory().getItemInOffHand().getType() == Material.SHIELD)))
			.displayItem(Material.STONE_BRICK_WALL);

	private final int mDuration;
	private final int mHeight;
	private final ShieldWallCS mCosmetic;

	private boolean mReposition = false;

	public ShieldWall(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, (isLevelOne() ? SHIELD_WALL_1_DURATION : SHIELD_WALL_2_DURATION));
		mHeight = SHIELD_WALL_HEIGHT + (int) CharmManager.getLevel(mPlayer, CHARM_HEIGHT);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ShieldWallCS());
	}

	public void cast() {
		if (isOnCooldown()) {
			mReposition = true;
			return;
		}
		float knockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, SHIELD_WALL_KNOCKBACK);
		double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, SHIELD_WALL_DAMAGE);
		double angle = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ANGLE, SHIELD_WALL_ANGLE);

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		mCosmetic.shieldStartEffect(world, mPlayer, loc, SHIELD_WALL_RADIUS);
		putOnCooldown();

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		cancelOnDeath(new BukkitRunnable() {
			int mT = 0;
			List<LivingEntity> mMobsAlreadyHit = new ArrayList<>();
			Location mLoc = loc;
			@Nullable Hitbox mHitbox = null;

			@Override
			public void run() {
				mT++;
				Vector vec;

				if (mReposition || mHitbox == null) {
					if (mReposition) {
						mT += SHIELD_WALL_REPOSITION_PENALTY;
					}
					mReposition = false;
					mLoc = mPlayer.getLocation();
					mHitbox = Hitbox.approximateHollowCylinderSegment(mLoc.clone().subtract(0, -1, 0), mHeight + 1, SHIELD_WALL_RADIUS - 0.6, SHIELD_WALL_RADIUS + 0.6, Math.toRadians(angle) / 2);
				}

				if (mT % 4 == 0) {
					for (double degree = 0; degree < angle; degree += 10) {
						double radian1 = Math.toRadians(degree - 0.5 * angle);
						vec = new Vector(-FastUtils.sin(radian1) * SHIELD_WALL_RADIUS, -1, FastUtils.cos(radian1) * SHIELD_WALL_RADIUS);
						vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());
						Location l = mLoc.clone().add(vec);
						for (int y = 0; y < mHeight + 1; y++) {
							l.add(0, 1, 0);
							mCosmetic.shieldWallDot(mPlayer, l, degree, angle, y, mHeight);
						}
					}
				}

				List<Projectile> projectiles = mHitbox.getHitEntitiesByClass(Projectile.class);
				for (Projectile proj : projectiles) {
					if (proj.getShooter() instanceof LivingEntity shooter && !(shooter instanceof Player)) {
						proj.remove();
						mCosmetic.shieldOnBlock(world, proj.getLocation(), mPlayer);
					}
				}

				List<LivingEntity> entities = mHitbox.getHitMobs();
				for (LivingEntity le : entities) {
					//Bosses and cc immune mobs should not be affected by slowness or knockback.
					boolean shouldKnockback = knockback > 0 && !EntityUtils.isCCImmuneMob(le);
					// This list does not update to the mobs hit this tick until after everything runs
					if (!mMobsAlreadyHit.contains(le)) {
						mMobsAlreadyHit.add(le);

						DamageUtils.damage(mPlayer, le, new DamageEvent.Metadata(DamageType.MELEE_SKILL, mInfo.getLinkedSpell(), playerItemStats), damage, false, true, false);
						if (shouldKnockback) {
							MovementUtils.knockAway(mLoc, le, knockback, true);
							mCosmetic.shieldOnHit(world, le.getLocation(), mPlayer);
						}
					} else if (shouldKnockback && le.getNoDamageTicks() + 5 < le.getMaximumNoDamageTicks()) {
						/*
						 * This is a temporary fix while we decide how to handle KBR mobs
						 *
						 * If a mob collides with shield wall halfway through its invulnerability period, assume it
						 * resists knockback and give it -100% speed for 2 seconds to halt the mob.
						 *
						 * This effect is reapplied each tick, so the mob is slowed drastically until 2 seconds
						 * after they leave shield wall hitbox.
						 */
						mPlugin.mEffectManager.addEffect(le, "ShieldWallRoot", new BaseMovementSpeedModifyEffect(20 * 2, -1));
					}
				}

				/*
				 * Compare the two lists of mobs and only remove from the
				 * actual hit tracker if the mob isn't detected as hit this
				 * tick, meaning it is no longer in the shield wall hitbox
				 * and is thus eligible for another hit.
				 */
				List<LivingEntity> mobsAlreadyHitAdjusted = new ArrayList<>();
				for (LivingEntity mob : mMobsAlreadyHit) {
					if (entities.contains(mob)) {
						mobsAlreadyHitAdjusted.add(mob);
					}
				}
				mMobsAlreadyHit = mobsAlreadyHitAdjusted;
				if (mT >= mDuration) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1));
	}

}
