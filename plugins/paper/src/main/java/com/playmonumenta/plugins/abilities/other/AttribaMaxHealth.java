package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class AttribaMaxHealth extends Ability {
	public static final double REDUCTION = -0.75;
	public static final String MODIFIER_NAME = "AttribaMaxHealth";

	public static final AbilityInfo<AttribaMaxHealth> INFO =
		new AbilityInfo<>(AttribaMaxHealth.class, null, AttribaMaxHealth::new)
			.canUse(player -> player != null && player.getScoreboardTags().contains(MODIFIER_NAME));

	public AttribaMaxHealth(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		if (player != null) {
			EntityUtils.addAttribute(player, Attribute.GENERIC_MAX_HEALTH,
				new AttributeModifier(MODIFIER_NAME, REDUCTION, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}
}
