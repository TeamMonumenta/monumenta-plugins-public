package com.playmonumenta.plugins.integrations;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.networkrelay.NetworkRelayMessageEvent;
import com.playmonumenta.networkrelay.RemotePlayerAbstraction;
import com.playmonumenta.networkrelay.RemotePlayerMinecraft;
import com.playmonumenta.networkrelay.RemotePlayerProxy;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.protocollib.PingListener;
import com.playmonumenta.plugins.utils.MMLog;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.player.PlayerLoadEvent;
import me.neznamy.tab.api.tablist.layout.Layout;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;

public class TABIntegration implements Listener {
	public static @MonotonicNonNull TABIntegration INSTANCE;
	final TabAPI mTab;
	boolean mFirstLoad = false;
	boolean mIsRefreshing = false;
	long mLastRefresh = System.currentTimeMillis();

	String mGlobalFooter = "%monumenta_boss_details_1% | &7Total players:&f -1 | %monumenta_boss_details_2%";

	private static class Slot {
		public final int mId;
		public final String mText;
		public final @Nullable String mSkin;
		public final @Nullable Integer mPing;

		public Slot(int id, String text, @Nullable String skin, @Nullable Integer ping) {
			mId = id;
			mText = text;
			mSkin = skin;
			mPing = ping;
		}
	}

	public static class CachedLayout {
		public ConcurrentHashMap.KeySetView<Slot, Boolean> mLayout = ConcurrentHashMap.newKeySet();

		public void addFixedSlot(int id, String text) {
			mLayout.add(new Slot(id, text, null, null));
		}

		public void addFixedSlot(int id, String text, @Nullable String skin) {
			mLayout.add(new Slot(id, text, skin, null));
		}

		public void addFixedSlot(int id, String text, @Nullable String skin, int ping) {
			mLayout.add(new Slot(id, text, skin, ping));
		}

		public void addFixedSlot(int id, String text, int ping) {
			mLayout.add(new Slot(id, text, null, ping));
		}

		public Layout toLayout() {
			Layout layout = TabAPI.getInstance().getLayoutManager().createNewLayout(UUID.randomUUID().toString());
			for (Slot slot : mLayout) {
				if (slot.mSkin != null && !slot.mSkin.isEmpty() && slot.mPing != null) {
					layout.addFixedSlot(slot.mId, slot.mText, slot.mSkin, slot.mPing);
				} else if (slot.mSkin != null && !slot.mSkin.isEmpty()) {
					layout.addFixedSlot(slot.mId, slot.mText, slot.mSkin);
				} else if (slot.mPing != null) {
					layout.addFixedSlot(slot.mId, slot.mText, slot.mPing);
				} else {
					layout.addFixedSlot(slot.mId, slot.mText);
				}
			}
			return layout;
		}

		public boolean isSameLayout(@Nullable CachedLayout other) {
			if (other == null) {
				return false;
			}
			return mLayout.equals(other.mLayout);
		}
	}

	public static class MonumentaPlayer {
		public final UUID mUuid;
		public final String mName;
		public @Nullable RemotePlayerMinecraft mShardPlayer = null;
		public @Nullable RemotePlayerProxy mProxyPlayer = null;
		public boolean mIsMod = false;
		public boolean mIsAdmin = false;
		public boolean mIsHidden = false;
		public @Nullable String mPrefix;
		public @Nullable String mSuffix;
		public @Nullable String mGuild;
		public @Nullable String mSkin;
		public String mShardId = "null";
		public String mProxyId = "null";
		public int mPing = -1;
		public @Nullable CachedLayout mLastLayout = null;

		public MonumentaPlayer(RemotePlayerAbstraction player) {
			this.mUuid = player.getUuid();
			this.mName = player.getName();
			// fallback skin
			this.set(player);
		}

		public static void setPing(UUID uuid, int ping) {
			mPlayers.computeIfPresent(uuid, (key, oldValue) -> {
				oldValue.mPing = ping;
				return oldValue;
			});
		}

