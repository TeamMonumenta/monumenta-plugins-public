package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;

import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.LuckPermsApi;

public class LuckPermsIntegration {
	public LuckPermsIntegration(Plugin plugin) {
		LuckPermsApi lp = LuckPerms.getApi();

		CreateGuild.register(plugin, lp);
		JoinGuild.register(plugin, lp);
		PromoteGuild.register(plugin, lp);
		LeaveGuild.register(plugin, lp);
	}
}
