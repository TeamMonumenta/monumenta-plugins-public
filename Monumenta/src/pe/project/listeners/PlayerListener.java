package pe.project.listeners;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedMainHandEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
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
import org.bukkit.GameMode;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.util.Vector;
import org.bukkit.World;

import pe.project.Constants;
import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.Plugin;
import pe.project.point.Point;
import pe.project.server.reset.DailyReset;
import pe.project.utils.CommandUtils;
import pe.project.utils.InventoryUtils;
import pe.project.utils.ItemUtils;
import pe.project.utils.LocationUtils;
import pe.project.utils.LocationUtils.LocationType;
import pe.project.utils.particlelib.ParticleEffect;
import pe.project.utils.PotionUtils;
import pe.project.utils.PotionUtils.PotionInfo;
import pe.project.utils.ScoreboardUtils;

public class PlayerListener implements Listener {
	Plugin mPlugin = null;
	World mWorld = null;
	Random mRandom = null;

	public PlayerListener(Plugin plugin, World world, Random random) {
		mPlugin = plugin;
		mWorld = world;
		mRandom = random;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerJoinEvent(PlayerJoinEvent event) {
		if (mPlugin.mServerProperties.getJoinMessagesEnabled() == false) {
			event.setJoinMessage("");
		}

		/* Mark this player as inventory locked until their inventory data is applied */
		event.getPlayer().setMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY, new FixedMetadataValue(mPlugin, true));

		new BukkitRunnable() {
			Integer tick = 0;
			@Override
			public void run() {
				if (++tick == 20) {
					Player player = event.getPlayer();

					mPlugin.mTrackingManager.addEntity(player);
					DailyReset.handle(mPlugin, player);

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerQuitEvent(PlayerQuitEvent event) {
		if (mPlugin.mServerProperties.getJoinMessagesEnabled() == false) {
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

		/* Don't let the player interact with the world when transferring */
		if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)) {
			event.setCancelled(true);
			return;
		}

		Material mat = (event.getClickedBlock() != null) ? event.getClickedBlock().getType() : Material.AIR;
		mPlugin.getClass(player).PlayerInteractEvent(player, event.getAction(), item, mat);

		//	Left Click.
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			if (!mPlugin.mItemOverrides.leftClickInteraction(mPlugin, player, action, item, block)) {
				event.setCancelled(true);
			}
		}
		//	Right Click.
		else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			if (!mPlugin.mItemOverrides.rightClickInteraction(mPlugin, player, action, item, block)) {
				event.setCancelled(true);
			}

		//	if (block.getType() == Material.CHEST) {
		//		ChestUtils.chestTest(mPlugin, player, block);
		//	}

			if (item != null && ItemUtils.isArmorItem(item.getType())) {
				InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player);
			}
		} else if (action == Action.PHYSICAL) {
			if (!mPlugin.mItemOverrides.physicsInteraction(mPlugin, player, action, item, block)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void BlockPlaceEvent(BlockPlaceEvent event) {
		ItemStack item = event.getItemInHand();
		Player player = event.getPlayer();

		if (!mPlugin.mItemOverrides.blockPlaceInteraction(mPlugin, player, item, event)) {
			event.setCancelled(true);
		}
	}

	// Player interacts with an entity (not triggered on armor stands for some reason
	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
		Player player = event.getPlayer();

		/* Don't let the player do this when transferring or if in a restricted zone */
		if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)
			|| (LocationUtils.getLocationType(mPlugin, player) == LocationType.RestrictedZone
				&& player.getGameMode() != GameMode.CREATIVE)) {
			event.setCancelled(true);
			return;
		}

		Entity clickedEntity = event.getRightClicked();
		ItemStack itemInHand = player.getEquipment().getItemInMainHand();

		if (!mPlugin.mItemOverrides.rightClickEntityInteraction(mPlugin, player, clickedEntity, itemInHand)) {
			event.setCancelled(true);
		}
	}

