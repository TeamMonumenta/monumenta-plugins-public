package com.playmonumenta.plugins.abilities.warlock.reaper;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.PotionUtils;

public class DeathsTouchNonReaper extends Ability {

	/*
	 * Allow other players to reap the benefits of a marked enemy.
	 */

	public DeathsTouchNonReaper(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, null);
	}

	@Override
	public boolean canUse(Player player) {
		return true;
	}

	private static Map<PotionEffectType, Integer> getOppositeEffects(LivingEntity e) {
		Map<PotionEffectType, Integer> effects = new HashMap<PotionEffectType, Integer>();
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
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (event.getEntity().hasMetadata("DeathsTouchBuffDuration")) {
			Map<PotionEffectType, Integer> effects = getOppositeEffects(event.getEntity());
			int duration = event.getEntity().getMetadata(DeathsTouch.DEATHS_TOUCH_BUFF_DURATION).get(0).asInt();
			int amplifierCap = event.getEntity().getMetadata(DeathsTouch.DEATHS_TOUCH_AMPLIFIER_CAP).get(0).asInt();
			for (Map.Entry<PotionEffectType, Integer> effect : effects.entrySet()) {
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_OTHER, new PotionEffect(effect.getKey(), duration, Math.min(amplifierCap, effect.getValue()), true, true));
			}
		}
	}

}
