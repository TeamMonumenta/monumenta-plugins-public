package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Arrow.PickupStatus;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.entity.TippedArrow;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class ThrowingKnife implements BaseEnchantment {

	private static final String PROPERTY_NAME = ChatColor.GRAY + "Throwing Knife";
	private static final double ARROW_VELOCITY = 3.25;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND);
	}

	@Override
	public void onPlayerInteract(Plugin plugin, Player player, PlayerInteractEvent event, int level) {
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			ItemStack item = event.getItem();
			if (item != null && (item.getType() == Material.ARROW || item.getType() == Material.SPECTRAL_ARROW || item.getType() == Material.TIPPED_ARROW)) {
				if (player.getCooldown(item.getType()) <= 0) {
					if (!item.containsEnchantment(Enchantment.ARROW_INFINITE)) {
						item.setAmount(item.getAmount() - 1);
					}
					player.setCooldown(item.getType(), (int)(20 * 0.75));
					Location loc = player.getLocation();
					Vector dir = loc.getDirection();
					if (item.getType() == Material.ARROW) {
						Arrow arrow = player.launchProjectile(Arrow.class);
						arrow.setVelocity(dir.clone().multiply(ARROW_VELOCITY));
						arrow.setCritical(true);
					} else if (item.getType() == Material.SPECTRAL_ARROW) {
						Arrow arrow = player.launchProjectile(SpectralArrow.class);
						arrow.setVelocity(dir.clone().multiply(ARROW_VELOCITY));
						arrow.setCritical(true);
					} else if (item.getType() == Material.TIPPED_ARROW) {
						PotionMeta meta = (PotionMeta) item.getItemMeta();
						TippedArrow arrow = player.launchProjectile(TippedArrow.class);
						arrow.setBasePotionData(meta.getBasePotionData());
						if (meta.hasCustomEffects()) {
							for (PotionEffect effect : meta.getCustomEffects()) {
								arrow.addCustomEffect(effect, true);
							}
						}
						arrow.setVelocity(dir.clone().multiply(ARROW_VELOCITY));
						arrow.setCritical(true);
						arrow.setPickupStatus(PickupStatus.DISALLOWED);
					}

					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1.65f);
				}
			}
		}
	}
}
