package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.ThunderStepCS;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


public class ThunderStep extends Ability {

	public static final String NAME = "Thunder Step";
	public static final ClassAbility ABILITY = ClassAbility.THUNDER_STEP;

	/*
	 * Cloud's standardised constant order:
	 *
	 * Damage/additional damage/bonus damage/healing,
	 * size/distance,
	 * amplifiers/multipliers,
	 * durations,
	 * other skill technicalities eg knockback,
	 * cooldowns
	 */
	public static final int DAMAGE_1 = 5;
	public static final int DAMAGE_2 = 8;
	public static final int SIZE = 4;
	public static final int DISTANCE_1 = 8;
	public static final int DISTANCE_2 = 10;
	public static final double CHECK_INCREMENT = 0.1;
	public static final int COOLDOWN_TICKS = 20 * 20;

	public static final double BACK_TELEPORT_MAX_DISTANCE = 128;
	public static final int BACK_TELEPORT_MAX_DELAY = 3 * 20;
	public static final int ENHANCEMENT_BONUS_DAMAGE_TIMER = 30 * 20;
	public static final int ENHANCEMENT_PARALYZE_DURATION = 5 * 20;
	public static final float ENHANCEMENT_DAMAGE_RATIO = 0.2f;

	public static final String CHARM_DAMAGE = "Thunder Step Damage";
	public static final String CHARM_COOLDOWN = "Thunder Step Cooldown";
	public static final String CHARM_RADIUS = "Thunder Step Radius";
	public static final String CHARM_DISTANCE = "Thunder Step Distance";
	public static final String CHARM_ENHANCEMENT_DURATION = "Thunder Step Enhancement Duration";

