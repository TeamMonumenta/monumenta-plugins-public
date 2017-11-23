package pe.project.listeners;

import java.util.Iterator;
import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.TippedArrow;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;

import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import pe.project.Plugin;
import pe.project.classes.BaseClass;
import pe.project.locations.safezones.SafeZoneConstants;
import pe.project.locations.safezones.SafeZoneConstants.SafeZones;
import pe.project.managers.LocationManager;
import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.utils.PlayerUtils;
import pe.project.utils.PotionUtils;
import pe.project.utils.PotionUtils.PotionInfo;

public class EntityListener implements Listener {
	Plugin mPlugin;
	World mWorld;

	public EntityListener(Plugin plugin, World world) {
		mPlugin = plugin;
		mWorld = world;
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
				mPlugin.getClass(player).PlayerDamagedByLivingEntityEvent((Player)damagee, (LivingEntity)damager, event.getFinalDamage());
			} else if (damager instanceof Firework) {
				//	If we're hit by a rocket, cancel the damage.
				event.setCancelled(true);
			} else {
				if (damager instanceof Projectile) {
					Player player = (Player)damagee;
					if (!mPlugin.getClass(player).PlayerDamagedByProjectileEvent((Player)damagee, (Projectile)damager)) {
						damager.remove();
						event.setCancelled(true);
					}
				}
			}
		}
		//	Else if the entity getting hurt is a LivingEntity.
		else if (damagee instanceof LivingEntity) {
			Entity damager = event.getDamager();

			//	Hit by player.
			if (damager instanceof Player) {
				if (damagee instanceof Villager) {
					mPlugin.mNpcManager.interactEvent((Player)damager, damagee.getCustomName());
				} else {
					//	Make sure to not trigger class abilities off Throrns.
					if (event.getCause() != DamageCause.THORNS) {
						Player player = (Player)damager;

						BaseClass _class = mPlugin.getClass(player);
						_class.ModifyDamage(player, _class, event);
						_class.LivingEntityDamagedByPlayerEvent(player, (LivingEntity)damagee, event.getDamage(), event.getCause());
					}
				}
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

		//	Don't hurt Villagers!
		if (damagee instanceof Villager) {
			event.setCancelled(true);
		}
	}

	//	Entity Hurt Event.
	@EventHandler(priority = EventPriority.LOWEST)
	public void EntityDamageEvent(EntityDamageEvent event) {
		Entity damagee = event.getEntity();
		if (damagee instanceof Player) {
			Player player = (Player)damagee;
			World world = player.getWorld();
			DamageCause source = event.getCause();
			if (source == DamageCause.SUFFOCATION && player.getVehicle() != null) {
				//	If the player is suffocating inside a wall we need to figure out what block they're suffocating in.
				Location playerLoc = player.getLocation();

				for (double y = -0.5; y <= 0.5; y += 1) {
					for (double x = -0.5; x <= 0.5; x += 1) {
						for (double z = -0.5; z <= 0.5; z += 1) {
							final int ny = (int)Math.floor(playerLoc.getY() + y * 0.1f + (float)player.getEyeHeight());
							final int nx = (int)Math.floor(playerLoc.getX() + x * 0.48f);
							final int nz = (int)Math.floor(playerLoc.getZ() + z * 0.48f);

							Material type = player.getWorld().getBlockAt(new Location(world, nx, ny, nz)).getType();
							if (type == Material.BEDROCK) {
								//	Remove their vehicle if they had one.
								Entity vehicle = player.getVehicle();
								if (vehicle != null) {
									vehicle.eject();
									vehicle.remove();
								}

								//	Also Give the player a strike.
								PlayerUtils.awardStrike(player, "being somewhere you probably shouldn't have been.");

								return;
							}
						}
					}
				}
			}
		}
	}

	//	Hanging Entity hurt by another entity.
	@EventHandler(priority = EventPriority.LOWEST)
	public void HangingBreakByEntityEvent(HangingBreakByEntityEvent event) {
		Entity damager = event.getRemover();

		//	If hurt by a player in adventure mode we want to prevent the break;
		if (damager instanceof Player) {
			Player player = (Player)damager;
			if (player.getGameMode() == GameMode.ADVENTURE) {
				event.setCancelled(true);
			}
		}
		//	If hurt by an arrow from a player in adventure mode.
		else if (damager instanceof Arrow || damager instanceof TippedArrow) {
			Arrow arrow = (Arrow)damager;

			ProjectileSource source = arrow.getShooter();
			if (source instanceof Player) {
				Player player = (Player)source;
				if (player.getGameMode() == GameMode.ADVENTURE) {
					event.setCancelled(true);
				}
			}
		}
	}

	//	Entity Spawn Event.
	@EventHandler(priority = EventPriority.LOWEST)
	public void EntitySpawnEvent(EntitySpawnEvent event) {
		Entity entity = event.getEntity();
		mPlugin.mTrackingManager.addEntity(entity);
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

	//	A players thrown potion splashed.
	@EventHandler(priority = EventPriority.HIGH)
	public void PotionSplashEvent(PotionSplashEvent event) {
		if (event.getEntityType() == EntityType.SPLASH_POTION) {
			ThrownPotion potion = event.getPotion();
			ProjectileSource source = potion.getShooter();
			if (source instanceof Player) {
				Player player = (Player)source;

				if (LocationManager.withinAnySafeZone(player) != SafeZones.None) {
					event.setCancelled(true);
					return;
				}

				boolean cancel = !mPlugin.getClass(player).PlayerSplashPotionEvent(player, event.getAffectedEntities(), potion);
				if (cancel) {
					event.setCancelled(true);
				}

				Iterator<LivingEntity> iter = event.getAffectedEntities().iterator();
				while (iter.hasNext()) {
					LivingEntity entity = iter.next();
					//	All affected players need to have the effect added to their potion manager.
					if (entity instanceof Player) {
						mPlugin.mPotionManager.addPotion((Player)entity, PotionID.APPLIED_POTION, potion.getEffects());
					} else if (entity instanceof Villager) {
						iter.remove();
					}
				}
			}
		}
	}

	//	Entity ran into the effect cloud.
	@EventHandler(priority = EventPriority.HIGH)
	public void AreaEffectCloudApplyEvent(AreaEffectCloudApplyEvent event) {
		AreaEffectCloud cloud = event.getEntity();
		ProjectileSource source = cloud.getSource();
		if (source instanceof Player) {
			Player player = (Player)source;

			if (LocationManager.withinAnySafeZone(player) != SafeZones.None) {
				return;
			}

			List<LivingEntity> entities = event.getAffectedEntities();

			mPlugin.getClass(player).AreaEffectCloudApplyEvent(entities, player);

			PotionInfo data = PotionUtils.getPotionInfo(cloud.getBasePotionData());
			List<PotionEffect> effects = cloud.hasCustomEffects() ? cloud.getCustomEffects() : null;

			for (LivingEntity entity : entities) {
				if (entity instanceof Player) {
					Player p = (Player)entity;

					if (data != null) {
						mPlugin.mPotionManager.addPotion(p, PotionID.APPLIED_POTION, data);
					}

					if (effects != null) {
						mPlugin.mPotionManager.addPotion(p, PotionID.APPLIED_POTION, effects);
					}
				}
			}
		}
	}

	//	Entity Explode Event
	//	Cancel explosions in safezones
	@EventHandler(priority = EventPriority.LOWEST)
	public void EntityExplodeEvent(EntityExplodeEvent event) {
		// Cancel the event immediately if within a safezone
		SafeZones safeZone = SafeZoneConstants.withinAnySafeZone(event.getLocation());
		if (safeZone != SafeZones.None) {
			event.setCancelled(true);
			return;
		}

		// If any block damaged by an explosion is with a safezone, cancel the explosion
		for (Block block : event.blockList()) {
			if (SafeZoneConstants.withinAnySafeZone(block.getLocation()) != SafeZones.None) {
				event.setCancelled(true);
				return;
			}
		}
	}

	//	An Arrow hit something.
	@EventHandler(priority = EventPriority.HIGH)
	public void ProjectileHitEvent(ProjectileHitEvent event) {
		EntityType type = event.getEntityType();

		if (type == EntityType.TIPPED_ARROW) {
			Entity entity = event.getHitEntity();
			if (entity != null) {
				if (entity instanceof Player) {
					Player player = (Player)entity;

					TippedArrow arrow = (TippedArrow)event.getEntity();

					if (player.isBlocking()) {
						Vector to = player.getLocation().toVector();
						Vector from = arrow.getLocation().toVector();

						if (to.subtract(from).dot(player.getLocation().getDirection()) < 0) {
							PotionData data = new PotionData(PotionType.AWKWARD);
							arrow.setBasePotionData(data);

							if (arrow.hasCustomEffects()) {
								Iterator<PotionEffect> effectIter = arrow.getCustomEffects().iterator();
								while (effectIter.hasNext()) {
									PotionEffect effect = effectIter.next();
									arrow.removeCustomEffect(effect.getType());
								}
							}
						}
					}

					PotionInfo info = PotionUtils.getPotionInfo(arrow.getBasePotionData());
					List<PotionEffect> effects = arrow.getCustomEffects();

					if (info != null) {
						mPlugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, info);
					}

					if (effects != null) {
						mPlugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, effects);
					}
				}
			}
		}

		if (type == EntityType.ARROW || type == EntityType.TIPPED_ARROW) {
			Arrow arrow = (Arrow)event.getEntity();
			ProjectileSource source = arrow.getShooter();
			if (source instanceof Player) {
				Player player = (Player)source;

				mPlugin.getClass(player).ProjectileHitEvent(player, arrow);
				mPlugin.mProjectileEffectTimers.removeEntity(arrow);
			}
		}
	}
}
