package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import de.tr7zw.nbtapi.NBTItem;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class Multiload implements Enchantment {

	private static final String AMMO_KEY = "RepeaterAmmo";

	@Override
	public String getName() {
		return "Multi-Load";
	}

	@Override
	public ItemStatUtils.EnchantmentType getEnchantmentType() {
		return ItemStatUtils.EnchantmentType.MULTILOAD;
	}

	@Override
	public EnumSet<ItemStatUtils.Slot> getSlots() {
		return EnumSet.of(ItemStatUtils.Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 1000;
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, double level, ProjectileLaunchEvent event, Projectile projectile) {
		ItemStack itemInMainHand = player.getInventory().getItemInMainHand();

		if (itemInMainHand.getType() == Material.CROSSBOW &&
			projectile instanceof AbstractArrow arrow) {
			new BukkitRunnable() {
				@Override public void run() {
					updateItemStack(itemInMainHand, arrow);
					player.updateInventory();
					player.sendActionBar(Component.text("Ammo: " + getAmmoCount(itemInMainHand) + " / " + ((int) level + 1), NamedTextColor.YELLOW));
				}
			}.runTaskLater(plugin, 1);
		}
	}

	@Override
	public void onLoadCrossbow(Plugin plugin, Player player, double level, EntityLoadCrossbowEvent event) {
		// When loading the crossbow, set level as ammo count.
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			ItemStack crossbow = event.getCrossbow();
			CrossbowMeta meta = (CrossbowMeta) crossbow.getItemMeta();
			ItemStack arrowItem = meta.getChargedProjectiles().get(0);

			int maxArrows = (int) level + 1;
			int arrowsToAccount = maxArrows - 1;
			for (ItemStack itemStack : player.getInventory()) {
				// Loop through the player's inventory for the arrows that is the same.
				if (itemStack != null && itemStack.isSimilar(arrowItem)) {
					if (itemStack.getAmount() > arrowsToAccount) {
						itemStack.setAmount(itemStack.getAmount() - arrowsToAccount);
						arrowsToAccount = 0;
					} else {
						arrowsToAccount -= itemStack.getAmount();
						itemStack.setAmount(0);
					}

					if (arrowsToAccount <= 0) {
						break;
					}
				}
			}

			player.sendActionBar(Component.text("Ammo: " + (maxArrows - arrowsToAccount) + " / " + maxArrows, NamedTextColor.YELLOW));
			setAmmoCount(crossbow, maxArrows - arrowsToAccount);
		}, 1);
	}

	private void updateItemStack(ItemStack itemStack, AbstractArrow arrow) {
		if (itemStack.getType() != Material.CROSSBOW) {
			return;
		}

		CrossbowMeta meta = (CrossbowMeta) itemStack.getItemMeta();

		int ammoCount = getAmmoCount(itemStack);

		if (ammoCount > 1) {
			// If there are more than 1 charge, reset crossbow.
			// Issue: Custom Arrows that adds projectile damage probably doesn't work.
			ItemStack arrowItem = arrow.getItemStack();
			meta.addChargedProjectile(arrowItem);
			itemStack.setItemMeta(meta);
			setAmmoCount(itemStack, ammoCount - 1);
		} else {
			setAmmoCount(itemStack, 0);
		}
	}

	private void setAmmoCount(ItemStack itemStack, int amount) {
		if (itemStack == null || itemStack.getType().isAir()) {
			return;
		}

		// Modifies the item directly to set amount of ammo
		NBTItem nbtItem = new NBTItem(itemStack);
		nbtItem.setInteger(AMMO_KEY, amount);

		itemStack.setItemMeta(nbtItem.getItem().getItemMeta());
	}

	private int getAmmoCount(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType().isAir()) {
			return 0;
		}

		NBTItem nbtItem = new NBTItem(itemStack);

		if (!nbtItem.hasKey(AMMO_KEY)) {
			return 0;
		}

		return nbtItem.getInteger(AMMO_KEY);
	}
}
