package pe.project.network.packet;

import java.util.UUID;

import pe.project.Main;
import pe.project.utils.PacketUtils;

public class SendPlayerPacket implements Packet {
	private String mNewServer;
	private String mPlayerName;
	private UUID mPlayerUUID;

	public SendPlayerPacket(String server, String playerName, UUID playerUUID) {
		mNewServer = server;
		mPlayerName = playerName;
		mPlayerUUID = playerUUID;
	}

	// TODO - Ugh, this is so annoying
	// Want to just be able to call SendPlayerPacket.getPacketChannel() without an object
	// But making that static just causes other problems
	public static String getStaticPacketChannel() {
		return "Monumenta.Bungee.SendPlayer";
	}

	@Override
	public String getPacketChannel() {
		return "Monumenta.Bungee.SendPlayer";
	}

	@Override
	public String getPacketData() throws Exception {
		String[] data = {mNewServer, mPlayerName, mPlayerUUID.toString()};
		return PacketUtils.encodeStrings(data);
	}

	public static void handlePacket(Main main, String data) throws Exception {
		throw new Exception("Got message from which should only be received by bungeecord");
	}
}
