package com.playmonumenta.plugins.abilities.other;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.events.EvasionEvent;
import com.playmonumenta.plugins.utils.BossUtils.BossAbilityDamageEvent;

public class EvasionEnchant extends Ability {

	// You can get above 20 specialized evasion and have below 20 normal evasion, so overflow is valuable; cap it arbitrarily
	private static final int MAX_COUNTER = 100;
	// Threshold -> Damage Reduction pairs
	private static final Map<Integer, Double> EVASION_DAMAGE_REDUCTION = new LinkedHashMap<Integer, Double>();

	static {
		// Reverse order checking evasion thresholds
		EVASION_DAMAGE_REDUCTION.put(20, 0.8);
		EVASION_DAMAGE_REDUCTION.put(15, 0.6);
		EVASION_DAMAGE_REDUCTION.put(10, 0.4);
		EVASION_DAMAGE_REDUCTION.put(5, 0.2);
	}

	public int mCounter = 0;

	public EvasionEnchant(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, null);
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		event.setDamage(evade(event.getDamage(), event));
		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		event.setDamage(evade(event.getDamage(), event));
		return true;
	}

	@Override
	public void playerDamagedByBossEvent(BossAbilityDamageEvent event) {
		event.setDamage(evade(event.getDamage(), null));
	}

	private double evade(double damage, EntityDamageByEntityEvent damageEvent) {
		Location loc = mPlayer.getLocation().add(0, 1, 0);

		for (Map.Entry<Integer, Double> entry : EVASION_DAMAGE_REDUCTION.entrySet()) {
			int threshold = entry.getKey();
			double damageReduction = entry.getValue();

			if (mCounter >= threshold) {
				// Cap the overflow Evasion counter
				mCounter = Math.min(MAX_COUNTER, mCounter - threshold);

				mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, threshold, 0.15, 0.25, 0.15, 0.05);
				mWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, threshold * 0.02f, 1.5f);
				mWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, threshold * 0.02f, 2f);

				EvasionEvent event = new EvasionEvent(mPlayer, damage, damageEvent);
				Bukkit.getPluginManager().callEvent(event);
				return damage * (1 - damageReduction);
			}
		}

		return damage;
	}

	@Override
	public boolean canUse(Player player) {
		return true;
	}

}
