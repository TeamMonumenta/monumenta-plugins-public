package com.playmonumenta.plugins.abilities.warrior;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Toughness extends Ability {

	public static final double PERCENT_HEALTH_1 = 0.1;
	public static final double PERCENT_HEALTH_2 = 0.2;
	public static final double DOT_DAMAGE_REDUCTION_1 = 0.25;
	public static final double DOT_DAMAGE_REDUCTION_2 = 0.50;
	public static final String TOUGHNESS_MODIFIER_NAME = "ToughnessPercentHealthModifier";

	private final double mDoTDamageReduction;

	public Toughness(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Toughness");
		mInfo.mScoreboardId = "Toughness";
		mInfo.mShorthandName = "Tgh";
		mInfo.mDescriptions.add("Gain +10% max health and damage from Poison, Wither, and Drowning is reduced by 25%.");
		mInfo.mDescriptions.add("Gain +20% max health and damage from Poison, Wither, and Drowning is reduced by 50%.");
		mDisplayItem = new ItemStack(Material.IRON_HELMET, 1);
		mDoTDamageReduction = getAbilityScore() == 1 ? DOT_DAMAGE_REDUCTION_1 : DOT_DAMAGE_REDUCTION_2;

		if (player != null) {
			double healthBoost = getAbilityScore() == 1 ? PERCENT_HEALTH_1 : PERCENT_HEALTH_2;
			EntityUtils.addAttribute(player, Attribute.GENERIC_MAX_HEALTH,
					new AttributeModifier(TOUGHNESS_MODIFIER_NAME, healthBoost, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}

	@Override
	public boolean playerDamagedEvent(EntityDamageEvent event) {
		if (event.getCause() == DamageCause.POISON || event.getCause() == DamageCause.WITHER || event.getCause() == DamageCause.DROWNING) {
			event.setDamage(event.getDamage() * (1 - mDoTDamageReduction));
		}

		return true;
	}

}
