package pe.project.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteStreams;

import pe.project.Main;
import pe.project.network.packet.Packet;
import pe.project.network.packet.TransferPlayerPacket;
import pe.project.playerdata.PlayerData;
import pe.project.utils.NetworkUtils;

public class PluginListener implements PluginMessageListener {
	Main mMain = null;
	
	public PluginListener(Main main) {
		mMain = main;
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals("BungeeCord")) {
			return;
		}

		Packet packet = NetworkUtils.ReadPacket(ByteStreams.newDataInput(message));
		if (packet != null) {
			String packetType = packet.getPacketName();
			if (packetType.equals("TransferPlayerPacket")) {
				TransferPlayerPacket tranferPacker = (TransferPlayerPacket)packet;
				Player p = Bukkit.getPlayer(tranferPacker.mPlayerName);
				if (p != null) {
					PlayerData.loadFromString(mMain, player, tranferPacker.mPlayerContent);
				}
			}
		}
	}
}
