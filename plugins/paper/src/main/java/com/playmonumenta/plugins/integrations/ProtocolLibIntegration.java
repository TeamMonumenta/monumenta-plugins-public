package com.playmonumenta.plugins.integrations;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.GlowingCommand;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

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

		if (!ServerProperties.getReplaceSpawnerEntities()) {
			mLogger.info("Will not replace spawner entities on this shard");
			return;
		} else {
			mLogger.info("Enabling replacement of spawner entities");
		}

		ProtocolManager manager = ProtocolLibrary.getProtocolManager();

		// packet listener for selectively disabling mob equipment in spawners
		manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL,
		                                            PacketType.Play.Server.TILE_ENTITY_DATA,
		                                            PacketType.Play.Server.MAP_CHUNK) {
			@Override
			public void onPacketSending(PacketEvent event) {
				if (event.getPacketType().equals(PacketType.Play.Server.TILE_ENTITY_DATA) ||
						event.getPacketType().equals(PacketType.Play.Server.MAP_CHUNK)) {

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
			}
		});

		// packet listener for selectively disabling the glowing effect
		manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL,
		                                            PacketType.Play.Server.ENTITY_METADATA,
		                                            PacketType.Play.Server.SPAWN_ENTITY,
		                                            PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB,
		                                            PacketType.Play.Server.SPAWN_ENTITY_LIVING,
		                                            PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
			private static final byte GLOWING_BIT = 0b01000000;

			@Override
			public void onPacketSending(PacketEvent event) {
				Player player = event.getPlayer();
				int playerSettings = ScoreboardUtils.getScoreboardValue(player, GlowingCommand.SCOREBOARD_OBJECTIVE).orElse(0);
				if (playerSettings == 0) { // everything enabled, return immediately for a slight performance gain
					return;
				}
				PacketContainer packet = event.getPacket();

				// Find the watchable objects containing the entity flags.
				// It is a byte and is at index 0, see https://wiki.vg/Entity_metadata#Entity
				// Different packet types have the value at different locations, so just use and modify whichever are present.
				List<WrappedWatchableObject> wrappedWatchableObjects = packet.getWatchableCollectionModifier().getValues().stream()
						.flatMap(Collection::stream)
						.filter(wwo -> wwo.getIndex() == 0 && wwo.getRawValue() instanceof Byte b && (b & GLOWING_BIT) != 0)
						.toList();
				List<WrappedDataWatcher> dataWatcherModifiers = packet.getDataWatcherModifier().getValues().stream()
						.filter(dw -> dw.hasIndex(0) && dw.getObject(0) instanceof Byte b && (b & GLOWING_BIT) != 0)
						.toList();
				if (wrappedWatchableObjects.isEmpty() && dataWatcherModifiers.isEmpty()) { // no glowing bit is set, so there's nothing to do
					return;
				}

				// check if glowing is disabled for the entity's type.
				if (playerSettings != 0xFFFFFFFF) { // If all glowing is disabled, this check can be skipped.
					int entityId = packet.getIntegers().read(0);
					Entity entity = NmsUtils.getEntityById(player.getWorld(), entityId);
					if (entity == null || GlowingCommand.isGlowingEnabled(playerSettings, entity)) {
						return;
					}
				}

				// Finally, unset the glowing bits
				for (WrappedWatchableObject wrappedWatchableObject : wrappedWatchableObjects) {
					wrappedWatchableObject.setValue((byte) (((Byte) wrappedWatchableObject.getValue()) & ~GLOWING_BIT));
				}
				for (WrappedDataWatcher dataWatcherModifier : dataWatcherModifiers) {
					dataWatcherModifier.setObject(0, (byte) (dataWatcherModifier.getByte(0) & ~GLOWING_BIT));
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
			mSpawnDataReplacement = (NbtCompound) spawnData.deepClone();
			for (String key : mSpawnDataReplacement.getKeys()) {
				if (!key.equals("id") && !key.equals("name")) {
					mSpawnDataReplacement.remove(key);
				}
			}
			mSpawnDataReplacement.put("id", "minecraft:ender_pearl");
		}
	}
}
