package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.networkchat.commands.ChatCommand;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.MMLog;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

public class GuildAccessCommand {
	private static final Argument<?> USER_ARG = new StringArgument("player").replaceSuggestions(ChatCommand.ALL_CACHED_PLAYER_NAMES_SUGGESTIONS);

	private static final CommandPermission PERMISSION = CommandPermission.fromString("monumenta.command.guild.access");
	private static final CommandPermission PERMISSION_MOD = CommandPermission.fromString("monumenta.command.guild.mod.access");

	public static CommandAPICommand attach(Plugin plugin, CommandAPICommand root, GuildAccessLevel accessLevel) {
		// <playername> <new access>
		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(USER_ARG);
		arguments.add(new LiteralArgument(accessLevel.mArgument));

		return root
			.withArguments(arguments)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, PERMISSION);
				CommandSender callee = sender;
				if (GuildCommand.senderCannotRunCommand(callee, false)) {
					throw CommandAPI.failWithString("You cannot run this command on 'build'");
				}

				if (callee instanceof ProxiedCommandSender proxied) {
					callee = proxied.getCallee();
				}
				if (callee instanceof Player player) {
					String targetName = args.getUnchecked("player");

					UUID targetUUID = MonumentaRedisSyncIntegration.cachedNameToUuid(targetName);
					if (targetUUID == null) {
						throw CommandAPI.failWithString("Could not identify that player.");
					}

					Group accessedGroup = LuckPermsIntegration.getGuild(player);
					if (accessedGroup == null) {
						throw CommandAPI.failWithString("You are not in a guild");
					}
					Group guild = LuckPermsIntegration.getGuildRoot(accessedGroup);
					if (guild == null) {
						throw CommandAPI.failWithString("You are not in a guild");
					}
					if (LuckPermsIntegration.isLocked(guild)) {
						player.sendMessage(Component.text("That guild is on lockdown; you may not interact with it at this time.", NamedTextColor.RED));
						player.playSound(player,
							Sound.BLOCK_IRON_DOOR_CLOSE,
							SoundCategory.PLAYERS,
							0.7f,
							Constants.Note.FS3.mPitch);
						return;
					}

					// If you want to use operator "mode" use the mod-only version
					getUserFromUUID(plugin, targetUUID)
						.exceptionally((throwable) -> {
							Bukkit.getScheduler().runTask(plugin, () -> {
								if (throwable instanceof WrapperCommandSyntaxException wrapperCSE) {
									sender.sendMessage(Component.text(wrapperCSE.getMessage(), NamedTextColor.RED));
								} else if (throwable != null) {
									MMLog.warning("Caught unexpected exception when loading luckperms user", throwable);
								}
							});
							return null;
						})
						.thenAccept((targetUser) -> {
							if (targetUser == null) {
								// Already handled
								return;
							}
							Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> run(plugin, sender, guild, player, targetName, targetUser, accessLevel, false));
						});
				} else {
					callee.sendMessage(Component.text("This command may only be run by a player", NamedTextColor.RED));
				}
			});
	}

	public static CommandAPICommand attachModOnly(Plugin plugin, CommandAPICommand root, GuildAccessLevel accessLevel) {
		// <guild> <playername> <new_access>

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(GuildCommand.GUILD_NAME_ARG);
		arguments.add(USER_ARG);
		arguments.add(new LiteralArgument(accessLevel.mArgument));

		return root
			.withArguments(arguments)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, PERMISSION_MOD);
				CommandSender callee = sender;
				if (GuildCommand.senderCannotRunCommand(callee, true)) {
					throw CommandAPI.failWithString("You cannot run this command.");
				}

				if (callee instanceof ProxiedCommandSender proxied) {
					callee = proxied.getCallee();
				}
				if (!(callee instanceof Player player)) {
					throw CommandAPI.failWithString("This command may only be run by a player");
				}

				String guildName = args.getUnchecked("guild name");

				String targetName = args.getUnchecked("player");

				UUID targetUUID = MonumentaRedisSyncIntegration.cachedNameToUuid(targetName);
				if (targetUUID == null) {
					throw CommandAPI.failWithString("Could not identify that player.");
				}

				String guildRoot = GuildArguments.getIdFromName(guildName);
				if (guildRoot == null) {
					throw CommandAPI.failWithString("Could not identify guild by name: " + guildName);
				}

				CompletableFuture<Group> guildFuture = getGuildFromName(plugin, guildRoot);
				guildFuture.exceptionally(throwable -> {
					Bukkit.getScheduler().runTask(plugin, () -> handleError(throwable, player));
					return null;
				});

				getUserFromUUID(plugin, targetUUID)
					.exceptionally((throwable) -> {
						Bukkit.getScheduler().runTask(plugin, () -> handleError(throwable, player));
						return null;
					})
					.thenAcceptBoth(guildFuture, (targetUser, group) -> {
						if (targetUser == null || group == null) {
							// Already handled
							return;
						}
						Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> run(plugin, sender, group, player, targetName, targetUser, accessLevel, true));
					});
			});
	}

	private static void handleError(Throwable thrown, CommandSender sender) {
		if (thrown instanceof WrapperCommandSyntaxException wrapperCSE) {
			sender.sendMessage(Component.text(wrapperCSE.getMessage(), NamedTextColor.RED));
		} else if (thrown != null) {
			MMLog.warning("Caught unexpected Exception during loading", thrown);
		}
	}

	private static CompletableFuture<Group> getGuildFromName(Plugin plugin, String guildRoot) {
		CompletableFuture<Group> result = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			Group guildRootGroup = LuckPermsIntegration.GM.loadGroup(guildRoot).join()
				.orElse(null);
			if (guildRootGroup == null) {
				result.completeExceptionally(CommandAPI.failWithString("Could not find Guild"));
				return;
			}
			result.complete(guildRootGroup);
		});
		return result;
	}

	private static CompletableFuture<User> getUserFromUUID(Plugin plugin, UUID uuid) {
		CompletableFuture<User> result = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			User targetUser = LuckPermsIntegration.loadUser(uuid).join();
			if (targetUser == null) {
				// Failed
				result.completeExceptionally(CommandAPI.failWithString("Could not get user from LuckPerms"));
			} else {
				result.complete(targetUser);
			}
		});

		return result;
	}

	protected static void run(
		Plugin plugin,
		CommandSender sender,
		Group guildRoot,
		Player player,
		String targetName,
		User target,
		GuildAccessLevel targetAccess,
		boolean isOperator
	) {
		User agent = LuckPermsIntegration.getUser(player);

		GuildAccessLevel agentAccess = LuckPermsIntegration.getAccessLevel(guildRoot, agent);
		GuildAccessLevel targetCurrentAccess = LuckPermsIntegration.getAccessLevel(guildRoot, target);

		// Operators can set any access level
		if (!isOperator) {
			if (!GuildPermission.MANAGE_MEMBERSHIP.hasAccess(guildRoot, agent)) {
				Bukkit.getScheduler().runTask(plugin, () ->
					sender.sendMessage(Component.text("You do not have permission to manage guild access in your guild.", NamedTextColor.RED))
				);
				return;
			}

			if (targetCurrentAccess.equals(targetAccess)) {
				Bukkit.getScheduler().runTask(plugin, () ->
					sender.sendMessage(Component.text("That would not change anything.", NamedTextColor.RED))
				);
				return;
			}

			if (targetCurrentAccess.equals(GuildAccessLevel.BLOCKED)) {
				// Blocked players can be unblocked, but must start out as "not in the guild"/kicked
				if (!targetAccess.equals(GuildAccessLevel.NONE)) {
					Bukkit.getScheduler().runTask(plugin, () ->
						sender.sendMessage(
							Component.text("If you wish to unblock someone, ", NamedTextColor.RED)
								.append(
									Component.text("kick them", NamedTextColor.GOLD)
										.hoverEvent(Component.text("Click to put command in chat bar", NamedTextColor.GOLD))
										.clickEvent(ClickEvent.suggestCommand("/guild access " + targetName + " kick"))
								)
								.append(Component.text(" (and optionally "))
								.append(
									Component.text("send them an invite", NamedTextColor.GOLD)
										.hoverEvent(Component.text("Click to put command in chat bar", NamedTextColor.GOLD))
										.clickEvent(ClickEvent.suggestCommand("/guild invite " + targetName + " "))
								)
								.append(Component.text(")."))
						)
					);
					return;
				}
			} else if (targetAccess.compareTo(GuildAccessLevel.MEMBER) <= 0 && targetCurrentAccess.compareTo(GuildAccessLevel.MEMBER) > 0) {
				// Target is either a guest or not in the guild - so they cannot be promoted to member or above
				Bukkit.getScheduler().runTask(plugin, () ->
					sender.sendMessage(
						Component.text("If you want to invite someone to your guild use ", NamedTextColor.RED)
							.append(
								Component.text("/guild invite", NamedTextColor.GOLD)
									.hoverEvent(Component.text("Click to put command in chat bar", NamedTextColor.GOLD))
									.clickEvent(ClickEvent.suggestCommand("/guild invite " + targetName + " member"))
							)
					)
				);
				return;
			} else if (targetAccess.compareTo(GuildAccessLevel.GUEST) <= 0 && targetCurrentAccess.compareTo(GuildAccessLevel.GUEST) > 0) {
				// Target is either not in the guild - so they cannot be promoted to guest or above
				Bukkit.getScheduler().runTask(plugin, () ->
					sender.sendMessage(
						Component.text("If you want to invite someone to your guild use ", NamedTextColor.RED)
							.append(
								Component.text("/guild invite", NamedTextColor.GOLD)
									.hoverEvent(Component.text("Click to put command in chat bar", NamedTextColor.GOLD))
									.clickEvent(ClickEvent.suggestCommand("/guild invite " + targetName + " guest"))
							)
					)
				);
				return;
			} else if (player.getUniqueId().equals(target.getUniqueId())) {
				// Targeting self
				if (agentAccess.compareTo(targetAccess) > 0) {
					Bukkit.getScheduler().runTask(plugin, () ->
						sender.sendMessage(Component.text("You cannot increase your own guild access.", NamedTextColor.RED))
					);
					return;
				}
			} else {
				// Targeting other
				if (agentAccess.compareTo(targetCurrentAccess) >= 0) {
					Bukkit.getScheduler().runTask(plugin, () ->
						sender.sendMessage(Component.text("You modify the access of someone with greater or equal access to yourself.", NamedTextColor.RED))
					);
					return;
				}

				if (agentAccess.compareTo(targetAccess) > 0) {
					Bukkit.getScheduler().runTask(plugin, () ->
						sender.sendMessage(Component.text("You cannot grant someone more access than yourself.", NamedTextColor.RED))
					);
					return;
				}
			}
		}

		if (!targetAccess.equals(GuildAccessLevel.NONE)) {
			Group targetAccessGroup = targetAccess.loadGroupFromRoot(guildRoot).join().orElse(null);
			if (targetAccessGroup == null) {
				Bukkit.getScheduler().runTask(plugin, () ->
					sender.sendMessage(Component.text("Could not find group " + targetAccess.groupNameFromRoot(guildRoot), NamedTextColor.RED))
				);
				return;
			}
		}

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			GuildInviteLevel.setInviteLevel(target, guildRoot, GuildInviteLevel.NONE);
			GuildAccessLevel.setAccessLevel(target, guildRoot, targetAccess);

			LuckPermsIntegration.pushUserUpdate(target);
			Bukkit.getScheduler().runTask(plugin, () -> {
				String guildRootDisplayName = guildRoot.getDisplayName();
				String targetUserName = target.getUsername();
				AuditListener.log(
					String.format("Changed guild %s access for %s from %s to %s\nTask executed by %s",
						guildRootDisplayName != null ? guildRootDisplayName : guildRoot.getName(), // needed because getDisplayName returns null if it is the same as getName
						targetUserName != null ? targetUserName : target.getUniqueId().toString(), // usb: this can return null if luckperms doesn't have a username associated with this uuid
						targetCurrentAccess.name(),
						targetAccess.name(),
						isOperator ? player.getName() + " (Operator)" : player.getName())
				);

				if (GuildAccessLevel.NONE.equals(targetAccess)) {
					sender.sendMessage(Component.text(String.format("Successfully removed %s from the guild", target.getUsername()), NamedTextColor.GOLD));
					return;
				}
				if (targetAccess != targetCurrentAccess) {
					boolean promoted = targetAccess.compareTo(targetCurrentAccess) < 0;
					sender.sendMessage(Component.text(String.format("Successfully %s %s to %s (player's guild permissions have been reset)",
						(promoted ? "promoted" : "demoted"), target.getUsername(), targetAccess.mLabel), NamedTextColor.GOLD));
				}
			});
		});
	}
}
