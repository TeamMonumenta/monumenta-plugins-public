package com.playmonumenta.plugins.integrations.luckperms.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.group.GroupCreateEvent;
import net.luckperms.api.event.group.GroupDeleteEvent;
import net.luckperms.api.event.sync.PostSyncEvent;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;

public class LPArguments {
	private static final ConcurrentSkipListSet<String> mGroupIds = new ConcurrentSkipListSet<>();

	public static final ArgumentSuggestions GROUP_SUGGESTIONS = ArgumentSuggestions
		.stringCollection(suggestionInfo -> CommandUtils.quoteIfNeeded(new ArrayList<>(mGroupIds)));

	public static void registerLuckPermsEvents(Plugin plugin, EventBus eventBus) {
		eventBus.subscribe(plugin, PostSyncEvent.class, LPArguments::postSyncEvent);

		refreshGroups();
	}

	// Methods for use in commands
	public static List<String> getGroupIds() {
		return StringUtils.sortedStrings(mGroupIds);
	}

	// Local events
	public static void groupCreateEvent(GroupCreateEvent event) {
		mGroupIds.add(event.getGroup().getName());
	}

	public static void groupDeleteEvent(GroupDeleteEvent event) {
		mGroupIds.remove(event.getGroupName());
	}

	// Remote events
	public static void postSyncEvent(PostSyncEvent event) {
		refreshGroups();
	}

	// Helper methods
	public static void refreshGroups() {
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			mGroupIds.clear();

			try {
				LuckPermsIntegration.loadAllGroups().join();
			} catch (Exception ex) {
				MMLog.warning("[LPArguments] Failed to load all groups:");
				MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
			}

			for (Group group : LuckPermsIntegration.getLoadedGroups()) {
				mGroupIds.add(group.getName());
			}
		});
	}
}
