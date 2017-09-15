package bungee.project.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import bungee.project.Main;
import net.md_5.bungee.api.plugin.Listener;

import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import fr.rhaz.socket4mc.Bungee.BungeeSocketHandshakeEvent;
import fr.rhaz.socket4mc.Bungee.BungeeSocketConnectEvent;
import fr.rhaz.socket4mc.Bungee.BungeeSocketJSONEvent;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;

public class EventListener implements Listener {
	Main mMain;

	public EventListener(Main main) {
		mMain = main;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onMessage(BungeeSocketJSONEvent e){
		String channel = e.getChannel(); // The channel ("MyBukkitPlugin")
		String name = e.getName(); // The Spigot server name
		String data = e.getData(); // The data the plugin sent you
		if(data.equals("Hello from Bukkit :)")){
			e.write("Hello, " + name + ", how're you?");
		}
	}

	//	Plugin Message Event
	private void PluginMessageEvent(PluginMessageEvent event) {
		if (event.getTag().equals("BungeeCord")) {
			if (event.getData() != null && event.getData().length > 0) {
				//	Because we don't want to manipulate the actual data we'll make a clone of it and work with that.
				byte[] packetInfo = event.getData().clone();
				ByteArrayDataInput input = ByteStreams.newDataInput(packetInfo);
				if (input != null) {
					//	The first part of the packet is what type it is.
					String packetType = input.readUTF();
					if (packetType != null) {
						//	One we know the type of packet we can manipulate it how we need to.
						if (packetType.equals("TransferPlayerPacket")) {
							//	Get the intended server target.
							String newServer = input.readUTF();

							//	Now grab that servers info and transfer us there.
							ServerInfo serverInfo = mMain.getProxy().getServers().get(newServer);
							if (serverInfo != null) {
								//	Grab the player.
								String playerName = input.readUTF();
								ProxiedPlayer player = mMain.getProxy().getPlayer(playerName);
								if (player != null) {
									//	First we wan't to transfer the player.
									player.connect(serverInfo, new Callback<Boolean>() {
										@Override
										public void done(Boolean arg0, Throwable arg1) {
											//mMain.getProxy().broadcast(new TextComponent("Player '" + playerName + "' transferred to '" + newServer + "'"));

											//	Once that's done send over their data.
											serverInfo.sendData("BungeeCord", event.getData());
										}
									});
								}
							}
						}
					}
				}
			}
		}
	}

	public void playerTransfered(boolean transfered) {

	}
}
