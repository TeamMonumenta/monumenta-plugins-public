package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class Starvation implements BaseEnchantment {

	private static final String PROPERTY_NAME = ChatColor.RED + "Starvation";
	
	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND);
	}

	@Override
	public void onConsume(Plugin plugin, Player player, PlayerItemConsumeEvent event, int level) {
		if (InventoryUtils.testForItemWithLore(event.getItem(), "Starvation")) {
			int currFood = player.getFoodLevel();
			float currSat = player.getSaturation();
			float newSat = Math.max(0, currSat - (float) level);
			float remainder = Math.max(0, (float) level - currSat);
			int newFood = Math.max(0, (int) (currFood - remainder));
			player.setSaturation(newSat);
			player.setFoodLevel(newFood);
			World world = player.getWorld();
			world.spawnParticle(Particle.SNEEZE, player.getLocation().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 1);
			world.spawnParticle(Particle.SLIME, player.getLocation().add(0, 1, 0), 25, 0.5, 0.45, 0.25, 1);
			world.playSound(player.getLocation(), Sound.ENTITY_HOGLIN_AMBIENT, 1, 1.25f);
		}
	}
}
