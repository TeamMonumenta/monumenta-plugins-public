package com.playmonumenta.plugins.protocollib;

import com.bergerkiller.bukkit.common.wrappers.ChatText;
import com.bergerkiller.generated.com.mojang.authlib.GameProfileHandle;
import com.bergerkiller.generated.com.mojang.authlib.properties.PropertyHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacketHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacketHandle.EnumPlayerInfoActionHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacketHandle.PlayerInfoDataHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutEntityDestroyHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutNamedEntitySpawnHandle;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.Converters;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.managers.PlayerSkinManager;
import com.playmonumenta.plugins.managers.PlayerSkinManager.SkinData;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/*
 * Why aren't we using a better plugin like Citizens or something?
 * Because this works and is funny :suffer:
 * - U5B_
 */
public class CursedListener extends PacketAdapter {
	public CursedListener(Plugin plugin) {
		super(plugin, PacketType.Play.Server.SPAWN_ENTITY, PacketType.Play.Server.ENTITY_DESTROY, PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.ENTITY_EQUIPMENT, PacketType.Play.Server.ENTITY_HEAD_ROTATION);
	}

	// Humanoid-looking mobs
	// Similar hitboxes to Players, with the exception of Giants (Eldrask)
	public static final EnumSet<EntityType> WHITELISTED_ENTITIES = EnumSet.of(
		// villager-like
		EntityType.EVOKER,
		EntityType.ILLUSIONER,
		EntityType.PILLAGER,
		EntityType.VINDICATOR,
		EntityType.WITCH,
		// skeletons
		EntityType.SKELETON,
		EntityType.WITHER_SKELETON, // technically they are taller than players but who cares
		EntityType.STRAY,
		// zombies
		EntityType.ZOMBIE,
		EntityType.ZOMBIE_VILLAGER,
		EntityType.DROWNED,
		EntityType.HUSK,
		EntityType.GIANT, // Eldrask
		// piglins
		EntityType.PIGLIN,
		EntityType.PIGLIN_BRUTE,
		EntityType.ZOMBIFIED_PIGLIN,
		// creeper c:
		EntityType.CREEPER
		// netural/passive
		// EntityType.VILLAGER,
		// EntityType.WANDERING_TRADER
	);

	public static final EnumSet<EntityType> BLACKLISTED_ENTITIES = EnumSet.of(
		EntityType.ARMOR_STAND,
		EntityType.PLAYER,
		EntityType.ARROW
	);

	public static final Map<UUID, Map<Integer, UUID>> playerMapToEntities = new HashMap<>();
	private static final WrappedDataValue ENABLE_SECOND_SKIN_LAYER = new WrappedDataValue(17, Registry.get(Byte.class), (byte) (0x01 | 0x02 | 0x04 | 0x08 | 0x10 | 0x20 | 0x40));
	private static final String permission = "monumenta.aprilfools"; // failsafe in case this feature causes stupidity

	@Override
	public void onPacketSending(PacketEvent event) {
		Player player = event.getPlayer();
		// we want to replace the entity type with a player
		// surely nothing will go wrong
		PacketContainer packet = event.getPacket();
		PacketType packetType = event.getPacketType();
		if (packetType.equals(PacketType.Play.Server.SPAWN_ENTITY)) {
			// if player doesn't have the permission, don't spawn it in
			if (!player.hasPermission(permission)) {
				return;
			}
			// https://wiki.vg/index.php?title=Protocol&oldid=18242#Spawn_Entity
			Entity entity = packet.getEntityModifier(event).readSafely(0);
			if (entity == null || !entity.getType().isAlive() || !WHITELISTED_ENTITIES.contains(entity.getType())) {
				return;
			}
			SkinData skinData = null;
			// real hack here
			if (entity.getType().equals(EntityType.WITHER_SKELETON) && entity.getName().contains("Kaul")) {
				skinData = PlayerSkinManager.fetchSkinByName("kauletta42069"); // goofy override
			} else if (FastUtils.randomIntInRange(0, 100) > 5) {
				return;
			}
			// cancel the event here, because we don't want the player recieving two entity spawn packets with the same id
			event.setCancelled(true);
			spawnFakePlayer(player, entity, skinData, null);
		} else if (packetType.equals(PacketType.Play.Server.ENTITY_DESTROY)) {
			// https://wiki.vg/index.php?title=Protocol&oldid=18242#Remove_Entities
			PacketPlayOutEntityDestroyHandle entityHandle = PacketPlayOutEntityDestroyHandle.createHandle(packet.getHandle());
			int[] entityIds = entityHandle.getEntityIds();
			// we don't cancel this event, since we want these entities to also be removed on the client
			removeFakePlayer(player, entityIds);
		} else if (packetType.equals(PacketType.Play.Server.ENTITY_METADATA)) {
			rewriteFakePlayerMetadata(event);
		} else if (packetType.equals(PacketType.Play.Server.ENTITY_EQUIPMENT)) {
			// specifcally for kauletta :suffer:
			if (!player.hasPermission(permission)) {
				return;
			}
			Entity entity = packet.getEntityModifier(event).readSafely(0);
			if (entity == null || !(entity.getType().equals(EntityType.WITHER_SKELETON) && entity.getName().contains("Kaul"))) {
				return;
			}
			List<Pair<EnumWrappers.ItemSlot, ItemStack>> items = packet.getSlotStackPairLists().readSafely(0);
			if (items == null) {
				return;
			}
			for (Pair<EnumWrappers.ItemSlot, ItemStack> pair : items) {
				EnumWrappers.ItemSlot itemSlot = pair.getFirst();
				if (itemSlot.equals(EnumWrappers.ItemSlot.MAINHAND) || itemSlot.equals(EnumWrappers.ItemSlot.OFFHAND)) {
					continue;
				}
				pair.setSecond(new ItemStack(Material.AIR));
			}
			packet.getSlotStackPairLists().writeSafely(0, items);
		} else if (packetType.equals(PacketType.Play.Server.ENTITY_HEAD_ROTATION)) {
			@Nullable Map<Integer, UUID> entitiesList = playerMapToEntities.get(player.getUniqueId());
			if (entitiesList == null || entitiesList.isEmpty()) {
				return;
			}
			Entity entity = packet.getEntityModifier(event).readSafely(0);
			if (entity == null || !entitiesList.containsKey(entity.getEntityId())) {
				return;
			}
			// this is the only solution I could find for force-updating the body to match the head's rotation for fake players
			// this technically shouldn't be noticable but is a really hacky fix
			entity.setRotation(entity.getLocation().getYaw(), entity.getLocation().getPitch());
		}
	}

