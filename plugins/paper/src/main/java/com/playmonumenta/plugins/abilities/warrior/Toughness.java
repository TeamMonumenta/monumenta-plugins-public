package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
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
		return new DescriptionBuilder<>(() -> INFO)
			.add("Gain ")
			.addPercent(a -> a.mBaseHealthBoost, PERCENT_HEALTH_1, false, Ability::isLevelOne)
			.add(" max health and take ")
			.addPercent(a -> a.mDoTDamageReduction, DOT_DAMAGE_REDUCTION_1, false, Ability::isLevelOne)
			.add(" less damage from poison, wither, and drowning.");
	}

	private static Description<Toughness> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The max health is increased to ")
			.addPercent(a -> a.mBaseHealthBoost, PERCENT_HEALTH_2, false, Ability::isLevelTwo)
			.add(" and the damage reduction is increased to ")
			.addPercent(a -> a.mDoTDamageReduction, DOT_DAMAGE_REDUCTION_2, false, Ability::isLevelTwo)
			.add(".");
	}

	private static Description<Toughness> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Gain an additional ")
			.addPercent(PERCENT_HEALTH_ENHANCEMENT)
			.add(" max health. Additionally, when below ")
			.addPercent(a -> a.mHealthThreshold, HEALTH_THRESHHOLD)
			.add(" health, gain ")
			.addPercent(a -> a.mHealing, HEALING_INCREASE)
			.add(" healing.");
	}
}
