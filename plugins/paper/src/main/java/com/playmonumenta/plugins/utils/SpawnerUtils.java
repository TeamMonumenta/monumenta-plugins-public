package com.playmonumenta.plugins.utils;

import com.goncalomb.bukkit.nbteditor.nbt.EntityNBT;
import com.goncalomb.bukkit.nbteditor.nbt.SpawnerNBTWrapper;
import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.listeners.SpawnerListener;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.spawners.SpawnerActionManager;
import com.playmonumenta.plugins.spawners.SpawnerBreakAction;
import com.playmonumenta.plugins.spawners.actions.CustomFunctionAction;
import com.playmonumenta.plugins.spawners.types.ProtectorSpawner;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTTileEntity;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.listeners.SpawnerListener.spawnerCatMap;
import static com.playmonumenta.plugins.spawners.types.RallySpawner.getRally;
import static com.playmonumenta.plugins.spawners.types.RallySpawner.triggerRallyEffect;

/* How Spawner Break Actions are stored in NBT:
BreakActions [
	{
		identifier: String
		parameters: {
			key1: value1
			key2: value2
			...
		}
	},
	{
		identifier: String
		parameters: {
			key1: value1
			key2: value2
			...
		}
	},
	...
]
*/

public class SpawnerUtils {
	// Backwards compatibility: spawners have been placed with this tag in use.
	// Only remove this tag, and add the one below it.
	public static final String SHIELDED_SPAWNER_MARKER_TAG = "spawner_shield_display_marker";
	public static final String EFFECTS_SPAWNER_MARKER_TAG = "spawner_effects_display_marker";
	public static final String SHIELDS_ATTRIBUTE = "Shields";
	public static final String LOS_POOL_ATTRIBUTE = "LoSPool";
	public static final String GUARDED_ATTRIBUTE = "Guarded";
	public static final String ENSNARED_ATTRIBUTE = "Ensnared";
	public static final String RALLY_ATTRIBUTE = "Rally";
	public static final String PROTECTOR_ATTRIBUTE = "Protector";
	public static final String CAT_ATTRIBUTE = "Cat";
	public static final String CAT_ATTRIBUTE_RADIUS = "CatRadius";
	public static final String SEQUENCE_ATTRIBUTE = "Sequence";
	public static final String SEQUENCE_ATTRIBUTE_RADIUS = "SequenceRadius";
	public static final String DECAYING_ATTRIBUTE = "Decaying";
	public static final String BREAK_ACTIONS_ATTRIBUTE = "BreakActions";

	private static final NamespacedKey ORIGINAL_MAX_DELAY = NamespacedKeyUtils.fromString("original_max_delay");
	private static final NamespacedKey ORIGINAL_MIN_DELAY = NamespacedKeyUtils.fromString("original_min_delay");
	private static final NamespacedKey LAST_MULTIPLIER = NamespacedKeyUtils.fromString("last_multiplier");
	private static final NamespacedKey TORCH_LOCATIONS = NamespacedKeyUtils.fromString("spawner_torches");
	private static final NamespacedKey X = NamespacedKeyUtils.fromString("x");
	private static final NamespacedKey Y = NamespacedKeyUtils.fromString("y");
	private static final NamespacedKey Z = NamespacedKeyUtils.fromString("z");
	private static final int MAX_TORCH_TAXICAB_DISTANCE = 8;
	private static final double SPAWNER_DELAY_MULTIPLIER_CAP = 10;
	public static final Set<Location> spawnersWithCat = new HashSet<>();
	public static final Map<Location, BukkitRunnable> ensnarementTasks = new HashMap<>();

