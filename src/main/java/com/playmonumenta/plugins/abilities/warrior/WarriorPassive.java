package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.Plugin;

import java.util.Random;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.World;

public class WarriorPassive extends Ability {

	private static final double PASSIVE_KNOCKBACK_RESISTANCE = 0.2;

	public WarriorPassive(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 2;
		mInfo.specId = -1;
	}

	@Override
	public void PlayerRespawnEvent() {
		AttributeInstance att = mPlayer.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
		att.setBaseValue(0);
		att.setBaseValue(PASSIVE_KNOCKBACK_RESISTANCE);
	}
}
