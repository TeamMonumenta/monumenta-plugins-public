package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.FirstStrikeCooldown;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FirstStrike implements Enchantment {

	private static double DAMAGE_PER_LEVEL = 0.1;
	private static int DURATION = 3 * 20;
	private static String SOURCE = "FirstStrikeDisable";

	@Override
	public @NotNull String getName() {
		return "First Strike";
	}

	@Override
	public double getPriorityAmount() {
		return 1;
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.FIRST_STRIKE;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		DamageEvent.DamageType type = event.getType();
		if ((type == DamageEvent.DamageType.MELEE && ItemStatUtils.isNotExclusivelyRanged(player.getInventory().getItemInMainHand())) || type == DamageEvent.DamageType.PROJECTILE) {
			if (plugin.mEffectManager.getEffects(player, FirstStrikeCooldown.class) == null) {
				event.setDamage(event.getDamage() * (1 + (DAMAGE_PER_LEVEL * level)));
				// TODO: Add sound + particles
			}
			plugin.mEffectManager.addEffect(player, SOURCE, new FirstStrikeCooldown(DURATION));
		}
	}
}
