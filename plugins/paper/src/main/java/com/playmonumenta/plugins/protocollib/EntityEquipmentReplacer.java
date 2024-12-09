package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.VanityManager;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Removes unnecessary info from equipment items to reduce network load.
 * Mostly benefits shulker boxes that by default have their entire contents sent to every other player nearby.
 */
public class EntityEquipmentReplacer extends PacketAdapter {
	public static final String SCOREBOARD = "ShouldDisplayOtherPlayerGear";

	private final Plugin mPlugin;

	public EntityEquipmentReplacer(Plugin plugin) {
		super(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.ENTITY_EQUIPMENT);
		mPlugin = plugin;
	}

	@Override
	public void onPacketSending(PacketEvent event) {

		// doc: https://wiki.vg/Protocol#Set_Equipment

		PacketContainer packet = event.getPacket();
		Entity entity = packet.getEntityModifier(event).read(0);
		VanityManager.VanityData vanityData = entity instanceof Player player && mPlugin.mVanityManager.getData(event.getPlayer()).mOtherVanityEnabled ? mPlugin.mVanityManager.getData(player) : null;

		boolean shouldClear =
			// we can only clear items for players
			entity instanceof Player &&
			// and for a player that is not the current player
			!entity.getUniqueId().equals(event.getPlayer().getUniqueId()) &&
			// and if the player that the packet is sent to has gear disabled
			ScoreboardUtils.getScoreboardValue(event.getPlayer(), SCOREBOARD).orElse(1) == 0;

		List<Pair<EnumWrappers.ItemSlot, ItemStack>> items = packet.getSlotStackPairLists().read(0);
		for (Pair<EnumWrappers.ItemSlot, ItemStack> pair : items) {
			if (shouldClear) {
				pair.setSecond(new ItemStack(Material.AIR));
			} else {
				ItemStack itemStack = pair.getSecond();
				if (vanityData != null && itemStack != null && itemStack.getType() != Material.AIR) {
					ItemStack vanity = vanityData.getEquipped(itemSlotToEquipmentSlot(pair.getFirst()));
					if (vanity != null && vanity.getType() != Material.AIR) {
						if (VanityManager.isInvisibleVanityItem(vanity)) {
							pair.setSecond(new ItemStack(Material.AIR));
						} else {
							pair.setSecond(ItemUtils.clone(vanity));
						}
					}
				}
				if (entity instanceof Player player
					    && pair.getFirst() == EnumWrappers.ItemSlot.MAINHAND
					    && itemStack != null
					    && PlayerUtils.isAlchemist(player)
					    && ItemUtils.isAlchemistItem(itemStack)) {
					// Alchemical Utensils
					itemStack = ItemUtils.clone(itemStack);
					VirtualItemsReplacer.handleAlchemistPotion(player, itemStack);
					pair.setSecond(itemStack);
				}
			}
			pair.setSecond(VanityManager.cleanCopyForDisplay(pair.getSecond()));
		}
		packet.getSlotStackPairLists().write(0, items);

	}

	private @Nullable EquipmentSlot itemSlotToEquipmentSlot(EnumWrappers.ItemSlot slot) {
		// There's a weird "body" item slot but no equipment slot.
		// Might be something that's changed in future versions
		return switch (slot) {
			case HEAD -> EquipmentSlot.HEAD;
			case CHEST -> EquipmentSlot.CHEST;
			case LEGS -> EquipmentSlot.LEGS;
			case FEET -> EquipmentSlot.FEET;
			case OFFHAND -> EquipmentSlot.OFF_HAND;
			case MAINHAND -> EquipmentSlot.HAND;
			default -> null;
		};
	}

}
