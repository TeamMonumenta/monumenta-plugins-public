package com.playmonumenta.plugins.integrations.luckperms.guildgui;

import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.integrations.luckperms.PlayerGuildInfo;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;

public class AllGuildsView extends View {
	protected static final int PAGE_START_X = 0;
	protected static final int PAGE_START_Y = 1;
	protected static final int PAGE_WIDTH = 9;

	private final List<PlayerGuildInfo> mAllGuilds = new ArrayList<>();
	private GuildOrder mOrder;

	public AllGuildsView(GuildGui gui, GuildOrder order) {
		super(gui);
		mOrder = order;
	}

	@Override
	public void setup() {
		int totalRows = Math.floorDiv((mAllGuilds.size() + PAGE_WIDTH - 1), PAGE_WIDTH);
		mGui.setPageArrows(totalRows);

		mGui.setTitle(Component.text("All Guilds"));

		int index = 0;
		for (int y = 0; y < GuildGui.PAGE_HEIGHT; y++) {
			if (index >= mAllGuilds.size()) {
				break;
			}

			for (int x = 0; x < PAGE_WIDTH; x++) {
				index = (mGui.mPage * GuildGui.PAGE_HEIGHT + y) * PAGE_WIDTH + x;
				if (index >= mAllGuilds.size()) {
					break;
				}

				mGui.setGuildIcon(PAGE_START_Y + y,
					PAGE_START_X + x,
					mAllGuilds.get(index));
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
			try {
				User user = LuckPermsIntegration.loadUser(mGui.mTargetUuid).join();

				List<PlayerGuildInfo> guilds = mOrder.sortGuilds(
					PlayerGuildInfo.ofCollection(
						user,
						LuckPermsIntegration.getGuilds(true, false).join()
					).join()
				).join();

				Bukkit.getScheduler().runTask(mGui.mMainPlugin, () -> {
					// Handle this list sync so that it can't be modified during reads
					mAllGuilds.clear();
					mAllGuilds.addAll(guilds);
					mGui.update();
				});
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(mGui.mMainPlugin, () -> {
					mGui.mPlayer.sendMessage(Component.text("An error occurred fetching all guilds:", NamedTextColor.RED));
					MessagingUtils.sendStackTrace(mGui.mPlayer, ex);
				});
			}
		});
	}
}
