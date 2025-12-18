package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.AmplifyingHexCS;
import com.playmonumenta.plugins.effects.Bleed;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.effects.EffectManager;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;


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
	// Only for charm design:
	private static final int MAX_DEBUFFS = 1000;

	public static final String CHARM_DAMAGE = "Amplifying Hex Damage";
	public static final String CHARM_RANGE = "Amplifying Hex Range";
	public static final String CHARM_COOLDOWN = "Amplifying Hex Cooldown";
	public static final String CHARM_CONE = "Amplifying Hex Cone";
	public static final String CHARM_POTENCY = "Amplifying Hex Damage per Effect Potency";
	public static final String CHARM_POTENCY_CAP = "Amplifying Hex Potency Cap";
	public static final String CHARM_ENHANCE_HEALTH = "Amplifying Hex Enhancement Health Threshold";
	public static final String CHARM_ENHANCE_DAMAGE = "Amplifying Hex Enhancement Damage Bonus";
	public static final String CHARM_MAX_DEBUFFS = "Amplifying Hex Max Debuffs";

	public static final AbilityInfo<AmplifyingHex> INFO =
		new AbilityInfo<>(AmplifyingHex.class, "Amplifying Hex", AmplifyingHex::new)
			.linkedSpell(ClassAbility.AMPLIFYING)
			.scoreboardId("AmplifyingHex")
			.shorthandName("AH")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Deal damage to mobs in front of you for each debuff they currently have.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", AmplifyingHex::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(Material.DRAGON_BREATH);

	private final double mFlatDamage;
	private final double mAmplifierDamage;
	private final double mRegionCap;
	private final int mAmplifierCap;
	private final double mDamagePerPoint;
	private double mDamage = 0;
	private final double mRadius;
	private final double mConeAngle;
	private final double mEnhanceHealthThreshold;
	private final double mEnhanceDamageBonus;
	private final int mMaxDebuffs;

	private final AmplifyingHexCS mCosmetic;

	public AmplifyingHex(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mFlatDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, FLAT_DAMAGE);
		mDamagePerPoint = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE_PER_SKILL_POINT);
		mRegionCap = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, ServerProperties.getAbilityEnhancementsEnabled(player) ? R3_CAP : ServerProperties.getClassSpecializationsEnabled(player) ? R2_CAP : R1_CAP);
		mAmplifierDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, 1) * CharmManager.calculateFlatAndPercentValue(player, CHARM_POTENCY, isLevelOne() ? AMPLIFIER_DAMAGE_1 : AMPLIFIER_DAMAGE_2);
		mAmplifierCap = (isLevelOne() ? AMPLIFIER_CAP_1 : AMPLIFIER_CAP_2) + (int) CharmManager.getLevel(player, CHARM_POTENCY_CAP);
		mRadius = CharmManager.getRadius(player, CHARM_RANGE, isLevelOne() ? RADIUS_1 : RADIUS_2);
		mConeAngle = Math.min(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CONE, ANGLE), 180);
		mEnhanceHealthThreshold = ENHANCEMENT_HEALTH_THRESHOLD + CharmManager.getLevelPercentDecimal(player, CHARM_ENHANCE_HEALTH);
		mEnhanceDamageBonus = CharmManager.calculateFlatAndPercentValue(player, CHARM_ENHANCE_DAMAGE, ENHANCEMENT_DAMAGE_MOD);
		// Special implementation: Set to 1000, unless charmed, in which case set to whatever the charm stat is
		mMaxDebuffs = (int) CharmManager.calculateFlatAndPercentValue(player, CHARM_MAX_DEBUFFS, MAX_DEBUFFS) - (CharmManager.calculateFlatAndPercentValue(player, CHARM_MAX_DEBUFFS, MAX_DEBUFFS) != MAX_DEBUFFS ? MAX_DEBUFFS : 0);

		Bukkit.getScheduler().runTask(plugin, () -> {
			mDamage = mFlatDamage + Math.min(mDamagePerPoint * AbilityUtils.getPlayerLevel(mPlayer), mRegionCap);
		});

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new AmplifyingHexCS());
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
			DamageUtils.damage(null, mPlayer, new DamageEvent.Metadata(DamageType.TRUE, null, null, null), 0.001, true, false, false);

			//multiply percent boost modifier
			percentBoost *= mEnhanceDamageBonus;
		}

		Hitbox hitbox = Hitbox.approximateCylinderSegment(LocationUtils.getHalfHeightLocation(mPlayer).add(0, -mRadius, 0), 2 * mRadius, mRadius, Math.toRadians(mConeAngle));
		for (LivingEntity mob : hitbox.getHitMobs()) {
			int debuffCount = 0;
			ArrayList<Integer> amplifierCounts = new ArrayList<>();

			// Potion effect debuffs. We avoid stream for speed (hopefully)
			for (PotionEffect e : mob.getActivePotionEffects()) {
				if (AbilityUtils.DEBUFFS.contains(e.getType())) {
					debuffCount++;
					amplifierCounts.add(Math.min(mAmplifierCap, e.getAmplifier()));
				}
			}

			// Other debuffs
			List<EffectManager.EffectPair> unfilteredEffectPairList = EffectManager.getInstance().getEffectPairs(mob);
			Map<String, Double> effectPairList = new HashMap<>();
			Map<String, Double> effectList = new HashMap<>();

			if (unfilteredEffectPairList != null) {
				for (EffectManager.EffectPair e : unfilteredEffectPairList) {
					effectPairList.put(e.mSource(), e.mEffect().getMagnitude());
					effectList.put(e.mEffect().mEffectID, Math.max(e.mEffect().getMagnitude(),
						effectList.getOrDefault(e.mEffect().mEffectID, 0.0)));
				}
			}

			Double inferno = effectPairList.get(Inferno.INFERNO_EFFECT_NAME);
			if (inferno != null) {
				debuffCount++;
				amplifierCounts.add(Math.min(mAmplifierCap, inferno.intValue()));
			} else if (mob.getFireTicks() > 0) {
				debuffCount++;
			}

			if (EntityUtils.isStunned(mob)) {
				debuffCount++;
			}

			if (EntityUtils.isStaggered(mob)) {
				debuffCount++;
			}

			if (effectPairList.containsKey(EntityUtils.PARALYZE_EFFECT_NAME)) {
				debuffCount++;
			}

			if (EntityUtils.isSilenced(mob)) {
				debuffCount++;
			}

			Double bleed = effectPairList.get(Bleed.BLEED_EFFECT_NAME);
			if (bleed != null) {
				debuffCount++;
				amplifierCounts.add(Math.min(mAmplifierCap, bleed.intValue() - 1));
			}

			//Custom slow effect interaction
			Double slow = effectPairList.get(EntityUtils.SLOW_EFFECT_NAME);
			if (slow != null && mob.getPotionEffect(PotionEffectType.SLOW) == null) {
				debuffCount++;
				int slowLevel = (int) Math.floor(slow * 10);
				amplifierCounts.add(Math.min(mAmplifierCap, Math.max(slowLevel - 1, 0)));
			}

			//Custom weaken interaction
			Double weaken = effectPairList.get(EntityUtils.WEAKEN_EFFECT_NAME);
			if (weaken != null) {
				debuffCount++;
				int weakLevel = (int) Math.floor(weaken * 10);
				amplifierCounts.add(Math.min(mAmplifierCap, Math.max(weakLevel - 1, 0)));
			}

			//Custom vuln interaction
			Double vulnerable = effectPairList.get(EntityUtils.VULNERABILITY_EFFECT_NAME);
			if (vulnerable != null) {
				debuffCount++;
				int vulnLevel = (int) Math.floor(vulnerable * 10);
				amplifierCounts.add(Math.min(mAmplifierCap, Math.max(vulnLevel - 1, 0)));
			}

			//Custom DoT interaction
			Double dot = effectList.get(CustomDamageOverTime.effectID);
			if (dot != null) {
				debuffCount++;
				amplifierCounts.add((int) Math.min(mAmplifierCap, dot - 1));
			}

			// Custom choleric flames antiheal interaction
			Double cholericFlames = effectPairList.get(CholericFlames.ANTIHEAL_EFFECT);
			if (cholericFlames != null) {
				debuffCount++;
			}

			if (debuffCount > 0) {
				mCosmetic.onHit(mPlayer, mob);
				debuffCount = Math.min(debuffCount, mMaxDebuffs);
				int finalDebuffCount = debuffCount;
				int amplifierCount = 0;

				amplifierCounts.sort(Comparator.naturalOrder());
				while (debuffCount > 0 && !amplifierCounts.isEmpty()) {
					amplifierCount += amplifierCounts.get(amplifierCounts.size() - 1);
					amplifierCounts.remove(amplifierCounts.size() - 1);
					debuffCount--;
				}

				double finalDamage = (finalDebuffCount * mDamage + amplifierCount * mAmplifierDamage) * (1 + percentBoost);
				DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, finalDamage, mInfo.getLinkedSpell(), true);
				MovementUtils.knockAway(mPlayer, mob, KNOCKBACK_SPEED, true);
			}
		}
		putOnCooldown();
		return true;
	}

	private static Description<AmplifyingHex> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Unleash a hex that damages mobs in front")
			.addLine("of you for each debuff they have, plus")
			.addLine("bonus damage for debuffs above level 1.")
			.addLine("(Fire, Slowness, Weakness, Bleed, etc.)")
			.addLine()
			.addStat("Damage: %d (s) per debuff")
				.statValues(stat((a, p) -> a.mDamage, (a, p) -> {
					double regionCap = switch (ServerProperties.getRegion(p)) {
						case RING -> R3_CAP;
						case ISLES -> R2_CAP;
						default -> R1_CAP;
					};
					return FLAT_DAMAGE + Math.min(DAMAGE_PER_SKILL_POINT * AbilityUtils.getPlayerLevel(p), regionCap);
				}))
			.tab().addLine("+%d1 per *extra debuff level* (max level %d1)").styles(WHITE)
				.statValues(stat(a -> a.mAmplifierDamage, AMPLIFIER_DAMAGE_1), stat(a -> a.mAmplifierCap + 1, AMPLIFIER_CAP_1 + 1))
			.addStat("Radius: %r1 (Cone-Shaped)")
				.statValues(stat(a -> a.mRadius, RADIUS_1))
			.addStat("Cooldown: %t")
				.statValues(cooldown(COOLDOWN))
			.addDashedLine();
	}

	private static Description<AmplifyingHex> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Amplifying Hex*'s bonus damage per extra").styles(UNDERLINED)
			.addLine("debuff level and its radius.")
			.addLine()
			.addStatComparison("Damage: +%d1 -> +%d2 per extra debuff level (max level %d2)")
				.statValues(stat(AMPLIFIER_DAMAGE_1), stat(a -> a.mAmplifierDamage, AMPLIFIER_DAMAGE_2), stat(a -> a.mAmplifierCap + 1, AMPLIFIER_CAP_2 + 1))
			.addStatComparison("Radius: %r1 -> %r2")
				.statValues(stat(RADIUS_1), stat(a -> a.mRadius, RADIUS_2))
			.addDashedLine();
	}

	private static Description<AmplifyingHex> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("If you're above %p HP, *Amplifying Hex*").styles(UNDERLINED)
				.statValues(stat(a -> a.mEnhanceHealthThreshold, ENHANCEMENT_HEALTH_THRESHOLD))
			.addLine("will reduce your health to %p and deal more")
				.statValues(stat(a -> a.mEnhanceHealthThreshold, ENHANCEMENT_HEALTH_THRESHOLD))
			.addLine("damage the more health you lost.")
			.addLine()
			.addStat("Damage Boost: +%p (s) per 1% HP lost")
				.statValues(stat(a -> a.mEnhanceDamageBonus / 100, ENHANCEMENT_DAMAGE_MOD / 100))
			.addDashedLine();
	}
}
