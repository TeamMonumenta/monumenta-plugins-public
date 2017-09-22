package bungee.project.listeners;

import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import bungee.project.Main;
import net.md_5.bungee.api.plugin.Listener;

import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import fr.rhaz.socketapi.SocketAPI.Server.SocketMessenger;
import fr.rhaz.socket4mc.Bungee.BungeeSocketHandshakeEvent;
import fr.rhaz.socket4mc.Bungee.BungeeSocketJSONEvent;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class EventListener implements Listener {

	Main mMain;

	// Collection of all connected sockets
	private static HashMap<String, SocketMessenger> mSockets;

	public EventListener(Main main) {
		mMain = main;
		mSockets = new HashMap<>();
	}

	// Every time a bungee server shard connects, add its socket to the list
	@EventHandler(priority = EventPriority.LOWEST)
    public void onHandshake(BungeeSocketHandshakeEvent e) {
        mSockets.put(e.getName(), e.getMessenger());
    }

	@EventHandler(priority = EventPriority.LOWEST)
	public void onMessage(BungeeSocketJSONEvent e) {
		String channel = e.getChannel(); // The channel ("MyBukkitPlugin")
		String name = e.getName(); // The Spigot server name
		String data = e.getData(); // The data the plugin sent you

		// Nothing to do with no payload
		if (data == null || data.length() <= 0) {
			mMain.getLogger().warning("Got message from '" + name + "' with empty data");
			return;
		}

		if ((!channel.equals("Monumenta.Bungee.Forward.TransferPlayerData"))
			&& (!channel.equals("Monumenta.Bungee.SendPlayer"))
			&& (!channel.equals("Monumenta.Bungee.Heartbeat"))) {
			mMain.getLogger().warning("Got message from '" + name + "' with invalid channel '" + channel + "'");
			return;
		}

		// Decode the input "string" into the byte array that was sent
		byte[] packetInfo = data.getBytes(StandardCharsets.ISO_8859_1);

		// Decode the input into an array of individual byte arrays
		ByteArrayDataInput input = ByteStreams.newDataInput(packetInfo);

		if (input == null) {
			mMain.getLogger().warning("Got message from '" + name + "' with invalid payload data");
			return;
		}

		switch(channel) {
			case "Monumenta.Bungee.Forward.TransferPlayerData":
				bungeeForward(channel, name, input, data);
				break;
			case "Monumenta.Bungee.SendPlayer":
				sendPlayer(channel, name, input, data);
				break;
			case "Monumenta.Bungee.Heartbeat":
				mMain.getLogger().info("Got heartbeat message from '" + name + "'");
				break;
			default:
				mMain.getLogger().warning("Got message from '" + name + "' with unhandled channel '" + channel + "'");
				break;
		}
	}

	private void bungeeForward(String channel, String name, ByteArrayDataInput input, String data) {
		// First component of this type of packet is the destination server
		String destination = input.readUTF();

		if (destination == null || (!mSockets.containsKey(destination))) {
			mMain.getLogger().warning("Cannot forward message from '" + name + "' to unknown destination '" + destination + "'");
			return;
		}

		// Look up the destination socket
		SocketMessenger socketDest = mSockets.get(destination);

		if ((!socketDest.isHandshaked()) || (!socketDest.isConnectedAndOpened())) {
			mMain.getLogger().warning("Cannot forward message from '" + name + "' because '" + destination + "' has not finished connecting");
			return;
		}

		// Finally forward the message
		socketDest.writeJSON(channel, data);
		mMain.getLogger().info("Forwarded message from '" + name + "' to '" + destination + "'");
	}

	private void sendPlayer(String channel, String name, ByteArrayDataInput input, String data) {
		// Message contains just player name and destination server
		String destination = input.readUTF();
		String player = input.readUTF();
		UUID uuid = UUID.fromString(input.readUTF());

		if (destination == null || player == null || uuid == null || destination.length() <= 0 || player.length() <= 0) {
			mMain.getLogger().warning("Got transfer message from '" + name + "' with invalid arguments");
			return;
		}

		// Get and validate the destination server
		ServerInfo serverInfo = mMain.getProxy().getServers().get(destination);
		if (serverInfo == null) {
			mMain.getLogger().warning("Cannot transfer player from '" + name + "' to unknown destination '" + destination + "'");
			return;
		}

		// Get and validate the player
		ProxiedPlayer playerInfo = mMain.getProxy().getPlayer(uuid);
		if (serverInfo == null) {
			mMain.getLogger().warning("Cannot transfer unknown player '" + player + "' from '" + name + "' to '" + destination + "'");
			return;
		}

		// Finally transfer the player
		playerInfo.connect(serverInfo, new Callback<Boolean>() {
			@Override
			public void done(Boolean arg0, Throwable arg1) {
				mMain.getLogger().info("Transferred '" + player + "' to '" + destination + "'");
			}
		});
	}
}