	/**
	 * Spawns a Fake Player on the client.
	 * A display name that is the same as the recievingPlayer's display name will trick the client into hiding it's username
	 * It also tricks usbplus into not showing a blank name
	 * on the sidebar, but still sets it to glowing
	 *
	 * @param recievingPlayer - Player that is recieving the packet
	 * @param entity          - Entity to create the packet off of
	 * @param skinData        - SkinData of the player from PlayerSkinManager or null
	 */
	public void spawnFakePlayer(Player recievingPlayer, Entity entity, @Nullable SkinData skinData, @Nullable String displayName) {
		UUID uuid = entity.getUniqueId(); // use entity uuid - surely this won't clash?
		// associate entity id and uuid with this player
		@Nullable Map<Integer, UUID> entitiesList = playerMapToEntities.get(recievingPlayer.getUniqueId());
		if (entitiesList == null) {
			entitiesList = new HashMap<>();
		}
		entitiesList.put(entity.getEntityId(), uuid);
		playerMapToEntities.put(recievingPlayer.getUniqueId(), entitiesList);

		// this must be sent before the fake player spawns in
		sendPlayerInfoPacket(recievingPlayer, entity, skinData, displayName);

		sendSpawnEntityPacket(recievingPlayer, entity);
	}

	public void sendSpawnEntityPacket(Player recievingPlayer, Entity entity) {
		// create actual player
		// https://wiki.vg/index.php?title=Protocol&oldid=18242#Spawn_Player
		// This packet gets removed in 1.20.2...
		PacketContainer playerPacket = new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
		playerPacket.getModifier().writeDefaults();
		PacketPlayOutNamedEntitySpawnHandle playerHandle = PacketPlayOutNamedEntitySpawnHandle.createHandle(playerPacket.getHandle());
		playerHandle.setEntityId(entity.getEntityId());
		playerHandle.setEntityUUID(entity.getUniqueId());
		playerHandle.setPosX(entity.getLocation().getX());
		playerHandle.setPosY(entity.getLocation().getY());
		playerHandle.setPosZ(entity.getLocation().getZ());
		playerHandle.setPitch(entity.getLocation().getPitch());
		playerHandle.setYaw(entity.getLocation().getYaw());
		ProtocolLibrary.getProtocolManager().sendServerPacket(recievingPlayer, playerPacket);
	}

