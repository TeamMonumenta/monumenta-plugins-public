package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import javax.annotation.Nullable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class AttribaKnockbackResistance extends Ability {
	public static final double REDUCTION = -1.0;
	public static final String MODIFIER_NAME = "AttribaKnockbackResistance";

	@Override
	public boolean canUse(Player player) {
		return player != null && player.getScoreboardTags().contains(MODIFIER_NAME);
	}

	public AttribaKnockbackResistance(Plugin plugin, @Nullable Player player) {
		super(plugin, player, null);

		if (player != null) {
			EntityUtils.addAttribute(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE,
			                         new AttributeModifier(MODIFIER_NAME, REDUCTION, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}
}
