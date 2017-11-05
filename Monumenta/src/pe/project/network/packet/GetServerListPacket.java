package pe.project.network.packet;

import java.util.UUID;

import pe.project.Main;
import pe.project.utils.PacketUtils;

public class GetServerListPacket implements Packet {
	private String mPlayerName;
	private UUID mPlayerUUID;

	public GetServerListPacket(String playerName, UUID playerUUID) {
		mPlayerName = playerName;
		mPlayerUUID = playerUUID;
	}

	// TODO - Ugh, this is so annoying
	// Want to just be able to call GetServerListPacket.getPacketChannel() without an object
	// But making that static just causes other problems
	public static String getStaticPacketChannel() {
		return "Monumenta.Bungee.GetServerList";
	}

	@Override
	public String getPacketChannel() {
		return "Monumenta.Bungee.GetServerList";
	}

	@Override
	public String getPacketData() throws Exception {
		String[] data = {mPlayerName, mPlayerUUID.toString()};
		return PacketUtils.encodeStrings(data);
	}

	public static void handlePacket(Main main, String data) throws Exception {
		throw new Exception("Got message from which should only be received by bungeecord");
	}
}
