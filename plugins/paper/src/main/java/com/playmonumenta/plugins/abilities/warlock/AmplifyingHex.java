package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.AmplifyingHexCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Inferno;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;


public class AmplifyingHex extends Ability {

	private static final float FLAT_DAMAGE = 2f;
	private static final float DAMAGE_PER_SKILL_POINT = 0.5f;
	private static final int AMPLIFIER_DAMAGE_1 = 1;
	private static final int AMPLIFIER_DAMAGE_2 = 2;
	private static final int AMPLIFIER_CAP_1 = 2;
	private static final int AMPLIFIER_CAP_2 = 3;
	private static final float R1_CAP = 3.5f;
	private static final float R2_CAP = 7f;
	private static final float R3_CAP = 10.5f;
	private static final int RADIUS_1 = 8;
	private static final int RADIUS_2 = 10;
	private static final double ANGLE = 70;
	private static final int COOLDOWN = 20 * 10;
	private static final float KNOCKBACK_SPEED = 0.12f;
	private static final double ENHANCEMENT_HEALTH_THRESHOLD = 0.8;
	private static final double ENHANCEMENT_DAMAGE_MOD = 1.25;

	public static final String CHARM_DAMAGE = "Amplifying Hex Damage";
	public static final String CHARM_RANGE = "Amplifying Hex Range";
	public static final String CHARM_COOLDOWN = "Amplifying Hex Cooldown";
	public static final String CHARM_CONE = "Amplifying Hex Cone";
	public static final String CHARM_POTENCY = "Amplifying Hex Damage per Effect Potency";
	public static final String CHARM_POTENCY_CAP = "Amplifying Hex Potency Cap";
	public static final String CHARM_ENHANCE_HEALTH = "Amplifying Hex Enhancement Health Threshold";
	public static final String CHARM_ENHANCE_DAMAGE = "Amplifying Hex Enhancement Damage Bonus";

