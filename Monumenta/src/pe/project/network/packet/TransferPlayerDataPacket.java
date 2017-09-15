package pe.project.network.packet;

import java.util.UUID;
import java.nio.charset.StandardCharsets;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import pe.project.Main;
import pe.project.playerdata.PlayerData;
import pe.project.utils.NetworkUtils;

public class TransferPlayerDataPacket implements Packet {
	public String mNewServer;
	public String mPlayerName;
	public UUID mPlayerUUID;
	public String mPlayerContent;

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
	public String getPacketData() {
		// Get an object to serialize the data
		ByteArrayDataOutput out = ByteStreams.newDataOutput();

		// Write the payload data
		out.writeUTF(mNewServer);
		out.writeUTF(mPlayerName);
		out.writeUTF(mPlayerUUID.toString());
		out.writeUTF(mPlayerContent);

		// Serialize the packet payload (resulting bytes depend on type of packet)
		byte[] bytes = out.toByteArray();

		// Convert that byte array to a generic string
		return new String(bytes, StandardCharsets.UTF_8);
	}

	@Override
	public void handlePacket(Main main, String data) {
		// Convert the data to a byte array
		ByteArrayDataInput input = ByteStreams.newDataInput(data.getBytes(StandardCharsets.UTF_8));

		// Read the payload data
		mNewServer = input.readUTF();
		if (mNewServer.isEmpty()) {
			main.getLogger().warning("Failed to retrieve new server name from packet");
			return;
		}

		mPlayerName = input.readUTF();
		if (mNewServer.isEmpty()) {
			main.getLogger().warning("Failed to retrieve player name from packet");
			return;
		}

		try {
			mPlayerUUID = UUID.fromString(input.readUTF());
		} catch (Exception e) {
			main.getLogger().warning("Failed to retrieve UUID from packet for '" + mPlayerName + "'");
			return;
		}

		mPlayerContent = input.readUTF();
		if (mPlayerContent.isEmpty()) {
			main.getLogger().warning("Failed to retrieve player data from packet for '" + mPlayerName + "'");
			return;
		}

		// Save the player data so that when the player logs in they'll get it applied to them
		if (PlayerData.savePlayerData(main, mPlayerUUID, mPlayerContent).isEmpty()) {
			main.getLogger().warning("Failed to save player data for '" + mPlayerName + "'");
			return;
		}

		// Everything looks good - request bungeecord transfer the player to the new server
		SendPlayerPacket packet = new SendPlayerPacket();

		packet.mNewServer = mNewServer;
		packet.mPlayerName = mPlayerName;
		packet.mPlayerUUID = mPlayerUUID;

		main.getLogger().info("Transferring " + mPlayerName + " to " + mNewServer);
		NetworkUtils.SendPacket(main, packet);
	}
}
