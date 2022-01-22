package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
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

	@Override
	public void onConsume(Plugin plugin, Player player, double level, PlayerItemConsumeEvent event) {
		if (InventoryUtils.testForItemWithLore(event.getItem(), "Starvation")) {
			apply(player, level);
		}
	}

	public static void apply(Player player, double level) {
		int currFood = player.getFoodLevel();
		float currSat = player.getSaturation();
		float newSat = (float) Math.max(0, currSat - level);
		float remainder = (float) Math.max(0, level - currSat);
		int newFood = Math.max(0, (int) (currFood - remainder));
		player.setSaturation(newSat);
		player.setFoodLevel(newFood);
		World world = player.getWorld();
		world.spawnParticle(Particle.SNEEZE, player.getLocation().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 1);
		world.spawnParticle(Particle.SLIME, player.getLocation().add(0, 1, 0), 25, 0.5, 0.45, 0.25, 1);
		world.playSound(player.getLocation(), Sound.ENTITY_HOGLIN_AMBIENT, 1, 1.25f);
	}

}
