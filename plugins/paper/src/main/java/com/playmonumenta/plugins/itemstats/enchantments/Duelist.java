package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import java.util.EnumSet;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;

public class Duelist implements Enchantment {

	private static final int DAMAGE_PER_LEVEL = 2;

	@Override
	public String getName() {
		return "Duelist";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.DUELIST;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 6;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (EntityUtils.isHumanlike(enemy)) {
			if (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Trident) {
				event.setDamage(event.getDamage() + level * DAMAGE_PER_LEVEL);
			} else if (event.getType() == DamageType.MELEE) {
				event.setDamage(event.getDamage() + DAMAGE_PER_LEVEL * level * player.getCooledAttackStrength(0));
			}
		}
	}
}
