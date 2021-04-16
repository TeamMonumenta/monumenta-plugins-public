package com.playmonumenta.plugins.abilities.scout;

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
	private static final int AGILITY_BONUS_DAMAGE = 1;
	private static final double SCALING_DAMAGE = 0.1;

	public Agility(Plugin plugin, Player player) {
		super(plugin, player, "Agility");
		mInfo.mScoreboardId = "Agility";
		mInfo.mShorthandName = "Agl";
		mInfo.mDescriptions.add("You gain permanent Haste I. Your melee attacks deal +1 extra damage.");
		mInfo.mDescriptions.add("You gain permanent Haste II. Increase melee damage by +1 plus 10% of final damage done.");
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() ==  DamageCause.ENTITY_ATTACK) {
			if (getAbilityScore() > 1) {
				event.setDamage((event.getDamage() + AGILITY_BONUS_DAMAGE) * (1 + SCALING_DAMAGE));
			} else {
				event.setDamage(event.getDamage() + AGILITY_BONUS_DAMAGE);
			}
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
