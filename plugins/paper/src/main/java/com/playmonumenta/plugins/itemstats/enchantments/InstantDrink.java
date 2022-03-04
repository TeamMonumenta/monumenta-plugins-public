package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
				if (meta.hasCustomEffects()) {
					for (PotionEffect effect : meta.getCustomEffects()) {
						if (effect.getType().equals(PotionEffectType.HEAL) || effect.getType().equals(PotionEffectType.HARM)) {
							PotionUtils.apply(player, new PotionInfo(effect.getType(), effect.getDuration() + 1, effect.getAmplifier(), false, false));
						} else {
							plugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, effect);
						}
					}
				} else {
					PotionInfo info = PotionUtils.getPotionInfo(meta.getBasePotionData(), 1);
					if (info != null) {
						PotionUtils.apply(player, info);
					}
				}

				//Apply Starvation if applicable
				int starvation = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.STARVATION);
				if (starvation > 0) {
					Starvation.apply(player, starvation);
				}

				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1, 1);
				World world = player.getWorld();
				Color color = meta.getColor();
				if (color != null) {
					double red = color.getRed() / 255D;
					double green = color.getGreen() / 255D;
					double blue = color.getBlue() / 255D;
					for (int i = 0; i < 30; i++) {
						double y = FastUtils.randomDoubleInRange(0.25, 1.75);
						world.spawnParticle(Particle.SPELL_MOB, player.getLocation().add(0, y, 0), 0, red, green, blue, 1);
					}
				} else {
					world.spawnParticle(Particle.SPELL, player.getLocation().add(0, 0.75, 0), 30, 0, 0.45, 0, 1);
				}

				//Wait, this is illegal for a potion to have.
				if (ItemStatUtils.getEnchantmentLevel(ItemStatUtils.getEnchantments(new NBTItem(item)), EnchantmentType.INFINITY) > 0) {
					event.setUseItemInHand(Result.DENY);
				} else if (player.getGameMode() != GameMode.CREATIVE) {
					item.setAmount(item.getAmount() - 1);
				}
			}
		}
	}
}
