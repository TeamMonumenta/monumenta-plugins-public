package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
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
import org.bukkit.inventory.ItemStack;
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

	public static final AbilityInfo<Toughness> INFO =
		new AbilityInfo<>(Toughness.class, "Toughness", Toughness::new)
			.scoreboardId("Toughness")
			.shorthandName("Tgh")
			.descriptions(
				"Gain +10% max health and damage from Poison, Wither, and Drowning is reduced by 20%.",
				"Gain +20% max health and damage from Poison, Wither, and Drowning is reduced by 40%.",
				"Gain an additional +5% max health. Additionally, when below 50% health, gain 20% healing.")
			.displayItem(new ItemStack(Material.IRON_HELMET, 1));

	private final double mDoTDamageReduction;

	public Toughness(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDoTDamageReduction = (isLevelOne() ? DOT_DAMAGE_REDUCTION_1 : DOT_DAMAGE_REDUCTION_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_REDUCTION);

		if (player != null) {
			double healthBoost = isLevelOne() ? PERCENT_HEALTH_1 : PERCENT_HEALTH_2;
			if (isEnhanced()) {
				healthBoost += PERCENT_HEALTH_ENHANCEMENT;
			}
			healthBoost += CharmManager.getLevelPercentDecimal(mPlayer, CHARM_HEALTH);
			EntityUtils.addAttribute(player, Attribute.GENERIC_MAX_HEALTH,
				new AttributeModifier(TOUGHNESS_MODIFIER_NAME, healthBoost, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.getType() == DamageType.AILMENT || event.getType() == DamageType.POISON || event.getCause() == EntityDamageEvent.DamageCause.DROWNING) {
			event.setDamage(event.getDamage() * (1 - mDoTDamageReduction));
		}
	}

	@Override
	public void playerRegainHealthEvent(EntityRegainHealthEvent event) {
		if (isEnhanced() && mPlayer.getHealth() <= EntityUtils.getMaxHealth(mPlayer) * HEALTH_THRESHHOLD) {
			event.setAmount(event.getAmount() * (1 + HEALING_INCREASE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_HEALING)));
		}
	}

}
