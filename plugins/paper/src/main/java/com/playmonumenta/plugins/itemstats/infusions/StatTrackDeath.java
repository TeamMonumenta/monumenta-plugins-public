package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class StatTrackDeath implements Infusion {
	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK_DEATH;
	}

	@Override
	public String getName() {
		return "Times Died";
	}

	@Override
	public void onDeath(Plugin plugin, Player player, double value, PlayerDeathEvent event) {
		PlayerInventory inv = player.getInventory();
		for (ItemStack is : inv.getArmorContents()) {
			StatTrackManager.getInstance().incrementStatImmediately(is, player, InfusionType.STAT_TRACK_DEATH, 1);
		}
		StatTrackManager.getInstance().incrementStatImmediately(inv.getItemInOffHand(), player, InfusionType.STAT_TRACK_DEATH, 1);
		StatTrackManager.getInstance().incrementStatImmediately(inv.getItemInMainHand(), player, InfusionType.STAT_TRACK_DEATH, 1);
	}
}
