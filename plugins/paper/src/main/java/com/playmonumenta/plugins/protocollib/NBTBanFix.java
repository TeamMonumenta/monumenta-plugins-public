package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import de.tr7zw.nbtapi.NBT;
import java.util.Arrays;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

/**
 * Packet listener for stripping unncessary data from dropped items
 */
public class NBTBanFix extends PacketAdapter {

	public NBTBanFix(Plugin plugin) {
		super(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.ENTITY_METADATA);
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		PacketContainer packet = event.getPacket();
		//  https://wiki.vg/Entity_metadata#Item_Entity
		Entity entity = packet.getEntityModifier(event).read(0);
		if (entity == null || !(entity instanceof Item entityItem)) {
			return;
		}
		ItemStack realItem = entityItem.getItemStack();
		if (!ItemUtils.isShulkerBox(realItem.getType())) { // may want to modify other block entities such as chests and such
			return;
		}
		try {
			packet = packet.deepClone();
		} catch (RuntimeException e) {
			// sometimes, cloning just fails for some reason?
			if (e.getMessage() != null && e.getMessage().startsWith("Unable to clone")) {
				MMLog.warning("Failed to clone packet of type " + packet.getType());
				return;
			}
			throw e;
		}
		ItemStack item = realItem.clone();
		NBT.modify(item, nbt -> {
			nbt.removeKey("BlockEntityTag");
		});
		StructureModifier<List<WrappedDataValue>> watchableAccessor = packet.getDataValueCollectionModifier();
		watchableAccessor.write(0, List.of(new WrappedDataValue(8, WrappedDataWatcher.Registry.getItemStackSerializer(false), MinecraftReflection.getMinecraftItemStack(item))));
		event.setPacket(packet);
	}
}
