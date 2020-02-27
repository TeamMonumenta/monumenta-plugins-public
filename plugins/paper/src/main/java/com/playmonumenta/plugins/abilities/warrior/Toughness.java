package com.playmonumenta.plugins.abilities.warrior;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;

public class Toughness extends Ability {

	public static final int TOUGHNESS_1_HEALTH_BOOST = 4;
	public static final int TOUGHNESS_2_HEALTH_BOOST = 8;

	public Toughness(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Toughness");
		mInfo.scoreboardId = "Toughness";
		mInfo.mShorthandName = "Tgh";
		mInfo.mDescriptions.add("You gain 5 seconds of Haste III after killing a mob. You lose this buff when you hold a pickaxe.");
		mInfo.mDescriptions.add("The buff is improved to 5 seconds of Haste IV and Speed I. You only lose the Haste buff when holding a pickaxe.");
		if (player != null) {
			AttributeInstance maxHealth = mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH);
			int healthBoost = getAbilityScore() == 1 ? TOUGHNESS_1_HEALTH_BOOST : TOUGHNESS_2_HEALTH_BOOST;
			maxHealth.setBaseValue(maxHealth.getBaseValue() + healthBoost);
		}
	}
}
