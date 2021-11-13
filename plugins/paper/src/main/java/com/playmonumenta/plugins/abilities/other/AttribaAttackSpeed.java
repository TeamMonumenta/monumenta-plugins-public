package com.playmonumenta.plugins.abilities.other;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;

public class AttribaAttackSpeed extends Ability {
	public static final double REDUCTION = -0.5;
	public static final String MODIFIER_NAME = "AttribaAttackSpeed";

	@Override
	public boolean canUse(Player player) {
		return player != null && player.getScoreboardTags().contains(MODIFIER_NAME);
	}

	public AttribaAttackSpeed(Plugin plugin, Player player) {
		super(plugin, player, "AttribaAttackSpeed");

		if (player != null) {
			EntityUtils.addAttribute(player, Attribute.GENERIC_ATTACK_SPEED,
				new AttributeModifier(MODIFIER_NAME, REDUCTION, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}
}
