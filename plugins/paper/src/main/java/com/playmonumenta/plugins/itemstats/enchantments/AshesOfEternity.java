package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import java.util.EnumSet;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AshesOfEternity implements Enchantment {

	@Override
	public String getName() {
		return "Ashes of Eternity";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ASHES_OF_ETERNITY;
	}

	@Override public EnumSet<ItemStatUtils.Slot> getSlots() {
		return EnumSet.of(ItemStatUtils.Slot.MAINHAND, ItemStatUtils.Slot.OFFHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 9980; // before Resurrection
	}

	@Override
	public void onHurtFatal(Plugin plugin, Player player, double value, DamageEvent event) {

		if (Resurrection.execute(plugin, player, event, null)) {

			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_DEATH, 2, 2);

			// Remove Enchant
			ItemStack item = player.getInventory().getItemInMainHand();
			if (ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.ASHES_OF_ETERNITY) == 0) {
				item = player.getInventory().getItemInOffHand();
			}
			ItemStatUtils.removeEnchantment(item, EnchantmentType.ASHES_OF_ETERNITY);
			ItemStatUtils.generateItemStats(item);
			ItemStatManager.PlayerItemStats playerItemStats = plugin.mItemStatManager.getPlayerItemStats(player);
			if (playerItemStats != null) {
				playerItemStats.updateStats(player, true, player.getMaxHealth(), true);
			}

		}
	}
}
