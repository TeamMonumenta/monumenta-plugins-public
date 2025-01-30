package com.playmonumenta.plugins.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.bosses.bosses.abilities.AlchemicalAberrationBoss;
import com.playmonumenta.plugins.depths.abilities.steelsage.SteelStallion;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.PotionEffectApplyEvent;
import com.playmonumenta.plugins.itemstats.enchantments.Inferno;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.player.EnderPearlTracker;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import de.tr7zw.nbtapi.NBTEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityEnterBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.entity.VillagerReplenishTradeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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

	public static final String INVULNERABLE_ITEM_TAG = "MonumentaInvulnerableItem";
	public static final String INVISIBLE_ITEM_FRAME_NAME = "Invisible Item Frame";
	public static final String BEES_BLOCK_HIVE_ENTER_EVENT = "BeeNoHive";
	private static final NamespacedKey INVISIBLE_ITEM_FRAME_LOOT_TABLE = NamespacedKeyUtils.fromString("epic:items/invisible_item_frame");

	private static final String FALLING_BLOCK_ADVENTURE_MODE_METADATA_KEY = "MonumentaFallingBlockAdventureMode";
	private static final String FALLING_BLOCK_NO_PLACE_ADVENTURE_MODE_TAG = "DisableBlockPlacementAdventureMode";

	private static final String AREA_EFFECT_CLOUD_POTION_METAKEY = "MonumentaAreaEffectCloudPotion";

	public static final String PROJECTILE_PASSTHROUGH_TAG = "projectile_passthrough";

	Plugin mPlugin;
	AbilityManager mAbilities;

	public EntityListener(Plugin plugin, AbilityManager abilities) {
		mPlugin = plugin;
		mAbilities = abilities;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void entityCombustByEntityEvent(EntityCombustByEntityEvent event) {
		Entity combustee = event.getEntity();
		Entity combuster = event.getCombuster();

		// Record the time of the player who sets a mob on fire
		// Used to prevent arcane strike from counting mobs on fire that were
		// set on fire by the same hit that triggered arcane strike
		// Only mark mobs that were not already burning
		if ((combuster instanceof Player) && (combustee.getFireTicks() <= 0)) {
			MetadataUtils.checkOnceThisTick(mPlugin, combustee,
				Constants.ENTITY_COMBUST_NONCE_METAKEY);
		}

		if ((combuster instanceof Player player) && (combustee.getFireTicks() > 0)) {
			if (player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.FIRE_ASPECT)
				&& combustee instanceof LivingEntity && Inferno.hasInferno(mPlugin, (LivingEntity) combustee)
				&& mPlugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.INFERNO) < Inferno.getInfernoLevel(mPlugin, (LivingEntity) combustee)) {
				event.setCancelled(true);
				return;
			}
		}

		if ((combustee instanceof Player player)) {

			if (!mAbilities.playerCombustByEntityEvent(player, event)) {
				event.setCancelled(true);
				return;
			}

			mPlugin.mItemStatManager.onCombust(mPlugin, player, event);
		}
	}

	//  An Entity hit another Entity.
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void entityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		Entity damagee = event.getEntity();
		Entity damager = event.getDamager();

		if (!EntityUtils.isHostileMob(damagee) && damager.getScoreboardTags().contains(AlchemicalAberrationBoss.identityTag)) {
			event.setCancelled(true);
			return;
		}

		//  If the entity getting hurt is the player.
		if (damagee instanceof Player) {
			if (damager instanceof LivingEntity) {
				MetadataUtils.checkOnceThisTick(mPlugin, damagee, Constants.PLAYER_DAMAGE_NONCE_METAKEY);
			} else if (damager instanceof Firework) {
				//  If we're hit by a rocket, cancel the damage.
				event.setCancelled(true);
				return;
			}

			// Disable thorns damage from guardians
			if (event.getCause() == DamageCause.THORNS && damager instanceof Guardian) {
				event.setCancelled(true);
				return;
			}
		} else {
			//  Don't hurt Villagers!
			if (damagee instanceof Villager) {
				event.setCancelled(true);
				return;
			}
			// Don't allow creepers to destroy items if either is in water
			if (damager instanceof Creeper && damagee instanceof Item && (damager.isInWater() || damagee.isInWater())) {
				event.setCancelled(true);
				return;
			}
			if (damagee instanceof ArmorStand || damagee.isInvulnerable()) {
				return;
			}
			// Don't allow evoker fangs to damage non-players
			if (damager instanceof EvokerFangs) {
				event.setCancelled(true);
				return;
			}
		}

		if (damager instanceof Player playerDamager) {
			if (damagee instanceof Player) {
				event.setCancelled(true);
				return;
			}

			// Plot Security: If damagee is inside a plot but the player is in adventure, cancel.
			if (playerDamager.getGameMode() == GameMode.ADVENTURE && ZoneUtils.isInPlot(damagee)) {
				event.setCancelled(true);
			}
		} else if (damager instanceof Projectile proj && proj.getShooter() instanceof Player player) {
			// Plot Security: If damagee is inside a plot but the player is in adventure, cancel.
			if (player.getGameMode() == GameMode.ADVENTURE && ZoneUtils.isInPlot(damagee)) {
				damager.remove();
				event.setCancelled(true);
				return;
			}

			if (damagee instanceof Player) {
				damager.remove();
				event.setCancelled(true);
			}
		}
	}

	// Entity Hurt Event.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void entityDamageEvent(EntityDamageEvent event) {
		Entity damagee = event.getEntity();
		DamageCause source = event.getCause();

		if ((source == DamageCause.BLOCK_EXPLOSION || source == DamageCause.ENTITY_EXPLOSION) &&
			(damagee.getScoreboardTags().contains("ExplosionImmune") || damagee instanceof ItemFrame || damagee instanceof Painting)) {
			event.setCancelled(true);
			return;
		}

		if (damagee instanceof ItemFrame || damagee instanceof Painting) {
			// Attempting to damage an item frame
			if (event instanceof EntityDamageByEntityEvent edbee) {
				// This event is damage attributable to an entity
				if (// This damage is from an entity, but that entity is not a player
					!(edbee.getDamager() instanceof Player player)
						// OR The damage is from a player but the item frame/painting is invulnerable and the player is not in creative
						|| (damagee.isInvulnerable() && !player.getGameMode().equals(GameMode.CREATIVE))
						// OR the damage is from a player, and they are in adventure mode, and the item frame is not tagged "Removable"
						|| (player.getGameMode().equals(GameMode.ADVENTURE) && !damagee.getScoreboardTags().contains("Removable"))) {
					// Don't allow it
					event.setCancelled(true);
					return;
				}
			} else {
				// This damage is not from a particular entity source - don't allow it
				event.setCancelled(true);
				return;
			}
			if (damagee instanceof ItemFrame frame
				&& INVISIBLE_ITEM_FRAME_NAME.equals(damagee.getName())) {
				Bukkit.getScheduler().runTask(mPlugin, () -> {
					if (frame.isValid()) {
						new NBTEntity(frame).setBoolean("Invisible", !ItemUtils.isNullOrAir(frame.getItem()));
					}
				});
			}
			// No more processing needed for invulnerable item frames/paintings
			return;
		}

		if (damagee instanceof Player player) {
			World world = player.getWorld();

			if (ZoneUtils.hasZoneProperty(player.getLocation(), ZoneProperty.RESIST_5)) {
				if (DAMAGE_CAUSES_IGNORED_IN_TOWNS.contains(source)) {
					event.setCancelled(true);
					return;
				}
			}

			if (source.equals(DamageCause.SUFFOCATION) && player.getVehicle() != null) {
				// If the player is suffocating inside a wall we need to figure out what block they're suffocating in.
				Location playerLoc = player.getLocation();

				for (double y = -0.5; y <= 0.5; y += 1) {
					for (double x = -0.5; x <= 0.5; x += 1) {
						for (double z = -0.5; z <= 0.5; z += 1) {
							final int ny = (int) Math.floor(playerLoc.getY() + y * 0.1f + (float) player.getEyeHeight());
							final int nx = (int) Math.floor(playerLoc.getX() + x * 0.48f);
							final int nz = (int) Math.floor(playerLoc.getZ() + z * 0.48f);

							Material type = player.getWorld().getBlockAt(new Location(world, nx, ny, nz)).getType();
							if (type == Material.BEDROCK) {
								// Remove their vehicle if they had one.
								Entity vehicle = player.getVehicle();
								if (vehicle != null) {

									//Fix for a Depths skill that caused too many false positives
									//Do not award strike or remove vehicle if the player is on the Steel Stallion horse
									if (SteelStallion.isSteelStallion(vehicle)) {
										return;
									}

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
		} else if (damagee instanceof Item) {
			if (damagee.getTicksLived() <= 100 && (source.equals(DamageCause.ENTITY_EXPLOSION) || source.equals(DamageCause.BLOCK_EXPLOSION))) {
				event.setCancelled(true);
				return;
			}
			if (damagee.getScoreboardTags().contains(INVULNERABLE_ITEM_TAG) && !source.equals(DamageCause.LAVA) && !DamageEvent.DamageType.is(source, DamageEvent.DamageType.TRUE)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void damageEvent(DamageEvent event) {
		// Prevent shop shulkers from taking damage, no matter what
		if (EntityUtils.isShopShulker(event.getDamagee())) {
			event.setFlatDamage(0);
		}
	}

	// Entity interacts with something
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityInteractEvent(EntityInteractEvent event) {
		Material material = event.getBlock().getType();

		if (ENTITY_UNINTERACTABLE_MATS.contains(material)) {
			Entity entity = event.getEntity();

			// Only items and players can activate tripwires
			// Also pigs, for the pig quest
			if (entity instanceof Item || entity instanceof Player || entity instanceof Pig || entity.getScoreboardTags().contains("block_interact")) {
				return;
			}

			event.setCancelled(true);
		}
	}

	// Hanging Entity hurt by another entity.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void hangingBreakByEntityEvent(HangingBreakByEntityEvent event) {
		Entity damager = event.getRemover();
		Hanging entity = event.getEntity();
		Location loc = entity.getLocation();

		if (ZoneUtils.hasZoneProperty(loc, ZoneProperty.ADVENTURE_MODE)) {
			if (damager instanceof Player && entity instanceof ItemFrame itemFrame && itemFrame.getItem().getType() != Material.AIR && ZoneUtils.hasZoneProperty(loc, ZoneProperty.ITEM_FRAMES_EDITABLE)) {
				return;
			}

			if (!(damager instanceof Player player
				      && (player.getGameMode() == GameMode.CREATIVE
					          || (player.getGameMode() == GameMode.SURVIVAL
						              && ZoneUtils.isInPlot(player)
						              && ZoneUtils.isInPlot(loc))))) {
				event.setCancelled(true);
				return;
			}
		}

		if (damager instanceof Player player) {
			// If hurt by a player in adventure mode we want to prevent the break;

			if (player.getGameMode() == GameMode.ADVENTURE) {
				event.setCancelled(true);
			}
		} else if (damager instanceof Projectile projectile) {
			// If hurt by a projectile from a player in adventure mode.

			ProjectileSource source = projectile.getShooter();
			if (source instanceof Player player) {
				if (player.getGameMode() == GameMode.ADVENTURE) {
					event.setCancelled(true);
				}
			}
		} else if (damager instanceof Creeper creeper) {
			if (creeper.getScoreboardTags().contains(AlchemicalAberrationBoss.identityTag)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityResurrectEvent(EntityResurrectEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof Player player) {
			//Updates custom enchants in inventory
			InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);

			new BukkitRunnable() {
				@Override
				public void run() {
					mPlugin.mAbilityManager.updatePlayerAbilities(player, false);
				}
			}.runTaskLater(mPlugin, 1);
		}
	}

	// Entity Spawn Event.
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entitySpawnEvent(EntitySpawnEvent event) {
		Entity entity = event.getEntity();
		mPlugin.mTrackingManager.addEntity(entity);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void entityAddToWorldEvent(EntityRemoveFromWorldEvent event) {
		Entity entity = event.getEntity();
		mPlugin.mTrackingManager.removeEntity(entity);
	}

	// Player shoots an arrow.
	//TODO PlayerLaunchProjectileEvent to access ItemStack
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void projectileLaunchEvent(ProjectileLaunchEvent event) {
		Projectile proj = event.getEntity();
		ProjectileSource shooter = proj.getShooter();
		if (shooter instanceof Player player) {

			/*
			 * Too many bugs arise as a result of being able to shoot things from offhand.
			 */
			if (ItemUtils.isShootableItem(player.getInventory().getItemInOffHand()) && player.getInventory().getItemInOffHand().getType() != Material.FIREWORK_ROCKET) {
				event.setCancelled(true);
				return;
			}

			mPlugin.mItemStatManager.onLaunchProjectile(mPlugin, player, event, proj);

			/* NOTE:
			 * Ignore IntelliJ's warning about the method annotation here. onLaunchProjectile can cancel the event.
			 */
			if (event.isCancelled()) {
				return;
			}

			// TODO: change the infinity stuff to lowest priority listener, and cancel and re-call the event so that other listeners can catch the proper event

			if (event.getEntityType() == EntityType.SNOWBALL) {
				Snowball origBall = (Snowball) proj;
				ItemStack itemInMainHand = player.getEquipment().getItemInMainHand();
				if (!mAbilities.playerShotProjectileEvent(player, proj)) {
					event.setCancelled(true);
				}

				// Check if the player has an infinity snowball and not throw rate
				if (itemInMainHand.getType().equals(Material.SNOWBALL)
					&& (itemInMainHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE) > 0 || ItemStatUtils.hasEnchantment(itemInMainHand, EnchantmentType.INFINITY))
					&& ItemStatUtils.getAttributeAmount(itemInMainHand, AttributeType.THROW_RATE, Operation.ADD, Slot.MAINHAND) == 0) {
					Snowball newBall = (Snowball) origBall.getWorld().spawnEntity(origBall.getLocation(), EntityType.SNOWBALL);

					// Copy the item's name/etc. so it can be textured
					newBall.getItem().setItemMeta(itemInMainHand.getItemMeta());
					ItemUtils.setPlainTag(newBall.getItem());

					newBall.setShooter(player);
					newBall.setVelocity(origBall.getVelocity());
					player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 0.4f, 0.5f);
					event.setCancelled(true);
					clearSnowballProjectile(newBall); // clearing infinity non-weapon snowballs
					return;
				}
				clearSnowballProjectile(origBall); // clearing plain snowballs
			} else if (event.getEntityType() == EntityType.ENDER_PEARL) {
				EnderPearl origPearl = (EnderPearl) proj;
				ItemStack itemInMainHand = player.getEquipment().getItemInMainHand();

				// Check if the player has an infinity ender pearl
				if (itemInMainHand.getType().equals(Material.ENDER_PEARL)
					&& (itemInMainHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE) > 0 || ItemStatUtils.hasEnchantment(itemInMainHand, EnchantmentType.INFINITY))) {
					EnderPearl newPearl = (EnderPearl) origPearl.getWorld().spawnEntity(origPearl.getLocation(), EntityType.ENDER_PEARL);

					// Copy the item's name/etc. so it can be textured
					newPearl.getItem().setItemMeta(itemInMainHand.getItemMeta());
					ItemUtils.setPlainTag(newPearl.getItem());

					newPearl.setShooter(player);
					newPearl.setVelocity(origPearl.getVelocity());
					event.setCancelled(true);
					if (!ZoneUtils.hasZoneProperty(player.getLocation(), ZoneProperty.NO_MOBILITY_ABILITIES) && !ZoneUtils.hasZoneProperty(player.getLocation(), ZoneProperty.DISABLE_MAGIC_TESS)) {
						EnderPearlTracker.startTracking(player, newPearl);
					}
				} else {
					if (!ZoneUtils.hasZoneProperty(player.getLocation(), ZoneProperty.NO_MOBILITY_ABILITIES) && !ZoneUtils.hasZoneProperty(player.getLocation(), ZoneProperty.DISABLE_MAGIC_TESS)) {
						EnderPearlTracker.startTracking(player, origPearl);
					}
				}
			} else if (event.getEntity() instanceof AbstractArrow arrow) {
				// Includes arrows and spectral arrows
				// Tridents are handled in ThrowRate
				if (!mAbilities.playerShotProjectileEvent(player, arrow)) {
					event.setCancelled(true);
					return;
				}

				MetadataUtils.checkOnceThisTick(mPlugin, player, Constants.PLAYER_BOW_SHOT_METAKEY);
			} else if (event.getEntityType() == EntityType.SPLASH_POTION) {
				ThrownPotion potion = (ThrownPotion) proj;
				ItemStack potionItem = potion.getItem();

				if (potionItem.getType().equals(Material.SPLASH_POTION)
					&& (potionItem.getEnchantmentLevel(Enchantment.ARROW_INFINITE) > 0 || ItemStatUtils.hasEnchantment(potionItem, EnchantmentType.INFINITY) || ItemStatUtils.hasEnchantment(potionItem, EnchantmentType.ALCHEMICAL_ALEMBIC))) {
					ThrownPotion potionClone = (ThrownPotion) potion.getWorld().spawnEntity(potion.getLocation(), EntityType.SPLASH_POTION);
					ItemStack newPotion = potionItem.clone();
					if (newPotion.hasItemMeta() && newPotion.getItemMeta().hasLore()) {
						List<Component> lore = newPotion.lore();
						lore.removeIf((component) -> !MessagingUtils.plainText(component).contains("* Alchemical Utensil *"));
						newPotion.lore(lore);
					}
					ItemUtils.setPlainTag(newPotion);

					potionClone.setItem(newPotion);
					potionClone.setShooter(player);
					potionClone.setVelocity(potion.getVelocity());
					//this potion should not have other metadata
					event.setCancelled(true);
					potion = potionClone;
				}

				if (potionItem.getType() == Material.SPLASH_POTION) {
					if (!mAbilities.playerThrewSplashPotionEvent(player, potion, event)) {
						event.setCancelled(true);
						return;
					}
				} else if (potionItem.getType() == Material.LINGERING_POTION) {
					if (!mAbilities.playerThrewLingeringPotionEvent(player, potion)) {
						event.setCancelled(true);
						return;
					}
				}

				// Remove thrown potions after 10 seconds.
				// This is so they don't get stuck in water bubble columns forever.
				ThrownPotion finalPotion = potion;
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					if (finalPotion != null) {
						finalPotion.remove();
					}
				}, 10 * 20);
			}
		}
	}

	// Remove snowballs thrown by the player after 10 seconds.
	// This is so they don't get stuck in water bubble columns forever.
	public static void clearSnowballProjectile(Snowball snowball) {
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			if (snowball.isValid()) {
				snowball.remove();
			}
		}, 10 * 20);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void potionSplashEvent(PotionSplashEvent event) {
		ThrownPotion potion = event.getPotion();
		ItemStack item = potion.getItem();
		ProjectileSource source = potion.getShooter();

		Collection<LivingEntity> affectedEntities = event.getAffectedEntities();
		List<Player> affectedPlayers = new ArrayList<>();

		// Never apply effects to villagers
		affectedEntities.removeIf(entity -> (entity instanceof Villager));

		// Don't apply effects to invulnerable entities
		affectedEntities.removeIf(Entity::isInvulnerable);

		boolean allowEffectsOnFriendlyMobs = ServerProperties.getShardName().contains("plot");

		/* If a potion has negative effects, don't apply them to any players except the thrower (if applicable) */
		if (source instanceof Player && (PotionUtils.hasNegativeEffects(potion.getItem()) || ItemStatUtils.hasNegativeEffect(potion.getItem(), true))) {
			affectedEntities.removeIf(entity -> ((entity instanceof Player && entity != source) || (allowEffectsOnFriendlyMobs && !EntityUtils.isHostileMob(entity))));
		}

		if (source instanceof Player && item.getItemMeta() instanceof PotionMeta potionMeta) {
			// Will never have negative effects at this point, just do basic clear of positive effects
			potionMeta.clearCustomEffects();
			potionMeta.setBasePotionType(PotionType.AWKWARD);
			item.setItemMeta(potionMeta);
			for (LivingEntity entity : affectedEntities) {
				if (entity instanceof Player player) {
					event.setIntensity(player, 0);
				}
			}
		}

		/* If a player threw this potion, trigger applicable abilities (potentially cancelling or modifying the event!) */
		if (source instanceof Player player) {
			mPlugin.mItemStatManager.onPlayerPotionSplashEvent(mPlugin, player, event);
			if (!mAbilities.playerSplashPotionEvent(player, affectedEntities, potion, event)) {
				event.setCancelled(true);
				return;
			}
		}

		/*
		 * If a player was hit by this potion, trigger applicable abilities (potentially cancelling or modifying the event!)
		 */
		for (LivingEntity entity : affectedEntities) {
			if (entity instanceof Player player) {
				affectedPlayers.add(player);
			}
		}

		for (Player player : affectedPlayers) {
			if (!mAbilities.playerSplashedByPotionEvent(player, affectedEntities, potion, event)) {
				event.setCancelled(true);
				return;
			}
		}

		// Run each custom effect on each afflicted entity
		for (LivingEntity entity : affectedEntities) {
			if (entity instanceof Player || (allowEffectsOnFriendlyMobs && !EntityUtils.isHostileMob(entity))) {
				double distance = Math.min(entity.getLocation().distance(event.getEntity().getLocation()), entity.getEyeLocation().distance(event.getEntity().getLocation()));
				distance = Math.min(Math.max(-0.1 * distance + 1, 0), 1);
				ItemStatUtils.applyCustomEffects(Plugin.getInstance(), entity, item, true, distance);
			}
		}

		// Track all player potion effects with the potion manager
		for (LivingEntity entity : affectedEntities) {
			if (entity instanceof Player player) {
				mPlugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, PotionUtils.getEffects(potion.getItem()),
					event.getIntensity(entity));
			}
		}

		for (Player p : affectedPlayers) {
			Collection<PotionEffect> appliedEffects = p.getActivePotionEffects();
			for (PotionEffect pe : appliedEffects) {
				if (pe.getType().equals(PotionEffectType.SLOW_FALLING) &&
					p.getGameMode().equals(GameMode.ADVENTURE)) {
					//Remove Slow Falling effects in Adventure mode areas (#947)
					p.sendMessage(Component.text("You cannot apply slow falling potion effects in adventure mode areas, other effects were still applied.", NamedTextColor.RED));
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> p.removePotionEffect(PotionEffectType.SLOW_FALLING), 1);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void lingeringPotionSplashEvent(LingeringPotionSplashEvent event) {
		MetadataUtils.setMetadata(event.getAreaEffectCloud(), AREA_EFFECT_CLOUD_POTION_METAKEY, event.getEntity().getItem());
		if (!event.getAreaEffectCloud().hasCustomEffects()) {
			// Area effect clouds without potion effects do not call AreaEffectCloudApplyEvent, so add a dummy effect
			event.getAreaEffectCloud().addCustomEffect(new PotionEffect(PotionEffectType.BAD_OMEN, 0, 0, true, false, false), false);
		}
	}

	// Entity ran into the effect cloud.
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void areaEffectCloudApplyEvent(AreaEffectCloudApplyEvent event) {
		AreaEffectCloud cloud = event.getEntity();
		Collection<LivingEntity> affectedEntities = event.getAffectedEntities();

		// Don't apply to players in stasis
		affectedEntities.removeIf(StasisListener::isInStasis);

		// Never apply effects to villagers
		affectedEntities.removeIf(entity -> (entity instanceof Villager));

		// Don't apply effects to invulnerable entities
		affectedEntities.removeIf(Entity::isInvulnerable);

		// Don't apply effects to dead entities
		affectedEntities.removeIf(entity -> (entity.isDead() || entity.getHealth() <= 0));

		// Don't apply slowness type lingering potions to players if a player dropped it
		affectedEntities.removeIf(entity -> (cloud.hasCustomEffect(PotionEffectType.SLOW) && entity instanceof Player && cloud.getSource() instanceof Player));

		// Don't apply invisibility type lingering potions to players if created by an (invisible) creeper exploding
		affectedEntities.removeIf(entity -> ((cloud.hasCustomEffect(PotionEffectType.INVISIBILITY) || cloud.hasCustomEffect(PotionEffectType.SLOW_FALLING)) && entity instanceof Player && cloud.getSource() instanceof Creeper));

		boolean allowEffectsOnFriendlyMobs = ServerProperties.getShardName().contains("plot");

		PotionType potionType = cloud.getBasePotionType();
		List<PotionInfo> infos = PotionUtils.getPotionInfoList(potionType, 4);
		List<PotionEffect> effects = cloud.hasCustomEffects() ? cloud.getCustomEffects() : null;

		// All affected players need to have the effect added to their potion manager.
		ItemStack potion = MetadataUtils.<ItemStack>getMetadata(cloud, AREA_EFFECT_CLOUD_POTION_METAKEY).orElse(null);
		for (LivingEntity entity : affectedEntities) {
			if (entity instanceof Player player) {
				for (PotionInfo info : infos) {
					mPlugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, info);
				}

				if (effects != null) {
					mPlugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, effects);
				}

				if (potion != null) {
					ItemStatUtils.applyCustomEffects(mPlugin, player, potion);
				}
			} else if (potion != null && allowEffectsOnFriendlyMobs && !EntityUtils.isHostileMob(entity) && !PotionUtils.hasNegativeEffects(potion) && !ItemStatUtils.hasNegativeEffect(potion, true)) {
				// only affect friendly mobs on plots, and only with beneficial potions
				ItemStatUtils.applyCustomEffects(mPlugin, entity, potion);
			}
		}

	}

	// Cancel explosions in adventure zones
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		// Cancel the event immediately if within a no-explosions zone
		if (ZoneUtils.hasZoneProperty(event.getLocation(), ZoneProperty.NO_EXPLOSIONS)) {
			event.setCancelled(true);
			return;
		}

		if (event.getEntity().getScoreboardTags().contains(AlchemicalAberrationBoss.identityTag)) {
			event.blockList().clear();
		}

		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Block block = iter.next();

			// If any block damaged by an explosion is with a no-explosions zone, cancel the explosion
			if (ZoneUtils.hasZoneProperty(block.getLocation(), ZoneProperty.NO_EXPLOSIONS)) {
				event.setCancelled(true);
				return;
			}

			// If this block is "unbreakable" than we want to remove it from the list.
			if (ServerProperties.getUnbreakableBlocks().contains(block.getType()) ||
				!mPlugin.mItemOverrides.blockExplodeInteraction(mPlugin, block)) {
				iter.remove();
			}
		}
	}

	// Cancel explosions in adventure zones
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockExplodeEvent(BlockExplodeEvent event) {
		// Cancel the event immediately if within a zone with no explosions
		if (ZoneUtils.hasZoneProperty(event.getBlock().getLocation(), ZoneProperty.NO_EXPLOSIONS)) {
			event.setCancelled(true);
			return;
		}

		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Block block = iter.next();

			// If any block damaged by an explosion is with an adventure zone, cancel the explosion
			if (ZoneUtils.hasZoneProperty(block.getLocation(), ZoneProperty.NO_EXPLOSIONS)) {
				event.setCancelled(true);
				return;
			}

			// If this block is "unbreakable" then we want to remove it from the list.
			if (ServerProperties.getUnbreakableBlocks().contains(block.getType()) ||
				!mPlugin.mItemOverrides.blockExplodeInteraction(mPlugin, block)) {
				iter.remove();
			}
		}
	}

	// Reset creeper explosions on stun
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void explosionPrimeEvent(ExplosionPrimeEvent event) {
		if (EntityUtils.isStunned(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	// Never generate new trades
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void villagerAcquireTradeEvent(VillagerAcquireTradeEvent event) {
		event.setCancelled(true);
	}

	// Never generate new trades
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void villagerCareerChangeEvent(VillagerCareerChangeEvent event) {
		event.setCancelled(true);
	}

	// Never generate new trades
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void villagerReplenishTradeEvent(VillagerReplenishTradeEvent event) {
		event.setCancelled(true);
	}

	// An Arrow hit something.
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void projectileHitEvent(ProjectileHitEvent event) {
		Entity entity = event.getHitEntity();
		Projectile proj = event.getEntity();
		ProjectileSource source = proj.getShooter();

		if (entity instanceof Player player) {
			mAbilities.playerHitByProjectileEvent(player, event);

			// Tipped Arrow shenanigans
			if (proj instanceof Arrow arrow) {

				if (player.isBlocking()) {
					Vector to = player.getLocation().toVector();
					Vector from = arrow.getLocation().toVector();

					if (to.subtract(from).dot(player.getLocation().getDirection()) < 0) {
						removePotionEffectsFromArrow(arrow);
					}
				}

				PotionType potionType = arrow.getBasePotionType();
				List<PotionInfo> infos = PotionUtils.getPotionInfoList(potionType, 8);
				List<PotionEffect> effects = arrow.hasCustomEffects() ? arrow.getCustomEffects() : null;

				for (PotionInfo info : infos) {
					mPlugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, info);
				}

				if (effects != null) {
					mPlugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, effects);
				}
			}
		}

		if (source instanceof Player) {
			mAbilities.projectileHitEvent((Player) source, event, proj);
			mPlugin.mItemStatManager.onProjectileHit(mPlugin, (Player) source, event, proj);
		}

		if (!event.isCancelled()) {
			mPlugin.mProjectileEffectTimers.removeEntity(proj);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void projectileHitEventLowest(ProjectileHitEvent event) {
		Entity entity = event.getHitEntity();
		Projectile proj = event.getEntity();
		ProjectileSource source = proj.getShooter();

		// Make projectiles shot by players pass through players and specified mobs
		if (source instanceof Player && entity != null && (entity instanceof Player || ScoreboardUtils.checkTag(entity, PROJECTILE_PASSTHROUGH_TAG)) && EntityUtils.isAbilityTriggeringProjectile(proj, false)) {
			event.setCancelled(true);
		}
	}

	public static void removePotionEffectsFromArrow(Arrow arrow) {
		arrow.setBasePotionType(PotionType.AWKWARD);
		arrow.clearCustomEffects();
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void entityTargetLivingEntityEvent(EntityTargetLivingEntityEvent event) {
		Entity entity = event.getEntity();
		LivingEntity target = event.getTarget();

		if (entity instanceof Creature && EntityUtils.isStunned(entity)) {
			event.setCancelled(true);
			return;
		}

		if (target != null) {
			if (target instanceof Player player) {
				if (AbilityUtils.isStealthed(player)) {
					event.setTarget(null);
					event.setCancelled(true);
				} else {
					mAbilities.entityTargetLivingEntityEvent(player, event);
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void potionEffectApplyEvent(PotionEffectApplyEvent event) {
		LivingEntity applied = event.getApplied();

		LivingEntity applier;
		if (event.getApplier() instanceof Projectile proj) {
			ProjectileSource shooter = proj.getShooter();
			if (shooter instanceof LivingEntity) {
				applier = (LivingEntity) shooter;
			} else {
				return;
			}
		} else if (event.getApplier() instanceof LivingEntity) {
			applier = (LivingEntity) event.getApplier();
		} else {
			return;
		}

		PotionEffectType type = event.getEffect().getType();
		/* Mark as applying slowness so arcane strike won't activate this tick */
		if (applier instanceof Player && !applied.hasPotionEffect(PotionEffectType.SLOW)
			&& type.equals(PotionEffectType.SLOW)) {
			MetadataUtils.checkOnceThisTick(mPlugin, applied, Constants.ENTITY_SLOWED_NONCE_METAKEY);
		}

		if (applier instanceof Player player) {
			mAbilities.potionEffectApplyEvent(player, event);
		}

		if (applier instanceof Dolphin && type.equals(PotionEffectType.DOLPHINS_GRACE) && applier.getScoreboardTags().contains("boss_no_dolphins_grace")) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void entityChangeBlockEvent(EntityChangeBlockEvent event) {
		event.setCancelled(!mPlugin.mItemOverrides.blockChangeInteraction(mPlugin, event.getBlock()));
		if (event.getEntity() instanceof Wither) {
			event.setCancelled(true);
			return;
		}

		// When sheep eat grass outside of plots, do not change to dirt
		if (event.getEntity() instanceof Sheep && !ZoneUtils.isInPlot(event.getBlock().getLocation())) {
			event.setCancelled(true);
			return;
		}

		// When a boat runs into a lilypad in an Adventure Mode zone, do not break the lilypad
		if (event.getEntity() instanceof Boat && !ZoneUtils.isMineable(event.getBlock().getLocation())) {
			event.setCancelled(true);
			return;
		}

		// Cancel Falling Blocks with SpellGrenade tag
		if (event.getEntity() instanceof FallingBlock fallingBlock && fallingBlock.getScoreboardTags().contains("DisableBlockPlacement")) {
			event.setCancelled(true);
			return;
		}

		// Prevent falling blocks forming into real blocks inside adventure mode areas if they come from outside
		// Note that this event is also called when the block starts to fall, thus a missing metadata on the block is assumed to be that start event and thus allowed
		if (ZoneUtils.hasZoneProperty(event.getBlock().getLocation(), ZoneProperty.ADVENTURE_MODE)
			    && event.getEntity() instanceof FallingBlock fallingBlock
			    && (!MetadataUtils.getMetadata(fallingBlock, FALLING_BLOCK_ADVENTURE_MODE_METADATA_KEY, true)
				    || fallingBlock.getScoreboardTags().contains(FALLING_BLOCK_NO_PLACE_ADVENTURE_MODE_TAG))) {
			if (fallingBlock.getDropItem()) {
				Material material = fallingBlock.getBlockData().getMaterial();
				if (!material.isAir() && material != Material.FROSTED_ICE) { // this can apparently happen, and frosted ice somehow gets turned into air too?
					try {
						fallingBlock.getWorld().dropItemNaturally(fallingBlock.getLocation(), new ItemStack(material));
					} catch (IllegalArgumentException e) {
						// The error with dropping air still exists - log it to see what the issue is
						MMLog.warning("IllegalArgumentException on dropping an item of type " + material, e);
					}
				}
			}
			fallingBlock.remove();
			event.setCancelled(true);
		}

	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityEnterBlockEvent(EntityEnterBlockEvent event) {
		//for the moment this event is used only for bees but for future proofing check for only bees
		if (event.getEntity() instanceof Bee bee && bee.getScoreboardTags().contains(BEES_BLOCK_HIVE_ENTER_EVENT)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		if (!mAbilities.blockBreakEvent(event.getPlayer(), event)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void entityDismountEvent(EntityDismountEvent event) {
		if (event.getDismounted() instanceof ArmorStand) {
			event.getDismounted().remove();
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void entityMountEvent(EntityMountEvent event) {
		if (event.getEntity().getScoreboardTags().contains(EntityUtils.DONT_ENTER_BOATS_TAG)
			    && event.getMount() instanceof Boat) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityAddToWorldEvent(EntityAddToWorldEvent event) {
		if (event.getEntity() instanceof Mob mob) {
			NmsUtils.getVersionAdapter().mobAIChanges(mob);
		}

		if (event.getEntity() instanceof IronGolem golem) {
			NmsUtils.getVersionAdapter().setAttackRange(golem, 2.5);
		} else if (event.getEntity() instanceof Ravager ravager) {
			NmsUtils.getVersionAdapter().setAttackRange(ravager, 2.5);
		} else if (event.getEntity() instanceof Vindicator vindicator) {
			// Remove the bonus range Vindicators get when riding Ravagers
			NmsUtils.getVersionAdapter().setAttackRange(vindicator, 1.43);
		} else if (event.getEntity() instanceof Zoglin zoglin) {
			NmsUtils.getVersionAdapter().setAttackRange(zoglin, 1.8);
		} else if (event.getEntity() instanceof Hoglin hoglin) {
			NmsUtils.getVersionAdapter().setAttackRange(hoglin, 1.8);
		} else if (event.getEntity() instanceof FallingBlock fallingBlock) {
			fallingBlock.setMetadata(FALLING_BLOCK_ADVENTURE_MODE_METADATA_KEY, new FixedMetadataValue(mPlugin, ZoneUtils.hasZoneProperty(fallingBlock.getLocation(), ZoneProperty.ADVENTURE_MODE)));
		} else if (event.getEntity() instanceof Bee bee) {
			// stop bees from running super fast when they get near the ground
			EntityUtils.setAttributeBase(bee, Attribute.GENERIC_MOVEMENT_SPEED, 0.1);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void hangingBreakEvent(HangingBreakEvent event) {
		if (event.getEntity() instanceof ItemFrame frame
				&& INVISIBLE_ITEM_FRAME_NAME.equals(frame.getName())
				&& !(event instanceof HangingBreakByEntityEvent breakByEntityEvent && breakByEntityEvent.getRemover() instanceof Player player && player.getGameMode() == GameMode.CREATIVE)) {
			event.setCancelled(true);
			Location centeredLocation = frame.getLocation().toCenterLocation();
			frame.getWorld().dropItem(centeredLocation, InventoryUtils.getItemFromLootTable(frame, INVISIBLE_ITEM_FRAME_LOOT_TABLE));
			if (!ItemUtils.isNullOrAir(frame.getItem())
					&& frame.getItemDropChance() >= FastUtils.RANDOM.nextFloat()) {
				frame.getWorld().dropItem(centeredLocation, frame.getItem());
			}
			frame.remove();
		}
	}
}
