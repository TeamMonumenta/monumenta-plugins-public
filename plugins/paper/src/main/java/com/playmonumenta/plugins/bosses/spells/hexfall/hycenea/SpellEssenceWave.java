package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.bosses.bosses.hexfall.HarrakfarGodOfLife;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPBezier;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellEssenceWave extends Spell {

	private static final String ABILITY_NAME = "Essence Wave";
	private final Plugin mPlugin;
	private final LivingEntity mBlue;
	private final int mCastTime;
	private final int mDamage;
	private final int mCooldown;
	private final int mConeRange;
	private final double mAngle;
	private final double mLockInPercentage;
	private final Location mBlueSpawnLoc;

	public SpellEssenceWave(Plugin plugin, LivingEntity mBlue, int castTime, int damage, int cooldown, int coneRange, int angle, double lockInPercentage, Location mBlueSpawnLoc) {
		this.mPlugin = plugin;
		this.mBlue = mBlue;
		this.mCastTime = castTime;
		this.mDamage = damage;
		this.mCooldown = cooldown;
		this.mConeRange = coneRange;
		this.mAngle = angle;
		this.mLockInPercentage = lockInPercentage;
		this.mBlueSpawnLoc = mBlueSpawnLoc;
	}

	@Override
	public void run() {

		List<Player> players = HexfallUtils.getPlayersInHycenea(mBlueSpawnLoc);

		if (!players.isEmpty()) {
			List<Location> coneStartLocs = new ArrayList<>();
			List<Vector> vecs = new ArrayList<>();

			for (Player player : players) {
				Location loc = mBlueSpawnLoc.clone();
				coneStartLocs.add(loc);
				vecs.add(LocationUtils.getDirectionTo(LocationUtils.fallToGround(player.getLocation(), mBlue.getLocation().getY()), loc));
			}

			BukkitRunnable runnable = new BukkitRunnable() {

				final List<BoundingBox> mBoxes = new ArrayList<>();
				int mT = 0;

				@Override
				public void run() {

					if (mBlue.getHealth() / EntityUtils.getAttributeBaseOrDefault(mBlue, Attribute.GENERIC_MAX_HEALTH, HarrakfarGodOfLife.mHealth) <= 0.5) {
						this.cancel();
					}

					if (mT % 5 == 0) {
						boolean moveOrLock;
						moveOrLock = mT <= mCastTime * mLockInPercentage;

						BlockData data = moveOrLock ? Material.LAPIS_BLOCK.createBlockData() : Material.FIRE_CORAL_BLOCK.createBlockData();

						if (mT % 20 != 0 || !moveOrLock) {
							for (int i = 0; i < players.size(); i++) {

								if ((double) mT / mCastTime <= mLockInPercentage) {
									Location yAdjust = players.get(i).getLocation().clone();
									yAdjust.setY(mBlueSpawnLoc.getY());
									vecs.set(i, LocationUtils.getDirectionTo(yAdjust, coneStartLocs.get(i)));
								}

								Location loc1 = coneStartLocs.get(i).clone().add(vecs.get(i).clone().rotateAroundY(Math.toRadians(-mAngle / 2)).multiply(mConeRange));
								Location loc2 = coneStartLocs.get(i).clone().add(vecs.get(i).clone().rotateAroundY(Math.toRadians(mAngle / 2)).multiply(mConeRange));

								new PPLine(Particle.BLOCK_DUST, coneStartLocs.get(i), loc1)
									.countPerMeter(4)
									.data(data)
									.spawnAsBoss();

								new PPLine(Particle.BLOCK_DUST, coneStartLocs.get(i), loc2)
									.countPerMeter(4)
									.data(data)
									.spawnAsBoss();

								List<Location> endpoints = Arrays.asList(
									loc1,
									coneStartLocs.get(i).clone().add(vecs.get(i).clone().multiply(mConeRange + 5)),
									loc2
								);

								new PPBezier(Particle.BLOCK_DUST, endpoints)
									.data(data)
									.count(10)
									.spawnAsBoss();

								if (!moveOrLock) {
									Vector rotate = vecs.get(i).clone().rotateAroundY(Math.toRadians(-mAngle / 2));
									for (double degree = 0; degree < mAngle; degree += 1) {
										rotate.rotateAroundY(Math.toRadians(1));

										new PPLine(Particle.BLOCK_DUST, coneStartLocs.get(i), rotate.clone().normalize(), mConeRange)
											.data(data)
											.countPerMeter(0.25)
											.spawnAsBoss();
									}
								}
							}

							for (Player player : players) {
								player.playSound(player.getLocation(), moveOrLock ? Sound.BLOCK_CHAIN_BREAK : Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, moveOrLock ? 2f : 0.5f, moveOrLock ? 1f : 2f);
							}
						}
					}

					if (mT++ >= mCastTime) {

						for (int i = 0; i < players.size(); i++) {
							vecs.get(i).rotateAroundY(Math.toRadians(-mAngle / 2));
							for (double degree = 0; degree < mAngle; degree += 1) {
								vecs.get(i).rotateAroundY(Math.toRadians(1));

								for (int r = 0; r < mConeRange; r++) {
									mBoxes.add(BoundingBox.of(coneStartLocs.get(i).clone().add(vecs.get(i).clone().normalize().multiply(r).setY(0)), 0.85, 15, 0.85));
								}

								new PPLine(Particle.SQUID_INK, coneStartLocs.get(i), vecs.get(i).clone().normalize(), mConeRange)
									.countPerMeter(0.5)
									.spawnAsBoss();
							}
						}

						Set<Player> playersHit = new HashSet<>();
						for (Player p : players) {
							for (BoundingBox box : mBoxes) {
								if (p.getBoundingBox().overlaps(box)) {
									playersHit.add(p);
								}
							}
						}

						for (Player player : playersHit) {
							DamageUtils.damage(mBlue, player, DamageEvent.DamageType.MAGIC, mDamage, null, true, true, ABILITY_NAME);
							player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1f, 1f);
							player.playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, SoundCategory.HOSTILE, 1f, 2f);
						}

						this.cancel();
					}
				}
			};
			runnable.runTaskTimer(mPlugin, 0, 1);
			mActiveRunnables.add(runnable);
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
