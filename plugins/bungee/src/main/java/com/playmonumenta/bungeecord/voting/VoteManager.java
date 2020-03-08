package com.playmonumenta.bungeecord.voting;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import com.playmonumenta.bungeecord.listeners.NameListener;
import com.vexsoftware.votifier.bungee.events.VotifierEvent;
import com.vexsoftware.votifier.model.Vote;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class VoteManager implements Listener {
	private static final int TICK_PERIOD_SECONDS = 60;

	private static VoteManager MANAGER = null;

	/* This lock protects all member variables in this class, but does not protect VoteContext */
	private final ReadWriteLock mLock = new ReentrantReadWriteLock();
	private final Plugin mPlugin;
	private final Logger mLogger;
	private final HashMap<UUID, VoteContext> mContexts = new HashMap<UUID, VoteContext>();
	private final ScheduledTask mTickTask;
	private final Map<String, String> mAlternateNames = new HashMap<String, String>();

	/*
	 * The time in minutes between allowed votes on these sites.
	 * Not padded - exact times
	 * Padding will be handled in the task that notifies players they became eligible to vote
	 */
	private static final Map<String, Long> SITE_TIMERS = new HashMap<String, Long>();

	public VoteManager(Plugin plugin, Configuration config) throws IllegalArgumentException {
		MANAGER = this;
		mPlugin = plugin;
		mLogger = plugin.getLogger();

		if (!config.contains("sites")) {
			throw new IllegalArgumentException("Voting config missing 'sites' string list");
		} else if (!config.contains("cooldown_minutes")) {
			throw new IllegalArgumentException("Voting config missing 'cooldown_minutes' int list");
		}

		List<String> urls = config.getStringList("sites");
		List<String> alternate_names = config.getStringList("alternate_names");
		List<Integer> times = config.getIntList("cooldown_minutes");

		if (urls.size() < 1 || times.size() < 1 || urls.size() != times.size()) {
			throw new IllegalArgumentException("Voting config sites / cooldown_minutes length mismatch (or they are empty)");
		}

		for (int i = 0; i < urls.size(); i++) {
			SITE_TIMERS.put(urls.get(i), Long.valueOf(times.get(i)));
			if (!alternate_names.get(i).isEmpty()) {
				mAlternateNames.put(alternate_names.get(i), urls.get(i));
			}
		}

		// For testing!
		//SITE_TIMERS.put("https://mctools.org/votifier-tester", Long.valueOf(2));

		mTickTask = plugin.getProxy().getScheduler().schedule(plugin, () -> {
			tick();
		}, TICK_PERIOD_SECONDS, TICK_PERIOD_SECONDS, TimeUnit.SECONDS);
	}

	public void unload() {
		mTickTask.cancel();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
    public void postLoginEvent(PostLoginEvent event) {
		ProxiedPlayer player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		try {
			VoteContext context = VoteContext.load(mPlugin, uuid);
			/* Tick the task to make sure times are current */
			long currentTime = LocalDateTime.now().toInstant(ZoneOffset.UTC).getEpochSecond();
			context.tick(currentTime);

			/* Tell the player their vote info and remind them about voting */
			context.sendVoteInfoShort(player);

			mLock.writeLock().lock();
			mContexts.put(uuid, context);
			mLock.writeLock().unlock();
		} catch (Exception ex) {
			mLogger.warning("Failed to load vote context for player '" + player.getName() + "':");
			ex.printStackTrace();
		}
    }

	@EventHandler(priority = EventPriority.LOW)
    public void playerDisconnectEvent(PlayerDisconnectEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();

		mLock.writeLock().lock();
		mContexts.remove(uuid);
		mLock.writeLock().unlock();
    }

	@EventHandler(priority = EventPriority.LOWEST)
    public void votifierEvent(VotifierEvent event) {
        Vote vote = event.getVote();
		String playerName = vote.getUsername();

		String matchingSite = null;
		long cooldown = 0;

		/* Match against URLs first (preferred) */
		for (Map.Entry<String, Long> site : SITE_TIMERS.entrySet()) {
			if (site.getKey().toLowerCase().contains(vote.getServiceName().toLowerCase())) {
				matchingSite = site.getKey();
				cooldown = site.getValue();
				break;
			}
		}
		/* Match alternate names in case URL matching failed */
		for (Map.Entry<String, String> alternate : mAlternateNames.entrySet()) {
			if (alternate.getKey().toLowerCase().contains(vote.getServiceName().toLowerCase())) {
				matchingSite = alternate.getValue();
				cooldown = SITE_TIMERS.get(matchingSite);
				break;
			}
		}
		if (matchingSite == null) {
			mLogger.severe("Got vote with no matching site : " + vote.toString());
			return;
		}

		UUID uuid = NameListener.name2uuid(playerName);
		if (uuid == null) {
			// Nothing to do - this player hasn't ever logged in here. Ignore the vote
			mLogger.warning("Got vote for unknown player '" + playerName + "'");
			return;
		}

		mLock.readLock().lock();
		VoteContext context = mContexts.get(uuid);
		mLock.readLock().unlock();

		try {
			if (context == null) {
				/* Player is not online - load their vote context or create & initialize a new one */
				context = VoteContext.load(mPlugin, uuid);
			}

			context.voteReceived(matchingSite, cooldown);
		} catch (Exception ex) {
			mLogger.warning("Failed to process vote for player '" + playerName + "':");
			ex.printStackTrace();
		}
    }

	public void onVoteCmd(ProxiedPlayer player) {
		VoteContext context = mContexts.get(player.getUniqueId());
		if (context == null) {
			player.sendMessage(new ComponentBuilder("BUG! You don't have a vote context. Please report this!").color(ChatColor.RED).create());
			return;
		}

		context.sendVoteInfoLong(player);
	}

	private void tick() {
		long currentTime = LocalDateTime.now().toInstant(ZoneOffset.UTC).getEpochSecond();

		for (ProxiedPlayer player : mPlugin.getProxy().getPlayers()) {
			VoteContext context = mContexts.get(player.getUniqueId());
			if (context != null) {
				boolean updated = context.tick(currentTime);
				if (updated) {
					context.sendVoteInfoShort(player);
				}
			}
		}
	}

	public static void gotShardVoteCountRequest(String senderName, UUID uuid, int votesUnclaimed) {
		if (MANAGER != null) {
			VoteContext context = MANAGER.mContexts.get(uuid);
			if (context == null) {
				MANAGER.mPlugin.getLogger().warning("Got vote count request for player " + uuid.toString() + " who has no vote context");
				return;
			}
			context.gotShardVoteCountRequest(senderName, uuid, votesUnclaimed);
		}
	}

	public static void gotShardRaffleEligibilityRequest(String senderName, UUID uuid, boolean claimReward, boolean eligible) {
		if (MANAGER != null) {
			VoteContext context = MANAGER.mContexts.get(uuid);
			if (context == null) {
				MANAGER.mPlugin.getLogger().warning("Got raffle eligibility request for player " + uuid.toString() + " who has no vote context");
				return;
			}
			context.gotShardRaffleEligibilityRequest(senderName, uuid, claimReward, eligible);
		}
	}

	public static Map<String, Long> getSiteTimers() {
		return SITE_TIMERS;
	}
}
