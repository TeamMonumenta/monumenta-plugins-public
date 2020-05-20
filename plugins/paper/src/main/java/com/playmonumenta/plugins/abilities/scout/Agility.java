package com.playmonumenta.plugins.abilities.scout;


import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;

public class Agility extends Ability {

	private static final int AGILITY_1_EFFECT_LVL = 0;
	private static final int AGILITY_2_EFFECT_LVL = 1;
	private static final int AGILITY_1_DAMAGE_BONUS = 1;
	private static final int AGILITY_2_DAMAGE_BONUS = 2;

	private final int mDamageBonus;

	public Agility(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Agility");
		mInfo.scoreboardId = "Agility";
		mInfo.mShorthandName = "Agl";
		mInfo.mDescriptions.add("You gain permanent Haste I. Your melee attacks deal +1 extra damage.");
		mInfo.mDescriptions.add("You gain permanent Haste II. Your melee attacks deal +2 extra damage.");
		mDamageBonus = getAbilityScore() == 1 ? AGILITY_1_DAMAGE_BONUS : AGILITY_2_DAMAGE_BONUS;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() ==  DamageCause.ENTITY_ATTACK) {
			event.setDamage(event.getDamage() + mDamageBonus);
		}

		return true;
	}

	@Override
	public void setupClassPotionEffects() {
		int effectLevel = getAbilityScore() == 1 ? AGILITY_1_EFFECT_LVL : AGILITY_2_EFFECT_LVL;
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
		                                 new PotionEffect(PotionEffectType.FAST_DIGGING, 1000000, effectLevel, true, false));
	}
}
