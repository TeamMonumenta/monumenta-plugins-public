package com.playmonumenta.plugins.abilities.scout.hunter;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.FireworkStrikeCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.PredatorStrikeCS;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Trident;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


public class PredatorStrike extends Ability {

	private static final int COOLDOWN_1 = 20 * 18;
	private static final int COOLDOWN_2 = 20 * 14;
	private static final double DISTANCE_SCALE_1 = 0.1;
	private static final double DISTANCE_SCALE_2 = 0.15;
	private static final int MAX_RANGE = 30;
	private static final int MAX_DAMAGE_RANGE = 12;
	private static final double EXPLODE_RADIUS = 0.75;
	private static final double HITBOX_LENGTH = 0.5;

	public static final String CHARM_COOLDOWN = "Predator Strike Cooldown";
	public static final String CHARM_DAMAGE = "Predator Strike Damage";
	public static final String CHARM_RADIUS = "Predator Strike Radius";

	private boolean mActive = false;
	private final double mDistanceScale;
	private final double mExplodeRadius;

	private final PredatorStrikeCS mCosmetic;

	public PredatorStrike(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Predator Strike");
		mInfo.mLinkedSpell = ClassAbility.PREDATOR_STRIKE;
		mInfo.mScoreboardId = "PredatorStrike";
		mInfo.mShorthandName = "PrS";
		mInfo.mDescriptions.add(String.format("Left-clicking with a bow, trident, or snowball while not sneaking will prime a Predator Strike that unprimes after 5s. When you fire a critical arrow, it will instantaneously travel in a straight line for up to %d blocks or until it hits an enemy or block and damages enemies in a %s block radius. This ability deals 100%% of your projectile base damage increased by %d%% for every block of distance from you and the target (up to %d blocks, or %d%% total). Hit targets contribute to Sharpshooter stacks. Cooldown: %ds.",
			MAX_RANGE, EXPLODE_RADIUS, (int)(DISTANCE_SCALE_1 * 100), MAX_DAMAGE_RANGE, MAX_DAMAGE_RANGE * (int)(DISTANCE_SCALE_1 * 100) + 100, COOLDOWN_1 / 20));
		mInfo.mDescriptions.add(String.format("Damage now increases %d%% for each block of distance (up to %d%%). Cooldown: %ds.", (int)(DISTANCE_SCALE_2 * 100), MAX_DAMAGE_RANGE * (int)(DISTANCE_SCALE_2 * 100) + 100, COOLDOWN_2 / 20));
		mInfo.mCooldown = CharmManager.getCooldown(mPlayer, CHARM_COOLDOWN, isLevelOne() ? COOLDOWN_1 : COOLDOWN_2);
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.SPECTRAL_ARROW, 1);

		mDistanceScale = isLevelOne() ? DISTANCE_SCALE_1 : DISTANCE_SCALE_2;
		mExplodeRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, EXPLODE_RADIUS);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new PredatorStrikeCS(), PredatorStrikeCS.SKIN_LIST);
	}

	@Override
	public void cast(Action action) {
		if (mPlayer != null && !mPlayer.isSneaking() && !mActive && !mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (ItemUtils.isBowOrTrident(mainHand) || mainHand.getType() == Material.SNOWBALL) {
				mActive = true;
				ClientModHandler.updateAbility(mPlayer, this);
				World world = mPlayer.getWorld();

				mCosmetic.strikeSoundReady(world, mPlayer);
				new BukkitRunnable() {
					int mTicks = 0;

					@Override
					public void run() {
						mTicks++;
						mCosmetic.strikeTick(mPlayer, mTicks);

						if (!mActive || mTicks >= 20 * 5) {
							mActive = false;
							this.cancel();
							ClientModHandler.updateAbility(mPlayer, PredatorStrike.this);
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}
		}
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (mPlayer != null && mActive && ((projectile instanceof AbstractArrow arrow && arrow.isCritical()) || projectile instanceof Trident || projectile instanceof Snowball)) {
			mActive = false;
			putOnCooldown();
			projectile.remove();
			mPlugin.mProjectileEffectTimers.removeEntity(projectile);

			Location loc = mPlayer.getEyeLocation();
			Vector direction = loc.getDirection();
			Vector shift = direction.normalize().multiply(HITBOX_LENGTH);
			BoundingBox box = BoundingBox.of(loc, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
			box.shift(direction);

			World world = mPlayer.getWorld();
			mCosmetic.strikeLaunch(world, mPlayer);

			Set<LivingEntity> nearbyMobs = new HashSet<LivingEntity>(EntityUtils.getNearbyMobs(loc, MAX_RANGE));

			boolean hit = false;
			for (double r = 0; r < MAX_RANGE; r += HITBOX_LENGTH) {
				Location bLoc = box.getCenter().toLocation(world);
				mCosmetic.strikeParticleProjectile(mPlayer, bLoc);

				if (!bLoc.isChunkLoaded() || bLoc.getBlock().getType().isSolid()) {
					bLoc.subtract(direction.multiply(0.5));
					// Snapshot the mobs that would be hit if it were instananeous.
					// This is very important for the Firework Strike Cosmetic
					List<LivingEntity> mobs = EntityUtils.getNearbyMobs(bLoc, mExplodeRadius, mPlayer);
					mCosmetic.strikeImpact(() -> explode(bLoc, mobs), bLoc, mPlayer);
					hit = true;
					break;
				}

				for (LivingEntity mob : nearbyMobs) {
					if (mob.getBoundingBox().overlaps(box)) {
						if (EntityUtils.isHostileMob(mob)) {
							// Snapshot the mobs that would be hit if it were instananeous.
							// This is very important for the Firework Strike Cosmetic
							List<LivingEntity> mobs = EntityUtils.getNearbyMobs(bLoc, mExplodeRadius, mPlayer);
							mCosmetic.strikeImpact(() -> explode(bLoc, mobs), bLoc, mPlayer);
							hit = true;
							break;
						}
					}
				}

				if (hit) {
					break;
				}
				box.shift(shift);
			}
			if (!hit) {
				Location bLoc = box.getCenter().toLocation(world);
				if (mCosmetic instanceof FireworkStrikeCS) {
					mCosmetic.strikeImpact(() -> mCosmetic.strikeExplode(world, mPlayer, bLoc, mExplodeRadius), bLoc, mPlayer);
				}
			}
		}
		return true;
	}

	private void explode(Location loc, List<LivingEntity> mobs) {
		if (mPlayer == null) {
			return;
		}
		World world = mPlayer.getWorld();

		double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, ItemStatUtils.getAttributeAmount(mPlayer.getInventory().getItemInMainHand(), ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_ADD, ItemStatUtils.Operation.ADD, ItemStatUtils.Slot.MAINHAND) * (2 + mDistanceScale * Math.min(mPlayer.getLocation().distance(loc), MAX_DAMAGE_RANGE)));

		mCosmetic.strikeExplode(world, mPlayer, loc, mExplodeRadius);

		for (LivingEntity mob : mobs) {
			MovementUtils.knockAway(loc, mob, 0.25f, 0.25f, true);
			DamageUtils.damage(mPlayer, mob, DamageType.PROJECTILE_SKILL, damage, mInfo.mLinkedSpell, true);
		}
	}

	@Override
	public @Nullable String getMode() {
		return mActive ? "active" : null;
	}
}
