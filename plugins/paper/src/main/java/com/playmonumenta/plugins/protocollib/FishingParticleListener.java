package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.playmonumenta.plugins.Plugin;
import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class FishingParticleListener extends PacketAdapter {
	private static final HashMap<Player, Location> mPlayerFishhookLocMap = new HashMap<>();

	public FishingParticleListener(Plugin plugin) {
		super(plugin, PacketType.Play.Server.WORLD_PARTICLES);
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		if (!mPlayerFishhookLocMap.containsKey(event.getPlayer())) {
			return;
		}

		PacketContainer packet = event.getPacket();

		// Executes if the packet particle is of type WATER_WAKE and is on the surface of the water of the player's fishhook.
		if (packet.getNewParticles().read(0).getParticle() == Particle.WATER_WAKE &&
			Math.ceil(mPlayerFishhookLocMap.get(event.getPlayer()).getY()) - packet.getDoubles().read(1) == 0) {

			event.setCancelled(true);
		}
	}

	public static void suppressFishingParticles(Player player, Location loc) {
		if (!mPlayerFishhookLocMap.containsKey(player)) {
			mPlayerFishhookLocMap.put(player, loc);
		}
	}

	public static void allowFishingParticles(Player player) {
		mPlayerFishhookLocMap.remove(player);
	}
}
