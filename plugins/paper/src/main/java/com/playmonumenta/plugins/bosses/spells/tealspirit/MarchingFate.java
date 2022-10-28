package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.warlock.reaper.VoodooBonds;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.TealSpirit;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.Stasis;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class MarchingFate extends Spell {
	private static final double DISTANCE = 21;
	private static final double HEIGHT = 2;
	private static final double PROXIMITY = 4;
	private static final String LOS = "MarchingFate";
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final TealSpirit mTealSpirit;
	private final List<Entity> mMarchers = new ArrayList<>();

	private final ChargeUpManager mBossBar;
	private boolean mSentMessage = false;
	private HashMap<Entity, Location> mSpawnIndex = new HashMap<>();
	private int mT = 0;
	private boolean mHasRun = false;

	public MarchingFate(LivingEntity boss, TealSpirit tealSpirit, boolean isHard) {
		mBoss = boss;
		mCenter = tealSpirit.mSpawnLoc;
		mTealSpirit = tealSpirit;

		mMarchers.add(LibraryOfSoulsIntegration.summon(mCenter.clone().add(DISTANCE, HEIGHT, 0), LOS));
		mMarchers.add(LibraryOfSoulsIntegration.summon(mCenter.clone().add(-DISTANCE, HEIGHT, 0), LOS));

		if (isHard) {
			mMarchers.add(LibraryOfSoulsIntegration.summon(mCenter.clone().add(0, HEIGHT, DISTANCE), LOS));
			mMarchers.add(LibraryOfSoulsIntegration.summon(mCenter.clone().add(0, HEIGHT, -DISTANCE), LOS));
		}

		for (Entity e : mMarchers) {
			mSpawnIndex.put(e, e.getLocation());
		}

		tealSpirit.setMarchers(mMarchers);

		mBossBar = new ChargeUpManager(mCenter, mBoss, 10000, ChatColor.DARK_AQUA + "Marching Fates", BarColor.PURPLE, BarStyle.SOLID, TealSpirit.detectionRange);
	}

	@Override
	public void run() {
		World world = mCenter.getWorld();

		double stepLength = 0;
		double currentBossHP = mBoss.getHealth();
		double bossMaxHP = EntityUtils.getMaxHealth(mBoss);
		double percentHP = currentBossHP / bossMaxHP;
		if (percentHP <= 0.3) {
			stepLength = 0.1;
		} else if (percentHP <= 0.6) {
			stepLength = 0.075;
		} else if (percentHP <= 0.9) {
			stepLength = 0.05;
		} else if (percentHP <= 1) {
			stepLength = 0.025;
		}

		List<Double> distances = new ArrayList<>();
		int obfuscation = 0;
		// Move marchers towards the center and kill party if they reach the center
		for (Entity marcher : mMarchers) {
			if (marcher.isDead() || !marcher.isValid()) {
				break;
			}
			Location loc = marcher.getLocation();
			Vector step = mCenter.toVector().subtract(loc.toVector()).setY(0).normalize().multiply(stepLength);
			Location newLoc = loc.clone().add(step).setDirection(step);
			double distance = newLoc.distance(mCenter);
			if (marcher.getLocation().distance(mCenter) >= 25) {
				Location temp = mSpawnIndex.get(marcher);
				marcher.teleport(temp);
			}
			if (distance <= 0.6 && !mHasRun) {
				marcher.teleport(mCenter);
				world.spawnParticle(Particle.EXPLOSION_HUGE, mCenter.clone().add(0, 3, 0), 25, 11, 3, 11);
				world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, mCenter, 40, 17, 1, 17);
				world.spawnParticle(Particle.SOUL_FIRE_FLAME, mCenter.clone().add(0, 3, 0), 40, 17, 3, 17);
				world.playSound(mCenter, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.8f);
				world.playSound(mCenter, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
				world.playSound(mCenter, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);

				Plugin plugin = Plugin.getInstance();
				for (Player player : PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true)) {
					plugin.mEffectManager.clearEffects(player, Stasis.GENERIC_NAME);
					plugin.mEffectManager.clearEffects(player, VoodooBonds.EFFECT_NAME);
					PotionEffect resist = player.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
					if (resist != null && resist.getAmplifier() >= 4) {
						player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
					}
					player.setInvulnerable(false);
					player.setHealth(0);
					DamageUtils.damage(mBoss, player, DamageEvent.DamageType.TRUE, 10000, null, true, false, "Marching Fates");
				}

				mTealSpirit.killMarchers();
				mHasRun = true;
				mBossBar.reset();
				return;
			}

			distances.add(distance);

			newLoc = LocationUtils.fallToGround(newLoc, mCenter.getBlockY());
			marcher.teleport(newLoc);
		}

		if (mT % 10 == 0) {
			// Damage players if there are marchers too close to each other
			outer: for (Entity marcher : mMarchers) {
				Location loc = marcher.getLocation();
				if (loc.distance(mCenter) < PROXIMITY) {
					continue;
				}
				for (Entity e : mMarchers) {
					if (e == marcher) {
						continue;
					}
					Location eLoc = e.getLocation();
					double distance = eLoc.distance(loc);
					if (distance < PROXIMITY) {
						List<Player> players = PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true);
						for (Player player : players) {
							BossUtils.bossDamagePercent(mBoss, player, 0.5, null, "Crossed Fates");
						}

						if (!mSentMessage) {
							players.forEach(player -> player.sendMessage(ChatColor.RED + "The Fates are too close to each other!"));
							mSentMessage = true;
						}

						Vector between = LocationUtils.getVectorTo(eLoc, loc).normalize();
						for (double i = 0; i < distance; i += distance / 5) {
							world.spawnParticle(Particle.CRIT_MAGIC, loc.clone().add(between.clone().multiply(i)).add(0, 1.5, 0), 3, 0.2, 0.2, 0.2, 0);
						}

						obfuscation = 4;

						break outer;
					}
				}
			}

			if (mMarchers.size() > 0) {
				double minDistance = 25;
				for (Entity e : mMarchers) {
					if (e.getLocation().distance(mCenter) < minDistance) {
						minDistance = e.getLocation().distance(mCenter);
					}
				}
				mBossBar.setTime((int) (10000 * Math.min(minDistance / DISTANCE, 1)));
				mBossBar.setTitle(obfuscate("Marching Fates", obfuscation, ChatColor.DARK_AQUA));
				mBossBar.update();
			} else {
				mBossBar.remove();
			}
		}
		mT += 5;
	}

	private String obfuscate(String s, int num, ChatColor color) {
		StringBuilder result = new StringBuilder();
		result.append(color);
		char[] chars = s.toCharArray();
		int length = chars.length;
		List<Integer> places = new ArrayList<>();
		for (int i = 0; i < num; i++) {
			places.add(FastUtils.RANDOM.nextInt(length));
		}
		for (int i = 0; i < length; i++) {
			if (chars[i] != ' ' && places.contains(i)) {
				result.append(ChatColor.MAGIC).append(chars[i]).append(ChatColor.RESET).append(color);
			} else {
				result.append(chars[i]);
			}
		}
		return result.toString();
	}

	public void removeMarchers() {
		for (Entity e : mMarchers) {
			e.remove();
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
