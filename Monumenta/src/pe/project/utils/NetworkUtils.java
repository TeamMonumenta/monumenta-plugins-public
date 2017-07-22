package pe.project.utils;

import org.bukkit.Bukkit;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import pe.project.Main;
import pe.project.network.packet.Packet;
import pe.project.network.packet.TransferPlayerPacket;

public class NetworkUtils {
	static public void SendPacket(Main main, Packet packet) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF(packet.getPacketName());
		
		byte[] bytes = packet.writePacketData(out).toByteArray();
		Bukkit.getServer().sendPluginMessage(main, "BungeeCord", bytes);
	}
	
	static public Packet ReadPacket(ByteArrayDataInput input) {
		String packetType = input.readUTF();
		
		if (packetType.equals("TransferPlayerPacket")) {
			TransferPlayerPacket packet = new TransferPlayerPacket();
			packet.readPacketData(input);
			
			return packet;
		}
		
		return null;
	}
}
