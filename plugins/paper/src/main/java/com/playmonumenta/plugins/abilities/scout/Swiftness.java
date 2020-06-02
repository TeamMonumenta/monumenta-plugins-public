package com.playmonumenta.plugins.abilities.scout;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;

public class Swiftness extends Ability {

	private static final int SWIFTNESS_EFFECT_SPEED_LVL = 0;
	private static final int SWIFTNESS_EFFECT_JUMP_LVL = 2;

	public Swiftness(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Swiftness");
		mInfo.scoreboardId = "Swiftness";
		mInfo.mShorthandName = "Swf";
		mInfo.mDescriptions.add("You gain permanent Speed I.");
		mInfo.mDescriptions.add("In addition, you gain Jump Boost III while you are not inside a town.");
	}

	@Override
	public void setupClassPotionEffects() {
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
		                                 new PotionEffect(PotionEffectType.SPEED, 1000000, SWIFTNESS_EFFECT_SPEED_LVL, true, false));

		int swiftness = getAbilityScore();
		if (swiftness > 1) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.JUMP, 1000000, SWIFTNESS_EFFECT_JUMP_LVL, true, false));
		}
	}

}
