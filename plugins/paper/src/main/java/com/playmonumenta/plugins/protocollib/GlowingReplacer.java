package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.commands.GlowingCommand;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Collection;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

/**
 * Packet listener for selectively disabling the glowing effect
 */
public class GlowingReplacer extends PacketAdapter {

	private static final byte GLOWING_BIT = 0b01000000;

	public GlowingReplacer(Plugin plugin) {
		super(plugin, ListenerPriority.NORMAL,
				PacketType.Play.Server.ENTITY_METADATA,
				PacketType.Play.Server.SPAWN_ENTITY,
				PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB,
				PacketType.Play.Server.SPAWN_ENTITY_LIVING,
				PacketType.Play.Server.NAMED_ENTITY_SPAWN);
	}

	@Override
	public void onPacketSending(PacketEvent event) {

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

		Player player = event.getPlayer();
		int playerSettings = ScoreboardUtils.getScoreboardValue(player, GlowingCommand.SCOREBOARD_OBJECTIVE).orElse(0);

		// check if glowing is disabled for the entity's type.
		if (playerSettings != 0xFFFFFFFF) { // If all glowing is disabled, this check can be skipped.
			Entity entity = packet.getEntityModifier(event).read(0); // NB: this is the first int, not (just) the first entity in the packet.
			if (entity == null || (GlowingCommand.isGlowingEnabled(player, playerSettings, entity)
				                      // Purified Ash can only be picked up by clerics, so it doesn't make sense to highlight them for other players
				                      && !(entity instanceof Item item && DivineJustice.isAsh(item) && !DivineJustice.canPickUpAsh(player)))) {
				return;
			}
		}

		// Finally, unset the glowing bits
		// We need to clone the packet to not affect other players, and get the data watchers again from the new packet
		packet = packet.deepClone();
		wrappedWatchableObjects = packet.getWatchableCollectionModifier().getValues().stream()
			.flatMap(Collection::stream)
			.filter(wwo -> wwo.getIndex() == 0 && wwo.getRawValue() instanceof Byte b && (b & GLOWING_BIT) != 0)
			.toList();
		dataWatcherModifiers = packet.getDataWatcherModifier().getValues().stream()
			.filter(dw -> dw.hasIndex(0) && dw.getObject(0) instanceof Byte b && (b & GLOWING_BIT) != 0)
			.toList();
		event.setPacket(packet);
		for (WrappedWatchableObject wrappedWatchableObject : wrappedWatchableObjects) {
			wrappedWatchableObject.setValue((byte) (((Byte) wrappedWatchableObject.getValue()) & ~GLOWING_BIT));
		}
		for (WrappedDataWatcher dataWatcherModifier : dataWatcherModifiers) {
			dataWatcherModifier.setObject(0, (byte) (dataWatcherModifier.getByte(0) & ~GLOWING_BIT));
		}
	}

}
