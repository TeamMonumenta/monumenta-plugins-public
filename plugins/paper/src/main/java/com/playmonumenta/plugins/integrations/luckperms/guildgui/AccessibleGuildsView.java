package com.playmonumenta.plugins.integrations.luckperms.guildgui;

import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.integrations.luckperms.PlayerGuildInfo;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;

public class AccessibleGuildsView extends View {
	protected static final int PAGE_START_X = 0;
	protected static final int PAGE_START_Y = 1;
	protected static final int PAGE_WIDTH = 9;

	private final List<PlayerGuildInfo> mAccessibleGuilds = new ArrayList<>();
	private GuildOrder mOrder;

	public AccessibleGuildsView(GuildGui gui, GuildOrder order) {
		super(gui);
		mOrder = order;
	}

	@Override
	public void setup() {
		int totalRows = Math.floorDiv((mAccessibleGuilds.size() + PAGE_WIDTH - 1), PAGE_WIDTH);
		setPageArrows(totalRows);

		mGui.setTitle(Component.text("Your Guilds"));

		int index = 0;
		for (int y = 0; y < GuildGui.PAGE_HEIGHT; y++) {
			if (index >= mAccessibleGuilds.size()) {
				break;
			}

			for (int x = 0; x < PAGE_WIDTH; x++) {
				index = (mPage * GuildGui.PAGE_HEIGHT + y) * PAGE_WIDTH + x;
				if (index >= mAccessibleGuilds.size()) {
					break;
				}

				mGui.setGuildIcon(PAGE_START_Y + y,
					PAGE_START_X + x,
					mAccessibleGuilds.get(index));
			}
		}
	}

	public void changeOrder(GuildOrder order) {
		mOrder = order;
		refresh();
	}

	@Override
	public void refresh() {
		super.refresh();

		Bukkit.getScheduler().runTaskAsynchronously(mGui.mMainPlugin, () -> {
			User user = LuckPermsIntegration.loadUser(mGui.mTargetUuid).join();

			List<PlayerGuildInfo> guilds = mOrder.sortGuilds(
				PlayerGuildInfo.ofCollection(
					user,
					LuckPermsIntegration.getRelevantGuilds(user, true, true)
				).join()
			).join();

			Group mainGuild = LuckPermsIntegration.getGuild(user);
			if (mainGuild != null) {
				PlayerGuildInfo mainGuildInfo = PlayerGuildInfo.of(user, mainGuild).join();
				if (mainGuildInfo != null) {
					guilds.remove(mainGuildInfo);
					guilds.add(0, mainGuildInfo);
				}
			}

			Bukkit.getScheduler().runTask(mGui.mMainPlugin, () -> {
				// Handle this list sync so that it can't be modified during reads
				mAccessibleGuilds.clear();
				mAccessibleGuilds.addAll(guilds);
				mGui.update();
			});
		});
	}
}
