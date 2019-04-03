package com.playmonumenta.plugins.abilities.warrior;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;

public class Toughness extends Ability {

	public Toughness(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Toughness";
	}

	@Override
	public void setupClassPotionEffects() {
		int toughness = getAbilityScore();
		int healthBoost = toughness == 1 ? 0 : 1;
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.HEALTH_BOOST, 1000000, healthBoost, true, false));
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.REGENERATION, 100, 4, true, false));
	}
}
