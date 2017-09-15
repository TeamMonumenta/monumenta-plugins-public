package pe.project.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import fr.rhaz.socket4mc.Bukkit.BukkitSocketHandshakeEvent;
import fr.rhaz.socket4mc.Bukkit.BukkitSocketConnectEvent;
import fr.rhaz.socket4mc.Bukkit.BukkitSocketJSONEvent;

// TODO: Which of these imports are actually needed
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteStreams;

import pe.project.Main;
import pe.project.network.packet.Packet;
import pe.project.network.packet.TransferPlayerPacket;
import pe.project.playerdata.PlayerData;
import pe.project.utils.NetworkUtils;

public class SocketListener implements Listener{
	Main mMain = null;

	public SocketListener(Main main) {
		mMain = main;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onConnect(BukkitSocketHandshakeEvent e){
		// You can now send some data
		e.getClient().writeJSON("region_1", "Hello from Bukkit :)");
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onMessage(BukkitSocketJSONEvent e){
		String channel = e.getChannel();
		String data = e.getData();
		if(channel.equals("region_1") && data.startsWith("Hello")){
			e.write("I'm fine!");
		}
	}

	private void onPluginMessageReceived(String channel, Player player, byte[] message) {
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