		public void set(RemotePlayerAbstraction player) {
			if (player instanceof RemotePlayerMinecraft playerShard) {
				this.mShardPlayer = playerShard;
				this.mIsHidden = player.isHidden() == null || player.isHidden();
				this.mShardId = player.getServerId();
				JsonObject pluginData = playerShard.getPluginData("monumenta");
				if (pluginData != null) {
					if (pluginData.has("ping")) {
						mPing = pluginData.get("ping").getAsInt();
					}
					if (pluginData.has("isMod")) {
						mIsMod = pluginData.get("isMod").getAsBoolean();
					}
					if (pluginData.has("isAdmin")) {
						mIsAdmin = pluginData.get("isAdmin").getAsBoolean();
					}
					if (pluginData.has("prefix")) {
						JsonElement temp = pluginData.get("prefix");
						if (!temp.isJsonNull()) {
							mPrefix = temp.getAsString();
						} else {
							mPrefix = null;
						}
					}
					if (pluginData.has("suffix")) {
						JsonElement temp = pluginData.get("suffix");
						if (!temp.isJsonNull()) {
							mSuffix = temp.getAsString();
						} else {
							mSuffix = null;
						}
					}
					if (pluginData.has("guild")) {
						JsonElement temp = pluginData.get("guild");
						if (!temp.isJsonNull()) {
							mGuild = temp.getAsString();
						} else {
							mGuild = null;
						}
					}
					if (pluginData.has("signed_texture")) {
						JsonElement temp = pluginData.get("signed_texture");
						if (!temp.isJsonNull()) {
							mSkin = "signed_texture:" + temp.getAsString();
						}
					}
				}
			} else if (player instanceof RemotePlayerProxy playerProxy) {
				this.mProxyPlayer = playerProxy;
				this.mProxyId = playerProxy.getServerId();
				JsonObject pluginData = playerProxy.getPluginData("monumenta-velocity");
				if (pluginData != null) {
					if (pluginData.has("signed_texture")) {
						JsonElement temp = pluginData.get("signed_texture");
						if (!temp.isJsonNull()) {
							mSkin = "signed_texture:" + temp.getAsString();
						}
					}
					if (pluginData.has("ping")) {
						mPing = pluginData.get("ping").getAsInt();
					}
				}
			}
		}
	}

	private static Map<UUID, MonumentaPlayer> mPlayers = new ConcurrentHashMap<>();

	public TABIntegration() {
		INSTANCE = this;
		this.mTab = TabAPI.getInstance();
		mTab.getEventBus().register(PlayerLoadEvent.class, this::playerLoadEvent);
		// Refresh for latency
		Bukkit.getScheduler().runTaskTimerAsynchronously(Plugin.getInstance(), () -> {
			onRefreshRequest(true);
		}, 0, 600);
	}

