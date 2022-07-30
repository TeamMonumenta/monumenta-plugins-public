package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.itemstats.enchantments.IntoxicatingWarmth;
import com.playmonumenta.plugins.itemstats.enchantments.JunglesNourishment;
import com.playmonumenta.plugins.itemstats.enchantments.LiquidCourage;
import com.playmonumenta.plugins.itemstats.enchantments.RageOfTheKeter;
import com.playmonumenta.plugins.itemstats.enchantments.TemporalBender;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class VirtualFoodReplacer extends PacketAdapter {

	private final Plugin mPlugin;

	// List of Infinity Food Enchantments
	private static final ItemStatUtils.EnchantmentType[] mEnchantmentTypes = {
		ItemStatUtils.EnchantmentType.JUNGLES_NOURISHMENT,
		ItemStatUtils.EnchantmentType.RAGE_OF_THE_KETER,
		ItemStatUtils.EnchantmentType.TEMPORAL_BENDER,
		ItemStatUtils.EnchantmentType.INTOXICATING_WARMTH,
		ItemStatUtils.EnchantmentType.LIQUID_COURAGE
	};

	public VirtualFoodReplacer(Plugin plugin) {
		super(plugin, PacketType.Play.Server.WINDOW_ITEMS, PacketType.Play.Server.SET_SLOT);
		mPlugin = plugin;
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		Player player = event.getPlayer();
		if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
			// Creative mode directly sends items to the server, so would break Firmaments.
			// Spectators don't see items anyway so skip them.
			return;
		}
		PacketContainer packet = event.getPacket();
		if (packet.getType().equals(PacketType.Play.Server.WINDOW_ITEMS)) {
			// doc: https://wiki.vg/Protocol#Window_Items
			if (packet.getIntegers().read(0) != 0) {
				// first int (should be a byte?) is the window ID, with ID 0 being the player inventory
				return;
			}
			for (List<ItemStack> items : packet.getItemListModifier().getValues()) {
				for (ItemStack item : items.subList(36, 46)) { // hotbar and offhand
					processItem(player, item);
				}
			}
		} else { // PacketType.Play.Server.SET_SLOT
			// doc: https://wiki.vg/Protocol#Set_Slot
			if (packet.getIntegers().read(0) != 0) {
				// first int (should be a byte?) is the window ID, with ID 0 being the player inventory
				return;
			}
			// second integer (should be first short?) is the slot ID
			int slot = packet.getIntegers().read(1);
			if (36 <= slot && slot < 46) {
				for (ItemStack itemStack : packet.getItemModifier().getValues()) {
					processItem(player, itemStack);
				}
			}
		}
	}

	public static boolean isVirtualFood(ItemStack itemStack) {
		for (ItemStatUtils.EnchantmentType enchantmentType : mEnchantmentTypes) {
			if (ItemStatUtils.getEnchantmentLevel(itemStack, enchantmentType) > 0) {
				return true;
			}
		}

		return false;
	}

	private void processItem(Player player, ItemStack itemStack) {
		if (isVirtualFood(itemStack)) {
			// If item is a virtual food, and if player is under effect of ItemCooldown by the enchantmentType
			for (ItemStatUtils.EnchantmentType enchantmentType : mEnchantmentTypes) {
				String source = ItemCooldown.toSource(enchantmentType);

				if (mPlugin.mEffectManager.hasEffect(player, source)) {
					// Then we need to replace the item in the packet.
					if (enchantmentType.equals(ItemStatUtils.EnchantmentType.JUNGLES_NOURISHMENT) && ItemStatUtils.getEnchantmentLevel(itemStack, ItemStatUtils.EnchantmentType.JUNGLES_NOURISHMENT) > 0) {
						itemStack.setType(JunglesNourishment.COOLDOWN_ITEM);
					}
					if (enchantmentType.equals(ItemStatUtils.EnchantmentType.RAGE_OF_THE_KETER) && ItemStatUtils.getEnchantmentLevel(itemStack, ItemStatUtils.EnchantmentType.RAGE_OF_THE_KETER) > 0) {
						itemStack.setType(RageOfTheKeter.COOLDOWN_ITEM);
					}

					if (enchantmentType.equals(ItemStatUtils.EnchantmentType.INTOXICATING_WARMTH) && ItemStatUtils.getEnchantmentLevel(itemStack, ItemStatUtils.EnchantmentType.INTOXICATING_WARMTH) > 0) {
						itemStack.setType(IntoxicatingWarmth.COOLDOWN_ITEM);
					}

					if (enchantmentType.equals(ItemStatUtils.EnchantmentType.TEMPORAL_BENDER) && ItemStatUtils.getEnchantmentLevel(itemStack, ItemStatUtils.EnchantmentType.TEMPORAL_BENDER) > 0) {
						itemStack.setType(TemporalBender.COOLDOWN_ITEM);
					}

					if (enchantmentType.equals(ItemStatUtils.EnchantmentType.LIQUID_COURAGE) && ItemStatUtils.getEnchantmentLevel(itemStack, ItemStatUtils.EnchantmentType.LIQUID_COURAGE) > 0) {
						itemStack.setType(LiquidCourage.COOLDOWN_ITEM);
					}
				}
			}
		}
	}

}
