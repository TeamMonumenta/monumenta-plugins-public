package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.effects.AbilitySilence;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPRectPrism;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import io.papermc.paper.entity.LookAnchor;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ChanceCubes {

	public static final String DESCRIPTION = "Chance a break of fate.";
	private static final LoSPool COLOSSAL_LAND_POOL = new LoSPool.LibraryPool("~DelveColossalLand");
	private static final String POOL_NAME_NORMAL = "~TwistedNormal";

	public static Component[] rankDescription(int level) {
		return new Component[]{
			Component.text("Breaking a spawner rolls an effect die."),
			Component.text("This modifier is not compatible with Colossal or Entropy.")
		};
	}

	public static void applyModifiers(Location blockLoc, int level) {
		if (level == 0) {
			return;
		}
		Location loc = blockLoc.toCenterLocation();
		new PartialParticle(Particle.FLASH, loc).minimumCount(1).spawnAsEnemy();
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 30, 0.2, 0.2, 0.2, 0.1).spawnAsEnemy();

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0:
						int diceRoll = FastUtils.randomIntInRange(1, 10);
						diceRollEffects(loc, diceRoll);
						loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BANJO, SoundCategory.PLAYERS, 2, 0.85f);
						break;
					case 8:
						int diceRoll2 = FastUtils.randomIntInRange(1, 10);
						diceRollEffects(loc, diceRoll2);
						loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BANJO, SoundCategory.PLAYERS, 2, 1.15f);
						break;
					case 16:
						int diceRoll3 = FastUtils.randomIntInRange(1, 10);
						diceRollEffects(loc, diceRoll3);
						loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BANJO, SoundCategory.PLAYERS, 2, 1.45f);
						break;
					case 24:
						int diceRoll4 = FastUtils.randomIntInRange(1, 10);
						diceRollEffects(loc, diceRoll4);
						diceRollExecution(loc, diceRoll4);
						loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BANJO, SoundCategory.PLAYERS, 2, 1.75f);
						loc.getWorld().getPlayers().stream()
							.filter(player -> player.getLocation().distance(loc) <= 20)
							.forEach(player -> {
								Component chanceCubePart = Component.text("CHANCE CUBE! ")
									.color(NamedTextColor.GOLD)
									.decorate(TextDecoration.BOLD);
								Component diceRollPart = Component.text("The die lands on a ")
									.color(NamedTextColor.WHITE)
									.append(Component.text(diceRoll4)
										.color(TextColor.color(diceSideColour(diceRoll4).asRGB())))
									.append(Component.text("!")
										.color(NamedTextColor.WHITE));
								Component chanceCubeMessage = chanceCubePart.append(diceRollPart);
								player.sendMessage(chanceCubeMessage);
							});

						break;
					default:
						if (mTicks > 32) {
							this.cancel();
						}
						break;
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

	}

	private static void diceRollEffects(Location loc, int diceRoll) {
		loc.getNearbyPlayers(15).forEach(player ->
			ParticleUtils.drawSevenSegmentNumber(
				diceRoll, loc.clone().add(0, 2.5, 0),
				player, 0.65, 0.5, Particle.REDSTONE, new Particle.DustOptions(diceSideColour(diceRoll), 1f)
			)
		);
		new PPRectPrism(Particle.REDSTONE, loc.clone().add(-0.5, -0.5, -0.5), loc.clone().add(0.5, 0.5, 0.5))
			.countPerMeter(20).edgeMode(true).data(new Particle.DustOptions(diceSideColour(diceRoll), 0.75f)).spawnAsEnemy();
	}

	private static void diceRollExecution(Location loc, int diceRoll) {
		switch (diceRoll) {
			case 1 -> {
				BukkitRunnable task = new BukkitRunnable() {
					private static final int TIME_LIMIT_TICKS = 60 * 20;
					private int mTicks = 0;
					@Override
					public void run() {
						List<Entity> nearbyEntities = (List<Entity>) loc.getWorld().getNearbyEntities(loc, 8, 7, 8);
						boolean hasNearbyMobs = nearbyEntities.stream()
							.anyMatch(entity -> entity instanceof LivingEntity && !(entity instanceof Player) && !entity.isInvulnerable() && !(entity instanceof Villager) && !(entity instanceof ArmorStand) && entity.isValid());
						boolean hasPlayersInRange = loc.getWorld().getPlayers().stream()
							.anyMatch(player -> player.getLocation().distance(loc) <= 10 + 5);
						if (!hasNearbyMobs && mTicks == 0) {
							Horse horse = (Horse) loc.getWorld().spawnEntity(loc, EntityType.HORSE);
							horse.customName(Component.text("Juan"));
							horse.setCustomNameVisible(true);
							horse.addScoreboardTag("Hostile");
							Objects.requireNonNull(horse.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(50.0);
							horse.setHealth(50.0);
							nearbyEntities = (List<Entity>) loc.getWorld().getNearbyEntities(loc, 10, 10, 10);
							hasNearbyMobs = nearbyEntities.stream()
								.anyMatch(entity -> entity instanceof LivingEntity && !(entity instanceof Player) && !entity.isInvulnerable() && !(entity instanceof Villager) && !(entity instanceof ArmorStand) && entity.isValid());
						}
						nearbyEntities.stream()
							.filter(entity -> entity instanceof LivingEntity && !(entity instanceof Player) && !entity.isInvulnerable() && !(entity instanceof Villager) && !(entity instanceof ArmorStand) && entity.isValid())
							.forEach(entity -> {
								GlowingManager.startGlowing(entity, NamedTextColor.LIGHT_PURPLE, 200, GlowingManager.PLAYER_ABILITY_PRIORITY);
							});
						if (!hasNearbyMobs || !hasPlayersInRange || mTicks >= TIME_LIMIT_TICKS) {
							loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_DESTROY, SoundCategory.HOSTILE, 0.75f, 2f);
							List<Entity> nearbyEntitiesTwo = (List<Entity>) loc.getWorld().getNearbyEntities(loc, 20, 20, 20);
							nearbyEntitiesTwo.stream()
								.filter(entity -> entity instanceof LivingEntity && !(entity instanceof Player) && !entity.isInvulnerable() && !(entity instanceof Villager) && !(entity instanceof ArmorStand) && entity.isValid())
								.forEach(GlowingManager::clearAll);
							this.cancel();
						}
						for (Player player : loc.getWorld().getPlayers()) {
							if (player.getGameMode() == GameMode.SPECTATOR) {
								continue;
							}
							Location playerLoc = player.getLocation().clone();
							playerLoc.setY(loc.getY());
							double horizontalDistance = playerLoc.distance(loc);
							if (horizontalDistance > 10 && horizontalDistance < 10 + 5) {
								MovementUtils.pullTowardsNormalized(loc, player, 0.8f, false);
							}
						}

						new PPCircle(Particle.END_ROD, loc.clone().add(0, 0, 0), 10).countPerMeter(0.05).directionalMode(false).rotateDelta(true).spawnAsEnemy();
						new PPCircle(Particle.END_ROD, loc.clone().add(0, 1, 0), 10).countPerMeter(0.05).directionalMode(false).rotateDelta(true).spawnAsEnemy();
						new PPCircle(Particle.END_ROD, loc.clone().add(0, 2, 0), 10).countPerMeter(0.05).directionalMode(false).rotateDelta(true).spawnAsEnemy();
						new PPCircle(Particle.END_ROD, loc.clone().add(0, 3, 0), 10).countPerMeter(0.05).directionalMode(false).rotateDelta(true).spawnAsEnemy();
						new PPCircle(Particle.END_ROD, loc.clone().add(0, 4, 0), 10).countPerMeter(0.05).directionalMode(false).rotateDelta(true).spawnAsEnemy();
						new PPCircle(Particle.END_ROD, loc.clone().add(0, 5, 0), 10).countPerMeter(0.05).directionalMode(false).rotateDelta(true).spawnAsEnemy();
						new PPCircle(Particle.END_ROD, loc.clone().add(0, 6, 0), 10).countPerMeter(0.05).directionalMode(false).rotateDelta(true).spawnAsEnemy();
						mTicks++;
					}
				};
				task.runTaskTimer(Plugin.getInstance(), 0, 1);
			}
			case 2 -> {
				loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 0.2f, 0.1f);
				loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.HOSTILE, 0.6f, 0.1f);
				loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.HOSTILE, 0.4f, 0.1f);
				COLOSSAL_LAND_POOL.spawn(loc);
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 0.2f, 0.1f);
					loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.HOSTILE, 0.6f, 0.1f);
					loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.HOSTILE, 0.4f, 0.1f);
					COLOSSAL_LAND_POOL.spawn(loc);
				}, 20);
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 0.2f, 0.1f);
					loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.HOSTILE, 0.6f, 0.1f);
					loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.HOSTILE, 0.4f, 0.1f);
					COLOSSAL_LAND_POOL.spawn(loc);
				}, 40);
			}
			case 3 -> {
					PufferFish pufferfish = (PufferFish) loc.getWorld().spawnEntity(loc, EntityType.PUFFERFISH);
					Objects.requireNonNull(pufferfish.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(150.0);
					pufferfish.setHealth(150.0);
					PufferFish pufferfish2 = (PufferFish) loc.getWorld().spawnEntity(loc, EntityType.PUFFERFISH);
					Objects.requireNonNull(pufferfish2.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(75.0);
					pufferfish2.setHealth(75.0);
					PufferFish pufferfish3 = (PufferFish) loc.getWorld().spawnEntity(loc, EntityType.PUFFERFISH);
					Objects.requireNonNull(pufferfish3.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(75.0);
					pufferfish3.setHealth(75.0);
					PufferFish pufferfish4 = (PufferFish) loc.getWorld().spawnEntity(loc, EntityType.PUFFERFISH);
					Objects.requireNonNull(pufferfish4.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(75.0);
					pufferfish4.setHealth(75.0);

					loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 0.1f, 0.2f);
					loc.getWorld().playSound(loc, Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 0.3f, 0.1f);
					loc.getWorld().playSound(loc, Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 1.2f, 0.1f);
					loc.getWorld().playSound(loc, Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.HOSTILE, 1.4f, 0.3f);
					loc.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.HOSTILE, 1.6f, 0.1f);
					loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 5f, 0.1f);

					Map<Soul, Integer> mobsPool = LibraryOfSoulsIntegration.getPool(POOL_NAME_NORMAL);
					if (mobsPool != null && !mobsPool.isEmpty()) {
						List<Soul> twistedMobs = new ArrayList<>(mobsPool.keySet());
						Soul randomTwisted = twistedMobs.get(FastUtils.RANDOM.nextInt(twistedMobs.size()));
						LivingEntity twistedMob = (LivingEntity) randomTwisted.summon(loc);
						if (twistedMob != null) {
							twistedMob.setInvulnerable(true);
							twistedMob.setGravity(false);
							pufferfish.addPassenger(twistedMob);
							Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
								twistedMob.setGravity(true);
								twistedMob.setInvulnerable(false);
								Objects.requireNonNull(twistedMob.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(twistedMob.getHealth());
								twistedMob.setHealth(Objects.requireNonNull(twistedMob.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue() / 1.9);
							}, 20 * 2 / 2);
						}
					}
			}
			case 4 -> {
				List<Player> nearbyPlayers = (List<Player>) loc.getNearbyPlayers(20);
				for (Player player : nearbyPlayers) {
					loc.getWorld().playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, SoundCategory.HOSTILE, 2f, 0.8f);
					loc.getWorld().playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, SoundCategory.HOSTILE, 2f, 0.8f);
					loc.getWorld().playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, SoundCategory.HOSTILE, 2f, 0.8f);
					Location spawnLocation = player.getLocation().add(0, 7, 0);
					int randomChoice = FastUtils.randomIntInRange(1, 12);
					if (randomChoice > 6) {
						TNTPrimed tnt = (TNTPrimed) player.getWorld().spawnEntity(spawnLocation, EntityType.PRIMED_TNT);
						tnt.setFuseTicks(40);
					} else if (randomChoice == 1) {
						FallingBlock fallingAnvil = player.getWorld().spawnFallingBlock(spawnLocation, Material.ANVIL.createBlockData());
						fallingAnvil.setDamagePerBlock(3F);
					} else {
						BlockData dripstoneData = Material.POINTED_DRIPSTONE.createBlockData();
						if (dripstoneData instanceof org.bukkit.block.data.type.PointedDripstone pointedDripstone) {
							pointedDripstone.setVerticalDirection(BlockFace.DOWN);
						}
						FallingBlock fallingDripstone = player.getWorld().spawnFallingBlock(spawnLocation, dripstoneData);
						fallingDripstone.setDropItem(false);
						fallingDripstone.setDamagePerBlock(3F);
					}
				}
			}
			case 5 -> {
				List<Player> nearbyPlayers = (List<Player>) loc.getNearbyPlayers(15);
				Map<Player, Entity> lockedEnemies = new HashMap<>();
				for (Player player : nearbyPlayers) {
					if (player.getGameMode() == GameMode.SPECTATOR) {
						continue;
					}
					if (lockedEnemies.containsKey(player)) {
						continue;
					}
					List<Entity> potentialEnemies = (List<Entity>) loc.getWorld().getNearbyEntities(player.getLocation(), 7, 7, 7);
					Entity randomEnemy = potentialEnemies.stream()
						.filter(entity -> entity instanceof LivingEntity &&
							!(entity instanceof Player) &&
							!(entity instanceof Villager) &&
							!(entity instanceof ArmorStand) &&
							!entity.isInvulnerable() &&
							entity.isValid() &&
							EntityUtils.isHostileMob(entity) &&
							player.hasLineOfSight(entity))
						.findAny()
						.orElse(null);
					if (randomEnemy == null) {
						Horse horse = (Horse) loc.getWorld().spawnEntity(loc, EntityType.HORSE);
						horse.customName(Component.text("Juan"));
						horse.setCustomNameVisible(true);
						horse.addScoreboardTag("Hostile");
						Objects.requireNonNull(horse.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(50.0);
						horse.setHealth(50.0);
						randomEnemy = horse;
					}
					lockedEnemies.put(player, randomEnemy);
					loc.getWorld().playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1f, 0.75f);
					Entity finalRandomEnemy = randomEnemy;
					new BukkitRunnable() {
						@Override
						public void run() {
							if (!finalRandomEnemy.isValid() || finalRandomEnemy.isDead() || !player.isOnline() || player.getLocation().distance(finalRandomEnemy.getLocation()) > 20) {
								lockedEnemies.remove(player);
								this.cancel();
								return;
							}

							Location enemyLoc = finalRandomEnemy.getLocation();
							PPLine line = new PPLine(Particle.CRIT, player.getLocation(), enemyLoc);
							line.countPerMeter(10).spawnAsEnemy();

							player.lookAt(finalRandomEnemy, LookAnchor.EYES, LookAnchor.EYES);
						}
					}.runTaskTimer(Plugin.getInstance(), 0, 15);
				}
			}

			case 6 -> {
				int range = 10;
				int radius = 5;
				EnumSet<Material> ignoredMats = EnumSet.of(
					Material.AIR,
					Material.CAVE_AIR,
					Material.VOID_AIR,
					Material.COMMAND_BLOCK,
					Material.CHAIN_COMMAND_BLOCK,
					Material.REPEATING_COMMAND_BLOCK,
					Material.BARRIER,
					Material.BEDROCK,
					Material.OBSIDIAN,
					Material.CHEST,
					Material.SPAWNER
				);

				List<Player> players = PlayerUtils.playersInRange(loc, range, true);
				if (!players.isEmpty()) {
					Player target = players.get(FastUtils.RANDOM.nextInt(players.size()));
					final int PHASE1_TICKS = 20;
					final int PHASE2_TICKS = 300;
					new BukkitRunnable() {
						int mTicks = 0;
						final List<BlockState> mBlocksToRestore = new ArrayList<>();
						Material mMaterial = Material.COBWEB;
						int mEffectDuration;

						@Override
						public void run() {
							if (mTicks > 0 && mTicks < PHASE2_TICKS) {
								for (BlockState state : mBlocksToRestore) {
									Location loc = state.getLocation().add(0.5f, 1f, 0.5f);
									if (FastUtils.RANDOM.nextInt(100) < 50) {
										new PartialParticle(Particle.ASH, loc, 1, 0.3, 0.3, 0.3, 0).spawnAsEnemy();
									}
								}
							}
							if (mTicks == 0) {
								target.playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1f, 4f);
								loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1f, 5f);
								target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 30, 4));
								if (FastUtils.RANDOM.nextBoolean()) {
									mMaterial = Material.LAVA;
								}
								mEffectDuration = PHASE2_TICKS;
								int playerY = target.getLocation().getBlockY();
								for (int dx = -radius; dx < radius; dx++) {
									for (int dz = -radius; dz < radius; dz++) {
										for (int dy = 0; dy <= 1; dy++) {
											Location blockLocation = target.getLocation().add(dx, 0, dz);
											blockLocation.setY(playerY - dy);
											BlockState state = blockLocation.getBlock().getState();
											if (!ignoredMats.contains(state.getType()) &&
												!state.getType().isInteractable() &&
												FastUtils.RANDOM.nextInt(100) < 37) {
												mBlocksToRestore.add(state);
											}
										}
									}
								}
							} else if (mTicks == PHASE1_TICKS) {
								for (BlockState state : mBlocksToRestore) {
									state.getLocation().getBlock().setType(mMaterial);
								}
							} else if (mTicks == mEffectDuration) {
								for (BlockState state : mBlocksToRestore) {
									state.update(true);
								}
							} else if (mTicks > mEffectDuration) {
								this.cancel();
							}
							mTicks++;
						}
					}.runTaskTimer(Plugin.getInstance(), 0, 1);
				}
			}
			case 7 -> {
				List<Player> nearbyPlayers = (List<Player>) loc.getNearbyPlayers(15);
				for (Player player : nearbyPlayers) {
					loc.getWorld().playSound(loc, Sound.BLOCK_BREWING_STAND_BREW, SoundCategory.HOSTILE, 1f, 1f);
					int potionDice = FastUtils.randomIntInRange(1, 12);
					switch (potionDice) {
						case 1 -> {
							Plugin.getInstance().mEffectManager.addEffect(player, "gambleEffect", new PercentSpeed(300, 0.3, "PercentSpeed"));
						}
						case 2 -> {
							Plugin.getInstance().mEffectManager.addEffect(player, "gambleEffect", new PercentDamageDealt(300, 0.3));
						}
						case 3 -> {
							Plugin.getInstance().mEffectManager.addEffect(player, "gambleEffect", new CustomRegeneration(300, 0.3, Plugin.getInstance()));
						}
						case 4, 11, 12 -> {
							player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 600, 2));
						}
						case 5 -> {
							player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 400, 0));
						}
						case 6 -> {
							player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 600, 0));
						}
						case 7 -> {
							player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 100, 0));
						}
						case 8 -> {
							player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 600, 2));
						}
						case 9 -> {
							player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 600, 2));
						}
						case 10 -> {
							Plugin.getInstance().mEffectManager.addEffect(player, "gambleEffect", new AbilitySilence(200));
						}
						default -> {
						}
					}
				}
			}
			case 8 -> {
				List<Entity> potentialEnemies = (List<Entity>) loc.getWorld().getNearbyEntities(loc, 15, 15, 15);
				List<LivingEntity> validEntities = potentialEnemies.stream()
					.filter(entity -> entity instanceof LivingEntity &&
						!(entity instanceof Villager) &&
						!(entity instanceof ArmorStand) &&
						entity.isValid())
					.map(entity -> (LivingEntity) entity)
					.toList();
				for (LivingEntity entity : validEntities) {
					loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1f, 1.5f);

					new PPCircle(Particle.CLOUD, entity.getLocation(), 1.2).countPerMeter(1).directionalMode(false).spawnAsEnemy();
					entity.setVelocity(new Vector(FastUtils.randomFloatInRange(-1.3F, 1.3F), 1.3, FastUtils.randomFloatInRange(-1.3F, 1.3F)));
				}
			}
			case 9 -> {
				BukkitRunnable task = new BukkitRunnable() {
					private int mSpawnCount = 0;
					@Override
					public void run() {
						if (mSpawnCount >= 15) {
							this.cancel();
							return;
						}
						loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.HOSTILE, 2f, 1f);
						loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.HOSTILE, 2f, 1f);
						loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.HOSTILE, 2f, 1f);
						loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.HOSTILE, 2f, 1f);
						loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.HOSTILE, 2f, 1f);
						ExperienceOrb xpOrb = loc.getWorld().spawn(loc.clone().add(0, 0.5, 0), ExperienceOrb.class, orb -> orb.setExperience(18));
						xpOrb.setVelocity(new Vector(FastUtils.randomFloatInRange(-0.5F, 0.5F), 0.5, FastUtils.randomFloatInRange(-0.5F, 0.5F)));
						mSpawnCount++;
					}
				};
				task.runTaskTimer(Plugin.getInstance(), 0, 5);
			}
			case 10 -> {
				List<Player> nearbyPlayers = (List<Player>) loc.getNearbyPlayers(7);
				for (Player player : nearbyPlayers) {
					PlayerUtils.healPlayer(Plugin.getInstance(), player, EntityUtils.getMaxHealth(player), player);
					AbsorptionUtils.addAbsorption(player, 6, 6, 300);
					Location pLoc = player.getLocation();
					new PartialParticle(Particle.HEART, pLoc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(player);
					new PartialParticle(Particle.END_ROD, pLoc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(player);
					pLoc.getWorld().playSound(pLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 2.0f, 1.6f);
					pLoc.getWorld().playSound(pLoc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.05f, 1.0f);
				}
			}
			default -> {
			}
		}
	}

	private static Color diceSideColour(int dice) {
		return switch (dice) {
			case 1 -> Color.RED;
			case 2 -> Color.ORANGE;
			case 3 -> Color.YELLOW;
			case 4 -> Color.LIME;
			case 5 -> Color.GREEN;
			case 6 -> Color.AQUA;
			case 7 -> Color.BLUE;
			case 8 -> Color.NAVY;
			case 9 -> Color.PURPLE;
			case 10 -> Color.FUCHSIA;
			default -> Color.WHITE;
		};
	}
}
