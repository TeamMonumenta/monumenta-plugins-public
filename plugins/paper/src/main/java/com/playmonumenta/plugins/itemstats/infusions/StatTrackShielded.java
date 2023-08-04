package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

public class StatTrackShielded implements Infusion {
	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK_SHIELD_BLOCKED;
	}

	@Override
	public String getName() {
		return "Times Blocked Damage";
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.isBlockedByShield()) {
			PlayerInventory inv = player.getInventory();
			StatTrackManager.getInstance().incrementStatImmediately(inv.getItemInOffHand(), player, InfusionType.STAT_TRACK_SHIELD_BLOCKED, 1);
			StatTrackManager.incrementStat(inv.getItemInMainHand(), player, InfusionType.STAT_TRACK_SHIELD_BLOCKED, 1);
		}
	}
}
