package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.playmonumenta.plugins.Plugin;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Removes unnecessary info from equipment items to reduce network load.
 * Mostly benefits shulker boxes that by default have their entire contents sent to every other player nearby.
 */
public class EntityEquipmentReplacer extends PacketAdapter {

	public EntityEquipmentReplacer(Plugin plugin) {
		super(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.ENTITY_EQUIPMENT);
	}

	@Override
	public void onPacketSending(PacketEvent event) {

		// doc: https://wiki.vg/Protocol#Entity_Equipment

		PacketContainer packet = event.getPacket();
		List<Pair<EnumWrappers.ItemSlot, ItemStack>> items = packet.getSlotStackPairLists().read(0);
		for (Pair<EnumWrappers.ItemSlot, ItemStack> pair : items) {
			ItemStack item = pair.getSecond();
			if (!item.hasItemMeta()) {
				continue;
			}
			NBTItem nbtItem = new NBTItem(item);
			nbtItem.removeKey("BlockEntityTag"); // most important one - shulker contents
			nbtItem.removeKey("display"); // plain.display is still sent which is used by the RP
			nbtItem.removeKey("Monumenta"); // not needed
			nbtItem.removeKey("AttributeModifiers"); // not needed
			pair.setSecond(nbtItem.getItem());
		}
		packet.getSlotStackPairLists().write(0, items);

	}

}
