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
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LingeringPotion;
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
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityDismountEvent;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.cleric.Celestial;
import com.playmonumenta.plugins.abilities.cleric.hierophant.EnchantedPrayer;
import com.playmonumenta.plugins.enchantments.AttributeBowDamage;
import com.playmonumenta.plugins.enchantments.Duelist;
import com.playmonumenta.plugins.enchantments.Frost;
import com.playmonumenta.plugins.enchantments.Impact;
import com.playmonumenta.plugins.enchantments.Inferno;
import com.playmonumenta.plugins.enchantments.PointBlank;
import com.playmonumenta.plugins.enchantments.Slayer;
import com.playmonumenta.plugins.enchantments.Sniper;
import com.playmonumenta.plugins.enchantments.ThrowingKnife;
import com.playmonumenta.plugins.enchantments.infusions.Focus;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.events.PotionEffectApplyEvent;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.tracking.PlayerTracking;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils.BossAbilityDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.GraveUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

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

	@EventHandler(priority = EventPriority.LOW)
	public void entityCombustByEntityEvent(EntityCombustByEntityEvent event) {
		if (event.isCancelled()) {
			return;
		}

		// Record the time of the player who sets a mob on fire
		// Used to prevent arcane strike from counting mobs on fire that were
		// set on fire by the same hit that triggered arcane strike
		// Only mark mobs that were not already burning
		Entity combustee = event.getEntity();
		Entity combuster = event.getCombuster();

		if ((combuster instanceof Player) && (combustee.getFireTicks() <= 0)) {
			MetadataUtils.checkOnceThisTick(mPlugin, combustee,
			                                Constants.ENTITY_COMBUST_NONCE_METAKEY);
		}

		if ((combuster instanceof Player) && (combustee.getFireTicks() > 0)) {
			Player player = (Player) combuster;
			if (player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.FIRE_ASPECT)
				&& combustee instanceof LivingEntity && Inferno.mobHasInferno(mPlugin, (LivingEntity) combustee)
				&& PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, Inferno.class) < Inferno.sTaggedMobs.get(combustee).mLevel) {
				event.setCancelled(true);
				return;
			}
		}

		if ((combustee instanceof Player)) {
			Player player = (Player)combustee;

			if (!mAbilities.playerCombustByEntityEvent(player, event)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void customDamageEvent(CustomDamageEvent event) {
		if (event.getDamager() instanceof Player) {
			mAbilities.playerDealtCustomDamageEvent((Player)event.getDamager(), event);
			// If the event has a valid spell, call onAbility
			if (event.getSpell() != null) {
				mPlugin.mTrackingManager.mPlayers.onAbility(mPlugin, (Player)event.getDamager(), event.getDamaged(), event);
			}
		}
	}

	//  An Entity hit another Entity.
	@EventHandler(priority = EventPriority.HIGH)
	public void entityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		if (event.isCancelled()) {
			return;
		}
		Entity damagee = event.getEntity();
		Entity damager = event.getDamager();

		//  If the entity getting hurt is the player.
		if (damagee instanceof Player) {
			Player player = (Player)damagee;

			mPlugin.mTrackingManager.mPlayers.onHurtByEntity(mPlugin, player, event);

			if (damager instanceof LivingEntity) {
				if (!mAbilities.playerDamagedByLivingEntityEvent(player, event)) {
					event.setCancelled(true);
				}
				MetadataUtils.checkOnceThisTick(mPlugin, damagee, Constants.PLAYER_DAMAGE_NONCE_METAKEY);
			} else if (damager instanceof Firework) {
				//  If we're hit by a rocket, cancel the damage.
				event.setCancelled(true);
			} else if (damager instanceof Projectile) {
				// Drowneds throwing tridents at players should deal the damage of the trident
				if (damager instanceof Trident) {
					ProjectileSource source = ((Projectile) damager).getShooter();
					if (source instanceof Drowned) {
						ItemMeta meta = ((Drowned)source).getEquipment().getItemInMainHand().getItemMeta();
						if (meta != null && meta.hasAttributeModifiers()) {
							Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(Attribute.GENERIC_ATTACK_DAMAGE);
							if (modifiers != null) {
								Iterator<AttributeModifier> iter = modifiers.iterator();
								while (iter.hasNext()) {
									AttributeModifier mod = iter.next();
									if (mod.getOperation().equals(AttributeModifier.Operation.ADD_NUMBER)) {
										// Use the last flat damage modifier on the trident, ignore other modifiers
										// +1 for base damage to be consistent with melee attack damage
										event.setDamage(mod.getAmount() + 1);
									}
								}
							}
						}
					}
				}

				if (!mAbilities.playerDamagedByProjectileEvent(player, event)) {
					damager.remove();
					event.setCancelled(true);
				}
			}
			if (event.getCause() == DamageCause.THORNS && MetadataUtils.happenedThisTick(mPlugin, damagee, "LastMagicUseTime", 0)) {
				// Check that thorns isn't being caused by 'magic'; if it is, cancel the damage
				event.setCancelled(true);
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
			// Don't allow evoker fangs to damage non-players
			if (damager instanceof EvokerFangs) {
				event.setCancelled(true);
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

			// Plot Security: If damagee is inside a plot but the player is in adventure, cancel.
			if (player.getGameMode() == GameMode.ADVENTURE
				&& ZoneUtils.inPlot(damagee, ServerProperties.getIsTownWorld())) {
				event.setCancelled(true);
				return;
			}

			// Make sure to not trigger class abilities off Thorns
			if (event.getCause() != DamageCause.THORNS) {
				// Class damage-based abilities only apply to living entities that are not villagers
				if (damagee instanceof LivingEntity && !(damagee instanceof Villager)) {
					// Apply any damage modifications that items they have may apply.
					mPlugin.mTrackingManager.mPlayers.onDamage(mPlugin, player, (LivingEntity)damagee, event);
					if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
						mPlugin.mTrackingManager.mPlayers.onAttack(mPlugin, player, (LivingEntity)damagee, event);
						// Also do Celestial Blessing since it is +%
						Celestial.modifyDamage(player, event);
					}

					if (!mAbilities.livingEntityDamagedByPlayerEvent(player, event)) {
						event.setCancelled(true);
					}

					if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
						EnchantedPrayer.onEntityAttack(mPlugin, player, (LivingEntity)damagee);
					}
				}
			}
		} else if (damager instanceof Arrow) {
			Arrow arrow = (Arrow)damager;
			if (arrow.getShooter() instanceof Player) {
				Player player = (Player)arrow.getShooter();

				// Plot Security: If damagee is inside a plot but the player is in adventure, cancel.
				if (player.getGameMode() == GameMode.ADVENTURE
					&& ZoneUtils.inPlot(damagee, ServerProperties.getIsTownWorld())) {
					damager.remove();
					event.setCancelled(true);
					return;
				}

				if (damagee instanceof LivingEntity && !(damagee instanceof Villager)) {
					if (damagee instanceof Player && !AbilityManager.getManager().isPvPEnabled((Player) damagee)) {
						damager.remove();
						event.setCancelled(true);
						/*
						 * If we don't return, then the side effects of LivingEntityShotByPlayerEvent() will
						 * still occur (e.g. wither) despite the damage event being canceled.
						 */
						return;
					}

					// If arrow is a throwing knife or trident, skip added damage calculations.
					if (ThrowingKnife.isThrowingKnife(arrow) || arrow.getType() == EntityType.TRIDENT) {
						return;
					}

					// Set the arrow damage from attributes
					AttributeBowDamage.onShootAttack(mPlugin, (Projectile) damager, (LivingEntity) damagee, event);

					mPlugin.mTrackingManager.mPlayers.onDamage(mPlugin, player, (LivingEntity)damagee, event);
					if (!mAbilities.livingEntityShotByPlayerEvent(player, arrow, (LivingEntity)damagee, event)) {
						damager.remove();
						event.setCancelled(true);
					}

					/*
					 * This handles bow damage for abilities
					 * Damage scaling abilities are applied to base arrow damage only (Volley, Pinning Shot)
					 * Flat damage bonus abilities and enchantments are applied at the end (Bow Mastery, Sharpshooter)
					 */

					if (damagee instanceof LivingEntity) {
						LivingEntity mob = (LivingEntity) damagee;

						if (arrow.hasMetadata("ArrowQuickdraw")) {
							event.setDamage(AbilityUtils.getArrowBaseDamage(arrow));
						}

						double bonusDamage = AbilityUtils.getArrowVelocityDamageMultiplier(arrow) * AbilityUtils.getArrowBonusDamage(arrow);
						double multiplier = AbilityUtils.getArrowFinalDamageMultiplier(arrow);
						if (mob.hasMetadata("PinningShotEnemyHasBeenPinned")
							&& mob.getMetadata("PinningShotEnemyHasBeenPinned").get(0).asInt() != player.getTicksLived()
							&& mob.hasMetadata("PinningShotEnemyIsPinned")) {
							multiplier *= mob.getMetadata("PinningShotEnemyIsPinned").get(0).asDouble();
							mob.removeMetadata("PinningShotEnemyIsPinned", mPlugin);
							mWorld.playSound(mob.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.5f);
							mWorld.spawnParticle(Particle.FIREWORKS_SPARK, arrow.getLocation(), 20, 0, 0, 0, 0.2);
							mWorld.spawnParticle(Particle.SNOWBALL, arrow.getLocation(), 30, 0, 0, 0, 0.25);
							mob.removePotionEffect(PotionEffectType.SLOW);
						}

						event.setDamage(event.getDamage() * multiplier + bonusDamage);
					}
				}

			}
		}

		if (damager instanceof Projectile && damagee instanceof LivingEntity) {
			Sniper.onShootAttack(mPlugin, (Projectile)damager, (LivingEntity)damagee, event);
			PointBlank.onShootAttack(mPlugin, (Projectile)damager, (LivingEntity)damagee, event);
			Frost.onShootAttack(mPlugin, (Projectile)damager, (LivingEntity)damagee, event);
			Inferno.onShootAttack(mPlugin, (Projectile)damager, (LivingEntity)damagee, event);
			Focus.onShootAttack(mPlugin, (Projectile)damager, (LivingEntity)damagee, event);
		}

		if (damager instanceof Trident && damagee instanceof LivingEntity) {
			Impact.onShootAttack(mPlugin, (Projectile)damager, (LivingEntity)damagee, event);
			Slayer.onShootAttack(mPlugin, (Projectile)damager, (LivingEntity)damagee, event);
			Duelist.onShootAttack(mPlugin, (Projectile)damager, (LivingEntity)damagee, event);
			Focus.onShootAttack(mPlugin, (Projectile)damager, (LivingEntity)damagee, event);
		}

		if (damagee instanceof LivingEntity) {
			LivingEntity mob = (LivingEntity) damagee;
			event.setDamage(event.getDamage() * EntityUtils.vulnerabilityMult(mob));
		}
	}

	// Entity Hurt Event.
	@EventHandler(priority = EventPriority.LOW)
	public void entityDamageEvent(EntityDamageEvent event) {
		Entity damagee = event.getEntity();
		DamageCause source = event.getCause();

		if (event.isCancelled()) {
			return;
		}
		if ((source == DamageCause.BLOCK_EXPLOSION || source == DamageCause.ENTITY_EXPLOSION) &&
		    (damagee.getScoreboardTags().contains("ExplosionImmune") || damagee instanceof ItemFrame)) {
			event.setCancelled(true);
			return;
		}
		if (damagee instanceof Player) {
			Player player = (Player)damagee;
			World world = player.getWorld();
			mPlugin.mTrackingManager.mPlayers.onHurt(mPlugin, player, event);

			if (ZoneUtils.hasZoneProperty(player.getLocation(), ZoneProperty.RESIST_5)) {
				if (DAMAGE_CAUSES_IGNORED_IN_TOWNS.contains(source)) {
					event.setCancelled(true);
					return;
				}
			}

			if (!mAbilities.playerDamagedEvent(player, event)) {
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
		} else if (damagee instanceof Item) {
			if (!damagee.getScoreboardTags().contains("ItemDamageTriggered")) {
				damagee.addScoreboardTag("ItemDamageTriggered");
				GraveUtils.destroyItemEntity((Item) damagee);
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
	public void entityInteractEvent(EntityInteractEvent event) {
		Material material = event.getBlock().getType();

		if (ENTITY_UNINTERACTABLE_MATS.contains(material)) {
			Entity entity = event.getEntity();

			// Only items and players can activate tripwires
			// Also pigs, for the pig quest
			if (entity instanceof Item || entity instanceof Player || entity instanceof Pig ||
				(entity.getScoreboardTags() != null && entity.getScoreboardTags().contains("block_interact"))) {
				return;
			}

			event.setCancelled(true);
		}
	}

	// Hanging Entity hurt by another entity.
	@EventHandler(priority = EventPriority.LOW)
	public void hangingBreakByEntityEvent(HangingBreakByEntityEvent event) {
		if (event.isCancelled()) {
			return;
		}

		Entity damager = event.getRemover();

		if (damager instanceof Player) {
			// If hurt by a player in adventure mode we want to prevent the break;
			Player player = (Player)damager;

			if (player.getGameMode() == GameMode.ADVENTURE) {
				event.setCancelled(true);
			}
		} else if (damager instanceof Arrow || damager instanceof TippedArrow) {
			// If hurt by an arrow from a player in adventure mode.
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
	public void entityResurrectEvent(EntityResurrectEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof Player) {
			ItemStack mainhand = ((Player) entity).getInventory().getItemInMainHand();
			ItemStack offhand = ((Player) entity).getInventory().getItemInOffHand();
			//If one hand has a shattered totem, do not resurrect
			if (mainhand.getType() == Material.TOTEM_OF_UNDYING && ItemUtils.isItemShattered(mainhand) ||
				offhand.getType() == Material.TOTEM_OF_UNDYING && ItemUtils.isItemShattered(offhand)) {
				event.setCancelled(true);
			}
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
	public void entitySpawnEvent(EntitySpawnEvent event) {
		Entity entity = event.getEntity();
		mPlugin.mTrackingManager.addEntity(entity);
	}

	// Player shoots an arrow.
	@EventHandler(priority = EventPriority.HIGH)
	public void projectileLaunchEvent(ProjectileLaunchEvent event) {
		if (event.isCancelled()) {
			return;
		}

		Projectile proj = event.getEntity();
		ProjectileSource shooter = proj.getShooter();

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
			} else if (event.getEntityType() == EntityType.ENDER_PEARL) {
				EnderPearl origPearl = (EnderPearl)proj;
				ItemStack itemInMainHand = player.getEquipment().getItemInMainHand();
				ItemStack itemInOffHand = player.getEquipment().getItemInOffHand();

				// Check if the player has an infinity ender pearl in main or off hand
				if (((itemInMainHand.getType().equals(Material.ENDER_PEARL)) &&
				     (itemInMainHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE) > 0)) ||
				    ((itemInOffHand.getType().equals(Material.ENDER_PEARL)) &&
				     (itemInOffHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE) > 0))) {

					EnderPearl newPearl = (EnderPearl)mWorld.spawnEntity(origPearl.getLocation(), EntityType.ENDER_PEARL);
					newPearl.setShooter(player);
					newPearl.setVelocity(origPearl.getVelocity());
					event.setCancelled(true);
					return;
				}
			} else if (event.getEntityType() == EntityType.ARROW || event.getEntityType() == EntityType.TIPPED_ARROW || event.getEntityType() == EntityType.SPECTRAL_ARROW) {
				Arrow arrow = (Arrow)proj;
				if (!mAbilities.playerShotArrowEvent(player, arrow)) {
					event.setCancelled(true);
				}

				MetadataUtils.checkOnceThisTick(mPlugin, player, Constants.PLAYER_BOW_SHOT_METAKEY);

				// Stores velocity for ability damage calculations
				AbilityUtils.setArrowVelocityDamageMultiplier(mPlugin, arrow);
			} else if (event.getEntityType() == EntityType.SPLASH_POTION) {
				SplashPotion potion = (SplashPotion)proj;
				if (!mAbilities.playerThrewSplashPotionEvent(player, potion)) {
					event.setCancelled(true);
				}
			} else if (event.getEntityType() == EntityType.LINGERING_POTION) {
				LingeringPotion potion = (LingeringPotion)proj;
				if (!mAbilities.playerThrewLingeringPotionEvent(player, potion)) {
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void potionSplashEvent(PotionSplashEvent event) {
		if (event.isCancelled()) {
			return;
		}

		ThrownPotion potion = event.getPotion();
		ProjectileSource source = potion.getShooter();
		Collection<LivingEntity> affectedEntities = event.getAffectedEntities();

		// Never apply effects to villagers
		affectedEntities.removeIf(entity -> (entity instanceof Villager));

		// Don't apply effects to invulnerable entities
		affectedEntities.removeIf(entity -> (entity.isInvulnerable()));

		/* If a potion has negative effects, don't apply them to any players except the thrower (if applicable) */
		if (source instanceof Player && PotionUtils.hasNegativeEffects(potion.getItem())) {
			affectedEntities.removeIf(entity -> (entity instanceof Player && entity != source));
		}

		/* If a player threw this potion, trigger applicable abilities (potentially cancelling or modifying the event!) */
		if (source instanceof Player) {
			if (!mAbilities.playerSplashPotionEvent((Player)source, affectedEntities, potion, event)) {
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
				if (!mAbilities.playerSplashedByPotionEvent((Player)entity, affectedEntities, potion, event)) {
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
	public void areaEffectCloudApplyEvent(AreaEffectCloudApplyEvent event) {
		if (event.isCancelled()) {
			return;
		}

		AreaEffectCloud cloud = event.getEntity();
		Collection<LivingEntity> affectedEntities = event.getAffectedEntities();

		// Never apply effects to villagers
		affectedEntities.removeIf(entity -> (entity instanceof Villager));

		// Don't apply effects to invulnerable entities
		affectedEntities.removeIf(entity -> (entity.isInvulnerable()));

		// Don't apply effects to dead entities
		affectedEntities.removeIf(entity -> (entity.isDead() || entity.getHealth() <= 0));

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

	// Cancel explosions in adventure zones
	@EventHandler(priority = EventPriority.LOWEST)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		// Cancel the event immediately if within a adventure zone
		if (ZoneUtils.hasZoneProperty(event.getLocation(), ZoneProperty.ADVENTURE_MODE)) {
			event.setCancelled(true);
			return;
		}

		// Cancel the event if from a confused creeper, damage still applies it seems
		Entity entity = event.getEntity();
		if (entity instanceof Creeper && EntityUtils.isConfused(event.getEntity())) {
			event.setCancelled(true);
			return;
		}

		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Block block = iter.next();

			// If any block damaged by an explosion is with a adventure zone, cancel the explosion
			if (ZoneUtils.hasZoneProperty(block.getLocation(), ZoneProperty.ADVENTURE_MODE)) {
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
	@EventHandler(priority = EventPriority.LOWEST)
	public void blockExplodeEvent(BlockExplodeEvent event) {
		// Cancel the event immediately if within a zone with no explosions
		if (ZoneUtils.hasZoneProperty(event.getBlock().getLocation(), ZoneProperty.ADVENTURE_MODE)) {
			event.setCancelled(true);
			return;
		}

		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Block block = iter.next();

			// If any block damaged by an explosion is with a adventure zone, cancel the explosion
			if (ZoneUtils.hasZoneProperty(block.getLocation(), ZoneProperty.ADVENTURE_MODE)) {
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

	// Never generate new trades
	@EventHandler(priority = EventPriority.LOWEST)
	public void villagerAcquireTradeEvent(VillagerAcquireTradeEvent event) {
		event.setCancelled(true);
	}

	//  An Arrow hit something.
	@EventHandler(priority = EventPriority.HIGH)
	public void projectileHitEvent(ProjectileHitEvent event) {
		EntityType type = event.getEntityType();

		Entity entity = event.getHitEntity();
		if (entity != null && entity instanceof Player) {
			Player player = (Player)entity;
			mAbilities.playerHitByProjectileEvent(player, event);
			if (type == EntityType.TIPPED_ARROW) {
				TippedArrow arrow = (TippedArrow)event.getEntity();

				if (player.isBlocking()) {
					Vector to = player.getLocation().toVector();
					Vector from = arrow.getLocation().toVector();

					if (to.subtract(from).dot(player.getLocation().getDirection()) < 0) {
						removePotionDataFromArrow(arrow);
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

		if (entity != null && entity instanceof LivingEntity) {
			LivingEntity hitEntity = (LivingEntity) entity;
			if (hitEntity.getFireTicks() > 0) {
				// Save old fireticks, the fire ticks will be managed in Inferno.onShootAttack(), which triggers for every projectile attack.
				hitEntity.setMetadata(Inferno.OLD_FIRE_TICKS_METAKEY, new FixedMetadataValue(mPlugin, hitEntity.getFireTicks()));
				hitEntity.setFireTicks(1);
			}
		}

		if (type == EntityType.ARROW || type == EntityType.TIPPED_ARROW || type == EntityType.SPECTRAL_ARROW) {
			Arrow arrow = (Arrow)event.getEntity();
			ProjectileSource source = arrow.getShooter();
			if (source instanceof Player) {
				Player player = (Player)source;
				mAbilities.projectileHitEvent(player, event, arrow);
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

	private void removePotionDataFromArrow(TippedArrow arrow) {
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

	@EventHandler
	public void entityTargetLivingEntityEvent(EntityTargetLivingEntityEvent event) {
		if (event.getEntity() instanceof Creature && (EntityUtils.isStunned(event.getEntity())
		                                              || EntityUtils.isConfused(event.getEntity()))) {
			event.setCancelled(true);
			return;
		}
		if (event.getTarget() instanceof Player) {
			Player player = (Player) event.getTarget();
			mAbilities.entityTargetLivingEntityEvent(player, event);
		}
	}

	@EventHandler
	public void potionEffectApplyEvent(PotionEffectApplyEvent event) {
		LivingEntity applied = event.getApplied();

		LivingEntity applier;
		if (event.getApplier() instanceof Projectile) {
			Projectile proj = (Projectile)event.getApplier();
			if (proj.getShooter() != null && (proj.getShooter() instanceof LivingEntity)) {
				applier = (LivingEntity) proj.getShooter();
			} else {
				return;
			}
		} else if (event.getApplier() instanceof LivingEntity) {
			applier = (LivingEntity) event.getApplier();
		} else {
			return;
		}

		/* Mark as applying slowness so arcane strike won't activate this tick */
		if (applier instanceof Player && !applied.hasPotionEffect(PotionEffectType.SLOW)
		    && event.getEffect().getType() == PotionEffectType.SLOW) {
			MetadataUtils.checkOnceThisTick(mPlugin, applied, Constants.ENTITY_SLOWED_NONCE_METAKEY);
		}

		if (applier instanceof Player) {
			Player player = (Player) applier;
			mAbilities.potionEffectApplyEvent(player, event);
		}
	}

	@EventHandler
	public void entityChangeBlockEvent(EntityChangeBlockEvent event) {
		event.setCancelled(!mPlugin.mItemOverrides.blockChangeInteraction(mPlugin, event.getBlock()));
		if (event.getEntity() instanceof Wither) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void entityDismountEvent(EntityDismountEvent event) {
		if (event.getDismounted() instanceof ArmorStand) {
			event.getDismounted().remove();
		}
	}

	@EventHandler
	public void bossAbilityDamageEvent(BossAbilityDamageEvent event) {
		if (event.getDamaged() instanceof Player) {
			Player player = event.getDamaged();
			mPlugin.mTrackingManager.mPlayers.onBossDamage(mPlugin, player, event);
			mAbilities.bossAbilityDamageEvent(player, event);
		}
	}

	// Fires whenever an item entity despawns due to time. Does not catch items that got killed in other ways.
	@EventHandler(priority = EventPriority.HIGH)
	public void itemDespawnEvent(ItemDespawnEvent event) {
		Item entity = event.getEntity();
		GraveUtils.destroyItemEntity(entity);
	}

	// Fires any time any entity is deleted.
	@EventHandler(priority = EventPriority.HIGH)
	public void entityRemoveFromWorldEvent(EntityRemoveFromWorldEvent event) {
		if (event.getEntity() instanceof Item) {
			// Check if an item entity was destroyed by the void.
			Item entity = (Item) event.getEntity();
			if (entity.getLocation().getY() <= -64) {
				GraveUtils.destroyItemEntity(entity);
			}
		}
	}

}