	/**
	 * Returns true if the damage is enough to break the spawner.
	 * Otherwise, removes shields equal to the damage done.
	 *
	 * @param block  the spawner block.
	 * @param damage the damage dealt by the pickaxe.
	 */
	public static boolean tryBreakSpawner(Block block, int damage, boolean tryBreak) {
		// start ensnare if needed
		if (getSpawnerType(block, ENSNARED_ATTRIBUTE) > 0 && !ensnarementTasks.containsKey(block.getLocation())) {
			List<Entity> nearbyEntities = (List<Entity>) block.getWorld().getNearbyEntities(block.getLocation().clone().add(0.5, 0.5, 0.5), getSpawnerType(block, ENSNARED_ATTRIBUTE), getSpawnerType(block, ENSNARED_ATTRIBUTE), getSpawnerType(block, ENSNARED_ATTRIBUTE));
			boolean hasNearbyMobs = nearbyEntities.stream()
				.anyMatch(entity -> entity instanceof LivingEntity && !(entity instanceof Player) && entity.isValid());
			if (hasNearbyMobs) {
				block.getLocation().getWorld().playSound(block.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 0.75f, 2f);
				startEnsnarementCheck(block);
			}
		}

		if (!hasShieldsAttribute(block) && !(getSpawnerType(block, GUARDED_ATTRIBUTE) > 0) && !(getSpawnerType(block, CAT_ATTRIBUTE) > 0) && !(getSpawnerType(block, SEQUENCE_ATTRIBUTE) > 0)) {
			return true;
		}
		int guarded = getSpawnerType(block, GUARDED_ATTRIBUTE);
		int shields = getShields(block);

		// see if cat's alive
		UUID catUUID = spawnerCatMap.get(block.getLocation());
		if (catUUID != null) {
			Cat cat = (Cat) Bukkit.getEntity(catUUID);
			if (cat != null && cat.isValid()) {
				int catRadius = getSpawnerType(block, CAT_ATTRIBUTE_RADIUS);
				if (cat.getLocation().distanceSquared(block.getLocation()) > catRadius * catRadius) {
					Location location = block.getLocation().add(0.5, 1, 0.5);
					Location catLocation = findValidCatSpawn(location, getSpawnerType(block, CAT_ATTRIBUTE_RADIUS));
					cat.teleport(catLocation);
					Plugin.getInstance().mEffectManager.addEffect(cat, "spawnerCatVuln", new PercentDamageReceived(30, -1.0));
					Plugin.getInstance().mEffectManager.addEffect(cat, "spawnerCatSpeed", new PercentSpeed(20, 1, "PercentSpeed"));
				}
				PPLine line = new PPLine(Particle.ENCHANTMENT_TABLE, block.getLocation().clone().add(0.5, 0.5, 0.5), cat.getLocation().clone().add(0, cat.getHeight() / 2, 0));
				line.countPerMeter(10).spawnAsEnemy();
				block.getLocation().getWorld().playSound(block.getLocation(), Sound.ENTITY_CAT_HURT, SoundCategory.HOSTILE, 1f, 1f);
				return false;
			} else {
				spawnerCatMap.remove(block.getLocation());
			}
		}

		// spawn cat if first attempt at break
		if (!spawnersWithCat.contains(block.getLocation())) {
			int catHealth = getSpawnerType(block, CAT_ATTRIBUTE);
			if (catHealth > 0) {
				Location location = block.getLocation().add(0.5, 1, 0.5);
				Location catLocation = findValidCatSpawn(location, getSpawnerType(block, CAT_ATTRIBUTE_RADIUS));
				Cat cat = (Cat) block.getWorld().spawnEntity(catLocation, EntityType.CAT);
				cat.setPersistent(true);
				// set health to specified
				Objects.requireNonNull(cat.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(catHealth);
				cat.setHealth(catHealth);
				cat.addScoreboardTag("Hostile");
				spawnerCatMap.put(block.getLocation(), cat.getUniqueId());
				spawnersWithCat.add(block.getLocation());

				Plugin.getInstance().mEffectManager.addEffect(cat, "spawnerCatVuln", new PercentDamageReceived(30, -1.0));
				Plugin.getInstance().mEffectManager.addEffect(cat, "spawnerCatSpeed", new PercentSpeed(20, 1, "PercentSpeed"));

				block.getLocation().getWorld().playSound(block.getLocation(), Sound.ENTITY_CAT_AMBIENT, SoundCategory.HOSTILE, 1f, 1f);

				// ensure ensnared is checked when a cat spawns
				if (getSpawnerType(block, ENSNARED_ATTRIBUTE) > 0 && !ensnarementTasks.containsKey(block.getLocation())) {
					List<Entity> nearbyEntities = (List<Entity>) block.getWorld().getNearbyEntities(block.getLocation().clone().add(0.5, 0.5, 0.5), getSpawnerType(block, ENSNARED_ATTRIBUTE), getSpawnerType(block, ENSNARED_ATTRIBUTE), getSpawnerType(block, ENSNARED_ATTRIBUTE));
					boolean hasNearbyMobs = nearbyEntities.stream()
						.anyMatch(entity -> entity instanceof LivingEntity && !(entity instanceof Player) && entity.isValid());
					if (hasNearbyMobs) {
						block.getLocation().getWorld().playSound(block.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 0.75f, 2f);
						startEnsnarementCheck(block);
					}
				}
				return false;
			}
		}

		// guarded mob check
		List<SpawnerListener.MobInfo> spawnerInfo = SpawnerListener.mSpawnerInfos.get(block.getLocation());
		if (spawnerInfo != null && !spawnerInfo.isEmpty()) {
			boolean hasNearbyMobs = spawnerInfo.stream().anyMatch(mobInfo -> {
				LivingEntity mob = mobInfo.getMob();
				double guardedSquared = guarded * guarded;
				return mob != null && mob.isValid() && mob.getLocation().distanceSquared(block.getLocation()) <= guardedSquared;
			});
			if (hasNearbyMobs) {
				block.getLocation().getWorld().playSound(block.getLocation(), Sound.BLOCK_CHAIN_FALL, SoundCategory.HOSTILE, 0.5f, 1f);
				// particles for unsuccessful break
				double guardedSquared = guarded * guarded;
				spawnerInfo.stream().filter(mobInfo -> {
					LivingEntity mob = mobInfo.getMob();
					return mob != null && mob.isValid() && mob.getLocation().distanceSquared(block.getLocation()) <= guardedSquared;
				}).forEach(mobInfo -> {
					LivingEntity mob = mobInfo.getMob();
					if (mob != null) {
						PPLine line = new PPLine(Particle.ENCHANTMENT_TABLE, block.getLocation().clone().add(0.5, 0.5, 0.5), mob.getLocation().clone().add(0, mob.getHeight() / 2, 0));
						line.countPerMeter(10).spawnAsEnemy();
						new PPCircle(Particle.SCULK_SOUL, block.getLocation().clone().add(0.5, 0.5, 0.5), 1).countPerMeter(2).directionalMode(false).rotateDelta(true).spawnAsEnemy();
					}
				});
				return false;
			}
		}

		// sequence check
		if (getSpawnerType(block, SEQUENCE_ATTRIBUTE) > 0 && !checkSequence(block, getSpawnerType(block, SEQUENCE_ATTRIBUTE_RADIUS))) {
			block.getLocation().getWorld().playSound(block.getLocation(), Sound.BLOCK_BARREL_CLOSE, SoundCategory.HOSTILE, 0.75f, 1f);
			return false;
		}

		// shield check
		if (shields < damage) {
			setShields(block, 0);
			if (getRally(block) > 0) {
				triggerRallyEffect(block, getRally(block));
			}
			return true;
		}

		// make sure it's not a pickaxe enchant method call
		if (tryBreak) {
			setShields(block, shields - damage);
			if (getSpawnerType(block, SEQUENCE_ATTRIBUTE) > 0) {
				SpawnerListener.doShieldBreakAnimation(block.getLocation().clone().add(0.5, 0.5, 0.5), getShields(block));
			}
		}
		return false;
	}

	public static void startSpawnerEffectsDisplay(Marker marker) {
		if (!marker.getScoreboardTags().contains(EFFECTS_SPAWNER_MARKER_TAG) && !marker.getScoreboardTags().contains(SHIELDED_SPAWNER_MARKER_TAG)) {
			return;
		}

		new BukkitRunnable() {
			final Marker mMarker = marker;
			final PPCircle mHealthyShield = new PPCircle(Particle.SOUL_FIRE_FLAME, mMarker.getLocation(), 1)
				.countPerMeter(2).distanceFalloff(20).ringMode(true);
			final boolean mHasLosPool = getLosPool(marker.getLocation().getBlock()) != null;

			@Override
			public void run() {
				if (PlayerUtils.playersInRange(marker.getLocation(), 20, false).isEmpty()) {
					return;
				}
				// If the marker despawned or got removed, cancel the runnable.
				if (!mMarker.isValid()) {
					cancel();
					return;
				}

				Block spawnerBlock = marker.getLocation().getBlock();
				if (!isSpawner(spawnerBlock)) {
					// The spawner was destroyed and the marker somehow is still lingering around.
					cancel();
					return;
				}

				// Spawn the particles
				int shields = getShields(spawnerBlock);
				if (shields > 0) {
					mHealthyShield.spawnFull();
				}

				if (mHasLosPool) {
					Location centerLoc = BlockUtils.getCenterBlockLocation(spawnerBlock);
					// Place colorful particles at the corners of the spawner.
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(0.5, 0.5, 0.5), 1).data(ParticleUtils.getRandomColorOptions(150, 1)).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(0.5, -0.5, 0.5), 1).data(ParticleUtils.getRandomColorOptions(150, 1)).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(-0.5, 0.5, 0.5), 1).data(ParticleUtils.getRandomColorOptions(150, 1)).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(-0.5, -0.5, 0.5), 1).data(ParticleUtils.getRandomColorOptions(150, 1)).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(0.5, 0.5, -0.5), 1).data(ParticleUtils.getRandomColorOptions(150, 1)).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(0.5, -0.5, -0.5), 1).data(ParticleUtils.getRandomColorOptions(150, 1)).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(-0.5, 0.5, -0.5), 1).data(ParticleUtils.getRandomColorOptions(150, 1)).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(-0.5, -0.5, -0.5), 1).data(ParticleUtils.getRandomColorOptions(150, 1)).spawnFull();

					// Explicit null check for Review Dog
					String losPool = getLosPool(spawnerBlock);
					if (losPool != null) {
						List<Soul> souls = LibraryOfSoulsIntegration.getPool(losPool).keySet().stream().toList();
						if (souls.isEmpty()) {
							return;
						}
						Soul soul = souls.get(0);
						SpawnerNBTWrapper wrapper = new SpawnerNBTWrapper(spawnerBlock);
						wrapper.clearEntities();
						wrapper.addEntity(new SpawnerNBTWrapper.SpawnerEntity(EntityNBT.fromEntityData(soul.getNBT()), 1));
						wrapper.save();
					}
				}
				int decay = getSpawnerType(spawnerBlock, DECAYING_ATTRIBUTE);
				if (decay > 0) {
					Location centerLoc = BlockUtils.getCenterBlockLocation(spawnerBlock);
					Particle.DustOptions mBROWN = new Particle.DustOptions(Color.fromRGB(125, 80, 9), 1.0f);
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1)), 1).data(mBROWN).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1)), 1).data(mBROWN).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1)), 1).data(mBROWN).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1)), 1).data(mBROWN).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1)), 1).data(mBROWN).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1)), 1).data(mBROWN).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1)), 1).data(mBROWN).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1)), 1).data(mBROWN).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1)), 1).data(mBROWN).spawnFull();
				}

				if (ProtectorSpawner.getProtector(spawnerBlock)) {
					Particle.DustOptions mYELLOW = new Particle.DustOptions(Color.fromRGB(237, 198, 26), 1.0f);
					new PPCircle(Particle.REDSTONE, spawnerBlock.getLocation().clone().add(0.5, 1.2, 0.5), 0.5).data(mYELLOW).countPerMeter(8).spawnAsEnemy();
				}

				List<String> breakActionIdentifiers = getBreakActionIdentifiers(spawnerBlock);
				breakActionIdentifiers.forEach(id -> {
					SpawnerBreakAction action = SpawnerActionManager.getAction(id);
					if (action != null) {
						action.periodicAesthetics(spawnerBlock);
					}
				});
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 20);
	}

	public static List<String> getBreakActionIdentifiers(ItemStack spawnerItem) {
		if (!isSpawner(spawnerItem)) {
			return Collections.emptyList();
		}

		return new NBTItem(spawnerItem).getCompoundList(BREAK_ACTIONS_ATTRIBUTE)
			.stream().map(compound -> compound.getString("identifier")).toList();
	}

	public static List<String> getBreakActionIdentifiers(Block spawnerBlock) {
		if (!isSpawner(spawnerBlock)) {
			return Collections.emptyList();
		}

		return new NBTTileEntity(spawnerBlock.getState()).getPersistentDataContainer().getCompoundList(BREAK_ACTIONS_ATTRIBUTE)
			.stream().map(compound -> compound.getString("identifier")).toList();
	}

	public static void transferBreakActionList(ItemStack spawnerItem, Block spawnerBlock) {
		if (!isSpawner(spawnerItem) || !isSpawner(spawnerBlock)) {
			return;
		}

		NBTItem item = new NBTItem(spawnerItem);
		NBTTileEntity tileEntity = new NBTTileEntity(spawnerBlock.getState());
		NBTCompoundList itemBreakActions = item.getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		NBTCompoundList tileBreakActions = tileEntity.getPersistentDataContainer().getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		itemBreakActions.forEach(tileBreakActions::addCompound);
	}

	public static void addBreakAction(ItemStack spawnerItem, String actionIdentifier) {
		if (!isSpawner(spawnerItem)) {
			return;
		}

		NBTItem item = new NBTItem(spawnerItem);
		NBTCompoundList breakActions = item.getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		List<ReadWriteNBT> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() == 0) {
			// Create a new container for the action, add the identifier,
			// and add the compound containing the parameters and their default values.
			NBTContainer container = new NBTContainer();
			container.setString("identifier", actionIdentifier);
			NBTCompound parameters = container.addCompound("parameters");
			addParametersToCompound(SpawnerActionManager.getActionParameters(actionIdentifier), parameters);
			breakActions.addCompound(container);
			spawnerItem.setItemMeta(item.getItem().getItemMeta());
		}
	}

	public static void addBreakAction(Block spawnerBlock, String actionIdentifier) {
		if (!isSpawner(spawnerBlock)) {
			return;
		}

		NBTTileEntity tileEntity = new NBTTileEntity(spawnerBlock.getState());
		NBTCompoundList breakActions = tileEntity.getPersistentDataContainer().getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		List<ReadWriteNBT> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() == 0) {
			// Create a new container for the action, add the identifier,
			// and add the compound containing the parameters and their default values.
			NBTContainer container = new NBTContainer();
			container.setString("identifier", actionIdentifier);
			NBTCompound parameters = container.addCompound("parameters");
			addParametersToCompound(SpawnerActionManager.getActionParameters(actionIdentifier), parameters);
			breakActions.addCompound(container);
		}
	}

	public static void addBreakActions(ItemStack spawnerItem, List<String> actionIdentifiers) {
		actionIdentifiers.forEach(actionIdentifier -> addBreakAction(spawnerItem, actionIdentifier));
	}

	public static void addBreakActions(Block spawnerBlock, List<String> actionIdentifiers) {
		actionIdentifiers.forEach(actionIdentifier -> addBreakAction(spawnerBlock, actionIdentifier));
	}

	public static void addParametersToCompound(Map<String, Object> parameters, NBTCompound compound) {
		parameters.forEach(compound::setObject);
	}

	public static void removeBreakAction(ItemStack spawnerItem, String actionIdentifier) {
		if (!isSpawner(spawnerItem)) {
			return;
		}

		NBTItem item = new NBTItem(spawnerItem);
		NBTCompoundList breakActions = item.getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		List<ReadWriteNBT> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() > 0) {
			// Remove the action
			breakActions.remove(wantedAction.get(0));
			spawnerItem.setItemMeta(item.getItem().getItemMeta());
		}
	}

	public static void setParameterValue(ItemStack spawnerItem, String actionIdentifier, String parameterName, Object value) {
		if (!isSpawner(spawnerItem)) {
			return;
		}

		NBTItem item = new NBTItem(spawnerItem);
		NBTCompoundList breakActions = item.getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		// Try to find the requested action
		List<ReadWriteNBT> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() > 0) {
			// Set the requested parameter
			ReadWriteNBT actionCompound = wantedAction.get(0);
			ReadWriteNBT parameters = actionCompound.getOrCreateCompound("parameters");
			parameters.setString(parameterName, MessagingUtils.GSON.toJson(value, value.getClass()));
			spawnerItem.setItemMeta(item.getItem().getItemMeta());
		}
	}

	public static void setParameterValue(Block spawnerBlock, String actionIdentifier, String parameterName, Object value) {
		if (!isSpawner(spawnerBlock)) {
			return;
		}

		NBTTileEntity tileEntity = new NBTTileEntity(spawnerBlock.getState());
		NBTCompoundList breakActions = tileEntity.getPersistentDataContainer().getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		// Try to find the requested action
		List<ReadWriteNBT> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() > 0) {
			// Set the requested parameter
			ReadWriteNBT actionCompound = wantedAction.get(0);
			ReadWriteNBT parameters = actionCompound.getOrCreateCompound("parameters");
			parameters.setString(parameterName, MessagingUtils.GSON.toJson(value, value.getClass()));
		}
	}

	public static @Nullable Object getParameterValue(ItemStack spawnerItem, String actionIdentifier, String parameterName) {
		if (!isSpawner(spawnerItem)) {
			return null;
		}

		NBTItem item = new NBTItem(spawnerItem);
		NBTCompoundList breakActions = item.getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		// Try to find the requested action
		List<ReadWriteNBT> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() > 0) {
			// Try to find the requested parameter
			ReadWriteNBT actionCompound = wantedAction.get(0);
			ReadWriteNBT parameters = actionCompound.getOrCreateCompound("parameters");
			if (parameters.hasTag(parameterName)) {
				String json = parameters.getOrNull(parameterName, String.class);
				if (json == null) {
					return null;
				}
				Object parameter = SpawnerActionManager.getActionParameters(actionIdentifier).get(parameterName);
				if (parameter == null) {
					return null;
				}
				return MessagingUtils.GSON.fromJson(json, parameter.getClass());
			}
		}

		return null;
	}

	public static @Nullable Object getParameterValue(Block spawnerBlock, String actionIdentifier, String parameterName) {
		if (!isSpawner(spawnerBlock)) {
			return null;
		}

		NBTTileEntity tileEntity = new NBTTileEntity(spawnerBlock.getState());
		NBTCompoundList breakActions = tileEntity.getPersistentDataContainer().getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		// Try to find the requested action
		List<ReadWriteNBT> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() > 0) {
			// Try to find the requested parameter
			ReadWriteNBT actionCompound = wantedAction.get(0);
			ReadWriteNBT parameters = actionCompound.getOrCreateCompound("parameters");
			if (parameters.hasTag(parameterName)) {
				String json = parameters.getOrNull(parameterName, String.class);
				if (json == null) {
					return null;
				}
				Object parameter = SpawnerActionManager.getActionParameters(actionIdentifier).get(parameterName);
				if (parameter == null) {
					return null;
				}
				return MessagingUtils.GSON.fromJson(json, parameter.getClass());
			}
		}

		return null;
	}

	public static Map<String, Object> getStoredParameters(Block spawnerBlock, String actionIdentifier) {
		if (!isSpawner(spawnerBlock)) {
			return new HashMap<>();
		}

		NBTTileEntity tileEntity = new NBTTileEntity(spawnerBlock.getState());
		NBTCompoundList breakActions = tileEntity.getPersistentDataContainer().getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		List<ReadWriteNBT> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() > 0) {
			HashMap<String, Object> parameterMap = new HashMap<>(SpawnerActionManager.getActionParameters(actionIdentifier));
			ReadWriteNBT actionCompound = wantedAction.get(0);
			ReadWriteNBT parameters = actionCompound.getOrCreateCompound("parameters");
			// Start from base parameters map and replace the values with the ones stored on the block.
			parameterMap.forEach((key, value) -> {
				String json = parameters.getOrNull(key, String.class);
				if (json == null) {
					return;
				}

				parameterMap.replace(key, MessagingUtils.GSON.fromJson(json, value.getClass()));
			});

			return parameterMap;
		}

		return new HashMap<>();
	}

	public static @Nullable String getLosPool(ItemStack spawnerItem) {
		if (!isSpawner(spawnerItem)) {
			return null;
		}

		String losPool = new NBTItem(spawnerItem).getString(LOS_POOL_ATTRIBUTE);

		return losPool.equals("") ? null : losPool;
	}

	public static @Nullable String getLosPool(Block spawnerBlock) {
		if (!isSpawner(spawnerBlock)) {
			return null;
		}

		String losPool = new NBTTileEntity(spawnerBlock.getState()).getPersistentDataContainer().getString(LOS_POOL_ATTRIBUTE);

		return losPool.equals("") ? null : losPool;
	}

	public static void setLosPool(ItemStack spawnerItem, String losPool) {
		if (!isSpawner(spawnerItem)) {
			return;
		}

		NBTItem item = new NBTItem(spawnerItem);
		item.setString(LOS_POOL_ATTRIBUTE, losPool);
		spawnerItem.setItemMeta(item.getItem().getItemMeta());
	}

	public static void setLosPool(Block spawnerBlock, String losPool) {
		if (!isSpawner(spawnerBlock)) {
			return;
		}

		NBTTileEntity tileEntity = new NBTTileEntity(spawnerBlock.getState());
		tileEntity.getPersistentDataContainer().setString(LOS_POOL_ATTRIBUTE, losPool);
	}

	public static boolean hasShieldsAttribute(Block spawnerBlock) {
		if (!isSpawner(spawnerBlock)) {
			return false;
		}

		return new NBTTileEntity(spawnerBlock.getState()).getPersistentDataContainer().getKeys().contains(SHIELDS_ATTRIBUTE);
	}

	public static int getShields(ItemStack spawnerItem) {
		if (!isSpawner(spawnerItem)) {
			return 0;
		}

		NBTItem item = new NBTItem(spawnerItem);

		if (!item.hasTag(SHIELDS_ATTRIBUTE)) {
			return 0;
		}

		return item.getInteger(SHIELDS_ATTRIBUTE);
	}

	public static int getShields(Block spawnerBlock) {
		if (!isSpawner(spawnerBlock)) {
			return 0;
		}

		NBTCompound dataContainer = new NBTTileEntity(spawnerBlock.getState()).getPersistentDataContainer();

		if (!dataContainer.hasTag(SHIELDS_ATTRIBUTE)) {
			return 0;
		}

		return dataContainer.getInteger(SHIELDS_ATTRIBUTE);
	}

	public static void setShields(ItemStack spawnerItem, int shields) {
		if (shields < 0 || !isSpawner(spawnerItem)) {
			return;
		}

		NBTItem item = new NBTItem(spawnerItem);
		item.setInteger(SHIELDS_ATTRIBUTE, shields);
		spawnerItem.setItemMeta(item.getItem().getItemMeta());
	}

	public static void setShields(Block spawnerBlock, int shields) {
		if (shields < 0 || !isSpawner(spawnerBlock)) {
			return;
		}

		NBTTileEntity tileEntity = new NBTTileEntity(spawnerBlock.getState());
		tileEntity.getPersistentDataContainer().setInteger(SHIELDS_ATTRIBUTE, shields);
	}

	public static void unsetCustomFunction(ItemStack spawnerItem) {
		if (!isSpawner(spawnerItem)) {
			return;
		}

		boolean shouldClearBreakActionsNBT = getBreakActionIdentifiers(spawnerItem).size() == 1;
		removeBreakAction(spawnerItem, CustomFunctionAction.IDENTIFIER);
		if (shouldClearBreakActionsNBT) {
			//clear it so it doesn't spawn particles when breaking the spawner.
			NBTItem item = new NBTItem(spawnerItem);
			item.removeKey(BREAK_ACTIONS_ATTRIBUTE);
			spawnerItem.setItemMeta(item.getItem().getItemMeta());
		}
	}

	public static void setCustomFunction(ItemStack spawnerItem, FunctionWrapper function) {
		if (!isSpawner(spawnerItem)) {
			return;
		}

		if (!SpawnerActionManager.actionExists(CustomFunctionAction.IDENTIFIER)) {
			MMLog.warning("Custom function break action is missing, could not apply custom function.");
			return;
		}
		addBreakAction(spawnerItem, CustomFunctionAction.IDENTIFIER);
		setParameterValue(spawnerItem, CustomFunctionAction.IDENTIFIER, CustomFunctionAction.FUNCTION_KEY, function.getKey().asString());
	}

	public static boolean isSpawner(Block block) {
		return block.getType().equals(Material.SPAWNER);
	}

	public static boolean isSpawner(ItemStack item) {
		return item.getType().equals(Material.SPAWNER);
	}

	public static void addEffectsDisplayMarker(Block block) {
		Entity entity = block.getWorld().spawnEntity(BlockUtils.getCenterBlockLocation(block), EntityType.MARKER);
		if (entity instanceof Marker marker) {
			marker.addScoreboardTag(EFFECTS_SPAWNER_MARKER_TAG);
			startSpawnerEffectsDisplay(marker);
		}
	}

	public static void removeEffectsDisplayMarker(Block block) {
		BlockUtils.getCenterBlockLocation(block).getNearbyEntities(0.1, 0.1, 0.1).forEach(e -> {
			if (e instanceof Marker marker &&
				(marker.getScoreboardTags().contains(SHIELDED_SPAWNER_MARKER_TAG) || marker.getScoreboardTags().contains(EFFECTS_SPAWNER_MARKER_TAG))) {
				marker.remove();
			}
		});
	}

	public static boolean hasEffectsDisplayMarker(Block block) {
		return BlockUtils.getCenterBlockLocation(block).getNearbyEntities(0.1, 0.1, 0.1)
			.stream().anyMatch(e -> (e instanceof Marker marker &&
				(marker.getScoreboardTags().contains(SHIELDED_SPAWNER_MARKER_TAG) || marker.getScoreboardTags().contains(EFFECTS_SPAWNER_MARKER_TAG)))
			);
	}

	public static void addTorch(Block block) {
		List<CreatureSpawner> spawners = getNearbySpawners(block);
		for (CreatureSpawner spawner : spawners) {
			if (addTorchToSpawner(spawner, block)) {
				updateSpawnDelay(spawner);
			}
		}
	}

	public static void removeTorch(Block block) {
		List<CreatureSpawner> spawners = getNearbySpawners(block);
		for (CreatureSpawner spawner : spawners) {
			if (filterTorchesFromSpawner(spawner)) {
				updateSpawnDelay(spawner);
			}
		}
	}

	private static List<CreatureSpawner> getNearbySpawners(Block block) {
		List<CreatureSpawner> spawners = new ArrayList<>();
		for (int x = -16; x <= 16; x += 16) {
			for (int z = -16; z <= 16; z += 16) {
				Chunk chunk = block.getLocation().clone().add(x, 0, z).getChunk();
				if (!chunk.isLoaded()) {
					continue;
				}
				chunk.getTileEntities();
				spawners.addAll(chunk.getTileEntities(b -> BlockUtils.taxiCabDistance(block, b) <= MAX_TORCH_TAXICAB_DISTANCE, true).stream().filter(b -> b instanceof CreatureSpawner).map(b -> (CreatureSpawner) b).toList());
			}
		}
		return spawners;
	}

	private static boolean addTorchToSpawner(CreatureSpawner spawner, Block torch) {
		if (BlockUtils.findNonOccludingTaxicabDistance(torch, spawner.getBlock(), MAX_TORCH_TAXICAB_DISTANCE) >= 0) {
			List<Block> torches = getTorches(spawner);
			torches.add(torch);
			setTorches(spawner, torches);
			return true;
		}
		return false;
	}

	private static boolean filterTorchesFromSpawner(CreatureSpawner spawner) {
		List<Block> torches = getTorches(spawner);
		int count = torches.size();
		torches.removeIf(b -> !BlockUtils.isTorch(b));
		if (torches.size() < count) {
			setTorches(spawner, torches);
			return true;
		}
		return false;
	}

	private static List<Block> getTorches(CreatureSpawner spawner) {
		World world = spawner.getWorld();
		PersistentDataContainer[] containers = getTorchContainers(spawner);
		return new ArrayList<>(Arrays.stream(containers).map(c -> getBlockFromContainer(c, world)).toList());
	}

	private static PersistentDataContainer[] getTorchContainers(CreatureSpawner spawner) {
		return spawner.getPersistentDataContainer().getOrDefault(TORCH_LOCATIONS, PersistentDataType.TAG_CONTAINER_ARRAY, new PersistentDataContainer[]{});
	}

	private static void setTorches(CreatureSpawner spawner, List<Block> torches) {
		PersistentDataContainer persistentDataContainer = spawner.getPersistentDataContainer();
		PersistentDataAdapterContext context = persistentDataContainer.getAdapterContext();
		persistentDataContainer.set(TORCH_LOCATIONS, PersistentDataType.TAG_CONTAINER_ARRAY,
			torches.stream().map(b -> getContainerFromBlock(b, context)).toArray(PersistentDataContainer[]::new));
	}

	private static Block getBlockFromContainer(PersistentDataContainer c, World world) {
		return new Location(world, Objects.requireNonNull(c.get(X, PersistentDataType.INTEGER)), Objects.requireNonNull(c.get(Y, PersistentDataType.INTEGER)), Objects.requireNonNull(c.get(Z, PersistentDataType.INTEGER))).getBlock();
	}

	private static PersistentDataContainer getContainerFromBlock(Block b, PersistentDataAdapterContext context) {
		PersistentDataContainer container = context.newPersistentDataContainer();
		container.set(X, PersistentDataType.INTEGER, b.getX());
		container.set(Y, PersistentDataType.INTEGER, b.getY());
		container.set(Z, PersistentDataType.INTEGER, b.getZ());
		return container;
	}

	private static void updateSpawnDelay(CreatureSpawner spawner) {
		PersistentDataContainer persistentDataContainer = spawner.getPersistentDataContainer();
		// Initialize the default values of the max and min delay
		int originalMaxDelay;
		if (!persistentDataContainer.has(ORIGINAL_MAX_DELAY)) {
			originalMaxDelay = spawner.getMaxSpawnDelay();
			persistentDataContainer.set(ORIGINAL_MAX_DELAY, PersistentDataType.INTEGER, originalMaxDelay);
		} else {
			originalMaxDelay = Objects.requireNonNull(persistentDataContainer.get(ORIGINAL_MAX_DELAY, PersistentDataType.INTEGER));
		}
		if (originalMaxDelay <= 0) {
			MMLog.warning("Non-positive spawner delay detected, setting it to 100. location=" + spawner.getLocation() + ", delay on spawner=" + spawner.getMaxSpawnDelay()
				+ ", delay in persistent container=" + Objects.requireNonNull(persistentDataContainer.get(ORIGINAL_MAX_DELAY, PersistentDataType.INTEGER)));
			originalMaxDelay = 100;
			persistentDataContainer.set(ORIGINAL_MAX_DELAY, PersistentDataType.INTEGER, originalMaxDelay);
		}

		int originalMinDelay;
		if (!persistentDataContainer.has(ORIGINAL_MIN_DELAY)) {
			originalMinDelay = spawner.getMinSpawnDelay();
			persistentDataContainer.set(ORIGINAL_MIN_DELAY, PersistentDataType.INTEGER, originalMinDelay);
		} else {
			originalMinDelay = Objects.requireNonNull(persistentDataContainer.get(ORIGINAL_MIN_DELAY, PersistentDataType.INTEGER));
		}
		if (originalMinDelay < 0) {
			originalMinDelay = 0;
		}

		double lastMultiplier = !persistentDataContainer.has(LAST_MULTIPLIER) ? 1 : Objects.requireNonNull(persistentDataContainer.get(LAST_MULTIPLIER, PersistentDataType.DOUBLE));

		Block spawnerBlock = spawner.getBlock();
		double multiplier = 1;
		List<Block> torches = getTorches(spawner);
		for (Block torch : torches) {
			int distance = BlockUtils.findNonOccludingTaxicabDistance(torch, spawnerBlock, MAX_TORCH_TAXICAB_DISTANCE);
			if (distance <= 0) {
				continue;
			}
			multiplier = Math.min(SPAWNER_DELAY_MULTIPLIER_CAP, (multiplier * (distance + 1)) / distance);
		}

		double multiplierRatio = multiplier / lastMultiplier;
		persistentDataContainer.set(LAST_MULTIPLIER, PersistentDataType.DOUBLE, multiplier);

		// Need to do them in the right order or else it sometimes throws an IllegalArgumentException
		spawner.setMinSpawnDelay(0);
		spawner.setMaxSpawnDelay((int) (originalMaxDelay * multiplier));
		spawner.setMinSpawnDelay((int) (originalMinDelay * multiplier));
		spawner.setDelay((int) (spawner.getDelay() * multiplierRatio));
		spawner.update(false, false);
	}

	public static void addSpawnerEffectMarkers(Location corner1, Location corner2) {
		int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
		int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
		int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
		int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
		int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
		int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Location blockLoc = corner1.clone().set(x, y, z);
					Block block = blockLoc.getBlock();

					if (isSpawner(block) && !hasEffectsDisplayMarker(block)) {
						addEffectsDisplayMarker(block);
					}
				}
			}
		}
	}

	public static int getSpawnerType(ItemStack spawnerItem, String attribute) {
		if (!isSpawner(spawnerItem)) {
			return 0;
		}

		NBTItem item = new NBTItem(spawnerItem);

		if (!item.hasTag(attribute)) {
			return 0;
		}

		return item.getInteger(attribute);
	}

	public static int getSpawnerType(Block spawnerBlock, String attribute) {
		if (!isSpawner(spawnerBlock)) {
			return 0;
		}

		NBTCompound dataContainer = new NBTTileEntity(spawnerBlock.getState()).getPersistentDataContainer();

		if (!dataContainer.hasTag(attribute)) {
			return 0;
		}

		return dataContainer.getInteger(attribute);
	}

	public static void setSpawnerType(ItemStack spawnerItem, String attribute, int attribute1) {
		if (attribute1 < 0 || !isSpawner(spawnerItem)) {
			return;
		}

		NBTItem item = new NBTItem(spawnerItem);
		item.setInteger(attribute, attribute1);
		spawnerItem.setItemMeta(item.getItem().getItemMeta());
	}

	public static void setSpawnerType(Block spawnerBlock, String attribute, int attribute1) {
		if (attribute1 < 0 || !isSpawner(spawnerBlock)) {
			return;
		}

		NBTTileEntity tileEntity = new NBTTileEntity(spawnerBlock.getState());
		tileEntity.getPersistentDataContainer().setInteger(attribute, attribute1);
	}

	public static void startEnsnarementCheck(Block block) {
		Location spawnerLocation = block.getLocation().clone().add(0.5, 0.5, 0.5);
		int ensnareRadius = getSpawnerType(block, ENSNARED_ATTRIBUTE);
		BukkitRunnable task = new BukkitRunnable() {
			private static final int TIME_LIMIT_TICKS = 5 * 60 * 20;
			private int mTicks = 0;
			@Override
			public void run() {
				List<Entity> nearbyEntities = (List<Entity>) spawnerLocation.getWorld().getNearbyEntities(spawnerLocation, ensnareRadius - 2, ensnareRadius - 2, ensnareRadius - 2);
				boolean hasNearbyMobs = nearbyEntities.stream()
					.anyMatch(entity -> entity instanceof LivingEntity && !(entity instanceof Player) && !(entity instanceof Villager) && !(entity instanceof ArmorStand) && entity.isValid());
				boolean hasPlayersInRange = spawnerLocation.getWorld().getPlayers().stream()
					.anyMatch(player -> player.getLocation().distance(spawnerLocation) <= 10 + 5);

				if (!hasNearbyMobs || !hasPlayersInRange || mTicks >= TIME_LIMIT_TICKS) {
					block.getLocation().getWorld().playSound(block.getLocation(), Sound.BLOCK_ANVIL_DESTROY, SoundCategory.HOSTILE, 0.75f, 2f);
					ensnarementTasks.remove(block.getLocation());
					this.cancel();
				}

				for (Player player : spawnerLocation.getWorld().getPlayers()) {
					if (player.getLocation().distance(spawnerLocation) > ensnareRadius && player.getLocation().distance(spawnerLocation) < ensnareRadius + 5) {
						if (player.getGameMode() == GameMode.SPECTATOR) {
							continue;
						}
						Location playerLoc = player.getLocation().clone();
						playerLoc.setY(spawnerLocation.getY());
						double horizontalDistance = playerLoc.distance(spawnerLocation);
						if (horizontalDistance > ensnareRadius && horizontalDistance < ensnareRadius + 5) {
							MovementUtils.pullTowardsNormalized(spawnerLocation, player, 0.8f, false);
						}
					}
				}

				new PPCircle(Particle.END_ROD, spawnerLocation.clone().add(0, 0, 0), ensnareRadius).countPerMeter(0.05).directionalMode(false).rotateDelta(true).spawnAsEnemy();
				new PPCircle(Particle.END_ROD, spawnerLocation.clone().add(0, 1, 0), ensnareRadius).countPerMeter(0.05).directionalMode(false).rotateDelta(true).spawnAsEnemy();
				new PPCircle(Particle.END_ROD, spawnerLocation.clone().add(0, 2, 0), ensnareRadius).countPerMeter(0.05).directionalMode(false).rotateDelta(true).spawnAsEnemy();
				new PPCircle(Particle.END_ROD, spawnerLocation.clone().add(0, 3, 0), ensnareRadius).countPerMeter(0.05).directionalMode(false).rotateDelta(true).spawnAsEnemy();
				new PPCircle(Particle.END_ROD, spawnerLocation.clone().add(0, 4, 0), ensnareRadius).countPerMeter(0.05).directionalMode(false).rotateDelta(true).spawnAsEnemy();
				new PPCircle(Particle.END_ROD, spawnerLocation.clone().add(0, 5, 0), ensnareRadius).countPerMeter(0.05).directionalMode(false).rotateDelta(true).spawnAsEnemy();
				new PPCircle(Particle.END_ROD, spawnerLocation.clone().add(0, 6, 0), ensnareRadius).countPerMeter(0.05).directionalMode(false).rotateDelta(true).spawnAsEnemy();
				mTicks++;
			}
		};
		task.runTaskTimer(Plugin.getInstance(), 0, 1);
		ensnarementTasks.put(block.getLocation(), task);
	}

	public static Location findValidCatSpawn(Location location, int catRadius) {
		// breadth first search algorithm to find the closest valid spot lol
		World world = location.getWorld();
		Queue<Location> queue = new ArrayDeque<>();
		Set<Location> visited = new HashSet<>();
		queue.add(location);
		visited.add(location);
		while (!queue.isEmpty()) {
			Location currentLocation = queue.poll();
			if (world.getBlockAt(currentLocation).getType() == Material.AIR) {
				int airCount = 0;
				for (BlockFace face : BlockFace.values()) {
					// check only horizontal faces
					if (face != BlockFace.UP && face != BlockFace.DOWN && face.isCartesian()) {
						if (world.getBlockAt(currentLocation.getBlock().getRelative(face).getLocation()).getType() == Material.AIR) {
							airCount++;
						}
					}
				}
				if (airCount >= 2) {
					return currentLocation;
				}
			}
			for (int x = -1; x <= 1; x++) {
				for (int y = -1; y <= 1; y++) {
					for (int z = -1; z <= 1; z++) {
						if (Math.abs(x) + Math.abs(y) + Math.abs(z) == 1) { // Only adjacent blocks
							Location newLocation = currentLocation.clone().add(x, y, z);
							if (!visited.contains(newLocation) && newLocation.distance(location) <= catRadius) {
								queue.add(newLocation);
								visited.add(newLocation);
							}
						}
					}
				}
			}
		}
		// fallback
		return location;
	}

	public static boolean checkSequence(Block spawnerBlock, int sequenceRadius) {
		// box to look for spawners
		boolean canBreak = true;
		Location center = spawnerBlock.getLocation();
		BoundingBox searchBox = new BoundingBox(
			center.getX() - sequenceRadius, center.getY() - sequenceRadius, center.getZ() - sequenceRadius,
			center.getX() + sequenceRadius, center.getY() + sequenceRadius, center.getZ() + sequenceRadius
		);
		for (int x = (int) searchBox.getMinX(); x <= searchBox.getMaxX(); x++) {
			for (int y = (int) searchBox.getMinY(); y <= searchBox.getMaxY(); y++) {
				for (int z = (int) searchBox.getMinZ(); z <= searchBox.getMaxZ(); z++) {
					Block block = center.getWorld().getBlockAt(x, y, z);
					if (block.getType() == Material.SPAWNER) {
						// spawner being broken needs to be the lowest number
						if (getSpawnerType(block, SEQUENCE_ATTRIBUTE) < getSpawnerType(spawnerBlock, SEQUENCE_ATTRIBUTE) && getSpawnerType(block, SEQUENCE_ATTRIBUTE) > 0) {
							PPLine line = new PPLine(Particle.ENCHANTMENT_TABLE, block.getLocation().clone().add(0.5, 0.5, 0.5), spawnerBlock.getLocation().clone().add(0.5, 0.5, 0.5));
							line.countPerMeter(10).spawnAsEnemy();
							spawnerBlock.getLocation().getNearbyPlayers(15).forEach(player ->
								ParticleUtils.drawSevenSegmentNumber(
									getSpawnerType(block, SEQUENCE_ATTRIBUTE), block.getLocation().clone().add(0.5, 2, 0.5),
									player, 0.65, 0.5, Particle.REDSTONE, new Particle.DustOptions(Color.MAROON, 1f)
								)
							);
							canBreak = false;

						}
					}
				}
			}
		}
		if (!canBreak) {
			spawnerBlock.getLocation().getNearbyPlayers(15).forEach(player ->
				ParticleUtils.drawSevenSegmentNumber(
					getSpawnerType(spawnerBlock, SEQUENCE_ATTRIBUTE), spawnerBlock.getLocation().clone().add(0.5, 2, 0.5),
					player, 0.65, 0.5, Particle.REDSTONE, new Particle.DustOptions(Color.MAROON, 1f)
				)
			);
		}
		return canBreak;
	}

}
