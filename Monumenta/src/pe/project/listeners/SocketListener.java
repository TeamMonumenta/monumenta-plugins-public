package pe.project.listeners;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import fr.rhaz.socket4mc.Bukkit.BukkitSocketHandshakeEvent;
import fr.rhaz.socket4mc.Bukkit.BukkitSocketJSONEvent;

import pe.project.Main;
import pe.project.utils.PacketUtils;
import pe.project.network.packet.HeartbeatPacket;

public class SocketListener implements Listener {
	Main mMain = null;

	public SocketListener(Main main) {
		mMain = main;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onConnect(BukkitSocketHandshakeEvent e){
		mMain.mSocketClient = e.getClient();

		try {
			// Send a simple hello message to bungee
			PacketUtils.SendPacket(mMain, new HeartbeatPacket());
		} catch (Exception ex) {
			mMain.getLogger().severe("Caught exception sending HeartbeatPacket: " + ex);
			ex.printStackTrace();
			return;
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onMessage(BukkitSocketJSONEvent e){
		String channel = e.getChannel();
		String data = e.getData();

		try {
			PacketUtils.ProcessPacket(mMain, channel, data);
		} catch (Exception ex) {
			mMain.getLogger().severe("Caught exception handling packet on channel '" + channel + "'");
			ex.printStackTrace();
			return;
		}
	}
}
