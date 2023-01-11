package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import java.util.EnumSet;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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
import org.jetbrains.annotations.NotNull;

public class ThrowingKnife implements Enchantment {

	private static final double ARROW_VELOCITY = 3.25;
	private static final String METADATA_KEY = "ThrowingKnife";

	@Override
	public @NotNull String getName() {
		return "Throwing Knife";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.THROWING_KNIFE;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public void onPlayerInteract(Plugin plugin, Player player, double value, PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			ItemStack item = event.getItem();
			if (item != null && (item.getType() == Material.ARROW || item.getType() == Material.SPECTRAL_ARROW || item.getType() == Material.TIPPED_ARROW)) {
				if (player.getCooldown(item.getType()) <= 0) {
					if (ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.INFINITY) == 0 && player.getGameMode() != GameMode.CREATIVE) {
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

					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1, 1.65f);
				}
			}
		}
	}

	public static boolean isThrowingKnife(AbstractArrow arrow) {
		return arrow.hasMetadata(METADATA_KEY);
	}

}
