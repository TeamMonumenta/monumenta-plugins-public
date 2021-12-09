package com.playmonumenta.plugins.enchantments.infusions.delves;

import java.util.EnumSet;
import java.util.NavigableSet;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;


import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.ItemUtils;

public class Ardor implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Ardor";
	private static final int DURATION = 4 * 20;
	private static final int AIR_INCREASE = 15; //Each breath bubble counts as 30
	private static final double PERCENT_SPEED_PER_LEVEL = 0.0375;
	private static final String PERCENT_SPEED_EFFECT_NAME = "ArdorPercentSpeedEffect";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, BlockBreakEvent event, ItemStack item, int level) {
		//If we break a spawner with a pickaxe
		if (ItemUtils.isPickaxe(item) && event.getBlock().getType() == Material.SPAWNER) {
			player.playSound(player.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1, 0.8f);
		    if (player.isInWaterOrBubbleColumn()) {
		        int currAir = player.getRemainingAir();
		        player.setRemainingAir(Math.min(300, currAir + (AIR_INCREASE * (int) DelveInfusionUtils.getModifiedLevel(plugin, player, level))));
		    } else {
				NavigableSet<Effect> speedEffects = plugin.mEffectManager.getEffects(player, PERCENT_SPEED_EFFECT_NAME);
				if (speedEffects != null) {
					for (Effect effect : speedEffects) {
						if (effect.getMagnitude() == PERCENT_SPEED_PER_LEVEL * DelveInfusionUtils.getModifiedLevel(plugin, player, level)) {
							effect.setDuration(DURATION);
						} else {
							effect.setDuration(1);
							plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME,
									new PercentSpeed(DURATION, PERCENT_SPEED_PER_LEVEL * DelveInfusionUtils.getModifiedLevel(plugin, player, level),
											PERCENT_SPEED_EFFECT_NAME));
						}
					}
				} else {
					plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME,
							new PercentSpeed(DURATION, PERCENT_SPEED_PER_LEVEL * DelveInfusionUtils.getModifiedLevel(plugin, player, level),
									PERCENT_SPEED_EFFECT_NAME));
				}
		    }
		}
	}

}
