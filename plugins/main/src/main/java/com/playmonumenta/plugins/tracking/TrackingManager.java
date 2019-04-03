package com.playmonumenta.plugins.tracking;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Villager;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;

public class TrackingManager {
	private static final String UNPUSHABLE_TEAM = "UNPUSHABLE_TEAM";
	private static final String UNPUSHABLE_TAG = "UNPUSHABLE";
	private static final String PUSHABLE_TAG = "PUSHABLE";

	private Plugin mPlugin;
	private Team mUnpushableTeam;

	public PlayerTracking mPlayers;
	public CreeperTracking mCreepers;
	public BoatTracking mBoats;
	public MinecartTracking mMinecarts;
	public SilverfishTracking mSilverfish;
	public FishingHookTracking mFishingHook;

	public TrackingManager(Plugin plugin, World world) {
		mPlugin = plugin;

		// Create a new team (or clear it if it exists) on the scoreboard to use to
		// make entities unpushable
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		mUnpushableTeam = scoreboard.getTeam(UNPUSHABLE_TEAM);
		if (mUnpushableTeam != null) {
			mUnpushableTeam.unregister();
		}
		mUnpushableTeam = scoreboard.registerNewTeam(UNPUSHABLE_TEAM);
		mUnpushableTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

		mPlayers = new PlayerTracking(mPlugin);
		mCreepers = new CreeperTracking();
		mBoats = new BoatTracking(mPlugin);
		mMinecarts = new MinecartTracking(mPlugin);
		mSilverfish = new SilverfishTracking(mPlugin);
		mFishingHook = new FishingHookTracking();

		List<Entity> entities = world.getEntities();
		for (Entity entity : entities) {
			addEntity(entity);
		}
	}

	public void unloadTrackedEntities() {
		mPlayers.unloadTrackedEntities();
		mCreepers.unloadTrackedEntities();
		mBoats.unloadTrackedEntities();
		mMinecarts.unloadTrackedEntities();
		mSilverfish.unloadTrackedEntities();
		mFishingHook.unloadTrackedEntities();
	}

	public void addEntity(Entity entity) {
		if (Constants.TRACKING_MANAGER_ENABLED) {
			// Check whether this entity should be pushable
			if (!(entity instanceof Monster)) {
				if ((entity instanceof Villager && (!entity.getScoreboardTags().contains(PUSHABLE_TAG))) ||
				    entity.getScoreboardTags().contains(UNPUSHABLE_TAG)) {

					// This entity should not be pushable - join to the unpushable team
					mUnpushableTeam.addEntry(entity.getUniqueId().toString());
				}
			}

			if (entity instanceof Player) {
				mPlayers.addEntity(entity);
			} else if (entity instanceof Creeper) {
				mCreepers.addEntity(entity);
			} else if (entity instanceof Boat) {
				mBoats.addEntity(entity);
			} else if (entity instanceof Minecart) {
				mMinecarts.addEntity(entity);
			} else if (entity instanceof Silverfish) {
				mSilverfish.addEntity(entity);
			}
		}
	}

	public void removeEntity(Entity entity) {
		if (entity instanceof Player) {
			mPlayers.removeEntity(entity);
		} else if (entity instanceof Creeper) {
			mCreepers.removeEntity(entity);
		} else if (entity instanceof Boat) {
			mBoats.removeEntity(entity);
		} else if (entity instanceof Minecart) {
			mMinecarts.removeEntity(entity);
		} else if (entity instanceof Silverfish) {
			mSilverfish.removeEntity(entity);
		}
	}

	public void update(World world, int ticks) {
		try {
			mPlayers.update(world, ticks);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			mCreepers.update(world, ticks);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			mBoats.update(world, ticks);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			mMinecarts.update(world, ticks);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			mSilverfish.update(world, ticks);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
