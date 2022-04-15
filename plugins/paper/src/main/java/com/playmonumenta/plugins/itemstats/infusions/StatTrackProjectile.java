package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import java.util.EnumSet;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class StatTrackProjectile implements Infusion {

	private static final EnumSet<DamageEvent.DamageType> TYPES = EnumSet.of(DamageEvent.DamageType.PROJECTILE, DamageEvent.DamageType.PROJECTILE_SKILL);

	@Override
	public String getName() {
		return "Projectile Damage Dealt";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK_PROJECTILE;
	}

	@Override
	public double getPriorityAmount() {
		return 6000; // after all damage modifiers
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (TYPES.contains(event.getType()) && !EntityUtils.isTrainingDummy(enemy)) {
			ItemStack is = player.getInventory().getItemInMainHand();
			StatTrackManager.incrementStat(is, player, InfusionType.STAT_TRACK_PROJECTILE, (int) event.getFinalDamage(false));
		}
	}
}
