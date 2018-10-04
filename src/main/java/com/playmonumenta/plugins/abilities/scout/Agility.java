package com.playmonumenta.plugins.abilities.scout;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;

public class Agility extends Ability {

	private static final int AGILITY_1_EFFECT_LVL = 0;
	private static final int AGILITY_2_EFFECT_LVL = 1;
	private static final int AGILITY_1_DAMAGE_BONUS = 1;
	private static final int AGILITY_2_DAMAGE_BONUS = 2;
	
	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) { 
		int extraDamage = (getAbilityScore(player) == 1) ? AGILITY_1_DAMAGE_BONUS : AGILITY_2_DAMAGE_BONUS;
		event.setDamage(event.getDamage() + extraDamage);
		return true;
	}
	
	@Override
	public void setupClassPotionEffects(Player player) {
		testForAgility(player);
	}
	
	@Override
	public void PlayerRespawnEvent(Player player) {
		testForAgility(player);
	}
	
	public void testForAgility(Player player) {
		int effectLevel = getAbilityScore(player) == 1 ? AGILITY_1_EFFECT_LVL : AGILITY_2_EFFECT_LVL;
		mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF,
		                                 new PotionEffect(PotionEffectType.FAST_DIGGING, 1000000, effectLevel, true, false));
	}
	
	@Override
	public AbilityInfo getInfo() {
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 6;
		info.specId = -1;
		info.scoreboardId = "Agility";
		return info;
	}
	
}