	public static final AbilityInfo<AmplifyingHex> INFO =
		new AbilityInfo<>(AmplifyingHex.class, "Amplifying Hex", AmplifyingHex::new)
			.linkedSpell(ClassAbility.AMPLIFYING)
			.scoreboardId("AmplifyingHex")
			.shorthandName("AH")
			.descriptions(
				"Left click while sneaking with a scythe to unleash a magic cone up to 8 blocks in front of you, " +
					"dealing magic damage equal to 2 + (0.5 * Player Level, capped based on region: R1 Level 7 / R2 Level 14 / R3 Level 21) " +
					"to each enemy for each debuff it has. Debuffs include, but are not limited to, Fire, Slowness, Weaken, Decay, Bleed, and more. " +
					"Deal an additional +1 damage for each extra level of debuff above 1, capped at 2 extra levels per debuff. 20% Slowness, Decay 2, etc. count as level 2 debuffs. Cooldown: 10s.",
				"Range increased to 10 blocks. The extra debuff level damage is increased to +2 per extra level, and the extra debuff level cap is increased to 3 extra levels.",
				"For every 1% health you have above 80% of your max health, Amplifying Hex will deal 1.25% more damage to enemies and deal 1% max health damage to yourself.")
			.simpleDescription("Deal damage to mobs in front of you for each debuff they currently have.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", AmplifyingHex::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(Material.DRAGON_BREATH);

	private final float mAmplifierDamage;
	private final int mAmplifierCap;
	private final float mRadius;
	private final double mConeAngle;
	private final float mRegionCap;
	private float mDamage = 0f;
	private final double mEnhanceHealthThreshold;
	private final double mEnhanceDamageBonus;

	private final AmplifyingHexCS mCosmetic;

	public AmplifyingHex(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mAmplifierDamage = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_POTENCY, isLevelOne() ? AMPLIFIER_DAMAGE_1 : AMPLIFIER_DAMAGE_2);
		mAmplifierCap = (int) CharmManager.calculateFlatAndPercentValue(player, CHARM_POTENCY_CAP, isLevelOne() ? AMPLIFIER_CAP_1 : AMPLIFIER_CAP_2);
		mRadius = (float) CharmManager.getRadius(player, CHARM_RANGE, isLevelOne() ? RADIUS_1 : RADIUS_2);
		mConeAngle = Math.min(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CONE, ANGLE), 180);
		mRegionCap = ServerProperties.getAbilityEnhancementsEnabled(player) ? R3_CAP : ServerProperties.getClassSpecializationsEnabled(player) ? R2_CAP : R1_CAP;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new AmplifyingHexCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			int charmPower = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.CHARM_POWER).orElse(0);
			charmPower = (charmPower > 0) ? (charmPower / 3) - 2 : 0;
			int totalLevel = AbilityUtils.getEffectiveTotalSkillPoints(player) +
				                 AbilityUtils.getEffectiveTotalSpecPoints(player) +
				                 ScoreboardUtils.getScoreboardValue(player, AbilityUtils.TOTAL_ENHANCE).orElse(0) +
				                 charmPower;
			mDamage = DAMAGE_PER_SKILL_POINT * totalLevel;
		});

		mEnhanceHealthThreshold = ENHANCEMENT_HEALTH_THRESHOLD + CharmManager.getLevelPercentDecimal(player, CHARM_ENHANCE_HEALTH);
		mEnhanceDamageBonus = CharmManager.calculateFlatAndPercentValue(player, CHARM_ENHANCE_DAMAGE, ENHANCEMENT_DAMAGE_MOD);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		mCosmetic.onCast(mPlayer, mRadius, mConeAngle);

		double maxHealth = EntityUtils.getMaxHealth(mPlayer);
		double percentBoost = 0;
		if (isEnhanced() && mPlayer.getHealth() > maxHealth * mEnhanceHealthThreshold) {
			percentBoost = mPlayer.getHealth() / maxHealth - mEnhanceHealthThreshold;
			double selfHarm = maxHealth * percentBoost;
			double absorp = mPlayer.getAbsorptionAmount();
			double newAbsorp = absorp - selfHarm;
			if (absorp > 0) {
				AbsorptionUtils.setAbsorption(mPlayer, (float) Math.max(newAbsorp, 0), -1);
			}
			if (newAbsorp < 0) {
				mPlayer.setHealth(maxHealth + newAbsorp);
			}
			//dummy damage
			DamageUtils.damage(null, mPlayer, new DamageEvent.Metadata(DamageType.OTHER, null, null, null), 0.001, true, false, false);

			//multiply percent boost modifier
			percentBoost *= mEnhanceDamageBonus;
		}

		Hitbox hitbox = Hitbox.approximateCylinderSegment(LocationUtils.getHalfHeightLocation(mPlayer).add(0, -mRadius, 0), 2 * mRadius, mRadius, Math.toRadians(mConeAngle));
		for (LivingEntity mob : hitbox.getHitMobs()) {
			int debuffCount = 0;
			int amplifierCount = 0;
			for (PotionEffectType effectType : AbilityUtils.DEBUFFS) {
				PotionEffect effect = mob.getPotionEffect(effectType);
				if (effect != null) {
					debuffCount++;
					amplifierCount += Math.min(mAmplifierCap, effect.getAmplifier());
				}
			}

			int inferno = Inferno.getInfernoLevel(mPlugin, mob);
			if (mob.getFireTicks() > 0 || inferno > 0) {
				debuffCount++;
				amplifierCount += Math.min(mAmplifierCap, inferno);
			}

			if (EntityUtils.isStunned(mob)) {
				debuffCount++;
			}

			if (EntityUtils.isParalyzed(mPlugin, mob)) {
				debuffCount++;
			}

			if (EntityUtils.isSilenced(mob)) {
				debuffCount++;
			}

			if (EntityUtils.isBleeding(mPlugin, mob)) {
				debuffCount++;
				amplifierCount += Math.min(mAmplifierCap, EntityUtils.getBleedLevel(mPlugin, mob) - 1);
			}

			//Custom slow effect interaction
			if (EntityUtils.isSlowed(mPlugin, mob) && mob.getPotionEffect(PotionEffectType.SLOW) == null) {
				debuffCount++;
				double slowAmp = EntityUtils.getSlowAmount(mPlugin, mob);
				int slowLevel = (int) Math.floor(slowAmp * 10);
				amplifierCount += Math.min((int) mAmplifierCap, Math.max(slowLevel - 1, 0));
			}

			//Custom weaken interaction
			if (EntityUtils.isWeakened(mPlugin, mob)) {
				debuffCount++;
				double weakAmp = EntityUtils.getWeakenAmount(mPlugin, mob);
				int weakLevel = (int) Math.floor(weakAmp * 10);
				amplifierCount += Math.min(mAmplifierCap, Math.max(weakLevel - 1, 0));
			}

			//Custom vuln interaction
			if (EntityUtils.isVulnerable(mPlugin, mob)) {
				debuffCount++;
				double vulnAmp = EntityUtils.getVulnAmount(mPlugin, mob);
				amplifierCount += Math.min(mAmplifierCap, Math.max((int) Math.floor(vulnAmp * 10) - 1, 0));
			}

			//Custom DoT interaction
			if (EntityUtils.hasDamageOverTime(mPlugin, mob)) {
				debuffCount++;
				int dotLevel = (int) EntityUtils.getHighestDamageOverTime(mPlugin, mob);
				amplifierCount += Math.min(mAmplifierCap, dotLevel - 1);
			}

			// Custom choleric flames antiheal interaction
			if (mPlugin.mEffectManager.getActiveEffect(mob, CholericFlames.ANTIHEAL_EFFECT) != null) {
				debuffCount++;
			}

			if (debuffCount > 0) {
				mCosmetic.onHit(mPlayer, mob);

				double finalDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, debuffCount * (FLAT_DAMAGE + Math.min(mDamage, mRegionCap)) + amplifierCount * mAmplifierDamage);
				finalDamage *= (1 + percentBoost);
				DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, finalDamage, mInfo.getLinkedSpell(), true);
				MovementUtils.knockAway(mPlayer, mob, KNOCKBACK_SPEED, true);
			}
		}
		putOnCooldown();
		return true;
	}

}
