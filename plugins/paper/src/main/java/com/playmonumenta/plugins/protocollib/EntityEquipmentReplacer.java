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
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Removes unnecessary info from equipment items to reduce network load.
 * Mostly benefits shulker boxes that by default have their entire contents sent to every other player nearby.
 */
public class EntityEquipmentReplacer extends PacketAdapter {

	private final Plugin mPlugin;

	public EntityEquipmentReplacer(Plugin plugin) {
		super(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.ENTITY_EQUIPMENT);
		mPlugin = plugin;
	}

	@Override
	public void onPacketSending(PacketEvent event) {

		// doc: https://wiki.vg/Protocol#Entity_Equipment

		PacketContainer packet = event.getPacket();
		Entity entity = packet.getEntityModifier(event).read(0);
		VanityManager.VanityData vanityData = entity instanceof Player player && mPlugin.mVanityManager.getData(event.getPlayer()).mOtherVanityEnabled ? mPlugin.mVanityManager.getData(player) : null;
		List<Pair<EnumWrappers.ItemSlot, ItemStack>> items = packet.getSlotStackPairLists().read(0);
		for (Pair<EnumWrappers.ItemSlot, ItemStack> pair : items) {
			if (vanityData != null && pair.getSecond() != null && pair.getSecond().getType() != Material.AIR) {
				ItemStack vanity = vanityData.getEquipped(itemSlotToEquipmentSlot(pair.getFirst()));
				if (vanity != null && vanity.getType() != Material.AIR) {
					if (VanityManager.isInvisibleVanityItem(vanity)) {
						pair.setSecond(new ItemStack(Material.AIR));
					} else {
						pair.setSecond(ItemUtils.clone(vanity));
					}
				}
			}
			pair.setSecond(VanityManager.cleanCopyForDisplay(pair.getSecond()));
		}
		packet.getSlotStackPairLists().write(0, items);

	}

	private EquipmentSlot itemSlotToEquipmentSlot(EnumWrappers.ItemSlot slot) {
		return switch (slot) {
			case HEAD -> EquipmentSlot.HEAD;
			case CHEST -> EquipmentSlot.CHEST;
			case LEGS -> EquipmentSlot.LEGS;
			case FEET -> EquipmentSlot.FEET;
			case OFFHAND -> EquipmentSlot.OFF_HAND;
			case MAINHAND -> EquipmentSlot.HAND;
		};
	}

}
