package com.playmonumenta.plugins.integrations.luckperms.guildgui;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.integrations.luckperms.GuildAccessLevel;
import com.playmonumenta.plugins.integrations.luckperms.GuildInviteLevel;
import com.playmonumenta.plugins.integrations.luckperms.GuildPermission;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.integrations.luckperms.PlayerGuildInfo;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.mail.MailCache;
import com.playmonumenta.plugins.mail.MailGui;
import com.playmonumenta.plugins.mail.MailGuiSettings;
import com.playmonumenta.plugins.mail.MailMan;
import com.playmonumenta.plugins.mail.Mailbox;
import com.playmonumenta.plugins.mail.recipient.GuildRecipient;
import com.playmonumenta.plugins.mail.recipient.Recipient;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration.ALL_CACHED_PLAYER_NAMES_SUGGESTIONS;

public class GuildGui extends MailGui {
	protected static final int HEADER_Y = 0;
	protected static final int ROW_LABEL_X = 0;
	protected static final int PAGE_HEIGHT = 5;

	public static Map<String, Consumer<GuildGui>> VIEW_ARGUMENTS = Map.of(
		"all", gui -> gui.setView(new AllGuildsView(gui, GuildOrder.DEFAULT)),
		"accessible", gui -> gui.setView(new AccessibleGuildsView(gui, GuildOrder.DEFAULT)),
		"members", gui -> gui.setView(new GuildMembersView(gui)),
		"lockdown", gui -> gui.setView(new EmergencyLockdownView(gui))
	);

	protected final Plugin mMainPlugin;
	protected UUID mTargetUuid;
	// The LuckPerms user for whoever opened the GUI
	protected User mTargetUser;
	protected @Nullable String mGuildId;
	protected @Nullable Group mGuildGroup;
	protected final MailGuiSettings mMailSettings = new MailGuiSettings();
	protected MailCache mMailCache;
	protected View mView;

	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.guild.gui");
		CommandPermission permsMod = CommandPermission.fromString("monumenta.command.guild.mod.gui");

