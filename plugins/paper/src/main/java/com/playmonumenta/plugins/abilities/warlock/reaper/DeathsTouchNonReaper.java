package com.playmonumenta.plugins.abilities.warlock.reaper;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker.KillTriggeredAbility;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.PotionUtils;

public class DeathsTouchNonReaper extends Ability implements KillTriggeredAbility {

	/*
	 * Allow other players to reap the benefits of a marked enemy.
	 */

	private final KillTriggeredAbilityTracker mTracker;

	public DeathsTouchNonReaper(Plugin plugin, World world, Player player) {
		super(plugin, world, player, null);
		mTracker = new KillTriggeredAbilityTracker(this);
	}

	@Override
	public boolean canUse(Player player) {
		return true;
	}

	private static Map<PotionEffectType, Integer> getOppositeEffects(LivingEntity e) {
		Map<PotionEffectType, Integer> effects = new HashMap<>();
		for (PotionEffect effect : e.getActivePotionEffects()) {
			PotionEffectType type = effect.getType();
			if (PotionUtils.hasNegativeEffects(type)) {
				type = PotionUtils.getOppositeEffect(type);
				if (type != null) {
					effects.put(type, effect.getAmplifier());
				}
			}
		}
		if (e.getFireTicks() > 0) {
			effects.put(PotionEffectType.FIRE_RESISTANCE, 0);
		}
		return effects;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		mTracker.updateDamageDealtToBosses(event);
		return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity damagee, EntityDamageByEntityEvent event) {
		mTracker.updateDamageDealtToBosses(event);
		return true;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		triggerOnKill(event.getEntity());
	}

	@Override
	public void triggerOnKill(LivingEntity mob) {
		if (mob.hasMetadata("DeathsTouchBuffDuration")) {
			Map<PotionEffectType, Integer> effects = getOppositeEffects(mob);
			int duration = mob.getMetadata(DeathsTouch.DEATHS_TOUCH_BUFF_DURATION).get(0).asInt();
			int amplifierCap = mob.getMetadata(DeathsTouch.DEATHS_TOUCH_AMPLIFIER_CAP).get(0).asInt();
			for (Map.Entry<PotionEffectType, Integer> effect : effects.entrySet()) {
				if (effect.getKey() == PotionEffectType.DAMAGE_RESISTANCE) {
					// Only do Resistance I regardless of Vulnerability level
					mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_OTHER, new PotionEffect(effect.getKey(), duration, 0, true, true));
				} else {
					mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_OTHER, new PotionEffect(effect.getKey(), duration, Math.min(amplifierCap, effect.getValue()), true, true));
				}
			}
		}
	}

}
