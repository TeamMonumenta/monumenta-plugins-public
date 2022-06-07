package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class WarmFireworkFinisher implements EliteFinisher {

	public static final String NAME = "Warm Firework";

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		Firework fw = (Firework) p.getWorld().spawnEntity(loc, EntityType.FIREWORK);
		FireworkMeta fwm = fw.getFireworkMeta();
		FireworkEffect.Builder fwBuilder = FireworkEffect.builder();
		fwBuilder.withColor(Color.RED, Color.ORANGE, Color.YELLOW);
		fwBuilder.with(FireworkEffect.Type.BURST);
		FireworkEffect fwEffect = fwBuilder.build();
		fwm.addEffect(fwEffect);
		fw.setFireworkMeta(fwm);

		new BukkitRunnable() {
			@Override
			public void run() {
				fw.detonate();
			}
		}.runTaskLater(Plugin.getInstance(), 40);
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIREWORK_ROCKET;
	}

}
