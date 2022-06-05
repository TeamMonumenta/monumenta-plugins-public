package com.playmonumenta.plugins.listeners;

import com.destroystokyo.paper.event.entity.EntityZapEvent;
import com.destroystokyo.paper.event.entity.PreSpawnerSpawnEvent;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

public class MobListener implements Listener {

	static final int SPAWNER_DROP_THRESHOLD = 20;
	static final int ALCH_PASSIVE_RADIUS = 12;
	private static final NamespacedKey ARMED_ARMOR_STAND_LOOT_TABLE = NamespacedKeyUtils.fromString("epic:items/armed_armor_stand");

	private final Plugin mPlugin;

	public MobListener(Plugin plugin) {
		mPlugin = plugin;
	}

	/**
	 * This method handles spawner spawn rules. We use a Paper patch that disables all vanilla span rules for spawners,
	 * so all types of mobs can spawn anywhere if there's enough space for the mob.
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	void preSpawnerSpawnEvent(PreSpawnerSpawnEvent event) {

		EntityType type = event.getType();
		boolean inWater = LocationUtils.isLocationInWater(event.getSpawnLocation());

		// water mobs: must spawn in water
		if (EntityUtils.isWaterMob(type)) {
			if (!inWater) {
				event.setCancelled(true);
			}
			return;
		}

		// land & air mobs: must not spawn in water
		if (inWater) {
			event.setCancelled(true);
			return;
		}

		// land mobs: must not spawn in the air (i.e. must have a block with collision below)
		// creepers don't follow this rule so that they can be used in drop creeper traps
		if (!EntityUtils.isFlyingMob(type) && type != EntityType.CREEPER) {
			if (!event.getSpawnLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
				event.setCancelled(true);
			}
			return;
		}

		// flying mobs: can spawn anywhere (except water), so no more checks

	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	void creatureSpawnEvent(CreatureSpawnEvent event) {
		LivingEntity entity = event.getEntity();
		CreatureSpawnEvent.SpawnReason spawnReason = event.getSpawnReason();

		if (spawnReason == CreatureSpawnEvent.SpawnReason.BUILD_WITHER ||
			    spawnReason == CreatureSpawnEvent.SpawnReason.CURED ||
			    spawnReason == CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE) {
			event.setCancelled(true);
			return;
		}

		// No natural bat or slime spawning
		if (
			(entity instanceof Bat || entity instanceof Slime)
				&& spawnReason.equals(CreatureSpawnEvent.SpawnReason.NATURAL)
		) {
			event.setCancelled(true);
			return;
		}

		// We need to allow spawning hostile mobs intentionally, but disable natural spawns.
		// It's easier to check the intentional ways than the natural ones.
		if (spawnReason != CreatureSpawnEvent.SpawnReason.CUSTOM &&
			    spawnReason != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG &&
			    spawnReason != CreatureSpawnEvent.SpawnReason.DISPENSE_EGG &&
			    spawnReason != CreatureSpawnEvent.SpawnReason.SPAWNER &&
			    spawnReason != CreatureSpawnEvent.SpawnReason.DEFAULT &&
			    spawnReason != CreatureSpawnEvent.SpawnReason.COMMAND &&
			    EntityUtils.isHostileMob(entity) &&
			    ZoneUtils.hasZoneProperty(entity, ZoneProperty.NO_NATURAL_SPAWNS)) {
			event.setCancelled(true);
			return;
		}

		if (!(entity instanceof Player) && !(entity instanceof ArmorStand)) {

			// Mark mobs not able to pick-up items.
			entity.setCanPickupItems(false);

			// Overwrite drop chances for mob armor and held items
			EntityEquipment equipment = entity.getEquipment();
			if (equipment != null) {
				equipment.setHelmetDropChance(ItemUtils.getItemDropChance(equipment.getHelmet()));
				equipment.setChestplateDropChance(ItemUtils.getItemDropChance(equipment.getChestplate()));
				equipment.setLeggingsDropChance(ItemUtils.getItemDropChance(equipment.getLeggings()));
				equipment.setBootsDropChance(ItemUtils.getItemDropChance(equipment.getBoots()));
				equipment.setItemInMainHandDropChance(ItemUtils.getItemDropChance(equipment.getItemInMainHand()));
				equipment.setItemInOffHandDropChance(ItemUtils.getItemDropChance(equipment.getItemInOffHand()));
			}

			mPlugin.mZoneManager.applySpawnEffect(mPlugin, entity);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	void spawnerSpawnEvent(SpawnerSpawnEvent event) {
		CreatureSpawner spawner = event.getSpawner();

		/* This can apparently happen sometimes...? */
		if (spawner == null) {
			return;
		}

