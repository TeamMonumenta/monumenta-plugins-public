package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.EnumSet;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class ThrowingKnife implements Enchantment {

	private static final float ARROW_VELOCITY = 3.0f;
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
			ItemStack item = player.getInventory().getItemInMainHand();
			int arrowCooldown = (int) (20 / Plugin.getInstance().mItemStatManager.getAttributeAmount(player, AttributeType.THROW_RATE));

			if (player.getCooldown(item.getType()) <= 0) {
				player.setCooldown(item.getType(), arrowCooldown);
				Arrow arrow;
				Location eyeLoc = player.getEyeLocation();
				Vector dir = eyeLoc.getDirection();
				World world = player.getWorld();
				if (item.getType() == Material.TIPPED_ARROW) {
					PotionMeta meta = (PotionMeta) item.getItemMeta();
					arrow = world.spawnArrow(eyeLoc, dir, ARROW_VELOCITY, 0, Arrow.class);
					arrow.setBasePotionType(meta.getBasePotionType());
					if (meta.hasCustomEffects()) {
						for (PotionEffect effect : meta.getCustomEffects()) {
							arrow.addCustomEffect(effect, true);
						}
					}
				} else {
					arrow = world.spawnArrow(eyeLoc, dir, ARROW_VELOCITY, 0, Arrow.class);
				}
				arrow.setCritical(true);
				arrow.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, null));
				arrow.setPickupStatus(PickupStatus.DISALLOWED);

				if (ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.INFINITY) == 0 && player.getGameMode() != GameMode.CREATIVE) {
					item.setAmount(item.getAmount() - 1);
				}

				arrow.setShooter(player);
				arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);

				ProjectileLaunchEvent eventLaunch = new ProjectileLaunchEvent(arrow);
				Bukkit.getPluginManager().callEvent(eventLaunch);
				if (eventLaunch.isCancelled()) {
					return;
				}

				ItemUtils.damageItemWithUnbreaking(plugin, player, item, 1, true);

				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1, 1.65f);

				Oversized.onOversizedShoot(player, true);
			}
		}
	}

	public static boolean isThrowingKnife(AbstractArrow arrow) {
		return arrow.hasMetadata(METADATA_KEY);
	}

}
