package pe.project.tracking;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import pe.project.Plugin;
import pe.project.utils.LocationUtils;
import pe.project.utils.LocationUtils.LocationType;

public class PlayerTracking implements EntityTracking {
	Plugin mPlugin = null;
	private HashMap<Player, Plugin> mPlayers = new HashMap<Player, Plugin>();

	PlayerTracking(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public void addEntity(Entity entity) {
		mPlayers.put((Player)entity, null);
	}

	@Override
	public void removeEntity(Entity entity) {
		Player player = (Player)entity;

		mPlayers.remove(player);
	}

	public Set<Player> getPlayers() {
		return mPlayers.keySet();
	}

	@Override
	public void update(World world, int ticks) {
		Iterator<Entry<Player, Plugin>> playerIter = mPlayers.entrySet().iterator();
		while (playerIter.hasNext()) {
			Entry<Player, Plugin> entry = playerIter.next();
			Player player = entry.getKey();

			boolean inSafeZone = false;
			GameMode mode = player.getGameMode();

			if (mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR) {
				LocationType zone = LocationUtils.getLocationType(mPlugin, player);
				inSafeZone = (zone != LocationType.None);

				if (inSafeZone && mode == GameMode.SURVIVAL) {
					player.setGameMode(GameMode.ADVENTURE);
				}

				//	Give potion effects to those in a City;
				if (!inSafeZone && mode == GameMode.ADVENTURE) {
					player.setGameMode(GameMode.SURVIVAL);
				}
			}
		}
	}

	@Override
	public void unloadTrackedEntities() {
		mPlayers.clear();
	}
}
