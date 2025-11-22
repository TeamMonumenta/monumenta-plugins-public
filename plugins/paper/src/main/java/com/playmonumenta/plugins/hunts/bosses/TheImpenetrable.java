package com.playmonumenta.plugins.hunts.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.HuntsManager;
import com.playmonumenta.plugins.hunts.bosses.spells.AmethystShards;
import com.playmonumenta.plugins.hunts.bosses.spells.BanishCrystallizingGaze;
import com.playmonumenta.plugins.hunts.bosses.spells.ImpenetrableTeleport;
import com.playmonumenta.plugins.hunts.bosses.spells.PassivePiercingGems;
import com.playmonumenta.plugins.hunts.bosses.spells.ShatteredSlash;
import com.playmonumenta.plugins.hunts.bosses.spells.ShellRupture;
import com.playmonumenta.plugins.hunts.bosses.spells.SwiftSpikes;
import com.playmonumenta.plugins.hunts.bosses.spells.TeleportRemnant;
import com.playmonumenta.plugins.hunts.bosses.spells.TransientCrystals;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class TheImpenetrable extends Quarry {
	public static final String identityTag = "boss_theimpenetrable";
	public static final TextColor COLOR = NamedTextColor.LIGHT_PURPLE;

	public static final int INNER_RADIUS = 40;
	public static final int OUTER_RADIUS = 70;

	// Maximum health of the boss
	public static final int MAX_HEALTH = 6500;

	// Value for setting the maximum open height
	public static final float PEEK_HEIGHT = 0.7405f;

	private boolean mIsOpen = false;
	private boolean mIsClosing = false;
	private boolean mShouldSpoil = true;
	private boolean mIsBanishing = false;

	private int mPhase = 1;

	private final World mWorld;

	private final BossBarManager mBossBar;

	private final PassivePiercingGems mPiercingGems;
	private final ImpenetrableTeleport mTeleportSpell;
	private final TeleportRemnant mTeleportRemnant;

	// FIXME: Shell state disconnects with peek (maybe this bug does not exist :>)
	public TheImpenetrable(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc, INNER_RADIUS, OUTER_RADIUS, HuntsManager.QuarryType.THE_IMPENETRABLE);

		mWorld = mBoss.getWorld();

		// Initialize base information
		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");
		mBoss.setAI(false);
		if (mBoss instanceof Shulker shulker) {
			shulker.setPeek(0.0f);
		}

		EntityUtils.setMaxHealthAndHealth(mBoss, MAX_HEALTH);

		mBoss.setInvulnerable(true);

		mTeleportSpell = new ImpenetrableTeleport(mBoss, mBoss.getVehicle(), mSpawnLoc);
		mTeleportSpell.run(false);
		spawnAndDeathAnimation(false);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mBoss.setInvulnerable(false);
		}, 30);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			toggleShell();
			openShell();
		}, 45);

		// Almost all spells not handled in normal spell manager/passives list
		mPiercingGems = new PassivePiercingGems(plugin, mBoss, spawnLoc, OUTER_RADIUS);
		List<Spell> passives = List.of(mPiercingGems,
			new ShellRupture(mPlugin, mBoss));
		SpellManager actives = new SpellManager(Collections.emptyList());

		mBanishSpell = new BanishCrystallizingGaze(mPlugin, mBoss, this);

		// Custom spell manager for more versatility
		List<Spell> openSpells = new ArrayList<>();
		List<Spell> closedSpells = new ArrayList<>();
		mTeleportRemnant = new TeleportRemnant(mPlugin, mBoss);
		new BukkitRunnable() {
			int mTicks = 0;
			int mCooldownTicks = 60;
			int mSpellsBeforeTeleport = 1;
			int mSpellsThisPhase = 1;
			int mTimeUntilRemnant = 40 * 20;

			@Override
			public void run() {
				mCooldownTicks--;

				// Run next spell unless banishing
				if (mCooldownTicks <= 0 && !mIsBanishing) {
					if (mSpellsThisPhase == 0) {
						// Switch phases
						toggleShell();
						mSpellsThisPhase = switch (mPhase) {
							case 1 -> FastUtils.randomIntInRange(2, 3);
							case 2 -> FastUtils.randomIntInRange(1, 3);
							case 3 -> FastUtils.randomIntInRange(1, 2);
							default -> 0;
						};
						mCooldownTicks = 20;
					} else if (!mIsOpen) {
						// Closed phase actions
						if (mSpellsBeforeTeleport == 0) {
							if (mTimeUntilRemnant <= 0) {
								mTeleportRemnant.run();
								mTimeUntilRemnant = 40 * 20;
							}
							mTeleportSpell.run();
							mCooldownTicks = mTeleportSpell.cooldownTicks();
							mSpellsBeforeTeleport = (int) FastUtils.randomDoubleInRange(1, 2.5); // Bias towards 1 spell
						} else {
							List<Spell> spellList = new ArrayList<>(closedSpells.stream().filter(Spell::canRun).toList());
							Collections.shuffle(spellList);
							if (spellList.isEmpty()) {
								mCooldownTicks = 20;
							} else {
								Spell nextSpell = spellList.get(0);
								nextSpell.run();
								mCooldownTicks = nextSpell.cooldownTicks();
							}

							mSpellsBeforeTeleport--;
							mSpellsThisPhase--;
						}
					} else {
						// Open phase actions
						List<Spell> spellList = new ArrayList<>(openSpells.stream().filter(Spell::canRun).toList());
						Collections.shuffle(spellList);
						if (spellList.isEmpty()) {
							mCooldownTicks = 20;
						} else {
							Spell nextSpell = spellList.get(0);
							nextSpell.run();
							mCooldownTicks = nextSpell.cooldownTicks();
						}

						mSpellsThisPhase--;
					}
				}

				mTicks++;
				mTimeUntilRemnant--;

				if (mTicks % 8 == 0) {
					checkStanding();
				}

				char stateCharacter = mIsClosing ? '▄' : (mIsOpen ? '█' : '▁');
				mBossBar.setTitle("§4§l" + stateCharacter + " - The Impenetrable - " + stateCharacter);

				updateVisibleShellState();

				if (!mBoss.isValid()) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		Map<Integer, BossBarManager.BossHealthAction> events = getBaseHealthEvents();
		events.put(100, mBoss -> {
				openSpells.clear();
				openSpells.addAll(List.of(
					new ShatteredSlash(plugin, mBoss, false),
					new AmethystShards(plugin, mBoss)
				));
				closedSpells.clear();
				closedSpells.add(
					new SwiftSpikes(plugin, mBoss)
				);
			}
		);
		events.put(66, mBoss -> {
				mPhase = 2;
				openSpells.clear();
				openSpells.addAll(List.of(
					new ShatteredSlash(plugin, mBoss, false),
					new AmethystShards(plugin, mBoss)
				));
				closedSpells.clear();
				closedSpells.addAll(List.of(
					new SwiftSpikes(plugin, mBoss),
					new TransientCrystals(plugin, mBoss, 1)
				));
			}
		);
		events.put(33, mBoss -> {
				mPhase = 3;
				openSpells.clear();
				openSpells.addAll(List.of(
					new ShatteredSlash(plugin, mBoss, true),
					new AmethystShards(plugin, mBoss)
				));
				closedSpells.clear();
				closedSpells.addAll(List.of(
					new SwiftSpikes(plugin, mBoss),
					new TransientCrystals(plugin, mBoss, 3)
				));
			}
		);

		// Initialize boss bar
		mBossBar = new BossBarManager(mBoss, OUTER_RADIUS, BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_10, events, true, true, mSpawnLoc);

		// Finish boss
		constructBoss(actives, passives, OUTER_RADIUS, mBossBar, 100, 1);
	}

	public void banishStarted() {
		mIsBanishing = true;
	}

	public void banishFinished() {
		mIsBanishing = false;
	}

	// Check if a player is standing on the boss and repel them if so
	private void checkStanding() {
		Hitbox hitbox = new Hitbox.AABBHitbox(mWorld, new BoundingBox(Math.floor(mBoss.getLocation().clone().add(0, 2, 0).getX()), Math.floor(mBoss.getLocation().clone().add(0, 2, 0).getY()), Math.floor(mBoss.getLocation().clone().add(0, 2, 0).getZ()),
			Math.ceil(mBoss.getLocation().clone().add(0, 2, 0).getX()), Math.ceil(mBoss.getLocation().clone().add(0, 2, 0).getY()), Math.ceil(mBoss.getLocation().clone().add(0, 2, 0).getZ())));
		for (Player player : hitbox.getHitPlayers(true)) {
			MovementUtils.pullTowards(mBoss.getLocation().clone().add(FastUtils.randomIntInRange(-8, 8), 4, FastUtils.randomIntInRange(-8, 8)), player, 0.2f);
			player.sendMessage(Component.text("The shell singes you!", NamedTextColor.LIGHT_PURPLE));
			player.playSound(mBoss.getLocation(), Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.HOSTILE, 2f, 2f);
		}
	}

	public void toggleShell() {
		if (mIsOpen) {
			closeShell();
		} else {
			openShell();
		}
	}

	public void openShell() {
		if (mBoss instanceof Shulker shulker) {
			shulker.setPeek(PEEK_HEIGHT);
			mIsOpen = true;
			mShouldSpoil = false;
		}
	}

	public void closeShell() {
		if (mIsClosing) {
			return;
		}
		if (mBoss instanceof Shulker shulker) {
			shulker.setPeek(0.0f);

			mWorld.playSound(mBoss.getLocation(), Sound.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.HOSTILE, 2.3f, 0.79f);

			// delay 14 ticks before registering as closed
			// delay 19 ticks before damage will spoil
			mIsClosing = true;
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				mIsOpen = false;
				mIsClosing = false;
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					mShouldSpoil = true;
				}, 5);
			}, 14);
		}
	}

	public void updateVisibleShellState() {
		if (mBoss instanceof Shulker shulker) {
			if (mIsOpen && !mIsClosing && shulker.getPeek() == 0.0f) {
				shulker.setPeek(PEEK_HEIGHT);
			} else if (shulker.getPeek() == PEEK_HEIGHT) {
				shulker.setPeek(0.0f);
			}
		}
	}

	// Returns the open location above the nearest solid ground block vertically down from the starting location
	public static @Nullable Location getOnNearestGround(Location startingLoc, int blocksDown) {
		for (int i = 0; i < blocksDown; i++) {
			if (startingLoc.clone().add(0, -i, 0).getBlock().isEmpty()) {
				if (startingLoc.clone().add(0, -i - 1, 0).getBlock().getType().isOccluding()) {
					return startingLoc.clone().add(0, -i, 0);
				}
			} else {
				return null;
			}
		}
		return null; // egregious mistake
	}

	@Override
	public void onHurt(DamageEvent event) {
		super.onHurt(event);

		if (mTeleportRemnant.hasExistingRemnant() && event.getDamager() instanceof Player player) {
			event.setFlatDamage(event.getDamage() * 0.5);

			player.playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_HIT, SoundCategory.HOSTILE, 1f, 2f);
		}

		// Prevent suffocation damage, caused by blocks being placed inside it
		if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
			event.setCancelled(true);
		}

		// If an attack happens while the shell is closed, prevent the attack and spoil the player
		Entity damager = event.getDamager();
		if (damager instanceof Player player) {
			if (!mIsOpen) {
				if (!AbilityUtils.isIndirectDamage(event)) {
					if (mShouldSpoil && spoil(player)) {
						player.sendMessage(Component.text("Your attack bounces off the crystalline shell with a thunderous clang, damaging it and spoiling your loot.", NamedTextColor.LIGHT_PURPLE));
					}

					Location loc = mBoss.getLocation().clone().add(0, 0.5, 0);

					new PartialParticle(Particle.FIREWORKS_SPARK, loc, 20, 0, 0, 0, 0.15).spawnAsEntityActive(mBoss);
					player.playSound(loc, Sound.BLOCK_COPPER_BREAK, SoundCategory.HOSTILE, 5.8f, 0.73f);
					player.playSound(loc, Sound.BLOCK_LARGE_AMETHYST_BUD_PLACE, SoundCategory.HOSTILE, 4.6f, 0.52f);
					player.playSound(loc, Sound.BLOCK_BELL_USE, SoundCategory.HOSTILE, 1.8f, 0.59f);

				}

				event.setCancelled(true);
			}
		}
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (event.getDamager() instanceof ShulkerBullet && damagee instanceof Player player) {
			mPiercingGems.hit(event, player);
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		super.death(event);

		spawnAndDeathAnimation(true);
		mPiercingGems.clearBullets();
	}

	private void spawnAndDeathAnimation(boolean death) {
		new BukkitRunnable() {
			int mTicks = 1;
			final List<ArmorStand> mArmorStands = new ArrayList<>();
			@Nullable LivingEntity mMob;

			@Override
			public void run() {
				if (mTicks == 1) {
					if (death) {
						mBoss.remove();
						mMob = EntityUtils.copyMob(mBoss);
						mMob.setHealth(1);
						mMob.setInvulnerable(true);
						mMob.setGravity(false);
						mMob.setCollidable(false);
						mMob.setAI(false);
						mMob.addScoreboardTag("SkillImmune");
					} else {
						mMob = mBoss;
					}
					// Figure out where and when to generate amethyst
					BoundingBox box = mMob.getBoundingBox();
					for (double x = box.getMinX() + 0.2; x <= box.getMaxX(); x += 0.6) {
						for (double y = box.getMinY(); y <= box.getMaxY(); y += 0.6) {
							for (double z = box.getMinZ() + 0.2; z <= box.getMaxZ(); z += 0.6) {
								int delay = (int) (Math.random() * 20) + 1;

								Location location = new Location(mWorld, x, y - 1.5, z);

								if (death) {
									Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
										ArmorStand amethyst = spawnAnimationAmethyst(location);
										mWorld.playSound(location, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.HOSTILE, 0.25f, 1.3f);
										mArmorStands.add(amethyst);
									}, delay);
								} else {
									ArmorStand amethyst = spawnAnimationAmethyst(location);

									Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
										amethyst.remove();
										mWorld.playSound(location, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.HOSTILE, 0.25f, 1.3f);
									}, delay + 10);
								}
							}
						}
					}
				} else if (mTicks == 29) {
					if (death) {
						// Remove the mob a tick before the finisher is done
						if (mMob != null) {
							mMob.remove();
						}
					}
				} else if (mTicks == 30 && death) {
					// Break the amethyst with particles and play the sound
					for (ArmorStand amethyst : mArmorStands) {
						amethyst.remove();
						new PartialParticle(Particle.BLOCK_CRACK, amethyst.getLocation().add(0, 1.5, 0), 5, Bukkit.createBlockData(Material.AMETHYST_BLOCK)).spawnAsBoss();
					}
					mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 5f, 1f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 0.8f, 0.7f);
				} else if (mTicks == 30) {
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 5f, 2f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_SHULKER_SHOOT, SoundCategory.HOSTILE, 5f, 0.8f);
				}
				if (mTicks > 30) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 1, 1);
	}

	private ArmorStand spawnAnimationAmethyst(Location location) {
		ArmorStand amethyst = mWorld.spawn(location, ArmorStand.class);
		amethyst.setVisible(false);
		amethyst.setGravity(false);
		amethyst.setVelocity(new Vector());
		amethyst.setMarker(true);
		amethyst.setCollidable(false);
		amethyst.getEquipment().setHelmet(new ItemStack(Material.AMETHYST_BLOCK));
		EntityUtils.setRemoveEntityOnUnload(amethyst);
		return amethyst;
	}

	@Override
	public String getUnspoiledLootTable() {
		return "epic:r3/hunts/loot/the_impenetrable_unspoiled";
	}

	@Override
	public String getSpoiledLootTable() {
		return "epic:r3/hunts/loot/the_impenetrable_spoiled";
	}

	@Override
	public String getAdvancement() {
		return "monumenta:challenges/r3/hunts/the_impenetrable";
	}

	@Override
	public String getQuestTag() {
		return "HuntShulker";
	}
}
