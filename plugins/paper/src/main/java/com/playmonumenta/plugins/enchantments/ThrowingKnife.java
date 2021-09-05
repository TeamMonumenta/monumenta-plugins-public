package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

public class ThrowingKnife implements BaseEnchantment {

	private static final String METADATA_KEY = "ThrowingKnife";
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Throwing Knife";
	private static final double ARROW_VELOCITY = 3.25;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND);
	}

	@Override
	public boolean isMultiLevel() {
		return false;
	}

	public static boolean isThrowingKnife(AbstractArrow arrow) {
		return arrow.hasMetadata(METADATA_KEY);
	}

	@Override
	public void onPlayerInteract(Plugin plugin, Player player, PlayerInteractEvent event, int level) {
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			ItemStack item = event.getItem();
			if (item != null && (item.getType() == Material.ARROW || item.getType() == Material.SPECTRAL_ARROW || item.getType() == Material.TIPPED_ARROW)) {
				if (player.getCooldown(item.getType()) <= 0) {
					if (!item.containsEnchantment(Enchantment.ARROW_INFINITE) && player.getGameMode() != GameMode.CREATIVE) {
						item.setAmount(item.getAmount() - 1);
					}
					player.setCooldown(item.getType(), (int)(20 * 0.75));
					Location loc = player.getLocation();
					Vector dir = loc.getDirection();
					if (item.getType() == Material.ARROW) {
						Arrow arrow = player.launchProjectile(Arrow.class);
						arrow.setVelocity(dir.clone().multiply(ARROW_VELOCITY));
						arrow.setCritical(true);
						arrow.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, null));
					} else if (item.getType() == Material.SPECTRAL_ARROW) {
						SpectralArrow arrow = player.launchProjectile(SpectralArrow.class);
						arrow.setVelocity(dir.clone().multiply(ARROW_VELOCITY));
						arrow.setCritical(true);
						arrow.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, null));
					} else if (item.getType() == Material.TIPPED_ARROW) {
						PotionMeta meta = (PotionMeta) item.getItemMeta();
						Arrow arrow = player.launchProjectile(Arrow.class);
						arrow.setBasePotionData(meta.getBasePotionData());
						if (meta.hasCustomEffects()) {
							for (PotionEffect effect : meta.getCustomEffects()) {
								arrow.addCustomEffect(effect, true);
							}
						}
						arrow.setVelocity(dir.clone().multiply(ARROW_VELOCITY));
						arrow.setCritical(true);
						arrow.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, null));
						arrow.setPickupStatus(PickupStatus.DISALLOWED);
					}

					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1.65f);
				}
			}
		}
	}
}
