package com.playmonumenta.plugins.enchantments.evasions;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.events.EvasionEvent;

public class EvasionInfo {

	private static final double DAMAGE_REDUCTION_INTERVAL = 0.2;
	private static final int THRESHOLD_INTERVAL = 5;
	private static final int MAX_STACKS = 20;

	// Threshold -> Damage Reduction pairs
	private static final Map<Integer, Double> EVASION_DAMAGE_REDUCTION = new LinkedHashMap<>();

	static {
		// Reverse order checking evasion thresholds
		EVASION_DAMAGE_REDUCTION.put(THRESHOLD_INTERVAL * 4, DAMAGE_REDUCTION_INTERVAL * 4);
		EVASION_DAMAGE_REDUCTION.put(THRESHOLD_INTERVAL * 3, DAMAGE_REDUCTION_INTERVAL * 3);
		EVASION_DAMAGE_REDUCTION.put(THRESHOLD_INTERVAL * 2, DAMAGE_REDUCTION_INTERVAL * 2);
		EVASION_DAMAGE_REDUCTION.put(THRESHOLD_INTERVAL * 1, DAMAGE_REDUCTION_INTERVAL * 1);
	}

	private static final Map<Player, EvasionInfo> EVASION_INFOS = new HashMap<Player, EvasionInfo>();

	private int mStacks = 0;

	public static void addStacks(Player player, int stacks) {
		EvasionInfo info = getInfo(player);
		info.mStacks += Math.min(MAX_STACKS, stacks);
	}

	public static void triggerEvasion(Player player, EntityDamageByEntityEvent event) {
		double damage = event.getDamage();
		World world = player.getWorld();
		Location loc = player.getLocation().add(0, 1, 0);
		EvasionInfo info = getInfo(player);

		for (Map.Entry<Integer, Double> entry : EVASION_DAMAGE_REDUCTION.entrySet()) {
			int threshold = entry.getKey();
			double damageReduction = entry.getValue();

			if (info.mStacks >= threshold) {
				info.mStacks -= threshold;
				event.setDamage(damage * (1 - damageReduction));

				world.spawnParticle(Particle.SMOKE_NORMAL, loc, threshold, 0.15, 0.25, 0.15, 0.05);
				world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, threshold * 0.02f, 2f);

				Bukkit.getPluginManager().callEvent(new EvasionEvent(player, event.getDamage()));
				break;
			}
		}
	}

	private static EvasionInfo getInfo(Player player) {
		EvasionInfo info = EVASION_INFOS.get(player);
		if (info == null) {
			info = new EvasionInfo();
			EVASION_INFOS.put(player, info);
		}

		return info;
	}

}
