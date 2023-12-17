package com.playmonumenta.plugins.depths;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.VanityManager;
import com.playmonumenta.plugins.delves.DelvesModifier;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.Enlightenment;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.Sundrops;
import com.playmonumenta.plugins.depths.abilities.earthbound.EarthenWrath;
import com.playmonumenta.plugins.depths.abilities.prismatic.Charity;
import com.playmonumenta.plugins.depths.abilities.prismatic.ColorSplash;
import com.playmonumenta.plugins.depths.abilities.steelsage.FireworkBlast;
import com.playmonumenta.plugins.depths.bosses.Broodmother;
import com.playmonumenta.plugins.depths.bosses.Callicarpa;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ExperienceUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import com.playmonumenta.scriptedquests.managers.SongManager;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class DepthsListener implements Listener {
	public static final String GRAVE_TAG = "DepthsGrave";
	private static final int GRAVE_DURATION = 20 * 20;
	private static final int ASCENSION_GRAVE_DURATION_DECREASE = 5 * 20;
	private static final int GRAVE_REVIVE_DURATION = 3 * 20;
	private static final int DISCONNECT_ANTICHEESE_RADIUS = 6;

	public DepthsListener() {
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		Player player = event.getPlayer();
		DepthsManager dm = DepthsManager.getInstance();

		if (!dm.isInSystem(player)) {
			return;
		}

		Block block = event.getBlock();
		Material type = block.getType();

		//Spawner break message
		if (type == Material.SPAWNER) {
			dm.playerBrokeSpawner(player, block.getLocation());

			DepthsParty party = dm.getDepthsParty(player);
			if (party == null) {
				return;
			}

			List<Sundrops> sundrops = party.getPlayers().stream().map(p -> Plugin.getInstance().mAbilityManager.getPlayerAbilityIgnoringSilence(p, Sundrops.class)).filter(Objects::nonNull).toList();
			Map<Sundrops, Double> weights = new HashMap<>();
			for (Sundrops ability : sundrops) {
				weights.put(ability, ability.getChance());
			}
			double total = weights.values().stream().mapToDouble(c -> c).sum();
			if (total > 1) {
				for (Sundrops ability : sundrops) {
					weights.put(ability, ability.getChance() / total);
				}
			}
			double roll = FastUtils.RANDOM.nextDouble();
			if (roll <= total) {
				for (Map.Entry<Sundrops, Double> weight : weights.entrySet()) {
					double chance = weight.getValue();
					if (roll <= chance) {
						weight.getKey().summonSundrop(block);
						break;
					} else {
						roll -= chance;
					}
				}
			}
		} else if (type == Material.CHEST) {
			//Player's can't break chests themselves
			event.setCancelled(true);
		}
	}

	//Enlightenment ability logic
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerExpChangeEvent(PlayerExpChangeEvent event) {
		Player player = event.getPlayer();
		DepthsManager dm = DepthsManager.getInstance();

		DepthsParty party = dm.getDepthsParty(player);
		if (party == null) {
			return;
		}

		double xpFactor = party.mPlayersInParty.stream()
			.map(DepthsPlayer::getPlayer)
			.map(p -> AbilityManager.getManager().getPlayerAbility(p, Enlightenment.class))
			.filter(Objects::nonNull)
			.mapToDouble(Enlightenment::getXPMultiplier)
			.max().orElse(1);

		if (party.mEndlessMode || party.getAscension() > 0) {
			xpFactor *= 0.5;
		}

		if (xpFactor != 1) {
			event.setAmount((int) Math.round(event.getAmount() * xpFactor));
		}
	}

	//Logic to replace chest opening with a ability selection gui, if applicable
	@EventHandler(ignoreCancelled = true)
	public void onInventoryOpenEvent(InventoryOpenEvent e) {
		Player p = (Player) e.getPlayer();
		if (e.getInventory().getHolder() instanceof Chest || e.getInventory().getHolder() instanceof DoubleChest) {
			if (DepthsManager.getInstance().getRoomReward(p, e.getInventory().getLocation(), false)) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void damageEvent(DamageEvent event) {
		LivingEntity damagee = event.getDamagee();
		LivingEntity source = event.getSource();

		if (damagee instanceof Player player) {
			DepthsPlayer dp = DepthsManager.getInstance().mPlayers.get(player.getUniqueId());
			DepthsParty party = DepthsManager.getInstance().getPartyFromId(dp);
			if (party == null) {
				return;
			}
			//If the player is in the revive room, make them immune to damage
			if (dp != null && dp.mGraveRunnable != null) {
				event.setCancelled(true);
				return;
			}

			// Handle earthen wrath damage
			EarthenWrath.handleDamageEvent(event, player, party);

			// Extra damage taken at higher floors
			int floor = party.getFloor();
			if (floor > 15) {
				double multiplier = 1 + (0.1 * (((floor - 1) / 3) - 4));
			    event.setDamage(event.getDamage() * multiplier);
			}
			if (source != null && EntityUtils.isBoss(source) && floor > 3) {
				double multiplier = 1 + (0.05 * ((floor - 1) / 3));
			    event.setDamage(event.getDamage() * multiplier);
			}
		}

		ClassAbility ability = event.getAbility();
		DamageEvent.DamageType type = event.getType();
		if (ability != null && !ability.isFake() && type != DamageEvent.DamageType.TRUE && type != DamageEvent.DamageType.OTHER) {
			event.setDamage(event.getDamage() * DepthsUtils.getDamageMultiplier());
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void projectileHitEvent(ProjectileHitEvent event) {
		Projectile proj = event.getEntity();
		ProjectileSource shooter = proj.getShooter();
		Entity damagee = event.getHitEntity();
		if (proj instanceof Firework firework && FireworkBlast.isDamaging(firework) && damagee instanceof Player) {
			// Firework Blast fireworks go through players
			event.setCancelled(true);
		} else if (damagee instanceof Slime && damagee.getName().contains("Eye") && shooter instanceof Player player) {
			// Sound on shooting an eye
			player.playSound(player.getLocation(), Sound.ENTITY_BAT_HURT, SoundCategory.PLAYERS, 0.4f, 0.2f);
		}
	}

	public int getGraveDuration(DepthsParty party, DepthsPlayer dp, boolean allowPermadeath) {
		int baseGraveDuration = party.getAscension() < DepthsEndlessDifficulty.ASCENSION_REVIVE_TIME ? GRAVE_DURATION : GRAVE_DURATION - ASCENSION_GRAVE_DURATION_DECREASE;
		int duration = baseGraveDuration - (int) (Math.sqrt(dp.mNumDeaths) * 7 * 20);

		if (allowPermadeath) {
			return Math.max(1, duration);
		}
		return Math.max(61, duration);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerDeathEvent(PlayerDeathEvent event) {
		DepthsManager dm = DepthsManager.getInstance();
		Player player = event.getPlayer();
		DepthsPlayer dp = dm.getDepthsPlayer(player);

		if (dp != null) {
			if (dp.mGraveRunnable != null) {
				// Died in death waiting room (e.g. used the exit button): send player to loot room
				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
					if (dp.mGraveRunnable != null) {
						dp.mGraveRunnable.cancel();
					}
					sendPlayerToLootRoom(player, true);
				});
				event.setCancelled(true);
				return;
			}

			DepthsParty party = dm.getPartyFromId(dp);

			if (party != null) {

				// Check how many players on team don't have an active grave. If it's less than 1 (current player), we can skip and send to loot room
				int partyAliveCount = 0;
				for (DepthsPlayer depthsPlayer : party.mPlayersInParty) {
					if (depthsPlayer.mGraveRunnable == null) {
						partyAliveCount++;
					}
				}

				if (party.mDeathWaitingRoomPoint != null && partyAliveCount > 1) {
					// If a death waiting room exists (DD2), send the player there and spawn a grave that allows others to revive the player

					event.setCancelled(true);

					// Handle death message, as we cancel the event
					Component deathMessage = event.deathMessage();
					if (deathMessage != null) {
						if (ScoreboardUtils.getScoreboardValue(player, Constants.SCOREBOARD_DEATH_MESSAGE).orElse(0) != 0) {
							player.sendMessage(deathMessage);
							player.sendMessage(Component.text("Only you saw this message. Change this with /deathmsg", NamedTextColor.AQUA));
						} else {
							for (Player p : player.getWorld().getPlayers()) {
								p.sendMessage(deathMessage);
							}
						}
					}

					Location deathLocation = player.getLocation();
					dp.mNumDeaths++;
					int graveDuration = getGraveDuration(party, dp, false);
					if (graveDuration > GRAVE_REVIVE_DURATION) {
						Location waitingRoomLocation = party.mDeathWaitingRoomPoint.toLocation(player.getWorld());
						Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> player.teleport(waitingRoomLocation));
						MessagingUtils.sendTitle(player, Component.text("You Died", NamedTextColor.RED),
							Component.text("Please wait to be revived!", NamedTextColor.RED));

						ArmorStand grave = player.getWorld().spawn(deathLocation, ArmorStand.class);
						grave.setInvulnerable(true);
						//grave.setDisabledSlots(EquipmentSlot.values());
						grave.setCollidable(false);
						grave.setBasePlate(false);
						grave.customName(Component.text(player.getName() + "'s Grave", NamedTextColor.RED));
						grave.setCustomNameVisible(true);
						grave.setGlowing(true);
						grave.setArms(true);
						grave.setGravity(true);
						ScoreboardUtils.addEntityToTeam(grave, "GraveGreen", NamedTextColor.GREEN);
						grave.addDisabledSlots(EquipmentSlot.values());
						grave.addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.REMOVING_OR_CHANGING);
						grave.addEquipmentLock(EquipmentSlot.CHEST, ArmorStand.LockType.REMOVING_OR_CHANGING);
						grave.addEquipmentLock(EquipmentSlot.LEGS, ArmorStand.LockType.REMOVING_OR_CHANGING);
						grave.addEquipmentLock(EquipmentSlot.FEET, ArmorStand.LockType.REMOVING_OR_CHANGING);
						grave.addEquipmentLock(EquipmentSlot.HAND, ArmorStand.LockType.REMOVING_OR_CHANGING);
						grave.addEquipmentLock(EquipmentSlot.OFF_HAND, ArmorStand.LockType.REMOVING_OR_CHANGING);
						grave.addScoreboardTag(GRAVE_TAG);

						VanityManager.VanityData vanityData = Plugin.getInstance().mVanityManager.getData(player);
						for (EquipmentSlot slot : EquipmentSlot.values()) {
							ItemStack item;
							if (slot != EquipmentSlot.HEAD) {
								item = ItemUtils.clone(player.getEquipment().getItem(slot));
								if (ItemUtils.isNullOrAir(item)) {
									continue;
								}
								VanityManager.applyVanity(item, vanityData, slot, false);
							} else {
								item = new ItemStack(Material.PLAYER_HEAD);
								if (item.getItemMeta() instanceof SkullMeta skullMeta) {
									skullMeta.setOwningPlayer(player);
									item.setItemMeta(skullMeta);
								}
							}
							grave.setItem(slot, item);
						}

						BossBar graveBar = BossBar.bossBar(Component.text(player.getName() + "'s Grave", NamedTextColor.RED),
							1, BossBar.Color.RED, BossBar.Overlay.PROGRESS, Set.of());

						for (Player p : player.getWorld().getPlayers()) {
							p.showBossBar(graveBar);
						}
						dp.mDead = true;
						dp.mGraveTicks = 0;
						dp.mReviveTicks = 0;
						dp.mCurrentlyReviving = false;
						dp.mGraveRunnable = new BukkitRunnable() {
							final double mBaseReviveRadiusSquared = 2;
							@Nullable BossBar mReviveBar;

							@Override
							public void run() {
								dp.mGraveTicks++;
								if (!player.isOnline()
									    || !player.getWorld().equals(grave.getWorld())
									    || player.getLocation().toVector().distanceSquared(party.mDeathWaitingRoomPoint) > 20 * 20) {
									// Player logged off, somehow switched worlds, or exited the waiting room:
									cancel();
									return;
								}

								ArrayList<Player> nearbyPlayers = new ArrayList<>(party.mPlayersInParty.stream()
									.map(DepthsPlayer::getPlayer)
									.filter(Objects::nonNull)
									.filter(p -> p.getLocation().distanceSquared(grave.getLocation()) <= mBaseReviveRadiusSquared + Math.pow(ColorSplash.DAWNBRINGER_EXTRA_REVIVE_RADIUS, 2)).toList());

								nearbyPlayers.removeIf(player -> {
									ColorSplash colorSplash = AbilityManager.getManager().getPlayerAbility(player, ColorSplash.class);
									if (colorSplash == null || !colorSplash.hasIncreasedReviveRadius()) {
										return player.getLocation().distanceSquared(grave.getLocation()) > mBaseReviveRadiusSquared;
									}
									return false;
								});

								if (!nearbyPlayers.isEmpty()) {
									int maxCharityLevel = nearbyPlayers.stream().map(DepthsManager.getInstance()::getDepthsPlayer).filter(Objects::nonNull).mapToInt(dp -> dp.mAbilities.getOrDefault(Charity.ABILITY_NAME, 0)).max().orElse(0);

									dp.mCurrentlyReviving = true;
									if (maxCharityLevel > 0) {
										dp.mReviveTicks += Charity.REVIVE_POWER[maxCharityLevel - 1];
									} else {
										dp.mReviveTicks++;
									}
									if (dp.mReviveTicks > 3 && mReviveBar == null) {
										mReviveBar = BossBar.bossBar(Component.text("Reviving " + player.getName(), NamedTextColor.GREEN),
											1f * (int) dp.mReviveTicks / GRAVE_REVIVE_DURATION, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS, Set.of());
										for (Player p : player.getWorld().getPlayers()) {
											p.showBossBar(mReviveBar);
										}
									}
									if (dp.mReviveTicks >= GRAVE_REVIVE_DURATION) {
										// Successfully revived the player
										Plugin.getInstance().mEffectManager.addEffect(player, "ZenithReviveResistance", new PercentDamageReceived(20, -100));
										player.teleport(grave.getLocation());
										Charity.onCharityRevive(player, nearbyPlayers, maxCharityLevel);
										dp.sendMessage("You have been revived!");
										party.sendMessage(player.getName() + " has been revived!", d -> d != dp);
										dp.mDead = false;
										cancel();
										return;
									}
								} else {
									dp.mCurrentlyReviving = false;
									dp.mReviveTicks = Math.max(0, dp.mReviveTicks - 3);
									if (dp.mReviveTicks <= 0) {
										if (mReviveBar != null) {
											for (Player p : player.getWorld().getPlayers()) {
												p.hideBossBar(mReviveBar);
											}
											mReviveBar = null;
										}
									}
								}
								if (dp.mGraveTicks > graveDuration && !dp.mCurrentlyReviving) {
									cancel();
									sendPlayerToLootRoom(player, true);
									return;
								}
								float remaining = 1 - 1f * dp.mGraveTicks / graveDuration;
								float oldRemaining = graveBar.progress();
								graveBar.progress(remaining >= 0 ? remaining : 0);
								int durationRemaining = Math.max(0, (graveDuration - dp.mGraveTicks) / 20);
								TextComponent titleName = Component.text(player.getName() + "'s Grave | " + durationRemaining + "s", NamedTextColor.RED);
								graveBar.name(titleName);
								grave.customName(titleName);
								if (mReviveBar != null) {
									mReviveBar.progress(1f * (int) dp.mReviveTicks / GRAVE_REVIVE_DURATION);
								}

								if (oldRemaining > 0.66 && remaining <= 0.66) {
									ScoreboardUtils.addEntityToTeam(grave, "GraveYellow", NamedTextColor.YELLOW);
								} else if (oldRemaining > 0.33 && remaining <= 0.33) {
									ScoreboardUtils.addEntityToTeam(grave, "GraveRed", NamedTextColor.RED);
								}
							}

							@Override
							public synchronized void cancel() throws IllegalStateException {
								if (!isCancelled()) {
									grave.remove();
									for (Player p : player.getWorld().getPlayers()) {
										p.hideBossBar(graveBar);
										if (mReviveBar != null) {
											p.hideBossBar(mReviveBar);
										}
									}
									dp.mGraveRunnable = null;
								}
								super.cancel();
							}
						};
						dp.mGraveRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
					} else {
						// died too often: immediately send to loot room
						dp.sendMessage("You have died too often and have been sent directly to the loot room!");
						Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> sendPlayerToLootRoom(player, true));
					}
				} else {
					// else send to loot room on death
					//Set treasure score at death time, so they can't just wait around in death screen for party to get more rewards
					dp.mFinalTreasureScore = party.mTreasureScore;
					dp.setDeathRoom(party.getRoomNumber());
					dp.sendMessage("You have died! Your final treasure score is " + dp.mFinalTreasureScore + "!");
					dp.sendMessage("You reached room " + party.mRoomNumber + "!");

					if (!party.mEndlessMode && party.mAscension == 0) {
						event.setKeepLevel(false);
						event.setDroppedExp(0);
						int keptXp = (int) (0.5 * ExperienceUtils.getTotalExperience(player));
						int keptLevel = ExperienceUtils.getLevel(keptXp);
						event.setNewLevel(keptLevel);
						event.setNewExp(keptXp - ExperienceUtils.getTotalExperience(keptLevel));
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		DepthsManager dm = DepthsManager.getInstance();
		DepthsPlayer dp = dm.getDepthsPlayer(player);

		if (
			dp != null &&
			!Plugin.getInstance().mPlayerListener.isPlayerTransferring(player) &&
			event.getReason() == PlayerQuitEvent.QuitReason.DISCONNECTED
		) {
			if (dp.getContent() == DepthsContent.DARKEST_DEPTHS) {
				return;
			}

			// Check if in active bossfight
			List<String> applicableBossTags = List.of(Callicarpa.identityTag, Broodmother.identityTag, Vesperidys.identityTag);
			boolean nearZenithBoss = new Hitbox.SphereHitbox(player.getLocation(), 150).getHitMobs().stream()
				.anyMatch(living -> {
					Set<String> scoreboardTags = living.getScoreboardTags();
					return applicableBossTags.stream().anyMatch(scoreboardTags::contains);
				});

			if (nearZenithBoss) {
				dp.mNumDeaths += 2;
			} else if (EntityUtils.getNearestHostile(player.getLocation(), DISCONNECT_ANTICHEESE_RADIUS) != null) {
				dp.mNumDeaths++;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerPostRespawnEvent(PlayerPostRespawnEvent event) {
		//Tp player to loot room when they respawn
		sendPlayerToLootRoom(event.getPlayer(), false);
	}

	private void sendPlayerToLootRoom(Player player, boolean setDeathStats) {
		DepthsManager dm = DepthsManager.getInstance();
		DepthsPlayer dp = dm.getDepthsPlayer(player);
		if (dp != null) {
			DepthsParty party = dm.getPartyFromId(dp);
			if (party != null) {
				if (setDeathStats) {
					dp.mFinalTreasureScore = party.mTreasureScore;
					dp.setDeathRoom(party.getRoomNumber());
					dp.sendMessage("You have died! Your final treasure score is " + dp.mFinalTreasureScore + "!");
					dp.sendMessage("You reached room " + party.mRoomNumber + "!");

					if (!party.mEndlessMode) {
						ExperienceUtils.setTotalExperience(player, (int) (0.5 * ExperienceUtils.getTotalExperience(player)));
					}
				}
				boolean victory = party.mEndlessMode && party.mRoomNumber > 30;
				DepthsUtils.storeRunStatsToFile(dp, Plugin.getInstance().getDataFolder() + File.separator + "DepthsStats", victory); //Save the player's stats
				party.populateLootRoom(player, victory);
			}
		}
		SongManager.stopSong(player, true);
	}

	//Save player data on logout or shard crash, to be loaded on startup later
	@EventHandler(ignoreCancelled = true)
	public void playerSave(PlayerSaveEvent event) {

		Player player = event.getPlayer();
		//If they are in the system with an access score of zero, remove them from the system
		if ((ScoreboardUtils.getScoreboardValue(player, DepthsManager.DEPTHS_ACCESS).orElse(0) == 0) && (ScoreboardUtils.getScoreboardValue(player, DepthsManager.ZENITH_ACCESS).orElse(0) == 0)) {
			DepthsManager.getInstance().deletePlayer(player);
		}

		DepthsManager.getInstance().save(Plugin.getInstance().getDataFolder() + File.separator + "depths");
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		//Call util to check for ice barrier slow
		DepthsUtils.explodeEvent(event);
		Entity exploder = event.getEntity();
		if (EntityUtils.isBoss(exploder)) {
			return;
		}

		// Only remove 1 chest from the list
		Block removedChest = null;
		for (Block block : event.blockList()) {
			if (block.getType() == Material.CHEST) {
				removedChest = block;
				break;
			}
		}
		if (removedChest != null) {
			event.blockList().remove(removedChest);
		}
		event.blockList().removeIf(block -> block.getType().equals(Material.STONE_BUTTON) && block.getRelative(BlockFace.EAST).getType() == Material.OBSIDIAN);
		if (exploder instanceof TNTPrimed) {
			event.blockList().removeIf(block -> block.getType().equals(Material.SPAWNER));
		} else if (exploder instanceof Creeper) {
			Player p = EntityUtils.getNearestPlayer(event.getLocation(), 100);
			if (p != null && DepthsManager.getInstance().isInSystem(p)) {
				event.blockList().stream().filter(b -> b.getType() == Material.SPAWNER).forEach(b -> DepthsManager.getInstance().playerBrokeSpawner(p, b.getLocation()));
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerItemDamageEvent(PlayerItemDamageEvent event) {
		DepthsParty party = DepthsManager.getInstance().getDepthsParty(event.getPlayer());
		if (party != null && party.getRoomNumber() % 10 == 0) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		DepthsManager manager = DepthsManager.getInstance();
		DepthsPlayer dp = manager.getDepthsPlayer(player);
		if (dp != null) {
			boolean shouldOfflineTeleport = true;
			DepthsParty party = manager.getPartyFromId(dp);
			if (party != null) {
				Map<DelvesModifier, Integer> delvePointsForParty = party.mDelveModifiers;
				for (DelvesModifier m : DelvesModifier.values()) {
					DelvesUtils.setDelvePoint(null, player, ServerProperties.getShardName(), m, delvePointsForParty.getOrDefault(m, 0));
				}
				// Check if the player should be dead due to too many logout cheese penalties
				if (getGraveDuration(party, dp, true) < GRAVE_REVIVE_DURATION) {
					player.sendMessage(Component.text("You have been punished for your hubris.", NamedTextColor.DARK_AQUA));
					sendPlayerToLootRoom(player, true);
					shouldOfflineTeleport = false;
				}
			}
			if (shouldOfflineTeleport) {
				dp.doOfflineTeleport();
			}
		}
	}
}
