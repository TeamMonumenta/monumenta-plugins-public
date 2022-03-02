package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.tracking.TrackingManager;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import io.papermc.paper.adventure.AdventureComponent;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Slime;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

public class PlayerTitlePacketAdapter extends PacketAdapter {

	private static class EntityMetadata {
		private final WrappedDataWatcher mDataWatcher;
		private final int mId;
		private final UUID mUuid;
		private final int mEntityType;

		private EntityMetadata(Entity entity) {
			mDataWatcher = WrappedDataWatcher.getEntityWatcher(entity);
			mId = entity.getEntityId();
			mUuid = entity.getUniqueId();
			mEntityType = NmsUtils.getVersionAdapter().getEntityTypeRegistryId(entity);
		}
	}

	private static class LineMetadata {
		private final EntityMetadata mArmorStand;
		private final EntityMetadata mSpacingEntity;

		public LineMetadata(EntityMetadata armorStand, EntityMetadata slime) {
			mArmorStand = armorStand;
			mSpacingEntity = slime;
		}
	}

	private static class PlayerMetadata {
		private final List<LineMetadata> mLines;
		private final List<Component> mDisplay;

		public PlayerMetadata(List<LineMetadata> lines, List<Component> display) {
			mLines = lines;
			mDisplay = display;
		}

		public boolean isMetadataEntity(int eid) {
			return mLines.stream().anyMatch(line -> line.mArmorStand.mId == eid || line.mSpacingEntity.mId == eid);
		}
	}

	private final ProtocolManager mProtocolManager;

	private static final Map<Integer, PlayerMetadata> METADATA = new HashMap<>();

	public PlayerTitlePacketAdapter(ProtocolManager protocolManager, Plugin plugin) {
		super(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.NAMED_ENTITY_SPAWN, PacketType.Play.Server.ENTITY_DESTROY);
		mProtocolManager = protocolManager;
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		if (event.getPacketType().equals(PacketType.Play.Server.NAMED_ENTITY_SPAWN)) {
			// doc: https://wiki.vg/Protocol#Spawn_Player

			Entity targetEntity = event.getPacket().getEntityModifier(event).read(0);

			if (!(targetEntity instanceof Player targetPlayer)) {
				return;
			}

			// Run the rest outside of the packet code, as it potentially modifies world state
			Bukkit.getScheduler().runTask(plugin, () -> {
				PlayerMetadata metadata = METADATA.computeIfAbsent(targetPlayer.getEntityId(), k -> createLines(targetPlayer, getDisplay(targetPlayer)));

				List<PacketContainer> packets = getSpawnLinesPackets(targetPlayer, metadata, 0);

				for (PacketContainer packet : packets) {
					if (!packet.getType().equals(PacketType.Play.Server.MOUNT)) {
						try {
							mProtocolManager.sendServerPacket(event.getPlayer(), packet, false);
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						}
					}
				}

				scheduleMountPackets(packets, event.getPlayer(), false);
			});
		} else { // ENTITY_DESTROY
			// doc: https://wiki.vg/Protocol#Destroy_Entities

			// Remove the fake entities along with the player
			int[] originalEntityIds = event.getPacket().getIntegerArrays().read(0);
			IntArrayList entityIds = new IntArrayList(originalEntityIds);
			boolean changed = false;
			for (int eid : originalEntityIds) {
				PlayerMetadata metadata = METADATA.get(eid);
				if (metadata != null) {
					for (LineMetadata line : metadata.mLines) {
						entityIds.add(line.mArmorStand.mId);
						entityIds.add(line.mSpacingEntity.mId);
						changed = true;
					}
				}
			}
			if (changed) {
				if (entityIds.isEmpty()) {
					event.setCancelled(true);
					return;
				}
				event.getPacket().getIntegerArrays().write(0, entityIds.toIntArray());
			}
		}
	}

