package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.bosses.ShieldSwitchBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellDimensionDoor extends Spell {
	private static final String SPELL_NAME = "Dimension Door";
	private static final int COOLDOWN = 20 * 45;
	private static final int PLAYER_CAP = 15;
	private static final List<WeakReference<Player>> mShadowed = new ArrayList<>();
	private static final HashSet<UUID> mWarned = new HashSet<>();
	private static final EnumSet<Material> IGNORED_MATS = EnumSet.of(
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.BEDROCK,
		Material.BARRIER,
		Material.END_PORTAL
	);

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private final ThreadLocalRandom mRand = ThreadLocalRandom.current();
	private final List<Player> mByPortal = new ArrayList<>();
	private final List<Location> mPortalLoc = new ArrayList<>();
	private final List<Location> mReplaceLoc = new ArrayList<>();
	private final double mRange;
	private final ChargeUpManager mChargeUp;
	private boolean mCanRun = true;
	private int mT = 20 * 10;

	public SpellDimensionDoor(Plugin plugin, LivingEntity boss, Location spawnLoc, double range) {
		mPlugin = plugin;
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mRange = range;
		mChargeUp = Lich.defaultChargeUp(mBoss, 25, "Channeling " + SPELL_NAME + "...");
	}

	public static List<Player> getShadowed() {
		// Return to the caller a list of all the non-garbage-collected player entries
		return mShadowed.stream().map(Reference::get).filter(Objects::nonNull).collect(Collectors.toList());
	}

	public static void clearShadowed() {
		mShadowed.clear();
	}

	@Override
	public void run() {
		mT -= 5;
		if (mT <= 0 && mCanRun) {
			mT = COOLDOWN;
			spawnPortal();
		}
	}

	private void spawnPortal() {
		//clear portal loc list
		mPortalLoc.clear();

		World world = mBoss.getWorld();
		mByPortal.clear();
		List<Player> players = Lich.playersInRange(mSpawnLoc, mRange, true);

		List<Player> shadowed = getShadowed();
		if (!shadowed.isEmpty()) {
			players.removeAll(shadowed);
		}
		List<Player> toRemove = new ArrayList<>();
		for (Player p : players) {
			p.sendMessage(Component.text("THE SHADOWS HOLD MANY SECRETS.", NamedTextColor.LIGHT_PURPLE));
			if (PlayerUtils.isCursed(com.playmonumenta.plugins.Plugin.getInstance(), p)) {
				p.sendMessage(Component.text("I can cleanse the curse on me if I enter the shadows.", NamedTextColor.AQUA));
			}
			if (p.getLocation().getY() < mSpawnLoc.getY() - 8) {
				toRemove.add(p);
			}
		}
		players.removeAll(toRemove);

		List<Player> targets = new ArrayList<>();
		if (players.size() <= 2) {
			targets = players;
		} else {
			int cap = (int) Math.min(PLAYER_CAP, Math.ceil(players.size() / 2.0));
			for (int i = 0; i < cap; i++) {
				Player player = players.get(mRand.nextInt(players.size()));
				if (targets.contains(player)) {
					cap++;
				} else {
					targets.add(player);
				}
			}
		}

		BukkitRunnable runA = new BukkitRunnable() {

			@Override
			public void run() {
				mCanRun = false;
				if (mChargeUp.nextTick()) {
					this.cancel();
					mChargeUp.setTitle(Component.text(SPELL_NAME + " Remaining Time", NamedTextColor.YELLOW));
					mChargeUp.setColor(net.kyori.adventure.bossbar.BossBar.Color.RED);
					new BukkitRunnable() {
						int mT = 0;

						@Override
						public void run() {
							mT++;
							double progress = 1.0d - mT / (20.0d * 30.0d);
							if (progress >= 0 && !Lich.phase3over()) {
								mChargeUp.setProgress(progress);
								mChargeUp.setColor(BossBar.Color.RED);
							} else {
								this.cancel();
								mCanRun = true;
								mChargeUp.reset();
								mChargeUp.setTitle(Component.text("Casting " + SPELL_NAME + "...", NamedTextColor.YELLOW));
								mChargeUp.setColor(BossBar.Color.YELLOW);
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}
			}

		};
		runA.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runA);

		for (Player p : targets) {
			Location pLoc = p.getLocation();
			world.playSound(pLoc, Sound.BLOCK_PORTAL_TRIGGER, SoundCategory.HOSTILE, 1f, 2.0f);
			world.playSound(pLoc, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 1f, 1.0f);

			PPCircle indicator = new PPCircle(Particle.SPELL_WITCH, pLoc, 3).count(3).delta(0.2, 0, 0.2);
			PPCircle indicator2 = new PPCircle(Particle.SMOKE_NORMAL, pLoc, 3).count(2).delta(0.2, 0, 0.2);

			List<BlockState> toRestore = new ArrayList<>();
			BukkitRunnable runB = new BukkitRunnable() {
				int mT = 0;
				List<Player> mTeleport = new ArrayList<>();
				final Location mLoc = p.getLocation();

				@Override
				public void run() {
					mT++;

					//move portal center to ground, stop above bedrock so that it doesn't replace bedrock
					Location locDown = mLoc.clone().subtract(0, 1, 0);
					while ((mLoc.getBlock().isPassable() || mLoc.getBlock().isLiquid()
								|| mLoc.getBlock().isEmpty()) && locDown.getBlock().getType() != Material.BEDROCK
							   && mLoc.getY() > mSpawnLoc.getY() - 5 && mT <= 5) {
						mLoc.setY(mLoc.getY() - 1);
						locDown = mLoc.clone().subtract(0, 1, 0);
					}

					if (mT <= 25) {
						indicator.location(mLoc.clone().add(0, 1.1, 0)).spawnAsBoss();
						indicator2.location(mLoc.clone().add(0, 1.1, 0)).spawnAsBoss();
					}

					//get blocks and replace
					Location portalCenterLoc = mLoc.clone();
					Location testLoc = portalCenterLoc.clone();
					List<Block> replace = new ArrayList<>();
					if (mT == 25) {
						mPortalLoc.add(portalCenterLoc);
						mTeleport = Lich.playersInRange(mSpawnLoc, mRange, true);
						world.playSound(mLoc, Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 1.0f, 1.0f);
						//get blocks 5x5
						for (int x = -2; x <= 2; x++) {
							testLoc.setX(portalCenterLoc.getX() + x);
							for (int z = -2; z <= 2; z++) {
								testLoc.setZ(portalCenterLoc.getZ() + z);

								//check if testLoc is already in set or is end portal
								if (!IGNORED_MATS.contains(testLoc.getBlock().getType()) && !mReplaceLoc.contains(testLoc.getBlock().getLocation())) {
									toRestore.add(testLoc.getBlock().getState());
									replace.add(testLoc.getBlock());
									mReplaceLoc.add(testLoc.getBlock().getLocation());
								}
							}
						}
						//repeat for 7x3
						for (int x = -3; x <= 3; x++) {
							testLoc.setX(portalCenterLoc.getX() + x);
							for (int z = -1; z <= 1; z++) {
								testLoc.setZ(portalCenterLoc.getZ() + z);

								//check if testLoc is already in set or is end portal
								if (!IGNORED_MATS.contains(testLoc.getBlock().getType()) && !mReplaceLoc.contains(testLoc.getBlock().getLocation())) {
									toRestore.add(testLoc.getBlock().getState());
									replace.add(testLoc.getBlock());
									mReplaceLoc.add(testLoc.getBlock().getLocation());
								}
							}
						}
						//repeat for 3x7
						for (int x = -1; x <= 1; x++) {
							testLoc.setX(portalCenterLoc.getX() + x);
							for (int z = -3; z <= 3; z++) {
								testLoc.setZ(portalCenterLoc.getZ() + z);

								//check if testLoc is already in set or is end portal
								if (!IGNORED_MATS.contains(testLoc.getBlock().getType()) && !mReplaceLoc.contains(testLoc.getBlock().getLocation())) {
									toRestore.add(testLoc.getBlock().getState());
									replace.add(testLoc.getBlock());
									mReplaceLoc.add(testLoc.getBlock().getLocation());
								}
							}
						}
						//replace all blocks with black concrete first, then change the smaller circle to end portal
						for (Block b : replace) {
							if (b.getType() != Material.END_PORTAL) {
								b.setType(Material.BLACK_CONCRETE);
							}
						}
						for (int x = -2; x <= 2; x++) {
							testLoc.setX(portalCenterLoc.getX() + x);
							for (int z = -1; z <= 1; z++) {
								testLoc.setZ(portalCenterLoc.getZ() + z);
								if (testLoc.getBlock().getType() == Material.BLACK_CONCRETE) {
									testLoc.getBlock().setType(Material.END_PORTAL);
								}
							}
						}
						for (int x = -1; x <= 1; x++) {
							testLoc.setX(portalCenterLoc.getX() + x);
							for (int z = -2; z <= 2; z++) {
								testLoc.setZ(portalCenterLoc.getZ() + z);
								if (testLoc.getBlock().getType() == Material.BLACK_CONCRETE) {
									testLoc.getBlock().setType(Material.END_PORTAL);
								}
							}
						}
					}

					/*
					 * for 30 seconds, keep portal open and teleport players to shadow realm for 10 seconds
					 * players who got in are immune to teleport until the portal closes
					 */

					mTeleport.removeAll(mByPortal);
					for (Player p : mTeleport) {
						Location tLoc = p.getLocation();
						for (Location loc : mPortalLoc) {
							if (tLoc.getBlock().getType() == Material.END_PORTAL && p.getLocation().distance(loc) <= 4) {
								mByPortal.add(p);
								getWealmed(mPlugin, p, mBoss, tLoc, true);
							}
						}
					}

					//cancels after 30 seconds from portal opening
					if (mT >= 20 * 31 || Lich.phase3over() || mBoss.isDead() || !mBoss.isValid()) {
						this.cancel();
						for (BlockState state : toRestore) {
							state.update(true);
						}
						toRestore.clear();
						mReplaceLoc.clear();
					}
				}

			};
			runB.runTaskTimer(mPlugin, 0, 1);
			mActiveRunnables.add(runB);
		}
	}

	public static void getWealmed(Plugin plugin, Player p, LivingEntity mBoss, Location tLoc, boolean byPortal) {
		Location tele = tLoc.clone();
		Location shadowLoc = Lich.getLichSpawn().subtract(0, 42, 0);
		tele.setY(shadowLoc.getY());
		if (tele.getBlock().getType() != Material.AIR) {
			tele = shadowLoc.clone().subtract(5, 0, 0);
		}
		p.teleport(tele, PlayerTeleportEvent.TeleportCause.UNKNOWN);
		p.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 5, 0));
		mShadowed.add(new WeakReference<>(p));

		String[] dio = new String[]{
			"There's something moving in the darkness! It looks like... me?!",
			"This darkness is dangerous. I need to find a way out!",
			"What is that pulsating mass in the center of the arena?"
		};

		//do different stuff for different entry method
		int t = 20 * 20;
		if (byPortal) {
			if (mWarned.contains(p.getUniqueId())) {
				p.sendMessage(Component.text(dio[FastUtils.RANDOM.nextInt(3)], NamedTextColor.AQUA));
			} else {
				p.sendMessage(Component.text(dio[0], NamedTextColor.AQUA));
				mWarned.add(p.getUniqueId());
			}
			//remove curse only through portal
			if (PlayerUtils.isCursed(com.playmonumenta.plugins.Plugin.getInstance(), p)) {
				PlayerUtils.removeCursed(com.playmonumenta.plugins.Plugin.getInstance(), p);
				p.sendMessage(Component.text("You felt a curse being lifted.", NamedTextColor.AQUA));
			}
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(p, Lich.curseSource);
		} else {
			t = 20 * 10;
			DamageUtils.damage(mBoss, p, DamageType.OTHER, 1);
			p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.HOSTILE, 1, 1);
			AbilityUtils.increaseDamageDealtPlayer(p, 20 * 30, -0.2, "Lich");
			Lich.cursePlayer(p);
		}
		int tick = t;

		//mirror location summon spectre
		Vector vec = LocationUtils.getVectorTo(tele, shadowLoc);
		Location spectreLoc = shadowLoc.clone().subtract(vec);
		LivingEntity spectre = Lich.summonSpectre(p, spectreLoc);
		int health = ScoreboardUtils.checkTag(spectre, ShieldSwitchBoss.identityTag) ? 150 : 200;
		EntityUtils.setMaxHealthAndHealth(spectre, health);
		spectre.setGlowing(true);
		ScoreboardUtils.addEntityToTeam(spectre, "Hekawt");
		((Creature) spectre).setTarget(p);

		BossBar bar = BossBar.bossBar(Component.text("Soul dissipating in " + tick / 20 + " seconds!", NamedTextColor.YELLOW), 1, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS, Set.of(BossBar.Flag.PLAY_BOSS_MUSIC));
		p.showBossBar(bar);

		new BukkitRunnable() {
			int mT = tick;
			boolean mTrigger = false;

			@Override
			public void run() {
				mT -= 2;
				new PartialParticle(Particle.SPELL_WITCH, spectre.getEyeLocation(), 1, 0.1, 0.1, 0.1, 0).spawnAsEntityActive(mBoss);
				float progress = mT * 1.0f / tick;
				if (progress >= 0) {
					bar.progress(progress);
				}
				if (progress <= 0.34) {
					bar.color(BossBar.Color.RED);
				} else if (progress <= 0.67) {
					bar.color(BossBar.Color.YELLOW);
				}
				if (mT % 20 == 0) {
					bar.name(Component.text("Soul dissipating in " + mT / 20 + " seconds!", NamedTextColor.YELLOW));
				}
				//kill player if time runs out. show that they are dying extremely quickly
				if (mT <= 0) {
					BossUtils.bossDamagePercent(mBoss, p, 0.1);
				}

				if (spectre.isDead() || !spectre.isValid() || Lich.phase3over() || mBoss.isDead() || !mBoss.isValid()) {
					// Death Report Advancement. If player escapes with less than 1 second.
					if (mT <= 20) {
						AdvancementUtils.grantAdvancement(p, "monumenta:challenges/r2/lich/death_report");
					}

					p.hideBossBar(bar);
					this.cancel();
					Location leaveLoc = tLoc.clone().add(0, 1.5, 0);
					mShadowed.removeIf((pref) -> pref.get() == p || pref.get() == null);
					p.teleport(leaveLoc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
					if (byPortal) {
						p.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 5, 0));
						p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 5, 0));
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, PercentDamageReceived.GENERIC_NAME,
							new PercentDamageReceived(20 * 5, -1.0));
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, CustomRegeneration.effectID,
							new CustomRegeneration(20 * 5, 1.0, 25, null, com.playmonumenta.plugins.Plugin.getInstance()));

						p.sendMessage(Component.text("Something feels different. The shadows aren't clinging to me anymore.", NamedTextColor.AQUA));
					} else {
						DamageUtils.damage(mBoss, p, DamageType.OTHER, 1);
						p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.HOSTILE, 1, 1);
					}
				}
				if (p.getLocation().getY() > Lich.getLichSpawn().getY() - 10 || !Lich.playersInRange(shadowLoc, 60, true).contains(p)) {
					mShadowed.removeIf((pref) -> pref.get() == p || pref.get() == null);
					p.hideBossBar(bar);
					this.cancel();
				}
				if (p.getLastDamageCause() != null && p.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.SUFFOCATION && !mTrigger) {
					//something went wrong with the other check, catching wrong tp
					p.teleport(shadowLoc.clone().add(-5, 0, 0), PlayerTeleportEvent.TeleportCause.UNKNOWN);
					spectre.teleport(shadowLoc.clone().add(5, 0, 0));
					mTrigger = true;
				}
			}
		}.runTaskTimer(plugin, 0, 2);
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}

}
