package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class CurseOfShrapnel implements BaseEnchantment {
	private static String PROPERTY_NAME = ChatColor.RED + "Curse of Shrapnel";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, BlockBreakEvent event, ItemStack item, int level) {
		if (InventoryUtils.isPickaxeItem(item) && event.getBlock().getType() == Material.SPAWNER) {
			EntityUtils.damageEntity(plugin, player, level, null);
			player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, player.getLocation().add(0, 1, 0), 8, 0.4, 0.4, 0.4, 0.1);
		}
	}

}
