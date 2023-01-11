package com.playmonumenta.plugins.bosses.spells.headlesshorseman;

import com.playmonumenta.plugins.bosses.bosses.HeadlessHorsemanBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/*
 * (Ultimate) Hallow’s End - A pillar of smoke and flames appears on the horseman, after 1 second the area
nearby explodes, dealing 20/35 damage to players in a 4 block radius, igniting them for 8 seconds and
launching them upwards greatly. Afterwards pillars appear underneath ⅓ of the players within 32 blocks of
the horseman, after 1 second they also explode dealing the same thing. This continues to repeat as long as a
player is dealt damage by the pillars explosion to a max of 5 casts of the skill. (In hard mode players
it are also blinded for 5 seconds.)
 */

public class SpellHallowsEnd extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private boolean mHit;
	private boolean mCooldown;
	private int mCooldownTicks;
	private HeadlessHorsemanBoss mHorseman;

	public SpellHallowsEnd(Plugin plugin, LivingEntity entity, int cooldown, HeadlessHorsemanBoss horseman) {
		mPlugin = plugin;
		mBoss = entity;
		mHit = false;
		mCooldown = false;
		mHorseman = horseman;
		mCooldownTicks = cooldown;
	}

	private void pillar(Location loc, boolean bounce) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.HOSTILE, 2, 0.85f);
		BukkitRunnable run = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				if (mTicks % 2 == 0) {
					for (int i = 0; i < 15; i++) {
						new PartialParticle(Particle.FLAME, loc.clone().add(0, i, 0), 3, 0.2, 0.2, 0.2, 0.05).spawnAsEntityActive(mBoss);
					}
				}

				if (mTicks >= 25) {
					this.cancel();
					for (int i = 0; i < 15; i++) {
						new PartialParticle(Particle.SMOKE_NORMAL, loc.clone().add(0, i, 0), 5, 0.2, 0.2, 0.2, 0.1).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.FLAME, loc.clone().add(0, i, 0), 8, 0.2, 0.2, 0.2, 0.125).spawnAsEntityActive(mBoss);
					}

					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0.75f);
					new PartialParticle(Particle.FLAME, loc, 75, 0, 0, 0, 0.15).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SMOKE_LARGE, loc, 25, 0, 0, 0, 0.1).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SMOKE_NORMAL, loc, 50, 0, 0, 0, 0.15).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);
					for (Player player : PlayerUtils.playersInRange(loc, 3.5, true)) {
						if (mHorseman.getSpawnLocation().distance(player.getLocation()) < HeadlessHorsemanBoss.detectionRange) {
							int mNDT = player.getNoDamageTicks();
							player.setNoDamageTicks(0);
							DamageUtils.damage(mBoss, player, DamageType.MAGIC, 50, null, false, true, "Hallow's End");
							EntityUtils.applyFire(com.playmonumenta.plugins.Plugin.getInstance(), 20 * 8, player, mBoss);
							player.setNoDamageTicks(mNDT);
							MovementUtils.knockAway(loc, player, 0.50f, 1.5f);
							if (bounce) {
								mHit = true;
							}
						}
					}
				}
			}
		};
		run.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(run);
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

		BukkitRunnable run = new BukkitRunnable() {
			boolean mInit = false;
			int mTicks = 0;
			int mHits = 0;

			@Override
			public void run() {
				mTicks++;

				if (!mInit) {
					mInit = true;
					List<Player> players = PlayerUtils.playersInRange(mHorseman.getSpawnLocation(), HeadlessHorsemanBoss.detectionRange, true);
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
					mTicks = 0;
					mHits++;
					List<Player> players = PlayerUtils.playersInRange(mHorseman.getSpawnLocation(), HeadlessHorsemanBoss.detectionRange, true);
					Collections.shuffle(players);

					int amt = players.size() / 3 + 2;
					if (players.size() < 3) {
						amt = players.size();
					}

					for (int i = 0; i < amt; i++) {
						pillar(players.get(i).getLocation(), true);
					}
				}

				if (mTicks > 30 || mHits >= 5) {
					this.cancel();
				}
			}
		};
		run.runTaskTimer(mPlugin, 25, 1);
		mActiveRunnables.add(run);
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

}
