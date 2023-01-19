package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PotionSplashEvent;

public class Quench implements Infusion {

	public static final double DURATION_BONUS_PER_LVL = 0.025;

	@Override
	public String getName() {
		return "Quench";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.QUENCH;
	}

	@Override
	public void onPlayerPotionSplash(Plugin plugin, Player player, double value, PotionSplashEvent event) {
		double distance = Math.min(player.getLocation().distance(event.getEntity().getLocation()), player.getEyeLocation().distance(event.getEntity().getLocation()));
		distance = Math.min(Math.max(-0.1 * distance + 1, 0), 1);
		ItemStatUtils.changeEffectsDurationSplash(player, event.getPotion().getItem(), distance * getDurationScaling(plugin, player));
	}

	public static double getDurationScaling(Plugin plugin, Player player) {
		int level = plugin.mItemStatManager.getInfusionLevel(player, InfusionType.QUENCH);
		return 1 + DURATION_BONUS_PER_LVL * level;
	}
}
