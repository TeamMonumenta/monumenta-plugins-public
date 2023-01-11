package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.NonClericProvisionsPassive;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.NotNull;

public class InstantDrink implements Enchantment {

	@Override
	public @NotNull String getName() {
		return "Instant Drink";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.INSTANT_DRINK;
	}

	@Override
	public void onPlayerInteract(Plugin plugin, Player player, double level, PlayerInteractEvent event) {
		Block block = event.getClickedBlock();
		if (event.getAction() == Action.RIGHT_CLICK_AIR || (event.getAction() == Action.RIGHT_CLICK_BLOCK && block != null && !ItemUtils.interactableBlocks.contains(block.getBlockData().getMaterial()))) {
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.POTION && item.getItemMeta() instanceof PotionMeta meta) {
				ItemStatUtils.applyCustomEffects(plugin, player, item);

				Starvation.apply(player, ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.STARVATION));

				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1, 1);
				Color color = meta.getColor();
				if (color != null) {
					double red = color.getRed() / 255D;
					double green = color.getGreen() / 255D;
					double blue = color.getBlue() / 255D;
					for (int i = 0; i < 30; i++) {
						double y = FastUtils.randomDoubleInRange(0.25, 1.75);
						new PartialParticle(Particle.SPELL_MOB, player.getLocation().add(0, y, 0), 1, red, green, blue, 1)
							.directionalMode(true).minimumMultiplier(false).spawnAsPlayerActive(player);
					}
				} else {
					new PartialParticle(Particle.SPELL, player.getLocation().add(0, 0.75, 0), 30, 0, 0.45, 0, 1).spawnAsPlayerActive(player);
				}

				//Wait, this is illegal for a potion to have.
				if (ItemStatUtils.hasEnchantment(item, EnchantmentType.INFINITY)) {
					event.setUseItemInHand(Result.DENY);
				} else if (player.getGameMode() != GameMode.CREATIVE) {
					if (NonClericProvisionsPassive.testRandomChance(player)) {
						NonClericProvisionsPassive.sacredProvisionsSound(player);
					} else {
						item.setAmount(item.getAmount() - 1);
					}
				}
			}
		}
	}
}
