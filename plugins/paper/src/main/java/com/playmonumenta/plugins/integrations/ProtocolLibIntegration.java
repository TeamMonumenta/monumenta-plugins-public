package com.playmonumenta.plugins.integrations;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.playmonumenta.plugins.Plugin;

import org.bukkit.event.Listener;

public class ProtocolLibIntegration implements Listener {
	private final Logger mLogger;

	private final Set<String> mRemoveIds = new HashSet<String>(Arrays.asList(
		"minecraft:bee",
		"minecraft:enderman",
		"minecraft:polar_bear",
		"minecraft:wolf",
		"minecraft:iron_golem",
		"minecraft:zombified_piglin"
	));
	private NbtCompound mSpawnDataReplacement = null;

	public ProtocolLibIntegration(Plugin plugin) {
		mLogger = plugin.getLogger();
		mLogger.info("Enabling ProtocolLib integration");

		ProtocolManager manager = ProtocolLibrary.getProtocolManager();
		manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL,
													PacketType.Play.Server.TILE_ENTITY_DATA,
													PacketType.Play.Server.MAP_CHUNK) {
			@Override
			public void onPacketSending(PacketEvent event) {

				if (event.getPacketType().equals(PacketType.Play.Server.TILE_ENTITY_DATA)) {
					// mLogger.info("GOT TILE_ENTITY_DATA");
					// mLogger.info(event.getPacket().toString());
					PacketContainer packet = event.getPacket();
					for (NbtBase<?> base : packet.getNbtModifier().getValues()) {
						stripNBT(NbtFactory.asCompound(base));
					}
					event.setPacket(packet);
				} else if (event.getPacketType().equals(PacketType.Play.Server.MAP_CHUNK)) {
					// mLogger.info("GOT MAP_CHUNK PACKET");
					// mLogger.info(event.getPacket().toString());
					PacketContainer packet = event.getPacket();
					for (List<NbtBase<?>> listNBT : packet.getListNbtModifier().getValues()) {
						for (NbtBase<?> base : listNBT) {
							stripNBT(NbtFactory.asCompound(base));
						}
					}
					event.setPacket(packet);
				}
			}
		});
	}

	private void stripNBT(NbtCompound nbt) {
		if (nbt.containsKey("SpawnData")) {
			/* Spawner !*/
			NbtCompound spawnData = nbt.getCompound("SpawnData");
			String id = spawnData.getString("id");
			// mLogger.info(nbt.toString());
			if (mRemoveIds.contains(id)) {
				/* Contains one of the disallowed spawner types that the client can't render */
				replaceSpawnData(nbt, spawnData);
			} else {
				/* Remove all the data from the mob except the mob itself */
				replaceSpawnDataSameId(nbt, spawnData, id);
			}
		}
	}


	private void replaceSpawnData(NbtCompound spawnerNbt, NbtCompound spawnData) {
		updateSpawnDataReplacement(spawnData);
		spawnerNbt.put("SpawnData", mSpawnDataReplacement.deepClone());
	}

	private void replaceSpawnDataSameId(NbtCompound spawnerNbt, NbtCompound spawnData, String id) {
		updateSpawnDataReplacement(spawnData);
		NbtCompound replacement = (NbtCompound) mSpawnDataReplacement.deepClone();
		replacement.put("id", id);
		spawnerNbt.put("SpawnData", replacement);
	}

	private void updateSpawnDataReplacement(NbtCompound spawnData) {
		if (mSpawnDataReplacement == null) {
			/* No cached NbtCompound - need to clean this one out and cache it */
			mSpawnDataReplacement = (NbtCompound)spawnData.deepClone();
			for (String key : mSpawnDataReplacement.getKeys()) {
				if (!key.equals("id") && !key.equals("name")) {
					mSpawnDataReplacement.remove(key);
				}
			}
			mSpawnDataReplacement.put("id", "minecraft:ender_pearl");
		}
	}
}
