package com.playmonumenta.bossfights.spells.spells_kaul;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.utils.Utils;

/*
 * Raise Jungle/Earth Elementals (dirt): In random spots in the arena,
 * Earth Elementals start rising slowly from below the ground.
 * While they are partially stuck in the ground, they are vulnerable
 * to melee attacks, but they have a very high level of projectile
 * protection. After 40 seconds, they are no longer stuck in the ground
 * and they can move around freely. They are extremely strong and fast,
 * strongly encouraging players to kill them while they are still stuck
 * in the ground. (The number of elementals spawned is equivalent to 2*
 * the number of players.)
 */

public class SpellRaiseJungle extends Spell {
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private double mSummonRange;
	private double mDetectRange;
	private int mSummonTime;
	private double mY;
	private List<UUID> summoned = new ArrayList<UUID>();
	private final String elemental = "{CustomName:\"{\\\"text\\\":\\\"Earth Elemental\\\"}\",Health:80.0f,ArmorItems:[{id:\"minecraft:leather_boots\",Count:1b,tag:{display:{color:8683051,Name:\"{\\\"text\\\":\\\"§fHobnailed Boots\\\"}\"},Damage:0}},{id:\"minecraft:leather_leggings\",Count:1b,tag:{display:{color:8683051,Name:\"{\\\"text\\\":\\\"§fHobnailed Leggings\\\"}\"},Damage:0}},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:8683051,Name:\"{\\\"text\\\":\\\"§fHobnailed Vest\\\"}\"},Damage:0}},{id:\"minecraft:coarse_dirt\",Count:1b,tag:{Enchantments:[{lvl:6s,id:\"minecraft:projectile_protection\"}],AttributeModifiers:[{UUIDMost:6302698651954335388L,UUIDLeast:-8389639952570264411L,Amount:14.0d,Slot:\"head\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"},{UUIDMost:-1762668789229140628L,UUIDLeast:-8025770187519780738L,Amount:0.385d,Slot:\"head\",AttributeName:\"generic.movementSpeed\",Operation:1,Name:\"Modifier\"}]}}],Attributes:[{Base:80.0d,Name:\"generic.maxHealth\"}]}";

	private int mCooldown;
	private boolean onCooldown = false;
	public SpellRaiseJungle(Plugin plugin, LivingEntity boss, double summonRange, double detectRange, int summonTime, int cooldown) {
		mPlugin = plugin;
		mBoss = boss;
		mSummonRange = summonRange;
		mDetectRange = detectRange;
		mSummonTime = summonTime;
		mCooldown = cooldown;
		mY = -1;
	}

	public SpellRaiseJungle(Plugin plugin, LivingEntity boss, double summonRange, double detectRange, int summonTime, int cooldown, double y) {
		mPlugin = plugin;
		mBoss = boss;
		mSummonRange = summonRange;
		mDetectRange = detectRange;
		mSummonTime = summonTime;
		mCooldown = cooldown;
		mY = y;
	}

	@Override
	public void run() {
		onCooldown = true;
		Location loc = mBoss.getLocation();
		if (mY > 0) {
			loc.setY(mY);
		}
		List<Player> players = Utils.playersInRange(loc, mDetectRange);
		players.removeIf(p -> p.getLocation().getY() >= 61);
		int num = 0;
		if (players.size() == 1) {
			num = 4;
		} else if (players.size() < 5) {
			num += 3 * players.size();
		} else if (players.size() < 11) {
			num += 12 + (2 * (players.size() - 4));
		} else if (players.size() >= 11) {
			num += 24 + (1 * (players.size() - 10));
		}
		int amt = num;
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		new BukkitRunnable() {

			@Override
			public void run() {
				for (int i = 0; i < amt; i++) {
					double x = rand.nextDouble(-mSummonRange, mSummonRange);
					double z = rand.nextDouble(-mSummonRange, mSummonRange);
					Location sLoc = loc.clone().add(x, 0.25, z);
					while (sLoc.getBlock().getType().isSolid() || sLoc.getBlock().isLiquid()) {
						x = rand.nextDouble(-mSummonRange, mSummonRange);
						z = rand.nextDouble(-mSummonRange, mSummonRange);
						sLoc = loc.clone().add(x, 0.25, z);
					}
					Location spawn = sLoc.clone().subtract(0, 1.75, 0);
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "summon minecraft:husk " + spawn.getX() + " " + spawn.getY() + " " + spawn.getZ() + " " + elemental);
					LivingEntity element = null;
					for (Entity e : spawn.getWorld().getNearbyEntities(spawn, 0.4, 0.4, 0.4)) {
						if (e instanceof LivingEntity && !(e instanceof Player) && e instanceof Zombie && !summoned.contains(e.getUniqueId())) {
							element = (LivingEntity) e;
							break;
						}
					}
					LivingEntity ele = element;
					Location scLoc = sLoc.clone();
					if (!summoned.contains(ele.getUniqueId())) {
						summoned.add(ele.getUniqueId());
						ele.setAI(false);
						new BukkitRunnable() {
							int t = 0;
							Location pLoc = scLoc;
							double yinc = 1.6 / ((mSummonTime));
							boolean raised = false;
							@Override
							public void run() {
								t++;

								if (!raised) {
									pLoc.getWorld().spawnParticle(Particle.BLOCK_DUST, pLoc, 2, 0.25, 0.1, 0.25, 0.25, Material.COARSE_DIRT.createBlockData());
									ele.teleport(ele.getLocation().add(0, yinc, 0));
								}

								if (t >= mSummonTime && !raised) {
									raised = true;
									ele.setAI(true);
									pLoc.getWorld().spawnParticle(Particle.BLOCK_DUST, pLoc, 20, 0.25, 0.1, 0.25, 0.25, Material.COARSE_DIRT.createBlockData());
								}

								if (mBoss.isDead() || !mBoss.isValid()) {
									ele.setHealth(0);
									this.cancel();
									return;
								}

								if (ele.isDead() || !ele.isValid() || ele == null) {
									this.cancel();
									summoned.remove(ele.getUniqueId());
									if (summoned.size() <= 0) {
										new BukkitRunnable() {

											@Override
											public void run() {
												onCooldown = false;
											}

										}.runTaskLater(mPlugin, mCooldown);
									}
								}
							}

						}.runTaskTimer(mPlugin, 0, 1);
					}

					new BukkitRunnable() {
						int t = 0;
						@Override
						public void run() {
							t++;
							if (t % 5 == 0) {
								for (Player player : players) {
									player.playSound(player.getLocation(), Sound.BLOCK_GRAVEL_HIT, 0.75f, 0.5f);
								}
							}

							if (mSummonTime <= t && !summoned.isEmpty()) {
								for (Player player : players) {
									player.playSound(player.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 1, 1f);
								}
								this.cancel();
							}

							if (summoned.isEmpty()) {
								this.cancel();
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}
			}

		}.runTaskLater(mPlugin, 20);
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		if (summoned.size() > 0) {
			event.setDamage(event.getDamage() * 0.4);
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_GRAVEL_HIT, 1, 0.5f);
			mBoss.getWorld().spawnParticle(Particle.BLOCK_DUST, mBoss.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.25, Material.DIRT.createBlockData());
		}
	};

	@Override
	public boolean canRun() {
		return summoned.size() <= 0 && !onCooldown;
	}

	@Override
	public int duration() {
		return mSummonTime + (20 * 18);
	}

	@Override
	public int castTime() {
		return mSummonTime;
	}

}
