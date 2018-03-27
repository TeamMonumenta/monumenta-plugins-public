package pe.project.listeners;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

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
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import pe.project.Constants;
import pe.project.Plugin;
import pe.project.classes.BaseClass;
import pe.project.managers.ZoneManager;
import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.utils.InventoryUtils;
import pe.project.utils.LocationUtils;
import pe.project.utils.LocationUtils.LocationType;
import pe.project.utils.MetadataUtils;
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

		if ((combuster instanceof Player) && (combustee.getFireTicks() <= 0)) {
			MetadataUtils.checkOnceThisTick(mPlugin, combuster,
			                                Constants.ENTITY_COMBUST_NONCE_METAKEY);
		}

		if ((combustee instanceof Player)) {
			Player player = (Player)combustee;
			if (!mPlugin.getClass(player).PlayerCombustByEntityEvent(player, combuster)) {
				event.setCancelled(true);
			}
		}
	}

	//  An Entity hit another Entity.
	@EventHandler(priority = EventPriority.LOWEST)
	public void EntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		Entity damagee = event.getEntity();
		Entity damager = event.getDamager();

		//  If the entity getting hurt is the player.
		if (damagee instanceof Player) {
			if (damager instanceof LivingEntity) {
				Player player = (Player)damagee;
				if(!mPlugin.getClass(player).PlayerDamagedByLivingEntityEvent((Player)damagee, (LivingEntity)damager,
				                                                              event.getFinalDamage())) 	{
					event.setCancelled(true);
				}

			} else if (damager instanceof Firework) {
				//  If we're hit by a rocket, cancel the damage.
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
		} else {
			if (damager instanceof Player) {
				Player player = (Player)damager;

				//  Make sure to not trigger class abilities off Throrns.
				if (event.getCause() != DamageCause.THORNS) {
					if (damagee instanceof LivingEntity && !(damagee instanceof Villager)) {
						if (!MetadataUtils.checkOnceThisTick(mPlugin, player, Constants.ENTITY_DAMAGE_NONCE_METAKEY)) {
							// This damage was just added by the player's class - don't process class effects again
							return;
						}

						//	Apply any damage modifications that items they have may apply.
						double damage = mPlugin.mTrackingManager.mPlayers.onAttack(mPlugin, player.getWorld(), player,
								(LivingEntity)damagee, event.getDamage(), event.getCause());

						event.setDamage(Math.max(damage, 0));

						BaseClass _class = mPlugin.getClass(player);
						_class.ModifyDamage(player, _class, event);
						_class.LivingEntityDamagedByPlayerEvent(player, (LivingEntity)damagee, event.getDamage(), event.getCause());
					}
				}
			} else if (damager instanceof Arrow) {
				Arrow arrow = (Arrow)damager;
				if (arrow.getShooter() instanceof Player && damagee instanceof LivingEntity && !(damagee instanceof Villager)) {
					Player player = (Player)arrow.getShooter();

					BaseClass _class = mPlugin.getClass(player);
					_class.ModifyDamage(player, _class, event);
					_class.LivingEntityShotByPlayerEvent(player, arrow, (LivingEntity)damagee, event);
				}
			}
		}

		//  Don't hurt Villagers!
		if (damagee instanceof Villager) {
			event.setCancelled(true);
		}
	}

	public static EnumSet<DamageCause> damageCausesIgnoredInTowns = EnumSet.of(
		DamageCause.FALL,
		DamageCause.FALLING_BLOCK,
		DamageCause.FIRE,
		DamageCause.FIRE_TICK,
		DamageCause.FLY_INTO_WALL,
		DamageCause.MAGIC,
		DamageCause.POISON,
		DamageCause.PROJECTILE,
		DamageCause.STARVATION,
		DamageCause.THORNS,
		DamageCause.WITHER
	);

	//	Entity Hurt Event.
	@EventHandler(priority = EventPriority.LOWEST)
	public void EntityDamageEvent(EntityDamageEvent event) {
		Entity damagee = event.getEntity();
		if (damagee instanceof Player) {
			Player player = (Player)damagee;
			World world = player.getWorld();
			DamageCause source = event.getCause();

			LocationType locType = LocationUtils.getLocationType(mPlugin, player.getLocation());
			if (locType == LocationType.Capital || locType == LocationType.SafeZone) {
				if (damageCausesIgnoredInTowns.contains(source)) {
					event.setCancelled(true);
				}
			}

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

		if (material == Material.TRIPWIRE || material == Material.TRIPWIRE_HOOK
			|| material == Material.WOOD_PLATE || material == Material.STONE_PLATE
			|| material == Material.GOLD_PLATE || material == Material.IRON_PLATE) {
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
		mPlugin.mZoneManager.applySpawnEffect(entity);
	}

	//	Player shoots an arrow.
	@EventHandler(priority = EventPriority.HIGH)
	public void ProjectileLaunchEvent(ProjectileLaunchEvent event) {
		if (event.getEntityType() == EntityType.SNOWBALL) {
			Snowball origBall = (Snowball)event.getEntity();
			if (origBall.getShooter() instanceof Player) {
				Player player = (Player)origBall.getShooter();
				ItemStack itemInMainHand = player.getEquipment().getItemInMainHand();
				ItemStack itemInOffHand = player.getEquipment().getItemInOffHand();

				// Check if the player has an infinity snowball in main or off hand
				if (((itemInMainHand.getType().equals(Material.SNOW_BALL)) &&
				     (itemInMainHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE) > 0)) ||
				    ((itemInOffHand.getType().equals(Material.SNOW_BALL)) &&
				     (itemInOffHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE) > 0))) {
					// If they do, cancel the event. This means players can't throw eachother's
					// soulbound snowballs
					event.setCancelled(true);

					if (((itemInMainHand.getType().equals(Material.SNOW_BALL)) &&
						 (itemInMainHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE) > 0) &&
						 (InventoryUtils.isSoulboundToPlayer(itemInMainHand, player))) ||
						((itemInOffHand.getType().equals(Material.SNOW_BALL)) &&
						 (itemInOffHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE) > 0) &&
						 (InventoryUtils.isSoulboundToPlayer(itemInOffHand, player)))) {

						Snowball newBall = (Snowball)mWorld.spawnEntity(origBall.getLocation(), EntityType.SNOWBALL);

						newBall.setShooter(player);
						newBall.setVelocity(origBall.getVelocity());
					}
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

	@EventHandler(priority = EventPriority.HIGH)
	public void PotionSplashEvent(PotionSplashEvent event) {
		ThrownPotion potion = event.getPotion();
		ProjectileSource source = potion.getShooter();
		Collection<LivingEntity> affectedEntities = event.getAffectedEntities();

		// If we are in any type of safezone don't apply splash effects to non-players
		if (LocationUtils.getLocationType(mPlugin, potion.getLocation()) != LocationType.None) {
			affectedEntities.removeIf(entity -> (!(entity instanceof Player)));
		}

		// Never apply effects to villagers
		affectedEntities.removeIf(entity -> (entity instanceof Villager));

		// Don't apply effects to invulnerable entities
		affectedEntities.removeIf(entity -> (entity.isInvulnerable()));

		if (source instanceof Player) {
			// If thrown by a player, that player's class determines how entities are affected
			Player player = (Player)source;

			if (!mPlugin.getClass(player).PlayerSplashPotionEvent(player, affectedEntities, potion, event)) {
				event.setCancelled(true);
				return;
			}
		} else {
			// If not thrown by a player, add tracked effects to the potion manager
			for (LivingEntity entity : affectedEntities) {
				if (entity instanceof Player) {
					mPlugin.mPotionManager.addPotion((Player)entity, PotionID.APPLIED_POTION, potion.getEffects(),
													 event.getIntensity(entity));
				}
			}
		}
	}

	//	Entity ran into the effect cloud.
	@EventHandler(priority = EventPriority.HIGH)
	public void AreaEffectCloudApplyEvent(AreaEffectCloudApplyEvent event) {
		AreaEffectCloud cloud = event.getEntity();
		ProjectileSource source = cloud.getSource();
		Collection<LivingEntity> affectedEntities = event.getAffectedEntities();
		List<PotionEffect> effects = cloud.hasCustomEffects() ? cloud.getCustomEffects() : null;

		// If we are in any type of safezone don't apply splash effects to non-players
		if (LocationUtils.getLocationType(mPlugin, cloud.getLocation()) != LocationType.None) {
			affectedEntities.removeIf(entity -> (!(entity instanceof Player)));
		}

		// Never apply effects to villagers
		affectedEntities.removeIf(entity -> (entity instanceof Villager));

		// Don't apply effects to invulnerable entities
		affectedEntities.removeIf(entity -> (entity.isInvulnerable()));

		// Class effects from splashing potion
		if (source instanceof Player) {
			Player player = (Player)source;

			mPlugin.getClass(player).AreaEffectCloudApplyEvent(affectedEntities, player);
		}

		//	All affected players need to have the effect added to their potion manager.
		for (LivingEntity entity : affectedEntities) {
			if (entity instanceof Player) {
				// TODO: Base potion data from lingering potions isn't applied here. The straightforward implementation
				// (check git history) results in full-duration effects being applied to the player, for example 8 minutes instead of 2

				if (effects != null) {
					mPlugin.mPotionManager.addPotion((Player)entity, PotionID.APPLIED_POTION, effects);
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
			if (mPlugin.mServerProperties.mUnbreakableBlocks.contains(block.getType()) ||
					!mPlugin.mItemOverrides.blockExplodeInteraction(mPlugin, block)) {
				iter.remove();
			}
		}
	}

	// Never generate new trades
	@EventHandler(priority = EventPriority.LOWEST)
	public void VillagerAcquireTradeEvent(VillagerAcquireTradeEvent event) {
		event.setCancelled(true);
	}

	//  An Arrow hit something.
	@EventHandler(priority = EventPriority.HIGH)
	public void ProjectileHitEvent(ProjectileHitEvent event) {
		EntityType type = event.getEntityType();

		Entity entity = event.getHitEntity();
		if (entity != null) {
			if (entity instanceof Player) {
				Player player = (Player)entity;

				// Give classes a chance to modify the projectile first
				mPlugin.getClass(player).ProjectileHitPlayerEvent(player, event.getEntity());

				if (type == EntityType.TIPPED_ARROW) {
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

		if ((type == EntityType.SNOWBALL)
			&& (event.getHitEntity() instanceof LivingEntity)
			&& (!(event.getHitEntity() instanceof Player))
			&& (!event.getHitEntity().isInvulnerable())) {

			PotionEffect effect = new PotionEffect(PotionEffectType.SLOW, 300, 0, false);
			((LivingEntity)event.getHitEntity()).addPotionEffect(effect);
		}
	}
}
