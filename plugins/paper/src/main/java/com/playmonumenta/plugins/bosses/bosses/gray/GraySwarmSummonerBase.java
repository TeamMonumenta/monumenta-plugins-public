package com.playmonumenta.plugins.bosses.bosses.gray;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSummon;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public abstract class GraySwarmSummonerBase extends BossAbilityGroup {
	private static final int SUMMON_TIME = 200;
	private static final int TIME_BETWEEN_CASTS = 700;
	private static final int SUMMON_MAX_TIME = 300;
	private static final int SUMMON_PARTICLE_DELAY = 20;
	private static final int PLAYER_RADIUS = 7;
	private static final int SPAWNS_PER_PLAYER = 8;
	private static final int PLAYER_RANGE = 32;
	private static final int MAX_NEARBY_SUMMONS = 25;

	GraySwarmSummonerBase(Plugin plugin, LivingEntity boss, String identityTag, int detectionRange, String mobName) throws Exception {
		super(plugin, identityTag, boss);
		if (!(boss instanceof Mob)) {
			throw new Exception("gray boss tags only work on mobs!");
		}

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseSummon(plugin, SUMMON_TIME, TIME_BETWEEN_CASTS, PLAYER_RADIUS, SPAWNS_PER_PLAYER, false,
				() -> {
					// Run on some number of nearby players. Scale a bit below linear to avoid insane spam
					List<Player> targets = PlayerUtils.playersInRange(boss.getLocation(), 20);
					Collections.shuffle(targets);
					switch (targets.size()) {
					case 0:
					case 1:
					case 2:
						return targets;
					case 3:
					case 4:
						targets.remove(0);
						return targets;
					case 5:
					case 6:
						targets.remove(0);
						targets.remove(0);
						return targets;
					case 7:
					case 8:
						targets.remove(0);
						targets.remove(0);
						targets.remove(0);
						return targets;
					default:
						return targets.subList(0, 5);
					}
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
										Entity entity = LibraryOfSoulsIntegration.summon(summonLoc, mobName);
										if (entity != null && entity instanceof Mob) {
											Mob mob = (Mob)entity;
											mob.setTarget(player);
										} else {
											plugin.getLogger().warning("Summoned mob but got something other than mob back!");
										}
									} catch (Exception ex) {
										ex.printStackTrace();
									}
									this.cancel();
								}
							}.runTaskLater(plugin, SUMMON_PARTICLE_DELAY);
						}
					};
					runnable.runTaskLater(plugin, FastUtils.RANDOM.nextInt(SUMMON_MAX_TIME));
					return runnable;
				},
				() -> {
					boss.getLocation().getWorld().playSound(boss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_WOLOLO, SoundCategory.HOSTILE, 1.5f, 1.0f);
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
				},
				() -> {
					List<Entity> nearbyEntities = boss.getNearbyEntities(PLAYER_RANGE, PLAYER_RANGE, PLAYER_RANGE);

					if (nearbyEntities.stream().filter(
							e -> e.getScoreboardTags().contains(GraySummoned.identityTag)
						).count() > MAX_NEARBY_SUMMONS) {
						return false;

					}

					if (((boss instanceof Mob) && (((Mob)boss).getTarget() instanceof Player))) {
						return true;
					}

					for (Player player : PlayerUtils.playersInRange(boss.getLocation(), PLAYER_RANGE)) {
						if (LocationUtils.hasLineOfSight(boss, player)) {
							return true;
						}
					}

					return false;
				}
			)
		));

		super.constructBoss(activeSpells, null, detectionRange, null);
	}
}

