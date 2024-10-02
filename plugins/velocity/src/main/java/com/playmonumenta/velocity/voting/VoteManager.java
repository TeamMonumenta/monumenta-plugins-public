package com.playmonumenta.velocity.voting;

import com.google.common.base.Ascii;
import com.playmonumenta.redissync.RedisAPI;
import com.playmonumenta.velocity.MonumentaVelocity;
import com.playmonumenta.velocity.MonumentaVelocity.MonumentaVelocityConfiguration;
import com.playmonumenta.velocity.integrations.NetworkRelayIntegration;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.velocity.event.VotifierEvent;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class VoteManager {
	private static final int TICK_PERIOD_SECONDS = 60;

	private static @MonotonicNonNull VoteManager MANAGER = null;

	private final MonumentaVelocity mPlugin;
	private final Map<UUID, VoteContext> mContexts = new ConcurrentHashMap<>();
	private final ScheduledTask mTickTask;
	private final Map<String, String> mAlternateNames = new HashMap<>();

	/*
	 * The time in minutes between allowed votes on these sites.
	 * Not padded - exact times
	 * Padding will be handled in the task that notifies players they became eligible to vote
	 */
	private static final Map<String, Long> SITE_TIMERS = new HashMap<>();

	public VoteManager(MonumentaVelocity plugin, MonumentaVelocityConfiguration config) throws IllegalArgumentException {
		MANAGER = this;
		mPlugin = plugin;

		List<String> urls = config.mVoting.mUrls;
		List<String> alternateNames = config.mVoting.mAlternateNames;
		List<Integer> times = config.mVoting.mCooldownMinutes;

		if (urls.size() < 1 || times.size() < 1 || urls.size() != times.size()) {
			throw new IllegalArgumentException("Voting config sites / cooldown_minutes length mismatch (or they are empty)");
		}

		for (int i = 0; i < urls.size(); i++) {
			SITE_TIMERS.put(urls.get(i), Long.valueOf(times.get(i)));
			if (!alternateNames.get(i).isEmpty()) {
				mAlternateNames.put(alternateNames.get(i), urls.get(i));
			}
		}

		// For testing!
		//SITE_TIMERS.put("https://mctools.org/votifier-tester", Long.valueOf(2));

		mTickTask = plugin.mServer.getScheduler().buildTask(plugin, this::tick).delay(TICK_PERIOD_SECONDS, TimeUnit.SECONDS).repeat(TICK_PERIOD_SECONDS, TimeUnit.SECONDS).schedule();
	}

	public void unload() {
		mTickTask.cancel();
	}

	@Subscribe(order = PostOrder.LAST)
	public void postLoginEvent(ServerPostConnectEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();

		VoteContext.getVoteContext(mPlugin, uuid).whenComplete((context, ex) -> {
			if (ex != null) {
				mPlugin.mLogger.warn("Exception getting vote context after login: " + ex.getMessage(), ex);
			} else {
				/* Tick the task to make sure times are current */
				long currentTime = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC);
				context.tick(currentTime);

				/* Tell the player their vote info and remind them about voting */
				context.sendVoteInfoShort(player);

				mContexts.put(uuid, context);
			}
		});
	}

	@Subscribe(order = PostOrder.EARLY)
	public void playerDisconnectEvent(DisconnectEvent event) {
		mContexts.remove(event.getPlayer().getUniqueId());
	}

	@Subscribe(order = PostOrder.FIRST)
	public void votifierEvent(VotifierEvent event) {
		Vote vote = event.getVote();
		String playerName = vote.getUsername();

		String matchingSite = null;
		long cooldown = 0;

		/* Match against URLs first (preferred) */
		for (Map.Entry<String, Long> site : SITE_TIMERS.entrySet()) {
			if (Ascii.toLowerCase(site.getKey()).contains(Ascii.toLowerCase(vote.getServiceName()))) {
				matchingSite = site.getKey();
				cooldown = site.getValue();
				break;
			}
		}
		/* Match alternate names in case URL matching failed */
		for (Map.Entry<String, String> alternate : mAlternateNames.entrySet()) {
			if (Ascii.toLowerCase(alternate.getKey()).contains(Ascii.toLowerCase(vote.getServiceName()))) {
				matchingSite = alternate.getValue();
				cooldown = Objects.requireNonNull(SITE_TIMERS.get(matchingSite));
				break;
			}
		}
		if (matchingSite == null) {
			mPlugin.mLogger.error("Got vote with no matching site : " + vote);
			return;
		}

		final String finalMatchingSite = matchingSite;
		final long finalCooldown = cooldown;

		// TODO: This needs to be updated once the redis sync API works correctly from bungee
		// The line actually used here is copied from there because the API is broken on bungee and fixing it is annoying
		// The whole name thing needs refactoring for bungee support...
		// MonumentaRedisSyncAPI.nameToUUID(playerName).whenComplete((uuid, ex) -> {
		RedisAPI.getInstance().async().hget("name2uuid", playerName).thenApply((uuid) -> (uuid == null || uuid.isEmpty()) ? null : UUID.fromString(uuid)).toCompletableFuture().whenComplete((uuid, ex) -> {
			if (ex != null) {
				mPlugin.mLogger.warn("Failed to look up name2uuid for " + playerName + "': " + ex.getMessage(), ex);
			} else {
				if (uuid == null) {
					mPlugin.mLogger.warn("Got vote for unknown player '" + playerName + "'");
				} else {
					VoteContext context = mContexts.get(uuid);
					if (context != null) {
						context.voteReceived(finalMatchingSite, finalCooldown); // Note that this will save internally

						// Broadcast an update to other bungee servers to update & potentially notify the player
						NetworkRelayIntegration.sendVoteNotifyPacket(uuid, finalMatchingSite, finalCooldown);
					} else {
						/* Player is not online on this proxy - load their vote context or create & initialize a new one */
						VoteContext.getVoteContext(mPlugin, uuid).whenComplete((ctx, e) -> {
							if (e != null) {
								mPlugin.mLogger.warn("Exception getting vote context for offline vote: " + e.getMessage(), e);
							} else {
								// Intentionally don't add the context to mContexts - they're (probably) not online.
								ctx.voteReceived(finalMatchingSite, finalCooldown); // Note that this will save internally

								// Broadcast an update to other bungee servers to update & potentially notify the player
								NetworkRelayIntegration.sendVoteNotifyPacket(uuid, finalMatchingSite, finalCooldown);
							}
						});
					}
				}
			}
		});
	}

	public static void gotVoteNotifyMessage(UUID playerUUID, String matchingSite, long cooldownMinutes) {
		if (MANAGER != null) {
			VoteContext context = MANAGER.mContexts.get(playerUUID);
			if (context != null) {
				/* Only need to notify the player if they're online on this bungee instance */
				context.voteNotify(matchingSite, cooldownMinutes); // Note that this will save internally
			}
		}
	}

	public void onVoteCmd(Player player) {
		UUID uuid = player.getUniqueId();
		VoteContext context = mContexts.get(uuid);
		if (context != null) {
			context.sendVoteInfoLong(player);
		} else {
			// This is weird, they're online, but they don't have a vote context. Might as well just load it and add to the map
			VoteContext.getVoteContext(mPlugin, uuid).whenComplete((ctx, e) -> {
				if (e != null) {
					mPlugin.mLogger.warn("Exception getting vote context for /vote command: " + e.getMessage(), e);
					player.sendMessage(Component.text("Encountered an unexpected error while running this command.", NamedTextColor.RED));
				} else {
					mContexts.put(uuid, ctx);
					ctx.sendVoteInfoLong(player);
				}
			});
		}
	}

	private void tick() {
		long currentTime = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC);

		for (Player player : mPlugin.mServer.getAllPlayers()) {
			VoteContext context = mContexts.get(player.getUniqueId());
			if (context != null) {
				boolean updated = context.tick(currentTime);
				if (updated) {
					context.sendVoteInfoShort(player);
				}
			}
		}
	}

	public static Map<String, Long> getSiteTimers() {
		return SITE_TIMERS;
	}
}
