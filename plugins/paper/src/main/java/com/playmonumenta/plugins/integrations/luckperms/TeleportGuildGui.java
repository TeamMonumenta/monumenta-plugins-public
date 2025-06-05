package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.integrations.luckperms.guildgui.GuildGui;
import com.playmonumenta.plugins.integrations.luckperms.guildgui.GuildOrder;
import com.playmonumenta.plugins.utils.CommandUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

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
	private List<@Nullable PlayerGuildInfo> mAccessibleGuilds = new ArrayList<>();
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
					new TeleportGuildGui(plugin, player).open();
				}
			})
			.register();
	}

	private TeleportGuildGui(Plugin plugin, Player player) {
		super(player, 54, Component.text("Teleport to Guild"));
		mMainPlugin = plugin;
		mPlayerUser = LuckPermsIntegration.getUser(mPlayer);

		// Faster initial load
		CompletableFuture<List<Group>> nonPublicGuildsFuture = new CompletableFuture<>();
		nonPublicGuildsFuture.complete(new ArrayList<>(LuckPermsIntegration.getRelevantGuilds(mPlayerUser, true, false)));
		refresh(nonPublicGuildsFuture);

		// Include public guilds
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

				PlayerGuildInfo playerGuildInfo = mAccessibleGuilds.get(index);
				if (playerGuildInfo != null) {
					setGuildIcon(PAGE_START_Y + y,
						PAGE_START_X + x,
						playerGuildInfo);
				} else {
					ItemStack fallbackIcon = LuckPermsIntegration.defaultGuildBanner();
					ItemMeta meta = fallbackIcon.getItemMeta();
					meta.displayName(GuildPlotUtils.FALLBACK_WORLD_COMPONENT);
					meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
					fallbackIcon.setItemMeta(meta);
					setItem(PAGE_START_Y + y,
						PAGE_START_X + x,
						fallbackIcon)
						.onClick((InventoryClickEvent event) ->
							GuildPlotUtils.sendGuildPlotHub(mPlayer, true));
				}
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
		List<GuildOrder> orders = GuildOrder.visibleGuildOrders(mPlayer);
		for (int i = 0; i < 9; i++) {
			if (i >= orders.size()) {
				break;
			}

			guildOrderLore.add(
				Component.text("", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false)
					.append(Component.keybind(Constants.Keybind.hotbar(i)))
					.append(Component.text(": " + orders.get(i).mName)));
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
					int index = event.getHotbarButton();
					if (index < 0 || index >= orders.size()) {
						order = GuildOrder.DEFAULT;
					} else {
						order = orders.get(index);
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
					if (mPlayer.hasPermission(GuildGui.MOD_GUI_PERMISSION)) {
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

				GuildPlotUtils.sendGuildPlotWorld(mPlayer, guild);
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
		refresh(LuckPermsIntegration.getGuilds());
	}

	public void refresh(CompletableFuture<List<Group>> guildsFuture) {
		Bukkit.getScheduler().runTaskAsynchronously(mMainPlugin, () -> {
			mPlayerUser = LuckPermsIntegration.loadUser(mPlayerUser.getUniqueId()).join();

			List<PlayerGuildInfo> unsortedGuilds = PlayerGuildInfo.ofCollection(
					mPlayerUser,
					guildsFuture.join()
				).join()
				.stream()
				.filter(playerGuildInfo -> (
					playerGuildInfo.getGuildPermissions().contains(GuildPermission.VISIT)
						&& playerGuildInfo.getGuildFlags().contains(GuildFlag.OWNS_PLOT)
				))
				.collect(Collectors.toList());

			List<@Nullable PlayerGuildInfo> guilds = new ArrayList<>();
			guilds.add(null);

			Group mainGuild = LuckPermsIntegration.getGuild(mPlayerUser);
			if (mainGuild != null) {
				PlayerGuildInfo mainGuildInfo = PlayerGuildInfo.of(mPlayerUser, mainGuild).join();
				if (mainGuildInfo != null && unsortedGuilds.contains(mainGuildInfo)) {
					unsortedGuilds.remove(mainGuildInfo);
					guilds.add(mainGuildInfo);
				}
			}

			// Guest guilds
			guilds.addAll(mOrder.sortGuilds(unsortedGuilds.stream()
				.filter(playerGuildInfo -> !playerGuildInfo.getAccessLevel().equals(GuildAccessLevel.NONE))
				.toList()
			).join());

			// Public guilds
			guilds.addAll(mOrder.sortGuilds(unsortedGuilds.stream()
				.filter(playerGuildInfo -> playerGuildInfo.getAccessLevel().equals(GuildAccessLevel.NONE))
				.toList()
			).join());

			Bukkit.getScheduler().runTask(mMainPlugin, () -> {
				// Handle this list sync so that it can't be modified during reads
				mAccessibleGuilds = guilds;
				update();
			});
		});
	}
}
