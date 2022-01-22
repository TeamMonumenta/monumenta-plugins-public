package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class StatTrackKills implements Infusion {

	@Override
	public String getName() {
		return "Mob Kills";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK_KILLS;
	}

	@Override
	public void onKill(Plugin plugin, Player player, double value, EntityDeathEvent event, LivingEntity enemy) {
		ItemStack is = player.getInventory().getItemInMainHand();

		//We killed a mob, so increase the stat and update it
		StatTrackManager.incrementStat(is, player, InfusionType.STAT_TRACK_KILLS, 1);

	}
}
