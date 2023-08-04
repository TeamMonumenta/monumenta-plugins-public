package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

public class StatTrackDamageTaken implements Infusion {

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK_DAMAGE_TAKEN;
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

		// Ensures no overkill damage is added on to the stat track, important in cases where 1,000,000+ damage is dealt by some attacks.
		int dmgTaken = (int) Math.round(Math.min(event.getFinalDamage(false), player.getHealth() + player.getAbsorptionAmount()));

		for (ItemStack is : inv.getArmorContents()) {
			StatTrackManager.getInstance().incrementStatImmediately(is, player, InfusionType.STAT_TRACK_DAMAGE_TAKEN, dmgTaken);
		}
		StatTrackManager.getInstance().incrementStatImmediately(inv.getItemInOffHand(), player, InfusionType.STAT_TRACK_DAMAGE_TAKEN, dmgTaken);
		StatTrackManager.incrementStat(inv.getItemInMainHand(), player, InfusionType.STAT_TRACK_DAMAGE_TAKEN, dmgTaken);
	}
}
