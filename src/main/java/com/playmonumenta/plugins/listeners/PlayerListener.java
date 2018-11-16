package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityCollection;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.magic.AbilityCastEvent;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.point.Point;
import com.playmonumenta.plugins.server.reset.DailyReset;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.LocationUtils.LocationType;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
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
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.GameMode;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.Particle;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.util.Vector;
import org.bukkit.World;

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
			@Override
			public void run() {
				Player player = event.getPlayer();

				mPlugin.mTrackingManager.addEntity(player);
				DailyReset.handle(mPlugin, player);
				AbilityManager.getManager().updatePlayerAbilities(player);
			}
		}.runTaskLater(mPlugin, 20);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerQuitEvent(PlayerQuitEvent event) {
		if (mPlugin.mServerProperties.getJoinMessagesEnabled() == false) {
			event.setQuitMessage("");
		}

		Player player = event.getPlayer();

		mPlugin.mTrackingManager.removeEntity(player);

		// If the player is opped don't apply anti-combat logging technology!
		for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), 20)) {
			mPlugin.mCombatLoggingTimers.addTimer(mob.getUniqueId(), Constants.TEN_MINUTES);
			mob.setRemoveWhenFarAway(false);
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

		Material mat = (block != null) ? block.getType() : Material.AIR;
		mPlugin.getSpecialization(player).PlayerInteractEvent(player, action, item, mat);
		AbilityManager.getManager().PlayerInteractEvent(player, action, item, mat);

		// Left Click.
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			if (!mPlugin.mItemOverrides.leftClickInteraction(mPlugin, player, action, item, block)) {
				event.setCancelled(true);
			}
		}
		// Right Click.
		else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			if (!mPlugin.mItemOverrides.rightClickInteraction(mPlugin, player, action, item, block)) {
				event.setCancelled(true);
			}

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

		/* Don't let the player interact with the world when transferring */
		if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)) {
			event.setCancelled(true);
			return;
		}

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

		if (clickedEntity instanceof ItemFrame) {
			ItemFrame frame = (ItemFrame) clickedEntity;
			if (frame.isInvulnerable()) {
				event.setCancelled(true);
			}
		}

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

	// The Player swapped their current selected item.
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerItemHeldEvent(PlayerItemHeldEvent event) {
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer());
	}

	// The player dropped an item.
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

	// An entity picked up an item
	@EventHandler(priority = EventPriority.HIGH)
	public void EntityPickupItemEvent(EntityPickupItemEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player)event.getEntity();

			/* Don't let the player do this when transferring or if in a restricted zone */
			if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)
			    || (LocationUtils.getLocationType(mPlugin, player) == LocationType.RestrictedZone
			        && player.getGameMode() != GameMode.CREATIVE)) {
				event.setCancelled(true);
				return;
			}

			InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player);
		}
	}

	// An item on the player breaks.
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerItemBreakEvent(PlayerItemBreakEvent event) {
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer());
	}

	// If an inventory interaction happened.
	@EventHandler(priority = EventPriority.HIGH)
	public void InventoryClickEvent(InventoryClickEvent event) {
		Inventory inventory = event.getInventory();
		InventoryHolder holder = inventory.getHolder();
		if (holder != null && holder instanceof Player) {
			InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, (Player)holder);
		}
	}

	// The player opened an inventory
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

	// ...Because there's a known bug with the stupid Item Property stuff and the InventoryClickEvent stuff...
	// The player inventory is closed
	@EventHandler(priority = EventPriority.HIGH)
	public void InventoryCloseEvent(InventoryCloseEvent event) {
		Inventory inventory = event.getInventory();
		InventoryHolder holder = inventory.getHolder();
		if (holder != null && holder instanceof Player) {
			Player player = (Player)holder;

			ItemStack mainHand = player.getInventory().getItemInMainHand();
			ItemStack offHand = player.getInventory().getItemInOffHand();

			AbilityManager.getManager().PlayerItemHeldEvent(player, mainHand, offHand);
			mPlugin.mTrackingManager.mPlayers.updateEquipmentProperties(player);
		}
	}

	// Something interacts with an inventory
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

	// Player changed hand items
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerChangedMainHandEvent(PlayerChangedMainHandEvent event) {
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer());
	}

	// Player swapped hand items
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer());
	}

	// The player has died
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();

		// Give the player a NewDeath score of 1 so the city guides will give items again
		ScoreboardUtils.setScoreboardValue(player, "NewDeath", 1);

		if (ScoreboardUtils.getScoreboardValue(player, Constants.SCOREBOARD_DEATH_MESSAGE) != 0) {
			player.sendMessage(event.getDeathMessage());
			player.sendMessage(ChatColor.AQUA + "Only you saw this message. Change this with /deathmsg");
			event.setDeathMessage("");
		}

		// Clear effects
		mPlugin.mPotionManager.clearAllPotions(player);
	}

	// The player has respawned.
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerRespawnEvent(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		final String name = player.getName();

		player.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, new Runnable() {
			@Override
			public void run() {
				Player player = Bukkit.getPlayer(name);

				mPlugin.mPotionManager.clearAllPotions(player);

				ItemStack mainHand = player.getInventory().getItemInMainHand();
				ItemStack offHand = player.getInventory().getItemInOffHand();

				AbilityManager.getManager().PlayerItemHeldEvent(player, mainHand, offHand);
				mPlugin.mTrackingManager.mPlayers.updateEquipmentProperties(player);

				AbilityManager.getManager().PlayerRespawnEvent(player);
			}
		}, 0);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerFishEvent(PlayerFishEvent event) {
		Player player = event.getPlayer();

		/* Don't let the player interact with the world when transferring */
		if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)) {
			event.setCancelled(true);
			return;
		}

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

		/* Don't let the player interact with the world when transferring */
		if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)) {
			event.setCancelled(true);
			return;
		}

		ItemStack item = event.getItem();
		if (item.getType() == Material.POTION) {
			PotionMeta meta = ItemUtils.getPotionMeta(item);
			if (meta != null) {
				// Add base potion effect.
				PotionData data = meta.getBasePotionData();
				PotionInfo info = (data != null) ? PotionUtils.getPotionInfo(data, 1) : null;

				if (info != null) {
					info.showParticles = true;
					mPlugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, info);
				}

				// Add custom potion effects.
				List<PotionEffect> effects = meta.getCustomEffects();
				if (effects != null) {

					// Kill the player if they drink a potion with instant damage 10+
					for (PotionEffect effect : effects) {
						if (effect.getType() != null &&
						    effect.getType().equals(PotionEffectType.HARM) &&
						    effect.getAmplifier() >= 9) {

							player.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, new Runnable() {
								@Override
								public void run() {
									player.setHealth(0);
								}
							}, 0);
						}
					}

					mPlugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, effects);
				}
			}
		}
	}

	// An item has taken damage.
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
	public void PlayerExpChangeEvent(PlayerExpChangeEvent event) {
		Player player = event.getPlayer();
		Integer xp = event.getAmount();

		/* Don't let the player interact with the world when transferring */
		if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)) {
			event.setAmount(0);
			return;
		}

		if (LocationUtils.OLDLABS.within(player.getLocation())) {
			xp = xp / 3;
			event.setAmount(xp);
		}

		mPlugin.mTrackingManager.mPlayers.onExpChange(mPlugin, player, event);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
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

		/* Don't let the player interact with the world when transferring */
		if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)) {
			event.setCancelled(true);
			return;
		}

		for (double y = 10; y > 0; y--) {
			loc = loc.subtract(0, 1, 0);
			Block testblock = loc.getBlock();
			BlockState state = testblock.getState();

			if (testblock.getType().equals(Material.COMMAND_BLOCK)
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

								world.playSound(teleLoc, Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1.0f, 1.3f);

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
			loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);
			loc.getWorld().spawnParticle(Particle.FLAME, loc, 100, 0, 0, 0, 0.25);
			loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.1);
			loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_DEATH, 1, 0);
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

	// This serves as a workaround for damaging players since PVP has been toggled off. EntityDamgedByEntityEvent doesn't work for Player v Player
	@EventHandler
	public void PlayerAnimationEvent(PlayerAnimationEvent event) {
		if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
			Player player = event.getPlayer();
			double maxDist = 3;
			Player target = null;
			for (Player p : PlayerUtils.getNearbyPlayers(player.getLocation(), 3D)) {
				if (PlayerUtils.hasLineOfSight(player, p)) {
					if (p.getLocation().distance(player.getLocation()) < maxDist) {
						maxDist = p.getLocation().distance(player.getLocation());
						target = p;
					}
				}
			}
			if (target != null) {
				mPlugin.getSpecialization(player).PlayerDamagedByPlayerEvent(player, target);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerToggleSneakEvent(PlayerToggleSneakEvent event, Player player) {
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
					AbilityManager.getManager().PlayerExtendedSneakEvent(player);
					player.removeMetadata(Constants.PLAYER_SNEAKING_TASK_METAKEY, mPlugin);
				}, 40);

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

	@EventHandler
	public void AbilityCastEvent(AbilityCastEvent event) {
		Player player = event.getCaster();
		mPlugin.getSpecialization(player).AbilityCastEvent(player, event);
	}
}
