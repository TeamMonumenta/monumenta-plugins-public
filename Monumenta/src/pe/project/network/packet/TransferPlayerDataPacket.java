package pe.project.network.packet;

import java.util.UUID;

import pe.project.Main;
import pe.project.playerdata.PlayerData;
import pe.project.utils.PacketUtils;
import pe.project.utils.NetworkUtils;

public class TransferPlayerDataPacket implements Packet {
	private String mNewServer;
	private String mPlayerName;
	private UUID mPlayerUUID;
	private String mPlayerContent;

	public TransferPlayerDataPacket(String server, String playerName, UUID playerUUID, String playerContent) {
		mNewServer = server;
		mPlayerName = playerName;
		mPlayerUUID = playerUUID;
		mPlayerContent = playerContent;
	}

	// TODO - Ugh, this is so annoying
	// Want to just be able to call TransferPlayerDataPacket.getPacketChannel() without an object
	// But making that static just causes other problems
	public static String getStaticPacketChannel() {
		return "Monumenta.Bungee.Forward.TransferPlayerData";
	}

	@Override
	public String getPacketChannel() {
		return "Monumenta.Bungee.Forward.TransferPlayerData";
	}

	@Override
	public String getPacketData() throws Exception {
		String[] data = {mNewServer, mPlayerName, mPlayerUUID.toString(), mPlayerContent};
		return PacketUtils.encodeStrings(data);
	}

	public static void handlePacket(Main main, String data) throws Exception {
		String[] rcvStrings = PacketUtils.decodeStrings(data);
		if (rcvStrings == null || rcvStrings.length != 4) {
			throw new Exception("Received string data is null or invalid length");
		}

		String server = rcvStrings[0];
		String playerName = rcvStrings[1];
		UUID playerUUID = UUID.fromString(rcvStrings[2]);
		String playerContent = rcvStrings[3];

		// Save the player data so that when the player logs in they'll get it applied to them
		PlayerData.savePlayerData(main, playerUUID, playerContent);

		// Everything looks good - request bungeecord transfer the player to this server
		NetworkUtils.sendPlayer(main, playerName, playerUUID, server);
	}
}
