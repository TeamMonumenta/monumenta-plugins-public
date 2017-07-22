package pe.project.listeners;

import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;

import net.md_5.bungee.api.ChatColor;
import pe.project.Constants;
import pe.project.Main;
import pe.project.locations.poi.PointOfInterest;
import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.point.Point;
import pe.project.server.reset.RegionReset;
import pe.project.utils.ItemUtils;
import pe.project.utils.PotionUtils;
import pe.project.utils.PotionUtils.PotionInfo;
import pe.project.utils.ScoreboardUtils;
import pe.project.utils.StringUtils;

public class PlayerListener implements Listener {
	Main mPlugin = null;
	World mWorld = null;
	
	public PlayerListener(Main plugin, World world) {
		mPlugin = plugin;
		mWorld = world;
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		
		mPlugin.mTrackingManager.addEntity(player);
		RegionReset.handle(mPlugin, player);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerQuitEvent(PlayerQuitEvent event) {
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
			//	Quest Compass.
			if (item != null && item.getType() == Material.COMPASS) {
				//	Show currently active quest.
				mPlugin.mQuestManager.showCurrentQuest(player);
			}
		}
		//	Right Click.
		else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			//	Quest Compass.
			if (item != null) {
				if (item.getType() == Material.MONSTER_EGG && action == Action.RIGHT_CLICK_BLOCK) {
					if (block.getType() == Material.MOB_SPAWNER) {
						event.setCancelled(true);
						return;
					}
				} else if (item.getType() == Material.COMPASS) {
					//	Show current POI respawn timer.
					if (player.isSneaking()) {
						List<PointOfInterest> pois = mPlugin.mPOIManager.allWithinAnyPointOfInterest(new Point(player.getLocation()));
						if (pois != null && pois.size() > 0) {
							for (PointOfInterest poi : pois) {
								int ticks = poi.getTimer();
								
								player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD +  poi.getName() + " is respawning in " + StringUtils.ticksToTime(ticks));
							}
						} else {
							player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You are not within range of a Point of Interest.");
						}
					}
					//	Cycle active Quest.
					else {
						mPlugin.mQuestManager.cycleQuestTracker(player);
					}
				} else if (item.getType() == Material.FISHING_ROD) {
					if (action == Action.RIGHT_CLICK_BLOCK) {
						Material blockType = block.getType();
						
						//	If this is an interactible block it means they didn't really want to be fishing! :D
						if (ItemUtils.isInteractable(blockType)) {
							if (mPlugin.mTrackingManager.mFishingHook.containsEntity(player)) {
								mPlugin.mTrackingManager.mFishingHook.removeEntity(player);
							}
						}
					}
				}
			}
			
			if (block != null) {
				if (player.getGameMode() != GameMode.CREATIVE && block.getType() == Material.ANVIL) {
					event.setCancelled(true);
				}
			}
			
			if (player.getGameMode() == GameMode.ADVENTURE) {
				ItemStack heldItem = player.getInventory().getItemInMainHand();
				if (ItemUtils.isBoat(heldItem.getType())) {
					event.setCancelled(true);
				}
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
				if (playerLoc.distance(receiver.getLocation()) > chatDistance) {
					iter.remove();
				}
			}
		}
	}
	
	//	The Player swapped their current selected item.
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerItemHeldEvent(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		if (player != null) {
			final String name = player.getName();
			
			player.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, new Runnable() {
				@Override
				public void run() {
				    Player player = Bukkit.getPlayer(name);
				    mPlugin.getClass(player).PlayerItemHeldEvent(player);
				}
			}, 0);
		}
	}
	
	//	The player dropped an item.
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerDropItemEvent(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		final String name = player.getName();
		
		player.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, new Runnable() {
			@Override
			public void run() {
			    Player player = Bukkit.getPlayer(name);
			    mPlugin.getClass(player).PlayerDropItemEvent(player);
			}
		}, 0);
	}
	
	//	An item on the player breaks.
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerItemBreakEvent(PlayerItemBreakEvent event) {
		Player player = event.getPlayer();
		final String name = player.getName();
		
		player.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, new Runnable() {
			@Override
			public void run() {
			    Player player = Bukkit.getPlayer(name);
			    mPlugin.getClass(player).PlayerItemBreakEvent(player);
			}
		}, 0);
	}
	
	//	The player clicked/released in their inventory.
	@EventHandler(priority = EventPriority.HIGH)
	public void InventoryClickEvent(InventoryClickEvent event) {
		Inventory inventory = event.getInventory();
		if (inventory.getType() == InventoryType.PLAYER) {
			Player player = (Player)inventory.getHolder();
			final String name = player.getName();
			
			player.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, new Runnable() {
				@Override
				public void run() {
				    Player player = Bukkit.getPlayer(name);
				    mPlugin.getClass(player).PlayerItemHeldEvent(player);
				}
			}, 0);
		}
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
}
