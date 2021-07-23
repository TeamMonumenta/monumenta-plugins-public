package com.playmonumenta.plugins.enchantments.infusions.delves;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;

public class Carapace implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Carapace";
	private static final double DAMAGE_REDUCTION_PER_LEVEL = 0.0125;
	private static final String DAMAGE_REDUCTION_EFFECT_NAME = "OrangeInfusionDamageReductionEffect";
	private static final int DURATION = 5 * 20;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onHurt(Plugin plugin, Player player, int level, EntityDamageEvent event) {
		double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, level);
		// Runs one tick later so that it does not affect this attack
		new BukkitRunnable() {
			@Override
			public void run() {
				plugin.mEffectManager.addEffect(player, DAMAGE_REDUCTION_EFFECT_NAME, new PercentDamageReceived(DURATION, -DAMAGE_REDUCTION_PER_LEVEL * modifiedLevel));
			}
		}.runTaskLater(plugin, 1);
	}
}
