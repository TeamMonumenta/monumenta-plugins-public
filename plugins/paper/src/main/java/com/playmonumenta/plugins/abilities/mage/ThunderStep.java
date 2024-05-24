package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
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
	public static final int COOLDOWN_SECONDS = 20;
	public static final int COOLDOWN_TICKS = COOLDOWN_SECONDS * 20;

	public static final double BACK_TELEPORT_MAX_DISTANCE = 64;
	public static final int BACK_TELEPORT_MAX_DELAY = 3 * 20;
	public static final int ENHANCEMENT_BONUS_DAMAGE_TIMER = 30 * 20;
	public static final int ENHANCEMENT_PARALYZE_DURATION = 5 * 20;
	public static final float ENHANCEMENT_DAMAGE_RATIO = 0.2f;

	public static final String CHARM_DAMAGE = "Thunder Step Damage";
	public static final String CHARM_COOLDOWN = "Thunder Step Cooldown";
	public static final String CHARM_RADIUS = "Thunder Step Radius";
	public static final String CHARM_DISTANCE = "Thunder Step Distance";

	private final ThunderStepCS mCosmetic;

	public static final AbilityInfo<ThunderStep> INFO =
		new AbilityInfo<>(ThunderStep.class, NAME, ThunderStep::new)
			.linkedSpell(ABILITY)
			.scoreboardId("ThunderStep")
			.shorthandName("TS")
			.descriptions(
				String.format(
					"Pressing the drop key while holding a wand materializes a flash of thunder," +
						" dealing %s thunder magic damage to all enemies in a %s block radius around you and knocking them away." +
						" The next moment, you teleport towards where you're looking, travelling up to %s blocks or until you hit a solid block," +
						" and repeat the thunder attack at your destination, ignoring iframes. Cooldown: %ss.",
					DAMAGE_1,
					SIZE,
					DISTANCE_1,
					COOLDOWN_SECONDS
				),
				String.format(
					"Damage is increased from %s to %s." +
						" Teleport range is increased from %s to %s blocks.",
					DAMAGE_1,
					DAMAGE_2,
					DISTANCE_1,
					DISTANCE_2
				),
				String.format("Within %ss of casting, use the same trigger to return to the original starting location, dealing %s%% of the skill's damage." +
					              " If you do not do so, your next Thunder Step within %ss will paralyze enemies for %ss.",
					BACK_TELEPORT_MAX_DELAY / 20,
					(int) (ENHANCEMENT_DAMAGE_RATIO * 100),
					ENHANCEMENT_BONUS_DAMAGE_TIMER / 20,
					ENHANCEMENT_PARALYZE_DURATION / 20
				)
			)
			.simpleDescription("Teleport forward, damaging mobs at the origin and destination.")
			.cooldown(COOLDOWN_TICKS, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ThunderStep::cast, new AbilityTrigger(AbilityTrigger.Key.DROP),
				AbilityTriggerInfo.HOLDING_MAGIC_WAND_RESTRICTION))
			.displayItem(Material.HORN_CORAL);

	private final float mLevelDamage;
	private final int mLevelDistance;

	private int mLastCastTick = -1;
	private @Nullable Location mLastCastLocation = null;
	private boolean mCanParalyze = false;

	public ThunderStep(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mLevelDamage = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mLevelDistance = (int) CharmManager.calculateFlatAndPercentValue(player, CHARM_DISTANCE, isLevelOne() ? DISTANCE_1 : DISTANCE_2);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ThunderStepCS());
	}

	public boolean cast() {
		if (ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return false;
		}

		float spellDamage = SpellPower.getSpellDamage(mPlugin, mPlayer, mLevelDamage);

		// if enhanced, can teleport back within a short time frame (regardless of if on cooldown or not)
		if (isEnhanced()
			    && Bukkit.getServer().getCurrentTick() <= mLastCastTick + BACK_TELEPORT_MAX_DELAY
			    && mLastCastLocation != null
			    && mLastCastLocation.getWorld() == mPlayer.getWorld()
			    && mLastCastLocation.distance(mPlayer.getLocation()) < BACK_TELEPORT_MAX_DISTANCE) {

			doDamage(mPlayer.getLocation(), spellDamage * ENHANCEMENT_DAMAGE_RATIO, false);
			mLastCastLocation.setDirection(mPlayer.getLocation().getDirection());
			mPlayer.teleport(mLastCastLocation);
			doDamage(mLastCastLocation, spellDamage * ENHANCEMENT_DAMAGE_RATIO, false);

			// prevent further back teleports as well as paralyze of any further casts
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
			CHECK_INCREMENT,
			true,
			null, -1, -1
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

		mPlayer.teleport(playerEndLocation);
		doDamage(playerEndLocation, spellDamage, doParalyze);

		return true;
	}

	public void doDamage(Location location, float spellDamage, boolean enhancementParalyze) {
		double radius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RADIUS, SIZE);
		double ratio = radius / SIZE;
		mCosmetic.castEffect(mPlayer, ratio);

		Hitbox hitbox = new Hitbox.SphereHitbox(location.clone().add(0, 0.9, 0), radius);
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

}
