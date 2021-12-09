package com.playmonumenta.plugins.enchantments.infusions.delves;

import java.util.EnumSet;
import java.util.List;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.BlockBreakEvent;

public class Mitosis implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Mitosis";
	private static final int DURATION = 3 * 20;
	private static final int RADIUS = 5;
	private static final double PERCENT_WEAKEN_PER_LEVEL = 0.0375;
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(214, 148, 181), 1.0f);

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
			player.playSound(player.getLocation(), Sound.ENTITY_SLIME_SQUISH, 0.7f, 0.7f);
			player.getWorld().spawnParticle(Particle.REDSTONE, event.getBlock().getLocation(), 25, 1.5, 1.5, 1.5, COLOR);
			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(event.getBlock().getLocation(), RADIUS);
			for (LivingEntity mob : mobs) {
				EntityUtils.applyWeaken(plugin, DURATION, PERCENT_WEAKEN_PER_LEVEL * DelveInfusionUtils.getModifiedLevel(plugin, player, level), mob);
			}
		}
	}

}
