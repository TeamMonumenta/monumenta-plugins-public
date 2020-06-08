package com.playmonumenta.plugins.abilities.other;

import java.util.LinkedHashMap;
import java.util.Map;

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

public class EvasionEnchant extends Ability {

	private static final int THRESHOLD_INTERVAL = 5;
	private static final double DAMAGE_REDUCTION_INTERVAL = 0.2;
	// Threshold -> Damage Reduction pairs
	private static final Map<Integer, Double> EVASION_DAMAGE_REDUCTION = new LinkedHashMap<>();

	static {
		// Reverse order checking evasion thresholds
		EVASION_DAMAGE_REDUCTION.put(THRESHOLD_INTERVAL * 4, DAMAGE_REDUCTION_INTERVAL * 4);
		EVASION_DAMAGE_REDUCTION.put(THRESHOLD_INTERVAL * 3, DAMAGE_REDUCTION_INTERVAL * 3);
		EVASION_DAMAGE_REDUCTION.put(THRESHOLD_INTERVAL * 2, DAMAGE_REDUCTION_INTERVAL * 2);
		EVASION_DAMAGE_REDUCTION.put(THRESHOLD_INTERVAL,     DAMAGE_REDUCTION_INTERVAL * 1);
	}

	public int mCounter = 0;
	public int mLastCounterAmountConsumed = 0;

	public EvasionEnchant(Plugin plugin, World world, Player player) {
		super(plugin, world, player, null);
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		evade(event);
		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		evade(event);
		return true;
	}

	private void evade(EntityDamageByEntityEvent event) {
		double damage = event.getDamage();
		Location loc = mPlayer.getLocation().add(0, 1, 0);

		for (Map.Entry<Integer, Double> entry : EVASION_DAMAGE_REDUCTION.entrySet()) {
			int threshold = entry.getKey();
			double damageReduction = entry.getValue();

			if (mCounter >= threshold) {
				mLastCounterAmountConsumed = threshold;
				mCounter -= threshold;
				event.setDamage(damage * (1 - damageReduction));

				mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, threshold, 0.15, 0.25, 0.15, 0.05);
				mWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, threshold * 0.02f, 2f);

				Bukkit.getPluginManager().callEvent(new EvasionEvent(mPlayer, event.getDamage()));
				break;
			}
		}
	}

	@Override
	public boolean canUse(Player player) {
		return true;
	}

}