	private List<PacketContainer> getSpawnLinesPackets(Player targetPlayer, PlayerMetadata metadata, int startLine) {
		List<PacketContainer> result = new ArrayList<>();
		int lastEntity = startLine == 0 ? targetPlayer.getEntityId() : metadata.mLines.get(startLine - 1).mArmorStand.mId;
		List<LineMetadata> linesToSend = metadata.mLines.subList(startLine, metadata.mLines.size());
		for (LineMetadata line : linesToSend) {
			// spawn armor stand
			// doc: https://wiki.vg/Protocol#Spawn_Living_Entity
			{
				PacketContainer packet = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
				packet.getIntegers().write(0, line.mArmorStand.mId); // id
				packet.getUUIDs().write(0, line.mArmorStand.mUuid); // uuid
				packet.getIntegers().write(1, line.mArmorStand.mEntityType); // type
				packet.getDoubles().write(0, targetPlayer.getLocation().getX()) // position: 256 blocks below the target player (to spawn in range but out of sight)
					.write(1, targetPlayer.getLocation().getY() - 256)
					.write(2, targetPlayer.getLocation().getZ());
				// other fields: keep defaults (0 velocity, 0 pitch/yaw/headYaw)
				result.add(packet);
			}
			// spawn spacing entity
			{
				PacketContainer packet = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
				packet.getIntegers().write(0, line.mSpacingEntity.mId); // id
				packet.getUUIDs().write(0, line.mSpacingEntity.mUuid); // uuid
				packet.getIntegers().write(1, line.mSpacingEntity.mEntityType); // type
				packet.getDoubles().write(0, targetPlayer.getLocation().getX()) // position: 256 blocks below the target player
					.write(1, targetPlayer.getLocation().getY() - 256)
					.write(2, targetPlayer.getLocation().getZ());
				// other fields: keep defaults (0 velocity, 0 pitch/yaw/headYaw)
				result.add(packet);
			}

			// set armor stand metadata
			// doc: https://wiki.vg/Protocol#Entity_Metadata
			{
				PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
				packet.getIntegers().write(0, line.mArmorStand.mId); // entity id
				packet.getWatchableCollectionModifier().write(0, line.mArmorStand.mDataWatcher.getWatchableObjects()); // data watcher objects
				result.add(packet);
			}
			// set spacing entity metadata
			{
				PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
				packet.getIntegers().write(0, line.mSpacingEntity.mId); // entity id
				packet.getWatchableCollectionModifier().write(0, line.mSpacingEntity.mDataWatcher.getWatchableObjects()); // data watcher objects
				result.add(packet);
			}

			// make the spacing entity ride on the player or the previous armor stand
			// doc: https://wiki.vg/Protocol#Set_Passengers
			{
				PacketContainer packet = new PacketContainer(PacketType.Play.Server.MOUNT);
				packet.getIntegers().write(0, lastEntity); // vehicle id
				packet.getIntegerArrays().write(0, new int[] {line.mSpacingEntity.mId}); // passengers
				result.add(packet);
			}
			// make the armor stand ride on the spacing entity
			{
				PacketContainer packet = new PacketContainer(PacketType.Play.Server.MOUNT);
				packet.getIntegers().write(0, line.mSpacingEntity.mId); // vehicle id
				packet.getIntegerArrays().write(0, new int[] {line.mArmorStand.mId}); // passengers
				result.add(packet);
			}
			lastEntity = line.mArmorStand.mId;
		}

		// assign spacing entities to the unpushable team to not make them push players around
		// doc: https://wiki.vg/Protocol#Teams
		{
			PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
			packet.getStrings().write(0, TrackingManager.UNPUSHABLE_TEAM); // team name
			packet.getIntegers().write(0, 3); // mode 3: add entity to team
			packet.getModifier().withType(Collection.class)
				.write(0, new ArrayList<>(linesToSend.stream().map(line -> line.mSpacingEntity.mUuid.toString()).toList())); // entity UUIDs
			result.add(packet);
		}

		return result;
	}

	// The client somehow handles mount packets before the other packets, so delay them by 1 tick.
	// Sometimes, even a 1-tick delay is insufficient, so keep sending these for a short while.
	private void scheduleMountPackets(List<PacketContainer> packets, Player player, boolean broadcast) {
		List<PacketContainer> mountPackets = packets.stream().filter(packet -> packet.getType().equals(PacketType.Play.Server.MOUNT)).toList();
		if (mountPackets.isEmpty()) {
			return;
		}
		new BukkitRunnable() {
			int mRepetitions = 0;

			@Override
			public void run() {
				for (PacketContainer packet : mountPackets) {
					// if the entities have since been changed, stop this runnable
					if (IntStream.of(packet.getIntegerArrays().read(0)).anyMatch(eid -> METADATA.values().stream().noneMatch(md -> md.isMetadataEntity(eid)))) {
						cancel();
						return;
					}
					if (broadcast) {
						broadcastPacketNoFilters(packet, player);
					} else {
						try {
							mProtocolManager.sendServerPacket(player, packet, false);
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						}
					}
				}
				mRepetitions++;
				if (mRepetitions >= 5) {
					cancel();
				}
			}
		}.runTaskTimer(plugin, 1, 10);
	}

	private static PlayerMetadata createLines(Player targetPlayer, List<Component> display) {
		List<LineMetadata> lines = new ArrayList<>();
		for (int i = 0; i < display.size(); i++) {
			lines.add(createLine(targetPlayer, display.get(i), i == 0));
		}
		return new PlayerMetadata(lines, display);
	}

	private static LineMetadata createLine(Player targetPlayer, Component text, boolean lowest) {
		ArmorStand armorStand = (ArmorStand) NmsUtils.getVersionAdapter().spawnWorldlessEntity(EntityType.ARMOR_STAND, targetPlayer.getWorld());
		armorStand.setMarker(true);
		armorStand.setInvisible(true);
		armorStand.setInvulnerable(true);
		armorStand.customName(text);
		armorStand.setCustomNameVisible(true);
		armorStand.setGravity(false);
		armorStand.setSmall(true);
		armorStand.setBasePlate(false);
		armorStand.setCollidable(false);
		EntityMetadata armorStandMetadata = new EntityMetadata(armorStand);

		LivingEntity spacingEntity;
		if (lowest) {
			Slime slime = (Slime) NmsUtils.getVersionAdapter().spawnWorldlessEntity(EntityType.SLIME, targetPlayer.getWorld());
			slime.setSize(1);
			spacingEntity = slime;
		} else {
			spacingEntity = (Silverfish) NmsUtils.getVersionAdapter().spawnWorldlessEntity(EntityType.SILVERFISH, targetPlayer.getWorld());
		}
		spacingEntity.setAI(false);
		spacingEntity.setInvisible(true);
		spacingEntity.setInvulnerable(true);
		spacingEntity.setSilent(true);
		spacingEntity.setGravity(false);
		armorStand.setCollidable(false);
		EntityMetadata spacingEntityMetadata = new EntityMetadata(spacingEntity);

		return new LineMetadata(armorStandMetadata, spacingEntityMetadata);
	}

