package pe.project.network.packet;

import pe.project.Main;

public interface Packet {
	String getPacketChannel();
	String getPacketData();
	void handlePacket(Main main, String data);
}
