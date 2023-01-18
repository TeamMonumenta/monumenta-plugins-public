package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;

public class CrossbowListener implements Listener {

	private final Plugin mPlugin;

	public CrossbowListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void entityLoadCrossbowEvent(EntityLoadCrossbowEvent event) {
		Entity entity = event.getEntity();

		if (entity instanceof Player player) {
			mPlugin.mItemStatManager.onLoadCrossbow(mPlugin, player, event);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void entityShootBowEvent(EntityShootBowEvent event) {
		if (!(event.getProjectile() instanceof AbstractArrow arrow)) {
			return;
		}

		ItemStack crossbow = event.getBow();
		if (crossbow == null || crossbow.getType() != Material.CROSSBOW) {
			return;
		}

		// Handle Flame on crossbows (for both players and mobs) - though player items should only use the custom Fire Aspect enchantment
		if (crossbow.getEnchantmentLevel(Enchantment.ARROW_FIRE) > 0) {
			arrow.setFireTicks(100);
		}

		// Handle Punch on crossbows (for both players and mobs)
		int punch = crossbow.getEnchantmentLevel(Enchantment.ARROW_KNOCKBACK);
		if (punch > 0) {
			arrow.setKnockbackStrength(punch);
		}

		// For players: Handle Infinity on crossbows
		if (event.getEntity() instanceof Player player) {
			//Infinity gives arrow to player if the arrow shot had no potion nor custom effects
			if (crossbow.getEnchantmentLevel(Enchantment.ARROW_INFINITE) > 0
				    && arrow instanceof Arrow regularArrow
				    && !regularArrow.hasCustomEffects()
				    && regularArrow.getBasePotionData().getType() == PotionType.UNCRAFTABLE // plain arrow
				    && arrow.getPickupStatus() == AbstractArrow.PickupStatus.ALLOWED) {
				arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
				if (player.getGameMode() != GameMode.CREATIVE) {
					InventoryUtils.giveItem(player, new ItemStack(Material.ARROW), true);
				}
			}
		}
	}

}
