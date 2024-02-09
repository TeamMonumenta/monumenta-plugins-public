package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class StatTrackDamage implements Infusion {

	@Override
	public String getName() {
		return "Damage Dealt";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK_DAMAGE;
	}

	@Override
	public double getPriorityAmount() {
		return 6000; // after all damage modifiers
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (!EntityUtils.isTrainingDummy(enemy)) {
			int dmgDealt = (int) Math.round(Math.min(event.getFinalDamage(false), enemy.getHealth() + enemy.getAbsorptionAmount()));
			ItemStack is = player.getInventory().getItemInMainHand();
			StatTrackManager.incrementStat(is, player, InfusionType.STAT_TRACK_DAMAGE, dmgDealt);
		}
	}
}
