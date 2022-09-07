package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.playmonumenta.plugins.Plugin;
import java.util.List;
import javax.annotation.Nullable;

public class SpawnerEntityReplacer extends PacketAdapter {

	private @Nullable NbtCompound mSpawnDataReplacement = null;

	public SpawnerEntityReplacer(Plugin plugin) {
		super(plugin, PacketType.Play.Server.TILE_ENTITY_DATA, PacketType.Play.Server.MAP_CHUNK);
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		if (event.getPlayer().getScoreboardTags().contains("displaySpawnerEquipment")) {
			return;
		}
		if (event.getPacketType().equals(PacketType.Play.Server.TILE_ENTITY_DATA)) {
			PacketContainer packet = event.getPacket();
			for (NbtBase<?> base : packet.getNbtModifier().getValues()) {
				if (base != null) {
					stripNBT(NbtFactory.asCompound(base));
				}
			}
			event.setPacket(packet);
		} else if (event.getPacketType().equals(PacketType.Play.Server.MAP_CHUNK)) {
			PacketContainer packet = event.getPacket();
			for (List<NbtBase<?>> listNBT : packet.getListNbtModifier().getValues()) {
				for (NbtBase<?> base : listNBT) {
					if (base != null) {
						stripNBT(NbtFactory.asCompound(base));
					}
				}
			}
			event.setPacket(packet);
		}
	}

	private void stripNBT(NbtCompound nbt) {
		if (!nbt.containsKey("SpawnData")) {
			return;
		}
		/* Spawner !*/
		NbtCompound spawnData = nbt.getCompound("SpawnData");
		if (!spawnData.containsKey("entity")) {
			return;
		}
		NbtCompound entityData = spawnData.getCompound("entity");
		String id = entityData.getString("id");
		replaceSpawnDataSameId(spawnData, entityData, id);
	}

	private void replaceSpawnDataSameId(NbtCompound spawnData, NbtCompound entityData, String id) {
		updateSpawnDataReplacement(entityData);
		NbtCompound replacement = (NbtCompound) mSpawnDataReplacement.deepClone();
		replacement.put("id", id);
		spawnData.put("entity", replacement);
	}

	private void updateSpawnDataReplacement(NbtCompound entityData) {
		if (mSpawnDataReplacement == null) {
			/* No cached NbtCompound - need to clean this one out and cache it */
			mSpawnDataReplacement = (NbtCompound) entityData.deepClone();
			for (String key : entityData.getKeys()) {
				if (!key.equals("id") && !key.equals("name")) {
					mSpawnDataReplacement.remove(key);
				}
			}
			mSpawnDataReplacement.put("id", "minecraft:ender_pearl");
		}
	}

}
