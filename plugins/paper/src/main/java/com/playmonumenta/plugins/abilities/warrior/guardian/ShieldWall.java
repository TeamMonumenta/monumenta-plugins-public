package com.playmonumenta.plugins.abilities.warrior.guardian;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.guardian.ShieldWallCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class ShieldWall extends Ability {

	private static final int SHIELD_WALL_1_DURATION = 8 * 30;
	private static final int SHIELD_WALL_2_DURATION = 10 * 20;
	private static final int SHIELD_WALL_DAMAGE = 3;
	private static final int SHIELD_WALL_1_COOLDOWN = 20 * 30;
	private static final int SHIELD_WALL_2_COOLDOWN = 20 * 20;
	private static final int SHIELD_WALL_ANGLE = 180;
	private static final float SHIELD_WALL_2_KNOCKBACK = 0.3f;
	private static final double SHIELD_WALL_RADIUS = 4.0;

	public static final String CHARM_DURATION = "Shield Wall Duration";
	public static final String CHARM_DAMAGE = "Shield Wall Damage";
	public static final String CHARM_COOLDOWN = "Shield Wall Cooldown";
	public static final String CHARM_ANGLE = "Shield Wall Angle";
	public static final String CHARM_KNOCKBACK = "Shield Wall Knockback";

	private int mDuration;
	private final ShieldWallCS mCosmetic;

	public ShieldWall(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Shield Wall");
		mInfo.mScoreboardId = "ShieldWall";
		mInfo.mShorthandName = "SW";
		mInfo.mDescriptions.add("Press the swap key while holding a shield in either hand to create a 180 degree arc of particles 5 blocks high and 4 blocks wide in front of the user. This blocks all enemy projectiles (Ghast fireballs explode on the wall) and deals 3 melee damage to enemies that pass through the wall. The shield lasts 8 seconds. Cooldown: 30s.");
		mInfo.mDescriptions.add("The shield lasts 10 seconds instead. Additionally, the shield knocks back enemies that try to go through it. Cooldown: 20s.");
		mInfo.mCooldown = CharmManager.getCooldown(mPlayer, CHARM_COOLDOWN, isLevelOne() ? SHIELD_WALL_1_COOLDOWN : SHIELD_WALL_2_COOLDOWN);
		mInfo.mLinkedSpell = ClassAbility.SHIELD_WALL;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.STONE_BRICK_WALL, 1);

		mDuration = (isLevelOne() ? SHIELD_WALL_1_DURATION : SHIELD_WALL_2_DURATION) + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ShieldWallCS(), ShieldWallCS.SKIN_LIST);
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (mPlayer == null) {
			return;
		}
		event.setCancelled(true);
		if (!isTimerActive()) {
			float knockback = (float) (isLevelTwo() ? CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, SHIELD_WALL_2_KNOCKBACK) : 0);
			double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, SHIELD_WALL_DAMAGE);
			double angle = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ANGLE, SHIELD_WALL_ANGLE);

			World world = mPlayer.getWorld();
			mCosmetic.shieldStartEffect(world, mPlayer, SHIELD_WALL_RADIUS);
			putOnCooldown();

			ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

			new BukkitRunnable() {
				int mT = 0;
				final int mHeight = 5;
				final Location mLoc = mPlayer.getLocation();
				final List<BoundingBox> mBoxes = new ArrayList<>();
				List<LivingEntity> mMobsAlreadyHit = new ArrayList<>();
				final List<LivingEntity> mMobsHitThisTick = new ArrayList<>();
				boolean mHitboxes = false;

				@Override
				public void run() {
					mT++;
					Vector vec;
					for (int y = 0; y < mHeight; y++) {
						for (double degree = 0; degree < angle; degree += 10) {
							double radian1 = Math.toRadians(degree - 0.5 * angle);
							vec = new Vector(-FastUtils.sin(radian1) * SHIELD_WALL_RADIUS, y, FastUtils.cos(radian1) * SHIELD_WALL_RADIUS);
							vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

							Location l = mLoc.clone().add(vec);
							if (mT % 4 == 0) {
								mCosmetic.shieldWallDot(mPlayer, l, degree, angle, y, mHeight);
							}
							if (!mHitboxes) {
								mBoxes.add(BoundingBox.of(l.clone().subtract(0.6, 0, 0.6),
								                         l.clone().add(0.6, 5, 0.6)));
							}
						}
						mHitboxes = true;
					}

					for (BoundingBox box : mBoxes) {
						for (Entity e :world.getNearbyEntities(box)) {
							Location eLoc = e.getLocation();
							if (e instanceof Projectile proj) {
								if (proj.getShooter() instanceof LivingEntity shooter && !(shooter instanceof Player)) {
									proj.remove();
									mCosmetic.shieldOnBlock(world, eLoc, mPlayer);
								}
							} else if (e instanceof LivingEntity le && EntityUtils.isHostileMob(e)) {
								// Stores mobs hit this tick
								mMobsHitThisTick.add(le);
								// This list does not update to the mobs hit this tick until after everything runs
								if (!mMobsAlreadyHit.contains(le)) {
									mMobsAlreadyHit.add(le);
									Vector v = le.getVelocity();

									DamageUtils.damage(mPlayer, le, new DamageEvent.Metadata(DamageType.MELEE_SKILL, mInfo.mLinkedSpell, playerItemStats), damage, false, true, false);

									//Bosses should not be affected by slowness or knockback.
									if (knockback > 0 && !e.getScoreboardTags().contains("Boss")) {
										MovementUtils.knockAway(mLoc, le, knockback, true);
										mCosmetic.shieldOnHit(world, eLoc, mPlayer);
									} else {
										le.setVelocity(v);
									}
								} else if (le.getNoDamageTicks() + 5 < le.getMaximumNoDamageTicks()) {
									if (knockback > 0 && !e.getScoreboardTags().contains("Boss")) {
										/*
										 * This is a temporary fix while we decide how to handle KBR mobs with Shield Wall level 2.
										 *
										 * If a mob collides with shield wall halfway through its invulnerability period, assume it
										 * resists knockback and give it Slowness V for 5 seconds to simulate the old effect of
										 * halting mobs with stunlock damage, minus the insane damage part.
										 *
										 * This effect is reapplied each tick, so the mob is slowed drastically until 2 seconds
										 * after they leave shield wall hitbox.
										 */
										PotionUtils.applyPotion(mPlayer, le, new PotionEffect(PotionEffectType.SLOW, 20 * 2, 4, true, false));
									}
								}
							}
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
						if (mMobsHitThisTick.contains(mob)) {
							mobsAlreadyHitAdjusted.add(mob);
						}
					}
					mMobsAlreadyHit = mobsAlreadyHitAdjusted;
					mMobsHitThisTick.clear();
					if (mT >= mDuration) {
						this.cancel();
						mBoxes.clear();
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public boolean runCheck() {
		if (mPlayer == null) {
			return false;
		}
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack oHand = mPlayer.getInventory().getItemInOffHand();
		return mHand.getType() == Material.SHIELD || oHand.getType() == Material.SHIELD;
	}
}
