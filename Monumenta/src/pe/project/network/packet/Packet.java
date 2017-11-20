package pe.project.network.packet;

public interface Packet {
	String getPacketChannel();
	String getPacketData() throws Exception;

	// Implementers should also have the following static methods:
	// String getPacketChannel();
	// void handlePacket(Main plugin, String data) throws Exception;
}
