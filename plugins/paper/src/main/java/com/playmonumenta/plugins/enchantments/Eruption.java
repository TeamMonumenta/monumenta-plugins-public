package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class Eruption implements BaseEnchantment {

	private static String PROPERTY_NAME = ChatColor.GRAY + "Eruption";
	private static final float RADIUS = 5.0f;
	private static final float DAMAGE_PER_LEVEL = 4.0f;


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
		//If we break a spawner with a pickaxe
		if (InventoryUtils.isPickaxeItem(item) && event.getBlock().getType() == Material.SPAWNER) {
			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(event.getBlock().getLocation(), RADIUS);
			//Damage any mobs in the area
			for (LivingEntity mob : mobs) {
				EntityUtils.damageEntity(plugin, mob, DAMAGE_PER_LEVEL * level, player, MagicType.FIRE);
			}
			//Player feedback
			player.playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, 0.6f, 0.9f);
			player.getWorld().spawnParticle(Particle.LAVA, event.getBlock().getLocation(), 25, 1.5, 1.5, 1.5);
		}
	}
}