	// Player interacts with an armor stand
	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent event) {
		Player player = event.getPlayer();

		/* Don't let the player do this when transferring or if in a restricted zone */
		if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)
			|| (LocationUtils.getLocationType(mPlugin, player) == LocationType.RestrictedZone
				&& player.getGameMode() != GameMode.CREATIVE)) {
			event.setCancelled(true);
			return;
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
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer());
	}

	//	The player dropped an item.
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerDropItemEvent(PlayerDropItemEvent event) {
		Player player = event.getPlayer();

		/* Don't let the player do this when transferring or if in a restricted zone */
		if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)
			|| (LocationUtils.getLocationType(mPlugin, player) == LocationType.RestrictedZone
				&& player.getGameMode() != GameMode.CREATIVE)) {
			event.setCancelled(true);
			return;
		}

		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player);
	}

	//	The player picked up an item.
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerPickupItemEvent(PlayerPickupItemEvent event) {
		Player player = event.getPlayer();

		/* Don't let the player do this when transferring or if in a restricted zone */
		if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)
			|| (LocationUtils.getLocationType(mPlugin, player) == LocationType.RestrictedZone
				&& player.getGameMode() != GameMode.CREATIVE)) {
			event.setCancelled(true);
			return;
		}

		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player);
	}

	//	An item on the player breaks.
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerItemBreakEvent(PlayerItemBreakEvent event) {
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer());
	}

	//	If an inventory interaction happened.
 	@EventHandler(priority = EventPriority.HIGH)
	public void InventoryClickEvent(InventoryClickEvent event) {
 		Inventory inventory = event.getInventory();
		InventoryHolder holder = inventory.getHolder();
		if (holder != null && holder instanceof Player) {
			InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, (Player)holder);
		}
 	}

	//	The player opened an inventory
	@EventHandler(priority = EventPriority.HIGH)
	public void InventoryOpenEvent(InventoryOpenEvent event) {
		if (event.getPlayer() instanceof Player) {
			Player player = (Player)event.getPlayer();

			/* Don't let the player do this when transferring or if in a restricted zone */
			if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)
				|| (LocationUtils.getLocationType(mPlugin, player) == LocationType.RestrictedZone
					&& player.getGameMode() != GameMode.CREATIVE
					&& player.getGameMode() != GameMode.SPECTATOR)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	//	...Because there's a known bug with the stupid Item Property stuff and the InventoryClickEvent stuff...
	//	The player inventory is closed
	@EventHandler(priority = EventPriority.HIGH)
	public void InventoryCloseEvent(InventoryCloseEvent event) {
		Inventory inventory = event.getInventory();
		InventoryHolder holder = inventory.getHolder();
		if (holder != null && holder instanceof Player) {
			Player player = (Player)holder;

			ItemStack mainHand = player.getInventory().getItemInMainHand();
			ItemStack offHand = player.getInventory().getItemInOffHand();

			mPlugin.getClass(player).PlayerItemHeldEvent(player, mainHand, offHand);
			mPlugin.mTrackingManager.mPlayers.updateEquipmentProperties(player);
		}
	}

	//	Something interacts with an inventory
	@EventHandler(priority = EventPriority.HIGH)
	public void InventoryInteractEvent(InventoryInteractEvent event) {
		if (event.getWhoClicked() instanceof Player) {
			Player player = (Player)event.getWhoClicked();

			/* Don't let the player do this when transferring or if in a restricted zone */
			if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)
				|| (LocationUtils.getLocationType(mPlugin, player) == LocationType.RestrictedZone
					&& player.getGameMode() != GameMode.CREATIVE
					&& player.getGameMode() != GameMode.SPECTATOR)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	//	Player changed hand items
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerChangedMainHandEvent(PlayerChangedMainHandEvent event) {
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer());
	}

	//	Player swapped hand items
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer());
	}

	//	The player has died
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();

		// Give the player a NewDeath score of 1 so the city guides will give items again
		ScoreboardUtils.setScoreboardValue(player, "NewDeath", 1);

		// Clear effects
		mPlugin.mPotionManager.clearAllEffects(player);
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

				mPlugin.mPotionManager.clearAllEffects(player);

				ItemStack mainHand = player.getInventory().getItemInMainHand();
				ItemStack offHand = player.getInventory().getItemInOffHand();

				mPlugin.getClass(player).PlayerItemHeldEvent(player, mainHand, offHand);
				mPlugin.mTrackingManager.mPlayers.updateEquipmentProperties(player);

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
				PotionInfo info = (data != null) ? PotionUtils.getPotionInfo(data, 1) : null;

				if (info != null) {
					info.showParticles = true;
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
			LocationType zone = LocationUtils.getLocationType(mPlugin, event.getPlayer());
			if (damage < 0 || zone == LocationType.Capital || zone == LocationType.SafeZone) {
				damage = 0;
			}

			event.setDamage(damage);
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void	PlayerExpChangeEvent(PlayerExpChangeEvent event) {
		Player player = event.getPlayer();
		Integer xp = event.getAmount();

		if (LocationUtils.OLDLABS.within(player.getLocation())) {
			xp = xp / 3;
			event.setAmount(xp);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("unchecked")
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

		Player player = event.getPlayer();

		// Only add the location to the back stack if the player didn't just use /back or /forward
		if (player.hasMetadata(Constants.PLAYER_SKIP_BACK_ADD_METAKEY)) {
			player.removeMetadata(Constants.PLAYER_SKIP_BACK_ADD_METAKEY, mPlugin);
		} else {
			// Get the stack of previous teleport locations
			Stack<Location> backStack = null;
			if (player.hasMetadata(Constants.PLAYER_BACK_STACK_METAKEY)) {
				List<MetadataValue> val = player.getMetadata(Constants.PLAYER_BACK_STACK_METAKEY);
				if (val != null && !val.isEmpty()) {
					backStack = (Stack<Location>)val.get(0).value();
				}
			}

			if (backStack == null) {
				backStack = new Stack<Location>();
			}

			backStack.push(event.getFrom());
			player.setMetadata(Constants.PLAYER_BACK_STACK_METAKEY, new FixedMetadataValue(mPlugin, backStack));
		}
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
					Point pt = Point.fromString(player, split[0], split[1], split[2], false, bed.getLocation());

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

	public static Set<Material> POTION_TYPES = EnumSet.of(Material.POTION, Material.SPLASH_POTION,
	                                                      Material.LINGERING_POTION);

	@EventHandler(priority = EventPriority.LOWEST)
	public void BrewEvent(BrewEvent event) {
		BrewerInventory inv = event.getContents();
		ItemStack ingred = inv.getIngredient();
		ItemStack[] cont = inv.getStorageContents();
		boolean malfunction = false;
		for (ItemStack i : cont) {
			if (i != null) {
				if (POTION_TYPES.contains(i.getType())) {
					if (ingred.getType() == Material.MAGMA_CREAM) {
						malfunction = true;
						break;
					} else if (ingred.getType() == Material.FERMENTED_SPIDER_EYE) {
						PotionMeta meta = (PotionMeta) i.getItemMeta();
						PotionData data = meta.getBasePotionData();
						if (data.getType() == PotionType.NIGHT_VISION) {
							malfunction = true;
							break;
						}
					}
				}
			}
		}
		if (malfunction) {
			Block block = event.getBlock();
			Location loc = block.getLocation().add(0.5, 0.5, 0.5);
			ParticleEffect.EXPLOSION_LARGE.display(0, 0, 0, 0, 1, loc, 40);
			ParticleEffect.FLAME.display(0, 0, 0, 0.25f, 100, loc, 40);
			ParticleEffect.SMOKE_LARGE.display(0, 0, 0, 0.1f, 50, loc, 40);
			loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMEN_DEATH, 1, 0);
			loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
			loc.getBlock().setType(Material.AIR);
			event.setCancelled(true);
			for (Entity e : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
				if (e instanceof Player) {
					Player player = (Player) e;
					Vector v = player.getLocation().toVector().subtract(
					               loc.toVector()).normalize(); // Create the vector.
					v.add(new Vector(0, 0.5, 0));
					player.setVelocity(v.multiply(1)); // Set the velocity.
				}
			}
		}
	}
}
