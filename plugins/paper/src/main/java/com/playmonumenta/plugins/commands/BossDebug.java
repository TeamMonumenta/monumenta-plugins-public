package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.bosses.BossManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;

public class BossDebug {
	public static void register() {
		new CommandAPICommand("bossdebug")
			.withPermission(CommandPermission.fromString("monumenta.bossdebug"))
			.executes((sender, args) -> {
				BossManager.getInstance().sendBossDebugInfo(sender);
			})
			.register();
	}
}
