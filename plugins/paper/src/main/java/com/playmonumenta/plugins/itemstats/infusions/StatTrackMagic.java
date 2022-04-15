package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class StatTrackMagic implements Infusion {

	@Override
	public String getName() {
		return "Magic Damage Dealt";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK_MAGIC;
	}

	@Override
	public double getPriorityAmount() {
		return 6000; // after all damage modifiers
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.MAGIC && !EntityUtils.isTrainingDummy(enemy)) {
			ItemStack is = player.getInventory().getItemInMainHand();
			StatTrackManager.incrementStat(is, player, InfusionType.STAT_TRACK_MAGIC, (int) event.getFinalDamage(false));
		}
	}
}
