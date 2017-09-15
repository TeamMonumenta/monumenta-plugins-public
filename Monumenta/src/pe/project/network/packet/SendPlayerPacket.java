package pe.project.network.packet;

import java.util.UUID;
import java.nio.charset.StandardCharsets;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import pe.project.Main;

public class SendPlayerPacket implements Packet {
	public String mNewServer;
	public String mPlayerName;
	public UUID mPlayerUUID;

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
	public String getPacketData() {
		// Get an object to serialize the data
		ByteArrayDataOutput out = ByteStreams.newDataOutput();

		// Write the payload data
		out.writeUTF(mNewServer);
		out.writeUTF(mPlayerName);
		out.writeUTF(mPlayerUUID.toString());

		// Serialize the packet payload (resulting bytes depend on type of packet)
		byte[] bytes = out.toByteArray();

		// Convert that byte array to a generic string
		return new String(bytes, StandardCharsets.UTF_8);
	}

	@Override
	public void handlePacket(Main main, String data) {
		main.getLogger().severe("Got " + getPacketChannel() + " message from which should only be received by bungeecord");
	}
}
