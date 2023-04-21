package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class StatTrackHealingDone implements Infusion {
	@Override public ItemStatUtils.InfusionType getInfusionType() {
		return ItemStatUtils.InfusionType.STAT_TRACK_HEALING_DONE;
	}

	@Override public String getName() {
		return "Healing Done";
	}

	public static void healingDone(Player player, double healAmount) {
		int amountHealed = (int) Math.round(healAmount + 0.49);
		PlayerInventory inv = player.getInventory();
		for (ItemStack is : inv.getArmorContents()) {
			StatTrackManager.getInstance().incrementStatImmediately(is, player, ItemStatUtils.InfusionType.STAT_TRACK_HEALING_DONE, amountHealed);
		}
		StatTrackManager.getInstance().incrementStatImmediately(inv.getItemInOffHand(), player, ItemStatUtils.InfusionType.STAT_TRACK_HEALING_DONE, amountHealed);
		StatTrackManager.incrementStat(inv.getItemInMainHand(), player, ItemStatUtils.InfusionType.STAT_TRACK_HEALING_DONE, amountHealed);
	}


}
