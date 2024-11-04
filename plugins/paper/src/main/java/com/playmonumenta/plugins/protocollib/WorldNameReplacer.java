package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.temporary.TemporaryPlayer;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.WorldNameCommand;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public class WorldNameReplacer extends PacketAdapter {

	private static final NamespacedKey WORLD_NAME_KEY = NamespacedKeyUtils.fromString("monumenta:world-name");

	public WorldNameReplacer(Plugin plugin) {
		super(plugin, PacketType.Play.Server.LOGIN, PacketType.Play.Server.RESPAWN);
	}

	@Override
	// we can't use Minecraft types here, so we have to deal with unchecked casts and raw types
	public void onPacketSending(PacketEvent event) {

		if (event.getPlayer() instanceof TemporaryPlayer
			    || !ScoreboardUtils.checkTag(event.getPlayer(), WorldNameCommand.TAG)
			    || !event.getPlayer().hasPermission(WorldNameCommand.PERMISSION)) {
			return;
		}

		final var newPacket = NmsUtils.getVersionAdapter().replaceWorldNames(event.getPacket().getHandle(), token -> {
			final var world = token.getWorld();
			String worldName = world == null ? null : world.getPersistentDataContainer().get(WORLD_NAME_KEY, PersistentDataType.STRING);
			if (worldName == null) {
				worldName = ServerProperties.getShardName();
			}
			if (worldName.endsWith("plots") && worldName.length() > 5) { // ends with 'plots', but is not 'plots' itself
				return;
			}

			token.setKey(NamespacedKey.fromString(worldName, Plugin.getInstance()));
		});

		event.setPacket(PacketContainer.fromPacket(newPacket));
	}
}
