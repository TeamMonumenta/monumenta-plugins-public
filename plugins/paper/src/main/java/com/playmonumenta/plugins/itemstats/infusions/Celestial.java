package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Celestial implements Infusion {
	public static final double DAMAGE_BONUS_PER_LEVEL = 0.015;

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.CELESTIAL;
	}

	@Override
	public String getName() {
		return "Celestial";
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (enemy.getLocation().getY() > player.getLocation().getY()) {
			event.updateGearDamageWithMultiplier(1 + DAMAGE_BONUS_PER_LEVEL * level);

			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 0.4f, 2f);
			player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.6f, 0.8f);
		}
	}
}
