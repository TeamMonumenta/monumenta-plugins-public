package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;



public class Sapper implements BaseEnchantment {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Sapper";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.ARMOR, ItemSlot.OFFHAND);
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, BlockBreakEvent event, ItemStack item, int level) {
		if (ItemUtils.isPickaxe(item) && event.getBlock().getType() == Material.SPAWNER) {
			PlayerUtils.healPlayer(player, level);
			player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 8, 0.4, 0.4, 0.4, 0.1);
		}
	}
}