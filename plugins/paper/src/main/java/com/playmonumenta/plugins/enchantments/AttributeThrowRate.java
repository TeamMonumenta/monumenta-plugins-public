package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NmsUtils;

public class AttributeThrowRate implements BaseEnchantment {
	//Trident attribute only
	private static final String PROPERTY_NAME = " Throw Rate";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean negativeLevelsAllowed() {
		return true;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public int getLevelFromItem(ItemStack item, Player player, ItemSlot slot) {
		return InventoryUtils.getCustomAttribute(item, getProperty(), player, slot);
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		if (proj instanceof Trident) {
			Trident trident = (Trident)proj;
			ItemStack item = NmsUtils.getTridentItem(trident);

			//Only run Throw Rate if the Infinity enchantment is not on the trident
			if (item.getEnchantmentLevel(Enchantment.ARROW_INFINITE) <= 0) {
				//Make trident unpickupable, set cooldown, damage trident based on Unbreaking enchant
				player.setCooldown(item.getType(), (int)(20 * (10 / InventoryUtils.getAttributeValue(level))));
				trident.setPickupStatus(PickupStatus.CREATIVE_ONLY);

				//Replace item in hand so that it stays in inventory
				if (player.getInventory().getItemInMainHand().equals(item)) {
					ItemUtils.damageItemWithUnbreaking(item, 1, false);
					player.getInventory().setItemInMainHand(item);
				} else if (player.getInventory().getItemInOffHand().equals(item)) {
					ItemUtils.damageItemWithUnbreaking(item, 1, false);
					player.getInventory().setItemInOffHand(item);
				}
			}
		}
	}
}
