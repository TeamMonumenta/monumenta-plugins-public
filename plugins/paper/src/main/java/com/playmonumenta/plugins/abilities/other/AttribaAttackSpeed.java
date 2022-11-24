package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class AttribaAttackSpeed extends Ability {
	public static final double REDUCTION = -0.5;
	public static final String MODIFIER_NAME = "AttribaAttackSpeed";

	public static final AbilityInfo<AttribaAttackSpeed> INFO =
		new AbilityInfo<>(AttribaAttackSpeed.class, null, AttribaAttackSpeed::new)
			.canUse(player -> player != null && player.getScoreboardTags().contains(MODIFIER_NAME))
			.ignoresSilence(true);

	public AttribaAttackSpeed(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		if (player != null) {
			EntityUtils.addAttribute(player, Attribute.GENERIC_ATTACK_SPEED,
				new AttributeModifier(MODIFIER_NAME, REDUCTION, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}
}
