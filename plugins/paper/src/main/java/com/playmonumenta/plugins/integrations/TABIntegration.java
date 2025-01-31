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
import com.playmonumenta.plugins.commands.TablistCommand;
import com.playmonumenta.plugins.protocollib.PingListener;
import com.playmonumenta.plugins.schedule.ExecutorWrapper;
import com.playmonumenta.plugins.utils.MMLog;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.player.PlayerLoadEvent;
import me.neznamy.tab.api.tablist.layout.Layout;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;

public class TABIntegration implements Listener {
	private static final Pattern RE_SHARD_INSTANCE_SUFFIX = Pattern.compile("-?[0-9]*$");

	public static @MonotonicNonNull TABIntegration INSTANCE;
	final TabAPI mTab;
	boolean mIsRefreshing = false;
	long mLastRefresh = System.currentTimeMillis();

	String mGlobalFooter = "%monumenta_boss_details_1% | &7Total players:&f -1 | %monumenta_boss_details_2%";

	public static class Slot {
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

		public Layout toLayout(UUID uuid) {
			Layout layout = Objects.requireNonNull(TabAPI.getInstance().getLayoutManager()).createNewLayout(uuid.toString());
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
		public String mShardId = "";
		public String mProxyShardId = "";
		public String mProxyId = "";
		public int mPing = -1;
		public @Nullable CachedLayout mLastLayout = null;

		public MonumentaPlayer(RemotePlayerAbstraction player) {
			mUuid = player.getUuid();
			mName = player.getName();
			// fallback skin
			set(player);
		}

		public MonumentaPlayer(String name, UUID uuid, String shard) {
			mUuid = uuid;
			mName = name;
			mShardId = shard;
		}

		public static void setPing(UUID uuid, int ping) {
			mPlayers.computeIfPresent(uuid, (key, oldValue) -> {
				oldValue.mPing = ping;
				return oldValue;
			});
		}

		public String getShardId() {
			return this.mShardId.isEmpty() ? this.mProxyShardId : this.mShardId;
		}

		public void set(RemotePlayerAbstraction player) {
			if (player instanceof RemotePlayerMinecraft playerShard) {
				mShardPlayer = playerShard;
				Boolean isHidden = player.isHidden();
				mIsHidden = isHidden == null || isHidden;
				mShardId = player.getServerId();
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
				mProxyPlayer = playerProxy;
				mProxyId = playerProxy.getServerId();
				mProxyShardId = playerProxy.targetShard();
				if (mShardId.isEmpty()) {
					mShardId = playerProxy.targetShard();
				}
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

	private static final Map<UUID, MonumentaPlayer> mPlayers = new ConcurrentHashMap<>();
	private static final ExecutorWrapper mExecutor = new ExecutorWrapper("TABIntegration");

	public TABIntegration() {
		INSTANCE = this;
		TablistCommand.register();
		mTab = TabAPI.getInstance();
		Objects.requireNonNull(mTab.getEventBus()).register(PlayerLoadEvent.class, this::playerLoadEvent);
		// Refresh for latency
		mExecutor.scheduleRepeatingTask(() -> onRefreshRequest(true), 0L, 30L, TimeUnit.SECONDS);
	}

	public static TABIntegration getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new TABIntegration();
		}
		return INSTANCE;
	}

	public void playerLoadEvent(PlayerLoadEvent event) {
		mExecutor.schedule(() -> refreshOnlinePlayer(event.getPlayer().getUniqueId(), true));
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
			getInstance().onRefreshRequest(false);
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
				getInstance().onRefreshRequest(false);
			}
			return oldValue;
		});
	}

	private static final Set<UUID> fakePlayers = new HashSet<>();

	// adds a fake player for testing - doesn't affect other shards
	public static void addFakePlayer(String name, String shard) {
		UUID fakeUuid = UUID.randomUUID();
		fakePlayers.add(fakeUuid);
		MonumentaPlayer fakePlayer = new MonumentaPlayer(name, fakeUuid, shard);
		mPlayers.put(fakeUuid, fakePlayer);
	}

	public static void removeFakePlayers() {
		for (UUID fake : fakePlayers) {
			mPlayers.remove(fake);
		}
		fakePlayers.clear();
	}

	public void refreshOnlinePlayer(UUID uuid, boolean latency) {
		if (latency) {
			Player bukkitPlayer = Bukkit.getPlayer(uuid);
			if (bukkitPlayer == null) {
				return;
			}
			refreshPing(bukkitPlayer).join();
		}
		TabPlayer viewer = mTab.getPlayer(uuid);
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
		Objects.requireNonNull(mTab.getLayoutManager()).sendLayout(viewer, finalLayout.toLayout(viewer.getUniqueId()));
	}

	private long mNextRefreshLatencyTime = 0L;
	private long mNextRefreshTime = 0L;
	private static boolean mNeedsRefresh = false;
	private static boolean mRefreshLatency = false;
	private static boolean mScheduled = false;

	private void onRefreshRequest(boolean latency) {
		mNeedsRefresh = true;
		mRefreshLatency = mRefreshLatency || latency;
	}

	// this should be run async
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
			TabPlayer[] tabPlayers = mTab.getOnlinePlayers();
			for (TabPlayer viewer : tabPlayers) {
				refreshOnlinePlayer(viewer.getUniqueId(), false);
			}
		} finally {
			mLastRefresh = System.currentTimeMillis();
			mIsRefreshing = false;
		}
	}

	private void attemptRefresh() {
		final long now = System.currentTimeMillis();
		if (mNeedsRefresh && !mScheduled && now >= mNextRefreshTime && (!mRefreshLatency || now >= mNextRefreshLatencyTime)) {
			final boolean refreshLatency = mRefreshLatency;
			mRefreshLatency = false;
			mNeedsRefresh = false;
			mScheduled = true;
			mExecutor.schedule(() -> {
				refresh(refreshLatency);
				mScheduled = false;
			});
			if (refreshLatency) {
				mNextRefreshLatencyTime = System.currentTimeMillis() + 5000L;
			}
			mNextRefreshTime = System.currentTimeMillis() + 1000L;
		}
	}


	public static void tick() {
		if (INSTANCE == null) {
			return;
		}
		getInstance().attemptRefresh();
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

	private static final class MonumentaPlayerComparator implements Comparator<MonumentaPlayer> {
		private final String mLocalShard;
		private final String mBaseShard;

		private MonumentaPlayerComparator(MonumentaPlayer viewer) {
			mLocalShard = viewer.mShardId == null ? "" : viewer.mShardId;
			mBaseShard = RE_SHARD_INSTANCE_SUFFIX
				.matcher(mLocalShard)
				.replaceAll("");
		}

		@Override
		public int compare(MonumentaPlayer a, MonumentaPlayer b) {
			// Prefix of 0 for set shards and 9 for null shards
			// ensures null shards go last without affecting displayed name
			String aShard = a.mShardId == null ? "zzznull" : a.mShardId;
			String bShard = b.mShardId == null ? "zzznull" : b.mShardId;

			String aBaseShard = RE_SHARD_INSTANCE_SUFFIX
				.matcher(aShard)
				.replaceAll("");
			String bBaseShard = RE_SHARD_INSTANCE_SUFFIX
				.matcher(bShard)
				.replaceAll("");

			int result = aBaseShard.compareToIgnoreCase(bBaseShard);
			if (result != 0) {
				if (aBaseShard.equals(mBaseShard)) {
					return -1;
				} else if (bBaseShard.equals(mBaseShard)) {
					return 1;
				}
				return result;
			}

			result = aShard.compareToIgnoreCase(bShard);
			if (result != 0) {
				if (aShard.equals(mLocalShard)) {
					result = -1;
				} else if (bShard.equals(mLocalShard)) {
					result = 1;
				}
				return result;
			}

			String aGuild = a.mGuild == null ? "zzznull" : a.mGuild;
			String bGuild = b.mGuild == null ? "zzznull" : b.mGuild;
			result = aGuild.compareToIgnoreCase(bGuild);
			if (result != 0) {
				return result;
			}

			return a.mName.compareToIgnoreCase(b.mName);
		}
	}

	private CachedLayout calculateLayout(CachedLayout layout, MonumentaPlayer monuPlayer) {
		// not optimized at all, store permission/meta somewhere when initially fetching the player
		int layoutIndex = 21;

		// sort by shard
		Collection<MonumentaPlayer> players = mPlayers.values().stream()
			.sorted(new MonumentaPlayerComparator(monuPlayer))
			.toList();

		boolean isAdminOrMod = (monuPlayer.mIsMod || monuPlayer.mIsAdmin);
		// mod list
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
				// layoutIndex is 22, never >= 81 here
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

		// player list
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
		// String footer = "%monumenta_boss_details_1% &7 Total players:&f %online% %monumenta_boss_details_2%"
		Objects.requireNonNull(mTab.getHeaderFooterManager()).setHeaderAndFooter(viewer, header, mGlobalFooter);
	}

	private String formatPlayer(MonumentaPlayer player, boolean canSeeVanished) {
		String prefix = player.mPrefix != null ? "&" + player.mPrefix + (player.mIsAdmin ? "" : " ") : ""; // luckperm prefixes include guild tag
		String suffix = player.mSuffix != null ? player.mSuffix : ""; // luckperm suffixes are just color codes
		String vanished = (canSeeVanished && player.mIsHidden) ? "&b[V]&r " : "";
		// &7<${player server}>${vanish_suffix}${adminOrNoGuild}${hexColorTag}${player luckpermsbungee_prefix}${isAdmin}&r${player luckpermsbungee_suffix}${player display_name}
		return MessageFormat.format("&7<{0}> {1}{2}&r{3}{4}", player.mShardId, vanished, prefix, suffix, player.mName);
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
			final List<UUID> uuids = players.stream().map(Entity::getUniqueId).toList();
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
					MMLog.warning("Failed to send global ping message: ", eex);
				}
				getInstance().onRefreshRequest(false);
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
				getInstance().onRefreshRequest(false);
			}
		}
	}
}
