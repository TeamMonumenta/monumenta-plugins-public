package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import io.papermc.paper.adventure.AdventureComponent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class PlayerTitleManager {

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
		private final double mHeight;

		public LineMetadata(EntityMetadata armorStand, double height) {
			mArmorStand = armorStand;
			mHeight = height;
		}
	}

	private static class PlayerMetadata {
		private final List<LineMetadata> mLines;
		private final List<Component> mDisplay;
		private final Set<UUID> mVisibleToPlayers = new HashSet<>();
		private Location mLastLocation;

		public PlayerMetadata(List<LineMetadata> lines, List<Component> display, Location location) {
			mLines = lines;
			mDisplay = display;
			mLastLocation = location;
		}
	}

	private final ProtocolManager mProtocolManager;

	private static final Map<UUID, PlayerMetadata> METADATA = new HashMap<>();

	public PlayerTitleManager(ProtocolManager protocolManager) {
		mProtocolManager = protocolManager;
	}

	public void tick() {

		Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
		for (Player player : onlinePlayers) {
			PlayerMetadata metadata = METADATA.get(player.getUniqueId());

			// If the player became invalid (died, maybe more) or became spectator/vanished remove all titles
			if (!player.isValid() || PremiumVanishIntegration.isInvisibleOrSpectator(player)) {
				if (metadata != null) {
					METADATA.remove(player.getUniqueId());
					if (!metadata.mVisibleToPlayers.isEmpty()) {
						PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
						packet.getIntegerArrays().write(0, metadata.mLines.stream().mapToInt(line -> line.mArmorStand.mId).toArray());
						broadcastPacketNoFilters(packet, player, metadata.mVisibleToPlayers);
					}
				}
				continue;
			}

			// Force new entities to be created when the player switches worlds
			if (metadata != null && !player.getWorld().equals(metadata.mLastLocation.getWorld())) {
				METADATA.remove(player.getUniqueId());
				metadata = null;
			}

			// Create new entities if necessary
			if (metadata == null) {
				metadata = createLines(player, getDisplay(player));
				METADATA.put(player.getUniqueId(), metadata);
			}

			// remove entities from players no longer in range
			List<Player> trackers = mProtocolManager.getEntityTrackers(player);
			for (Iterator<UUID> iterator = metadata.mVisibleToPlayers.iterator(); iterator.hasNext(); ) {
				UUID visibleToPlayer = iterator.next();
				if (trackers.stream().noneMatch(tracker -> tracker.getUniqueId().equals(visibleToPlayer))) {
					iterator.remove();
					Player otherPlayer = Bukkit.getPlayer(visibleToPlayer);
					if (otherPlayer != null) {
						PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
						packet.getIntegerArrays().write(0, metadata.mLines.stream().mapToInt(line -> line.mArmorStand.mId).toArray());
						sendPacketNoFilters(otherPlayer, packet);
					}
				}
			}

			// Check if display has changed and update if so
			List<Component> display = getDisplay(player);
			if (!metadata.mDisplay.equals(display)) {

				// create new lines
				int existingSize = metadata.mLines.size();
				if (display.size() > existingSize) {
					while (display.size() > metadata.mLines.size()) {
						metadata.mLines.add(createLine(player, display.get(metadata.mLines.size()), metadata.mLines.size()));
					}
					List<PacketContainer> packets = getSpawnLinesPackets(player, metadata, existingSize);
					for (PacketContainer packet : packets) {
						broadcastPacketNoFilters(packet, player, metadata.mVisibleToPlayers);
					}
				}

				// delete removed lines
				if (display.size() < metadata.mLines.size()) {
					List<LineMetadata> subList = metadata.mLines.subList(display.size(), metadata.mLines.size());

					PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
					packet.getIntegerArrays().write(0, subList.stream().mapToInt(line -> line.mArmorStand.mId).toArray());
					broadcastPacketNoFilters(packet, player, metadata.mVisibleToPlayers);

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
							broadcastPacketNoFilters(packet, player, metadata.mVisibleToPlayers);
						}
					}
				}

				metadata.mDisplay.clear();
				metadata.mDisplay.addAll(display);

			}

			// Move titles if the player has moved
			Location location = player.getEyeLocation();
			if (location.toVector().distanceSquared(metadata.mLastLocation.toVector()) > 0.0001) { // moved at least 0.01 meters
				// Use smaller rel_move packet if the movement was small enough for the packet,
				// except every once in a while send a teleport packet to prevent small errors from accumulating
				if (Math.abs(location.getX() - metadata.mLastLocation.getX()) < 8
					    && Math.abs(location.getY() - metadata.mLastLocation.getY()) < 8
					    && Math.abs(location.getZ() - metadata.mLastLocation.getZ()) < 8
					    && player.getTicksLived() % 200 != 0) {
					for (LineMetadata line : metadata.mLines) {
						// doc: https://wiki.vg/Protocol#Entity_Position
						PacketContainer packet = new PacketContainer(PacketType.Play.Server.REL_ENTITY_MOVE);
						packet.getIntegers().write(0, line.mArmorStand.mId);
						packet.getShorts().write(0, (short) ((location.getX() - metadata.mLastLocation.getX()) * 32 * 128))
							.write(1, (short) ((location.getY() - metadata.mLastLocation.getY()) * 32 * 128))
							.write(2, (short) ((location.getZ() - metadata.mLastLocation.getZ()) * 32 * 128));
						broadcastPacketNoFilters(packet, player, metadata.mVisibleToPlayers);
					}
				} else {
					for (LineMetadata line : metadata.mLines) {
						// doc: https://wiki.vg/Protocol#Entity_Teleport
						PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
						packet.getIntegers().write(0, line.mArmorStand.mId);
						packet.getDoubles().write(0, location.getX())
							.write(1, location.getY() + line.mHeight)
							.write(2, location.getZ());
						broadcastPacketNoFilters(packet, player, metadata.mVisibleToPlayers);
					}
				}
				metadata.mLastLocation = location;
			}

			// spawn in entities for players newly in range
			for (Player tracker : trackers) {
				if (metadata.mVisibleToPlayers.add(tracker.getUniqueId())) {
					List<PacketContainer> packets = getSpawnLinesPackets(player, metadata, 0);
					for (PacketContainer packet : packets) {
						sendPacketNoFilters(tracker, packet);
					}
				}
			}
		}

		// When a player logs off, destroy the titles
		for (Iterator<Map.Entry<UUID, PlayerMetadata>> iterator = METADATA.entrySet().iterator(); iterator.hasNext(); ) {
			Map.Entry<UUID, PlayerMetadata> entry = iterator.next();
			if (Bukkit.getPlayer(entry.getKey()) == null) {
				iterator.remove();
				PlayerMetadata metadata = entry.getValue();
				PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
				packet.getIntegerArrays().write(0, metadata.mLines.stream().mapToInt(line -> line.mArmorStand.mId).toArray());
				for (UUID visibleToPlayer : metadata.mVisibleToPlayers) {
					Player player = Bukkit.getPlayer(visibleToPlayer);
					if (player != null) {
						sendPacketNoFilters(player, packet);
					}
				}
			}
		}

	}

	private List<PacketContainer> getSpawnLinesPackets(Player targetPlayer, PlayerMetadata metadata, int startLine) {
		List<PacketContainer> result = new ArrayList<>();
		List<LineMetadata> linesToSend = metadata.mLines.subList(startLine, metadata.mLines.size());
		for (LineMetadata line : linesToSend) {
			// spawn armor stand
			// doc: https://wiki.vg/Protocol#Spawn_Living_Entity
			{
				PacketContainer packet = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
				packet.getIntegers().write(0, line.mArmorStand.mId); // id
				packet.getUUIDs().write(0, line.mArmorStand.mUuid); // uuid
				packet.getIntegers().write(1, line.mArmorStand.mEntityType); // type
				packet.getDoubles().write(0, targetPlayer.getEyeLocation().getX())
					.write(1, targetPlayer.getEyeLocation().getY() + line.mHeight)
					.write(2, targetPlayer.getEyeLocation().getZ());
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
		}

		return result;
	}

	private static PlayerMetadata createLines(Player targetPlayer, List<Component> display) {
		List<LineMetadata> lines = new ArrayList<>();
		for (int i = 0; i < display.size(); i++) {
			lines.add(createLine(targetPlayer, display.get(i), i));
		}
		return new PlayerMetadata(lines, display, targetPlayer.getEyeLocation());
	}

	private static LineMetadata createLine(Player targetPlayer, Component text, int index) {
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

		double height = 0.1 + index * 0.25;

		return new LineMetadata(armorStandMetadata, height);
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

	/**
	 * Broadcasts a packet to all trackers of the given entity that are also in the given set, ignoring packet filters/listeners.
	 */
	private void broadcastPacketNoFilters(PacketContainer packet, Entity entity, Set<UUID> visibleToPlayers) {
		for (Player tracker : mProtocolManager.getEntityTrackers(entity)) {
			if (visibleToPlayers.contains(tracker.getUniqueId())) {
				try {
					mProtocolManager.sendServerPacket(tracker, packet, false);
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void sendPacketNoFilters(Player receiver, PacketContainer packet) {
		try {
			mProtocolManager.sendServerPacket(receiver, packet, false);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

}
