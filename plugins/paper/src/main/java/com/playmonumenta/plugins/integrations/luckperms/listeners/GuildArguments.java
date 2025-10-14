package com.playmonumenta.plugins.integrations.luckperms.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.mail.recipient.GuildRecipient;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.event.sync.PostSyncEvent;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.MetaNode;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_ROOT_NAME_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_ROOT_PLAIN_TAG_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_ROOT_PLOT_MK;

public class GuildArguments {
	private static final Set<String> RELEVANT_META_KEYS = Set.of(
		GUILD_ROOT_NAME_MK,
		GUILD_ROOT_PLAIN_TAG_MK,
		GUILD_ROOT_PLOT_MK
	);

	private static final ConcurrentHashMap<String, String> mIdByName = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, String> mIdByTag = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, String> mNameById = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, String> mTagById = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, Long> mIdToNumber = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Long, GuildRecipient> mRecipientByNumber = new ConcurrentHashMap<>();

	public static final ArgumentSuggestions<CommandSender> NAME_SUGGESTIONS = ArgumentSuggestions
		.stringCollection(suggestionInfo -> {
			List<String> guildNames = getGuildNames();
			if (guildNames.isEmpty()) {
				guildNames = List.of("Example Guild");
			}
			return CommandUtils.alwaysQuote(guildNames);
		});
	public static final ArgumentSuggestions<CommandSender> TAG_SUGGESTIONS = ArgumentSuggestions
		.stringCollection(suggestionInfo -> {
			List<String> guildTags = getGuildTags();
			if (guildTags.isEmpty()) {
				guildTags = List.of("TAG");
			}
			return CommandUtils.alwaysQuote(guildTags);
		});

	@SuppressWarnings("resource")
	public static void registerLuckPermsEvents(Plugin plugin, EventBus eventBus) {
		eventBus.subscribe(plugin, NodeAddEvent.class, GuildArguments::nodeAddEvent);
		eventBus.subscribe(plugin, NodeRemoveEvent.class, GuildArguments::nodeRemoveEvent);
		eventBus.subscribe(plugin, PostSyncEvent.class, GuildArguments::postSyncEvent);

		refreshGuilds();
	}

	// Methods for use in commands
	public static List<String> getGuildIds() {
		return StringUtils.sortedStrings(mNameById.keySet());
	}

	public static List<String> getGuildNames() {
		return StringUtils.sortedStrings(mIdByName.keySet());
	}

	public static List<String> getGuildTags() {
		return StringUtils.sortedStrings(mIdByTag.keySet());
	}

	public static @Nullable String getIdFromName(@Nullable String name) {
		if (name == null) {
			return null;
		}
		return mIdByName.get(name);
	}

	public static @Nullable String getIdFromTag(@Nullable String tag) {
		if (tag == null) {
			return null;
		}
		return mIdByTag.get(tag);
	}

	public static @Nullable String getNameFromId(@Nullable String id) {
		if (id == null) {
			return null;
		}
		return mNameById.get(id);
	}

	public static @Nullable String getNameFromTag(@Nullable String tag) {
		return getNameFromId(getIdFromTag(tag));
	}

	public static @Nullable String getTagFromId(@Nullable String id) {
		if (id == null) {
			return null;
		}
		return mTagById.get(id);
	}

	public static @Nullable String getTagFromName(@Nullable String name) {
		return getTagFromId(getIdFromName(name));
	}

	public static GuildRecipient getRecipientFromNumber(long guildPlotNumber) {
		return mRecipientByNumber.computeIfAbsent(guildPlotNumber, k -> new GuildRecipient(k, null));
	}

	// Local events
	public static void nodeAddEvent(NodeAddEvent event) {
		PermissionHolder permissionHolder = event.getTarget();
		if (!(permissionHolder instanceof Group group)) {
			return;
		}

		if (!group.equals(LuckPermsIntegration.getGuildRoot(group))) {
			Long guildPlotNumber = LuckPermsIntegration.getGuildPlotId(group);
			if (guildPlotNumber != null) {
				getRecipientFromNumber(guildPlotNumber).updateGuildRoot(group);
			}
		}

		Node node = event.getNode();
		if (!(node instanceof MetaNode metaNode)) {
			return;
		}

		if (!RELEVANT_META_KEYS.contains(metaNode.getMetaKey())) {
			return;
		}

		processGuild(group);
	}

	public static void nodeRemoveEvent(NodeRemoveEvent event) {
		PermissionHolder permissionHolder = event.getTarget();
		if (!(permissionHolder instanceof Group group)) {
			return;
		}

		if (!group.equals(LuckPermsIntegration.getGuildRoot(group))) {
			Long guildPlotNumber = LuckPermsIntegration.getGuildPlotId(group);
			if (guildPlotNumber != null) {
				getRecipientFromNumber(guildPlotNumber).updateGuildRoot(group);
			}
		}

		Node node = event.getNode();
		if (!(node instanceof MetaNode metaNode)) {
			return;
		}

		if (!RELEVANT_META_KEYS.contains(metaNode.getMetaKey())) {
			return;
		}

		processGuild(group);
	}

	// Remote events
	public static void postSyncEvent(PostSyncEvent event) {
		refreshGuilds();
	}

	// Helper methods
	public static void refreshGuilds() {
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			List<Group> guilds = LuckPermsIntegration.getGuildRoots().join();
			for (Group guild : guilds) {
				processGuild(guild);
			}
		});
	}

	public static void processGuild(Group group) {
		if (!group.equals(LuckPermsIntegration.getGuildRoot(group))) {
			return;
		}
		String id = group.getName();

		// Remove old info
		String oldName = mNameById.remove(id);
		String oldTag = mTagById.remove(id);
		if (oldName != null) {
			mIdByName.remove(oldName);
		}
		if (oldTag != null) {
			mIdByTag.remove(oldTag);
		}

		// Put new info
		String newName = LuckPermsIntegration.getRawGuildName(group);
		if (newName != null) {
			mNameById.put(id, newName);
			mIdByName.put(newName, id);
		}
		String newTag = LuckPermsIntegration.getGuildPlainTag(group);
		if (newTag != null) {
			mTagById.put(id, newTag);
			mIdByTag.put(newTag, id);
		}

		Long guildPlotNumber = LuckPermsIntegration.getGuildPlotId(group);
		if (guildPlotNumber != null) {
			getRecipientFromNumber(guildPlotNumber).updateGuildRoot(group);
		} else {
			// Guild is being deleted
			Long plotNumber = mIdToNumber.get(id);
			if (plotNumber != null) {
				GuildRecipient guildRecipient = mRecipientByNumber.remove(plotNumber);
				if (guildRecipient != null) {
					guildRecipient.updateGuildRoot(null);
				}
			}
		}
	}
}