	public void rewriteFakePlayerMetadata(PacketEvent event) {
		Player recievingPlayer = event.getPlayer();
		@Nullable Map<Integer, UUID> entitiesList = playerMapToEntities.get(recievingPlayer.getUniqueId());
		if (entitiesList == null || entitiesList.isEmpty()) {
			return;
		}
		PacketContainer entityPacket = event.getPacket();
		Entity entity = entityPacket.getEntityModifier(event).readSafely(0);
		if (entity == null || !entitiesList.containsKey(entity.getEntityId())) {
			return;
		}

		// deepcloning might not be necessary, but better safe than sorry since we are
		// modifying this packet, not resending it
		try {
			entityPacket = event.getPacket().deepClone();
		} catch (RuntimeException e) {
			// sometimes, cloning just fails for some reason?
			if (e.getMessage() != null && e.getMessage().startsWith("Unable to clone")) {
				MMLog.warning("Failed to clone packet of type " + event.getPacketType());
				return;
			}
			throw e;
		}

		// remove entities that conflict with https://wiki.vg/Entity_metadata#Player
		StructureModifier<List<WrappedDataValue>> watchableAccessor = entityPacket.getDataValueCollectionModifier();
		List<WrappedDataValue> metaItems = watchableAccessor.readSafely(0);
		if (metaItems == null || metaItems.isEmpty()) {
			return;
		}

		ListIterator<WrappedDataValue> metaIterator = metaItems.listIterator();
		while (metaIterator.hasNext()) {
			WrappedDataValue metadataItem = metaIterator.next();
			// hardcoded player exceptions
			int index = metadataItem.getIndex();
			Object value = metadataItem.getValue();
			// Additional hearts
			if (index == 15 && !(value instanceof Float)) {
				metaIterator.remove();
				continue;
			}
			// Score
			if (index == 16 && !(value instanceof Integer)) {
				metaIterator.remove();
				continue;
			}
			// we override this value (skin data)
			if (index == 17 /* && !(value instanceof Byte) */) {
				metaIterator.remove();
				continue;
			}
			// mainhand/offhand
			if (index == 18 && !(value instanceof Byte)) {
				metaIterator.remove();
				continue;
			}
			// parrot shoulder nbt
			if ((index == 19 || index == 20) && !(value instanceof NbtCompound)) {
				metaIterator.remove();
				continue;
			}
			if (index > 20) {
				metaIterator.remove();
				continue;
			}
		}
		// set second skin layer to show
		metaIterator.add(ENABLE_SECOND_SKIN_LAYER);
		watchableAccessor.writeSafely(0, metaItems);
		event.setPacket(entityPacket);
	}

	public void sendPlayerInfoPacket(Player recievingPlayer, Entity entity, @Nullable SkinData textureData, @Nullable String displayName) {
		// create player info packet
		// https://wiki.vg/index.php?title=Protocol&oldid=18242#Player_Info_Update
		PacketContainer playerInfoPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
		playerInfoPacket.getModifier().writeDefaults();
		ClientboundPlayerInfoUpdatePacketHandle playerInfoHandle = ClientboundPlayerInfoUpdatePacketHandle.createHandle(playerInfoPacket.getHandle());
		playerInfoHandle.setAction(EnumPlayerInfoActionHandle.ADD_PLAYER);
		// setting the fake player's name to the recievingPlayer's name makes the nametag completely invisible
		// setting the fake player's name to an empty string will show a nametag but it is very subtle (Wynncraft)
		// but we can set this string to anything, as long as it is within the 16 character limit
		GameProfileHandle playerData = GameProfileHandle.createNew(entity.getUniqueId(), displayName != null ? displayName : "");
		// use player's current skin
		// playerData.setAllProperties(GameProfileHandle.getForPlayer(recievingPlayer));
		if (textureData == null) {
			textureData = PlayerSkinManager.fetchFallbackSkin();
		}
		playerData.putProperty("textures", PropertyHandle.createNew("textures", textureData.texture(), textureData.signature()));
		playerInfoHandle.setPlayers(List.of(PlayerInfoDataHandle.createNew(playerInfoHandle, playerData, 0, GameMode.ADVENTURE, ChatText.fromMessage(""), false)));
		ProtocolLibrary.getProtocolManager().sendServerPacket(recievingPlayer, playerInfoPacket);
	}

	/**
	 * Remove Fake Player from client
	 * Relevant packets:
	 * - https://wiki.vg/index.php?title=Protocol&oldid=18242#Player_Info_Remove
	 * We convert a Remove_Entities packet into a Player_Info_Remove packet by getting the list of entities ids
	 * and checking the recieving player's map to send the correct uuid.
	 * @param recievingPlayer - Player that is recieving the packet
	 * @param entityIds - Entity ids to check/remove
	 */
	public void removeFakePlayer(Player recievingPlayer, int[] entityIds) {
		// Check if the player has any "fake" entities
		@Nullable
		Map<Integer, UUID> entitiesList = playerMapToEntities.get(recievingPlayer.getUniqueId());
		if (entitiesList == null || entitiesList.isEmpty()) {
			return;
		}
		// Lookup to see if one of the entity ids has a uuid match
		List<UUID> uuids = new ArrayList<>(1);
		for (int entityId : entityIds) {
			@Nullable
			UUID uuid = entitiesList.remove(entityId);
			if (uuid == null) {
				continue;
			}
			uuids.add(uuid);
		}
		if (uuids.isEmpty()) {
			return;
		}
		// https://wiki.vg/index.php?title=Protocol&oldid=18242#Player_Info_Remove
		PacketContainer playerInfoPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO_REMOVE);
		playerInfoPacket.getModifier().writeDefaults();
		// I tried to use BKCommonLib here but it failed to write a list of uuids for some reason
		playerInfoPacket.getLists(Converters.passthrough(UUID.class)).writeSafely(0, uuids);
		ProtocolLibrary.getProtocolManager().sendServerPacket(recievingPlayer, playerInfoPacket);
	}

	/**
	 * Remove cached Fake Player entity ids and uuids on logout
	 * To prevent the server for leaking memory
	 * @param uuid - UUID of the player to remove data for
	 */
	public static void removeFakePlayerEntities(UUID uuid) {
		playerMapToEntities.remove(uuid);
	}
}
