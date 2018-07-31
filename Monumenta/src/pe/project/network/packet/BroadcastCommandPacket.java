package pe.project.network.packet;

import org.bukkit.Bukkit;

import pe.project.Plugin;
import pe.project.utils.PacketUtils;

public class BroadcastCommandPacket implements Packet {
	public static final String StaticPacketChannel = "Monumenta.Bungee.Broadcast.BroadcastCommand";
	private String mCommand;

	public BroadcastCommandPacket(String command) {
		mCommand = command;
	}

	@Override
	public String getPacketChannel() {
		return StaticPacketChannel;
	}

	@Override
	public String getPacketData() throws Exception {
		String[] data = {mCommand};
		return PacketUtils.encodeStrings(data);
	}

	public static void handlePacket(Plugin plugin, String data) throws Exception {
		String[] rcvStrings = PacketUtils.decodeStrings(data);
		if (rcvStrings == null || rcvStrings.length != 1) {
			throw new Exception("Received string data is null or invalid length");
		}

		if (plugin.mServerProperties.getBroadcastCommandEnabled() == true
			|| rcvStrings[0].startsWith("say")
			|| rcvStrings[0].startsWith("msg")
			|| rcvStrings[0].startsWith("tell")
			|| rcvStrings[0].startsWith("tellraw")) {

			plugin.getLogger().info("Executing broadcast received command '" + rcvStrings[0] + "'");
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), rcvStrings[0]);
		}
	}
}
