package com.playmonumenta.bossfights.bosses.gray;

import java.util.Arrays;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.bossfights.SpellManager;
import com.playmonumenta.bossfights.bosses.BossAbilityGroup;
import com.playmonumenta.bossfights.spells.SpellBaseSummon;
import com.playmonumenta.bossfights.utils.Utils;

public abstract class GraySwarmSummonerBase extends BossAbilityGroup {
	private static final int SUMMON_TIME = 300;
	private static final int TIME_BETWEEN_CASTS = 800;
	private static final int SUMMON_MAX_TIME = 300;
	private static final int SUMMON_PARTICLE_DELAY = 20;
	private static final int PLAYER_RADIUS = 7;
	private static final int SPAWNS_PER_PLAYER = 8;
	private static final Random mRand = new Random();

	GraySwarmSummonerBase(Plugin plugin, LivingEntity boss, String identityTag, int detectionRange, String mobType, String mobNBT) throws Exception {
		if (!(boss instanceof Mob)) {
			throw new Exception("gray boss tags only work on mobs!");
		}

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseSummon(plugin, SUMMON_TIME, TIME_BETWEEN_CASTS, PLAYER_RADIUS, SPAWNS_PER_PLAYER, false,
				() -> {
					// Run on all nearby players
					//TODO: Logarithmic instead?
					return Utils.playersInRange(boss.getLocation(), 20);
				},
				(summonLoc, player) -> {
					BukkitRunnable runnable = new BukkitRunnable() {
						@Override
						public void run() {
							summonLoc.getWorld().spawnParticle(Particle.PORTAL, summonLoc, 10, 0, 0, 0, 0.4);

							new BukkitRunnable() {
								@Override
								public void run() {
									try {
										summonLoc.getWorld().playSound(summonLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.4f);
										Entity entity = Utils.summonEntityAt(summonLoc, mobType, mobNBT);
										if (entity != null && entity instanceof Mob) {
											Mob mob = (Mob)entity;
											mob.setTarget(player);
										} else {
											plugin.getLogger().warning("Summoned mob but got something other than mob back!");
										}
									} catch (Exception ex) {
										ex.printStackTrace();
									}
								}
							}.runTaskLater(plugin, SUMMON_PARTICLE_DELAY);
						}
					};
					runnable.runTaskLater(plugin, mRand.nextInt(SUMMON_MAX_TIME));
					return runnable;
				},
				() -> {
					boss.getLocation().getWorld().playSound(boss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_WOLOLO, SoundCategory.HOSTILE, 1.0f, 1.0f);
					BukkitRunnable runnable = new BukkitRunnable() {
						int mTicks = 0;

						@Override
						public void run() {
							mTicks++;

							if (mTicks < SUMMON_TIME) {
								((Mob)boss).setTarget(null);

								//TODO: Helix
								Location mobLoc = boss.getLocation();
								mobLoc.getWorld().spawnParticle(Particle.SPELL_INSTANT, mobLoc, 2, 0.5, 0.5, 0.5, 0);
							} else {
								this.cancel();
							}
						}
					};

					runnable.runTaskTimer(plugin, 1, 1);

					return runnable;
				}
			)
		));

		super.constructBoss(plugin, identityTag, boss, activeSpells, null, detectionRange, null);
	}
}