	private static List<Component> getDisplay(Player player) {
		List<Component> result = new ArrayList<>();

		// lowest: optional title
		Cosmetic title = CosmeticsManager.getInstance().getActiveCosmetic(player, CosmeticType.TITLE);
		if (title != null) {
			result.add(Component.text(title.mName, NamedTextColor.GRAY));
		}

		// middle: health
		int health = (int) Math.round(player.getHealth());
		int maxHealth = (int) Math.round(EntityUtils.getMaxHealth(player));
		float redFactor = Math.max(0, Math.min(1, 1.25f * health / maxHealth - 0.25f)); // 100% red at 20% HP or below, white at full HP
		Component healthLine = Component.text(health + "/" + maxHealth + " \u2665", TextColor.color(1f, redFactor, redFactor));
		int absorption = (int) Math.round(AbsorptionUtils.getAbsorption(player));
		if (absorption > 0) {
			healthLine = healthLine.append(Component.text(" +" + absorption, NamedTextColor.YELLOW));
		}
		result.add(healthLine);

		// top: name
		result.add(Component.text(player.getName(), NamedTextColor.WHITE));

		return result;
	}

	// called every 2 ticks
	public void tick() {

		Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
		for (Player player : onlinePlayers) {
			PlayerMetadata metadata = METADATA.get(player.getEntityId());
			if (metadata == null || !player.isValid()) {
				return;
			}
			List<Component> display = getDisplay(player);
			if (!metadata.mDisplay.equals(display) || player.getTicksLived() % 200 == 0) {
				// display has changed, update
				// also just update every 10 seconds because some teleportation/respawn stuff breaks the current implementation

				// create new lines
				int existingSize = metadata.mLines.size();
				if (display.size() > existingSize) {
					while (display.size() > metadata.mLines.size()) {
						metadata.mLines.add(createLine(player, display.get(metadata.mLines.size()), metadata.mLines.size() == 0));
					}
					List<PacketContainer> packets = getSpawnLinesPackets(player, metadata, existingSize);
					for (PacketContainer packet : packets) {
						if (!packet.getType().equals(PacketType.Play.Server.MOUNT)) {
							broadcastPacketNoFilters(packet, player);
						}
					}
					scheduleMountPackets(packets, player, true);
				}

				// delete removed lines
				if (display.size() < metadata.mLines.size()) {
					List<LineMetadata> subList = metadata.mLines.subList(display.size(), metadata.mLines.size());

					PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
					packet.getIntegerArrays().write(0, subList.stream().flatMapToInt(line -> IntStream.of(line.mArmorStand.mId, line.mSpacingEntity.mId)).toArray());
					broadcastPacketNoFilters(packet, player);

					subList.clear();
				}

				// update changed lines
				int updatedSize = Math.min(existingSize, display.size());
				for (int i = 0; i < updatedSize; i++) {
					if (!display.get(i).equals(metadata.mDisplay.get(i))) {
						EntityMetadata armorStand = metadata.mLines.get(i).mArmorStand;
						WrappedWatchableObject nameWatchableObject = null;
						for (WrappedWatchableObject watchableObject : armorStand.mDataWatcher.getWatchableObjects()) {
							if (watchableObject.getValue() instanceof Optional<?> optional
								    && optional.isPresent()
								    && optional.get() instanceof AdventureComponent) {
								watchableObject.setValue(Optional.of(new AdventureComponent(display.get(i))));
								nameWatchableObject = watchableObject;
								break;
							}
						}
						if (nameWatchableObject != null) {
							PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
							packet.getIntegers().write(0, armorStand.mId);
							packet.getWatchableCollectionModifier().write(0, new ArrayList<>(List.of(nameWatchableObject))); // protocollib needs an ArrayList
							broadcastPacketNoFilters(packet, player);
						}
					}
				}

				metadata.mDisplay.clear();
				metadata.mDisplay.addAll(display);

			}
		}

		// clean up old player metadata
		METADATA.keySet().removeIf(id -> onlinePlayers.stream().noneMatch(p -> id == p.getEntityId()));

	}

	/**
	 * Broadcasts a packet to all trackers of the given entity, ignoring packet filters/listeners.
	 */
	private void broadcastPacketNoFilters(PacketContainer packet, Entity entity) {
		for (Player tracker : mProtocolManager.getEntityTrackers(entity)) {
			try {
				mProtocolManager.sendServerPacket(tracker, packet, false);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

}
