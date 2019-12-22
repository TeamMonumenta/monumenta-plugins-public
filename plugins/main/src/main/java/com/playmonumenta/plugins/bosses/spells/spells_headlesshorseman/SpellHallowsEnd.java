package com.playmonumenta.plugins.bosses.spells.spells_headlesshorseman;

import java.util.Collections;
import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.bosses.HeadlessHorsemanBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.utils.DamageUtils;
import com.playmonumenta.plugins.bosses.utils.Utils;

/*
 * (Ultimate) Hallow’s End - A pillar of smoke and flames appears on the horseman, after 1 second the area
nearby explodes, dealing 20/35 damage to players in a 4 block radius, igniting them for 8 seconds and
launching them upwards greatly. Afterwards pillars appear underneath ⅓ of the players within 32 blocks of
the horseman, after 1 second they also explode dealing the same thing. This continues to repeat as long as a
player is dealt damage by the pillars explosion to a max of 8 casts of the skill. (In hard mode players
it are also blinded for 5 seconds.)
 */

public class SpellHallowsEnd extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private boolean mHit;
	private boolean mCooldown;
	private HeadlessHorsemanBoss mHorseman;
	public SpellHallowsEnd(Plugin plugin, LivingEntity entity, HeadlessHorsemanBoss horseman) {
		mPlugin = plugin;
		mBoss = entity;
		mHit = false;
		mCooldown = false;
		mHorseman = horseman;
	}

	private void pillar(Location loc, boolean bounce) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 2, 0.85f);
		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				t++;
				for (int i = 0; i < 15; i++) {
					world.spawnParticle(Particle.FLAME, loc.clone().add(0, i, 0), 3, 0.2, 0.2, 0.2, 0.05);
				}

				if (t >= 20) {
					this.cancel();
					for (int i = 0; i < 15; i++) {
						world.spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(0, i, 0), 10, 0.2, 0.2, 0.2, 0.1);
						world.spawnParticle(Particle.FLAME, loc.clone().add(0, i, 0), 15, 0.2, 0.2, 0.2, 0.125);
					}

					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.75f);
					world.spawnParticle(Particle.FLAME, loc, 90, 0, 0, 0, 0.15);
					world.spawnParticle(Particle.SMOKE_LARGE, loc, 35, 0, 0, 0, 0.1);
					world.spawnParticle(Particle.SMOKE_NORMAL, loc, 65, 0, 0, 0, 0.15);
					world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);
					for (Player player : Utils.playersInRange(loc, 4)) {
						if (mHorseman.getSpawnLocation().distance(player.getLocation()) < HeadlessHorsemanBoss.detectionRange
								&& player.getGameMode() == GameMode.SURVIVAL) {
							DamageUtils.damage(mBoss, player, 35);
							player.setFireTicks(20 * 8);
							Utils.KnockAway(loc, player, 0.50f, 1.5f);
							if (bounce) {
								mHit = true;
							}
						}
					}
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void run() {
		mCooldown = true;
		new BukkitRunnable() {

			@Override
			public void run() {
				mCooldown = false;
			}

		}.runTaskLater(mPlugin, 20 * 35);
		mHit = false;
		pillar(mBoss.getLocation(), false);

		new BukkitRunnable() {
			boolean init = false;
			int t = 0;
			int hits = 0;
			@Override
			public void run() {
				t++;

				if (!init) {
					init = true;
					List<Player> players = Utils.playersInRange(mHorseman.getSpawnLocation(), HeadlessHorsemanBoss.detectionRange);
					Collections.shuffle(players);

					int amt = players.size() / 3;
					if (players.size() < 3) {
						amt = players.size();
					}

					for (int i = 0; i < amt; i++) {
						pillar(players.get(i).getLocation(), true);
					}
				}

				if (mHit) {
					mHit = false;
					t = 0;
					hits++;
					List<Player> players = Utils.playersInRange(mHorseman.getSpawnLocation(), HeadlessHorsemanBoss.detectionRange);
					Collections.shuffle(players);

					int amt = players.size() / 3;
					if (players.size() < 3) {
						amt = players.size();
					}

					for (int i = 0; i < amt; i++) {
						pillar(players.get(i).getLocation(), true);
					}
				}

				if (t >= 25 || hits >= 5) {
					this.cancel();
				}


			}

		}.runTaskTimer(mPlugin, 20, 1);
	}

	@Override
	public int duration() {
		// TODO Auto-generated method stub
		return 20 * 12;
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

}
