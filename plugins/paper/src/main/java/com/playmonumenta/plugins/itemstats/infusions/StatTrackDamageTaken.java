package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

public class StatTrackDamageTaken implements Infusion {

	@Override
	public ItemStatUtils.InfusionType getInfusionType() {
		return ItemStatUtils.InfusionType.STAT_TRACK_DAMAGE_TAKEN;
	}

	@Override
	public String getName() {
		return "Damage Taken";
	}

	@Override
	public double getPriorityAmount() {
		return 6000; // after all damage reduction
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		PlayerInventory inv = player.getInventory();

		int dmgTaken = (int) Math.round(event.getFinalDamage(false));

		for (ItemStack is : inv.getArmorContents()) {
			StatTrackManager.getInstance().incrementStatImmediately(is, player, ItemStatUtils.InfusionType.STAT_TRACK_DAMAGE_TAKEN, dmgTaken);
		}
		StatTrackManager.getInstance().incrementStatImmediately(inv.getItemInOffHand(), player, ItemStatUtils.InfusionType.STAT_TRACK_DAMAGE_TAKEN, dmgTaken);
		StatTrackManager.incrementStat(inv.getItemInMainHand(), player, ItemStatUtils.InfusionType.STAT_TRACK_DAMAGE_TAKEN, dmgTaken);
	}
}
