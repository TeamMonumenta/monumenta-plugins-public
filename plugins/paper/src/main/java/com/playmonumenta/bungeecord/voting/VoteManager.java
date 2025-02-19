package com.playmonumenta.bungeecord.voting;

import com.google.common.base.Ascii;
import com.playmonumenta.bungeecord.integrations.NetworkRelayIntegration;
import com.playmonumenta.redissync.RedisAPI;
import com.vexsoftware.votifier.bungee.events.VotifierEvent;
import com.vexsoftware.votifier.model.Vote;
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
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class VoteManager implements Listener {
	private static final int TICK_PERIOD_SECONDS = 60;

	private static @MonotonicNonNull VoteManager MANAGER = null;

	private final Plugin mPlugin;
	private final Map<UUID, VoteContext> mContexts = new ConcurrentHashMap<>();
	private final ScheduledTask mTickTask;
	private final Map<String, String> mAlternateNames = new HashMap<>();

	/*
	 * The time in minutes between allowed votes on these sites.
	 * Not padded - exact times
	 * Padding will be handled in the task that notifies players they became eligible to vote
	 */
	private static final Map<String, Long> SITE_TIMERS = new HashMap<>();

	public VoteManager(Plugin plugin, Configuration config) throws IllegalArgumentException {
		MANAGER = this;
		mPlugin = plugin;

		if (!config.contains("sites")) {
			throw new IllegalArgumentException("Voting config missing 'sites' string list");
		} else if (!config.contains("cooldown_minutes")) {
			throw new IllegalArgumentException("Voting config missing 'cooldown_minutes' int list");
		}

		List<String> urls = config.getStringList("sites");
		List<String> alternateNames = config.getStringList("alternate_names");
		List<Integer> times = config.getIntList("cooldown_minutes");

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

		mTickTask = plugin.getProxy().getScheduler().schedule(plugin, this::tick, TICK_PERIOD_SECONDS, TICK_PERIOD_SECONDS, TimeUnit.SECONDS);
	}

	public void unload() {
		mTickTask.cancel();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void postLoginEvent(PostLoginEvent event) {
		ProxiedPlayer player = event.getPlayer();
		UUID uuid = player.getUniqueId();

		VoteContext.getVoteContext(mPlugin, uuid).whenComplete((context, ex) -> {
			if (ex != null) {
				mPlugin.getLogger().warning("Exception getting vote context after login: " + ex.getMessage());
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

	@EventHandler(priority = EventPriority.LOW)
	public void playerDisconnectEvent(PlayerDisconnectEvent event) {
		mContexts.remove(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.LOWEST)
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
			mPlugin.getLogger().severe("Got vote with no matching site : " + vote);
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
				mPlugin.getLogger().warning("Failed to look up name2uuid for " + playerName + "': " + ex.getMessage());
			} else {
				if (uuid == null) {
					mPlugin.getLogger().warning("Got vote for unknown player '" + playerName + "'");
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
								mPlugin.getLogger().warning("Exception getting vote context for offline vote: " + e.getMessage());
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

	public void onVoteCmd(ProxiedPlayer player) {
		UUID uuid = player.getUniqueId();
		VoteContext context = mContexts.get(uuid);
		if (context != null) {
			context.sendVoteInfoLong(player);
		} else {
			// This is weird, they're online, but they don't have a vote context. Might as well just load it and add to the map
			VoteContext.getVoteContext(mPlugin, uuid).whenComplete((ctx, e) -> {
				if (e != null) {
					mPlugin.getLogger().warning("Exception getting vote context for /vote command: " + e.getMessage());
					player.sendMessage(BungeeComponentSerializer.get().serialize(Component.text("Encountered an unexpected error while running this command.", NamedTextColor.RED)));
				} else {
					mContexts.put(uuid, ctx);
					ctx.sendVoteInfoLong(player);
				}
			});
		}
	}

	private void tick() {
		long currentTime = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC);

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

	public static Map<String, Long> getSiteTimers() {
		return SITE_TIMERS;
	}
}
