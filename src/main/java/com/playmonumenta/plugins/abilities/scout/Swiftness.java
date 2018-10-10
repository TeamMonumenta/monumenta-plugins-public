package com.playmonumenta.plugins.abilities.scout;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;

public class Swiftness extends Ability {

	private static final int SWIFTNESS_EFFECT_SPEED_LVL = 0;
	private static final int SWIFTNESS_EFFECT_JUMP_LVL = 2;
	
	public Swiftness(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 6;
		mInfo.specId = -1;
		mInfo.scoreboardId = "Swiftness";
	}
	
	@Override
	public void setupClassPotionEffects() {
		testForSwiftness();
	}

	@Override
	public void PlayerRespawnEvent() {
		testForSwiftness();
	}
	
	public void testForSwiftness() {
		int swiftness = getAbilityScore();
		if (swiftness > 0) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.SPEED, 1000000, SWIFTNESS_EFFECT_SPEED_LVL, true, false));

			if (swiftness > 1) {
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
				                                 new PotionEffect(PotionEffectType.JUMP, 1000000, SWIFTNESS_EFFECT_JUMP_LVL, true, false));
			}
		}
	}

}