		new CommandAPICommand("guild")
			.withArguments(new LiteralArgument("gui"))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, perms);
				run(plugin, sender, null, null, null);
			})
			.register();

		for (Map.Entry<String, Consumer<GuildGui>> viewEntry : VIEW_ARGUMENTS.entrySet()) {
			new CommandAPICommand("guild")
				.withArguments(new LiteralArgument("gui"))
				.withArguments(new LiteralArgument(viewEntry.getKey()))
				.executes((sender, args) -> {
					CommandUtils.checkPerm(sender, perms);
					run(plugin, sender, null, null, viewEntry.getValue());
				})
				.register();
		}

		new CommandAPICommand("guild")
			.withArguments(new LiteralArgument("mod"))
			.withArguments(new LiteralArgument("gui"))
			.withArguments(new LiteralArgument("player"))
			.withArguments(new TextArgument("player").replaceSuggestions(ALL_CACHED_PLAYER_NAMES_SUGGESTIONS))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, permsMod);
				run(plugin, sender, args.getUnchecked("player"), null, null);
			})
			.register();

		new CommandAPICommand("guild")
			.withArguments(new LiteralArgument("mod"))
			.withArguments(new LiteralArgument("gui"))
			.withArguments(new LiteralArgument("guild"))
			.withArguments(new TextArgument("guild name").replaceSuggestions(GuildArguments.NAME_SUGGESTIONS))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, permsMod);
				run(plugin, sender, null, args.getUnchecked("guild name"), null);
			})
			.register();
	}

	public static void showDefaultView(Plugin plugin, Player player) {
		try {
			run(plugin, player, null, null, null);
		} catch (WrapperCommandSyntaxException ex) {
			player.sendMessage(Component.text(ex.getMessage(), NamedTextColor.RED));
		}
	}

	private static void run(
		Plugin plugin,
		CommandSender sender,
		@Nullable String targetName,
		@Nullable String guildName,
		@Nullable Consumer<GuildGui> viewSetter
	) throws WrapperCommandSyntaxException {
		if (ServerProperties.getShardName().contains("build")) {
			throw CommandAPI.failWithString("This command cannot be run on the build shard.");
		}

		CommandSender callee = CommandUtils.getCallee(sender);
		if (!(callee instanceof Player player)) {
			throw CommandAPI.failWithString("The guild GUI can only be shown to players");
		}

		boolean openedAsModerator = targetName != null || guildName != null;

		UUID targetUuid;
		if (targetName == null) {
			targetUuid = player.getUniqueId();
		} else {
			targetUuid = MonumentaRedisSyncIntegration.cachedNameToUuid(targetName);
			if (targetUuid == null) {
				throw CommandAPI.failWithString(
					"Could not identify that player - is the spelling/capitalization correct?");
			}
		}

		String guildId = null;
		if (guildName != null) {
			guildId = GuildArguments.getIdFromName(guildName);
			if (guildId == null) {
				player.sendMessage(Component.text("Could not identify guild by name: " + guildName,
					NamedTextColor.RED));
			}
		}

		GuildGui gui = new GuildGui(plugin, player, targetUuid, guildId, openedAsModerator);
		if (viewSetter != null) {
			viewSetter.accept(gui);
		}
		gui.open();
	}

	private GuildGui(Plugin plugin, Player player, UUID targetUuid, @Nullable String guildId, boolean openedAsModerator) {
		super(
			player,
			54,
			Component.text("Guild Manager:", NamedTextColor.GRAY, TextDecoration.BOLD),
			openedAsModerator
		);
		mMainPlugin = plugin;
		mTargetUser = LuckPermsIntegration.getUser(mPlayer);
		mTargetUuid = targetUuid;
		mGuildId = guildId;

		// Temporary value to be overridden ASAP
		Recipient tempRecipient = new GuildRecipient(GuildRecipient.DUMMY_ID_NOT_LOADED, null);
		mMailCache = new MailCache(tempRecipient);

		// Copied from setView to make nullaway warning go away
		Group guild = LuckPermsIntegration.getGuild(player);
		if (LuckPermsIntegration.getRelevantGuilds(player, true, true).isEmpty()) {
			mView = new AllGuildsView(this, GuildOrder.DEFAULT);
		} else if (guild == null) {
			mView = new AccessibleGuildsView(this, GuildOrder.DEFAULT);
		} else {
			mView = new GuildMembersView(this);
		}
		if (guildId != null) {
			updateGuildGroup();
		} else {
			updateTargetUser();
		}
	}

	@Override
	public void open() {
		super.openBypassAccessCheck();
	}

	/*
	 * Sets the view, refreshing the LP info it requires
	 */
	protected void setView(View view) {
		mView = view;
		mPlayer.playSound(mPlayer, Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0f, 1.0f);
		refresh();
	}

	/*
	 * Applies any LuckPerms changes that may have occurred while the GUI is open
	 */
	@Override
	public void refresh() {
		updateGuildGroup();
	}

	@Override
	public void refreshMailbox(Mailbox mailbox) {
		if (mView instanceof MailView mailView) {
			mailView.refreshMailboxSlot(mailbox);
		}
	}

	@Override
	public MailCache getOwnerCache() {
		return mMailCache;
	}

	@Override
	public Collection<MailCache> getRecipientCaches() {
		return List.of(mMailCache);
	}

	@Override
	protected void setup() {
		setHeader();
		mView.setup();
	}

	/*
	 * Sets the header items that take you to each view, with placeholders for prev/next page:
	 * - All Guilds
	 * - Your Accessible Guilds (or for target player in mod GUI)
	 * - Guild members/guests for selected guild
	 * - Guild mailbox
	 * - Emergency Lock to prevent edits/access until mod intervention
	 */
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

		item = new ItemStack(Material.BLUE_BANNER);
		meta = item.getItemMeta();
		meta.displayName(Component.text("All Guilds", NamedTextColor.GOLD)
			.decoration(TextDecoration.ITALIC, false));
		lore = new ArrayList<>();
		lore.add(Component.text("A list of every guild on Monumenta", NamedTextColor.YELLOW)
			.decoration(TextDecoration.ITALIC, false));
		lore.addAll(guildOrderLore);
		meta.lore(lore);
		if (meta instanceof BannerMeta bannerMeta) {
			// https://www.planetminecraft.com/banner/the-earth-408589/
			bannerMeta.addPattern(new Pattern(DyeColor.LIME, PatternType.GLOBE));
			bannerMeta.addPattern(new Pattern(DyeColor.GREEN, PatternType.GLOBE));
			bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.CURLY_BORDER));
			bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.BORDER));
			bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
			bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
			meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
		}
		item.setItemMeta(meta);
		setItem(HEADER_Y, 1, item)
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
				} else {
					order = GuildOrder.DEFAULT;
				}

				if (mView instanceof AllGuildsView allGuildsView) {
					allGuildsView.changeOrder(order);
				} else {
					setView(new AllGuildsView(this, order));
				}
			});

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
			String targetName = MonumentaRedisSyncIntegration.cachedUuidToName(mTargetUuid);
			if (targetName == null) {
				headMeta.setOwningPlayer(Bukkit.getOfflinePlayer(mTargetUuid));
			} else {
				headMeta.setPlayerProfile(Bukkit.createProfile(mTargetUuid, targetName));
			}
		}
		item.setItemMeta(meta);
		setItem(HEADER_Y, 2, item)
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
				} else {
					order = GuildOrder.DEFAULT;
				}

				if (mView instanceof AccessibleGuildsView accessibleGuildsView) {
					accessibleGuildsView.changeOrder(order);
				} else {
					setView(new AccessibleGuildsView(this, order));
				}
			});

		if (mGuildGroup == null) {
			item = new ItemStack(Material.BLACK_BANNER);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Selected Guild Members", NamedTextColor.DARK_RED)
				.decoration(TextDecoration.ITALIC, false));
			lore = List.of(Component.text("You have not selected a guild", NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false));
			meta.lore(lore);
			if (meta instanceof BannerMeta bannerMeta) {
				// https://www.planetminecraft.com/banner/question-mark-banner-209623/
				bannerMeta.addPattern(new Pattern(DyeColor.YELLOW, PatternType.STRIPE_TOP));
				bannerMeta.addPattern(new Pattern(DyeColor.YELLOW, PatternType.STRIPE_RIGHT));
				bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.HALF_HORIZONTAL_MIRROR));
				bannerMeta.addPattern(new Pattern(DyeColor.YELLOW, PatternType.STRIPE_MIDDLE));
				bannerMeta.addPattern(new Pattern(DyeColor.YELLOW, PatternType.SQUARE_BOTTOM_LEFT));
				bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.BORDER));
				meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
			}
			item.setItemMeta(meta);
			setItem(HEADER_Y, 3, item);
		} else {
			item = LuckPermsIntegration.getGuildBanner(mGuildGroup);
			meta = item.getItemMeta();
			meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);

			String guildName = LuckPermsIntegration.getNonNullGuildName(mGuildGroup);
			TextColor guildColor = LuckPermsIntegration.getGuildColor(mGuildGroup);

			meta.displayName(Component.text("", NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false)
				.append(Component.text(guildName, guildColor))
				.append(Component.text(" Guild Members")));
			lore = List.of(
				Component.text("Players with access to ", NamedTextColor.YELLOW)
					.decoration(TextDecoration.ITALIC, false)
					.append(Component.text(guildName, guildColor)),
				Component.text("Guests included", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false)
			);
			meta.lore(lore);
			item.setItemMeta(meta);
			setItem(HEADER_Y, 3, item)
				.onClick((InventoryClickEvent event) -> setView(new GuildMembersView(this)));
		}

		if (
			mGuildGroup != null
				&& mPlayer.hasPermission(MAIL_PERM.toString())
				&& (
				mPlayer.hasPermission(MAIL_MOD_PERM.toString())
					|| GuildPermission.MAIL.hasAccess(mGuildGroup, mPlayer)
			)
		) {
			setItem(HEADER_Y, 4, mMailCache.recipient().mailboxIcon(mMailSettings))
				.onClick((InventoryClickEvent event) -> {
					guiSettingsClick(event, mMailSettings);
					setView(new MailView(this));
				});
		} else {
			setItem(HEADER_Y, 4,
				GUIUtils.createBasicItem(Material.ENDER_CHEST, mMailCache.recipient().mailboxIconName(mMailSettings)))
				.onClick((InventoryClickEvent event) -> {
					mPlayer.sendMessage(Component.text("You do not have access to your guild's mail.", NamedTextColor.RED));
					mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				});
		}

		// Emergency Lockdown (with confirm screen)
		item = new ItemStack(Material.IRON_BARS);
		meta = item.getItemMeta();
		meta.displayName(Component.text("Emergency Lockdown", NamedTextColor.RED)
			.decoration(TextDecoration.ITALIC, false));
		meta.lore(List.of(
			Component.text("WARNING!", NamedTextColor.DARK_RED, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false),
			Component.text("- Prevents modification to guild access", NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false),
			Component.text("- Prevents access to guild areas", NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false),
			Component.text("- Access can only be restored by a moderator!", NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false),
			Component.text("- Accept the confirmation button to apply", NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false)
		));
		item.setItemMeta(meta);
		setItem(HEADER_Y, 7, item)
			.onClick((InventoryClickEvent event) -> setView(new EmergencyLockdownView(this)));
	}

	private void updateTargetUser() {
		Bukkit.getScheduler().runTaskAsynchronously(mMainPlugin, () -> {
			mTargetUser = LuckPermsIntegration.loadUser(mTargetUuid).join();
			mGuildGroup = LuckPermsIntegration.getGuildRoot(LuckPermsIntegration.getGuild(mTargetUser));
			mGuildId = mGuildGroup == null ? null : mGuildGroup.getName();
			updateMailCache();
			Bukkit.getScheduler().runTask(mMainPlugin, () -> {
				mView.refresh();
				update();
			});
		});
	}

	private void updateGuildGroup() {
		Bukkit.getScheduler().runTaskAsynchronously(mMainPlugin, () -> {
			if (mGuildId == null) {
				mGuildGroup = null;
			} else {
				mGuildGroup = LuckPermsIntegration.loadGroup(mGuildId).join().orElse(null);
			}
			updateMailCache();
			Bukkit.getScheduler().runTask(mMainPlugin, () -> {
				mView.refresh();
				update();
			});
		});
	}

	private void updateMailCache() {
		Long guildPlotId = mGuildGroup == null ? GuildRecipient.DUMMY_ID_NO_GUILD : LuckPermsIntegration.getGuildPlotId(mGuildGroup);
		if (guildPlotId == null) {
			guildPlotId = GuildRecipient.DUMMY_ID_NO_GUILD_NUMBER;
		}
		Recipient recipient = new GuildRecipient(guildPlotId, mGuildGroup);
		mMailCache = MailMan.recipientMailCache(recipient);
	}

	protected void setGuildIcon(int row, int column, PlayerGuildInfo guildInfo) {
		Group guild = guildInfo.getGuild();
		GuildAccessLevel accessLevel = guildInfo.getAccessLevel();
		GuildInviteLevel inviteLevel = guildInfo.getInviteLevel();

		boolean hasGuildMembership = !GuildAccessLevel.NONE.equals(accessLevel);
		boolean hasGuildInvite = !GuildInviteLevel.NONE.equals(inviteLevel);

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
		for (GuildPermission guildPermission : GuildPermission.values()) {
			if (guildInfo.getGuildPermissions().contains(guildPermission)) {
				lore.add(Component.text("- May ", NamedTextColor.GREEN)
					.decoration(TextDecoration.ITALIC, false)
					.append(Component.text(guildPermission.mLabel)));
			} else {
				lore.add(Component.text("- May Not ", NamedTextColor.RED)
					.decoration(TextDecoration.ITALIC, false)
					.append(Component.text(guildPermission.mLabel)));
			}
		}
		lore.add(Component.empty());

		if (hasGuildMembership) {
			lore.add(Component.text(accessLevel.mLabel, NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false));
		}
		if (hasGuildInvite) {
			lore.add(Component.text(inviteLevel.mDescription, NamedTextColor.GREEN)
				.decoration(TextDecoration.ITALIC, false));
		}
		if (LuckPermsIntegration.isLocked(guild)) {
			lore.add(Component.text("CURRENTLY ON LOCKDOWN", NamedTextColor.DARK_GRAY, TextDecoration.BOLD));
		} else {
			if (accessLevel.compareTo(GuildAccessLevel.MEMBER) <= 0) {
				lore.add(Component.text("", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false)
					.append(Component.keybind(Constants.Keybind.HOTBAR_9))
					.append(Component.text(": Leave guild")));
			} else if (hasGuildInvite) {
				lore.add(Component.text("Invite options (hotbar keys to select):", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false));
				switch (inviteLevel) {
					case MEMBER_INVITE: {
						Group mainGuild = LuckPermsIntegration.getGuild(mTargetUser);
						if (mainGuild != null) {
							lore.add(Component.text("", NamedTextColor.DARK_GRAY)
								.decoration(TextDecoration.ITALIC, false)
								.append(Component.keybind(Constants.Keybind.HOTBAR_7)
									.color(NamedTextColor.RED))
								.append(Component.text(": Cannot accept invite as member, already in a guild")));
						} else {
							lore.add(Component.text("", NamedTextColor.GRAY)
								.decoration(TextDecoration.ITALIC, false)
								.append(Component.keybind(Constants.Keybind.HOTBAR_7))
								.append(Component.text(": Accept invite as member")));
						}
					}
					// fall through
					case GUEST_INVITE: {
						if (GuildAccessLevel.GUEST.equals(accessLevel)) {
							lore.add(Component.text("", NamedTextColor.DARK_GRAY)
								.decoration(TextDecoration.ITALIC, false)
								.append(Component.keybind(Constants.Keybind.HOTBAR_8))
								.append(Component.text(": Already a guest")));
						} else {
							lore.add(Component.text("", NamedTextColor.GRAY)
								.decoration(TextDecoration.ITALIC, false)
								.append(Component.keybind(Constants.Keybind.HOTBAR_8))
								.append(Component.text(": Accept invite as guest")));
						}
						lore.add(Component.text("", NamedTextColor.GRAY)
							.decoration(TextDecoration.ITALIC, false)
							.append(Component.keybind(Constants.Keybind.HOTBAR_9))
							.append(Component.text(": Discard invite")));
						break;
					}
					default: {
						lore.add(Component.text("- This should not appear!", NamedTextColor.RED)
							.decoration(TextDecoration.ITALIC, false));
					}
				}
			} else if (accessLevel.equals(GuildAccessLevel.GUEST)) {
				lore.add(Component.text("", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false)
					.append(Component.keybind(Constants.Keybind.HOTBAR_9))
					.append(Component.text(": Give up guest access")));
			}
		}
		if (mPlayer.isOp()) {
			lore.add(Component.text("Extra dev/mod info:", NamedTextColor.DARK_GRAY)
				.decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("Guild LP Group: " + guild.getName(), NamedTextColor.DARK_GRAY)
				.decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("Plot World ID (does not guarantee plot is purchased): " + LuckPermsIntegration.getGuildPlotId(guild), NamedTextColor.DARK_GRAY)
				.decoration(TextDecoration.ITALIC, false));
		}
		meta.lore(lore);

		item.setItemMeta(meta);
		GuiItem guiItem = setItem(row, column, new GuiItem(item, false));

		guiItem.onClick((InventoryClickEvent event) -> {
			if (LuckPermsIntegration.isLocked(guild)) {
				mPlayer.sendMessage(Component.text("That guild is on lockdown; you may not interact with it at this time.", NamedTextColor.RED));
				mPlayer.playSound(mPlayer,
					Sound.BLOCK_IRON_DOOR_CLOSE,
					SoundCategory.PLAYERS,
					0.7f,
					Constants.Note.FS3.mPitch);
				refresh();
				return;
			}
			if (accessLevel.compareTo(GuildAccessLevel.MEMBER) <= 0) {
				if (event.getHotbarButton() == 8) {
					GuildAccessLevel currentAccessLevel = LuckPermsIntegration.getAccessLevel(guild, mTargetUser);
					if (!currentAccessLevel.equals(accessLevel)) {
						refresh();
						return;
					}

					Bukkit.getScheduler().runTaskAsynchronously(mMainPlugin, () -> {
						GuildAccessLevel.setAccessLevel(mTargetUser, guild, GuildAccessLevel.NONE).join();
						Bukkit.getScheduler().runTask(mMainPlugin, () -> {
							mPlayer.sendMessage(Component.text("You left ", NamedTextColor.GOLD)
								.append(LuckPermsIntegration.getGuildFullComponent(guild))
								.append(Component.text(".")));
							refresh();
						});
					});
				}
			} else if (hasGuildInvite) {
				Group mainGuild = LuckPermsIntegration.getGuild(mTargetUser);
				GuildInviteLevel currentInviteLevel = LuckPermsIntegration.getInviteLevel(guild, mTargetUser);
				switch (event.getHotbarButton()) {
					case 8 -> {
						// Discard invite

						if (currentInviteLevel.equals(GuildInviteLevel.NONE)) {
							return;
						}

						Bukkit.getScheduler().runTaskAsynchronously(mMainPlugin, () -> {
							GuildInviteLevel.setInviteLevel(mTargetUser, guild, GuildInviteLevel.NONE).join();
							Bukkit.getScheduler().runTask(mMainPlugin, () -> {
								mPlayer.sendMessage(Component.text("You have discarded your invite to ", NamedTextColor.GOLD)
									.append(LuckPermsIntegration.getGuildFullComponent(guild))
									.append(Component.text(".")));
								refresh();
							});
						});
					}
					case 7 -> {
						// Accept guest invite

						if (currentInviteLevel.equals(GuildInviteLevel.NONE)) {
							return;
						}

						Bukkit.getScheduler().runTaskAsynchronously(mMainPlugin, () -> {
							GuildAccessLevel.setAccessLevel(mTargetUser, guild, GuildAccessLevel.GUEST).join();
							if (GuildInviteLevel.GUEST_INVITE.equals(currentInviteLevel)) {
								GuildInviteLevel.setInviteLevel(mTargetUser, guild, GuildInviteLevel.NONE).join();
							}
							Bukkit.getScheduler().runTask(mMainPlugin, () -> {
								mPlayer.sendMessage(Component.text("You are now a guest of ", NamedTextColor.GOLD)
									.append(LuckPermsIntegration.getGuildFullComponent(guild))
									.append(Component.text("!")));
								refresh();
							});
						});
					}
					case 6 -> {
						// Accept member invite

						if (mainGuild != null || !currentInviteLevel.equals(GuildInviteLevel.MEMBER_INVITE)) {
							return;
						}

						Bukkit.getScheduler().runTaskAsynchronously(mMainPlugin, () -> {
							GuildAccessLevel.setAccessLevel(mTargetUser, guild, GuildAccessLevel.MEMBER).join();
							GuildInviteLevel.setInviteLevel(mTargetUser, guild, GuildInviteLevel.NONE).join();
							Bukkit.getScheduler().runTask(mMainPlugin, () -> {
								mPlayer.sendMessage(Component.text("Welcome to ", NamedTextColor.GOLD)
									.append(LuckPermsIntegration.getGuildFullComponent(guild))
									.append(Component.text("!")));
								refresh();
							});
						});
					}
					default // Do nothing
						-> mPlayer.playSound(mPlayer, Sound.UI_BUTTON_CLICK, SoundCategory.PLAYERS, 1.0f, 0.5f);
				}
			} else if (accessLevel.equals(GuildAccessLevel.GUEST)) {
				if (event.getHotbarButton() == 8) {
				GuildAccessLevel currentAccessLevel = LuckPermsIntegration.getAccessLevel(guild, mTargetUser);
				if (!currentAccessLevel.equals(accessLevel)) {
					refresh();
					return;
				}

				Bukkit.getScheduler().runTaskAsynchronously(mMainPlugin, () -> {
					GuildAccessLevel.setAccessLevel(mTargetUser, guild, GuildAccessLevel.NONE).join();
					Bukkit.getScheduler().runTask(mMainPlugin, () -> {
						mPlayer.sendMessage(Component.text("You gave up your guest access to ", NamedTextColor.GOLD)
							.append(LuckPermsIntegration.getGuildFullComponent(guild))
							.append(Component.text(".")));
						refresh();
					});
				});
				}
			}
		});
	}

	protected void setAccessHeaderIcon(int row, int column, GuildAccessLevel accessLevel) {
		ItemStack item = getAccessHeaderIcon(accessLevel);

		GuiItem guiItem = setItem(row, column, item);
		if (!GuildAccessLevel.FOUNDER.equals(accessLevel)) {
			guiItem.onClick(onAccessHeaderClick(accessLevel));
		}
	}

	protected ItemStack getAccessHeaderIcon(GuildAccessLevel accessLevel) {
		Material material;
		Component name;
		List<Component> lore = new ArrayList<>();
		switch (accessLevel) {
			case FOUNDER -> {
				material = Material.NETHERITE_HELMET;
				name = Component.text("Founder", NamedTextColor.DARK_GRAY);
			}
			case MANAGER -> {
				material = Material.DIAMOND_HELMET;
				name = Component.text("Manager", NamedTextColor.AQUA);
			}
			case MEMBER -> {
				material = Material.GOLDEN_HELMET;
				name = Component.text("Member", NamedTextColor.GOLD);
			}
			case GUEST -> {
				material = Material.IRON_HELMET;
				name = Component.text("Guest", NamedTextColor.GRAY);
			}
			default -> {
				material = Material.LEATHER_HELMET;
				name = Component.text("None?", NamedTextColor.BLACK);
			}
		}
		switch (accessLevel) {
			case FOUNDER:
				lore.add(
					Component.text("Founders:", NamedTextColor.DARK_GRAY)
						.decoration(TextDecoration.ITALIC, false));
				lore.add(
					Component.text("- May promote and demote", NamedTextColor.DARK_GRAY)
						.decoration(TextDecoration.ITALIC, false));
				lore.add(
					Component.text("  members to managers", NamedTextColor.DARK_GRAY)
						.decoration(TextDecoration.ITALIC, false));
				lore.add(
					Component.text("- May promote but not demote", NamedTextColor.DARK_GRAY)
						.decoration(TextDecoration.ITALIC, false));
				lore.add(
					Component.text("  members to founders", NamedTextColor.DARK_GRAY)
						.decoration(TextDecoration.ITALIC, false));
				// fall through
			case MANAGER:
				lore.add(
					Component.text("Managers and up:", NamedTextColor.AQUA)
						.decoration(TextDecoration.ITALIC, false));
				lore.add(
					Component.text("- May add and remove", NamedTextColor.AQUA)
						.decoration(TextDecoration.ITALIC, false));
				lore.add(
					Component.text("  guests and members", NamedTextColor.AQUA)
						.decoration(TextDecoration.ITALIC, false));
				lore.add(
					Component.text("- May lock the guild until", NamedTextColor.AQUA)
						.decoration(TextDecoration.ITALIC, false));
				lore.add(
					Component.text("  a moderator is available", NamedTextColor.AQUA)
						.decoration(TextDecoration.ITALIC, false));
				// fall through
			case MEMBER:
				lore.add(
					Component.text("Members and up:", NamedTextColor.GOLD)
						.decoration(TextDecoration.ITALIC, false));
				break;
			case GUEST:
				lore.add(
					Component.text("Guests:", NamedTextColor.GRAY)
						.decoration(TextDecoration.ITALIC, false));
				break;
			default:
				lore.add(Component.text("- This should not appear!", NamedTextColor.RED)
					.decoration(TextDecoration.ITALIC, false));
		}

		Group guildRoot = LuckPermsIntegration.getGuildRoot(mGuildGroup);
		Group displayedAccessLevel;
		if (guildRoot != null) {
			displayedAccessLevel = accessLevel.getLoadedGroupFromRoot(guildRoot);
			if (displayedAccessLevel != null) {
				for (GuildPermission guildPermission : GuildPermission.values()) {
					if (guildPermission.hasAccess(guildRoot, displayedAccessLevel)) {
						lore.add(Component.text("- May ", NamedTextColor.GREEN)
							.decoration(TextDecoration.ITALIC, false)
							.append(Component.text(guildPermission.mLabel)));
					} else {
						lore.add(Component.text("- May Not ", NamedTextColor.RED)
							.decoration(TextDecoration.ITALIC, false)
							.append(Component.text(guildPermission.mLabel)));
					}
				}
			}
		}

		if (
			!GuildAccessLevel.FOUNDER.equals(accessLevel)
			&& mayManagePermissions(accessLevel, false)
		) {
			lore.add(Component.empty());
			lore.add(Component.text("", NamedTextColor.GRAY)
				.decoration(TextDecoration.ITALIC, false)
				.append(Component.keybind(Constants.Keybind.HOTBAR_1))
				.append(Component.text(": Edit Guild's Guest Permissions")));
		}

		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(name.decoration(TextDecoration.ITALIC, false));
		meta.lore(lore);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		item.setItemMeta(meta);

		return item;
	}

	protected Consumer<InventoryClickEvent> onAccessHeaderClick(GuildAccessLevel accessLevel) {
		return (InventoryClickEvent event) -> {
			if (mGuildGroup == null) {
				return;
			}
			Group accessLevelGroup = accessLevel.loadGroupFromRoot(mGuildGroup).join().orElse(null);
			if (accessLevelGroup == null) {
				return;
			}

			if (
				event.getHotbarButton() == 0
					&& mayManagePermissions(accessLevel, false)
			) {
				View view = new PermissionView(
					this,
					accessLevel.equals(GuildAccessLevel.GUEST),
					accessLevelGroup,
					getAccessHeaderIcon(accessLevel),
					() -> {
						CompletableFuture<ItemStack> future = new CompletableFuture<>();
						future.complete(getAccessHeaderIcon(accessLevel));
						return future;
					},
					onAccessHeaderClick(accessLevel)
				);
				setView(view);
			}
		};
	}

	protected void setPlayerIcon(int y, int x, int relativeIndex, List<PlayerGuildInfo> players) {
		if (relativeIndex >= players.size()) {
			return;
		}
		PlayerGuildInfo playerGuildInfo = players.get(relativeIndex);

		setPlayerIcon(y, x, playerGuildInfo);
	}

	/**
	 * Sets an icon for a specified player. The player's UUID and/or name must be known.
	 * @param y The zero-indexed row of the inventory
	 * @param x The zero-indexed column of the inventory
	 * @param playerGuildInfo The player's information for a given guild
	 */
	protected void setPlayerIcon(int y, int x, PlayerGuildInfo playerGuildInfo) {
		setItem(y, x, getPlayerIconItem(playerGuildInfo))
			.onClick(onPlayerIconClick(playerGuildInfo));
	}

	protected ItemStack getPlayerIconItem(PlayerGuildInfo playerGuildInfo) {
		ItemStack item;
		ItemMeta meta;

		UUID playerUuid = playerGuildInfo.getUniqueId();
		String playerName = playerGuildInfo.getNonNullName();

		if (playerUuid == null) {
			item = new ItemStack(Material.BARRIER);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Could not look up UUID for " + playerName, NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false));
		} else {
			item = ItemUtils.createPlayerHead(playerUuid, playerName);
			meta = item.getItemMeta();
		}
		meta.displayName(Component.text(playerName, NamedTextColor.YELLOW)
			.decoration(TextDecoration.ITALIC, false));

		List<Component> lore = meta.lore();
		if (lore == null) {
			lore = new ArrayList<>();
		}

		GuildAccessLevel accessLevel = playerGuildInfo.getAccessLevel();
		if (!GuildAccessLevel.FOUNDER.equals(accessLevel)) {
			for (GuildPermission guildPermission : GuildPermission.values()) {
				if (playerGuildInfo.getGuildPermissions().contains(guildPermission)) {
					lore.add(Component.text("- May ", NamedTextColor.GREEN)
						.decoration(TextDecoration.ITALIC, false)
						.append(Component.text(guildPermission.mLabel)));
				} else {
					lore.add(Component.text("- May Not ", NamedTextColor.RED)
						.decoration(TextDecoration.ITALIC, false)
						.append(Component.text(guildPermission.mLabel)));
				}
			}
			lore.add(Component.empty());

			if (mayManagePermissions(accessLevel, false)) {
				Component baseLoreFormatting = Component.text("", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false);

				lore.add(baseLoreFormatting
					.append(Component.keybind(Constants.Keybind.HOTBAR_1))
					.append(Component.text(": Edit Player Permissions")));
			}
		}

		meta.lore(lore);
		item.setItemMeta(meta);
		return item;
	}

	protected Consumer<InventoryClickEvent> onPlayerIconClick(PlayerGuildInfo playerGuildInfo) {
		return (InventoryClickEvent event) -> {
			GuildAccessLevel accessLevel = playerGuildInfo.getAccessLevel();
			if (
				event.getHotbarButton() == 0
					&& !GuildAccessLevel.FOUNDER.equals(accessLevel)
					&& mayManagePermissions(accessLevel, false)
			) {
				View view = new PermissionView(
					this,
					playerGuildInfo.getAccessLevel().equals(GuildAccessLevel.GUEST),
					playerGuildInfo.getUser(),
					getPlayerIconItem(playerGuildInfo),
					() -> {
						CompletableFuture<ItemStack> future = new CompletableFuture<>();

						Bukkit.getScheduler().runTaskAsynchronously(mMainPlugin, () -> {
							PlayerGuildInfo updated = playerGuildInfo.getUpdated().join();
							future.complete(getPlayerIconItem(updated));
						});

						return future;
					},
					onPlayerIconClick(playerGuildInfo)
				);
				setView(view);
			}
		};
	}

	protected boolean isManager() {
		return isAccessLevel(GuildAccessLevel.MANAGER);
	}

	protected boolean isAccessLevel(GuildAccessLevel accessLevel) {
		Group guildAccessGroup = LuckPermsIntegration.getGuild(mTargetUser);
		Group guildRoot = LuckPermsIntegration.getGuildRoot(guildAccessGroup);
		return guildAccessGroup != null
			&& guildRoot != null
			&& guildRoot.equals(LuckPermsIntegration.getGuildRoot(mGuildGroup))
			&& GuildAccessLevel.byGroup(guildAccessGroup).compareTo(accessLevel) <= 0;
	}

	protected boolean mayManagePermissions(GuildAccessLevel targetLevel, boolean showErrorMessages) {
		if (isAccessLevel(targetLevel.compareTo(GuildAccessLevel.MANAGER) <= 0 ? GuildAccessLevel.FOUNDER : GuildAccessLevel.MANAGER)) {
			return true;
		}

		if (mPlayer.isOp()) {
			if (showErrorMessages) {
				mPlayer.sendMessage(Component.text(
					"Your operator status bypassed the guild manager requirement to manage guild permissions.",
					NamedTextColor.RED));
			}
			return true;
		} else {
			if (showErrorMessages) {
				mPlayer.sendMessage(Component.text(
					"You need to be a guild manager to manage guild permissions.",
					NamedTextColor.RED));
			}
			return false;
		}
	}
}