	public static final AbilityInfo<ThunderStep> INFO =
		new AbilityInfo<>(ThunderStep.class, NAME, ThunderStep::new)
			.linkedSpell(ABILITY)
			.scoreboardId("ThunderStep")
			.shorthandName("TS")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Teleport forward, damaging mobs at the origin and destination.")
			.cooldown(COOLDOWN_TICKS, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ThunderStep::cast, new AbilityTrigger(AbilityTrigger.Key.DROP),
				AbilityTriggerInfo.HOLDING_MAGIC_WAND_RESTRICTION))
			.displayItem(Material.HORN_CORAL);

	private final double mLevelDamage;
	private final double mLevelDistance;
	private final double mRadius;
	private final int mBackTeleportMaxDelay;

	private final ThunderStepCS mCosmetic;

	private int mLastCastTick = -1;
	private @Nullable Location mLastCastLocation = null;
	private boolean mCanParalyze = false;

	public ThunderStep(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mLevelDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mLevelDistance = CharmManager.calculateFlatAndPercentValue(player, CHARM_DISTANCE, isLevelOne() ? DISTANCE_1 : DISTANCE_2);
		mRadius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RADIUS, SIZE);
		mBackTeleportMaxDelay = (int) CharmManager.calculateFlatAndPercentValue(player, CHARM_ENHANCEMENT_DURATION, BACK_TELEPORT_MAX_DELAY);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ThunderStepCS());
	}

	public boolean cast() {
		if (ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return false;
		}

		float spellDamage = SpellPower.getSpellDamage(mPlugin, mPlayer, (float) mLevelDamage);

		// if enhanced, can teleport back within a short time frame (regardless of if on cooldown or not)
		if (isEnhanced()
			&& Bukkit.getServer().getCurrentTick() <= mLastCastTick + mBackTeleportMaxDelay
			&& mLastCastLocation != null
			&& mLastCastLocation.getWorld() == mPlayer.getWorld()
			&& mLastCastLocation.distance(mPlayer.getLocation()) < BACK_TELEPORT_MAX_DISTANCE) {

			Location recastStartLocation = mPlayer.getLocation();
			doDamage(recastStartLocation, spellDamage * ENHANCEMENT_DAMAGE_RATIO, false);
			mLastCastLocation.setDirection(mPlayer.getLocation().getDirection());
			PlayerUtils.playerTeleport(mPlayer, mLastCastLocation);
			doDamage(mLastCastLocation, spellDamage * ENHANCEMENT_DAMAGE_RATIO, false);
			mCosmetic.trailEffect(mPlayer, recastStartLocation, mLastCastLocation);

			// prevent further back teleports as well as paralyze of any further casts
			mCosmetic.playerTeleportedBack();
			mLastCastLocation = null;
			mLastCastTick = -1;
			mCanParalyze = false;
			return true;
		}

		// on cooldown and didn't teleport back: stop here
		if (isOnCooldown()) {
			return false;
		}

		boolean doParalyze = isEnhanced() && mCanParalyze && Bukkit.getServer().getCurrentTick() <= mLastCastTick + ENHANCEMENT_BONUS_DAMAGE_TIMER;
		mCanParalyze = !doParalyze;

		putOnCooldown();
		mLastCastLocation = mPlayer.getLocation();
		mLastCastTick = Bukkit.getServer().getCurrentTick();

		Location playerStartLocation = mPlayer.getLocation();
		doDamage(playerStartLocation, spellDamage, doParalyze);

		World world = mPlayer.getWorld();
		BoundingBox movingPlayerBox = mPlayer.getBoundingBox();
		Vector vector = playerStartLocation.getDirection();
		LocationUtils.travelTillObstructed(
			world,
			movingPlayerBox,
			mLevelDistance,
			vector,
			CHECK_INCREMENT
		);
		Location playerEndLocation = movingPlayerBox
			.getCenter()
			.setY(movingPlayerBox.getMinY())
			.toLocation(world)
			.setDirection(vector);

		if (!playerEndLocation.getWorld().getWorldBorder().isInside(playerEndLocation)
			|| ZoneUtils.hasZoneProperty(playerEndLocation, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return true;
		}
		PlayerUtils.playerTeleport(mPlayer, playerEndLocation);
		doDamage(playerEndLocation, spellDamage, doParalyze);
		mCosmetic.trailEffect(mPlayer, playerStartLocation, playerEndLocation);
		if (isEnhanced() && mLastCastTick > -1) {
			mCosmetic.lingeringEffect(mPlugin, mPlayer, playerStartLocation, mBackTeleportMaxDelay);
		}

		return true;
	}

	public void doDamage(Location location, float spellDamage, boolean enhancementParalyze) {
		double ratio = mRadius / SIZE;
		mCosmetic.castEffect(mPlayer, ratio, mRadius);

		Hitbox hitbox = new Hitbox.SphereHitbox(location.clone().add(0, 0.9, 0), mRadius);
		List<LivingEntity> enemies = hitbox.getHitMobs();
		int mobParticles = Math.max(
			1, 20 / Math.max(1, enemies.size()) // Never divide by 0. Always maximum 20 particles for <= 1 enemy
		);
		for (LivingEntity enemy : enemies) {
			if (spellDamage > 0) {
				DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, spellDamage, ABILITY, true);
			}

			if (enhancementParalyze && !EntityUtils.isBoss(enemy)) {
				EntityUtils.paralyze(mPlugin, ENHANCEMENT_PARALYZE_DURATION, enemy);
			}
			mCosmetic.onDamage(mPlayer, enemy, mobParticles);
		}
	}

	@Override
	public void playerTeleportEvent(PlayerTeleportEvent event) {
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
			mLastCastLocation = null;
		}
	}

	private static Description<ThunderStep> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to materialize a flash of thunder, dealing ")
			.add(a -> a.mLevelDamage, DAMAGE_1, false, Ability::isLevelOne)
			.add(" thunder magic damage to all enemies within ")
			.add(a -> a.mRadius, SIZE)
			.add(" blocks around you. The next moment, you teleport in the direction you're looking, travelling up to ")
			.add(a -> a.mLevelDistance, DISTANCE_1, false, Ability::isLevelOne)
			.add(" blocks or until you hit a solid block, and repeat the thunder attack at your destination.")
			.addCooldown(COOLDOWN_TICKS);
	}

	private static Description<ThunderStep> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Damage is increased to ")
			.add(a -> a.mLevelDamage, DAMAGE_2, false, Ability::isLevelTwo)
			.add(" and teleport range is increased to ")
			.add(a -> a.mLevelDistance, DISTANCE_2, false, Ability::isLevelTwo)
			.add(" blocks.");
	}

	private static Description<ThunderStep> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Within ")
			.addDuration(BACK_TELEPORT_MAX_DELAY)
			.add(" seconds of casting, trigger again to return to the original starting location, dealing ")
			.addPercent(ENHANCEMENT_DAMAGE_RATIO)
			.add(" of the skill's damage. If you do not do so, your next Thunder Step within ")
			.addDuration(ENHANCEMENT_BONUS_DAMAGE_TIMER)
			.add(" seconds will paralyze enemies for ")
			.addDuration(ENHANCEMENT_PARALYZE_DURATION)
			.add(" seconds.");
	}
}
