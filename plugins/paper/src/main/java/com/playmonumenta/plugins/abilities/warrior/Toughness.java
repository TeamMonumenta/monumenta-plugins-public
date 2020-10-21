package com.playmonumenta.plugins.abilities.warrior;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Toughness extends Ability {

	public static final double PERCENT_HEALTH_1 = 0.1;
	public static final double PERCENT_HEALTH_2 = 0.2;
	public static final double DOT_DAMAGE_REDUCTION_1 = 0.33;
	public static final double DOT_DAMAGE_REDUCTION_2 = 0.66;
	public static final String TOUGHNESS_MODIFIER_NAME = "ToughnessPercentHealthModifier";

	private final double mDoTDamageReduction;

	public Toughness(Plugin plugin, Player player) {
		super(plugin, player, "Toughness");
		mInfo.mScoreboardId = "Toughness";
		mInfo.mShorthandName = "Tgh";
		mInfo.mDescriptions.add("Gain +10% max health and damage from Poison and Wither is reduced by 33%.");
		mInfo.mDescriptions.add("Gain +20% max health and damage from Poison and Wither is reduced by 66%.");
		mDoTDamageReduction = getAbilityScore() == 1 ? DOT_DAMAGE_REDUCTION_1 : DOT_DAMAGE_REDUCTION_2;

		if (player != null) {
			removeModifier(player);
			double healthBoost = getAbilityScore() == 1 ? PERCENT_HEALTH_1 : PERCENT_HEALTH_2;
			EntityUtils.addAttribute(player, Attribute.GENERIC_MAX_HEALTH,
					new AttributeModifier(TOUGHNESS_MODIFIER_NAME, healthBoost, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}

	@Override
	public boolean playerDamagedEvent(EntityDamageEvent event) {
		if (event.getCause() == DamageCause.POISON || event.getCause() == DamageCause.WITHER) {
			event.setDamage(event.getDamage() * (1 - mDoTDamageReduction));
		}

		return true;
	}

	public static void removeModifier(Player player) {
		EntityUtils.removeAttribute(player, Attribute.GENERIC_MAX_HEALTH, TOUGHNESS_MODIFIER_NAME);
	}
}
