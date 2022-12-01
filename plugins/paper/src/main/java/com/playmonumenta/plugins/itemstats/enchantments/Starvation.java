package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import java.util.EnumSet;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.jetbrains.annotations.NotNull;

public class Starvation implements Enchantment {

	@Override
	public @NotNull String getName() {
		return "Starvation";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.STARVATION;
	}

	@Override public EnumSet<ItemStatUtils.Slot> getSlots() {
		return EnumSet.of(ItemStatUtils.Slot.MAINHAND, ItemStatUtils.Slot.OFFHAND);
	}

	@Override
	public void onConsume(Plugin plugin, Player player, double level, PlayerItemConsumeEvent event) {
		int starvation = ItemStatUtils.getEnchantmentLevel(event.getItem(), EnchantmentType.STARVATION);
		if (starvation > 0) {
			apply(player, starvation);
		}
	}

	public static void apply(Player player, int level) {
		if (level > 0) {
			int currFood = player.getFoodLevel();
			float currSat = player.getSaturation();
			float newSat = Math.max(0, currSat - level);
			float remainder = Math.max(0, level - currSat);
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
