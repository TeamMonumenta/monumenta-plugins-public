package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import java.util.EnumSet;
import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ProtectionOfTheDepths implements Enchantment {

	private static final double DAMAGE_MULTIPLIER_R1 = 0.85; // 15% reduction for region 1
	private static final double DAMAGE_MULTIPLIER_R2 = 0.75; // 25% reduction for region 2

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
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		event.setDamage(event.getDamage() * getDamageMultiplier(ServerProperties.getClassSpecializationsEnabled()));
	}

	public static double getDamageMultiplier(boolean region2) {
		return region2 ? DAMAGE_MULTIPLIER_R2 : DAMAGE_MULTIPLIER_R1;
	}
}
