package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class StatTrackMelee implements Infusion {

	@Override
	public String getName() {
		return "Melee Damage Dealt";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK_MELEE;
	}

	@Override
	public double getPriorityAmount() {
		return 6000; // after all damage modifiers
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		ItemStack is = player.getInventory().getItemInMainHand();

		if (!isTrainingDummy(enemy)) {
			StatTrackManager.incrementStat(is, player, InfusionType.STAT_TRACK_MELEE, (int) event.getDamage());
		}
	}

	public boolean isTrainingDummy(LivingEntity e) {
		Set<String> tags = e.getScoreboardTags();
		return tags.contains("boss_training_dummy");
	}
}
