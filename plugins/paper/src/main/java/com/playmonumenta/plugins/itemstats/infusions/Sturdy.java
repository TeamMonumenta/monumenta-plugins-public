package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageShieldedEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class Sturdy implements Infusion {
	public static final double CDR_PER_LEVEL = 0.05;

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STURDY;
	}

	@Override
	public String getName() {
		return "Sturdy";
	}

	@Override
	public void onDamageShielded(Plugin plugin, Player player, double value, DamageShieldedEvent event) {
		Bukkit.getScheduler().runTask(plugin, () -> {
			int shieldBrokenTicks = player.getCooldown(Material.SHIELD);
			if (shieldBrokenTicks > 0) {
				int finalTicks = updateStunCooldown(shieldBrokenTicks, value);
				player.setCooldown(Material.SHIELD, finalTicks);
			}
		});
	}

	public static int updateStunCooldown(double ticks, double value) {
		return (int) (ticks * (1 - value * CDR_PER_LEVEL));
	}
}
