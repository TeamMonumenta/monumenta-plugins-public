package com.playmonumenta.plugins.integrations.luckperms.guildgui;

import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.integrations.luckperms.GuildAccessLevel;
import com.playmonumenta.plugins.integrations.luckperms.GuildInviteLevel;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class GuildMembersView extends View {
	protected static final int PAGE_START_X = 1;
	protected static final int PAGE_START_Y = 1;
	protected static final int PAGE_WIDTH = 8;
	private static final List<GuildAccessLevel> GUILD_ACCESS_LEVELS = List.of(
		GuildAccessLevel.FOUNDER,
		GuildAccessLevel.MANAGER,
		GuildAccessLevel.MEMBER,
		GuildAccessLevel.GUEST
	);
	private static final List<GuildInviteLevel> GUILD_INVITE_LEVELS = List.of(
		GuildInviteLevel.MEMBER_INVITE,
		GuildInviteLevel.GUEST_INVITE
	);

	protected TreeMap<GuildAccessLevel, List<String>> mGuildPlayers = new TreeMap<>();
	protected TreeMap<GuildInviteLevel, List<String>> mGuildInvites = new TreeMap<>();

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
		for (Map.Entry<GuildAccessLevel, List<String>> accessLevelEntry : mGuildPlayers.entrySet()) {
			List<String> playerList = accessLevelEntry.getValue();

			GuildAccessLevel accessLevel = accessLevelEntry.getKey();
			accessStartIndices.put(totalRows * PAGE_WIDTH, accessLevel);
			totalRows += Math.max(1, (playerList.size() + PAGE_WIDTH - 1) / PAGE_WIDTH);
		}

		TreeMap<Integer, GuildInviteLevel> inviteStartIndices = new TreeMap<>();
		for (Map.Entry<GuildInviteLevel, List<String>> inviteLevelEntry : mGuildInvites.entrySet()) {
			List<String> inviteList = inviteLevelEntry.getValue();

			GuildInviteLevel inviteLevel = inviteLevelEntry.getKey();
			inviteStartIndices.put(totalRows * PAGE_WIDTH, inviteLevel);
			totalRows += Math.max(1, (inviteList.size() + PAGE_WIDTH - 1) / PAGE_WIDTH);
		}

		mGui.setPageArrows(totalRows);

		if (mGui.mGuildGroup == null) {
			mGui.setTitle(Component.text("Guild not found"));
		} else {
			mGui.setTitle(Component.text("Members of ")
				.append(Component.text(LuckPermsIntegration.getNonNullGuildName(mGui.mGuildGroup))));
		}

		for (int y = 0; y < GuildGui.PAGE_HEIGHT; y++) {
			for (int x = 0; x < PAGE_WIDTH; x++) {
				int absoluteIndex = (mGui.mPage * GuildGui.PAGE_HEIGHT + y) * PAGE_WIDTH + x;
				Map.Entry<Integer, GuildInviteLevel> inviteIndexLevelPair
					= inviteStartIndices.floorEntry(absoluteIndex);
				if (inviteIndexLevelPair != null) {
					int relativeIndex = absoluteIndex - inviteIndexLevelPair.getKey();
					GuildInviteLevel inviteLevel = inviteIndexLevelPair.getValue();

					if (relativeIndex == 0 || (y == 0 && x == 0)) {
						setInviteHeaderItem(y, inviteLevel);
					}

					List<String> inviteLevelPlayers = mGuildInvites.computeIfAbsent(inviteLevel, k -> new ArrayList<>());
					setPlayerIcon(x, y, relativeIndex, inviteLevelPlayers);

					continue;
				}

				Map.Entry<Integer, GuildAccessLevel> accessIndexLevelPair
					= accessStartIndices.floorEntry(absoluteIndex);
				int relativeIndex = absoluteIndex - accessIndexLevelPair.getKey();
				GuildAccessLevel accessLevel = accessIndexLevelPair.getValue();

				if (relativeIndex == 0 || (y == 0 && x == 0)) {
					setAccessHeaderItem(y, accessLevel);
				}

				List<String> accessLevelPlayers = mGuildPlayers.computeIfAbsent(accessLevel, k -> new ArrayList<>());
				setPlayerIcon(x, y, relativeIndex, accessLevelPlayers);
			}
		}
	}

	@Override
	public void refresh() {
		super.refresh();

		Bukkit.getScheduler().runTaskAsynchronously(mGui.mMainPlugin, () -> {
			if (mGui.mGuildGroup == null) {
				return;
			}

			TreeMap<GuildAccessLevel, List<String>> guildPlayers = new TreeMap<>();
			for (GuildAccessLevel accessLevel : GUILD_ACCESS_LEVELS) {
				List<String> sortedPlayers;

				try {
					Collection<UUID> playerUuids
						= LuckPermsIntegration.getGuildMembers(mGui.mGuildGroup, accessLevel).join();
					sortedPlayers = PlayerUtils.sortedPlayerNamesFromUuids(playerUuids);
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

			TreeMap<GuildInviteLevel, List<String>> guildInvites = new TreeMap<>();
			for (GuildInviteLevel inviteLevel : GUILD_INVITE_LEVELS) {
				List<String> sortedInvites;

				try {
					Collection<UUID> playerUuids
						= LuckPermsIntegration.getGuildInvites(mGui.mGuildGroup, inviteLevel).join();
					sortedInvites = PlayerUtils.sortedPlayerNamesFromUuids(playerUuids);
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

	private void setAccessHeaderItem(int pageY, GuildAccessLevel accessLevel) {
		Material material;
		Component name;
		List<Component> lore = new ArrayList<>();
		switch (accessLevel) {
			case FOUNDER -> {
				material = Material.NETHERITE_HELMET;
				name = Component.text("Founder", NamedTextColor.DARK_GRAY);
			}
			case MANAGER -> {
				material = Material.DIAMOND_HELMET;
				name = Component.text("Manager", NamedTextColor.AQUA);
			}
			case MEMBER -> {
				material = Material.GOLDEN_HELMET;
				name = Component.text("Member", NamedTextColor.GOLD);
			}
			case GUEST -> {
				material = Material.IRON_HELMET;
				name = Component.text("Guest", NamedTextColor.GRAY);
			}
			default -> {
				material = Material.LEATHER_HELMET;
				name = Component.text("None?", NamedTextColor.BLACK);
			}
		}
		switch (accessLevel) {
			case FOUNDER:
				lore.add(
					Component.text("Founders may:", NamedTextColor.DARK_GRAY)
						.decoration(TextDecoration.ITALIC, false));
				lore.add(
					Component.text("- May promote and demote", NamedTextColor.DARK_GRAY)
						.decoration(TextDecoration.ITALIC, false));
				lore.add(
					Component.text("  members to managers", NamedTextColor.DARK_GRAY)
						.decoration(TextDecoration.ITALIC, false));
				lore.add(
					Component.text("- May promote but not demote", NamedTextColor.DARK_GRAY)
						.decoration(TextDecoration.ITALIC, false));
				lore.add(
					Component.text("  members to founders", NamedTextColor.DARK_GRAY)
						.decoration(TextDecoration.ITALIC, false));
				// fall through
			case MANAGER:
				lore.add(
					Component.text("Managers and up may:", NamedTextColor.AQUA)
						.decoration(TextDecoration.ITALIC, false));
				lore.add(
					Component.text("- May add and remove", NamedTextColor.AQUA)
						.decoration(TextDecoration.ITALIC, false));
				lore.add(
					Component.text("  guests and members", NamedTextColor.AQUA)
						.decoration(TextDecoration.ITALIC, false));
				lore.add(
					Component.text("- May lock the guild until", NamedTextColor.AQUA)
						.decoration(TextDecoration.ITALIC, false));
				lore.add(
					Component.text("  a moderator is available", NamedTextColor.AQUA)
						.decoration(TextDecoration.ITALIC, false));
				// fall through
			case MEMBER:
				lore.add(
					Component.text("Members and up may:", NamedTextColor.GOLD)
						.decoration(TextDecoration.ITALIC, false));
				lore.add(
					Component.text("- May talk in guild chat", NamedTextColor.GOLD)
						.decoration(TextDecoration.ITALIC, false));
				// fall through
			case GUEST:
				lore.add(
					Component.text("Guests and up may:", NamedTextColor.GRAY)
						.decoration(TextDecoration.ITALIC, false));
				lore.add(
					Component.text("- May visit the guild", NamedTextColor.GRAY)
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

	private void setInviteHeaderItem(int pageY, GuildInviteLevel inviteLevel) {
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

	private void setPlayerIcon(int x, int y, int relativeIndex, List<String> players) {
		ItemStack item;
		ItemMeta meta;

		if (relativeIndex >= players.size()) {
			return;
		}
		String playerName = players.get(relativeIndex);

		boolean nameIsUnknown;
		UUID playerUuid;
		try {
			playerUuid = UUID.fromString(playerName);
			nameIsUnknown = true;
		} catch (IllegalArgumentException unused) {
			playerUuid = MonumentaRedisSyncIntegration.cachedNameToUuid(playerName);
			nameIsUnknown = false;
		}
		if (playerUuid == null) {
			item = new ItemStack(Material.BARRIER);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Could not look up UUID for " + playerName, NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			mGui.setItem(y + PAGE_START_Y, x + PAGE_START_X, item);
			return;
		}

		item = new ItemStack(Material.PLAYER_HEAD);
		meta = item.getItemMeta();
		if (meta instanceof SkullMeta skullMeta) {
			if (nameIsUnknown) {
				skullMeta.setPlayerProfile(Bukkit.createProfile(playerUuid, playerUuid.toString()));
			} else {
				skullMeta.setPlayerProfile(Bukkit.createProfile(playerUuid, playerName));
			}
		}
		item.setItemMeta(meta);
		mGui.setItem(PAGE_START_Y + y, PAGE_START_X + x, item);
	}
}
