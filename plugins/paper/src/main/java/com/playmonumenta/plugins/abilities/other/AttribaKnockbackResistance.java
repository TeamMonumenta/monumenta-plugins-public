package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class AttribaKnockbackResistance extends Ability {
	public static final double REDUCTION = -1.0;
	public static final String MODIFIER_NAME = "AttribaKnockbackResistance";

	public static final AbilityInfo<AttribaKnockbackResistance> INFO =
		new AbilityInfo<>(AttribaKnockbackResistance.class, null, AttribaKnockbackResistance::new)
			.canUse(player -> player != null && player.getScoreboardTags().contains(MODIFIER_NAME))
			.ignoresSilence(true);

	public AttribaKnockbackResistance(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		if (player != null) {
			EntityUtils.addAttribute(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE,
				new AttributeModifier(MODIFIER_NAME, REDUCTION, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}
}
