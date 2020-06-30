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
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.EvasionEvent;

public class EvasionInfo {

	private static final double DAMAGE_REDUCTION_INTERVAL = 0.2;
	private static final int THRESHOLD_INTERVAL = 5;
	private static final int MAX_STACKS = 20;
	private static final int SECOND_WIND_IFRAMES = 20 * 2;

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
	private int mLastConsumed = 0;

	public static void addStacks(Player player, int stacks) {
		EvasionInfo info = getInfo(player);
		info.mStacks = Math.min(MAX_STACKS, info.mStacks + stacks);
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
				info.mLastConsumed = threshold;
				info.mStacks -= threshold;
				event.setDamage(damage * (1 - damageReduction));

				world.spawnParticle(Particle.SMOKE_NORMAL, loc, threshold, 0.15, 0.25, 0.15, 0.05);
				world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, threshold * 0.02f, 2f);

				Bukkit.getPluginManager().callEvent(new EvasionEvent(player, event.getDamage()));
				break;
			}
		}
	}

	public static void triggerSecondWind(Plugin plugin, Player player, EntityDamageByEntityEvent event) {
		World world = player.getWorld();
		Location loc = player.getLocation().add(0, 1, 0);
		EvasionInfo info = getInfo(player);

		double originalDamage = event.getDamage() / (1 - info.mLastConsumed / 25.0);
		info.mStacks -= 5;
		info.mLastConsumed += 5;
		event.setDamage(originalDamage * (1 - info.mLastConsumed / 25.0));

		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1.5f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 2f);
		world.playSound(loc, Sound.ITEM_TOTEM_USE, 0.5f, 0.2f);
		world.spawnParticle(Particle.CLOUD, loc, 50, 0, 0, 0, 0.5);
		world.spawnParticle(Particle.TOTEM, loc, 50, 0, 0, 0, 0.5);

		new BukkitRunnable() {
			@Override
			public void run() {
				player.setNoDamageTicks(SECOND_WIND_IFRAMES);
			}
		}.runTaskLater(plugin, 1);
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
