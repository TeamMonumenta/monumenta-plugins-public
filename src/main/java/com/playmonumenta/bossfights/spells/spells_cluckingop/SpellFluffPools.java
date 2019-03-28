package com.playmonumenta.bossfights.spells.spells_cluckingop;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.utils.Utils;

public class SpellFluffPools extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private int mRange;

	public SpellFluffPools(Plugin plugin, LivingEntity boss, int range) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
	}

	@Override
	public void run() {
		List<Player> players = Utils.playersInRange(mBoss.getLocation(), mRange);
		List<Player> targets = new ArrayList<Player>();
		if (players.size() <= 3) {
			targets = players;
		} else {
			//Too lazy to do a random
			targets.add(players.get(0));
			targets.add(players.get(1));
			targets.add(players.get(2));
		}
		for (Player player : targets) {
			fluff(player.getLocation());
		}
	}

	private void fluff(Location loc) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1, 0.5f);
		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				t++;
				world.spawnParticle(Particle.CLOUD, loc, 1, 1, 0.1, 1, 0.1);

				if (t >= 20) {
					this.cancel();
					world.spawnParticle(Particle.CLOUD, loc, 250, 0, 0, 0, 0.3);
					world.playSound(loc, Sound.ITEM_TOTEM_USE, 1, 1f);
					new BukkitRunnable() {
						int i = 0;
						@Override
						public void run() {
							i += 2;
							for (Player player : Utils.playersInRange(loc, 3)) {
								player.damage(1, mBoss);
								player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 5, -6));
							}
							world.spawnParticle(Particle.CLOUD, loc, 35, 3, 0.1, 3, 0.1);
							if (i >= 20 * 8) {
								this.cancel();
							}
						}

					}.runTaskTimer(mPlugin, 0, 2);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int duration() {
		// TODO Auto-generated method stub
		return 20 * 12;
	}

}
