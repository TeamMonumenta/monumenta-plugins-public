package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.AdventureChatComponentArgument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.Objects;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

public class ChargeUpBarCommand {
	public static String COMMAND = "chargebar";

	public static void register(Plugin plugin) {
		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.chargebar")
			.withArguments(
				new EntitySelectorArgument.OneEntity("entity"),
				new IntegerArgument("startValue"),
				new IntegerArgument("maxValue"),
				new StringArgument("direction").includeSuggestions(
					ArgumentSuggestions.strings("ascending", "descending")
				),
				new StringArgument("style").includeSuggestions(
					ArgumentSuggestions.strings("PROGRESS", "NOTCHED_6", "NOTCHED_10", "NOTCHED_12", "NOTCHED_20")
				),
				new StringArgument("color").includeSuggestions(
					ArgumentSuggestions.strings("PINK", "BLUE", "RED", "GREEN", "YELLOW", "PURPLE", "WHITE")
				),
				new IntegerArgument("range"),
				new AdventureChatComponentArgument("title")
			)
			.executes((sender, args) -> {
				Entity entity = args.getUnchecked("entity");
				int startValue = args.getUnchecked("startValue");
				int maxValue = args.getUnchecked("maxValue");
				String direction = args.getUnchecked("direction");
				String style = args.getUnchecked("style");
				String color = args.getUnchecked("color");
				int range = args.getUnchecked("range");
				Component title = args.getUnchecked("title");

				ChargeUpManager chargeUp = new ChargeUpManager(entity, maxValue, title,
					BossBar.Color.valueOf(color), BossBar.Overlay.valueOf(style), range);

				boolean descending = Objects.equals(direction, "descending");
				chargeUp.setTime(startValue);
				new BukkitRunnable() {
					final boolean mDescending = descending;
					final ChargeUpManager mChargeUp = chargeUp;

					@Override
					public void run() {
						if (mDescending) {
							if (mChargeUp.previousTick()) {
								this.cancel();
							}
						} else if (mChargeUp.nextTick()) {
							this.cancel();
						}
					}
				}.runTaskTimer(plugin, 0, 1);
			})
			.register();
	}
}
