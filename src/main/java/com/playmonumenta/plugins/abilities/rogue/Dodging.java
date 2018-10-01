package com.playmonumenta.plugins.abilities.rogue;

import java.util.Iterator;

import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TippedArrow;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.MetadataUtils;

public class Dodging extends Ability {

	private static final int DODGING_SPEED_EFFECT_DURATION = 15 * 20;
	private static final int DODGING_SPEED_EFFECT_LEVEL = 0;
	private static final int DODGING_COOLDOWN_1 = 12 * 20;
	private static final int DODGING_COOLDOWN_2 = 10 * 20;
	private static final String ROGUE_DODGING_NONCE_METAKEY = "MonumentaRogueDodgingNonce";
	
	@Override
	public boolean PlayerDamagedByProjectileEvent(Player player, EntityDamageByEntityEvent event) {
		EntityType type = event.getDamager().getType();
		Projectile damager = (Projectile) event.getDamager();
		int dodging = getAbilityScore(player);
		World world = player.getWorld();
		if (dodging > 1) {
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.SPEED,
			                                                  DODGING_SPEED_EFFECT_DURATION,
			                                                  DODGING_SPEED_EFFECT_LEVEL,
			                                                  true, false));
			world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0f, 0.5f);
		}

		world.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);

		int cooldown = dodging == 1 ? DODGING_COOLDOWN_1 : DODGING_COOLDOWN_2;
		mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.DODGING, cooldown);

		// Remove effects from tipped arrows
		// TODO: This is the same code as for removing from shields, should probably be
		// a utility function
		if (type == EntityType.TIPPED_ARROW) {
			TippedArrow arrow = (TippedArrow)damager;
			PotionData data = new PotionData(PotionType.AWKWARD);
			arrow.setBasePotionData(data);

			if (arrow.hasCustomEffects()) {
				Iterator<PotionEffect> effectIter = arrow.getCustomEffects().iterator();
				while (effectIter.hasNext()) {
					PotionEffect effect = effectIter.next();
					arrow.removeCustomEffect(effect.getType());
				}
			}
		}

		// Set metadata indicating this event happened this tick
		MetadataUtils.checkOnceThisTick(mPlugin, player, ROGUE_DODGING_NONCE_METAKEY);
		event.setCancelled(true);
		return false; 
	}
	
	@Override
	public AbilityInfo getInfo() { 
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 4;
		info.specId = -1;
		info.linkedSpell = Spells.DODGING;
		info.scoreboardId = "Dodging";
		int cd = getAbilityScore(player) == 1 ? DODGING_COOLDOWN_1 : DODGING_COOLDOWN_2;
		info.cooldown = cd;
		return info; 
	}
	
	@Override
	public boolean runCheck(Player player) {
		return player.getLastDamageCause().getCause() == DamageCause.PROJECTILE;
	}
}
