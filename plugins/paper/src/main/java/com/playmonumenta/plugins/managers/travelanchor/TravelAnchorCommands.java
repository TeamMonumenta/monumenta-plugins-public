package com.playmonumenta.plugins.managers.travelanchor;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.GuildPlotUtils;
import com.playmonumenta.plugins.managers.travelanchor.gui.AnchorGroupGui;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;

public class TravelAnchorCommands {
	public static Argument<Entity> ANCHOR_ARG = new EntitySelectorArgument.OneEntity("anchor");

	public static void register() {
		new CommandAPICommand("travelanchor")
			.withPermission("monumenta.command.travelanchor")
			.withSubcommand(getAddSubcommand())
			.withSubcommand(getGroupsSubcommand())
			.withSubcommand(getRemoveSubcommand())
			.withSubcommand(getRenameSubcommand())
			.withSubcommand(getTravelSubcommand())
			.register();
	}

	public static CommandAPICommand getAddSubcommand() {
		return new CommandAPICommand("add")
			.withArguments(ANCHOR_ARG)
			.executes((sender, args) -> {
				if (sender instanceof Player player) {
					if (GuildPlotUtils.guildPlotEditTravelAnchorBlocked(player)) {
						player.sendMessage(GuildPlotUtils.NO_TRAVEL_ANCHOR_ACCESS_COMPONENT);
						return;
					}
				}
				Entity anchor = Objects.requireNonNull(args.getByArgument(ANCHOR_ARG));
				TravelAnchorManager.getInstance().newAnchor(anchor);
			});
	}

	public static CommandAPICommand getRemoveSubcommand() {
		return new CommandAPICommand("remove")
			.withArguments(ANCHOR_ARG)
			.executes((sender, args) -> {
				if (sender instanceof Player player) {
					if (GuildPlotUtils.guildPlotEditTravelAnchorBlocked(player)) {
						player.sendMessage(GuildPlotUtils.NO_TRAVEL_ANCHOR_ACCESS_COMPONENT);
						return;
					}
				}
				Entity anchor = Objects.requireNonNull(args.getByArgument(ANCHOR_ARG));
				TravelAnchorManager.getInstance().removeAnchor(anchor);
			});
	}

	public static CommandAPICommand getRenameSubcommand() {
		return new CommandAPICommand("rename")
			.withArguments(ANCHOR_ARG)
			.executes((sender, args) -> {
				if (!(CommandUtils.getCallee(sender) instanceof Player player)) {
					sender.sendMessage(Component.text("This command must be run as a player.", NamedTextColor.RED));
					return;
				}

				if (GuildPlotUtils.guildPlotEditTravelAnchorBlocked(player)) {
					player.sendMessage(GuildPlotUtils.NO_TRAVEL_ANCHOR_ACCESS_COMPONENT);
					return;
				}

				Entity anchorEntity = Objects.requireNonNull(args.getByArgument(ANCHOR_ARG));
				EntityTravelAnchor anchor = TravelAnchorManager
					.getInstance()
					.anchorsInWorld(anchorEntity.getWorld())
					.getAnchor(anchorEntity);
				if (anchor == null) {
					sender.sendMessage(Component.text("That entity is not a travel anchor right now.", NamedTextColor.RED));
					return;
				}

				String oldLabel = anchor.label();
				int spaceIndex = getSpaceIndex(oldLabel);
				SignUtils
					.newMenu(List.of(
						oldLabel.substring(0, spaceIndex).trim(),
						oldLabel.substring(spaceIndex).trim(),
						"~~~~~~~~~~~",
						"Enter new name"
					))
					.reopenIfFail(false)
					.response(
						(p, text) -> {
							String label = (
								StringUtils.substring(text[0], 0, 24).trim()
									+ " "
									+ StringUtils.substring(text[1], 0, 24).trim()
							).trim();
							anchor.label(label);
							return true;
						}
					)
					.open(player);
			});
	}

	public static CommandAPICommand getGroupsSubcommand() {
		return new CommandAPICommand("groups")
			.withArguments(ANCHOR_ARG)
			.executes((sender, args) -> {
				if (!(CommandUtils.getCallee(sender) instanceof Player player)) {
					sender.sendMessage(Component.text("This command must be run as a player.", NamedTextColor.RED));
					return;
				}

				if (GuildPlotUtils.guildPlotEditTravelAnchorBlocked(player)) {
					player.sendMessage(GuildPlotUtils.NO_TRAVEL_ANCHOR_ACCESS_COMPONENT);
					return;
				}

				Entity anchorEntity = Objects.requireNonNull(args.getByArgument(ANCHOR_ARG));
				EntityTravelAnchor anchor = TravelAnchorManager
					.getInstance()
					.anchorsInWorld(anchorEntity.getWorld())
					.getAnchor(anchorEntity);
				if (anchor == null) {
					sender.sendMessage(Component.text("That entity is not a travel anchor right now.", NamedTextColor.RED));
					return;
				}

				new AnchorGroupGui(player, anchor).open();
			});
	}

	public static CommandAPICommand getTravelSubcommand() {
		return new CommandAPICommand("travel")
			.withArguments(ANCHOR_ARG)
			.executes((sender, args) -> {
				if (!(CommandUtils.getCallee(sender) instanceof Player player)) {
					sender.sendMessage(Component.text("This command must be run as a player.", NamedTextColor.RED));
					return;
				}

				if (GuildPlotUtils.guildPlotUseTravelAnchorBlocked(player)) {
					player.sendMessage(GuildPlotUtils.NO_TRAVEL_ANCHOR_ACCESS_COMPONENT);
					return;
				}

				if (
					player.getVehicle() instanceof Interaction interaction &&
						interaction.getScoreboardTags().contains(TravelUi.TRAVEL_ANCHOR_UI_SEAT_TAG)
				) {
					// Already teleporting
					return;
				}

				Entity anchorEntity = Objects.requireNonNull(args.getByArgument(ANCHOR_ARG));
				EntityTravelAnchor anchor = TravelAnchorManager
					.getInstance()
					.anchorsInWorld(anchorEntity.getWorld())
					.getAnchor(anchorEntity);

				if (anchor == null) {
					sender.sendMessage(Component.text("That entity is not a travel anchor right now.", NamedTextColor.RED));
					return;
				}

				new TravelUi(player, anchor)
					.runTaskTimer(Plugin.getInstance(), 0L, 1L);
			});
	}

	private static int getSpaceIndex(String existingName) {
		int spaceIndex1 = existingName.indexOf(' ', existingName.length() / 2);
		int spaceIndex2 = existingName.lastIndexOf(' ', existingName.length() / 2);
		int spaceIndex = spaceIndex2 < 0 || (spaceIndex1 >= 0 && Math.abs(spaceIndex1 - existingName.length() / 2) < Math.abs(spaceIndex2 - existingName.length() / 2))
			? spaceIndex1 : spaceIndex2;
		if (spaceIndex < 0) {
			spaceIndex = existingName.length();
		}
		return spaceIndex;
	}
}
