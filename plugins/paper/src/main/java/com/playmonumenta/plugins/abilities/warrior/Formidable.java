package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.Warrior;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class Formidable extends Ability {

	private static final double PASSIVE_KNOCKBACK_RESISTANCE = 0.2;
	private static final String MODIFIER_NAME = "FormidableKnockbackResistance";

	public static final AbilityInfo<Formidable> INFO =
		new AbilityInfo<>(Formidable.class, null, Formidable::new)
			.canUse(player -> ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME).orElse(0) == Warrior.CLASS_ID)
			.remove(player -> EntityUtils.removeAttribute(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE, MODIFIER_NAME));

	public Formidable(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		EntityUtils.addAttribute(mPlayer, Attribute.GENERIC_KNOCKBACK_RESISTANCE,
			new AttributeModifier(MODIFIER_NAME, PASSIVE_KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_NUMBER));
	}
}
