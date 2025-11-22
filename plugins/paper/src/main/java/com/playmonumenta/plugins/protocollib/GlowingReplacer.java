package com.playmonumenta.plugins.protocollib;

import com.bergerkiller.bukkit.common.wrappers.ChatText;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutEntityMetadataHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeamHandle;
import com.bergerkiller.generated.net.minecraft.world.entity.EntityHandle;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.GlowingCommand;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

/**
 * Packet listener for selectively disabling the glowing effect
 */
public class GlowingReplacer extends PacketAdapter implements Listener {

	private static final byte GLOWING_BIT = 0b01000000;

	private static final Map<UUID, Set<String>> SENT_TEAMS = new HashMap<>();

	public GlowingReplacer(Plugin plugin) {
		super(plugin, ListenerPriority.NORMAL,
			PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.SCOREBOARD_TEAM);
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@Override
	public void onPacketSending(PacketEvent event) {

		PacketContainer packet = event.getPacket();
		Player player = event.getPlayer();

		if (packet.getType().equals(PacketType.Play.Server.ENTITY_METADATA)) {

			// Entity flags is a byte and is at index 0, see https://wiki.vg/Entity_metadata#Entity
			PacketPlayOutEntityMetadataHandle handle = PacketPlayOutEntityMetadataHandle.createHandle(packet.getHandle());
			if (handle.getMetadataItems().isEmpty()
				|| handle.getMetadataItems().get(0) == null
				|| !handle.getMetadataItems().get(0).isForKey(EntityHandle.DATA_FLAGS)
				|| !(handle.getMetadataItems().get(0).value() instanceof Byte data)
				|| (data & GLOWING_BIT) == 0) {
				// No glowing bit is set, so there's nothing to do
				return;
			}

			int playerSettings = ScoreboardUtils.getScoreboardValue(player, GlowingCommand.SCOREBOARD_OBJECTIVE).orElse(0);

			// Check if glowing is disabled for the entity's type.
			if (playerSettings != 0xFFFFFFFF) { // If all glowing is disabled, this check can be skipped.
				Entity entity = packet.getEntityModifier(event).read(0); // NB: this is the first int, not (just) the first entity in the packet.
				if (entity == null || (GlowingCommand.isGlowingEnabled(player, playerSettings, entity) && GlowingManager.isGlowingForPlayer(entity, player))) {
					// If glowing is enabled then there's nothing to do
					return;
				}
			}

			// Finally, unset the glowing bits
			// We need to clone the packet to not affect other players (as the packet is shared between players)
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
			handle = PacketPlayOutEntityMetadataHandle.createHandle(packet.getHandle());
			handle.getMetadataItems().set(0, handle.getMetadataItems().get(0).cloneWithValue((byte) (data & ~GLOWING_BIT)));
			event.setPacket(packet);

		} else { // SCOREBOARD_TEAM

			// On vanilla team updates, remove any entities with virtual glowing colors from the packet
			PacketPlayOutScoreboardTeamHandle handle = PacketPlayOutScoreboardTeamHandle.createHandle(packet.getHandle());
			List<String> toRemove = new ArrayList<>();
			for (String entry : handle.getPlayers()) {
				if (GlowingManager.getTeamForPlayer(entry, player) != null) {
					toRemove.add(entry);
				}
			}

			if (!toRemove.isEmpty()) {
				ArrayList<String> entries = new ArrayList<>(handle.getPlayers());
				entries.removeAll(toRemove);
				handle.setPlayers(entries);
				if (entries.isEmpty()) {
					event.setCancelled(true);
				}
			}

		}
	}

	public static void sendTeamUpdate(Entity entity, Player player, @Nullable String oldTeam, @Nullable NamedTextColor newTeam) {
		Team realTeam = Bukkit.getScoreboardManager().getMainScoreboard().getEntityTeam(entity);

		if (oldTeam != null || realTeam != null) {
			// remove from old team first
			PacketPlayOutScoreboardTeamHandle handle = PacketPlayOutScoreboardTeamHandle.createNew();
			handle.setName(oldTeam != null ? oldTeam : realTeam.getName());
			handle.setMethod(PacketPlayOutScoreboardTeamHandle.METHOD_LEAVE);
			handle.setPlayers(List.of(ScoreboardUtils.getScoreHolderName(entity)));
			ProtocolLibrary.getProtocolManager().sendServerPacket(player, PacketContainer.fromPacket(handle.getRaw()), false);
		}

		if (newTeam != null || realTeam != null) {
			String newTeamName = newTeam != null ? getColoredGlowingTeamName(newTeam, entity) : realTeam.getName();
			if (newTeam != null && SENT_TEAMS.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(newTeamName)) {
				// new team not yet sent to player, so send the creation packet
				PacketPlayOutScoreboardTeamHandle handle = PacketPlayOutScoreboardTeamHandle.createNew();
				handle.setName(newTeamName);
				handle.setMethod(PacketPlayOutScoreboardTeamHandle.METHOD_ADD);
				handle.setVisibility(entity instanceof Player ? "never" : "always");
				handle.setCollisionRule(isUnpushable(entity) ? "never" : "always");
				handle.setColor(namedTextColorToChatColor(newTeam));
				handle.setDisplayName(ChatText.fromMessage(newTeam.toString()));
				handle.setPrefix(ChatText.empty());
				handle.setSuffix(ChatText.empty());
				handle.setPlayers(List.of(ScoreboardUtils.getScoreHolderName(entity)));

				ProtocolLibrary.getProtocolManager().sendServerPacket(player, PacketContainer.fromPacket(handle.getRaw()), false);
			} else {
				// going back to real team or new team already sent to client, only send an update packet
				PacketPlayOutScoreboardTeamHandle handle = PacketPlayOutScoreboardTeamHandle.createNew();
				handle.setName(newTeamName);
				handle.setMethod(PacketPlayOutScoreboardTeamHandle.METHOD_JOIN);
				handle.setPlayers(List.of(ScoreboardUtils.getScoreHolderName(entity)));
				ProtocolLibrary.getProtocolManager().sendServerPacket(player, PacketContainer.fromPacket(handle.getRaw()), false);
			}
		}
	}

	public static void resendEntityMetadataFlags(Entity entity, Player player) {
		final PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		final WrappedDataWatcher dataWatcher = WrappedDataWatcher.getEntityWatcher(entity);
		final List<WrappedWatchableObject> dataWatcherObjects = dataWatcher.getWatchableObjects();
		if (dataWatcherObjects.isEmpty()
			|| dataWatcherObjects.get(0).getIndex() != 0
			|| !(dataWatcherObjects.get(0).getValue() instanceof Byte)) {
			return;
		}
		packet.getIntegers().write(0, entity.getEntityId());
		WrappedDataWatcher.Serializer byteSerializer = WrappedDataWatcher.Registry.get(Byte.class);
		packet.getDataValueCollectionModifier().write(0, List.of(new WrappedDataValue(0, byteSerializer, dataWatcherObjects.get(0).getValue())));
		ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet, true);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent e) {
		SENT_TEAMS.remove(e.getPlayer().getUniqueId());
	}

	private static boolean isUnpushable(Entity entity) {
		Team team = ScoreboardUtils.getEntityTeam(entity);
		return team != null && team.getOption(Team.Option.COLLISION_RULE) == Team.OptionStatus.NEVER;
	}

	public static String getColoredGlowingTeamName(NamedTextColor color, Entity entity) {
		return getColoredGlowingTeamName(color, entity instanceof Player, isUnpushable(entity));
	}

	public static String getColoredGlowingTeamName(NamedTextColor color, boolean forPlayers, boolean unpushable) {
		return "_glowing_color_" + color + (forPlayers ? "_players" : "") + (unpushable ? "_unpushable" : "");
	}

	// BKCommonLib insists on using ChatColor
	@SuppressWarnings("deprecation")
	private static ChatColor namedTextColorToChatColor(NamedTextColor color) {
		return ChatColor.valueOf(color.toString().toUpperCase(Locale.ROOT));
	}

}
