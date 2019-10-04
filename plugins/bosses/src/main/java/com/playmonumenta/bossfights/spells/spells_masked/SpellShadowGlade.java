package com.playmonumenta.bossfights.spells.spells_masked;

import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.utils.Utils;

public class SpellShadowGlade extends Spell {

	private final Plugin mPlugin;
	private final int mCount;
	private final Location mLoc;
	private final Random mRand = new Random();
	private int j;

	public SpellShadowGlade(Plugin plugin, Location loc, int count) {
		mPlugin = plugin;
		mLoc = loc;
		mCount = count;
	}

	@Override
	public void run() {
		j = 0;
		boolean[] isQuadrantDone = new boolean[4];
		Location[] possibleLocs = new Location[4];
		int i = 0;
		for (int x = 0; x < 2; x++) {
			for (int y = 0; y < 2; y++) {
				possibleLocs[i] = new Location(mLoc.getWorld(), mLoc.getX() - 8.25 + x * 12.5, mLoc.getY() + 0.5, mLoc.getZ() - 8.25 + y * 12.5);
				i++;
			}
		}
		int chosen;
		int count = mCount;
		while (count > 0) {
			chosen = mRand.nextInt(4);
			if (!isQuadrantDone[chosen]) {
				count--;
				isQuadrantDone[chosen] = true;
				animation(possibleLocs[chosen]);
				damage(possibleLocs[chosen]);
			}
		}
	}

	@Override
	public int duration() {
		return 200; // 10 seconds
	}

	public void animation(Location zoneStart) {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		List<Player> pList = Utils.playersInRange(zoneStart, 40);

		Runnable anim_loop = new Runnable() {
			@Override
			public void run() {
				for (Player player : pList) {
					Location pPos = player.getLocation();
					if (pPos.getX() > zoneStart.getX() - 8.25 && pPos.getX() < zoneStart.getX() + 8.25 && pPos.getZ() > zoneStart.getZ() - 8.25 && pPos.getZ() < zoneStart.getZ() + 8.25) {
						pPos.getWorld().playSound(pPos, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1f, 0.5f);
					}
				}
				zoneStart.getWorld().spawnParticle(Particle.FLAME, zoneStart, (j / mCount) * 10, 4, 0, 4, 0.01);
				if (j / mCount >= 24) {
					for (Player player : pList) {
						Location pPos = player.getLocation();
						pPos.getWorld().playSound(pPos, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 0.8f);
					}
					zoneStart.getWorld().spawnParticle(Particle.LAVA, zoneStart, (j / mCount) * 10, 4, 0, 4, 0.01);
				}
				j++;
			}
		};

		for (int i = 0; i < 35; i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, anim_loop, i * 4);
		}
	}

	public void damage(Location zoneStart) {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		List<Player> pList = Utils.playersInRange(zoneStart, 40);

		Runnable burst = new Runnable() {
			@Override
			public void run() {
				for (Player player : pList) {
					Location pPos = player.getLocation();
					if (pPos.getX() > zoneStart.getX() - 8.25 && pPos.getX() < zoneStart.getX() + 8.25 && pPos.getZ() > zoneStart.getZ() - 8.25 && pPos.getZ() < zoneStart.getZ() + 8.25) {
						pPos.getWorld().playSound(pPos, Sound.ENTITY_GHAST_HURT, 1f, 0.7f);
						player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 7 * 20, 3));
						player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 7 * 20, 1));
						player.setFireTicks(20 * 7);
					}
				}
			}
		};
		scheduler.scheduleSyncDelayedTask(mPlugin, burst, 140L);
	}
}
