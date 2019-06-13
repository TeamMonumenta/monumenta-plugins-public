package com.playmonumenta.plugins.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.TippedArrow;
import org.bukkit.entity.Trident;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityDismountEvent;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.cleric.Celestial;
import com.playmonumenta.plugins.abilities.cleric.hierophant.EnchantedPrayer;
import com.playmonumenta.plugins.enchantments.Duelist;
import com.playmonumenta.plugins.enchantments.Frost;
import com.playmonumenta.plugins.enchantments.Inferno;
import com.playmonumenta.plugins.enchantments.PointBlank;
import com.playmonumenta.plugins.enchantments.Impact;
import com.playmonumenta.plugins.enchantments.Slayer;
import com.playmonumenta.plugins.enchantments.Sniper;
import com.playmonumenta.plugins.events.BossAbilityDamageEvent;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.events.PotionEffectApplyEvent;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.safezone.SafeZoneManager.LocationType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;

public class EntityListener implements Listener {
	private static final Set<Material> ENTITY_UNINTERACTABLE_MATS = EnumSet.of(
	            Material.TRIPWIRE,
	            Material.TRIPWIRE_HOOK,
	            Material.OAK_PRESSURE_PLATE,
	            Material.ACACIA_PRESSURE_PLATE,
	            Material.BIRCH_PRESSURE_PLATE,
	            Material.DARK_OAK_PRESSURE_PLATE,
	            Material.JUNGLE_PRESSURE_PLATE,
	            Material.SPRUCE_PRESSURE_PLATE,
	            Material.STONE_PRESSURE_PLATE,
	            Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
	            Material.HEAVY_WEIGHTED_PRESSURE_PLATE
	        );

	public static final Set<DamageCause> DAMAGE_CAUSES_IGNORED_IN_TOWNS = EnumSet.of(
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

	Plugin mPlugin;
	World mWorld;
	AbilityManager mAbilities;

	public EntityListener(Plugin plugin, World world, AbilityManager abilities) {
		mPlugin = plugin;
		mWorld = world;
		mAbilities = abilities;
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
			Player player = (Player)combuster;

			/* Don't let the player interact with the world when transferring */
			if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)) {
				event.setCancelled(true);
				return;
			}