	public static TABIntegration getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new TABIntegration();
		}
		return INSTANCE;
	}

	public void playerLoadEvent(PlayerLoadEvent event) {
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> refreshOnlinePlayer(event.getPlayer().getUniqueId(), true));
	}

	public static void loadRemotePlayer(RemotePlayerAbstraction player) {
		if (INSTANCE == null) {
			return;
		}
		MonumentaPlayer monuPlayer = mPlayers.merge(player.getUuid(), new MonumentaPlayer(player), (oldValue, defaultValue) -> {
			oldValue.set(player);
			return oldValue;
		});
		if (monuPlayer.mShardPlayer != null && monuPlayer.mProxyPlayer != null) {
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> getInstance().onRefreshRequest(true));
		}
	}

	public static void unloadRemotePlayer(RemotePlayerAbstraction player) {
		if (INSTANCE == null) {
			return;
		}
		mPlayers.computeIfPresent(player.getUuid(), (key, oldValue) -> {
			if (player instanceof RemotePlayerMinecraft) {
				oldValue.mShardPlayer = null;
				oldValue.mShardId = "";
			} else if (player instanceof RemotePlayerProxy) {
				oldValue.mProxyPlayer = null;
				oldValue.mProxyId = "";
			}
			if (oldValue.mShardPlayer == null && oldValue.mProxyPlayer == null) {
				oldValue = null;
				Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> getInstance().onRefreshRequest(false));
			}
			return oldValue;
		});
	}

	public void refreshOnlinePlayer(UUID uuid, boolean latency) {
		if (latency) {
			Player bukkitPlayer = Bukkit.getPlayer(uuid);
			if (bukkitPlayer == null) {
				return;
			}
			refreshPing(bukkitPlayer).join();
		}
		TabPlayer viewer = this.mTab.getPlayer(uuid);
		if (viewer == null || !viewer.isLoaded()) {
			return;
		}
		final @Nullable MonumentaPlayer monuPlayer = mPlayers.get(uuid);
		if (monuPlayer == null) {
			return;
		}
		CachedLayout layout = createBaseLayout();
		layout = calculateLayout(layout, monuPlayer);
		if (layout.isSameLayout(monuPlayer.mLastLayout)) {
			return;
		}
		final CachedLayout finalLayout = layout;
		mPlayers.computeIfPresent(uuid, (key, value) -> {
			value.mLastLayout = finalLayout;
			return value;
		});
		setHeaderAndFooter(viewer, monuPlayer);
		this.mTab.getLayoutManager().sendLayout(viewer, finalLayout.toLayout());
	}

	private long mNextRefreshLatencyTime = 0L;
	private long mNextRefreshTime = 0L;
	private @Nullable BukkitTask mRefreshTimer = null;

	private void onRefreshRequest(boolean latency) {
		long now = System.currentTimeMillis();
		if (now >= mNextRefreshTime) {
			if (latency && now >= mNextRefreshLatencyTime) {
				mNextRefreshLatencyTime = now + 15000;
				latency = true;
			} else {
				latency = false;
			}
			mNextRefreshTime = now + 1000;
			if (mRefreshTimer != null) {
				mRefreshTimer.cancel();
			}
			mRefreshTimer = null;
			refresh(latency);
		} else {
			if (mRefreshTimer != null) {
				return;
			}
			final boolean finalLatency = latency;

			mRefreshTimer = Bukkit.getScheduler().runTaskLaterAsynchronously(Plugin.getInstance(), () -> onRefreshRequest(finalLatency), 20L);
		}
	}

	private void refresh(boolean latency) {
		if (latency) {
			refreshPingForLocalPlayersAndBroadcast();
			return;
		}
		if (mIsRefreshing) {
			return;
		}
		mIsRefreshing = true;
		try {
			mGlobalFooter = "%monumenta_boss_details_1% | &7Total players:&f " + NetworkRelayAPI.getVisiblePlayerNames().size() + " | %monumenta_boss_details_2%";
			TabPlayer[] tabPlayers = this.mTab.getOnlinePlayers();
			for (TabPlayer viewer : tabPlayers) {
				refreshOnlinePlayer(viewer.getUniqueId(), false);
			}
		} finally {
			mLastRefresh = System.currentTimeMillis();
			mIsRefreshing = false;
		}
	}


	private CachedLayout createBaseLayout() {
		CachedLayout layout = new CachedLayout();
		layout.addFixedSlot(1, " &6Patreon Shrine", "%monumenta_patron_shrine%");
		String[] shrines = {"Speed", "Resistance", "Strength", "Intuitive", "Thrift", "Harvester"};
		int shrineIndex = 2;
		for (String shrine : shrines) {
			layout.addFixedSlot(shrineIndex, "%monumenta_shrine_" + shrine + "%", "%condition:monumenta_shrine_" + shrine + "%");
			shrineIndex++;
		}
		// patron
		layout.addFixedSlot(9, "&6Custom Effects", "%monumenta_custom_effects%");
		for (int effectIndex = 1; effectIndex <= 10; effectIndex++) {
			layout.addFixedSlot(9 + effectIndex, "%monumenta_effect_" + effectIndex + "%");
		}
		layout.addFixedSlot(20, "%monumenta_effect_more%");
		return layout;
	}

	private static class MonumentaPlayerComparator implements Comparator<MonumentaPlayer> {
		@Override
		public int compare(MonumentaPlayer a, MonumentaPlayer b) {
			String aShard = a.mShardId == null ? "" : a.mShardId;
			String bShard = b.mShardId == null ? "" : b.mShardId;
			int result = 0;
			if (aShard.equals(bShard)) {
				result = 0;
			} else if (aShard.isEmpty()) {
				result = 1;
			} else if (bShard.isEmpty()) {
				result = -1;
			} else {
				result = aShard.compareToIgnoreCase(bShard);
			}
			if (result != 0) {
				return result;
			}
			String aGuild = a.mGuild == null ? "" : a.mGuild;
			String bGuild = b.mGuild == null ? "" : b.mGuild;
			if (aGuild.equals(bGuild)) {
				result = 0;
			} else if (aGuild.isEmpty()) {
				result = 1;
			} else if (bGuild.isEmpty()) {
				result = -1;
			} else {
				result = aGuild.compareToIgnoreCase(bGuild);
			}
			if (result != 0) {
				return result;
			}
			return a.mName.compareToIgnoreCase(b.mName);
		}
	}

	private CachedLayout calculateLayout(CachedLayout layout, MonumentaPlayer monuPlayer) {
		// not optimized at all, store permission/meta somewhere when initally fetching the player
		int layoutIndex = 21;

		// sort by shard
		Collection<MonumentaPlayer> players = mPlayers.values().stream()
			.sorted(new MonumentaPlayerComparator())
			.collect(Collectors.toList());

		boolean isAdminOrMod = (monuPlayer.mIsMod || monuPlayer.mIsAdmin);
		// modlist
		boolean modHasHeader = false;
		for (MonumentaPlayer modPlayer : players) {
			if (!modPlayer.mIsMod) {
				continue;
			}
			if (!isAdminOrMod && modPlayer.mIsHidden) {
				continue;
			}
			if (!modHasHeader) { // :suffer:
				modHasHeader = true;
				layout.addFixedSlot(layoutIndex, "&c&lModerators", "mineskin:1160568696");
				layoutIndex++;
				if (layoutIndex >= 81) {
					return layout;
				}
			}
			layout.addFixedSlot(layoutIndex, formatPlayer(modPlayer, isAdminOrMod), modPlayer.mSkin, modPlayer.mPing);
			layoutIndex++;
			if (layoutIndex >= 81) {
				return layout;
			}
		}

		// guild
		boolean guildHasHeader = false;
		if (monuPlayer.mGuild != null) {
			for (MonumentaPlayer guildPlayer : players) {
				// hide check
				if (!isAdminOrMod && guildPlayer.mIsHidden) {
					continue;
				}
				if (guildPlayer.mGuild == null || !guildPlayer.mGuild.equals(monuPlayer.mGuild)) {
					continue;
				}
				if (!guildHasHeader) {
					guildHasHeader = true;
					if (modHasHeader) {
						layout.addFixedSlot(layoutIndex, "");
						layoutIndex++;
						if (layoutIndex >= 81) {
							return layout;
						}
					}
					layout.addFixedSlot(layoutIndex, "&a&lGuild", "mineskin:224445819");
					layoutIndex++;
					if (layoutIndex >= 81) {
						return layout;
					}
				}
				layout.addFixedSlot(layoutIndex, formatPlayer(guildPlayer, isAdminOrMod), guildPlayer.mSkin, guildPlayer.mPing);
				layoutIndex++;
				if (layoutIndex >= 81) {
					return layout;
				}
			}
		}

		// playerlist
		boolean regularHasHeader = false;
		for (MonumentaPlayer regularPlayer : players) {
			// hide check
			if (!isAdminOrMod && regularPlayer.mIsHidden) {
				continue;
			}
			if (regularPlayer.mIsMod || (regularPlayer.mGuild != null && regularPlayer.mGuild.equals(monuPlayer.mGuild))) {
				continue;
			}
			if (!regularHasHeader) {
				regularHasHeader = true;
				if (modHasHeader || guildHasHeader) {
					layout.addFixedSlot(layoutIndex, "");
					layoutIndex++;
					if (layoutIndex >= 81) {
						return layout;
					}
				}
				layout.addFixedSlot(layoutIndex, "&f&lPlayers", "mineskin:1105851698");
				layoutIndex++;
				if (layoutIndex >= 81) {
					return layout;
				}
			}
			layout.addFixedSlot(layoutIndex, formatPlayer(regularPlayer, isAdminOrMod), regularPlayer.mSkin, regularPlayer.mPing);
			layoutIndex++;
			if (layoutIndex >= 81) {
				return layout;
			}
		}

		return layout;
	}

	private void setHeaderAndFooter(TabPlayer viewer, MonumentaPlayer player) {
		String header = "Connected to proxy: &7<" + player.mProxyId + ">&r shard: &7<" + player.mShardId + ">";
		// String footer = "%monumenta_boss_details_1% &7 Total players:&f %online% %monumenta_boss_details_2%")
		this.mTab.getHeaderFooterManager().setHeaderAndFooter(viewer, header, mGlobalFooter);
	}

	private String formatPlayer(MonumentaPlayer player, boolean canSeeVanished) {
		String prefix = player.mPrefix != null ? "&" + player.mPrefix + (player.mIsAdmin ? "" : " ") : ""; // luckperm prefixes include guild tag
		String suffix = player.mSuffix != null ? player.mSuffix : ""; // luckperm suffixes are just color codes
		String vanished = (canSeeVanished && player.mIsHidden) ? "&b[V]&r " : "";
		// &7<${player server}>${vanish_suffix}${adminOrNoGuild}${hexColorTag}${player luckpermsbungee_prefix}${isAdmin}&r${player luckpermsbungee_suffix}${player display_name}
		String finished = MessageFormat.format("&7<{0}> {1}{2}&r{3}{4}", player.mShardId, vanished, prefix, suffix, player.mName);
		return finished;
	}

	// Ping handling
	private CompletableFuture<Void> refreshPing(@Nullable Player player) {
		if (player == null) {
			return CompletableFuture.completedFuture(null);
		}
		CompletableFuture<Void> promise = new CompletableFuture<>();
		PingListener.submitPingAction(player, (ping) -> {
			MonumentaPlayer.setPing(player.getUniqueId(), ping);
			promise.complete(null);
		}, Constants.TICKS_PER_SECOND * 1, false, () -> {
			MonumentaPlayer.setPing(player.getUniqueId(), 1000);
			promise.complete(null);
		});
		return promise;
	}

	private void refreshPingForLocalPlayersAndBroadcast() {
		// have to run on main thread because Bukkit.getOnlinePlayers shouldn't be used in an async thread
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			List<CompletableFuture<Void>> futures = new ArrayList<>();
			Collection<Player> players = ImmutableList.copyOf(Bukkit.getOnlinePlayers());
			for (Player player : players) {
				futures.add(refreshPing(player));
			}
			final List<UUID> uuids = players.stream().map(x -> x.getUniqueId()).toList();
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenCompleteAsync((a, ex) -> {
				if (ex != null) {
					// cry
					return;
				}
				JsonObject pingData = new JsonObject();
				for (UUID uuid : uuids) {
					final MonumentaPlayer player = mPlayers.get(uuid);
					if (player == null) {
						continue;
					}
					pingData.addProperty(uuid.toString(), player.mPing);
				}
				JsonObject data = new JsonObject();
				data.add("ping", pingData);
				try {
					NetworkRelayAPI.sendBroadcastMessage("monumenta.ping", data);
				} catch (Exception eex) {
					MMLog.warning("Failed to send global ping message: ", ex);
				}
				onRefreshRequest(false);
			});
		});
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
	public void remotePlayerLoad(NetworkRelayMessageEvent event) {
		if (event.getChannel().equals("monumenta.ping") && !event.getSource().equals(NetworkRelayAPI.getShardName())) {
			JsonObject data = event.getData();
			if (data.has("ping")) {
				Set<Map.Entry<String, JsonElement>> uuids = data.getAsJsonObject("ping").entrySet();
				for (Map.Entry<String, JsonElement> entry : uuids) {
					try {
						UUID uuid = UUID.fromString(entry.getKey());
						int ping = entry.getValue().getAsInt();
						MonumentaPlayer.setPing(uuid, ping);
					} catch (Exception ex) {
						MMLog.warning("Failed to process global ping message: ", ex);
					}
				}
				onRefreshRequest(false);
			}
		}
	}
}