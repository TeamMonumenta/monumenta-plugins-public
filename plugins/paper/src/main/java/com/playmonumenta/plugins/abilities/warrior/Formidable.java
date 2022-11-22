package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public class Formidable extends Ability {

	private static final double PASSIVE_KNOCKBACK_RESISTANCE = 0.2;

	public static final AbilityInfo<Formidable> INFO =
		new AbilityInfo<>(Formidable.class, null, Formidable::new)
			.canUse(player -> ScoreboardUtils.getScoreboardValue(player, "Class").orElse(0) == 2);

	public Formidable(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void setupClassPotionEffects() {
		AttributeInstance knockbackResistance = mPlayer.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
		if (knockbackResistance != null) {
			knockbackResistance.setBaseValue(knockbackResistance.getBaseValue() + PASSIVE_KNOCKBACK_RESISTANCE);
		}
	}
}
