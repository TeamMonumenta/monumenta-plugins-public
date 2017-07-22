package pe.project.network.packet;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

public interface Packet {
	String getPacketName();
	ByteArrayDataOutput writePacketData(ByteArrayDataOutput out);
	void readPacketData(ByteArrayDataInput input);
}
