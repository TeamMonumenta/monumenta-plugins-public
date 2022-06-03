package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.server.TemporaryPlayer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.Converters;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.WorldNameCommand;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Set;
import java.util.stream.Collectors;

public class WorldNameReplacer extends PacketAdapter {

	public WorldNameReplacer(Plugin plugin) {
		super(plugin, PacketType.Play.Server.LOGIN, PacketType.Play.Server.RESPAWN);
	}

	@Override
	// we can't use Minecraft types here, so we have to deal with unchecked casts and raw types
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void onPacketSending(PacketEvent event) {

		if (event.getPlayer() instanceof TemporaryPlayer
			    || !ScoreboardUtils.checkTag(event.getPlayer(), WorldNameCommand.TAG)
			    || !event.getPlayer().hasPermission(WorldNameCommand.PERMISSION)) {
			return;
		}

		// docs:
		// login: https://wiki.vg/Protocol#Join_Game
		// respawn: https://wiki.vg/Protocol#Respawn

		PacketContainer packet = event.getPacket();

		Class resourceKeyClass = NmsUtils.getVersionAdapter().getResourceKeyClass();

		Object shardWorldNameKey = NmsUtils.getVersionAdapter().createDimensionTypeResourceKey("monumenta", ServerProperties.getShardName());

		// first identifier (in both packets): world name
		StructureModifier worldName = packet.getSpecificModifier(resourceKeyClass);
		Object currentWorldKey = worldName.read(0);
		worldName.write(0, shardWorldNameKey);

		// login packets also have a set of possible world names - replace the changed name in that set
		if (event.getPacketType().equals(PacketType.Play.Server.LOGIN)) {
			StructureModifier<Set<?>> worldNames = packet.getSets(Converters.passthrough(resourceKeyClass));
			worldNames.write(0, worldNames.read(0).stream().map(key -> key.equals(currentWorldKey) ? shardWorldNameKey : key).collect(Collectors.toSet()));
		}

	}

}
