package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.integrations.luckperms.guildgui.GuildOrder;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

// Note: Much of this is copied from GuildGui's AccessibleGuildsView
public class TeleportGuildGui extends Gui {
	private static final int HEADER_Y = 0;
	private static final int PAGE_START_X = 0;
	private static final int PAGE_START_Y = 1;
	private static final int PAGE_HEIGHT = 5;
	private static final int PAGE_WIDTH = 9;

	private final Plugin mMainPlugin;
	private User mPlayerUser;
	private int mPage = 0;
	private List<PlayerGuildInfo> mAccessibleGuilds = new ArrayList<>();
	private GuildOrder mOrder = GuildOrder.DEFAULT;

	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.guild.teleportgui");
		String permsOther = "monumenta.command.guild.teleportgui.other";

		new CommandAPICommand("guild")
			.withArguments(new LiteralArgument("teleportgui"))
			.withArguments(new EntitySelectorArgument.ManyPlayers("Players"))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, perms);
				Collection<Player> players = args.getUnchecked("Players");

				if (!sender.hasPermission(permsOther)) {
					for (Player player : players) {
						if (!player.equals(sender)) {
							throw CommandAPI.failWithString("You do not have permission to run this on other players.");
						}
					}
				}

				for (Player player : players) {
					run(plugin, player);
				}
			})
			.register();
	}

	private static void run(Plugin plugin, Player player) {
		if (!ServerProperties.getShardName().equals("plots")) {
			player.sendMessage(Component.text("The guild teleport GUI can only be run on plots for now."));
			return;
		}

		new TeleportGuildGui(plugin, player).open();
	}

	private TeleportGuildGui(Plugin plugin, Player player) {
		super(player, 54, Component.text("Teleport to Guild"));
		mMainPlugin = plugin;
		mPlayerUser = LuckPermsIntegration.getUser(mPlayer);
		refresh();
	}

	@Override
	protected void setup() {
		setHeader();

		int totalRows = Math.floorDiv((mAccessibleGuilds.size() + PAGE_WIDTH - 1), PAGE_WIDTH);
		setPageArrows(totalRows);

		int index = 0;
		for (int y = 0; y < PAGE_HEIGHT; y++) {
			if (index >= mAccessibleGuilds.size()) {
				break;
			}

			for (int x = 0; x < PAGE_WIDTH; x++) {
				index = (mPage * PAGE_HEIGHT + y) * PAGE_WIDTH + x;
				if (index >= mAccessibleGuilds.size()) {
					break;
				}

				setGuildIcon(PAGE_START_Y + y,
					PAGE_START_X + x,
					mAccessibleGuilds.get(index));
			}
		}
	}

	private void setHeader() {
		ItemStack item;
		ItemMeta meta;
		List<Component> lore;

		List<Component> guildOrderLore = new ArrayList<>();
		guildOrderLore.add(Component.empty());
		guildOrderLore.add(Component.text("Sort order (hotbar keys to select):", NamedTextColor.GRAY)
			.decoration(TextDecoration.ITALIC, false));
		GuildOrder[] orders = GuildOrder.values();
		for (int i = 0; i < 9; i++) {
			if (i >= orders.length) {
				break;
			}

			guildOrderLore.add(
				Component.text("", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false)
					.append(Component.keybind(Constants.Keybind.hotbar(i)))
					.append(Component.text(": " + orders[i].mName)));
		}

		item = new ItemStack(Material.PLAYER_HEAD);
		meta = item.getItemMeta();
		meta.displayName(Component.text("Your Guilds", NamedTextColor.GOLD)
			.decoration(TextDecoration.ITALIC, false));
		lore = new ArrayList<>();
		lore.add(Component.text("Guilds you have access to", NamedTextColor.YELLOW)
			.decoration(TextDecoration.ITALIC, false));
		lore.addAll(guildOrderLore);
		meta.lore(lore);
		if (meta instanceof SkullMeta headMeta) {
			headMeta.setOwningPlayer(Bukkit.getOfflinePlayer(mPlayer.getUniqueId()));
		}
		item.setItemMeta(meta);
		setItem(HEADER_Y, 4, item)
			.onClick((InventoryClickEvent event) -> {
				GuildOrder order;
				if (event.getClick().equals(ClickType.NUMBER_KEY)) {
					GuildOrder[] guildOrders = GuildOrder.values();
					int index = event.getHotbarButton();
					if (index < 0 || index >= guildOrders.length) {
						order = GuildOrder.DEFAULT;
					} else {
						order = guildOrders[index];
					}
					changeOrder(order);
				} else {
					refresh();
				}
			});
	}

	protected void setGuildIcon(int row, int column, PlayerGuildInfo guildInfo) {
		Group guild = guildInfo.getGuild();
		ItemStack item = LuckPermsIntegration.getGuildBanner(guild);
		ItemMeta meta = item.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);

		Component oldDisplayName = meta.displayName();
		Component displayName = Component.text("", NamedTextColor.GOLD, TextDecoration.BOLD)
			.decoration(TextDecoration.ITALIC, false)
			.append(LuckPermsIntegration.getGuildFullComponent(guild));
		if (oldDisplayName != null) {
			displayName = displayName.append(Component.space()).append(oldDisplayName);
		}
		meta.displayName(displayName);

		List<Component> lore = meta.lore();
		if (lore == null) {
			lore = new ArrayList<>();
		}
		if (LuckPermsIntegration.isLocked(guild)) {
			lore.add(Component.text("CURRENTLY ON LOCKDOWN", NamedTextColor.DARK_GRAY, TextDecoration.BOLD));
		}
		meta.lore(lore);

		item.setItemMeta(meta);
		setItem(row, column, new GuiItem(item, false))
			.onClick((InventoryClickEvent event) -> {
				if (!GuildPermission.VISIT.hasAccess(guild, mPlayerUser)) {
					mPlayer.sendMessage(Component.text("You no longer have access to that guild's plot.", NamedTextColor.RED));
					refresh();
					return;
				}

				if (LuckPermsIntegration.isLocked(guild)) {
					if (mPlayer.isOp()) {
						mPlayer.sendMessage(Component.text("That guild is on lockdown; your operator status bypassed this check.", NamedTextColor.RED));
					} else {
						mPlayer.sendMessage(Component.text("That guild is on lockdown; you may not teleport to it at this time.", NamedTextColor.RED));
						mPlayer.playSound(mPlayer,
							Sound.BLOCK_IRON_DOOR_CLOSE,
							SoundCategory.PLAYERS,
							0.7f,
							Constants.Note.FS3.mPitch);
						return;
					}
				}

				World world = mPlayer.getWorld();
				Bukkit.getScheduler().runTaskAsynchronously(mMainPlugin, () -> {
					try {
						Optional<Location> optLoc = LuckPermsIntegration.getGuildTp(world, guild).join();
						Bukkit.getScheduler().runTask(mMainPlugin, () -> {
							if (optLoc.isEmpty()) {
								mPlayer.sendMessage(Component.text("The teleport for your guild is not set up", NamedTextColor.RED));
								mPlayer.sendMessage(Component.text("Please ask a moderator to fix this", NamedTextColor.RED));
								return;
							}

							mPlayer.teleport(optLoc.get(), PlayerTeleportEvent.TeleportCause.COMMAND);
						});
					} catch (Exception ex) {
						Bukkit.getScheduler().runTask(mMainPlugin, () -> {
							mPlayer.sendMessage(Component.text("Unable to teleport you to that guild:", NamedTextColor.RED));
							MessagingUtils.sendStackTrace(mPlayer, ex);
						});
					}
				});
			});
	}

	protected void setPageArrows(int totalRows) {
		int maxPage = Math.floorDiv(Math.max(0, totalRows - 1), PAGE_HEIGHT);
		mPage = Math.max(0, Math.min(mPage, maxPage));

		ItemStack item;
		ItemMeta meta;

		// Prev/Next page buttons
		if (mPage > 0) {
			item = new ItemStack(Material.ARROW);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Previous Page", NamedTextColor.WHITE, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			setItem(HEADER_Y, 0, item).onClick((InventoryClickEvent event) -> clickPrev());
		}

		if (mPage < maxPage) {
			item = new ItemStack(Material.ARROW);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Next Page", NamedTextColor.WHITE, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			setItem(HEADER_Y, 8, item).onClick((InventoryClickEvent event) -> clickNext());
		}
	}

	private void clickPrev() {
		mPage--;
		update();
	}

	private void clickNext() {
		mPage++;
		update();
	}

	public void changeOrder(GuildOrder order) {
		mOrder = order;
		refresh();
	}

	public void refresh() {
		Bukkit.getScheduler().runTaskAsynchronously(mMainPlugin, () -> {
			mPlayerUser = LuckPermsIntegration.loadUser(mPlayerUser.getUniqueId()).join();

			List<PlayerGuildInfo> guilds = mOrder.sortGuilds(
				PlayerGuildInfo.ofCollection(
						mPlayerUser,
						LuckPermsIntegration.getRelevantGuilds(mPlayerUser, true, false)
					).join()
					.stream()
					.filter(playerGuildInfo -> playerGuildInfo.getGuildPermissions().contains(GuildPermission.VISIT))
					.collect(Collectors.toList())
			).join();

			Group mainGuild = LuckPermsIntegration.getGuild(mPlayerUser);
			if (mainGuild != null) {
				PlayerGuildInfo mainGuildInfo = PlayerGuildInfo.of(mPlayerUser, mainGuild).join();
				if (mainGuildInfo != null) {
					guilds.remove(mainGuildInfo);
					guilds.add(0, mainGuildInfo);
				}
			}

			Bukkit.getScheduler().runTask(mMainPlugin, () -> {
				// Handle this list sync so that it can't be modified during reads
				mAccessibleGuilds = guilds;
				update();
			});
		});
	}
}
