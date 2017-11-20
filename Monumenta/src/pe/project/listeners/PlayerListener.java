package pe.project.listeners;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;
import pe.project.Constants;
import pe.project.Main;
import pe.project.items.QuestingCompass;
import pe.project.locations.safezones.SafeZoneConstants;
import pe.project.locations.safezones.SafeZoneConstants.SafeZones;
import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.point.Point;
import pe.project.server.reset.DailyReset;
import pe.project.server.reset.RegionReset;
import pe.project.utils.CommandUtils;
import pe.project.utils.ItemUtils;
import pe.project.utils.PotionUtils;
import pe.project.utils.PotionUtils.PotionInfo;
import pe.project.utils.ScoreboardUtils;

public class PlayerListener implements Listener {
	Main mPlugin = null;
	World mWorld = null;
	Random mRandom = null;

	/*
	 * List of materials that are allowed to be placed by
	 * players in survival even if they have lore text
	 */
	public static Set<Material> ALLOW_LORE_MATS = new HashSet<>(Arrays.asList(
		Material.CHEST,
		Material.PACKED_ICE,
		Material.WOOL,
		Material.SKULL,
		Material.SKULL_ITEM
	));


	public PlayerListener(Main plugin, World world, Random random) {
		mPlugin = plugin;
		mWorld = world;
		mRandom = random;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerJoinEvent(PlayerJoinEvent event) {
		if (mPlugin.mServerProporties.getJoinMessagesEnabled() == false) {
			event.setJoinMessage("");
		}

		new BukkitRunnable() {
			Integer tick = 0;
			@Override
			public void run() {
				if (++tick == 20) {
					Player player = event.getPlayer();

					mPlugin.mTrackingManager.addEntity(player);
					RegionReset.handle(mPlugin, player);
					DailyReset.handle(mPlugin, player);

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerQuitEvent(PlayerQuitEvent event) {
		if (mPlugin.mServerProporties.getJoinMessagesEnabled() == false) {
			event.setQuitMessage("");
		}

		Player player = event.getPlayer();

		mPlugin.mTrackingManager.removeEntity(player);

		//		If the player is opped don't apply anti-combat logging technology!
		List<Entity> nearbyEntities = player.getNearbyEntities(20, 20, 20);
		if (nearbyEntities.size() > 0) {
			for (Entity entity : nearbyEntities) {
				if (entity instanceof Monster) {
					Monster mob = (Monster)entity;
					mPlugin.mCombatLoggingTimers.addTimer(mob.getUniqueId(), Constants.TEN_MINUTES);
					mob.setRemoveWhenFarAway(false);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerInteractEvent(PlayerInteractEvent event) {
		Action action = event.getAction();
		Player player = event.getPlayer();
		ItemStack item = event.getItem();
		Block block = event.getClickedBlock();

		Material mat = (event.getClickedBlock() != null) ? event.getClickedBlock().getType() : Material.AIR;
		mPlugin.getClass(player).PlayerInteractEvent(player, event.getAction(), mat);

		//	Left Click.
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			if (!_handleLeftClickInteract(player, action, item, block)) {
				event.setCancelled(true);
			}
		}
		//	Right Click.
		else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			if (!_handleRightClickInteract(player, action, item, block)) {
				event.setCancelled(true);
			}
		} else if (event.getAction() == Action.PHYSICAL) {
			if (block != null) {
				if (block.getType() == Material.SOIL) {
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void BlockPlaceEvent(BlockPlaceEvent event) {
		ItemStack item = event.getItemInHand();

		if (item.hasItemMeta() && item.getItemMeta().hasLore()
			&& event.getPlayer().getGameMode() != GameMode.CREATIVE
			&& !(ALLOW_LORE_MATS.contains(item.getType()))) {
			/* Prevent accidentally placing most lore items (plants, etc.) */
			event.setCancelled(true);
		} else if (item.getType() == Material.PACKED_ICE
		           && item.hasItemMeta() && item.getItemMeta().hasLore()
				   && event.getPlayer().getGameMode() == GameMode.SURVIVAL
				   && ((SafeZoneConstants.withinAnySafeZone(event.getPlayer().getLocation()) != SafeZones.None) || mPlugin.mServerProporties.getIsTownWorld())) {
			// Special packed ice that becomes water in a plot
			event.getBlockPlaced().setType(Material.STATIONARY_WATER);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
		Entity entity = event.getRightClicked();
		if (entity instanceof ZombieVillager) {
			Player player = event.getPlayer();
			ItemStack item = player.getEquipment().getItemInMainHand();
			if (item != null) {
				Material type = item.getType();
				if (type == Material.GOLDEN_APPLE) {
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void AsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		Location playerLoc = player.getLocation();
		int guild = ScoreboardUtils.getScoreboardValue(player, "Guild");

		Iterator<Player> iter = event.getRecipients().iterator();
		while (iter.hasNext()) {
			Player receiver = iter.next();
			int receiverGuild = ScoreboardUtils.getScoreboardValue(receiver, "Guild");

			if (guild == 0 || guild != receiverGuild) {
				int chatDistance = ScoreboardUtils.getScoreboardValue(receiver, "chatDistance");

				if (chatDistance != 0 && (playerLoc.distance(receiver.getLocation()) > chatDistance)) {
					iter.remove();
				}
			}
		}
	}

	//	The Player swapped their current selected item.
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerItemHeldEvent(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();

		ItemStack mainHand = player.getInventory().getItem(event.getNewSlot());
		ItemStack offHand = player.getInventory().getItemInOffHand();

		mPlugin.getClass(player).PlayerItemHeldEvent(player, mainHand, offHand);
	}

	//	The player dropped an item.
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerDropItemEvent(PlayerDropItemEvent event) {
		Player player = event.getPlayer();

		ItemStack mainHand = player.getInventory().getItemInMainHand();
		ItemStack offHand = player.getInventory().getItemInOffHand();

		mPlugin.getClass(player).PlayerDropItemEvent(player, mainHand, offHand);
	}

	//	The player picked up an item.
	@EventHandler(priority = EventPriority.HIGH)
	public void EntityPickupItemEvent(PlayerPickupItemEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();

		player.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, new Runnable() {
			@Override
			public void run() {
				Player p = Bukkit.getPlayer(name);

				ItemStack mainHand = p.getInventory().getItemInMainHand();
				ItemStack offHand = p.getInventory().getItemInOffHand();

				mPlugin.getClass(p).PlayerItemHeldEvent(p, mainHand, offHand);
			}
		}, 0);
	}

	//	An item on the player breaks.
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerItemBreakEvent(PlayerItemBreakEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();

		player.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, new Runnable() {
			@Override
			public void run() {
				Player p = Bukkit.getPlayer(name);

				ItemStack mainHand = p.getInventory().getItemInMainHand();
				ItemStack offHand = p.getInventory().getItemInOffHand();

				mPlugin.getClass(p).PlayerItemBreakEvent(p, mainHand, offHand);
			}
		}, 0);
	}

	//	The player moved item in their inventory.
	@EventHandler(priority = EventPriority.HIGH)
	public void InventoryClickEvent(InventoryClickEvent event) {
		Inventory inventory = event.getInventory();
		if (inventory.getType() == InventoryType.CRAFTING) {
			Player player = (Player)inventory.getHolder();
			String name = player.getName();

			player.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, new Runnable() {
				@Override
				public void run() {
					Player p = Bukkit.getPlayer(name);

					ItemStack mainHand = p.getInventory().getItemInMainHand();
					ItemStack offHand = p.getInventory().getItemInOffHand();

					mPlugin.getClass(p).PlayerItemHeldEvent(p, mainHand, offHand);
				}
			}, 0);
		}
	}

	//	Player swapped hand items
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		Player player = event.getPlayer();

		ItemStack mainHand = event.getMainHandItem();
		ItemStack offHand = event.getOffHandItem();

		mPlugin.getClass(player).PlayerItemHeldEvent(player, mainHand, offHand);
	}

	//	The player has died
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();

		// Give the player a NewDeath score of 1 so the city guides will give items again
		ScoreboardUtils.setScoreboardValue(player, "NewDeath", 1);
	}

	//	The player has respawned.
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerRespawnEvent(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		final String name = player.getName();

		player.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, new Runnable() {
			@Override
			public void run() {
				Player player = Bukkit.getPlayer(name);
				mPlugin.getClass(player).PlayerRespawnEvent(player);
			}
		}, 0);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerFishEvent(PlayerFishEvent event) {
		if (event.getState() == State.FISHING) {
			mPlugin.mTrackingManager.mFishingHook.addEntity(event.getPlayer(), event.getHook());
		} else if (event.getState() == State.CAUGHT_ENTITY || event.getState() == State.CAUGHT_FISH) {
			mPlugin.mTrackingManager.mFishingHook.removeEntity(event.getPlayer());

			if (event.getState() == State.CAUGHT_ENTITY && !(event.getCaught() instanceof Monster)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerItemConsumeEvent(PlayerItemConsumeEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getItem();
		if (item.getType() == Material.POTION) {
			PotionMeta meta = ItemUtils.getPotionMeta(item);
			if (meta != null) {
				//	Add base potion effect.
				PotionData data = meta.getBasePotionData();
				PotionInfo info = PotionUtils.getPotionInfo(data);
				info.showParticles = true;
				if (info != null) {
					mPlugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, info);
				}

				//	Add custom potion effects.
				List<PotionEffect> effects = meta.getCustomEffects();
				if (effects != null) {
					mPlugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, effects);
				}
			}
		}
	}

	//	An item has taken damage.
	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerItemDamageEvent(PlayerItemDamageEvent event) {
		ItemStack item = event.getItem();

		if (ItemUtils.isArmorItem(item.getType())) {
			int damage = event.getDamage();

			int unbreaking = item.getEnchantmentLevel(Enchantment.DURABILITY);
			if (unbreaking > 0) {
				for (int i = 0; i < damage; i++) {
					if (mRandom.nextInt(unbreaking + 2) > 1) {
						damage--;
					}
				}
			}

			// Players that get resistance from safezones don't take armor damage
			SafeZones zone = SafeZoneConstants.withinAnySafeZone(event.getPlayer().getLocation());
			if (damage < 0 || SafeZoneConstants.safeZoneAppliesEffects(zone)) {
				damage = 0;
			}

			event.setDamage(damage);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerTeleportEvent(PlayerTeleportEvent event) {
		// Cancel teleports caused by forbidden sources
		TeleportCause cause = event.getCause();
		if (cause.equals(TeleportCause.CHORUS_FRUIT)
			|| cause.equals(TeleportCause.ENDER_PEARL)
			|| cause.equals(TeleportCause.END_GATEWAY)
			|| cause.equals(TeleportCause.END_PORTAL)
			|| cause.equals(TeleportCause.NETHER_PORTAL)) {
			event.setCancelled(true);
			return;
		}

		UUID playerUUID = event.getPlayer().getUniqueId();

		// Only add the location to the back stack if the player didn't just use /back or /forward
		Boolean skipBackLocation = mPlugin.mSkipBackLocation.get(playerUUID);
		if (skipBackLocation == null) {
			skipBackLocation = new Boolean(false);
		}
		if (skipBackLocation == false) {
			// Get the stack of previous teleport locations
			Stack<Location> backStack = mPlugin.mBackLocations.get(playerUUID);
			if (backStack == null) {
				backStack = new Stack<Location>();
			}

			backStack.push(event.getFrom());
			mPlugin.mBackLocations.put(playerUUID, backStack);
		}

		// Indicate the next teleport will not be skipped unless overwritten by /back or /forward
		skipBackLocation = false;
		mPlugin.mSkipBackLocation.put(playerUUID, skipBackLocation);
	}

	/** Implements bed teleporters.
	 *
	 * When a player sleeps in a bed, the 10 blocks under that bed are checked for
	 * a regular command block.
	 *
	 * If a command block is found, it's command string is used for coordinates to teleport
	 * the player to after a short delay
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerBedEnterEvent(PlayerBedEnterEvent event) {
		Player player = event.getPlayer();
		Block bed = event.getBed();
		Location loc = bed.getLocation();
		World world = loc.getWorld();

		for (double y = 10; y > 0; y--) {
			loc = loc.subtract(0, 1, 0);
			Block testblock = loc.getBlock();
			BlockState state = testblock.getState();

			if (testblock.getType().equals(Material.COMMAND)
				&& state instanceof CommandBlock) {

				String str = ((CommandBlock)state).getCommand();

				String[] split = str.split(" ");
				if (split.length != 5) {
					player.sendMessage(ChatColor.RED + "Command block should be of the format 'x y z pitch yaw'");
					player.sendMessage(ChatColor.RED + "Relative and absolute coordinates accepted");
					continue;
				}

				try {
					// Coordinates are relative to the head of the bed
					Point pt = CommandUtils.parsePointFromString(player, split[0], split[1], split[2],
					                                             false, bed.getLocation());

					float yaw = (float)CommandUtils.parseDoubleFromString(player, split[3]);
					float pitch = (float)CommandUtils.parseDoubleFromString(player, split[4]);

					// Adjust for player teleports being off-center
					Location teleLoc = new Location(world, pt.mX - 0.5, pt.mY, pt.mZ - 0.5, yaw, pitch);

					// Create a deferred task to eject player and teleport them after a short sleep
					new BukkitRunnable() {
						Integer tick = 0;
						@Override
						public void run() {
							GameMode mode;
							final int BED_TELE_TIME = 20 * 3;

							if (++tick == BED_TELE_TIME) {
								// Abort, player got out of bed early
								if (player.isSleeping() == false) {
									this.cancel();
									return;
								}

								// Get player's current gamemode
								mode = player.getGameMode();

								// Set player's gamemode to survival so they can be damaged
								player.setGameMode(GameMode.SURVIVAL);

								// Poke the player to eject them from the bed
								player.damage(0.001);

								// Set player's gamemode back to whatever it was
								player.setGameMode(mode);
							} else if (tick >= BED_TELE_TIME + 1) {
								player.teleport(teleLoc);

								world.playSound(teleLoc, "entity.elder_guardian.death", 1.0f, 1.3f);

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
	}

	//	This function returns false if we don't want to handle this action.
	private boolean _handleRightClickInteract(Player player, Action action, ItemStack item, Block block) {
		GameMode gamemode = player.getGameMode();
		Material itemType = (item != null) ? item.getType() : Material.AIR;
		Material blockType = (block != null) ? block.getType() : Material.AIR;

		//	Handle Item Interactions.
		if (itemType != Material.AIR) {
			switch (itemType) {
			case MONSTER_EGG: {
				return (action == Action.RIGHT_CLICK_AIR) ||
						(action == Action.RIGHT_CLICK_BLOCK && blockType != Material.MOB_SPAWNER);
			}
			case COMPASS: {
				QuestingCompass.handleInteraction(mPlugin, player, action);
				break;
			}
			case FISHING_ROD: {
				if (action == Action.RIGHT_CLICK_BLOCK) {
					//	If this is an interactable block it means they didn't really want to be fishing! :D
					if (ItemUtils.isInteractable(blockType)) {
						if (mPlugin.mTrackingManager.mFishingHook.containsEntity(player)) {
							mPlugin.mTrackingManager.mFishingHook.removeEntity(player);
						}
					}
				}
				break;
			}
			case BOAT:
			case BOAT_ACACIA:
			case BOAT_BIRCH:
			case BOAT_DARK_OAK:
			case BOAT_JUNGLE:
			case BOAT_SPRUCE: {
				return (action == Action.RIGHT_CLICK_AIR) ||
						(action == Action.RIGHT_CLICK_BLOCK && gamemode != GameMode.ADVENTURE);	//	Prevent placing boars in adventure mode.
			}
			case BUCKET:
			case WATER_BUCKET:
			case LAVA_BUCKET: {
				return (action == Action.RIGHT_CLICK_AIR) ||
						(action == Action.RIGHT_CLICK_BLOCK && gamemode == GameMode.CREATIVE);
			}

			default:
				break;
			}
		}

		//	Handle Block Interactions
		if (blockType != Material.AIR) {
			switch (blockType) {
			case ANVIL: {
				return (gamemode == GameMode.CREATIVE);
			}

			default:
				break;
			}
		}

		return true;
	}

	private boolean _handleLeftClickInteract(Player player, Action action, ItemStack item, Block block) {
		//GameMode gamemode = player.getGameMode();
		Material itemType = (item != null) ? item.getType() : Material.AIR;
		Material blockType = (block != null) ? block.getType() : Material.AIR;

		//	Handle Item Interactions
		if (itemType != Material.AIR) {
			switch (itemType) {
			case COMPASS: {
				QuestingCompass.handleInteraction(mPlugin, player, action);
				break;
			}

			default:
				break;
			}
		}

		//	Handle Block Interactions
		if (blockType != Material.AIR) {
			switch (blockType) {

			default:
				break;
			}
		}

		return true;
	}
}
