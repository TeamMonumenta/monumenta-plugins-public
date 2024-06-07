package com.playmonumenta.plugins.integrations.luckperms.guildgui;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.integrations.luckperms.PlayerGuildInfo;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;

// Order of enum values determines order displayed in GUI; first entry is default
// Up to 9 entries may be defined (corresponding to the 9 hotbar slots)
public enum GuildOrder {
	TAG("[Guild Tag]",
		(PlayerGuildInfo guildInfo) -> {
			CompletableFuture<String> future = new CompletableFuture<>();

			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				Group guild = guildInfo.getGuild();
				try {
					Component guildFullComponent = LuckPermsIntegration.getGuildFullComponent(guild);
					String guildFullPlainString = MessagingUtils.plainText(guildFullComponent);
					future.complete(StringUtils.getNaturalSortKey(guildFullPlainString));
				} catch (Exception ex) {
					Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
						MMLog.warning("Exception sorting guild " + guild.getName() + " with sort type TAG");
						MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
					});
					future.complete("~ERROR " + guild.getName());
				}
			});

			return future;
		}),
	NAME("Guild Name",
		(PlayerGuildInfo guildInfo) -> {
			CompletableFuture<String> future = new CompletableFuture<>();

			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				Group guild = guildInfo.getGuild();
				try {
					String guildName = LuckPermsIntegration.getNonNullGuildName(guild);
					future.complete(StringUtils.getNaturalSortKey(guildName));
				} catch (Exception ex) {
					Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
						MMLog.warning("Exception sorting guild " + guild.getName() + " with sort type NAME");
						MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
					});
					future.complete("~ERROR " + guild.getName());
				}
			});

			return future;
		}),
	COUNT("Member Count",
		(PlayerGuildInfo guildInfo) -> {
			CompletableFuture<String> future = new CompletableFuture<>();

			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				Group guild = guildInfo.getGuild();
				try {
					int count;
					try {
						Group root = LuckPermsIntegration.getGuildRoot(guild);
						if (root == null) {
							// Non-guild
							count = 0;
						} else {
							count = LuckPermsIntegration.getAllGuildMembers(root, true).join().size();
						}
					} catch (Exception ex) {
						count = 0;
						Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
							MMLog.warning("Exception sorting guild " + guild.getName()
								+ " with sort type Count:");
							MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
						});
					}
					future.complete(String.format("%010d %s",
						Integer.MAX_VALUE - count,
						TAG.sortKey(guildInfo).join()));
				} catch (Exception ex) {
					Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
						MMLog.warning("Exception sorting guild " + guild.getName() + " with sort type COUNT");
						MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
					});
					future.complete("~ERROR " + guild.getName());
				}
			});

			return future;
		});

	public final String mName;
	private final Function<PlayerGuildInfo, CompletableFuture<String>> mSortMethod;

	GuildOrder(String name, Function<PlayerGuildInfo, CompletableFuture<String>> sortMethod) {
		mName = name;
		mSortMethod = sortMethod;
	}

	public static GuildOrder DEFAULT = GuildOrder.TAG;

	// Must always be unique for different guilds
	public CompletableFuture<String> sortKey(PlayerGuildInfo guildInfo) {
		return mSortMethod.apply(guildInfo);
	}

	public CompletableFuture<List<PlayerGuildInfo>> sortGuilds(Collection<PlayerGuildInfo> guilds) {
		CompletableFuture<List<PlayerGuildInfo>> future = new CompletableFuture<>();

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			// Get the sort keys in parallel
			ConcurrentHashMap<String, PlayerGuildInfo> fastAsyncMap = new ConcurrentHashMap<>();
			List<CompletableFuture<Void>> sortKeyFutures = new ArrayList<>();

			for (PlayerGuildInfo guildInfo : guilds) {
				CompletableFuture<Void> guildFuture = new CompletableFuture<>();
				sortKeyFutures.add(guildFuture);

				Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
					String sortKey = sortKey(guildInfo).join();
					fastAsyncMap.put(sortKey, guildInfo);
					guildFuture.complete(null);
				});
			}

			// Wait for all keys to be ready
			sortKeyFutures.forEach(CompletableFuture::join);

			// Sort
			TreeMap<String, PlayerGuildInfo> sortedSyncMap = new TreeMap<>(fastAsyncMap);
			future.complete(new ArrayList<>(sortedSyncMap.values()));
		});

		return future;
	}

	@Override
	public String toString() {
		return mName;
	}
}
