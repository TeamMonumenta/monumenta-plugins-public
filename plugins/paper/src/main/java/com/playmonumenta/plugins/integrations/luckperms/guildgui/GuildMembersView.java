package com.playmonumenta.plugins.integrations.luckperms.guildgui;

import com.playmonumenta.plugins.integrations.luckperms.GuildAccessLevel;
import com.playmonumenta.plugins.integrations.luckperms.GuildInviteLevel;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.integrations.luckperms.PlayerGuildInfo;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuildMembersView extends View {
	protected static final int PAGE_START_X = 1;
	protected static final int PAGE_START_Y = 1;
	protected static final int PAGE_WIDTH = 8;
	private static final List<GuildAccessLevel> GUILD_ACCESS_LEVELS = List.of(
		GuildAccessLevel.FOUNDER,
		GuildAccessLevel.MANAGER,
		GuildAccessLevel.MEMBER,
		GuildAccessLevel.GUEST,
		GuildAccessLevel.NONE,
		GuildAccessLevel.BLOCKED
	);
	private static final List<GuildInviteLevel> GUILD_INVITE_LEVELS = List.of(
		GuildInviteLevel.MEMBER_INVITE,
		GuildInviteLevel.GUEST_INVITE
	);

	protected TreeMap<GuildAccessLevel, List<PlayerGuildInfo>> mGuildPlayers = new TreeMap<>();
	protected TreeMap<GuildInviteLevel, List<PlayerGuildInfo>> mGuildInvites = new TreeMap<>();

	public GuildMembersView(GuildGui gui) {
		super(gui);
		// Placeholder value while real values load
		for (GuildAccessLevel accessLevel : GUILD_ACCESS_LEVELS) {
			mGuildPlayers.put(accessLevel, List.of());
		}
		for (GuildInviteLevel inviteLevel : GUILD_INVITE_LEVELS) {
			mGuildInvites.put(inviteLevel, List.of());
		}
	}

	@Override
	public void setup() {
		// Ensure current page is valid
		int totalRows = 0;

		TreeMap<Integer, GuildAccessLevel> accessStartIndices = new TreeMap<>();
		for (Map.Entry<GuildAccessLevel, List<PlayerGuildInfo>> accessLevelEntry : mGuildPlayers.entrySet()) {
			List<PlayerGuildInfo> playerList = accessLevelEntry.getValue();

			GuildAccessLevel accessLevel = accessLevelEntry.getKey();
			accessStartIndices.put(totalRows * PAGE_WIDTH, accessLevel);
			totalRows += Math.max(1, (playerList.size() + PAGE_WIDTH - 1) / PAGE_WIDTH);
		}

		TreeMap<Integer, GuildInviteLevel> inviteStartIndices = new TreeMap<>();
		for (Map.Entry<GuildInviteLevel, List<PlayerGuildInfo>> inviteLevelEntry : mGuildInvites.entrySet()) {
			List<PlayerGuildInfo> inviteList = inviteLevelEntry.getValue();

			GuildInviteLevel inviteLevel = inviteLevelEntry.getKey();
			inviteStartIndices.put(totalRows * PAGE_WIDTH, inviteLevel);
			totalRows += Math.max(1, (inviteList.size() + PAGE_WIDTH - 1) / PAGE_WIDTH);
		}

		setPageArrows(totalRows);

		if (mGui.mGuildGroup == null) {
			mGui.setTitle(Component.text("Guild not found"));
		} else {
			mGui.setTitle(Component.text("Members of ")
				.append(Component.text(LuckPermsIntegration.getNonNullGuildName(mGui.mGuildGroup))));
		}

		for (int y = 0; y < GuildGui.PAGE_HEIGHT; y++) {
			for (int x = 0; x < PAGE_WIDTH; x++) {
				int absoluteIndex = (mPage * GuildGui.PAGE_HEIGHT + y) * PAGE_WIDTH + x;
				Map.Entry<Integer, GuildInviteLevel> inviteIndexLevelPair
					= inviteStartIndices.floorEntry(absoluteIndex);
				if (inviteIndexLevelPair != null) {
					int relativeIndex = absoluteIndex - inviteIndexLevelPair.getKey();
					GuildInviteLevel inviteLevel = inviteIndexLevelPair.getValue();

					if (relativeIndex == 0 || (y == 0 && x == 0)) {
						setInviteHeaderIcon(y, inviteLevel);
					}

					List<PlayerGuildInfo> inviteLevelPlayers = mGuildInvites.computeIfAbsent(inviteLevel, k -> new ArrayList<>());
					mGui.setPlayerIcon(PAGE_START_Y + y, PAGE_START_X + x, relativeIndex, inviteLevelPlayers);

					continue;
				}

				Map.Entry<Integer, GuildAccessLevel> accessIndexLevelPair
					= accessStartIndices.floorEntry(absoluteIndex);
				int relativeIndex = absoluteIndex - accessIndexLevelPair.getKey();
				GuildAccessLevel accessLevel = accessIndexLevelPair.getValue();

				if (relativeIndex == 0 || (y == 0 && x == 0)) {
					setAccessHeaderIcon(y, accessLevel);
				}

				List<PlayerGuildInfo> accessLevelPlayers = mGuildPlayers.computeIfAbsent(accessLevel, k -> new ArrayList<>());
				mGui.setPlayerIcon(PAGE_START_Y + y, PAGE_START_X + x, relativeIndex, accessLevelPlayers);
			}
		}
	}

	@Override
	public void refresh() {
		super.refresh();

		Bukkit.getScheduler().runTaskAsynchronously(mGui.mMainPlugin, () -> {
			TreeMap<GuildAccessLevel, List<PlayerGuildInfo>> guildPlayers = new TreeMap<>();
			TreeMap<GuildInviteLevel, List<PlayerGuildInfo>> guildInvites = new TreeMap<>();
			if (mGui.mGuildGroup == null) {
				Bukkit.getScheduler().runTask(mGui.mMainPlugin, mGui::update);
				return;
			}

			for (GuildAccessLevel accessLevel : GUILD_ACCESS_LEVELS) {
				List<PlayerGuildInfo> sortedPlayers;

				try {
					Collection<UUID> playerUuids
						= LuckPermsIntegration.getGuildMembers(mGui.mGuildGroup, accessLevel).join();
					Map<String, PlayerGuildInfo> playerSortingMap = new TreeMap<>();
					for (UUID playerUuid : playerUuids) {
						User user = LuckPermsIntegration.loadUser(playerUuid).join();
						PlayerGuildInfo playerGuildInfo = PlayerGuildInfo.of(user, mGui.mGuildGroup).join();
						String sortKey = playerGuildInfo.getNameSortKey();
						playerSortingMap.put(sortKey, playerGuildInfo);
					}
					sortedPlayers = new ArrayList<>(playerSortingMap.values());
				} catch (Exception ex) {
					Bukkit.getScheduler().runTask(mGui.mMainPlugin, () -> {
						mGui.mPlayer.sendMessage(Component.text("Failed to get guild " + accessLevel.mId + ":",
							NamedTextColor.RED));
						MessagingUtils.sendStackTrace(mGui.mPlayer, ex);
					});
					sortedPlayers = List.of();
				}
				guildPlayers.put(accessLevel, sortedPlayers);
			}

			for (GuildInviteLevel inviteLevel : GUILD_INVITE_LEVELS) {
				List<PlayerGuildInfo> sortedInvites;

				try {
					Collection<UUID> playerUuids
						= LuckPermsIntegration.getGuildInvites(mGui.mGuildGroup, inviteLevel).join();
					Map<String, PlayerGuildInfo> playerSortingMap = new TreeMap<>();
					for (UUID playerUuid : playerUuids) {
						User user = LuckPermsIntegration.loadUser(playerUuid).join();
						PlayerGuildInfo playerGuildInfo = PlayerGuildInfo.of(user, mGui.mGuildGroup).join();
						String sortKey = playerGuildInfo.getNameSortKey();
						playerSortingMap.put(sortKey, playerGuildInfo);
					}
					sortedInvites = new ArrayList<>(playerSortingMap.values());
				} catch (Exception ex) {
					Bukkit.getScheduler().runTask(mGui.mMainPlugin, () -> {
						mGui.mPlayer.sendMessage(Component.text("Failed to get guild " + inviteLevel.mId + ":",
							NamedTextColor.RED));
						MessagingUtils.sendStackTrace(mGui.mPlayer, ex);
					});
					sortedInvites = List.of();
				}
				guildInvites.put(inviteLevel, sortedInvites);
			}

			Bukkit.getScheduler().runTask(mGui.mMainPlugin, () -> {
				mGuildPlayers = guildPlayers;
				mGuildInvites = guildInvites;
				mGui.update();
			});
		});
	}

	private void setAccessHeaderIcon(int pageY, GuildAccessLevel accessLevel) {
		mGui.setAccessHeaderIcon(PAGE_START_Y + pageY, GuildGui.ROW_LABEL_X, accessLevel);
	}

	private void setInviteHeaderIcon(int pageY, GuildInviteLevel inviteLevel) {
		Material material;
		Component name;
		List<Component> lore = new ArrayList<>();
		switch (inviteLevel) {
			case MEMBER_INVITE -> {
				material = Material.GOLDEN_BOOTS;
				name = Component.text("Invited as Member", NamedTextColor.GOLD);
			}
			case GUEST_INVITE -> {
				material = Material.IRON_BOOTS;
				name = Component.text("Invited as Guest", NamedTextColor.GRAY);
			}
			default -> {
				material = Material.LEATHER_BOOTS;
				name = Component.text("None?", NamedTextColor.BLACK);
			}
		}
		switch (inviteLevel) {
			case MEMBER_INVITE:
				lore.add(
					Component.text("- May join the guild as a member", NamedTextColor.GOLD)
						.decoration(TextDecoration.ITALIC, false));
				// fall through
			case GUEST_INVITE:
				lore.add(
					Component.text("- May join the guild as a guest", NamedTextColor.GRAY)
						.decoration(TextDecoration.ITALIC, false));
				break;
			default:
				lore.add(Component.text("- This should not appear!", NamedTextColor.RED)
					.decoration(TextDecoration.ITALIC, false));
		}

		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(name.decoration(TextDecoration.ITALIC, false));
		meta.lore(lore);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		item.setItemMeta(meta);

		mGui.setItem(PAGE_START_Y + pageY, GuildGui.ROW_LABEL_X, item);
	}
}
