package pe.project.network.packet;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

public class TransferPlayerPacket implements Packet {
	public String mNewServer;
	public String mPlayerName;
	public String mPlayerContent;

	@Override
	public String getPacketName() {
		return "TransferPlayerPacket";
	}
	
	@Override
	public ByteArrayDataOutput writePacketData(ByteArrayDataOutput out) {
		out.writeUTF(mNewServer);
		
		out.writeUTF(mPlayerName);
		
		out.writeUTF(mPlayerContent);
		
		return out;
	}

	@Override
	public void readPacketData(ByteArrayDataInput input) {
		mNewServer = input.readUTF();
		
		mPlayerName = input.readUTF();
		
		mPlayerContent = input.readUTF();
	}
}
