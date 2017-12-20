package pe.project.listeners;

import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.TippedArrow;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import pe.project.Constants;
import pe.project.Plugin;
import pe.project.classes.BaseClass;
import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.utils.LocationUtils;
import pe.project.utils.LocationUtils.LocationType;
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

	@EventHandler(priority = EventPriority.LOWEST)
	public void EntityCombustByEntityEvent(EntityCombustByEntityEvent event) {
		// Record the time of the player who sets a mob on fire
		// Used to prevent arcane strike from counting mobs on fire that were
		// set on fire by the same hit that triggered arcane strike
		// Only mark mobs that were not already burning
		Entity combustee = event.getEntity();
		Entity combuster = event.getCombuster();

		if (combustee.getFireTicks() <= 0) {
			combustee.setMetadata(Constants.ENTITY_COMBUST_NONCE_METAKEY,
								  new FixedMetadataValue(mPlugin, combuster.getTicksLived()));
		}
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
		} else if (damagee instanceof LivingEntity) {
			Entity damager = event.getDamager();

			if (damager instanceof Player) {
				if (damagee instanceof Villager) {
					mPlugin.mNpcManager.interactEvent((Player)damager, damagee.getCustomName());
				} else {
					//	Make sure to not trigger class abilities off Throrns.
					if (event.getCause() != DamageCause.THORNS) {
						Player player = (Player)damager;

						if (damagee.hasMetadata(Constants.ENTITY_DAMAGE_NONCE_METAKEY)
								&& damagee.getMetadata(Constants.ENTITY_DAMAGE_NONCE_METAKEY).get(0).asInt() == player.getTicksLived()) {
							// This damage was just added by the player's class - don't process class effects again
							return;
						} else {
							// New damage this tick - mark entity so that this event handler will be skipped if
							// more damage is applied by the player's class
							damagee.setMetadata(Constants.ENTITY_DAMAGE_NONCE_METAKEY,
							                    new FixedMetadataValue(mPlugin, player.getTicksLived()));
						}

						BaseClass _class = mPlugin.getClass(player);
						_class.ModifyDamage(player, _class, event);
						_class.LivingEntityDamagedByPlayerEvent(player, (LivingEntity)damagee, event.getDamage(), event.getCause());
					}
				}
			} else if (damager instanceof Arrow) {
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

	// Entity interacts with something
	@EventHandler(priority = EventPriority.LOWEST)
	public void EntityInteractEvent(EntityInteractEvent event) {
		Material material = event.getBlock().getType();

		if (material == Material.TRIPWIRE || material == Material.TRIPWIRE_HOOK) {
			Entity entity = event.getEntity();

			// Only items and players can activate tripwires
			if (entity instanceof Item || entity instanceof Player) {
				return;
			}

			event.setCancelled(true);
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
		if (event.getEntityType() == EntityType.SNOWBALL) {
			Snowball origBall = (Snowball)event.getEntity();
			if (origBall.getShooter() instanceof Player) {
				Player player = (Player)origBall.getShooter();
				ItemStack itemInHand = player.getEquipment().getItemInMainHand();
				if (itemInHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE) > 0) {
					// This is an infinite snowball - summon a new one and cancel the event
					Snowball newBall = (Snowball)mWorld.spawnEntity(origBall.getLocation(), EntityType.SNOWBALL);

					newBall.setShooter(player);
					newBall.setVelocity(origBall.getVelocity());
					//newBall.setGlowing(true);

					event.setCancelled(true);
					return;
				}
			}
		}
		if (event.getEntityType() == EntityType.ARROW || event.getEntityType() == EntityType.TIPPED_ARROW || event.getEntityType() == EntityType.SPECTRAL_ARROW) {
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

				if (LocationUtils.getLocationType(mPlugin, player) != LocationType.None) {
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

			if (LocationUtils.getLocationType(mPlugin, player) != LocationType.None) {
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
		LocationType zone = LocationUtils.getLocationType(mPlugin, event.getLocation());
		if (zone != LocationType.None) {
			event.setCancelled(true);
			return;
		}

		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Block block = iter.next();

			// If any block damaged by an explosion is with a safezone, cancel the explosion
			if (LocationUtils.getLocationType(mPlugin, block.getLocation()) != LocationType.None) {
				event.setCancelled(true);
				return;
			}

			//	If this block is "unbreakable" than we want to remove it from the list.
			if (mPlugin.mServerProporties.mUnbreakableBlocks.contains(block.getType())) {
				iter.remove();
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

		if (type == EntityType.ARROW || type == EntityType.TIPPED_ARROW || type == EntityType.SPECTRAL_ARROW) {
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
