package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.player.PPGroundCircle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellDimensionDoor extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mSpawnLoc;
	private ThreadLocalRandom mRand = ThreadLocalRandom.current();
	private static List<Player> mShadowed = new ArrayList<Player>();
	private static List<Player> mWarned = new ArrayList<Player>();
	private List<Player> mByPortal = new ArrayList<Player>();
	private List<Location> mPortalLoc = new ArrayList<Location>();
	private List<Location> mReplaceLoc = new ArrayList<Location>();
	private double mRange;
	private int mCoolDown = 20 * 45;
	private int mT = 20 * 10;
	private int mCap = 15;
	private boolean mCanRun = true;
	private final EnumSet<Material> mIgnoredMats = EnumSet.of(
			Material.COMMAND_BLOCK,
			Material.CHAIN_COMMAND_BLOCK,
			Material.REPEATING_COMMAND_BLOCK,
			Material.BEDROCK,
			Material.BARRIER,
			Material.END_PORTAL
		);
	private ChargeUpManager mChargeUp;

	public SpellDimensionDoor(Plugin plugin, LivingEntity boss, Location spawnLoc, double range) {
		mPlugin = plugin;
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mRange = range;
		mChargeUp = new ChargeUpManager(mBoss, 25, ChatColor.YELLOW + "Channeling Dimensional Door...", BarColor.YELLOW, BarStyle.SOLID, 50);
	}

	public static List<Player> getShadowed() {
		return mShadowed;
	}

	public static void clearShadowed() {
		mShadowed.clear();
	}

	@Override
	public void run() {
		mT -= 5;
		if (mT <= 0 && mCanRun) {
			mT = mCoolDown;
			spawnPortal();
		}
	}

	private void spawnPortal() {
		//clear portal loc list
		mPortalLoc.clear();

		World world = mBoss.getWorld();
		mByPortal.clear();
		List<Player> players = Lich.playersInRange(mSpawnLoc, mRange, true);

		if (mShadowed != null && mShadowed.size() > 0) {
			players.removeAll(mShadowed);
		}
		List<Player> toRemove = new ArrayList<Player>();
		for (Player p : players) {
			p.sendMessage(ChatColor.LIGHT_PURPLE + "THE SHADOWS HOLD MANY SECRETS.");
			if (Lich.getCursed().contains(p) || PlayerUtils.isCursed(com.playmonumenta.plugins.Plugin.getInstance(), p)) {
				p.sendMessage(ChatColor.AQUA + "I can cleanse the curse on me if I enter the shadows.");
			}
			if (p.getLocation().getY() < mSpawnLoc.getY() - 8) {
				toRemove.add(p);
			}
		}
		players.removeAll(toRemove);

		List<Player> targets = new ArrayList<Player>();
		if (players.size() <= 2) {
			targets = players;
		} else {
			int cap = (int) Math.min(mCap, Math.ceil(players.size() / 2));
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
					mChargeUp.setTitle(ChatColor.YELLOW + "Dimensional Door Remaining Time");
					mChargeUp.setColor(BarColor.RED);
					new BukkitRunnable() {
						int mT = 0;

						@Override
						public void run() {
							mT++;
							double progress = 1.0d - mT / (20.0d * 30.0d);
							if (progress >= 0 && !Lich.phase3over()) {
								mChargeUp.setProgress(progress);
								mChargeUp.setColor(BarColor.RED);
							} else {
								this.cancel();
								mCanRun = true;
								mChargeUp.reset();
								mChargeUp.setTitle(ChatColor.YELLOW + "Casting Dimension Door...");
								mChargeUp.setColor(BarColor.YELLOW);
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

			PPGroundCircle indicator = new PPGroundCircle(Particle.SPELL_WITCH, pLoc, 3, 0.2, 0, 0.2, 0).init(3, true);
			PPGroundCircle indicator2 = new PPGroundCircle(Particle.SMOKE_NORMAL, pLoc, 2, 0.2, 0, 0.2, 0).init(3, true);

			List<BlockState> toRestore = new ArrayList<>();
			BukkitRunnable runB = new BukkitRunnable() {
				int mT = 0;
				List<Player> mTeleport = new ArrayList<>();
				Location mLoc = p.getLocation();
				@Override
				public void run() {
					mT++;

					//move portal center to ground, stop above bedrock so it doesn't replace bedrock
					Location locdown = mLoc.clone().subtract(0, 1, 0);
					while ((mLoc.getBlock().isPassable() || mLoc.getBlock().isLiquid()
							|| mLoc.getBlock().isEmpty()) && locdown.getBlock().getType() != Material.BEDROCK
							&& mLoc.getY() > mSpawnLoc.getY() - 5 && mT <= 5) {
						mLoc.setY(mLoc.getY() - 1);
						locdown = mLoc.clone().subtract(0, 1, 0);
					}

					if (mT <= 25) {
						indicator.location(mLoc.clone().add(0, 1.1, 0)).spawnAsBoss();
						indicator2.location(mLoc.clone().add(0, 1.1, 0)).spawnAsBoss();
					}

					//get blocks and replace
					Location portalCenterLoc = mLoc.clone();
					Location testloc = portalCenterLoc.clone();
					List<Block> replace = new ArrayList<Block>();
					if (mT == 25) {
						mPortalLoc.add(portalCenterLoc);
						mTeleport = Lich.playersInRange(mSpawnLoc, mRange, true);
						world.playSound(mLoc, Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 1.0f, 1.0f);
						//get blocks 5x5
						for (int x = -2; x <= 2; x++) {
							testloc.setX(portalCenterLoc.getX() + x);
							for (int z = -2; z <= 2; z++) {
								testloc.setZ(portalCenterLoc.getZ() + z);

								//check if testloc is already in set or is end portal
								if (!mIgnoredMats.contains(testloc.getBlock().getType()) && !mReplaceLoc.contains(testloc.getBlock().getLocation())) {
									toRestore.add(testloc.getBlock().getState());
									replace.add(testloc.getBlock());
									mReplaceLoc.add(testloc.getBlock().getLocation());
								}
							}
						}
						//repeat for 7x3
						for (int x = -3; x <= 3; x++) {
							testloc.setX(portalCenterLoc.getX() + x);
							for (int z = -1; z <= 1; z++) {
								testloc.setZ(portalCenterLoc.getZ() + z);

								//check if testloc is already in set or is end portal
								if (!mIgnoredMats.contains(testloc.getBlock().getType()) && !mReplaceLoc.contains(testloc.getBlock().getLocation())) {
									toRestore.add(testloc.getBlock().getState());
									replace.add(testloc.getBlock());
									mReplaceLoc.add(testloc.getBlock().getLocation());
								}
							}
						}
						//repeat for 3x7
						for (int x = -1; x <= 1; x++) {
							testloc.setX(portalCenterLoc.getX() + x);
							for (int z = -3; z <= 3; z++) {
								testloc.setZ(portalCenterLoc.getZ() + z);

								//check if testloc is already in set or is end portal
								if (!mIgnoredMats.contains(testloc.getBlock().getType()) && !mReplaceLoc.contains(testloc.getBlock().getLocation())) {
									toRestore.add(testloc.getBlock().getState());
									replace.add(testloc.getBlock());
									mReplaceLoc.add(testloc.getBlock().getLocation());
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
							testloc.setX(portalCenterLoc.getX() + x);
							for (int z = -1; z <= 1; z++) {
								testloc.setZ(portalCenterLoc.getZ() + z);
								if (testloc.getBlock().getType() == Material.BLACK_CONCRETE) {
									testloc.getBlock().setType(Material.END_PORTAL);
								}
							}
						}
						for (int x = -1; x <= 1; x++) {
							testloc.setX(portalCenterLoc.getX() + x);
							for (int z = -2; z <= 2; z++) {
								testloc.setZ(portalCenterLoc.getZ() + z);
								if (testloc.getBlock().getType() == Material.BLACK_CONCRETE) {
									testloc.getBlock().setType(Material.END_PORTAL);
								}
							}
						}
					}

					/*
					 * for 30 seconds, keep portal open and teleport players to shadow realm for 10 seconds
					 * players who got in are immune to teleport until the portal closes
					 */

					mTeleport.removeIf(p -> mByPortal.contains(p));
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
		p.teleport(tele);
		p.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 5, 0));
		mShadowed.add(p);

		String[] dio = new String[] {
				"There's something moving in the darkness! It looks like... me?!",
				"This darkness is dangerous. I need to find a way out!",
				"What is that pulsating mass in the center of the arena?"
				};

		//do different stuff for different entry method
		int t = 20 * 20;
		if (byPortal) {
			if (mWarned.contains(p)) {
				p.sendMessage(ChatColor.AQUA + "" + dio[FastUtils.RANDOM.nextInt(3)]);
			} else {
				p.sendMessage(ChatColor.AQUA + "" + dio[0]);
				mWarned.add(p);
			}
			//remove curse only through portal
			if (PlayerUtils.isCursed(com.playmonumenta.plugins.Plugin.getInstance(), p)) {
				PlayerUtils.removeCursed(com.playmonumenta.plugins.Plugin.getInstance(), p);
				p.sendMessage(ChatColor.AQUA + "You felt a curse being lifted.");
			}
			if (Lich.getCursed().contains(p)) {
				Lich.getCursed().remove(p);
			}
		} else {
			t = 20 * 10;
			DamageUtils.damage(mBoss, p, DamageType.OTHER, 1);
			p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.HOSTILE, 1, 1);
			AbilityUtils.increaseDamageDealtPlayer(p, 20 * 30, -0.2, "Lich");
			Lich.cursePlayer(plugin, p);
		}
		int tick = t;

		//mirror location summon spectre
		Vector vec = LocationUtils.getVectorTo(tele, shadowLoc);
		Location spectreLoc = shadowLoc.clone().subtract(vec);
		LivingEntity spectre = Lich.summonSpectre(p, spectreLoc);
		String name = p.getName();
		spectre.setCustomName(ChatColor.GOLD + name);
		spectre.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(200);
		spectre.setHealth(200);
		if (spectre.getScoreboardTags().contains("boss_shieldswitch")) {
			spectre.setCustomName(ChatColor.WHITE + name);
			spectre.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(150);
			spectre.setHealth(150);
		}
		spectre.setGlowing(true);
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team join Hekawt " + spectre.getUniqueId());
		((Creature) spectre).setTarget(p);

		BossBar bar = Bukkit.getServer().createBossBar(null, BarColor.PURPLE, BarStyle.SOLID, BarFlag.PLAY_BOSS_MUSIC);
		bar.setTitle(ChatColor.YELLOW + "Soul dissipating in " + tick / 20 + " seconds!");
		bar.setVisible(true);
		bar.addPlayer(p);

		new BukkitRunnable() {
			int mT = tick;

			@Override
			public void run() {
				mT -= 2;
				p.spawnParticle(Particle.SPELL_WITCH, spectre.getEyeLocation(), 1, 0.1, 0.1, 0.1, 0);
				double progress = mT * 1.0d / tick;
				if (progress >= 0) {
					bar.setProgress(progress);
				}
				if (progress <= 0.34) {
					bar.setColor(BarColor.RED);
				} else if (progress <= 0.67) {
					bar.setColor(BarColor.YELLOW);
				}
				if (mT % 20 == 0) {
					bar.setTitle(ChatColor.YELLOW + "Soul dissipating in " + mT / 20 + " seconds!");
				}
				//kill player if time runs out. show that they are dying extremely quickly
				if (mT <= 0) {
					BossUtils.bossDamagePercent(mBoss, p, 0.1);
				}

				if (spectre.isDead() || !spectre.isValid() || Lich.phase3over() || mBoss.isDead() || !mBoss.isValid()) {
					bar.setVisible(false);
					bar.removeAll();
					this.cancel();
					Location leaveLoc = tLoc.clone().add(0, 1.5, 0);
					mShadowed.remove(p);
					if (byPortal) {
						p.teleport(leaveLoc);
						p.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 5, 0));
						p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 5, 10));
						p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 5, 0));
						p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 5, 1));
						p.sendMessage(ChatColor.AQUA + "Something feels different. The shadows aren't clinging to me anymore.");
					} else {
						p.teleport(leaveLoc);
						DamageUtils.damage(mBoss, p, DamageType.OTHER, 1);
						p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.HOSTILE, 1, 1);
					}
				}
				if (p.getLocation().getY() > Lich.getLichSpawn().getY() - 10 || !Lich.playersInRange(shadowLoc, 60, true).contains(p)) {
					mShadowed.remove(p);
					bar.setVisible(false);
					bar.removeAll();
					this.cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 2);
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}

}