		Entity mob = event.getEntity();
		int spawnCount = 1;

		if (spawner.hasMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
			// There should only be one value - just use the latest one
			for (MetadataValue value : spawner.getMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
				// Previous value found - add one to it for the currently-spawning mob
				spawnCount = value.asInt() + 1;
			}
		}

		// Create new metadata entries
		spawner.setMetadata(Constants.SPAWNER_COUNT_METAKEY, new FixedMetadataValue(mPlugin, spawnCount));
		mob.setMetadata(Constants.SPAWNER_COUNT_METAKEY, new FixedMetadataValue(mPlugin, spawnCount));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityDamageByEntityEvent(EntityDamageByEntityEvent event) {

		//TODO look for this "bug" (?) https://discord.com/channels/186225508562763776/186266724440604673/913044974768103484
		//When a zombie damage another zombie with BossUtils.bossDamage(..) or bossDamagePercent(..)
		//it also hit himselft, causing to setTarget(himselft) and make so it strikes itself to death
		if (event.getDamager() == event.getEntity() && event.getDamager() instanceof Zombie) {
			event.setCancelled(true);
			if (((Mob) event.getDamager()).getTarget() == event.getDamager()) {
				((Mob) event.getDamager()).setTarget(null);
			}
			return;
		}
		//end-todo

		// Set base custom damage of crossbows and tridents before other modifications
		// No firework damage!
		if (event.getDamager() instanceof Firework) {
			event.setCancelled(true);
			return;
		}

		// Disable the randomness of Iron Golems' attacks
		if (event.getDamager() instanceof IronGolem golem) {
			event.setDamage(EntityUtils.getAttributeOrDefault(golem, Attribute.GENERIC_ATTACK_DAMAGE, 0));
		}

		if (event.getEntity() instanceof Player) {
			Entity damager = event.getDamager();
			if (damager instanceof AbstractArrow arrow) {
				ProjectileSource source = arrow.getShooter();
				if (source instanceof LivingEntity le) {
					EntityEquipment equipment = le.getEquipment();
					if (equipment != null) {
						ItemStack mainhand = equipment.getItemInMainHand();
						Material material = mainhand.getType();
						if (material == Material.TRIDENT || material == Material.CROSSBOW) {
							ItemMeta meta = mainhand.getItemMeta();
							if (meta != null && meta.hasAttributeModifiers()) {
								Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(Attribute.GENERIC_ATTACK_DAMAGE);
								if (modifiers != null) {
									for (AttributeModifier modifier : modifiers) {
										if (modifier.getOperation() == Operation.ADD_NUMBER) {
											event.setDamage(modifier.getAmount() + 1);
										}
									}
								}
							}
						}
					}
				}
			} else if (damager instanceof Fireball fireball) {
				//Custom damage of fireball set by the horse jump strength attribute in the mainhand of the mob
				ProjectileSource source = fireball.getShooter();
				if (source instanceof LivingEntity le) {
					EntityEquipment equipment = le.getEquipment();
					if (equipment != null) {
						ItemStack mainhand = equipment.getItemInMainHand();

						if (mainhand != null) {
							ItemMeta meta = mainhand.getItemMeta();
							if (meta != null && meta.hasAttributeModifiers()) {
								Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(Attribute.HORSE_JUMP_STRENGTH);
								if (modifiers != null) {
									for (AttributeModifier modifier : modifiers) {
										if (modifier.getOperation() == Operation.ADD_NUMBER) {
											event.setDamage(modifier.getAmount() + 1);
										}
									}
								}
							}
						}
					}
				}
			} else if (damager instanceof EvokerFangs fangs) {
				//Custom damage for evoker fangs, tied to main hand damage of evoker.
				LivingEntity source = fangs.getOwner();
				if (source != null) {
					EntityEquipment equipment = source.getEquipment();
					if (equipment != null) {
						ItemStack mainhand = equipment.getItemInMainHand();

						if (mainhand != null) {
							ItemMeta meta = mainhand.getItemMeta();
							if (meta != null && meta.hasAttributeModifiers()) {
								Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(Attribute.GENERIC_ATTACK_DAMAGE);
								if (modifiers != null) {
									for (AttributeModifier modifier : modifiers) {
										if (modifier.getOperation() == Operation.ADD_NUMBER) {
											event.setDamage(modifier.getAmount() + 1);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/* Prevent fire from catching in towns */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	void blockIgniteEvent(BlockIgniteEvent event) {
		Block block = event.getBlock();

		// If the block is within a safezone, cancel the ignition unless it was from a player in creative mode
		if (ZoneUtils.hasZoneProperty(block.getLocation(), ZoneProperty.ADVENTURE_MODE)) {
			if (event.getCause().equals(BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL)) {
				Player player = event.getPlayer();
				if (player != null && player.getGameMode() != GameMode.ADVENTURE) {
					// Don't cancel the event for non-adventure players
					return;
				}
			}

			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void entityDeathEvent(EntityDeathEvent event) {
		LivingEntity livingEntity = event.getEntity();
		boolean shouldGenDrops = true;

		// Check if this mob was likely spawned by a grinder spawner
		if (livingEntity.hasMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
			int spawnCount = 0;

			// There should only be one value - just use the latest one
			for (MetadataValue value : livingEntity.getMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
				spawnCount = value.asInt();
			}

			if (spawnCount > SPAWNER_DROP_THRESHOLD) {
				shouldGenDrops = false;

				// Don't drop any exp
				event.setDroppedExp(0);

				// Remove all drops except special lore text items
				event.getDrops().removeIf(itemStack -> !ItemUtils.doDropItemAfterSpawnerLimit(itemStack));
			}
		}

		Player player = livingEntity.getKiller();
		if (player != null && EntityUtils.isHostileMob(livingEntity)) {
			//  Player kills a mob
			mPlugin.mItemStatManager.onKill(mPlugin, player, event, livingEntity);
			AbilityManager.getManager().entityDeathEvent(player, event, shouldGenDrops);
			for (Player p : PlayerUtils.playersInRange(livingEntity.getLocation(), 20, true)) {
				AbilityManager.getManager().entityDeathRadiusEvent(p, event, shouldGenDrops);
			}
		}

		//Do not run below if it is the death of a player
		if (livingEntity instanceof Player) {
			return;
		}

		//Give wither to vexes spawned from the evoker that died so they die over time
		if (livingEntity instanceof Evoker) {
			List<LivingEntity> vexes = EntityUtils.getNearbyMobs(livingEntity.getLocation(), 30, EnumSet.of(EntityType.VEX));
			for (LivingEntity vex : vexes) {
				if (vex instanceof Vex && livingEntity.equals(((Vex) vex).getSummoner())) {
					vex.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 9999, 3));
				}
			}
		}

		// If the item has meta, run through the lore to check if it has quest item in the lore list
		// Don't do this for armor stands to prevent item duplication
		if (!(livingEntity instanceof ArmorStand)) {
			ListIterator<ItemStack> iter = event.getDrops().listIterator();
			while (iter.hasNext()) {
				ItemStack item = iter.next();
				if (item == null) {
					continue;
				}
				List<String> lore = ItemUtils.getPlainLore(item);
				if (lore != null && !lore.isEmpty()) {
					for (String loreEntry : lore) {
						if (loreEntry.contains("Quest Item")) {
							//Scales based off player count in a 20 meter radius, drops at least one quest item
							int count = PlayerUtils.playersInRange(livingEntity.getLocation(), 20, true).size();
							if (count < 1) {
								count = 1;
							}
							if (count > item.getAmount()) {
								item.setAmount(count);
							}
							return;
						}
					}
				}
			}
		}

		// Drop armed armor stands from armed variants
		if (livingEntity instanceof ArmorStand armorStand && armorStand.hasArms()) {
			List<ItemStack> drops = event.getDrops();
			if (drops.size() > 0 && drops.get(0).equals(new ItemStack(Material.ARMOR_STAND, 1))) {
				ItemStack armedArmorStand = InventoryUtils.getItemFromLootTable(event.getEntity(), ARMED_ARMOR_STAND_LOOT_TABLE);
				if (armedArmorStand != null) {
					drops.set(0, armedArmorStand);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void entityZapEvent(EntityZapEvent event) {
		if (event.getEntityType().equals(EntityType.VILLAGER)) {
			event.setCancelled(true);
		}
	}
}
