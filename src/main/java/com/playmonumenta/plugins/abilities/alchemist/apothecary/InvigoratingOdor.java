package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import java.util.Collection;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;

/*
 * Invigorating Odor: Alchemist potions give Speed 1 and 
 * Regeneration 1 (0:10) on allies they hit, also your alch 
 * pots deal +2/3 damage. At level 2, they also provide Resistance 
 * 1 for 10 seconds.
 */
public class InvigoratingOdor extends Ability {

	public InvigoratingOdor(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "InvigoratingOdor";
	}

	@Override
	public boolean PlayerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata("AlchemistPotion")) {
			if (affectedEntities != null && !affectedEntities.isEmpty()) {
				boolean resistance = getAbilityScore() > 1 ? true : false;
				for (LivingEntity le : affectedEntities) {
					if (le instanceof Player) {
						Player player = (Player) le;
						mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.SPEED, 20 * 10, 0, true, true));
						mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 0, true, true));
						if (resistance)
							mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 0, true, true));
					} else if (EntityUtils.isHostileMob(le)) {
						EntityUtils.damageEntity(mPlugin, le, getAbilityScore() == 1 ? 2 : 3, mPlayer);
					}
				}
			}
		}
		return true;
	}
	
}
