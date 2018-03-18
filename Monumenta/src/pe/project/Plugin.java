package pe.project;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import fr.rhaz.socketapi.SocketAPI.Client.SocketClient;
import pe.project.commands.Back;
import pe.project.commands.BroadcastCommand;
import pe.project.commands.Forward;
import pe.project.commands.TransferServer;
import pe.project.listeners.EntityListener;
import pe.project.listeners.PlayerListener;
import pe.project.listeners.SocketListener;
import pe.project.listeners.VehicleListener;
import pe.project.server.properties.ServerProperties;
import pe.project.tracking.TrackingManager;

public class Plugin extends JavaPlugin {
	public ServerProperties mServerProperties = new ServerProperties();

	public TrackingManager mTrackingManager;

	public SocketClient mSocketClient;

	public World mWorld;

	//	Logic that is performed upon enabling the plugin.
	@Override
	public void onEnable() {
		PluginManager manager = getServer().getPluginManager();

		mWorld = Bukkit.getWorlds().get(0);

		//	TODO: Move this out of here and into it's own EventManager class.
		manager.registerEvents(new SocketListener(this), this);
		manager.registerEvents(new PlayerListener(this), this);
		manager.registerEvents(new EntityListener(this), this);
		manager.registerEvents(new VehicleListener(), this);

		//	TODO: Move this out of here and into it's own CommandManager class.
		//	Add some slash commands
		if (Constants.COMMANDS_SERVER_ENABLED) {
			getCommand("transferServer").setExecutor(new TransferServer(this));
			getCommand("broadcastCommand").setExecutor(new BroadcastCommand(this));
			getCommand("back").setExecutor(new Back(this));
			getCommand("forward").setExecutor(new Forward(this));
		}

		mTrackingManager = new TrackingManager(this, mWorld);

		mServerProperties.load(this);

		//	Move the logic out of Plugin and into it's own class that derives off Runnable, a Timer class of some type.
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			int ticks = 0;

			@Override
			public void run() {
				final boolean fourHertz = (ticks % 5) == 0;

				//	4 times a second.
				if (fourHertz) {
					mTrackingManager.update(mWorld, Constants.QUARTER_TICKS_PER_SECOND);
				}
			}
		}, 0L, 1L);
	}

	//	Logic that is performed upon disabling the plugin.
	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);

		mTrackingManager.unloadTrackedEntities();
	}

	public Player getPlayer(UUID playerID) {
		return getServer().getPlayer(playerID);
	}
}
