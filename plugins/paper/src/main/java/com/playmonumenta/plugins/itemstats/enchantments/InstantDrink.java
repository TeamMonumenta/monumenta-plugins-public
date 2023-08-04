package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.NonClericProvisionsPassive;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
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
				PotionUtils.instantDrinkParticles(player, color);

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
				// fix for ghost potions on hotbar - bug #14893
				// is hard to reproduce even with high ping so this may not be necessary
				player.updateInventory();
			}
		}
	}
}
