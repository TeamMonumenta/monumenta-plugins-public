package com.playmonumenta.plugins.abilities.warrior;

import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class WarriorPassive extends Ability {

	private static final double PASSIVE_KNOCKBACK_RESISTANCE = 0.2;

	public WarriorPassive(Plugin plugin, World world, Player player) {
		super(plugin, world, player, null);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class") == 2;
	}

	@Override
	public void setupClassPotionEffects() {
		AttributeInstance att = mPlayer.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
		att.setBaseValue(att.getBaseValue() + PASSIVE_KNOCKBACK_RESISTANCE);
	}
}
