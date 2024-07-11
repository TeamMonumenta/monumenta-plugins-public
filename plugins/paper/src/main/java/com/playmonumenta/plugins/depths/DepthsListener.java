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
import com.playmonumenta.plugins.depths.abilities.dawnbringer.EternalSavior;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.Sundrops;
import com.playmonumenta.plugins.depths.abilities.earthbound.EarthenWrath;
import com.playmonumenta.plugins.depths.abilities.prismatic.Charity;
import com.playmonumenta.plugins.depths.abilities.prismatic.ColorSplash;
import com.playmonumenta.plugins.depths.abilities.prismatic.Rebirth;
import com.playmonumenta.plugins.depths.abilities.steelsage.FireworkBlast;
import com.playmonumenta.plugins.depths.bosses.Broodmother;
import com.playmonumenta.plugins.depths.bosses.Callicarpa;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.graves.GraveManager;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ExperienceUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
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
import org.bukkit.GameMode;
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
	private static final String DISCONNECT_ANTICHEESE_MOB_TAG = "ZenithDisconnectedNearMobs";
	private static final String DISCONNECT_ANTICHEESE_BOSS_TAG = "ZenithDisconnectedNearBoss";

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

			Location loc = block.getLocation().toCenterLocation();
			attemptSundrops(loc, player);
		} else if (type == Material.CHEST) {
			//Player's can't break chests themselves
			event.setCancelled(true);
		}
	}

	public static void attemptSundrops(Location loc, Player player) {
		DepthsParty party = DepthsManager.getInstance().getDepthsParty(player);
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
					weight.getKey().summonSundrop(loc);
					break;
				} else {
					roll -= chance;
				}
			}
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
			int scalingFactor = (floor - 1) / 3;
			if (floor > 15) {
				double multiplier = 1 + (0.1 * (scalingFactor - 4));
			    event.setDamage(event.getDamage() * multiplier);
			}
			if (source != null && EntityUtils.isBoss(source) && floor > 3) {
				double multiplier = 1 + (0.05 * scalingFactor);
			    event.setDamage(event.getDamage() * multiplier);
			}
		}

		ClassAbility ability = event.getAbility();
		DamageEvent.DamageType type = event.getType();
		if (ability != null && !ability.isFake() && type != DamageEvent.DamageType.TRUE && type != DamageEvent.DamageType.OTHER && event.getSource() instanceof Player player) {
			event.setDamage(event.getFlatDamage() * DepthsUtils.getDamageMultiplier());

			ItemStatManager.PlayerItemStats playerItemStats = event.getPlayerItemStats();
			if (playerItemStats == null) {
				playerItemStats = Plugin.getInstance().mItemStatManager.getPlayerItemStats(player);
			}
			double adaptiveMultiplier = DepthsUtils.getAdaptiveDamageMultiplier(playerItemStats, type);
			if (adaptiveMultiplier > 0) {
				event.updateGearDamageWithMultiplier(adaptiveMultiplier);
			}
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
					MMLog.finer(player.getName() + " died. mNumDeaths = " + dp.mNumDeaths);
					int graveDuration = getGraveDuration(party, dp, false);
					dp.mNumDeaths++;
					if (graveDuration > GRAVE_REVIVE_DURATION) {
						Location waitingRoomLocation = party.mDeathWaitingRoomPoint.toLocation(player.getWorld());
						Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> player.teleport(waitingRoomLocation));
						MessagingUtils.sendTitle(player, Component.text("You Died", NamedTextColor.RED),
							Component.text("Please wait to be revived!", NamedTextColor.RED));

						ArmorStand grave = player.getWorld().spawn(deathLocation, ArmorStand.class, g -> {
							g.setInvulnerable(true);
							// TODO: setDisabledSlots/addDisabledSlots DOES NOT WORK FOR OFFHANDS - cancel PlayerArmorStandManipulateEvent (or use GraveManager.DISABLE_INTERACTION_TAG) instead - usb
							g.setDisabledSlots(EquipmentSlot.values());
							g.setCollidable(false);
							g.setBasePlate(false);
							g.customName(Component.text(player.getName() + "'s Grave", NamedTextColor.RED));
							g.setCustomNameVisible(true);
							g.setGlowing(true);
							g.setArms(true);
							g.setGravity(true);
							ScoreboardUtils.addEntityToTeam(g, "GraveGreen", NamedTextColor.GREEN);
							g.addScoreboardTag(GRAVE_TAG);
							g.addScoreboardTag(GraveManager.DISABLE_INTERACTION_TAG);

							// TODO: steal this code to be used in other graves or standardize graves across content - usb
							VanityManager.VanityData vanityData = Plugin.getInstance().mVanityManager.getData(player);
							for (EquipmentSlot slot : EquipmentSlot.values()) {
								ItemStack item;
								if (slot != EquipmentSlot.HEAD) {
									item = ItemUtils.clone(player.getInventory().getItem(slot).clone());
									if (ItemUtils.isNullOrAir(item)) {
										continue;
									}
									VanityManager.applyVanity(item, vanityData, slot, false);
									// usb: remove stats from item before adding to armorstand in case of dupe
									// this should happen after applying vanity
									ItemUpdateHelper.removeStats(item);
								} else {
									item = new ItemStack(Material.PLAYER_HEAD);
									if (item.getItemMeta() instanceof SkullMeta skullMeta) {
										skullMeta.setOwningPlayer(player);
										item.setItemMeta(skullMeta);
									}
								}
								g.setItem(slot, item);
							}
						});

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
									.toList());

								nearbyPlayers.removeIf(player -> {
									// if a player is within range due to any one of their abilities, ensure they won't be removed

									ColorSplash colorSplash = AbilityManager.getManager().getPlayerAbility(player, ColorSplash.class);
									if (colorSplash != null && colorSplash.hasIncreasedReviveRadius()) {
										if (player.getLocation().distanceSquared(grave.getLocation()) <= mBaseReviveRadiusSquared + Math.pow(ColorSplash.DAWNBRINGER_EXTRA_REVIVE_RADIUS, 2)) {
											return false;
										}
									}

									EternalSavior eternalSavior = AbilityManager.getManager().getPlayerAbility(player, EternalSavior.class);
									if (eternalSavior != null && eternalSavior.hasIncreasedReviveRadius()) {
										if (eternalSavior.getSaviorLocation().distanceSquared(grave.getLocation()) <= Math.pow(eternalSavior.getIncreasedReviveRadius(), 2)) {
											return false;
										}
									}

									return player.getLocation().distanceSquared(grave.getLocation()) > mBaseReviveRadiusSquared;
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
										Plugin.getInstance().mEffectManager.addEffect(player, "ZenithReviveResistance", new PercentDamageReceived(20, -1));
										if (!dp.doOfflineTeleport()) {
											player.teleport(grave.getLocation());
										}
										Charity.onCharityRevive(player, nearbyPlayers, maxCharityLevel);
										dp.sendMessage("You have been revived!");
										party.sendMessage(player.getName() + " has been revived!", d -> d != dp);
										dp.mDead = false;
										MMLog.info(player.getName() + " was revived.");
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
									if (!attemptRebirth(player, dp, grave.getLocation())) {
										sendPlayerToLootRoom(player, true);
									}
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
						if (!attemptRebirth(player, dp, player.getLocation())) {
							// died too often: immediately send to loot room
							dp.sendMessage("You have died too often and have been sent directly to the loot room!");
							Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> sendPlayerToLootRoom(player, true));
						}
					}
				} else if (!attemptRebirth(player, dp, player.getLocation())) {
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
				} else {
					event.setCancelled(true);
				}
			}
		}
	}

	public boolean attemptRebirth(Player player, DepthsPlayer dp, Location teleportTo) {
		Rebirth rebirth = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Rebirth.class);
		if (rebirth != null) {
			rebirth.activationEffects();
			rebirth.rerollAbilities(dp);
			rebirth.applyResistance();
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> player.teleport(teleportTo));
			dp.mDead = false;
			dp.mCurrentlyReviving = false;
			dp.mNumDeaths--;
			dp.sendMessage("You have been reborn!");
			DepthsParty party = DepthsManager.getInstance().getPartyFromId(dp);
			if (party != null) {
				party.sendMessage(player.getName() + " has been reborn!", other -> other != dp);
			}
			// Might need more protections but I think this should be enough
			DepthsManager.getInstance().validateOfferings(dp);
			return true;
		}
		return false;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		DepthsManager dm = DepthsManager.getInstance();
		DepthsPlayer dp = dm.getDepthsPlayer(player);

		MMLog.finer("Player " + player.getName() + " quit." +
			" has_depths_player=" + (dp != null) +
			" transferring=" + Plugin.getInstance().mPlayerListener.isPlayerTransferring(player) +
			" quit_reason=" + event.getReason().name()
		);

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

			MMLog.finer("Player " + player.getName() +
				" nearZenithBoss=" + nearZenithBoss +
				" nearHostileMob=" + (EntityUtils.getNearestHostileTargetable(player.getLocation(), DISCONNECT_ANTICHEESE_RADIUS) != null)
			);

			if (nearZenithBoss) {
				player.getScoreboardTags().add(DISCONNECT_ANTICHEESE_BOSS_TAG);
				MMLog.info(player.getName() + " logged out near boss and will have their death counter increased.");
			} else {
				List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(player.getLocation(), DISCONNECT_ANTICHEESE_RADIUS);
				nearbyMobs.removeIf(e -> e.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
				if (!nearbyMobs.isEmpty()) {
					player.getScoreboardTags().add(DISCONNECT_ANTICHEESE_MOB_TAG);
					MMLog.info(player.getName() + " logged out near mobs and will have their death counter increased. Number of mobs: " + nearbyMobs.size());
				}
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

					if (!party.mEndlessMode && party.mAscension == 0) {
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
		MMLog.finer("Player " + player.getName() + " logged in. has_depths_player=" + (dp != null));
		if (dp != null) {
			boolean shouldOfflineTeleport = true;

			if (dp.mDead) {
				shouldOfflineTeleport = false;
				sendPlayerToLootRoom(player, true);
				MMLog.info(player.getName() + " was sent to the loot room because they logged in while awaiting respawn.");
			}

			DepthsParty party = manager.getPartyFromId(dp);
			MMLog.finer("Player " + player.getName() + " has_party=" + (party != null));
			if (party != null) {
				Map<DelvesModifier, Integer> delvePointsForParty = party.mDelveModifiers;
				for (DelvesModifier m : DelvesModifier.values()) {
					DelvesUtils.setDelvePoint(null, player, ServerProperties.getShardName(), m, delvePointsForParty.getOrDefault(m, 0));
				}

				boolean disconnectAnticheese = false;

				// Check if the player should be dead due to too many logout cheese penalties
				if (player.getScoreboardTags().contains(DISCONNECT_ANTICHEESE_MOB_TAG)) {
					disconnectAnticheese = true;
					dp.mNumDeaths++;
					MMLog.finer(player.getName() + " logged in with anticheese mob tag. mNumDeaths = " + dp.mNumDeaths);
				}
				if (player.getScoreboardTags().contains(DISCONNECT_ANTICHEESE_BOSS_TAG)) {
					disconnectAnticheese = true;
					dp.mNumDeaths += 2;
					MMLog.finer(player.getName() + " logged in with anticheese boss tag. mNumDeaths = " + dp.mNumDeaths);
				}

				if (disconnectAnticheese && getGraveDuration(party, dp, true) < GRAVE_REVIVE_DURATION) {
					player.sendMessage(Component.text("You have been punished for your hubris.", NamedTextColor.DARK_AQUA));
					sendPlayerToLootRoom(player, true);
					shouldOfflineTeleport = false;
					MMLog.info(player.getName() + " was punished for their hubris (send to lootroom on login due to anticheese) in Zenith. They \"died\" " + dp.mNumDeaths + " times, including artificial deaths from anticheese.");
					AuditListener.logDeath(player.getName() + " was punished for their hubris (send to lootroom on login due to anticheese) in Zenith. They \"died\" " + dp.mNumDeaths + " times, including artificial deaths from anticheese.");
				}
			}
			if (shouldOfflineTeleport) {
				dp.doOfflineTeleport();
			}
		}

		// Make sure to remove anticheese tags
		player.getScoreboardTags().remove(DISCONNECT_ANTICHEESE_MOB_TAG);
		player.getScoreboardTags().remove(DISCONNECT_ANTICHEESE_BOSS_TAG);

		if (dp == null
			&& player.getGameMode() == GameMode.SURVIVAL
			&& !ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.LOOTROOM)
			&& manager.getParty(player.getWorld()) != null
			&& Plugin.IS_PLAY_SERVER) {
			PlayerUtils.executeCommandOnPlayer(player, "function monumenta:lobbies/abandon_instance");
		}
	}
}
