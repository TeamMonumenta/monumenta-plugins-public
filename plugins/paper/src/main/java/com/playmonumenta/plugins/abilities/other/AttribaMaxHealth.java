package com.playmonumenta.plugins.abilities.other;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;

public class AttribaMaxHealth extends Ability {
	public static final double REDUCTION = -0.75;
	public static final String MODIFIER_NAME = "AttribaMaxHealth";

	@Override
	public boolean canUse(Player player) {
		return player != null && player.getScoreboardTags().contains(MODIFIER_NAME);
	}

	public AttribaMaxHealth(Plugin plugin, Player player) {
		super(plugin, player, "AttribaMaxHealth");

		if (player != null) {
			EntityUtils.addAttribute(player, Attribute.GENERIC_MAX_HEALTH,
				new AttributeModifier(MODIFIER_NAME, REDUCTION, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}
}
