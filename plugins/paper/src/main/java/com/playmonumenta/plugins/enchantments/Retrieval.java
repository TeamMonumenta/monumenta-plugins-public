package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.FastUtils;

public class Retrieval implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Retrieval";
	private static final float RETRIEVAL_CHANCE = 0.1f;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.ARMOR, ItemSlot.OFFHAND);
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		if (proj.getType() == EntityType.ARROW || proj.getType() == EntityType.SPECTRAL_ARROW) {
			AbstractArrow arrow = (AbstractArrow)proj;
			ItemStack mainHand = player.getInventory().getItemInMainHand();
			ItemStack offHand = player.getInventory().getItemInOffHand();
			if (InventoryUtils.isBowItem(mainHand) || InventoryUtils.isBowItem(offHand)) {
				int infLevel = Math.max(mainHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE), offHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE));
				if (arrow.getPickupStatus() == Arrow.PickupStatus.ALLOWED) {
					if (infLevel == 0 && FastUtils.RANDOM.nextDouble() < RETRIEVAL_CHANCE * level) {
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.3f, 1.0f);

						arrow.setPickupStatus(Arrow.PickupStatus.ALLOWED);
						Inventory playerInv = player.getInventory();
						int firstArrow = playerInv.first(Material.ARROW);
						int firstTippedArrow = playerInv.first(Material.TIPPED_ARROW);
						int firstSpectralArrow = playerInv.first(Material.SPECTRAL_ARROW);

						final int arrowSlot;
						if (firstArrow == -1 && firstTippedArrow > -1 && firstSpectralArrow == -1) {
							arrowSlot = firstTippedArrow;
						} else if (firstArrow > - 1 && firstTippedArrow == -1 && firstSpectralArrow == -1) {
							arrowSlot = firstArrow;
						} else if (firstArrow == -1 && firstTippedArrow == -1 && firstSpectralArrow > -1) {
							arrowSlot = firstSpectralArrow;
						} else if (firstArrow > - 1 && firstTippedArrow > -1) {
							arrowSlot = Math.min(firstArrow, firstTippedArrow);
						} else if (firstArrow > -1 && firstSpectralArrow > -1) {
							arrowSlot = Math.min(firstArrow, firstTippedArrow);
						} else if (firstTippedArrow > -1 && firstSpectralArrow > -1) {
							arrowSlot = Math.min(firstSpectralArrow, firstTippedArrow);
						} else if (firstTippedArrow > -1 && firstSpectralArrow > -1 && firstArrow > -1) {
							arrowSlot = Math.min(firstSpectralArrow, Math.min(firstSpectralArrow, firstArrow));
						} else {
							return;
						}
						arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
						if (arrow.isShotFromCrossbow()) {
							playerInv.getItem(arrowSlot).setAmount(playerInv.getItem(arrowSlot).getAmount() + 1);
						} else {
							playerInv.setItem(arrowSlot, playerInv.getItem(arrowSlot));
						}
					}
				}
			}
		}
	}

}
