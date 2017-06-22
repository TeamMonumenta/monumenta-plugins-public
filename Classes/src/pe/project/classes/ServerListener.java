package pe.project.classes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.projectiles.ProjectileSource;

import pe.project.classes.Classes.BaseClass;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ServerListener implements Listener {
	Main mPlugin = null;
	
	ServerListener(Main plugin) {
		mPlugin = plugin;
	}

	//	An Entity hit another Entity.
	@EventHandler(priority = EventPriority.LOWEST)
	public void EntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		Entity damagee = event.getEntity();
		
		//	If the entity getting hurt is the player.
		if (damagee instanceof Player) {
			Entity damager = event.getDamager();
			if (damager instanceof LivingEntity) {
				Player player = (Player)damagee;
				mPlugin.getClass(player).PlayerDamagedByLivingEntityEvent((Player)damagee, (LivingEntity)damager, event.getDamage());
			}
		}
		//	Else if the entity getting hurt is a LivingEntity.
		else if (damagee instanceof LivingEntity) {
			Entity damager = event.getDamager();
			
			//	Hit by player.
			if (damager instanceof Player) {
				Player player = (Player)damager;
				
				BaseClass _class = mPlugin.getClass(player);
				_class.ModifyDamage(player, _class, event);
				_class.LivingEntityDamagedByPlayerEvent(player, (LivingEntity)damagee, event.getDamage());
			}
			//	Hit by arrow.
			else if (damager instanceof Arrow) {
				Arrow arrow = (Arrow)damager;
				if (arrow.getShooter() instanceof Player) {
					Player player = (Player)arrow.getShooter();
					
					BaseClass _class = mPlugin.getClass(player);
					_class.ModifyDamage(player, _class, event);
					_class.LivingEntityShotByPlayerEvent(player, arrow, (LivingEntity)damagee, event);
				}
			}
		}
	}
	
	//	Player shoots an arrow.
	@EventHandler(priority = EventPriority.HIGH)
	public void ProjectileLaunchEvent(ProjectileLaunchEvent event) {
		if (event.getEntityType() == EntityType.ARROW || event.getEntityType() == EntityType.TIPPED_ARROW) {
			Arrow arrow = (Arrow)event.getEntity();
			if (arrow.getShooter() instanceof Player) {
				Player player = (Player)arrow.getShooter();		
				mPlugin.getClass(player).PlayerShotArrowEvent(player, arrow);
			}
		} else if(event.getEntityType() == EntityType.SPLASH_POTION) {
			SplashPotion potion = (SplashPotion)event.getEntity();
			if (potion.getShooter() instanceof Player) {
				Player player = (Player)potion.getShooter();
				mPlugin.getClass(player).PlayerThrewSplashPotionEvent(player, potion);
			}
		}
	}
	
	//	The Player swapped their current selected item.
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerItemHeldEvent(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		final String name = player.getName();
		
		player.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, new Runnable() {
			@Override
			public void run() {
			    Player player = Bukkit.getPlayer(name);
			    mPlugin.getClass(player).PlayerItemHeldEvent(player);
			}
		}, 0);
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
	
	//	The player has died.
	@EventHandler(priority = EventPriority.HIGH)
	public void EntityDeathEvent(EntityDeathEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof LivingEntity) {
			LivingEntity livingEntity = (LivingEntity)entity;
			Player player = livingEntity.getKiller();
			if (player != null) {
				mPlugin.getClass(player).EntityDeathEvent(player, livingEntity);
			}
		}
	}
	
	//	The player interacts.
	@EventHandler(priority = EventPriority.HIGH)
	public void PlayerInteractEvent(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Material mat = (event.getClickedBlock() != null) ? event.getClickedBlock().getType() : Material.AIR;
		mPlugin.getClass(player).PlayerInteractEvent(player, event.getAction(), mat);
	}
	
	//	An Arrow hit something.
	@EventHandler(priority = EventPriority.HIGH)
	public void ProjectileHitEvent(ProjectileHitEvent event) {
		if (event.getEntityType() == EntityType.ARROW || event.getEntityType() == EntityType.TIPPED_ARROW) {
			Arrow arrow = (Arrow)event.getEntity();
			ProjectileSource source = arrow.getShooter();
			if (source instanceof Player) {
				Player player = (Player)source;
				
				mPlugin.getClass(player).ProjectileHitEvent(player, arrow);
				mPlugin.mProjectileEffectTimers.removeEntity(arrow);
			}
		}
	}
	
	//	A players thrown potion splashed.
	@EventHandler(priority = EventPriority.HIGH)
	public void PotionSplashEvent(PotionSplashEvent event) {
		if (event.getEntityType() == EntityType.SPLASH_POTION) {
			ThrownPotion potion = event.getPotion();
			ProjectileSource source = potion.getShooter();
			if (source instanceof Player) {
				Player player = (Player)source;
				boolean cancel = !mPlugin.getClass(player).PlayerSplashPotionEvent(player, event.getAffectedEntities(), potion);
				if (cancel) {
					event.setCancelled(true);
				}
			}
		}
	}
	
	//	Entity ran into the effect cloud.
	@EventHandler(priority = EventPriority.HIGH)
	public void AreaEffectCloudApplyEvent(AreaEffectCloudApplyEvent event) {
		ProjectileSource source = event.getEntity().getSource();
		
		if (source instanceof Player) {
			Player player = (Player)source;
			mPlugin.getClass(player).AreaEffectCloudApplyEvent(event.getAffectedEntities(), player);
		}
	}
}
