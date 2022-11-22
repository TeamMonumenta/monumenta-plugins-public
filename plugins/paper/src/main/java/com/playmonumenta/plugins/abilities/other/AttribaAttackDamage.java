package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class AttribaAttackDamage extends Ability {
	public static final double REDUCTION = -0.75;
	public static final String MODIFIER_NAME = "AttribaAttackDamage";

	public static final AbilityInfo<AttribaAttackDamage> INFO =
		new AbilityInfo<>(AttribaAttackDamage.class, null, AttribaAttackDamage::new)
			.canUse(player -> player != null && player.getScoreboardTags().contains(MODIFIER_NAME));

	public AttribaAttackDamage(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		if (player != null) {
			EntityUtils.addAttribute(player, Attribute.GENERIC_ATTACK_DAMAGE,
				new AttributeModifier(MODIFIER_NAME, REDUCTION, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}
}
