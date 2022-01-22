package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class StatTrackBoss implements Infusion {

	@Override
	public String getName() {
		return "Boss Damage Dealt";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK_BOSS;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		ItemStack is = player.getInventory().getItemInMainHand();

		//Track damage dealt to bosses
		if (EntityUtils.isBoss(enemy)) {
			StatTrackManager.incrementStat(is, player, InfusionType.STAT_TRACK_BOSS, (int) event.getDamage());
		}
	}
}
