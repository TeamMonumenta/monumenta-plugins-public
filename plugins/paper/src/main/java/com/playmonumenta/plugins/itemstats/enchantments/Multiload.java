package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.ArrowConsumeEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.NBTItem;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;

public class Multiload implements Enchantment {

	private static final String AMMO_KEY = "RepeaterAmmo";

	@Override
	public String getName() {
		return "Multi-Load";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.MULTILOAD;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public void onConsumeArrow(Plugin plugin, Player player, double level, ArrowConsumeEvent event) {
		ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
		ItemStack arrow = event.getArrow();
		if (itemInMainHand.getType() == Material.CROSSBOW) {
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				useCrossbow(player, itemInMainHand, arrow, (int) level + 1);
				player.updateInventory();
			}, 1);
		}
	}

	@Override
	public void onLoadCrossbow(Plugin plugin, Player player, double level, EntityLoadCrossbowEvent event) {
		// When loading the crossbow, set level as ammo count.
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			ItemStack crossbow = event.getCrossbow();
			if (crossbow != null && crossbow.getItemMeta() instanceof CrossbowMeta crossbowMeta) {
				ItemStack ammoItem = crossbowMeta.getChargedProjectiles().get(0);
				int maxAmmo = (int) level + 1;
				int ammoToAccount = maxAmmo - 1;
				for (ItemStack itemStack : player.getInventory()) {
					// Loop through the player's inventory for the arrows that is the same.
					if (itemStack != null && itemStack.isSimilar(ammoItem)) {
						if (itemStack.getAmount() > ammoToAccount) {
							itemStack.setAmount(itemStack.getAmount() - ammoToAccount);
							ammoToAccount = 0;
						} else {
							ammoToAccount -= itemStack.getAmount();
							itemStack.setAmount(0);
						}
						if (ammoToAccount <= 0) {
							break;
						}
					}
				}
				loadCrossbow(player, crossbow, ammoItem, maxAmmo, maxAmmo - ammoToAccount);
			}
		}, 1);
	}

	public static void loadCrossbow(Player player, ItemStack crossbow, ItemStack ammoItem, int maxAmmo, int currentAmmo) {
		if (crossbow.getItemMeta() instanceof CrossbowMeta crossbowMeta) {
			if (currentAmmo > 0) {
				// If there are more than 0 ammo, reset crossbow.
				// Issue: Custom Arrows that adds projectile damage probably doesn't work.
				crossbowMeta.setChargedProjectiles(List.of(ammoItem));
				crossbow.setItemMeta(crossbowMeta);
			}
			player.sendActionBar(Component.text("Ammo: " + currentAmmo + " / " + maxAmmo, NamedTextColor.YELLOW));
			setAmmoCount(crossbow, currentAmmo);
		}
	}

	private static void useCrossbow(Player player, ItemStack crossbow, ItemStack ammoItem, int maxAmmo) {
		loadCrossbow(player, crossbow, ammoItem, maxAmmo, getAmmoCount(crossbow) - 1);
	}

	private static int getAmmoCount(ItemStack crossbow) {
		if (crossbow == null || crossbow.getType() != Material.CROSSBOW) {
			return 0;
		}
		return NBT.get(crossbow, nbt -> {
			return nbt.getOrDefault(AMMO_KEY, 0);
		});
	}

	private static void setAmmoCount(ItemStack crossbow, int amount) {
		if (crossbow == null || crossbow.getType() != Material.CROSSBOW) {
			return;
		}
		// Modifies the item directly to set amount of ammo
		NBTItem nbtItem = new NBTItem(crossbow);
		nbtItem.setInteger(AMMO_KEY, amount);
		crossbow.setItemMeta(nbtItem.getItem().getItemMeta());
	}
}
