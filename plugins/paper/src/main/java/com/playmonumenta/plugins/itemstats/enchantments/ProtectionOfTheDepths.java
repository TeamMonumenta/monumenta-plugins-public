package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import org.bukkit.entity.Player;

import java.util.EnumSet;

public class ProtectionOfTheDepths implements Enchantment {

	private double mReductionPct = 0;

	@Override
	public String getName() {
		return "Protection of the Depths";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.PROTECTION_OF_THE_DEPTHS;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double level, DamageEvent event) {
		if (ServerProperties.getClassSpecializationsEnabled() == true) {
			mReductionPct = .25; //25% reduction for region2
		} else {
			mReductionPct = .15; //15% reduction for region 1
		}
		event.setDamage(event.getDamage() * (1.0 - mReductionPct));
	}
}
