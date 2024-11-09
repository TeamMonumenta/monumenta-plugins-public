package com.playmonumenta.plugins.listeners;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Constants.Colors;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.ShardSorterCommand;
import com.playmonumenta.plugins.commands.ToggleSwap;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.GearChanged;
import com.playmonumenta.plugins.effects.RespawnStasis;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.ArrowConsumeEvent;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import com.playmonumenta.plugins.events.EffectTypeApplyFromPotionEvent;
import com.playmonumenta.plugins.events.EntityGainAbsorptionEvent;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmsGUI;
import com.playmonumenta.plugins.itemstats.enchantments.CurseOfEphemerality;
import com.playmonumenta.plugins.itemstats.enchantments.Multitool;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.infusions.Phylactery;
import com.playmonumenta.plugins.itemstats.infusions.StatTrackManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.overrides.FirmamentOverride;
import com.playmonumenta.plugins.overrides.WorldshaperOverride;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.particle.ParticleManager;
import com.playmonumenta.plugins.player.EnderPearlTracker;
import com.playmonumenta.plugins.poi.POIManager;
import com.playmonumenta.plugins.point.Point;
import com.playmonumenta.plugins.portals.PortalManager;
import com.playmonumenta.plugins.protocollib.VirtualItemsReplacer;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.server.reset.DailyReset;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import com.playmonumenta.redissync.event.PlayerServerTransferEvent;
import com.playmonumenta.redissync.event.PlayerTransferFailEvent;
import com.playmonumenta.scriptedquests.managers.TranslationsManager;
import de.tr7zw.nbtapi.NBTEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Light;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PlayerListener implements Listener {

	private static final String PLAYERS_TEAM_NAME = "players";
	public static final String SOLO_DEATH_MOB_SLOW_EFFECT_NAME = "SoloDeathMobSlownessDebuff";
	public static final String SOLO_DEATH_MOB_WEAKEN_EFFECT_NAME = "SoloDeathMobWeakenDebuff";
	public static final String PLAYER_DEATH_TICK_TAG = "PlayerDeathTick";


	private final Plugin mPlugin;
	private @Nullable BukkitTask mContentRunnable = null;
	private final Set<UUID> mTransferringPlayers = new HashSet<>();

	public PlayerListener(Plugin plugin) {
		mPlugin = plugin;

		// Delete the players team on startup to clear any lingering entries (and recreate it later with proper settings)
		Team playersTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(PLAYERS_TEAM_NAME);
		if (playersTeam != null) {
			playersTeam.unregister();
		}

		contentLockCheckOnReload();
	}

	public void contentLockCheckOnReload() {
		if (!ServerProperties.getShardOpen() && mContentRunnable == null) {
			mContentRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					for (Player player : Bukkit.getOnlinePlayers()) {
						if (
							MonumentaRedisSyncIntegration.isPlayerTransferring(player)
								|| player.hasPermission("group.devops")
						) {
							continue;
						}
						player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1, false, false));
						player.teleport(player.getWorld().getSpawnLocation());
						AuditListener.logPlayer(player.getName() + " was kicked from a content-locked shard");
						player.sendMessage(Component.text(
								"The shard is currently closed for maintenance. Please try again later.")
							.color(NamedTextColor.RED));
						try {
							ShardSorterCommand.sortToShard(player, "valley", null);
						} catch (Exception e) {
							player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 15, 1, false, false));
							MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), e);
						}
					}
				}
			}.runTaskTimer(mPlugin, 0, 40);
		} else if (ServerProperties.getShardOpen() && mContentRunnable != null) {
			mContentRunnable.cancel();
			mContentRunnable = null;
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		contentLockCheckOnReload();

		if (!ServerProperties.getJoinMessagesEnabled()) {
			event.joinMessage(null);
		}

		String shardName = ServerProperties.getShardName();
		if (shardName.startsWith("valley") || shardName.startsWith("isles") || shardName.startsWith("ring")) {
			if (player.getScoreboardTags().contains("resetMessage")) {
				// It's after weekly update on an overworld shard; check for smuggled items/state
				InventoryUtils.removeSpecialItems(event.getPlayer(), false, true, false);
			}
		}

		if (!player.isOp() && player.getGameMode() == GameMode.CREATIVE) {
			ZoneUtils.setExpectedGameMode(player);
			MonumentaNetworkRelayIntegration.sendAdminMessage(player.getName() + " logged in in creative mode despite not being opped.");
		}

		/* This needs to stick around basically forever to remove this no-longer-needed tag */
		player.removeScoreboardTag("MidTransfer");

		mPlugin.mTrackingManager.addEntity(player);
		mPlugin.mCosmeticsManager.playerJoinEvent(event);
		POIManager.getInstance().playerJoinEvent(event);
		mPlugin.mAbilityManager.playerJoinEvent(player, event);

		DailyReset.handle(player);

		ParticleManager.updateParticleSettings(player);

		// add player to the players team (and create the team if it doesn't exist already)
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
			Team playersTeam = scoreboard.getTeam(PLAYERS_TEAM_NAME);
			if (playersTeam == null) {
				playersTeam = scoreboard.registerNewTeam(PLAYERS_TEAM_NAME);
				playersTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
				playersTeam.setCanSeeFriendlyInvisibles(false);
			}
			if (!playersTeam.hasEntry(player.getName())) {
				playersTeam.addEntry(player.getName());
			}
		}, 1);

		// transform old NO_SELF_PARTICLES tag into new partial particle option
		// can be removed after a while
		if (ScoreboardUtils.checkTag(player, Constants.Tags.NO_SELF_PARTICLES)) {
			player.removeScoreboardTag(Constants.Tags.NO_SELF_PARTICLES);
			ScoreboardUtils.setScoreboardValue(player, Objects.requireNonNull(ParticleCategory.OWN_PASSIVE.mObjectiveName), 0);
		}

		//TODO: Remove this when custom effects logout handling is better dealt with
		EntityUtils.applyRecoilDisable(mPlugin, 9999, 99, player);


		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			if (ClientModHandler.playerHasClientMod(player)) {
				ClientModHandler.updateEffects(player);
			}
		}, 20);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerChannelEvent(PlayerRegisterChannelEvent event) {
		if (ClientModHandler.CHANNEL_ID.equals(event.getChannel())
			&& event.getPlayer().isOnline()) {
			// Check for isOnline() as this event sometimes gets called before the join event,
			// and updating client mod state causes initialisation of player abilities, which must not happen before login or some abilities break.
			// Also, cannot just remove this, as sometimes the channel is registered after login, so abilities are already instantiated and not sent to the player.
			Bukkit.getScheduler().runTask(mPlugin, () -> {
				Player player = event.getPlayer();
				ClientModHandler.sendLocationPacket(player, ServerProperties.getShardName());
				ClientModHandler.updateAbilities(player);
			});
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		if (!ServerProperties.getJoinMessagesEnabled()) {
			event.quitMessage(Component.empty());
		}

		Player player = event.getPlayer();

		/* Remove portals from player */
		PortalManager.clearAllPortals(player);

		/* Remove ephemeral items on logout */
		InventoryUtils.removeSpecialItems(player, true, true);

		mPlugin.mAbilityManager.playerQuitEvent(player, event);

		mPlugin.mTrackingManager.removeEntity(player);

		Gui.playerQuit(player);

		Team playersTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(PLAYERS_TEAM_NAME);
		if (playersTeam != null) {
			playersTeam.removeEntry(player.getName());
		}

		ParticleManager.removeParticleSettings(player);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerSaveEvent(PlayerSaveEvent event) {
		Player player = event.getPlayer();
		mPlugin.mAbilityManager.playerSaveEvent(player, event);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void playerChangeWorldEvent(PlayerChangedWorldEvent event) {
		InventoryUtils.removeSpecialItems(event.getPlayer(), false, true, false);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void playerInteractEvent(PlayerInteractEvent event) {
		if (event.useInteractedBlock() == Event.Result.DENY && event.useItemInHand() == Event.Result.DENY) {
			return;
		}
		Action action = event.getAction();
		Player player = event.getPlayer();
		ItemStack item = event.getItem();
		Block block = event.getClickedBlock();
		Material mat = (block != null) ? block.getType() : Material.AIR;

		// Plot Security: If block is in a plot but the player is in adventure, cancel.
		if (block != null && player.getGameMode() == GameMode.ADVENTURE && ZoneUtils.isInPlot(block.getLocation())) {
			event.setCancelled(true);
			return;
		}

		BlockData blockData;
		if (block == null) {
			blockData = null;
		} else {
			blockData = block.getBlockData();
		}

		if (player.getGameMode() != GameMode.CREATIVE && block != null && !(blockData instanceof Powerable) && ZoneUtils.hasZoneProperty(block.getLocation(), ZoneProperty.RESTRICTED)) {
			event.setCancelled(true);
			event.setUseInteractedBlock(Event.Result.DENY);
			return;
		}

		if (player.getGameMode() != GameMode.CREATIVE && block != null && blockData instanceof Powerable && ZoneUtils.hasZoneProperty(block.getLocation(), ZoneProperty.DISABLE_REDSTONE_INTERACTIONS)) {
			event.setCancelled(true);
			event.setUseInteractedBlock(Event.Result.DENY);
			return;
		}

		mPlugin.mAbilityManager.playerInteractEvent(event, mat);
		mPlugin.mItemStatManager.onPlayerInteract(mPlugin, player, event);

		// Overrides
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			if (item != null) {
				mPlugin.mItemOverrides.leftClickInteraction(mPlugin, player, action, item, block, event);
				if (event.useInteractedBlock() == Event.Result.DENY && event.useItemInHand() == Event.Result.DENY) {
					return;
				}
			}
		} else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			mPlugin.mItemOverrides.rightClickInteraction(mPlugin, player, action, item, block, event);
			if (event.useInteractedBlock() == Event.Result.DENY && event.useItemInHand() == Event.Result.DENY) {
				return;
			}
		} else if (action == Action.PHYSICAL) {
			mPlugin.mItemOverrides.physicalBlockInteraction(mPlugin, player, action, block, event);
				if (event.useInteractedBlock() == Event.Result.DENY) {
				return;
			}
		}

		// Item Interactions
		if (event.useItemInHand() != Event.Result.DENY) {
			if (ItemUtils.isArmor(item)) {
				InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);
			}
		}

		// Block Interactions
		if (event.useInteractedBlock() != Event.Result.DENY) {
			if (block != null) {
				if ((player.getGameMode() == GameMode.ADVENTURE
					&& ZoneUtils.isInPlot(block.getLocation()))) {
					event.setUseInteractedBlock(Event.Result.DENY);
					return;
				}
			}
		}

		// Immediately load crossbows when the right mouse button is held down for long enough instead of only when released.
		// Also send an inventory update when a crossbow is released.
		// This prevents "ghost" loading crossbows, where the client thinks the button was held for long enough, but the server disagrees.
		if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
			&& event.useItemInHand() != Event.Result.DENY
			&& item != null
			&& item.getType() == Material.CROSSBOW
			&& item.getItemMeta() instanceof CrossbowMeta meta
			&& meta.getChargedProjectiles().isEmpty()) {
			int slot = player.getInventory().getHeldItemSlot();
			new BukkitRunnable() {
				@Override
				public void run() {
					if (player.isDead() || !player.isOnline()) {
						this.cancel();
						return;
					}

					if (slot != player.getInventory().getHeldItemSlot()
						|| !item.equals(player.getActiveItem())
						|| !player.isHandRaised()) {
						this.cancel();
						player.updateInventory();
						return;
					}
					if (player.getItemUseRemainingTime() <= 0) {
						this.cancel();
						if (player.getItemUseRemainingTime() <= 0 && player.getItemUseRemainingTime() >= -1) {
							NmsUtils.getVersionAdapter().releaseActiveItem(player, false);
						}
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}

		// Update inventory to handle virtual removal of depth strider while riptiding
		if (player.getScoreboardTags().contains(Constants.Tags.DEPTH_STRIDER_DISABLED_ONLY_WHILE_RIPTIDING)
			&& (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
			&& event.useItemInHand() != Event.Result.DENY
			&& item != null
			&& item.getType() == Material.TRIDENT
			&& item.containsEnchantment(Enchantment.RIPTIDE)
			&& playerHasDepthStrider(player)) {

			Bukkit.getScheduler().runTask(mPlugin, () -> {
				PlayerUtils.resendItems(player,
					Stream.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD)
						.filter(slot -> player.getEquipment().getItem(slot).containsEnchantment(Enchantment.DEPTH_STRIDER))
						.toArray(EquipmentSlot[]::new));
			});

			// Update inventory again after riptiding (or aborting) to re-add the removed depth strider
			if (player.getScoreboardTags().contains(Constants.Tags.DEPTH_STRIDER_DISABLED_ONLY_WHILE_RIPTIDING) && playerHasDepthStrider(player)) {
				new BukkitRunnable() {
					@Override
					public void run() {
						if (!player.isOnline() || player.isDead()) {
							cancel();
							return;
						}
						if (!player.isHandRaised() && !player.isRiptiding()) {
							PlayerUtils.resendItems(player,
								Stream.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD)
									.filter(slot -> player.getEquipment().getItem(slot).containsEnchantment(Enchantment.DEPTH_STRIDER))
									.toArray(EquipmentSlot[]::new));
							cancel();
						}
					}
				}.runTaskTimer(mPlugin, 2, 1);
			}
		}

	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		ItemStack item = event.getItemInHand();
		Player player = event.getPlayer();
		Block block = event.getBlock();

		if (!mPlugin.mItemOverrides.blockPlaceInteraction(mPlugin, player, item, event)) {
			event.setCancelled(true);
			return;
		}

		// Prevent players in survival mode placing blocks in adventure mode zones not including plots
		if (player.getGameMode() == GameMode.SURVIVAL && !ZoneUtils.playerCanMineBlock(player, block)) {
			event.setCancelled(true);
			return;
		}

		// Only allow placing monument wools on Monuments
		if (player.getGameMode() == GameMode.ADVENTURE && ItemUtils.isWool(block.getType()) && !ZoneUtils.hasZoneProperty(block.getLocation(), ZoneProperty.MONUMENT)) {
			event.setCancelled(true);
			return;
		}

		if (ZoneUtils.hasZoneProperty(player.getLocation(), ZoneProperty.NO_PLACING_CONTAINERS)) {
			Material material = block.getType();
			if (material == Material.CHEST || material == Material.BARREL || (ItemUtils.isShulkerBox(material) && !FirmamentOverride.isFirmamentItem(item) && !WorldshaperOverride.isWorldshaperItem(item))) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockPlaceEventMonitor(BlockPlaceEvent event) {
		// If replacing a light block in survival mode, place new light blocks around the replaced block to not suddenly make the area much darker
		if (event.getPlayer().getGameMode() == GameMode.SURVIVAL
			&& event.getBlockReplacedState().getType() == Material.LIGHT
			&& event.getBlockReplacedState().getBlockData() instanceof Light light
			&& light.getLevel() > light.getMinimumLevel()) {
			Light lowerAirLight = (Light) Bukkit.createBlockData(Material.LIGHT);
			lowerAirLight.setLevel(light.getLevel() - 1);
			Light lowerWaterLight = (Light) Bukkit.createBlockData(Material.LIGHT);
			lowerWaterLight.setLevel(light.getLevel() - 1);
			lowerWaterLight.setWaterlogged(true);
			for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN}) {
				Block relative = event.getBlock().getRelative(face);
				if (!ZoneUtils.playerCanMineBlock(event.getPlayer(), relative)) {
					continue;
				}
				if (relative.getType() == Material.AIR) {
					relative.setBlockData(lowerAirLight, false);
				} else if (relative.getType() == Material.WATER
					&& relative.getBlockData() instanceof Levelled fluid
					&& fluid.getLevel() == 0) { // 0 == full block of water
					relative.setBlockData(lowerWaterLight, false);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void signChangeEvent(SignChangeEvent event) {
		Player player = event.getPlayer();
		Location loc = event.getBlock().getLocation();
		Component allSignLines = MessagingUtils.concatenateComponents(event.lines(), Component.newline());
		if (MonumentaNetworkChatIntegration.hasBadWord(player, allSignLines)) {
			AuditListener.logSevere(player.getName()
				+ " attempted to place a sign with a bad word: `/s "
				+ ServerProperties.getShardName()
				+ "` `/world " + loc.getWorld().getName()
				+ "` `/tp @s " + loc.getBlockX()
				+ " " + loc.getBlockY()
				+ " " + loc.getBlockZ()
				+ "`"
			);
			event.setCancelled(true);
			if (event.getBlock().getState() instanceof Sign sign) {
				int lineNum = 0;
				for (Component oldLine : sign.lines()) {
					event.line(lineNum, oldLine);
					lineNum++;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void playerInteractEntityEventWithCancelled(PlayerInteractEntityEvent event) {

		// Need to ignore the left click that follows this right click, thus handle cancelled events too
		mPlugin.mAbilityManager.playerInteractEntityEvent(event);

	}

	// Player interacts with an entity (not triggered on armor stands, as those only trigger a PlayerInteractAtEntityEvent)
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		Player player = event.getPlayer();

		/* Don't let the player do this when in a restricted zone */
		if (ZoneUtils.hasZoneProperty(player, ZoneProperty.RESTRICTED) && player.getGameMode() != GameMode.CREATIVE) {
			event.setCancelled(true);
			return;
		}

		Entity clickedEntity = event.getRightClicked();
		ItemStack itemInHand = player.getEquipment().getItemInMainHand();
		if (event.getHand().equals(EquipmentSlot.OFF_HAND)) {
			itemInHand = player.getEquipment().getItemInOffHand();
		}

		//Prevent feeding special item lore items to animals (specifically horses)
		if (clickedEntity instanceof Animals && itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasLore()) {
			event.setCancelled(true);
			return;
		} else if (clickedEntity instanceof ItemFrame frame) {

			// Plot Security: If item frame is in a plot but the player is in adventure, cancel.
			if (player.getGameMode() == GameMode.ADVENTURE && ZoneUtils.isInPlot(frame)) {
				event.setCancelled(true);
				return;
			}

			if (frame.isInvulnerable()) {
				if (player.getGameMode().equals(GameMode.CREATIVE)) {
					player.sendMessage(Component.text("This item frame is invulnerable / creative only", NamedTextColor.GOLD));
				} else {
					event.setCancelled(true);
				}

				ItemStack frameItem = frame.getItem();
				if (frameItem.getType().equals(Material.FILLED_MAP)) {
					if (player.getGameMode().equals(GameMode.ADVENTURE)) {
						ItemStack giveMap = frameItem.clone();
						ItemMeta mapMeta;

						if (giveMap.hasItemMeta()) {
							mapMeta = giveMap.getItemMeta();
						} else {
							mapMeta = Bukkit.getServer().getItemFactory().getItemMeta(Material.FILLED_MAP);
						}

						List<Component> mapLore = mapMeta.lore();
						if (mapLore == null) {
							mapLore = new ArrayList<>(1);
						}
						mapLore.add(Component.text("* Official Map *", NamedTextColor.GOLD));
						mapMeta.lore(mapLore);
						giveMap.setItemMeta(mapMeta);
						ItemUtils.setPlainLore(giveMap);
						InventoryUtils.giveItem(player, giveMap);
					}
				}
			}

			if (!event.isCancelled()
				&& EntityListener.INVISIBLE_ITEM_FRAME_NAME.equals(frame.getName())) {
				Bukkit.getScheduler().runTask(mPlugin, () -> {
					if (frame.isValid()) {
						new NBTEntity(frame).setBoolean("Invisible", !ItemUtils.isNullOrAir(frame.getItem()));
					}
				});
			}
		}

		if (!mPlugin.mItemOverrides.rightClickEntityInteraction(mPlugin, player, clickedEntity, itemInHand)) {
			event.setCancelled(true);
		}
	}

	// Player interacts with an armor stand
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent event) {
		Player player = event.getPlayer();
		ArmorStand armorStand = event.getRightClicked();

		/* Don't let the player do this in a restricted zone */
		if (ZoneUtils.hasZoneProperty(player, ZoneProperty.RESTRICTED) && player.getGameMode() != GameMode.CREATIVE) {
			event.setCancelled(true);
			return;
		}

		// Plot Security: If armor stand is in a plot but the player is in adventure, cancel.
		if (player.getGameMode() == GameMode.ADVENTURE && ZoneUtils.isInPlot(armorStand)) {
			event.setCancelled(true);
		}
	}

	// The Player swapped their current selected item.
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerItemHeldEvent(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);

		// Update inventory if switching to or from a riptide trident while having depth strider to virtually remove that depth strider
		ItemStack previousItem = player.getInventory().getItem(event.getPreviousSlot());
		ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
		if (((previousItem != null && previousItem.containsEnchantment(Enchantment.RIPTIDE)) || (newItem != null && newItem.containsEnchantment(Enchantment.RIPTIDE)))
			&& playerHasDepthStrider(player)) {
			Bukkit.getScheduler().runTask(mPlugin, player::updateInventory);
		}
	}

	private boolean playerHasDepthStrider(Player player) {
		return (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
			&& Stream.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD).anyMatch(slot -> player.getEquipment().getItem(slot).containsEnchantment(Enchantment.DEPTH_STRIDER));
	}

	// The player dropped an item.
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerDropItemEvent(PlayerDropItemEvent event) {
		Player player = event.getPlayer();

		/* Don't let the player do this when in a restricted zone */
		if (ZoneUtils.hasZoneProperty(player, ZoneProperty.RESTRICTED) && player.getGameMode() != GameMode.CREATIVE) {
			event.setCancelled(true);
			return;
		}
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer(), event);
	}

	// An entity picked up an item
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void entityPickupItemEvent(EntityPickupItemEvent event) {
		if (event.getEntity() instanceof Player player) {

			/* Don't let the player do this when in a restricted zone */
			if (ZoneUtils.hasZoneProperty(player, ZoneProperty.RESTRICTED) && player.getGameMode() != GameMode.CREATIVE) {
				event.setCancelled(true);
				return;
			}

			/* Don't let players pick up items that have already been processed for shattering */
			if (event.getItem().getScoreboardTags().contains("ShatteringNoPickup")) {
				event.setCancelled(true);
				return;
			}

			// If the item contains lore, schedule an equipment update
			if (event.getItem().getItemStack().getItemMeta().hasLore()) {
				InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);
			}

			/* Mark the item so it won't get shattered later */
			event.getItem().addScoreboardTag("ShatterProcessed");
		} else {
			/* Non-player entities don't get to pick up items */
			event.setCancelled(true);
		}
	}

	// An item on the player breaks.
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerItemBreakEvent(PlayerItemBreakEvent event) {
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer(), event);
	}

	// If an inventory interaction happened.
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}

		ItemStack item = event.getCurrentItem();

		// If item contains curse of ephemerality, prevent from putting in other inventories
		// Checks for player inventory unless it's a shift click
		if (!event.getWhoClicked().getGameMode().equals(GameMode.CREATIVE) &&
			(
				// Prevent shift-clicking an ephemeral item from your inventory to something else
				(
					(event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT)
						&& item != null
						&& CurseOfEphemerality.isEphemeral(item)
						&& event.getView().getTopInventory().getType() != InventoryType.CRAFTING // Allow shift-click between inventory and hotbar
						&& event.getClickedInventory() instanceof PlayerInventory
				)
					// Prevent clicking an ephemeral item from your cursor down into something else
					|| (
					event.getClick() != ClickType.SHIFT_LEFT
						&& event.getClick() != ClickType.SHIFT_RIGHT
						&& event.getCursor() != null
						&& CurseOfEphemerality.isEphemeral(event.getCursor())
						&& !(event.getClickedInventory() instanceof PlayerInventory)
				)
					// Prevent moving ephemeral item using number keys
					|| (
					event.getClick() == ClickType.NUMBER_KEY
						&& CurseOfEphemerality.isEphemeral(player.getInventory().getItem(event.getHotbarButton()))
						&& !(event.getClickedInventory() instanceof PlayerInventory)
				)
					// Prevent moving ephemeral item using swap offhand keys
					|| (
					event.getClick() == ClickType.SWAP_OFFHAND
						&& CurseOfEphemerality.isEphemeral(player.getInventory().getItemInOffHand())
						&& !(event.getClickedInventory() instanceof PlayerInventory)
				)
			)
		) {
			event.setCancelled(true);
			GUIUtils.refreshOffhand(event);
			return;
		}

		// Apply Gear Changed effect briefly if the clicked slot is one of armor.
		if (event.getSlot() >= 36) {
			EffectManager.getInstance().addEffect(player, GearChanged.effectID, new GearChanged(GearChanged.DURATION));
		}

		if (player.getGameMode() != GameMode.CREATIVE
			&& event.getClickedInventory() == player.getInventory()
			&& 36 <= event.getSlot() && event.getSlot() <= 40
			&& (ItemUtils.isNullOrAir(event.getCursor())
			|| event.getClick().isShiftClick()
			|| event.getClick() == ClickType.UNKNOWN
			|| (event.getClick() == ClickType.NUMBER_KEY && ItemUtils.isNullOrAir(player.getInventory().getItem(event.getHotbarButton()))))
			&& (player.getLocation().getY() < player.getWorld().getMinHeight() || EntityUtils.touchesLava(player))) {
			player.sendMessage(Component.text("Unequipping gear in lava or void is not allowed!", NamedTextColor.RED));
			event.setCancelled(true);
			GUIUtils.refreshOffhand(event);
			return;
		}

		if (!mPlugin.mItemOverrides.inventoryClickInteraction(mPlugin, player, event) || !mPlugin.mItemOverrides.inventoryClickEvent(mPlugin, player, event)) {
			event.setCancelled(true);
			GUIUtils.refreshOffhand(event);
		}

		// If right-clicking charm, open GUI
		if (event.getClick() == ClickType.RIGHT
			&& item != null
			&& item.getAmount() == 1
			&& ItemUtils.isNullOrAir(event.getCursor())) {
			for (CharmManager.CharmType charmType : CharmManager.CharmType.values()) {
				if (charmType.isCharm(item)) {
					new CharmsGUI(player, charmType).open();
					break;
				}
			}
		}

		if (event.getClick() == ClickType.SWAP_OFFHAND
			&& event.getClickedInventory() == player.getInventory()
			&& ItemUtils.isNullOrAir(event.getCursor())
			&& ItemStatUtils.hasEnchantment(item, EnchantmentType.MULTITOOL)) {
			Multitool.swap(mPlugin, (Player) event.getWhoClicked(), item);
			GUIUtils.refreshOffhand(event);
		}

		if (item != null && item.getType() == Material.WRITTEN_BOOK && item.getAmount() == 1 && ItemUtils.isNullOrAir(event.getCursor()) && event.getClick() == ClickType.RIGHT) {
			player.openBook(item);
			player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1, 1);
			event.setCancelled(true);
		}

	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void inventoryClickEventHighest(InventoryClickEvent event) {
		if (event.getClick() == ClickType.SWAP_OFFHAND
			&& event.getWhoClicked().getScoreboardTags().contains(ToggleSwap.SWAP_INVENTORY_TAG)) {
			event.setCancelled(true);
			GUIUtils.refreshOffhand(event);
		}
	}

	// If an item is being dragged in an inventory
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void inventoryDragEvent(InventoryDragEvent event) {
		if (event.getWhoClicked() instanceof Player player) {
			InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);

			//If item contains curse of ephemerality, prevent from putting in other inventories
			if (player.getGameMode() != GameMode.CREATIVE && !(event.getInventory() instanceof PlayerInventory)) {
				if (event.getCursor() != null && CurseOfEphemerality.isEphemeral(event.getCursor())) {
					event.setCancelled(true);
				} else {
					for (Map.Entry<Integer, ItemStack> iter : event.getNewItems().entrySet()) {
						if (CurseOfEphemerality.isEphemeral(iter.getValue())) {
							event.setCancelled(true);
							return;
						}
					}
				}
			}
		}
	}

	// The player opened an inventory
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void inventoryOpenEvent(InventoryOpenEvent event) {
		if (event.getPlayer() instanceof Player player) {

			/* Don't let the player do this when in a restricted zone */
			if (ZoneUtils.hasZoneProperty(player, ZoneProperty.RESTRICTED)
				&& player.getGameMode() != GameMode.CREATIVE
				&& player.getGameMode() != GameMode.SPECTATOR) {
				event.setCancelled(true);
			}
		}
	}

	// ...Because there's a known bug with the stupid Item Property stuff and the InventoryClickEvent stuff...
	// The player inventory is closed
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		Inventory inventory = event.getInventory();
		InventoryHolder holder = inventory.getHolder();
		if (holder instanceof Chest chest) {
			// Break Halloween creeper chests in safe zones automatically when closed
			if (ChestUtils.isEmpty(chest) && (chest.customName() != null && MessagingUtils.plainText(chest.customName()).contains("Creeperween Chest"))) {
				chest.getBlock().breakNaturally();
			}
		}
		if (event.getPlayer() instanceof Player player) {
			InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);

			// Workaround for 1.17 clients not having proper inventory state after cancelling click events
			// (when opening a new inventory after a click event is canceled, a "ghost" item is on the cursor)
			Bukkit.getScheduler().runTaskLater(mPlugin, player::updateInventory, 1);
		}
	}

	// Something interacts with an inventory
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void inventoryInteractEvent(InventoryInteractEvent event) {
		if (event.getWhoClicked() instanceof Player player) {

			/* Don't let the player do this when in a restricted zone */
			if (ZoneUtils.hasZoneProperty(player, ZoneProperty.RESTRICTED)
				&& player.getGameMode() != GameMode.CREATIVE
				&& player.getGameMode() != GameMode.SPECTATOR) {
				event.setCancelled(true);
				return;
			}
			InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void playerEditBookEvent(PlayerEditBookEvent event) {
		Player player = event.getPlayer();
		Location loc = player.getLocation();
		BookMeta bookMeta = event.getNewBookMeta();

		String title = bookMeta.getTitle();
		if (title != null) {
			if (MonumentaNetworkChatIntegration.hasBadWord(player, title)) {
				AuditListener.logSevere(player.getName()
					+ " attempted to title a book with a bad word: `/s "
					+ ServerProperties.getShardName()
					+ "` `/world " + loc.getWorld().getName()
					+ "` `/tp @s " + loc.getBlockX()
					+ " " + loc.getBlockY()
					+ " " + loc.getBlockZ()
					+ "`"
				);
				event.setCancelled(true);
				event.setNewBookMeta(event.getPreviousBookMeta());
				return;
			}
		}

		for (Component page : bookMeta.pages()) {
			if (MonumentaNetworkChatIntegration.hasBadWord(player, page)) {
				AuditListener.logSevere(player.getName()
					+ " attempted to update a book with a bad word: `/s "
					+ ServerProperties.getShardName()
					+ "` `/world " + loc.getWorld().getName()
					+ "` `/tp @s " + loc.getBlockX()
					+ " " + loc.getBlockY()
					+ " " + loc.getBlockZ()
					+ "`"
				);
				event.setCancelled(true);
				event.setNewBookMeta(event.getPreviousBookMeta());
				return;
			}
		}
	}

	// Player ran a command
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerCommandPreprocessEvent(PlayerCommandPreprocessEvent event) {
		final String NBT_EDITOR_OPTIONAL_PREFIX = "nbteditor:";

		Player player = event.getPlayer();
		String command = event.getMessage().substring(1);

		if (command.startsWith(NBT_EDITOR_OPTIONAL_PREFIX)) {
			command = command.substring(NBT_EDITOR_OPTIONAL_PREFIX.length());
		}

		if (command.startsWith("nbte")
			|| command.startsWith("nbti")
			|| command.startsWith("nbtp")) {
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				ItemStack item = player.getEquipment().getItemInMainHand();
				if (item.getAmount() > 0) {
					ItemUtils.setPlainTag(item);
					player.getEquipment().setItemInMainHand(item, true);
				}
			}, 0);
		}
	}

	// Player changed hand items
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerChangedMainHandEvent(PlayerChangedMainHandEvent event) {
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer(), event);
	}

	// Player swapped hand items
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (mPlugin.mItemOverrides.swapHandsInteraction(mPlugin, event.getPlayer())) {
			event.setCancelled(true);
		} else {
			mPlugin.mAbilityManager.playerSwapHandItemsEvent(event.getPlayer(), event);
			mPlugin.mItemStatManager.onPlayerSwapHands(mPlugin, event.getPlayer(), event);
		}
		if (event.getPlayer().getScoreboardTags().contains(ToggleSwap.SWAP_TAG)) {
			event.setCancelled(true);
		}
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer(), event);
	}

	// The player has died
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();

		if (player.getHealth() > 0) {
			return;
		}

		InventoryUtils.removeSpecialItems(player, true, false);

		// Debuff mobs around player in dungeon if running solo
		if (Plugin.IS_PLAY_SERVER && ScoreboardUtils.getScoreboardValue("$IsDungeon", "const").orElse(0) == 1) {
			if (PlayerUtils.otherPlayersInRange(player, 64, true).isEmpty()) {
				// Delay by a tick to allow the entity that killed the player (if any) to still be valid until all damage/death events resolve
				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
					List<LivingEntity> nearbyEntities = EntityUtils.getNearbyMobs(player.getLocation(), 20);
					for (LivingEntity entity : nearbyEntities) {
						EntityUtils.applySlow(mPlugin, 5 * 60 * 20, .1, entity, SOLO_DEATH_MOB_SLOW_EFFECT_NAME);
						EntityUtils.applyWeaken(mPlugin, 5 * 60 * 20, .25, entity, null, SOLO_DEATH_MOB_WEAKEN_EFFECT_NAME);
					}
				});
			}
		}

		// Prevent an inescapable death loop by overriding KeepInventory if your Max Health is 0
		if (event.getKeepInventory()) {
			AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
			if (maxHealth != null && maxHealth.getValue() <= 0) {
				event.setKeepInventory(false);
			}
		}

		mPlugin.mAbilityManager.playerDeathEvent(player, event);
		mPlugin.mItemStatManager.onDeath(mPlugin, player, event);

		// Give the player a NewDeath score of 1 so the city guides will give items again
		ScoreboardUtils.setScoreboardValue(player, "NewDeath", 1);

		Component deathMessage = event.deathMessage();
		if (deathMessage != null && ScoreboardUtils.getScoreboardValue(player, Constants.SCOREBOARD_DEATH_MESSAGE).orElse(0) != 0) {
			player.sendMessage(deathMessage);
			if (mPlugin.mAuditListener != null) {
				// The audit won't work later if the death message is null
				mPlugin.mAuditListener.death(event);
			}
			player.sendMessage(Component.text("Only you saw this message. Change this with /deathmsg", NamedTextColor.AQUA));
			event.deathMessage(null);
		}
		// Don't repeat if they died in the last 5 ticks
		if (!MetadataUtils.checkOnceInRecentTicks(mPlugin, player, PLAYER_DEATH_TICK_TAG, 5)) {
			event.deathMessage(null);
			// Clear effects
			mPlugin.mPotionManager.clearAllPotions(player);
			mPlugin.mAbilityManager.updatePlayerAbilities(player, true);
		}
	}

	// The player has respawned.
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerRespawnEvent(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		World world = event.getRespawnLocation().getWorld();

		Location respawnLocation = PlayerUtils.getRespawnLocationAndClear(player, world, event.getRespawnLocation());
		event.setRespawnLocation(respawnLocation);

		Bukkit.getScheduler().runTask(mPlugin, () -> {
			if (player.isOnline()) {
				mPlugin.mPotionManager.clearAllPotions(player);
				mPlugin.mAbilityManager.updatePlayerAbilities(player, true);

				InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);
			}
		});

		Phylactery.applyStoredEffects(mPlugin, player);
		mPlugin.mEffectManager.applyEffectsOnRespawn(mPlugin, player);

		mPlugin.mEffectManager.addEffect(player, RespawnStasis.NAME,
			new RespawnStasis(player.getLocation(), event.getRespawnLocation()));
		player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 1, 0.75f);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerPostRespawnEvent(PlayerPostRespawnEvent event) {
		// Teleport the player to the respawn location as vanilla might have moved the player again after the respawn event
		// (e.g. due to intersecting unbreakable blocks or solid entities like boats).
		event.getPlayer().teleport(event.getRespawnedLocation());
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerFishEvent(PlayerFishEvent event) {
		Player player = event.getPlayer();
		if (event.getState() == State.FISHING) {
			mPlugin.mTrackingManager.mFishingHook.addEntity(player, event.getHook());
		} else if (event.getState() == State.CAUGHT_ENTITY || event.getState() == State.CAUGHT_FISH) {
			mPlugin.mTrackingManager.mFishingHook.removeEntity(player);

			if (event.getState() == State.CAUGHT_ENTITY && !EntityUtils.isHostileMob(event.getCaught())) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerItemConsumeEvent(PlayerItemConsumeEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getItem();

		if (ItemStatUtils.isCharm(item)) {
			event.setCancelled(true);
			return;
		}

		if (item.getItemMeta() instanceof PotionMeta potionMeta) {
			if (ItemStatUtils.hasConsumeEffect(item)) {
				// If it's a custom potion, remove all effects
				potionMeta.clearCustomEffects();
				potionMeta.setBasePotionData(new PotionData(PotionType.AWKWARD));
			} else {
				// If it's a vanilla potion, remove positive effects (Ensure legacies stay unusable)
				PotionUtils.removePositiveEffects(potionMeta);
				if (PotionUtils.hasPositiveEffects(potionMeta.getBasePotionData().getType().getEffectType())) {
					// If base potion is vanilla positive potion, set to AWKWARD, otherwise keep (ensures negative effects remain)
					potionMeta.setBasePotionData(new PotionData(PotionType.AWKWARD));
				}
			}
			item.setItemMeta(potionMeta);
			event.setItem(item);
		}

		if (!mPlugin.mItemOverrides.playerItemConsume(mPlugin, player, event)) {
			event.setCancelled(true);
			return;
		}

		mPlugin.mAbilityManager.playerItemConsumeEvent(player, event);

		ItemStatUtils.applyCustomEffects(mPlugin, player, item);

		mPlugin.mItemStatManager.onConsume(mPlugin, player, event);

		if (item.containsEnchantment(Enchantment.ARROW_INFINITE)) {
			// Stat tracker for consuming infinity items
			// Needs to update the player's active item, not the event item, as that is a copy.
			ItemStack activeItem = player.getActiveItem();
			StatTrackManager.getInstance().incrementStatImmediately(activeItem, player, InfusionType.STAT_TRACK_CONSUMED, 1);

			// Set replacement to a copy of the original, so it is not consumed (must be a copy as the internal code checks for reference equality)
			event.setReplacement(ItemUtils.clone(activeItem));
		}

		for (PotionEffect effect : PotionUtils.getEffects(item)) {
			// Kill the player if they drink a potion with instant damage 10+
			PotionEffectType effectType = effect.getType();
			if (effectType.equals(PotionEffectType.HARM) && effect.getAmplifier() >= 9) {

				player.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, () -> player.setHealth(0), 0);
			} else if (effectType.equals(PotionEffectType.SLOW_FALLING)) {
				//Remove Slow Falling effects
				player.sendMessage(Component.text("You cannot apply slow falling potion effects, other effects were still applied.", NamedTextColor.RED));
				player.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, () -> player.removePotionEffect(PotionEffectType.SLOW_FALLING), 1);
			}
		}
	}

	// An item has taken damage.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerItemDamageEvent(PlayerItemDamageEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getItem();

		if (ItemStatUtils.isCharm(item) || ServerProperties.getIsTownWorld()) {
			event.setCancelled(true);
			return;
		}

		mPlugin.mAbilityManager.playerItemDamageEvent(player, event);

		int oldDamage = event.getDamage();
		int newDamage = oldDamage;

		// Vanilla hoes and axes take 2 durability, we only want them to take 1
		if ((ItemUtils.isHoe(item) || ItemUtils.isAxe(item)) && oldDamage > 1) {
			newDamage = oldDamage / 2;
		}

		if (ItemUtils.isArmor(item) || item.getType() == Material.TRIDENT) {
			// Armor and tridents do not take durability damage in No Equipment Damage zones
			if (oldDamage < 0 || ZoneUtils.hasZoneProperty(player, ZoneProperty.NO_EQUIPMENT_DAMAGE)) {
				newDamage = 0;
			}
		}

		event.setDamage(newDamage);
		mPlugin.mItemStatManager.onItemDamage(mPlugin, player, event);

		// Low durability notification code
		Material mat = item.getType();
		if (item.getItemMeta() instanceof Damageable dMeta) {
			int itemDamage = dMeta.getDamage();
			if (itemDamage < mat.getMaxDurability() * 0.9 && itemDamage + newDamage >= mat.getMaxDurability() * 0.9) {
				World world = player.getWorld();
				Location loc = player.getLocation();
				world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1, 0.45f);
				world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1, 0.25f);
				BlockData fallingDustData = Material.ANVIL.createBlockData();
				new PartialParticle(Particle.FALLING_DUST, loc.add(0, 1, 0), 20,
					1.1, 0.6, 1.1, fallingDustData).spawnAsPlayerActive(player);
				Component itemName = ItemUtils.getDisplayName(item).decoration(TextDecoration.UNDERLINED, false);
				String translatedMessage = TranslationsManager.translate(player, "Your %s is about to break!");
				Component message = Component.text(translatedMessage).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
					.replaceText(TextReplacementConfig.builder().matchLiteral("%s").replacement(itemName).build());
				player.sendMessage(message);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerRiptideEvent(PlayerRiptideEvent event) {
		Player player = event.getPlayer();
		Location loc = player.getLocation();

		//Manually forces the player in place during the riptide if they use it out of water (in rain) or have the riptide disable effect
		if (StasisListener.isInStasis(player) || !mPlugin.mItemOverrides.playerRiptide(mPlugin, player, event)) {
			player.teleport(loc);
			player.setCooldown(Material.TRIDENT, 15 * 20);
			new BukkitRunnable() {
				@Override
				public void run() {
					if (!player.isRiptiding() || !player.isOnline() || player.isDead()) {
						this.cancel();
						return;
					}
					player.setVelocity(player.getVelocity().multiply(0));
				}
			}.runTaskTimer(mPlugin, 0, 2);
			return;
		}

		mPlugin.mItemStatManager.onRiptide(mPlugin, player, event);

	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerExpChangeEvent(PlayerExpChangeEvent event) {
		Player player = event.getPlayer();

		mPlugin.mItemStatManager.onExpChange(mPlugin, player, event);

		if (event.getAmount() > 0) {
			ScoreboardUtils.getScoreboardValue(player, "XpGainBonus")
				.ifPresent(value -> event.setAmount((int) Math.ceil(event.getAmount() * (1 + value / 100.0))));
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerTeleportEvent(PlayerTeleportEvent event) {
		// Cancel teleports caused by forbidden sources
		TeleportCause cause = event.getCause();

		if (cause == TeleportCause.ENDER_PEARL) {
			EnderPearlTracker.allowTeleport(event.getPlayer());
			event.setCancelled(true);
			return;
		}

		if (cause.equals(TeleportCause.CHORUS_FRUIT)
			|| cause.equals(TeleportCause.END_GATEWAY)
			|| cause.equals(TeleportCause.END_PORTAL)
			|| cause.equals(TeleportCause.NETHER_PORTAL)) {
			event.setCancelled(true);
			return;
		}

		Player player = event.getPlayer();
		if (cause == TeleportCause.SPECTATE && !player.isOp()) {
			event.setCancelled(true);
			return;
		}

		mPlugin.mAbilityManager.playerTeleportEvent(player, event);

		// If the teleport wasn't cancelled by anything, update their gamemode and other location-based info
		if (!event.isCancelled()) {
			mPlugin.mTrackingManager.mPlayers.updateLocation(player, event.getTo(), 0);
		}

		Gui gui = Gui.getOpenGui(player);
		if (gui != null && gui.getCloseOnTeleport()) {
			gui.close();
		}
	}

	private static boolean collidesWithUnbreakableBlock(World world, BoundingBox boundingBox) {
		return NmsUtils.getVersionAdapter().hasCollisionWithBlocks(world, boundingBox, true,
			mat -> mat.getHardness() < 0);
	}

	/**
	 * Cancel player movement into unbreakable blocks (e.g. bedrock or barriers).
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerMoveEvent(PlayerMoveEvent event) {
		if (event instanceof PlayerTeleportEvent
			|| event.getFrom().getWorld() != event.getTo().getWorld()) {
			// Teleports handled in teleport listener. Movement between worlds should be a teleport, but better ignore it just in case.
			return;
		}
		Player player = event.getPlayer();
		if (player.getGameMode() == GameMode.SPECTATOR) {
			// Spectators can move freely through blocks
			return;
		}

		// Don't allow moving into any blocks. Still allows moving within a block if somehow stuck in one.
		BoundingBox fromBox = player.getBoundingBox()
			.shift(player.getLocation().multiply(-1))
			.shift(event.getFrom());
		BoundingBox toBox = player.getBoundingBox()
			.shift(player.getLocation().multiply(-1))
			.shift(event.getTo());
		Set<Block> collidingBlocks = NmsUtils.getVersionAdapter().getCollidingBlocks(player.getWorld(), toBox, true);
		if (!collidingBlocks.isEmpty()
			&& !NmsUtils.getVersionAdapter().getCollidingBlocks(player.getWorld(), fromBox, true).containsAll(collidingBlocks)) {
			event.setCancelled(true);
			return;
		}

		// If moving a lot, check collision with unbreakable blocks on the way.
		// Only check a small bounding box of 0.2x0.2x0.2 for collision to reduce false positives (e.g. from moving around a corner).
		// This still prevents moving through solid walls/floors.
		if (event.getFrom().distanceSquared(event.getTo()) > 1) {
			double height = player.isSwimming() ? 0.2 : 0.6;
			BoundingBox movingBox = BoundingBox.of(event.getFrom().clone().add(-0.1, height, -0.1), event.getFrom().clone().add(0.1, height + 0.2, 0.1));
			// check collision twice per meter
			int steps = (int) Math.floor(event.getTo().distance(event.getFrom()) * 2) - 1;
			if (steps > 100) {
				// >50 meters: just no.
				event.setCancelled(true);
				return;
			}
			Vector stepDir = event.getTo().clone().subtract(event.getFrom()).toVector().normalize().multiply(0.5);
			for (int step = 0; step < steps; step++) {
				movingBox.shift(stepDir);
				if (collidesWithUnbreakableBlock(player.getWorld(), movingBox)) {
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerBedEnterEvent(PlayerBedEnterEvent event) {
		Player player = event.getPlayer();
		Block bed = event.getBed();
		Location loc = bed.getLocation();
		World world = loc.getWorld();

		/* Prevent entering beds designed to glitch through blocks */
		Material aboveMat = loc.add(0, 1, 0).getBlock().getType();
		if (aboveMat.equals(Material.BEDROCK) || aboveMat.equals(Material.BARRIER) || aboveMat.equals(Material.OBSIDIAN)) {
			new BukkitRunnable() {
				float mFreq = 0;

				@Override
				public void run() {
					player.playSound(player.getLocation(), Sound.ENTITY_HORSE_DEATH, SoundCategory.PLAYERS, 1, mFreq);
					mFreq += 0.05f;
					if (!player.isOnline() || mFreq > 1.5) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 10);

			event.setCancelled(true);
			return;
		}

		/* Implements bed teleporters.
		 *
		 * When a player sleeps in a bed, the 10 blocks under that bed are checked for
		 * a regular command block.
		 *
		 * If a command block is found, its command string is used for coordinates to teleport
		 * the player to after a short delay
		 */
		for (double y = 10; y > 0; y--) {
			loc = loc.subtract(0, 1, 0);
			Block testblock = loc.getBlock();
			BlockState state = testblock.getState();

			if (testblock.getType().equals(Material.COMMAND_BLOCK)
				&& state instanceof CommandBlock commandBlock) {

				String str = commandBlock.getCommand();

				String[] split = str.split(" ");
				if (split.length != 5) {
					player.sendMessage(Component.text("Command block should be of the format 'x y z pitch yaw'", NamedTextColor.RED));
					player.sendMessage(Component.text("Relative and absolute coordinates accepted", NamedTextColor.RED));
					continue;
				}

				try {
					// Coordinates are relative to the head of the bed
					Point pt = Point.fromString(player, split[0], split[1], split[2], false, bed.getLocation());

					float yaw = (float) CommandUtils.parseDoubleFromString(player, split[3]);
					float pitch = (float) CommandUtils.parseDoubleFromString(player, split[4]);

					// Adjust for player teleports being off-center
					Location teleLoc = new Location(world, pt.mX - 0.5, pt.mY, pt.mZ - 0.5, yaw, pitch);

					final Location oldPlayerSpawn = player.getBedSpawnLocation();

					// Create a deferred task to eject player and teleport them after a short sleep
					new BukkitRunnable() {
						static final int BED_TELE_TIME = 20 * 3;
						int mTicks = 0;

						@Override
						public void run() {
							GameMode mode;

							if (mTicks == 0) {
								//Set player's spawn point back to whatever it was
								player.setBedSpawnLocation(oldPlayerSpawn, true);
							}


							if (++mTicks == BED_TELE_TIME) {
								// Get player's current gamemode
								mode = player.getGameMode();

								// Set player's gamemode to survival, so they can be damaged
								player.setGameMode(GameMode.SURVIVAL);

								// Poke the player to eject them from the bed
								player.damage(0.001);

								// Set player's gamemode back to whatever it was
								player.setGameMode(mode);

							} else if (mTicks >= BED_TELE_TIME + 1) {
								player.teleport(teleLoc, PlayerTeleportEvent.TeleportCause.UNKNOWN);

								world.playSound(teleLoc, Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.PLAYERS, 1.0f, 1.3f);

								this.cancel();
							} else if (!player.isSleeping() || !player.isOnline() || player.isDead()) {
								// Abort, player got out of bed early
								this.cancel();
							}
						}
					}.runTaskTimer(mPlugin, 0, 1);

					return;
				} catch (Exception e) {
					player.sendMessage("Failed to parse teleport coordinates");
				}
			}
		}

		// If we have not returned yet, check if the interaction should be blocked
		BlockInteractionsListener.playerEnteredNonTeleporterBed(event);
	}

	private static final String PLAYER_LEFT_BED_TICK_METAKEY = "PlayerLeftBedTick";

	/*
	 * Prevent players from passing through 1-thick barriers using beds
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerBedLeaveEvent(PlayerBedLeaveEvent event) {
		Player player = event.getPlayer();
		if (MetadataUtils.checkOnceThisTick(mPlugin, player, PLAYER_LEFT_BED_TICK_METAKEY)) {
			Block bed = event.getBed();
			Location loc = bed.getLocation();
			player.teleport(loc.add(0.5, 0.6, 0.5));
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerToggleSneakEvent(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();

		if (event.isSneaking()) {
			/*
			 * Player started sneaking
			 *
			 * Create a task that will fire in 2s and attach that taskId to the player via metadata
			 *
			 * If that task fires, trigger the extended sneak event and remove the metadata
			 */
			int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(mPlugin,
				() -> {
					mPlugin.mAbilityManager.playerExtendedSneakEvent(player);
					player.removeMetadata(Constants.PLAYER_SNEAKING_TASK_METAKEY, mPlugin);
				}, 20);

			player.setMetadata(Constants.PLAYER_SNEAKING_TASK_METAKEY, new FixedMetadataValue(mPlugin, taskId));
		} else {
			/*
			 * Player stopped sneaking
			 *
			 * Check if they still have the metadata (they haven't been sneaking for 2s)
			 * If so, remove it and cancel the task
			 */
			if (player.hasMetadata(Constants.PLAYER_SNEAKING_TASK_METAKEY)) {
				int taskId = player.getMetadata(Constants.PLAYER_SNEAKING_TASK_METAKEY).get(0).asInt();
				Bukkit.getScheduler().cancelTask(taskId);
				player.removeMetadata(Constants.PLAYER_SNEAKING_TASK_METAKEY, mPlugin);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void abilityCastEvent(AbilityCastEvent event) {
		Player player = event.getCaster();
		mPlugin.mAbilityManager.abilityCastEvent(player, event);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();

		if (!mPlugin.mItemOverrides.blockBreakInteraction(mPlugin, event.getPlayer(), block, event)) {
			event.setCancelled(true);
			return;
		}

		//Prevent players in survival mode breaking blocks in adventure mode zones not including plots
		if (player.getGameMode() == GameMode.SURVIVAL && !ZoneUtils.playerCanMineBlock(player, block)) {
			event.setCancelled(true);
			return;
		}

		mPlugin.mItemStatManager.onBlockBreak(mPlugin, player, event);
	}

	@EventHandler(ignoreCancelled = true)
	public void playerAnimationEvent(PlayerAnimationEvent event) {
		Player player = event.getPlayer();
		if (event.getAnimationType().equals(PlayerAnimationType.ARM_SWING)) {
			mPlugin.mAbilityManager.playerAnimationEvent(player, event);
		}
	}

	//Player is healed.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onRegain(EntityRegainHealthEvent event) {
		if (event.getEntity() instanceof Player player) {
			mPlugin.mItemStatManager.playerRegainHealthEvent(mPlugin, player, event);
			mPlugin.mAbilityManager.playerRegainHealthEvent(player, event);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onGainAbsorption(EntityGainAbsorptionEvent event) {
		if (event.getEntity() instanceof Player player) {
			mPlugin.mAbilityManager.playerGainAbsorptionEvent(player, event);
		}
	}

	/*
	 * Pick survival or adventure as appropriate for zone properties when
	 * switching into either gamemode.
	 */
	@EventHandler(ignoreCancelled = true)
	public void gamemodeCorrection(PlayerGameModeChangeEvent event) {
		GameMode newGameMode = event.getNewGameMode();
		Player player = event.getPlayer();
		GameMode expectedGameMode = ZoneUtils.expectedGameMode(player);

		//NOTE Once we update Paper version more,
		// replace the event's generic failure message via
		// event.cancelMessage(),
		// instead of doing player.sendMessage() separately
		if (
			GameMode.SURVIVAL.equals(newGameMode)
				&& GameMode.ADVENTURE.equals(expectedGameMode)
		) {
			event.setCancelled(true);
			player.setGameMode(GameMode.ADVENTURE);
			// Op check since listener also catches mechanisms putting players
			// into the wrong game mode for their zone, such as Frost Giant,
			// and in that case we don't want to notify normal players who
			// didn't choose to enter that game mode
			// /who won't see vanilla "Set own game mode" messages.
			// This check also won't be needed in the future once we're just
			// replacing event.cancelMessage().
			if (player.isOp()) {
				player.sendMessage(
					Component
						.text("Set own game mode to ", Colors.GREENISH_BLUE)
						.append(
							Component.text("Adventure Mode ", Colors.GREENISH_BLUE_DARK)
						)
						.append(
							Component.text(
								"instead of Survival due to zone.",
								Colors.GREENISH_BLUE
							)
						)
				);
			}
		} else if (GameMode.ADVENTURE.equals(newGameMode) && GameMode.SURVIVAL.equals(expectedGameMode)) {
			event.setCancelled(true);
			player.setGameMode(GameMode.SURVIVAL);
			if (player.isOp()) {
				player.sendMessage(
					Component
						.text("Set own game mode to ", Colors.GREENISH_BLUE)
						.append(
							Component.text("Survival Mode ", Colors.GREENISH_BLUE_DARK)
						)
						.append(
							Component.text(
								"instead of Adventure due to zone.",
								Colors.GREENISH_BLUE
							)
						)
				);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void gamemodeChangeEvent(PlayerGameModeChangeEvent event) {
		// When switching to creative, update the inventory to update any virtual items back into normal forms to prevent breaking them.
		// Also update when switching away from creative or spectator to show virtual items again.
		if (event.getNewGameMode() == GameMode.CREATIVE
			|| (event.getPlayer().getGameMode() == GameMode.CREATIVE && event.getNewGameMode() != GameMode.SPECTATOR)
			|| event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> event.getPlayer().updateInventory(), 1);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void inventoryCreativeEvent(InventoryCreativeEvent event) {
		// The inventory update initiated above takes a while, during which the virtual items can become broken anyway, so need to watch for these events as well
		if ((event.getCurrentItem() != null && VirtualItemsReplacer.isVirtualItem(event.getCurrentItem()))
			|| VirtualItemsReplacer.isVirtualItem(event.getCursor())) {
			event.setCancelled(true);
		}
	}

	/*
	 * Refresh class abilities after event (end of tick)
	 * when switching to/from spectator.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void spectatorAbilityRefresh(PlayerGameModeChangeEvent event) {
		Player player = event.getPlayer();
		GameMode oldGameMode = player.getGameMode();
		GameMode newGameMode = event.getNewGameMode();

		if (
			GameMode.SPECTATOR.equals(oldGameMode)
				|| GameMode.SPECTATOR.equals(newGameMode)
		) {
			Plugin plugin = Plugin.getInstance();
			if (plugin != null) {
				Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.mAbilityManager.updatePlayerAbilities(player, true), 0);
			}
		}
	}

	/*
	 * Prevent crafting with custom items
	 */
	@EventHandler(ignoreCancelled = true)
	public void craftItemEvent(CraftItemEvent event) {
		ItemStack result = event.getCurrentItem();
		if (result == null) {
			return;
		}
		Material resultMat = result.getType();
		String resultMatStr = resultMat.getKey().toString();

		boolean cancel = false;

		boolean gotDye = false;
		boolean resultIsDyed = resultMat.equals(Material.FIREWORK_STAR)
			|| resultMatStr.startsWith("minecraft:leather_")
			|| resultMatStr.endsWith("_banner")
			|| resultMatStr.endsWith("_shulker_box");

		boolean gotBanner = false;
		boolean gotShield = resultMat.equals(Material.SHIELD);

		for (ItemStack item : event.getInventory().getMatrix()) {
			if (item != null) {
				Material mat = item.getType();
				String matStr = mat.getKey().toString();

				ItemMeta meta = item.getItemMeta();
				if (meta != null && meta.hasLore() && ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.MATERIAL) == 0) {
					cancel = true;
				} else {
					if (matStr.endsWith("_dye")) {
						gotDye = true;
					}
					if (matStr.endsWith("_banner")) {
						gotBanner = true;
					}
				}
			}
		}

		if (gotDye && resultIsDyed) {
			cancel = false;
		}
		if (gotBanner && gotShield) {
			cancel = false;
		}
		if (resultMat.equals(Material.WRITTEN_BOOK)) {
			cancel = false;
		}
		if (resultMat.equals(Material.TIPPED_ARROW)) {
			cancel = false;
		}
		event.setCancelled(cancel);

		// Easter egg: Times Dyed for shulker boxes
		if (!cancel && ItemUtils.isShulkerBox(resultMat) && event.getWhoClicked() instanceof Player player) {
			StatTrackManager.getInstance().incrementStatImmediately(result, player, InfusionType.STAT_TRACK_DEATH, 1);
		}
	}

	private static final Set<DamageCause> DISABLE_KNOCKBACK_DAMAGE_CAUSES = Set.of(
		DamageCause.CONTACT,
		DamageCause.FALL,
		DamageCause.FIRE,
		DamageCause.FIRE_TICK,
		DamageCause.LAVA,
		DamageCause.VOID,
		DamageCause.STARVATION,
		DamageCause.POISON,
		DamageCause.WITHER,
		DamageCause.HOT_FLOOR);

	private static final Set<DamageCause> SCALABLE_REGION_DAMAGE_CAUSES = Set.of(
		DamageCause.FIRE_TICK,
		DamageCause.LAVA
	);

	private final Set<UUID> mIgnoreKnockbackThisTick = new HashSet<>();
	private final Set<UUID> mIgnoreKnockbackNextTick = new HashSet<>();
	private int mKnockbackTaskId = -1;

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityDamageEvent(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player player
			&& player.getNoDamageTicks() <= player.getMaximumNoDamageTicks() / 2.0f // can only take knockback again after half the iframes are over
			&& DISABLE_KNOCKBACK_DAMAGE_CAUSES.contains(event.getCause())) {
			mIgnoreKnockbackNextTick.add(player.getUniqueId());

			// NB: The two sets are required because this task runs after the damage event, but before the velocity change event.
			if (mKnockbackTaskId < 0 || !Bukkit.getScheduler().isQueued(mKnockbackTaskId)) {
				mKnockbackTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(mPlugin, () -> {
					mIgnoreKnockbackThisTick.clear();
					mIgnoreKnockbackThisTick.addAll(mIgnoreKnockbackNextTick);
					mIgnoreKnockbackNextTick.clear();
					if (mIgnoreKnockbackThisTick.isEmpty()) {
						Bukkit.getScheduler().cancelTask(mKnockbackTaskId);
						mKnockbackTaskId = -1;
					}
				}, 0, 1);
			}
		}

		// For Fire / Fall damage in R2 and R3, take more damage.
		if (event.getEntity() instanceof Player player
			&& SCALABLE_REGION_DAMAGE_CAUSES.contains(event.getCause())) {
			if (ServerProperties.getAbilityEnhancementsEnabled(player)) {
				// R3, Take +40% more damage
				event.setDamage(event.getDamage() * 1.4);
			} else if (ServerProperties.getClassSpecializationsEnabled(player)) {
				// R2, Take +20% more damage
				event.setDamage(event.getDamage() * 1.2);
			}
		}
	}

	// Handles cancelled events to properly remove the player from the mIgnoreNextKnockback set even if the event has already been cancelled by another listener
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerVelocityEvent(PlayerVelocityEvent event) {
		if (mIgnoreKnockbackThisTick.remove(event.getPlayer().getUniqueId())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void playerTakeLecternBookEvent(PlayerTakeLecternBookEvent event) {
		if (event.getPlayer().getGameMode() == GameMode.ADVENTURE) {
			event.setCancelled(true);
		}
	}

	// Bows: can set 'consume item' to false if not consumed
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void entityShootBowEvent(EntityShootBowEvent event) {
		if (event.getEntity() instanceof Player player
			&& event.shouldConsumeItem()
			&& event.getBow() != null
			&& event.getBow().getType() == Material.BOW) {
			ArrowConsumeEvent arrowConsumeEvent = new ArrowConsumeEvent(player, event.getConsumable());
			Bukkit.getPluginManager().callEvent(arrowConsumeEvent);
			if (arrowConsumeEvent.isCancelled()) {
				event.setConsumeItem(false);
				((AbstractArrow) event.getProjectile()).setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
				player.updateInventory();
			}
		}
	}

	// Crossbows: cannot set 'consume item', must give back an arrow. Thus use MONITOR to make sure that the arrow is actually shot.
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityShootBowEventMonitor(EntityShootBowEvent event) {
		if (event.getEntity() instanceof Player player
			&& event.getBow() != null
			&& event.getBow().getType() == Material.CROSSBOW
			&& event.getProjectile() instanceof AbstractArrow arrow
			&& arrow.getPickupStatus() == AbstractArrow.PickupStatus.ALLOWED) {
			ArrowConsumeEvent arrowConsumeEvent = new ArrowConsumeEvent(player, event.getConsumable());
			Bukkit.getPluginManager().callEvent(arrowConsumeEvent);
			if (arrowConsumeEvent.isCancelled()) {
				InventoryUtils.giveItem(player, ItemUtils.clone(event.getConsumable()), true);
				arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
				player.updateInventory();
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void arrowConsumeEvent(ArrowConsumeEvent event) {
		mPlugin.mItemStatManager.onConsumeArrow(mPlugin, event.getPlayer(), event);
		if (!event.isCancelled() && !mPlugin.mAbilityManager.playerConsumeArrowEvent(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void entityPotionEffectEvent(EntityPotionEffectEvent event) {
		if (event.getEntity() instanceof Player player) {
			// If Player pops a totem of undying, replace absorption event with absorption utils.
			if (event.getCause() == EntityPotionEffectEvent.Cause.TOTEM && event.getNewEffect() != null && event.getNewEffect().getType().equals(PotionEffectType.ABSORPTION)) {
				event.setCancelled(true);
				AbsorptionUtils.addAbsorption(player, 8, 8, 5 * 20);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerServerTransferEvent(PlayerServerTransferEvent event) {
		mTransferringPlayers.add(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerTransferFailEvent(PlayerTransferFailEvent event) {
		mTransferringPlayers.remove(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerFinishedTransferring(PlayerQuitEvent event) {
		mTransferringPlayers.remove(event.getPlayer().getUniqueId());
	}

	public boolean isPlayerTransferring(Player player) {
		return mTransferringPlayers.contains(player.getUniqueId());
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void customEffectApplyEvent(CustomEffectApplyEvent event) {
		if (event.getEntity() instanceof Player player) {
			mPlugin.mAbilityManager.customEffectApplyEvent(player, event);
			mPlugin.mItemStatManager.onCustomEffectApply(mPlugin, player, event);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void effectTypeApplyFromPotionEvent(EffectTypeApplyFromPotionEvent event) {
		if (event.getEntity() instanceof Player player) {
			mPlugin.mAbilityManager.effectTypeApplyFromPotionEvent(player, event);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void serverTickEndEvent(ServerTickEndEvent event) {
		ParticleManager.tick();
	}
}
