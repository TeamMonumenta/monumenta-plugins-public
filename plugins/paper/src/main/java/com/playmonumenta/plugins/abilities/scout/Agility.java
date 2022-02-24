package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nullable;

public class Agility extends Ability {

	private static final int AGILITY_1_EFFECT_LVL = 0;
	private static final int AGILITY_2_EFFECT_LVL = 1;
	private static final int AGILITY_BONUS_DAMAGE = 1;
	private static final double SCALING_DAMAGE = 0.1;

	public Agility(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Agility");
		mInfo.mScoreboardId = "Agility";
		mInfo.mShorthandName = "Agl";
		mInfo.mDescriptions.add("You gain permanent Haste I. Your melee attacks deal +1 extra damage.");
		mInfo.mDescriptions.add("You gain permanent Haste II. Increase melee damage by +1 plus 10% of final damage done.");
		mDisplayItem = new ItemStack(Material.GOLDEN_PICKAXE, 1);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_ENCH || event.getType() == DamageType.MELEE_SKILL) {
			if (isLevelTwo()) {
				event.setDamage((event.getDamage() + AGILITY_BONUS_DAMAGE) * (1 + SCALING_DAMAGE));
			} else {
				event.setDamage(event.getDamage() + AGILITY_BONUS_DAMAGE);
			}
		}
		return false; // only changes event damage
	}

	@Override
	public void setupClassPotionEffects() {
		if (mPlayer != null) {
			int effectLevel = isLevelOne() ? AGILITY_1_EFFECT_LVL : AGILITY_2_EFFECT_LVL;
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.FAST_DIGGING, 1000000, effectLevel, true, false));
		}
	}
}
