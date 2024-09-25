package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.SanguineHarvestCS;
import com.playmonumenta.plugins.effects.SanguineHarvestBlight;
import com.playmonumenta.plugins.effects.SanguineMark;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class SanguineHarvest extends Ability implements AbilityWithDuration {

	private static final int BLEED_DURATION = 10 * 20;
	private static final double BLEED_LEVEL = 0.2;
	private static final int RANGE = 8;
	private static final int RADIUS = 4;
	private static final double HEAL_PERCENT_1 = 0.05;
	private static final double HEAL_PERCENT_2 = 0.1;
	private static final double DAMAGE_BOOST = 0.1;
	private static final int MARK_DURATION = 20 * 20;
	private static final int COOLDOWN = 20 * 20;
	private static final double HITBOX_LENGTH = 0.55;

	private static final double ENHANCEMENT_DMG_INCREASE = 0.03;
	private static final int ENHANCEMENT_BLIGHT_DURATION = 20 * 6;
	private static final String BLIGHT_EFFECT_NAME = "SanguineHarvestBlightEffect";

	private static final String SANGUINE_NAME = "SanguineEffect";

	public static final String CHARM_RADIUS = "Sanguine Harvest Radius";
	public static final String CHARM_RANGE = "Sanguine Harvest Range";
	public static final String CHARM_COOLDOWN = "Sanguine Harvest Cooldown";
	public static final String CHARM_HEAL = "Sanguine Harvest Healing";
	public static final String CHARM_KNOCKBACK = "Sanguine Harvest Knockback";
	public static final String CHARM_BLEED = "Sanguine Harvest Bleed Amplifier";
	public static final String CHARM_DAMAGE_BOOST = "Sanguine Harvest Damage Boost Amplifier";

	public static final AbilityInfo<SanguineHarvest> INFO =
		new AbilityInfo<>(SanguineHarvest.class, "Sanguine Harvest", SanguineHarvest::new)
			.linkedSpell(ClassAbility.SANGUINE_HARVEST)
			.scoreboardId("SanguineHarvest")
			.shorthandName("SH")
			.actionBarColor(TextColor.color(179, 0, 0))
			.descriptions(
				("Enemies you damage with an ability are afflicted with Bleed II for %s seconds. " +
					"Bleed gives mobs 10%% Slowness and 10%% Weaken per level if the mob is below 50%% Max Health. " +
					"Additionally, right click while holding a scythe and not sneaking to fire a burst of darkness. " +
					"This projectile travels up to %s blocks and upon contact with a block or enemy or reaching max range, it explodes, " +
					"knocking back all mobs within %s blocks and applying a blood mark to them for %ss. " +
					"Any player that kills a marked mob is healed for %s%% of max health. Cooldown: %ss.")
					.formatted(StringUtils.ticksToSeconds(BLEED_DURATION), RANGE, RADIUS, StringUtils.ticksToSeconds(MARK_DURATION), StringUtils.multiplierToPercentage(HEAL_PERCENT_1), StringUtils.ticksToSeconds(COOLDOWN)),
				("Increase the mark's healing to %s%% of max health. " +
					"Melee attacks against marked mobs heal the player for %s%% of their max health, deal %s%% more damage, and consume the mark.")
					.formatted(StringUtils.multiplierToPercentage(HEAL_PERCENT_2), StringUtils.multiplierToPercentage(HEAL_PERCENT_2), StringUtils.multiplierToPercentage(DAMAGE_BOOST)),
				("Sanguine Harvest creates a %s-block cone of blight on the ground that lasts for %ss. " +
					"Mobs in the blighted area take %s%% extra damage per debuff they currently have. Blight is not counted as a debuff.")
					.formatted(RANGE, StringUtils.ticksToSeconds(ENHANCEMENT_BLIGHT_DURATION), StringUtils.multiplierToPercentage(ENHANCEMENT_DMG_INCREASE)))
			.simpleDescription("Passively apply Bleed with your abilities. Activate to mark mobs, healing whoever kills them.")
			.quest216Message("-------s-------m-------")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", SanguineHarvest::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(false),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(Material.NETHER_STAR);

	private final double mRadius;
	private final double mBleedLevel;
	private final double mHealPercent;
	private final double mDamageBoost;

	private int mCurrDuration = -1;

	private final ArrayList<Location> mMarkedLocations = new ArrayList<>(); // To mark locations (Even if block is not replaced)

	private final SanguineHarvestCS mCosmetic;

	public SanguineHarvest(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(player, CHARM_RADIUS, RADIUS);
		mHealPercent = CharmManager.calculateFlatAndPercentValue(player, CHARM_HEAL, isLevelOne() ? HEAL_PERCENT_1 : HEAL_PERCENT_2);
		mDamageBoost = CharmManager.getLevelPercentDecimal(player, CHARM_DAMAGE_BOOST) + DAMAGE_BOOST;
		mBleedLevel = CharmManager.getLevelPercentDecimal(player, CHARM_BLEED) + BLEED_LEVEL;
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new SanguineHarvestCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		Vector direction = loc.getDirection();

		World world = mPlayer.getWorld();
		mCosmetic.onCast(world, loc);

		double range = CharmManager.getRadius(mPlayer, CHARM_RANGE, RANGE);

		if (isEnhanced()) {
			mMarkedLocations.clear();
			mCosmetic.onEnhancementCast(mPlayer, mPlayer.getLocation());
			Vector v;
			for (double degree = -40; degree < 40; degree += 10) {
				for (double r = 0; r <= range; r += 0.55) {
					double radian = Math.toRadians(degree);
					v = new Vector(Math.cos(radian) * r, 0, Math.sin(radian) * r);
					v = VectorUtils.rotateZAxis(v, mPlayer.getLocation().getPitch());
					v = VectorUtils.rotateYAxis(v, mPlayer.getLocation().getYaw() + 90);

					Location location = mPlayer.getEyeLocation().clone().add(v);

					Location marker = location.clone();

					// If enhanced, we want to find where the lowest block is.
					// First, search downwards by 5 blocks until a block is reached.
					// And then set it to just above the block as the saved location.
					while (marker.distance(location) <= 5) {
						Block block = marker.getBlock();
						if (block.isSolid()) {
							// Success, add this location as cursed.
							marker.setY(1.1 + (int) marker.getY());

							// don't add mark locations too close together when they would cover the same space anyways
							if (mMarkedLocations.stream().allMatch(markLoc -> markLoc.distance(marker) > HITBOX_LENGTH)) {
								mMarkedLocations.add(marker);
							}

							break;
						} else {
							marker.add(0, -0.5, 0);
						}
					}

					if (location.getBlock().isSolid()) {
						// Break here because I decided that this ability shouldn't pass through blocks.
						// How mean!
						break;
					}
				}
			}

			if (!mMarkedLocations.isEmpty()) {
				runMarkerRunnable();
			}
		}

		RayTraceResult result = world.rayTrace(loc, direction, range, FluidCollisionMode.NEVER, true, 0.425,
			e -> EntityUtils.isHostileMob(e) && !ScoreboardUtils.checkTag(e, AbilityUtils.IGNORE_TAG) && !e.isDead() && e.isValid());

		Location endLoc;
		if (result == null) {
			endLoc = loc.clone().add(direction.multiply(range));
		} else {
			endLoc = result.getHitPosition().toLocation(world);
		}
		explode(endLoc);

		mCosmetic.projectileParticle(mPlayer, loc, endLoc);
		return true;
	}

	private void explode(Location loc) {
		World world = mPlayer.getWorld();
		mCosmetic.onExplode(mPlayer, world, loc, mRadius);

		Hitbox hitbox = new Hitbox.SphereHitbox(loc, mRadius);
		for (LivingEntity mob : hitbox.getHitMobs()) {
			MovementUtils.knockAway(loc, mob, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, 0.2f), 0.2f, true);
			mPlugin.mEffectManager.addEffect(mob, SANGUINE_NAME, new SanguineMark(isLevelTwo(), mHealPercent, mDamageBoost, MARK_DURATION, mPlayer, mPlugin, mCosmetic));
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getAbility() == null || event.getAbility().isFake()) {
			return false;
		}
		EntityUtils.applyBleed(mPlugin, BLEED_DURATION, mBleedLevel, enemy);
		return false; // applies bleed on damage to all mobs hit, causes no recursion
	}

	private void runMarkerRunnable() {
		if (!mMarkedLocations.isEmpty() &&
			isEnhanced()) {
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					if (mTicks > ENHANCEMENT_BLIGHT_DURATION || mMarkedLocations.isEmpty()) {
						mMarkedLocations.clear();
						this.cancel();
						return;
					}

					for (Location location : mMarkedLocations) {
						BoundingBox boundingBox = BoundingBox.of(location, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
						mCosmetic.atMarkedLocation(mPlayer, location);

						if (mTicks % 20 == 0) {
							List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(location, 1);
							for (LivingEntity mob : nearbyMobs) {
								if (mob.getBoundingBox().overlaps(boundingBox)) {
									mPlugin.mEffectManager.addEffect(mob, BLIGHT_EFFECT_NAME, new SanguineHarvestBlight(20, ENHANCEMENT_DMG_INCREASE, mPlugin));
								}
							}
						}
					}

					mTicks += 10;
					mCurrDuration += 10;
				}

				@Override
				public synchronized void cancel() {
					super.cancel();
					mCurrDuration = -1;
					ClientModHandler.updateAbility(mPlayer, SanguineHarvest.this);
				}
			}.runTaskTimer(mPlugin, 0, 10);
			mCurrDuration = 0;
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	@Override
	public int getInitialAbilityDuration() {
		return isEnhanced() ? ENHANCEMENT_BLIGHT_DURATION : 0;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 && isEnhanced() ? getInitialAbilityDuration() - this.mCurrDuration : 0;
	}
}
