package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class Usurper implements Infusion {

	public static final double HEAL_PCT_PER_LVL = 0.025;

	@Override
	public String getName() {
		return "Usurper";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.USURPER;
	}

	@Override
	public void onKill(Plugin plugin, Player player, double value, EntityDeathEvent event, LivingEntity enemy) {
		if (EntityUtils.isBoss(enemy) || EntityUtils.isElite(enemy)) {
			double healAmount = EntityUtils.getMaxHealth(player) * HEAL_PCT_PER_LVL * DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value);
			PlayerUtils.healPlayer(plugin, player, healAmount);
		}
	}

}
