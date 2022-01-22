package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PPGroundCircle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/*
Grasping Hands - Pools of "hands" appear under ½  players in a 5 block radius. Players inside
it after 1 second(s) take 10% max health damage every 0.5s second(s) and are given slowness 3
and negative jump boost for 4 seconds. “Minions” in the pools are healed for 5% max hp every 0.5 seconds
while in a pool.

-maybe change to spoopy's house of jump scare boss hand attack
 */
public class SpellGraspingHands extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private ThreadLocalRandom mRand = ThreadLocalRandom.current();
	private int mCap = 7;
	private static final Particle.DustOptions GRASPING_HANDS_COLOR = new Particle.DustOptions(Color.fromRGB(0, 135, 96), 1.65f);
	private ChargeUpManager mChargeUp;
	private boolean mCanRun = true;
	private double mDuration = 20 * 7.0d;

	public SpellGraspingHands(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mChargeUp = new ChargeUpManager(mBoss, 110, ChatColor.YELLOW + "Channeling Grasping Hands...", BarColor.YELLOW, BarStyle.SOLID, 50);
	}

	@Override
	public void run() {
		mCanRun = false;
		World world = mBoss.getWorld();
		List<Player> players = Lich.playersInRange(mBoss.getLocation(), 50, true);
		players.removeIf(p -> SpellDimensionDoor.getShadowed().contains(p));
		List<Player> targets = new ArrayList<Player>();
		if (players.size() <= 2) {
			targets = players;
		} else {
			int cap = (int) Math.min(mCap, Math.ceil(players.size() / 3));
			for (int i = 0; i < cap; i++) {
				Player player = players.get(mRand.nextInt(players.size()));
				if (targets.contains(player)) {
					cap++;
				} else {
					targets.add(player);
				}
			}
		}
		//boss bar
		BukkitRunnable runA = new BukkitRunnable() {

			@Override
			public void run() {
				if (mChargeUp.nextTick()) {
					this.cancel();
					mChargeUp.setTitle(ChatColor.YELLOW + "Casting Grasping Hands...");
					mChargeUp.setColor(BarColor.RED);
					BukkitRunnable runB = new BukkitRunnable() {
						int mT = 0;

						@Override
						public void run() {
							mT++;
							double progress = 1.0d - mT / mDuration;
							if (progress >= 0) {
								mChargeUp.setProgress(progress);
							} else {
								this.cancel();
								mChargeUp.reset();
								mChargeUp.setTitle(ChatColor.YELLOW + "Charging Grasping Hands...");
							}
							if (Lich.phase3over()) {
								this.cancel();
								mChargeUp.reset();
								mChargeUp.setTitle(ChatColor.YELLOW + "Charging Grasping Hands...");
							}
						}

					};
					runB.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(runB);
				}
			}

		};
		runA.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runA);

		for (Player player : targets) {
			player.playSound(player.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, SoundCategory.HOSTILE, 1, 0.75f);
			player.sendMessage(ChatColor.AQUA
	                   + "A pool forms under you.");
			world.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 1, 0.5f);
			world.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 1, 0.75f);

			PPGroundCircle indicator = new PPGroundCircle(Particle.REDSTONE, player.getLocation(), 8, 0.25, 0.1, 0.25, 0, GRASPING_HANDS_COLOR).init(0, true);
			PPGroundCircle indicator2 = new PPGroundCircle(Particle.SMOKE_NORMAL, player.getLocation(), 6, 0.2, 0, 0.2, 0).init(0, true);
			PPGroundCircle indicator3 = new PPGroundCircle(Particle.DRAGON_BREATH, player.getLocation(), 4, 0.25, 0.1, 0.25, mRand.nextDouble(0.01, 0.05)).init(0, true);

			BukkitRunnable runC = new BukkitRunnable() {
				int mT = 0;
				Location mLoc = player.getLocation();
				double mRadius = 5;
				@Override
				public void run() {
					mT++;
					if (mT <= 20 * 4) {
						mLoc = player.getLocation();
					}

					indicator.radius(mRadius).location(mLoc).spawnAsBoss();
					indicator2.radius(mRadius).location(mLoc).spawnAsBoss();

					if (mT >= 20 * 5.5) {
						world.playSound(player.getLocation(), Sound.ENTITY_SPIDER_STEP, SoundCategory.HOSTILE, 1, 0.75f);
						world.playSound(player.getLocation(), Sound.ENTITY_SPIDER_STEP, SoundCategory.HOSTILE, 1, 0.5f);
						world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 1, 0.75f);
						BukkitRunnable runD = new BukkitRunnable() {
							int mT = 0;
							@Override
							public void run() {
								mT++;
								if (mT % 10 == 0) {
									//damage the player
									for (Player p : Lich.playersInRange(mLoc, 5.0, true)) {
										Vector v = p.getVelocity();
										BossUtils.bossDamagePercent(mBoss, p, 0.15, p.getLocation(), "Grasping Hands");
										p.setVelocity(v);
										p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 4, 2));
										p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 4, -4));
									}
									//heal mobs except boss
									List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mLoc, 5.0, 3.0, 5.0);
									for (LivingEntity e : mobs) {
										if (!(e.getType() == EntityType.PLAYER) && !e.isDead()) {
											double maxHealth = EntityUtils.getMaxHealth(e);
											double restore = e.getHealth() + maxHealth * 0.01 + 3;
											if (restore >= maxHealth) {
												e.setHealth(maxHealth);
											} else {
												e.setHealth(restore);
											}
										}
									}
								}

								indicator.radius(mRadius).location(mLoc).spawnAsBoss();
								indicator2.radius(mRadius).location(mLoc).spawnAsBoss();

								if (mT % 4 == 0) {
									for (double r = 1; r < mRadius; r++) {
										indicator3.radius(r).location(mLoc).spawnAsBoss();
									}
								}
								if (mT >= mDuration) {
									this.cancel();
									mCanRun = true;
								}
							}

						};
						runD.runTaskTimer(mPlugin, 0, 1);
						mActiveRunnables.add(runD);
						this.cancel();
					}
				}

			};
			runC.runTaskTimer(mPlugin, 0, 1);
			mActiveRunnables.add(runC);
		}
	}

	@Override
	public boolean canRun() {
		return mCanRun;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 8;
	}

}