			MetadataUtils.checkOnceThisTick(mPlugin, combustee,
			                                Constants.ENTITY_COMBUST_NONCE_METAKEY);

		}

		if ((combustee instanceof Player)) {
			Player player = (Player)combustee;

			/* Don't let the player interact with the world when transferring */
			if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)) {
				event.setCancelled(true);
				return;
			}

			if (!mAbilities.PlayerCombustByEntityEvent(player, event)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void CustomDamageEvent(CustomDamageEvent event) {
		if (event.getDamager() instanceof Player) {
			mAbilities.PlayerDealtCustomDamageEvent((Player)event.getDamager(), event);
		}
	}

	//  An Entity hit another Entity.
	@EventHandler(priority = EventPriority.HIGH)
	public void EntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		Entity damagee = event.getEntity();
		Entity damager = event.getDamager();

		//  If the entity getting hurt is the player.
		if (damagee instanceof Player) {
			Player player = (Player)damagee;

			/* Don't let the player interact with the world when transferring */
			if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)) {
				event.setCancelled(true);
				return;
			}

			mPlugin.mTrackingManager.mPlayers.onHurtByEntity(mPlugin, player, event);
			if (damager instanceof LivingEntity) {
				if (!mAbilities.PlayerDamagedByLivingEntityEvent(player, event)) {
					event.setCancelled(true);
				}
				MetadataUtils.checkOnceThisTick(mPlugin, damagee, Constants.PLAYER_DAMAGE_NONCE_METAKEY);
			} else if (damager instanceof Firework) {
				//  If we're hit by a rocket, cancel the damage.
				event.setCancelled(true);
			} else if (damager instanceof Projectile) {
				if (!mAbilities.PlayerDamagedByProjectileEvent(player, event)) {
					damager.remove();
					event.setCancelled(true);
				}
			}
		} else {
			//  Don't hurt Villagers!
			if (damagee instanceof Villager) {
				event.setCancelled(true);
			}
			// Don't trigger class effects if the event was already cancelled (NPCs, etc.)
			if (event.isCancelled() || damagee instanceof ArmorStand || damagee.isInvulnerable()) {
				return;
			}
		}

		if (damager instanceof Player) {
			Player player = (Player)damager;
			if (damagee instanceof Player) {
				if (!mAbilities.isPvPEnabled((Player)damagee) || !mAbilities.isPvPEnabled((Player)damager)) {
					event.setCancelled(true);
					return;
				}
			}

			// Don't let the player interact with the world when transferring
			if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)) {
				event.setCancelled(true);
				return;
			}

			// Make sure to not trigger class abilities off Thorns
			if (event.getCause() != DamageCause.THORNS) {
				if (damagee instanceof LivingEntity && !(damagee instanceof Villager)) {
					// Apply any damage modifications that items they have may apply.
					mPlugin.mTrackingManager.mPlayers.onDamage(mPlugin, player, (LivingEntity)damagee, event);
					if (event.getCause().equals(DamageCause.ENTITY_ATTACK)
						&& !MetadataUtils.happenedThisTick(mPlugin, event.getDamager(), EntityUtils.PLAYER_DEALT_CUSTOM_DAMAGE_METAKEY, 0)) {
						mPlugin.mTrackingManager.mPlayers.onAttack(mPlugin, player, (LivingEntity)damagee, event);
					}
				}

				if (damagee instanceof LivingEntity && !(damagee instanceof Villager)) {
					if (event.getCause().equals(DamageCause.ENTITY_ATTACK)
					    && !MetadataUtils.happenedThisTick(mPlugin, event.getDamager(), EntityUtils.PLAYER_DEALT_CUSTOM_DAMAGE_METAKEY, 0)) {
						Celestial.modifyDamage(player, event);
					}
				}

				if (!mAbilities.LivingEntityDamagedByPlayerEvent(player, event)) {
					event.setCancelled(true);
				}

				if (damagee instanceof LivingEntity && !(damagee instanceof Villager)) {
					if (event.getCause().equals(DamageCause.ENTITY_ATTACK)
					    && !MetadataUtils.happenedThisTick(mPlugin, event.getDamager(), EntityUtils.PLAYER_DEALT_CUSTOM_DAMAGE_METAKEY, 0)) {
						EnchantedPrayer.onEntityAttack(mPlugin, player, (LivingEntity)damagee);
					}
				}
			}
		} else if (damager instanceof Arrow) {
			Arrow arrow = (Arrow)damager;
			if (arrow.getShooter() instanceof Player && damagee instanceof LivingEntity && !(damagee instanceof Villager)) {
				Player player = (Player)arrow.getShooter();
				mPlugin.mTrackingManager.mPlayers.onDamage(mPlugin, player, (LivingEntity)damagee, event);
				if (!mAbilities.LivingEntityShotByPlayerEvent(player, arrow, (LivingEntity)damagee, event)) {
					damager.remove();
					event.setCancelled(true);
				}
			}
		}

		if (damager instanceof Projectile && damagee instanceof LivingEntity) {
			Sniper.onShootAttack(mPlugin, (Projectile)damager, (LivingEntity)damagee, event);
			PointBlank.onShootAttack(mPlugin, (Projectile)damager, (LivingEntity)damagee, event);
			Frost.onShootAttack(mPlugin, (Projectile)damager, (LivingEntity)damagee, event);
			Inferno.onShootAttack(mPlugin, (Projectile)damager, (LivingEntity)damagee, event);
		}

		if (damager instanceof Trident && damagee instanceof LivingEntity) {
			Impact.onShootAttack(mPlugin, (Projectile)damager, (LivingEntity)damagee, event);
			Slayer.onShootAttack(mPlugin, (Projectile)damager, (LivingEntity)damagee, event);
			Duelist.onShootAttack(mPlugin, (Projectile)damager, (LivingEntity)damagee, event);
		}

		if (damagee instanceof LivingEntity) {
			LivingEntity mob = (LivingEntity) damagee;
			event.setDamage(event.getDamage() * EntityUtils.vulnerabilityMult(mob));
		}
	}

	// Entity Hurt Event.
	@EventHandler(priority = EventPriority.LOWEST)
	public void EntityDamageEvent(EntityDamageEvent event) {
		Entity damagee = event.getEntity();
		DamageCause source = event.getCause();
		if (damagee instanceof Player) {
			Player player = (Player)damagee;
			World world = player.getWorld();
			mPlugin.mTrackingManager.mPlayers.onHurt(mPlugin, player, event);
			/* Don't let the player interact with the world when transferring */
			if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)) {
				event.setCancelled(true);
				return;
			}

			LocationType locType = mPlugin.mSafeZoneManager.getLocationType(player.getLocation());
			if (locType == LocationType.Capital || locType == LocationType.SafeZone) {
				if (DAMAGE_CAUSES_IGNORED_IN_TOWNS.contains(source)) {
					event.setCancelled(true);
					return;
				}
			}

			if (!mAbilities.PlayerDamagedEvent(player, event)) {
				event.setCancelled(true);
			}

			if (source.equals(DamageCause.SUFFOCATION) && player.getVehicle() != null) {
				// If the player is suffocating inside a wall we need to figure out what block they're suffocating in.
				Location playerLoc = player.getLocation();

				for (double y = -0.5; y <= 0.5; y += 1) {
					for (double x = -0.5; x <= 0.5; x += 1) {
						for (double z = -0.5; z <= 0.5; z += 1) {
							final int ny = (int)Math.floor(playerLoc.getY() + y * 0.1f + (float)player.getEyeHeight());
							final int nx = (int)Math.floor(playerLoc.getX() + x * 0.48f);
							final int nz = (int)Math.floor(playerLoc.getZ() + z * 0.48f);

							Material type = player.getWorld().getBlockAt(new Location(world, nx, ny, nz)).getType();
							if (type == Material.BEDROCK) {
								// Remove their vehicle if they had one.
								Entity vehicle = player.getVehicle();
								if (vehicle != null) {
									vehicle.eject();
									vehicle.remove();
								}

								// Also Give the player a strike.
								PlayerUtils.awardStrike(mPlugin, player, "being somewhere you probably shouldn't have been.");

								return;
							}
						}
					}
				}
			}
		} else {
			// Not damaging a player

			// If this damage was caused by burning, check if the mob takes extra damage from Inferno
			if (source.equals(DamageCause.FIRE_TICK) && (damagee instanceof LivingEntity)) {
				Inferno.onFireTick((LivingEntity)damagee, event);
			}
		}
	}

	// Entity interacts with something
	@EventHandler(priority = EventPriority.LOWEST)
	public void EntityInteractEvent(EntityInteractEvent event) {
		Material material = event.getBlock().getType();

		if (ENTITY_UNINTERACTABLE_MATS.contains(material)) {
			Entity entity = event.getEntity();

			// Only items and players can activate tripwires
			// Also pigs, for the pig quest
			if (entity instanceof Item || entity instanceof Player || entity instanceof Pig) {
				return;
			}

			event.setCancelled(true);
		}
	}

	// Hanging Entity hurt by another entity.
	@EventHandler(priority = EventPriority.LOWEST)
	public void HangingBreakByEntityEvent(HangingBreakByEntityEvent event) {
		Entity damager = event.getRemover();

		// If hurt by a player in adventure mode we want to prevent the break;
		if (damager instanceof Player) {
			Player player = (Player)damager;

			/* Don't let the player interact with the world when transferring */
			if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)) {
				event.setCancelled(true);
				return;
			}

			if (player.getGameMode() == GameMode.ADVENTURE) {
				event.setCancelled(true);
			}
		}
		// If hurt by an arrow from a player in adventure mode.
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

	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityResurrectEvent(EntityResurrectEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof Player) {
			new BukkitRunnable() {
				@Override
				public void run() {
					mPlugin.mAbilityManager.updatePlayerAbilities((Player)entity);
				}
			}.runTaskLater(mPlugin, 1);
		}
	}

	// Entity Spawn Event.
	@EventHandler(priority = EventPriority.LOWEST)
	public void EntitySpawnEvent(EntitySpawnEvent event) {
		Entity entity = event.getEntity();
		mPlugin.mTrackingManager.addEntity(entity);
	}

	// Player shoots an arrow.
	@EventHandler(priority = EventPriority.HIGH)
	public void ProjectileLaunchEvent(ProjectileLaunchEvent event) {
		Projectile proj = event.getEntity();
		ProjectileSource shooter = proj.getShooter();
		if (shooter != null && shooter instanceof Player && ((Player)shooter).hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)) {
			event.setCancelled(true);
			return;
		}

		if (shooter instanceof Player) {
			Player player = (Player)shooter;

			mPlugin.mTrackingManager.mPlayers.onLaunchProjectile(mPlugin, player, proj, event);
			if (event.isCancelled()) {
				return;
			}

			if (event.getEntityType() == EntityType.SNOWBALL) {
				Snowball origBall = (Snowball)proj;
				ItemStack itemInMainHand = player.getEquipment().getItemInMainHand();
				ItemStack itemInOffHand = player.getEquipment().getItemInOffHand();

				// Check if the player has an infinity snowball in main or off hand
				if (((itemInMainHand.getType().equals(Material.SNOWBALL)) &&
				     (itemInMainHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE) > 0)) ||
				    ((itemInOffHand.getType().equals(Material.SNOWBALL)) &&
				     (itemInOffHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE) > 0))) {

					Snowball newBall = (Snowball)mWorld.spawnEntity(origBall.getLocation(), EntityType.SNOWBALL);
					newBall.setShooter(player);
					newBall.setVelocity(origBall.getVelocity());
					event.setCancelled(true);
					return;
				}
			} else if (event.getEntityType() == EntityType.ARROW || event.getEntityType() == EntityType.TIPPED_ARROW || event.getEntityType() == EntityType.SPECTRAL_ARROW) {
				Arrow arrow = (Arrow)proj;
				if (!mAbilities.PlayerShotArrowEvent(player, arrow)) {
					event.setCancelled(true);
				}

				MetadataUtils.checkOnceThisTick(mPlugin, player, Constants.PLAYER_BOW_SHOT_METAKEY);
			} else if (event.getEntityType() == EntityType.SPLASH_POTION) {
				SplashPotion potion = (SplashPotion)proj;
				if (!mAbilities.PlayerThrewSplashPotionEvent(player, potion)) {
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void PotionSplashEvent(PotionSplashEvent event) {
		ThrownPotion potion = event.getPotion();
		ProjectileSource source = potion.getShooter();
		Collection<LivingEntity> affectedEntities = event.getAffectedEntities();

		// Never apply effects to villagers
		affectedEntities.removeIf(entity -> (entity instanceof Villager));

		// Don't apply effects to invulnerable entities
		affectedEntities.removeIf(entity -> (entity.isInvulnerable()));

		/* Don't let the player interact with the world when transferring */
		affectedEntities.removeIf(entity -> (entity instanceof Player && ((Player)entity).hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)));

		/* If a potion has negative effects, don't apply them to any players except the thrower (if applicable) */
		if (source instanceof Player && PotionUtils.hasNegativeEffects(potion.getItem())) {
			affectedEntities.removeIf(entity -> (entity instanceof Player && entity != source));
		}

		/* If a player threw this potion, trigger applicable abilities (potentially cancelling or modifying the event!) */
		if (source instanceof Player) {
			if (!mAbilities.PlayerSplashPotionEvent((Player)source, affectedEntities, potion, event)) {
				event.setCancelled(true);
				return;
			}
		}

		/*
		 * If a player was hit by this potion, trigger applicable abilities (potentially cancelling or modifying the event!)
		 *
		 * Since the ability might modify the affectedEntities list while iterating, need to make a copy of it
		 */
		for (LivingEntity entity : new ArrayList<LivingEntity>(affectedEntities)) {
			if (entity instanceof Player) {
				if (!mAbilities.PlayerSplashedByPotionEvent((Player)entity, affectedEntities, potion, event)) {
					event.setCancelled(true);
					return;
				}
			}
		}

		// If event was not cancelled, track all player potion effects with the potion manager
		if (!event.isCancelled()) {
			for (LivingEntity entity : affectedEntities) {
				if (entity instanceof Player) {
					mPlugin.mPotionManager.addPotion((Player)entity, PotionID.APPLIED_POTION, PotionUtils.getEffects(potion.getItem()),
					                                 event.getIntensity(entity));
				}
			}
		}
	}

	// Entity ran into the effect cloud.
	@EventHandler(priority = EventPriority.HIGH)
	public void AreaEffectCloudApplyEvent(AreaEffectCloudApplyEvent event) {
		AreaEffectCloud cloud = event.getEntity();
		ProjectileSource source = cloud.getSource();
		Collection<LivingEntity> affectedEntities = event.getAffectedEntities();

		// Never apply effects to villagers
		affectedEntities.removeIf(entity -> (entity instanceof Villager));

		// Don't apply effects to invulnerable entities
		affectedEntities.removeIf(entity -> (entity.isInvulnerable()));

		// Don't apply effects to dead entities
		affectedEntities.removeIf(entity -> (entity.isDead() || entity.getHealth() <= 0));

		/* Don't let the player interact with the world when transferring */
		affectedEntities.removeIf(entity -> (entity instanceof Player && ((Player)entity).hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)));

		// Class effects from splashing potion
		if (source instanceof Player) {
			//Player player = (Player)source;

			//mPlugin.getClass(player).AreaEffectCloudApplyEvent(affectedEntities, player);
		}

		PotionData data = cloud.getBasePotionData();
		PotionInfo info = (data != null) ? PotionUtils.getPotionInfo(data, 4) : null;
		List<PotionEffect> effects = cloud.hasCustomEffects() ? cloud.getCustomEffects() : null;

		// All affected players need to have the effect added to their potion manager.
		for (LivingEntity entity : affectedEntities) {
			if (entity instanceof Player) {
				if (info != null) {
					mPlugin.mPotionManager.addPotion((Player)entity, PotionID.APPLIED_POTION, info);
				}

				if (effects != null) {
					mPlugin.mPotionManager.addPotion((Player)entity, PotionID.APPLIED_POTION, effects);
				}
			}
		}
	}

	// Cancel explosions in safezones
	@EventHandler(priority = EventPriority.LOWEST)
	public void EntityExplodeEvent(EntityExplodeEvent event) {
		// Cancel the event immediately if within a safezone
		LocationType zone = mPlugin.mSafeZoneManager.getLocationType(event.getLocation());
		if (zone != LocationType.None) {
			event.setCancelled(true);
			return;
		}

		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Block block = iter.next();

			// If any block damaged by an explosion is with a safezone, cancel the explosion
			if (mPlugin.mSafeZoneManager.getLocationType(block.getLocation()) != LocationType.None) {
				event.setCancelled(true);
				return;
			}

			// If this block is "unbreakable" than we want to remove it from the list.
			if (mPlugin.mServerProperties.mUnbreakableBlocks.contains(block.getType()) ||
			    !mPlugin.mItemOverrides.blockExplodeInteraction(mPlugin, block)) {
				iter.remove();
			}
		}
	}

	// Cancel explosions in safezones
	@EventHandler(priority = EventPriority.LOWEST)
	public void BlockExplodeEvent(BlockExplodeEvent event) {
		// Cancel the event immediately if within a safezone
		LocationType zone = mPlugin.mSafeZoneManager.getLocationType(event.getBlock().getLocation());
		if (zone != LocationType.None) {
			event.setCancelled(true);
			return;
		}

		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Block block = iter.next();

			// If any block damaged by an explosion is with a safezone, cancel the explosion
			if (mPlugin.mSafeZoneManager.getLocationType(block.getLocation()) != LocationType.None) {
				event.setCancelled(true);
				return;
			}

			// If this block is "unbreakable" than we want to remove it from the list.
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
		if (entity != null && entity instanceof Player) {
			Player player = (Player)entity;
			mAbilities.PlayerHitByProjectileEvent(player, event);
			if (type == EntityType.TIPPED_ARROW) {
				TippedArrow arrow = (TippedArrow)event.getEntity();

				/* Don't let the player interact with the world when transferring */
				if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)) {
					_removePotionDataFromArrow(arrow);
					return;
				}

				if (player.isBlocking()) {
					Vector to = player.getLocation().toVector();
					Vector from = arrow.getLocation().toVector();

					if (to.subtract(from).dot(player.getLocation().getDirection()) < 0) {
						_removePotionDataFromArrow(arrow);
					}
				}

				PotionData data = arrow.getBasePotionData();
				PotionInfo info = (data != null) ? PotionUtils.getPotionInfo(data, 8) : null;
				List<PotionEffect> effects = arrow.hasCustomEffects() ? arrow.getCustomEffects() : null;

				if (info != null) {
					mPlugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, info);
				}

				if (effects != null) {
					mPlugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, effects);
				}
			}
		}

		if (type == EntityType.ARROW || type == EntityType.TIPPED_ARROW || type == EntityType.SPECTRAL_ARROW) {
			Arrow arrow = (Arrow)event.getEntity();
			ProjectileSource source = arrow.getShooter();
			if (source instanceof Player) {
				Player player = (Player)source;
				mAbilities.ProjectileHitEvent(player, event, arrow);
			}
		}

		mPlugin.mProjectileEffectTimers.removeEntity(event.getEntity());

		if ((type == EntityType.SNOWBALL)
		    && (event.getHitEntity() instanceof LivingEntity)
		    && (!(event.getHitEntity() instanceof Player))
		    && (!event.getHitEntity().isInvulnerable())) {

			PotionEffect effect = new PotionEffect(PotionEffectType.SLOW, 300, 0, false);
			((LivingEntity)event.getHitEntity()).addPotionEffect(effect);
		}
	}

	private void _removePotionDataFromArrow(TippedArrow arrow) {
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


	@EventHandler(priority = EventPriority.LOWEST)
	public void ItemSpawnEvent(ItemSpawnEvent event) {
		mPlugin.mEnchantmentManager.ItemSpawnEvent(mPlugin, event.getEntity());
	}

	@EventHandler
	public void EntityTargetLivingEntityEvent(EntityTargetLivingEntityEvent event) {
		if (event.getEntity() instanceof Creature && (event.getEntity().hasMetadata(EntityUtils.MOB_IS_STUNNED_METAKEY)
			|| event.getEntity().hasMetadata(EntityUtils.MOB_IS_CONFUSED_METAKEY))) {
			event.setCancelled(true);
			return;
		}
		if (event.getTarget() instanceof Player) {
			Player player = (Player) event.getTarget();
			mAbilities.EntityTargetLivingEntityEvent(player, event);
		}
	}

	@EventHandler
	public void PotionEffectApplyEvent(PotionEffectApplyEvent event) {
		LivingEntity applied = (LivingEntity) event.getApplied();
		LivingEntity applier = (LivingEntity) event.getApplier();

		if (applier instanceof Player && !applied.hasPotionEffect(PotionEffectType.SLOW)
			&& event.getEffect().getType() == PotionEffectType.SLOW) {
			MetadataUtils.checkOnceThisTick(mPlugin, applied, Constants.ENTITY_SLOWED_NONCE_METAKEY);
		}

		if (applier instanceof Player) {
			Player player = (Player) applier;
			mAbilities.PotionEffectApplyEvent(player, event);
		}
	}

	@EventHandler
	public void EntityChangeBlockEvent(EntityChangeBlockEvent event) {
		event.setCancelled(!mPlugin.mItemOverrides.blockChangeInteraction(mPlugin, event.getBlock()));
		if (event.getEntity() instanceof Wither) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void EntityDismountEvent(EntityDismountEvent event) {
		if (event.getDismounted() instanceof ArmorStand) {
			event.getDismounted().remove();
		}
	}

	@EventHandler
	public void BossAbilityDamageEvent(BossAbilityDamageEvent event) {
		if (event.getDamaged() instanceof Player) {
			Player player = (Player) event.getDamaged();
			mPlugin.mTrackingManager.mPlayers.onBossDamage(mPlugin, player, event);
			mAbilities.BossAbilityDamageEvent(player, event);
		}
	}

}
