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
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
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
		//<playername> <new access>
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
					String stringUser = args.getUnchecked("player");

					UUID targetUUID = MonumentaRedisSyncIntegration.cachedNameToUuid(stringUser);
					if (targetUUID == null) {
						throw CommandAPI.failWithString("Given target does not exist in Redis.");
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

					//If you want to use operator "mode" use the mod-only version
					getUserFromUUID(plugin, targetUUID)
						.exceptionally((throwable) -> {
							Bukkit.getScheduler().runTask(plugin, () -> {
								if (throwable instanceof WrapperCommandSyntaxException wrapperCSE) {
									sender.sendMessage(Component.text(wrapperCSE.getMessage(), NamedTextColor.RED));
								} else if (throwable != null) {
									MMLog.warning("Caught unexpected Exception when loading luckperms User", throwable);
								}
							});
							return null;
						})
						.thenAccept((user) -> {
							if (user == null) {
								//already handled
								return;
							}
							Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> run(plugin, sender, guild, player, user, accessLevel, false));
						});
				} else {
					callee.sendMessage(Component.text("This command may only be run by a player", NamedTextColor.RED));
				}
			});
	}

	public static CommandAPICommand attachModOnly(Plugin plugin, CommandAPICommand root, GuildAccessLevel accessLevel) {
		//<guild> <playername> <new_access>

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

				String stringUser = args.getUnchecked("player");

				UUID targetUUID = MonumentaRedisSyncIntegration.cachedNameToUuid(stringUser);
				if (targetUUID == null) {
					throw CommandAPI.failWithString("Given target does not exist in Redis.");
				}

				String guildRoot = GuildArguments.getIdFromName(guildName);
				if (guildRoot == null) {
					throw CommandAPI.failWithString("Could not identify guild by name: " + guildName);
				}

				CompletableFuture<Group> guildFuture = getGuildFromName(plugin, guildRoot);
				guildFuture.exceptionally(throwable -> {
					Bukkit.getScheduler().runTask(plugin, () -> GuildAccessCommand.handleError(throwable, player));
					return null;
				});

				getUserFromUUID(plugin, targetUUID)
					.exceptionally((throwable) -> {
						Bukkit.getScheduler().runTask(plugin, () -> GuildAccessCommand.handleError(throwable, player));
						return null;
					})
					.thenAcceptBoth(guildFuture, (user, group) -> {
						if (user == null || group == null) {
							//already handled
							return;
						}
						Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> run(plugin, sender, group, player, user, accessLevel, true));
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
				//failed
				result.completeExceptionally(CommandAPI.failWithString("Could not get user from LuckPerms"));
			} else {
				result.complete(targetUser);
			}
		});

		return result;
	}

	protected static void run(Plugin plugin, CommandSender sender, Group guildRoot, Player player, User target, GuildAccessLevel targetAccess, boolean isOperator) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			User agent = LuckPermsIntegration.getUser(player);

			GuildAccessLevel agentAccess = LuckPermsIntegration.getAccessLevel(guildRoot, agent);
			GuildAccessLevel targetCurrentAccess = LuckPermsIntegration.getAccessLevel(guildRoot, target);

			boolean canAgentSetPermission =
				(agentAccess.ordinal() < targetCurrentAccess.ordinal() &&
					agentAccess.ordinal() <= targetAccess.ordinal() &&
					agentAccess.ordinal() <= GuildAccessLevel.MANAGER.ordinal()) || //Apply on other

					(player.getUniqueId().equals(target.getUniqueId()) &&
						agentAccess.ordinal() < targetAccess.ordinal()) || //Apply on self

					isOperator; //Operator bypass

			if (!canAgentSetPermission) {
				Bukkit.getScheduler().runTask(plugin, ()
					-> sender.sendMessage(Component.text(String.format("You cannot change %s's permission from %s to %s", target.getUsername(), targetCurrentAccess.mLabel, targetAccess.mLabel), NamedTextColor.RED)));
				return;
			}

			if (targetAccess != GuildAccessLevel.NONE) {
				Group targetAccessGroup = targetAccess.loadGroupFromRoot(guildRoot).join().orElse(null);
				if (targetAccessGroup == null) {
					Bukkit.getScheduler().runTask(plugin, ()
						-> sender.sendMessage(Component.text(String.format("Could not find group %s", targetAccess.groupNameFromRoot(guildRoot)), NamedTextColor.RED)));
					return;
				}

				if (targetCurrentAccess.ordinal() > GuildAccessLevel.MEMBER.ordinal() && !isOperator) {
					Bukkit.getScheduler().runTask(plugin, ()
						-> sender.sendMessage(Component.text("If you want to invite someone to your guild use ", NamedTextColor.RED).append(Component.text("/guild invite", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/guild invite ")))));
					return;
				}
			}

			//access removal
			String targetAccessGroupId = targetCurrentAccess.groupNameFromRoot(guildRoot);
			NodeMap targetData = target.data();
			for (Node node : targetData.toCollection()) {
				if (!(node instanceof InheritanceNode inheritanceNode)) {
					continue;
				}

				if (inheritanceNode.getGroupName().equals(targetAccessGroupId)) {
					targetData.remove(node);
					for (GuildPermission guildPermission : GuildPermission.values()) {
						guildPermission.setExplicitPermission(guildRoot, target, null);
					}
				}
			}

			if (targetAccess != GuildAccessLevel.NONE) {
				//add access.
				targetData.add(InheritanceNode.builder(targetAccess.groupNameFromRoot(guildRoot)).build());
			}
			//broadcast message to guild chat saying that user's access got changed?

			LuckPermsIntegration.pushUserUpdate(target);
			AuditListener.log(
				String.format("Changed guild %s access for %s from %s to %s\nTask executed by %s",
					guildRoot.getDisplayName() != null ? guildRoot.getDisplayName() : guildRoot.getName(), // needed because getDisplayName returns null if it is the same as getName
					target.getUsername() != null ? target.getUsername() : target.getUniqueId().toString(), // usb: this can return null if luckperms doesn't have a username associated with this uuid
					targetCurrentAccess.name(),
					targetAccess.name(),
					isOperator ? player.getName() + " (Operator)" : player.getName())
			);

			Bukkit.getScheduler().runTask(plugin, () -> {
				if (GuildAccessLevel.NONE.equals(targetAccess)) {
					sender.sendMessage(Component.text(String.format("Successfully removed %s from the guild", target.getUsername()), NamedTextColor.GOLD));
					return;
				}
				boolean promoted = targetAccess.ordinal() < targetCurrentAccess.ordinal();
				sender.sendMessage(Component.text(String.format("Successfully %s %s to %s", (promoted ? "promoted" : "demoted"), target.getUsername(), targetAccess.mLabel), NamedTextColor.GOLD));
			});
		});
	}
}
