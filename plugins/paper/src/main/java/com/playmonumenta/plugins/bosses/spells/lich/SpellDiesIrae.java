package com.playmonumenta.plugins.bosses.spells.lich;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;

/*
 * This isn't inspired by etrian odyssey 4 at all *heavy sarcasm*
 */

public class SpellDiesIrae extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private LivingEntity mKey;
	private Location mCenter;
	private double mRange;
	private int mCeiling;
	private List<Location> mCrystalLoc;
	private Collection<EnderCrystal> mCrystal = new ArrayList<EnderCrystal>();
	private String mCrystalNBT;
	private static boolean mActive = false;

	public SpellDiesIrae(Plugin plugin, LivingEntity boss, LivingEntity key, Location loc, double range, int ceil, List<Location> crystalLoc, String crystalnbt) {
		mPlugin = plugin;
		mBoss = boss;
		mKey = key;
		mCenter = loc;
		mRange = range;
		mCeiling = ceil;
		mCrystalLoc = crystalLoc;
		mCrystalNBT = crystalnbt;
	}

	public static boolean getActive() {
		return mActive;
	}

	public static void setActive(boolean active) {
		mActive = active;
	}

	@Override
	public void run() {
		mActive = true;
		World world = mBoss.getWorld();
		BossBar bar = Bukkit.getServer().createBossBar(null, BarColor.GREEN, BarStyle.SOLID, BarFlag.PLAY_BOSS_MUSIC);
		bar.setVisible(true);
		//calc the amount of crystal need to spawn
		List<Player> players = Lich.playersInRange(mCenter, mRange, true);
		double tospawn = Math.min(7, Math.sqrt(players.size())) + 1;

		Lich.spawnCrystal(mCrystalLoc, tospawn, mCrystalNBT);

		//tele lich to center + 10 blocks + invuln + no ai
		mBoss.teleport(mCenter.clone().add(0, 10, 0));
		mBoss.setAI(false);
		mBoss.setGravity(false);
		mBoss.setInvulnerable(true);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 3.0f, 0.5f);
		new PartialParticle(Particle.CLOUD, mBoss.getLocation(), 50, 0.1, 0.1, 0.1, 0.1).spawnAsBoss();

		//get all active crystals
		for (Location l : mCrystalLoc) {
			mCrystal.addAll(l.getNearbyEntitiesByType(EnderCrystal.class, 10));
		}

		//time limit 6s to break all active crystals
		BukkitRunnable runA = new BukkitRunnable() {
			double mT;
			int mCount = mCrystal.size();
			@Override
			public void run() {
				//keep boss in place in case of tp function
				mBoss.teleport(mCenter.clone().add(0, 10, 0));
				//glowy crystal to tell players to break
				if (mT == 0) {
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team modify crystal color white");
					for (EnderCrystal e : mCrystal) {
						e.setGlowing(true);
						e.setBeamTarget(mBoss.getLocation().add(0, 0, 0));
						UUID uuid = e.getUniqueId();
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team join crystal " + uuid);
					}
				}
				//exit function
				mCrystal.removeIf(en -> !en.isValid());
				if (mCrystal.size() == 0) {
					world.playSound(mBoss.getLocation(), Sound.BLOCK_GLASS_BREAK, 4.0f, 0.5f);
					Lich.bossGotHit(true);
					mBoss.setAI(true);
					mBoss.setGravity(true);
					mBoss.setInvulnerable(false);
					bar.setVisible(false);
					mActive = false;
					this.cancel();
				}
				//warning 1
				if (mT == 20 * 2) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 3.0f, 0.75f);
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team modify crystal color yellow");
				}
				//warning 2
				if (mT == 20 * 4) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 3.0f, 0.75f);
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team modify crystal color red");
				}
				//execute order 66
				if (mT >= 20 * 6) {
					bar.setVisible(false);
					attack();
					this.cancel();
				}
				mT++;
				//boss bar stuff
				int remain = mCrystal.size();
				double progress = remain * 1.0d / mCount;
				bar.setTitle(ChatColor.YELLOW + "" + remain + " Death Crystals Remaining!");
				bar.setProgress(progress);
				if (progress <= 0.34) {
					bar.setColor(BarColor.RED);
				} else if (progress <= 0.67) {
					bar.setColor(BarColor.YELLOW);
				}
				for (Player player : players) {
					if (player.getLocation().distance(mBoss.getLocation()) < mRange) {
						bar.addPlayer(player);
					} else {
						bar.removePlayer(player);
					}
				}
			}
		};
		runA.runTaskTimer(mPlugin, 20 * 1, 1);
		mActiveRunnables.add(runA);
	}

	private void attack() {
		World world = mBoss.getWorld();

		//healing and final damage calc
		double heal = mBoss.getHealth() + mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * mCrystal.size() * 0.025;
		double healthFinal = Math.min(heal, mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		double keyheal = mKey.getHealth() + mKey.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * mCrystal.size() * 0.025;
		double keyHealthFinal = Math.min(keyheal, mKey.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		double damage = Math.min(1, mCrystal.size() * 0.15 + 0.15);

		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 10.0f, 0.5f);
		//kill ghast shield
		for (Location loc : mCrystalLoc) {
			List<LivingEntity> enList = EntityUtils.getNearbyMobs(loc, 3);
			for (LivingEntity en : enList) {
				en.setHealth(0);
			}
		}
		//kill end crystals
		for (EnderCrystal e : mCrystal) {
			e.remove();
			new PartialParticle(Particle.EXPLOSION_HUGE, e.getLocation(), 1, 0, 0, 0, 0.1).spawnAsBoss();
			world.playSound(e.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 3f, 1f);
		}

		//heal boss + damage with delay (only once)
		BukkitRunnable runB = new BukkitRunnable() {
			int mInc = 0;
			@Override
			public void run() {
				mInc++;
				if (mInc < 20 * 2) {
					for (EnderCrystal e : mCrystal) {
						Location mStart = e.getLocation();
						Location mEnd = mBoss.getLocation().add(0, 1.5, 0);
						Vector vec = LocationUtils.getVectorTo(mEnd, mStart);

						Location particleLoc = mStart.add(vec.multiply(mInc / 40d));
						new PartialParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 2, 0, 0, 0, 0.03).spawnAsBoss();
						new PartialParticle(Particle.DRAGON_BREATH, particleLoc, 2, 0.1, 0.1, 0.1, 0).spawnAsBoss();
					}
				}
				if (mInc == 20 * 2) {
					new PartialParticle(Particle.HEART, mBoss.getLocation(), 20, 0.5, 0.5, 0.5, 0.1).spawnAsBoss();
					world.playSound(mBoss.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 3.0f, 1.5f);
					mBoss.setHealth(healthFinal);
					if (mKey.isValid()) {
						mKey.setHealth(keyHealthFinal);
					}
				}
				if (mInc == 20 * 3) {
					Location pitch = mBoss.getLocation();
					pitch.setPitch(90);
					mBoss.teleport(pitch);
				}
				if (mInc == 20 * 4) {
					world.playSound(mBoss.getLocation().clone(), Sound.ENTITY_WITHER_SPAWN, 10.0f, 0.5f);
				}
				if (mInc >= 20 * 4 && mInc < 20 * (4 + 1)) {
					Vector vec = LocationUtils.getVectorTo(mCenter, mBoss.getLocation()).multiply((mInc - 4 * 20) / 20d);
					Location pLoc = mBoss.getLocation();
					new PartialParticle(Particle.EXPLOSION_LARGE, pLoc.add(vec), 1, 0, 0, 0, 0).spawnAsBoss();
					new PartialParticle(Particle.DRAGON_BREATH, pLoc.add(vec), 15, 0.4, 0.4, 0.4, 0.01).spawnAsBoss();
				}
				if (mInc == 20 * (4 + 1)) {
					new PartialParticle(Particle.EXPLOSION_LARGE, mCenter.clone().add(0, 0.5, 0), mCrystal.size() * 125 + 1000, 42, 0.75, 42, 0).spawnAsBoss();
					new PartialParticle(Particle.DRAGON_BREATH, mCenter.clone(), mCrystal.size() * 1000 + 7000, 42, 0, 42, 0.01).spawnAsBoss();
					List<Player> players = Lich.playersInRange(mCenter, mRange, true);
					players.removeIf(pl -> SpellDimensionDoor.getShadowed().contains(pl) || pl.getLocation().getY() >= mCenter.getY() + mCeiling);
					for (Player p : players) {
						BossUtils.bossDamagePercent(mBoss, p, damage, null, "Dies Irae");
						world.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
					}
				}
				if (mInc >= 20 * (4 + 2)) {
					Location reset = mCenter.clone();
					reset.setPitch(0);
					Lich.bossGotHit(true);
					mBoss.teleport(reset);
					mBoss.setAI(true);
					mBoss.setGravity(true);
					mBoss.setInvulnerable(false);
					mActive = false;
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 3.0f, 0.5f);
					this.cancel();
				}
			}
		};
		runB.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runB);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 16;
	}

}
