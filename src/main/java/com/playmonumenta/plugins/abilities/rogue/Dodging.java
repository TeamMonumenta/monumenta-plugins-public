package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.MetadataUtils;

import java.util.Iterator;
import java.util.Random;

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
import org.bukkit.Sound;
import org.bukkit.World;

public class Dodging extends Ability {

	private static final int DODGING_SPEED_EFFECT_DURATION = 15 * 20;
	private static final int DODGING_SPEED_EFFECT_LEVEL = 0;
	private static final int DODGING_COOLDOWN_1 = 12 * 20;
	private static final int DODGING_COOLDOWN_2 = 10 * 20;
	private static final String ROGUE_DODGING_NONCE_METAKEY = "MonumentaRogueDodgingNonce";

	public Dodging(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 4;
		mInfo.specId = -1;
		mInfo.linkedSpell = Spells.DODGING;
		mInfo.scoreboardId = "Dodging";
		// NOTE: getAbilityScore() can only be used after the scoreboardId is set!
		mInfo.cooldown = getAbilityScore() == 1 ? DODGING_COOLDOWN_1 : DODGING_COOLDOWN_2;
	}

	@Override
	public boolean PlayerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		EntityType type = event.getDamager().getType();
		Projectile damager = (Projectile) event.getDamager();
		int dodging = getAbilityScore();
		if (dodging > 1) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.SPEED,
			                                                  DODGING_SPEED_EFFECT_DURATION,
			                                                  DODGING_SPEED_EFFECT_LEVEL,
			                                                  true, false));
			mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0f, 0.5f);
		}

		mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);

		int cooldown = dodging == 1 ? DODGING_COOLDOWN_1 : DODGING_COOLDOWN_2;
		mPlugin.mTimers.AddCooldown(mPlayer.getUniqueId(), Spells.DODGING, cooldown);

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
		MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, ROGUE_DODGING_NONCE_METAKEY);
		event.setCancelled(true);
		putOnCooldown();
		return false;
	}

	@Override
	public boolean runCheck() {
		return mPlayer.getLastDamageCause().getCause() == DamageCause.PROJECTILE;
	}
}
