package com.playmonumenta.plugins.abilities.warrior;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;

public class Toughness extends Ability {

	private void toughness(Player player) {
		int toughness = getAbilityScore(player);
		int healthBoost = toughness == 1 ? 0 : 1;
		mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.HEALTH_BOOST, 1000000, healthBoost, true, false));
		mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.REGENERATION, 100, 4, true, false));
	}
	
	@Override
	public void setupClassPotionEffects(Player player) { 
		toughness(player);
	}
	
	@Override
	public void PlayerRespawnEvent(Player player) { 
		toughness(player);
	}
	
	@Override
	public AbilityInfo getInfo() {
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 2;
		info.specId = -1;
		info.scoreboardId = "Toughness";
		return info;
	}
	
}
