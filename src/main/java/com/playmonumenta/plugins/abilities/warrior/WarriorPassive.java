package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public class WarriorPassive extends Ability {

	private static final double PASSIVE_KNOCKBACK_RESISTANCE = 0.2;

	@Override
	public void PlayerRespawnEvent(Player player) {
		AttributeInstance att = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
		att.setBaseValue(0);
		att.setBaseValue(PASSIVE_KNOCKBACK_RESISTANCE);
	}

	@Override
	public AbilityInfo getInfo() {
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 2;
		info.specId = -1;
		return info;
	}

}
