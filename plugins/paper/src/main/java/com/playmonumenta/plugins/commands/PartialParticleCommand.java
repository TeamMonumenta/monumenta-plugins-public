package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.StringUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class PartialParticleCommand {

	private static final UUID CONSOLE_UUID = UUID.randomUUID();
	private static final Map<UUID, BukkitTask> mPlayerLoggingTasks = new HashMap<>();

	public static void register() {

		new CommandAPICommand("partialparticle")
			.withPermission("monumenta.command.partialparticle")
			.withSubcommand(new CommandAPICommand("logging")
				                .withArguments(new LiteralArgument("start"))
				                .executes((sender, args) -> {
					                start(sender, 20);
				                }))
			.withSubcommand(new CommandAPICommand("logging")
				                .withArguments(new LiteralArgument("start"),
					                new DoubleArgument("period in seconds", 0.05, 60))
				                .executes((sender, args) -> {
					                start(sender, Math.max(1, (int) (20 * (double) args.get("period in seconds"))));
				                }))
			.withSubcommand(new CommandAPICommand("logging")
				                .withArguments(new LiteralArgument("stop"))
				                .executes((sender, args) -> {
					                UUID uuid = sender instanceof Player player ? player.getUniqueId() : CONSOLE_UUID;
					                BukkitTask task = mPlayerLoggingTasks.remove(uuid);
					                if (task != null) {
						                task.cancel();
						                sender.sendMessage(Component.text("PartialParticle logging stopped", NamedTextColor.YELLOW));
					                } else {
						                sender.sendMessage(Component.text("PartialParticle logging is not active", NamedTextColor.GRAY));
					                }
				                }))
			.register();

	}

	private static void start(CommandSender sender, int period) {
		UUID uuid = sender instanceof Player player ? player.getUniqueId() : CONSOLE_UUID;
		BukkitTask oldTask = mPlayerLoggingTasks.remove(uuid);
		if (oldTask != null) {
			oldTask.cancel();
		}
		mPlayerLoggingTasks.put(uuid, new BukkitRunnable() {
			long mLastCount = PartialParticle.getSpawnedParticles();

			@Override
			public void run() {
				if (!uuid.equals(CONSOLE_UUID) && Bukkit.getPlayer(uuid) == null) {
					cancel();
					BukkitTask activeTask = mPlayerLoggingTasks.get(uuid);
					if (activeTask != null && activeTask.getTaskId() == this.getTaskId()) {
						mPlayerLoggingTasks.remove(uuid);
					}
					return;
				}
				long newCount = PartialParticle.getSpawnedParticles();
				if (newCount > mLastCount) {
					sender.sendMessage(Component.text("Spawned ", NamedTextColor.WHITE)
						                   .append(Component.text(newCount - mLastCount, NamedTextColor.GOLD))
						                   .append(Component.text(" particles in the last " + StringUtils.ticksToSeconds(period) + "s", NamedTextColor.WHITE)));
				}
				mLastCount = newCount;
			}
		}.runTaskTimer(Plugin.getInstance(), period, period));
		sender.sendMessage(Component.text("PartialParticle logging started with a period of " + StringUtils.ticksToSeconds(period) + "s", NamedTextColor.YELLOW));
	}

}
