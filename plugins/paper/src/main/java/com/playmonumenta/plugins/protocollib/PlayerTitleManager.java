package com.playmonumenta.plugins.protocollib;

import com.bergerkiller.bukkit.common.wrappers.ChatText;
import com.bergerkiller.bukkit.common.wrappers.DataWatcher;
import com.bergerkiller.generated.net.minecraft.network.protocol.PacketHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutEntityDestroyHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutEntityHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutEntityMetadataHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutEntityTeleportHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLivingHandle;
import com.bergerkiller.generated.net.minecraft.world.entity.EntityHandle;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
		private final DataWatcher mDataWatcher;
		private final int mId;
		private final UUID mUuid;
		private final EntityType mEntityType;

		private EntityMetadata(Entity entity) {
			mDataWatcher = new DataWatcher(WrappedDataWatcher.getEntityWatcher(entity).getHandle());
			mId = entity.getEntityId();
			mUuid = entity.getUniqueId();
			mEntityType = entity.getType();
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

	private int mTick = 0;

	public void tick() {
		mTick++;
		Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
		for (Player player : onlinePlayers) {
			PlayerMetadata metadata = METADATA.get(player.getUniqueId());

			// If the player became invalid (died, maybe more), logged out (despite being online?), or became spectator/vanished remove all titles
			if (!player.isValid() || !player.isOnline() || PremiumVanishIntegration.isInvisibleOrSpectator(player)) {
				if (metadata != null) {
					METADATA.remove(player.getUniqueId());
					destroyEntities(metadata);
				}
				continue;
			}

			// Force new entities to be created when the player switches worlds or teleports a large distance (> 100 blocks)
			if (metadata != null && (!player.getWorld().equals(metadata.mLastLocation.getWorld()) || metadata.mLastLocation.distanceSquared(player.getLocation()) > 10000)) {
				METADATA.remove(player.getUniqueId());
				destroyEntities(metadata);
				metadata = null;
			}

			// Create new entities if necessary
			if (metadata == null) {
				metadata = createLines(player, getDisplay(player));
				METADATA.put(player.getUniqueId(), metadata);
			}

			// remove entities from players no longer in range
			List<Player> trackers = getEntityTrackers(player);
			for (Iterator<UUID> iterator = metadata.mVisibleToPlayers.iterator(); iterator.hasNext(); ) {
				UUID visibleToPlayer = iterator.next();
				if (trackers.stream().noneMatch(tracker -> tracker.getUniqueId().equals(visibleToPlayer))) {
					iterator.remove();
					Player otherPlayer = Bukkit.getPlayer(visibleToPlayer);
					if (otherPlayer != null) {
						sendPacketNoFilters(otherPlayer, createDestroyPacket(metadata.mLines));
					}
				}
			}

			if (mTick % 3 == 0) {
				// Check if display has changed and update if so
				List<Component> display = getDisplay(player);
				if (!metadata.mDisplay.equals(display)) {

					// create new lines
					int existingSize = metadata.mLines.size();
					if (display.size() > existingSize) {
						while (display.size() > metadata.mLines.size()) {
							metadata.mLines.add(createLine(player, display.get(metadata.mLines.size()), metadata.mLines.size()));
						}
						List<PacketHandle> packets = getSpawnLinesPackets(player, metadata.mLines.subList(existingSize, metadata.mLines.size()));
						for (PacketHandle packet : packets) {
							broadcastPacketNoFilters(packet, metadata.mVisibleToPlayers);
						}
					}

					// delete removed lines
					if (display.size() < metadata.mLines.size()) {
						List<LineMetadata> subList = metadata.mLines.subList(display.size(), metadata.mLines.size());
						broadcastPacketNoFilters(createDestroyPacket(subList), metadata.mVisibleToPlayers);
						subList.clear();
					}

					// update changed lines
					int updatedSize = Math.min(existingSize, display.size());
					for (int i = 0; i < updatedSize; i++) {
						if (!display.get(i).equals(metadata.mDisplay.get(i))) {
							EntityMetadata armorStand = metadata.mLines.get(i).mArmorStand;
							DataWatcher.Item<ChatText> name = armorStand.mDataWatcher.getItem(EntityHandle.DATA_CUSTOM_NAME);
							if (name != null) {
								name.setValue(ChatText.fromJson(MessagingUtils.toGson(display.get(i)).toString()), true);
								DataWatcher dw = new DataWatcher();
								dw.watch(name);
								PacketPlayOutEntityMetadataHandle handle = PacketPlayOutEntityMetadataHandle.createNew(armorStand.mId, dw, true);
								broadcastPacketNoFilters(handle, metadata.mVisibleToPlayers);
							}
						}
					}

					metadata.mDisplay.clear();
					metadata.mDisplay.addAll(display);
				}
			}

			// Move titles if the player has moved
			Location location = player.getEyeLocation();
			if (location.toVector().distanceSquared(metadata.mLastLocation.toVector()) > 0.0001) { // moved at least 0.01 meters
				// Use smaller rel_move packet if the movement was small enough for the packet,
				// except every once in a while send a teleport packet to prevent small errors from accumulating
				if (Math.abs(location.getX() - metadata.mLastLocation.getX()) < 8
					    && Math.abs(location.getY() - metadata.mLastLocation.getY()) < 8
					    && Math.abs(location.getZ() - metadata.mLastLocation.getZ()) < 8
					    && ((player.getTicksLived() % 200) + 200) % 200 > 1) {
					for (LineMetadata line : metadata.mLines) {
						PacketPlayOutEntityHandle.PacketPlayOutRelEntityMoveHandle handle = PacketPlayOutEntityHandle.PacketPlayOutRelEntityMoveHandle.createNew(
							line.mArmorStand.mId,
							location.getX() - metadata.mLastLocation.getX(),
							location.getY() - metadata.mLastLocation.getY(),
							location.getZ() - metadata.mLastLocation.getZ(),
							false);
						broadcastPacketNoFilters(handle, metadata.mVisibleToPlayers);
					}
				} else {
					for (LineMetadata line : metadata.mLines) {
						PacketPlayOutEntityTeleportHandle handle = PacketPlayOutEntityTeleportHandle.createNew(
							line.mArmorStand.mId,
							location.getX(), location.getY() + line.mHeight, location.getZ(),
							0, 0, false
						);
						broadcastPacketNoFilters(handle, metadata.mVisibleToPlayers);
					}
				}
				metadata.mLastLocation = location;
			}

			// spawn in entities for players newly in range
			for (Player tracker : trackers) {
				if (metadata.mVisibleToPlayers.add(tracker.getUniqueId())) {
					List<PacketHandle> packets = getSpawnLinesPackets(player, metadata.mLines);
					for (PacketHandle packet : packets) {
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
				destroyEntities(metadata);
			}
		}

	}

	private List<PacketHandle> getSpawnLinesPackets(Player targetPlayer, List<LineMetadata> linesToSend) {
		List<PacketHandle> result = new ArrayList<>();
		for (LineMetadata line : linesToSend) {
			// spawn armor stand
			{
				PacketPlayOutSpawnEntityLivingHandle handle = PacketPlayOutSpawnEntityLivingHandle.createNew();
				handle.setEntityId(line.mArmorStand.mId);
				handle.setEntityUUID(line.mArmorStand.mUuid);
				handle.setEntityType(line.mArmorStand.mEntityType);
				handle.setPosX(targetPlayer.getEyeLocation().getX());
				handle.setPosY(targetPlayer.getEyeLocation().getY() + line.mHeight);
				handle.setPosZ(targetPlayer.getEyeLocation().getZ());
				// other fields: keep defaults (0 velocity, 0 pitch/yaw/headYaw)
				result.add(handle);
			}

			// set armor stand metadata
			{
				PacketPlayOutEntityMetadataHandle handle = PacketPlayOutEntityMetadataHandle.createNew(
					line.mArmorStand.mId,
					line.mArmorStand.mDataWatcher,
					true
				);
				result.add(handle);
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

		double height = 0.15 + index * 0.25;

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

	private void destroyEntities(PlayerMetadata metadata) {
		if (!metadata.mVisibleToPlayers.isEmpty()) {
			broadcastPacketNoFilters(createDestroyPacket(metadata.mLines), metadata.mVisibleToPlayers);
			metadata.mVisibleToPlayers.clear();
		}
	}


	/**
	 * Broadcasts a packet to all player in the given set, ignoring packet filters/listeners.
	 */
	private void broadcastPacketNoFilters(PacketHandle packet, Set<UUID> visibleToPlayers) {
		for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
			if (visibleToPlayers.contains(otherPlayer.getUniqueId())) {
				sendPacketNoFilters(otherPlayer, packet);
			}
		}
	}

	private void sendPacketNoFilters(Player receiver, PacketHandle packet) {
		try {
			mProtocolManager.sendServerPacket(receiver, PacketContainer.fromPacket(packet.getRaw()), false);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	private List<Player> getEntityTrackers(Entity entity) {
		try {
			return mProtocolManager.getEntityTrackers(entity);
		} catch (IllegalArgumentException e) {
			// ProtocolLib sometimes throws this if it cannot find trackers. Appears to happen near logout.
			return Collections.emptyList();
		}
	}

	private PacketHandle createDestroyPacket(List<LineMetadata> lines) {
		return PacketPlayOutEntityDestroyHandle.createNewMultiple(lines.stream().mapToInt(line -> line.mArmorStand.mId).toArray());
	}

}
