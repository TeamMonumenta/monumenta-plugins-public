package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.ToughnessCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.perLevel;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.GREY;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class Toughness extends Ability {

	public static final double PERCENT_HEALTH_1 = 0.1;
	public static final double PERCENT_HEALTH_2 = 0.2;
	public static final double PERCENT_HEALTH_ENHANCEMENT = 0.05;
	public static final double DOT_DAMAGE_REDUCTION_1 = 0.2;
	public static final double DOT_DAMAGE_REDUCTION_2 = 0.40;
	public static final double HEALTH_THRESHHOLD = 0.5;
	public static final double HEALING_INCREASE = 0.2;
	public static final String TOUGHNESS_MODIFIER_NAME = "ToughnessPercentHealthModifier";

	public static final String CHARM_HEALTH = "Toughness Health";
	public static final String CHARM_REDUCTION = "Toughness Damage Reduction";
	public static final String CHARM_HEALING = "Toughness Healing Increase";
	public static final String CHARM_HEALTH_THRESHOLD = "Toughness Health Threshold";

	public static final AbilityInfo<Toughness> INFO =
		new AbilityInfo<>(Toughness.class, "Toughness", Toughness::new)
			.scoreboardId("Toughness")
			.shorthandName("Tgh")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Increase your max health and resistance to ailment damage.")
			.remove(player -> EntityUtils.removeAttribute(player, Attribute.GENERIC_MAX_HEALTH, TOUGHNESS_MODIFIER_NAME))
			.displayItem(Material.IRON_HELMET);

	private final double mBaseHealthBoost;
	private final double mHealthBoost;
	private final double mDoTDamageReduction;
	private final double mHealing;
	private final double mHealthThreshold;
	private final ToughnessCS mCosmetic;

	public Toughness(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mBaseHealthBoost = (isLevelOne() ? PERCENT_HEALTH_1 : PERCENT_HEALTH_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_HEALTH);
		mHealthBoost = mBaseHealthBoost + (isEnhanced() ? PERCENT_HEALTH_ENHANCEMENT : 0);
		mDoTDamageReduction = (isLevelOne() ? DOT_DAMAGE_REDUCTION_1 : DOT_DAMAGE_REDUCTION_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_REDUCTION);
		mHealing = HEALING_INCREASE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_HEALING);
		mHealthThreshold = HEALTH_THRESHHOLD + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_HEALTH_THRESHOLD);

		EntityUtils.addAttribute(player, Attribute.GENERIC_MAX_HEALTH,
			new AttributeModifier(TOUGHNESS_MODIFIER_NAME, mHealthBoost, AttributeModifier.Operation.MULTIPLY_SCALAR_1));

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ToughnessCS());
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.getType() == DamageType.AILMENT || event.getType() == DamageType.POISON || event.getCause() == EntityDamageEvent.DamageCause.DROWNING) {
			event.setFlatDamage(event.getDamage() * (1 - mDoTDamageReduction));
		}
	}

	@Override
	public void playerRegainHealthEvent(EntityRegainHealthEvent event) {
		if (isEnhanced() && mPlayer.getHealth() <= EntityUtils.getMaxHealth(mPlayer) * mHealthThreshold) {
			event.setAmount(event.getAmount() * (1 + mHealing));
			mCosmetic.toughnessEnhancement(mPlayer);
		}
	}

	private static Description<Toughness> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Gain increased max health and")
			.addLine("take less damage from drowning,")
			.addLine("poison, and wither.")
			.addLine()
			.addStat("Effect: +%p1e Max Health")
				.statValues(stat(a -> a.mBaseHealthBoost, PERCENT_HEALTH_1))
			.addStat("Effect: +%p1 Ailment Resistance")
				.statValues(stat(a -> a.mDoTDamageReduction, DOT_DAMAGE_REDUCTION_1))
			.addDashedLine();
	}

	private static Description<Toughness> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Toughness*'s health boost and").styles(UNDERLINED)
			.addLine("ailment resistance.")
			.addLine()
			.addStatComparison("Effect: +%p1e -> +%p2e Max Health")
				.statValues(stat(PERCENT_HEALTH_1), stat(a -> a.mBaseHealthBoost, PERCENT_HEALTH_2))
			.addStatComparison("Effect: +%p1 -> +%p2 Ailment Resistance")
				.statValues(stat(DOT_DAMAGE_REDUCTION_1), stat(a -> a.mDoTDamageReduction, DOT_DAMAGE_REDUCTION_2))
			.addDashedLine();
	}

	private static Description<Toughness> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Increase *Toughness*'s health boost even").styles(UNDERLINED)
			.addLine("further.")
			.addLine()
			.addLine("Gain increased healing while at low HP.")
			.addLine()
			.addStatComparison("Effect: +%p1e -> +%p3 Max Health")
				.statValues(perLevel(PERCENT_HEALTH_1, PERCENT_HEALTH_2), perLevel(a -> a.mHealthBoost, PERCENT_HEALTH_1 + PERCENT_HEALTH_ENHANCEMENT, PERCENT_HEALTH_2 + PERCENT_HEALTH_ENHANCEMENT))
			.addStat("Effect: +%p Healing *while below* %p HP").styles(GREY)
				.statValues(stat(a -> a.mHealing, HEALING_INCREASE), stat(a -> a.mHealthThreshold, HEALTH_THRESHHOLD))
			.addDashedLine();
	}
}
