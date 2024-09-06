package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellEarthenCleave extends Spell {

	private static final String ABILITY_NAME = "Earthen Cleave";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private final double mRadius;
	private final int mDamage;
	private final int mCastTime;
	private final int mCooldown;
	private final int mYaw;

	private final List<Block> mChangedBlocks = new ArrayList<>();

	public SpellEarthenCleave(Plugin plugin, LivingEntity boss, Location spawnLoc, double range, int damage, int castTime, int cooldown, int yaw) {
		mPlugin = plugin;
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mRadius = range;
		mDamage = damage;
		mCastTime = castTime;
		mCooldown = cooldown;
		mYaw = yaw;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		int mInterval = mCastTime / 18;
		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;
			int mRad = 0;
			@Override
			public void run() {
				mT++;
				boolean isSoulSoil = true;
				if (mT % mInterval == 0 && mRad <= mRadius) {
					for (double degree = 165; degree <= 375; degree += 2) {
						double rad = Math.toRadians(degree);
						Vector vec = new Vector(FastUtils.cos(rad) * mRad, 0, FastUtils.sin(rad) * mRad);
						vec = VectorUtils.rotateYAxis(vec, mYaw);
						Location l = mSpawnLoc.clone().add(0, -1, 0).add(vec);

						Material type = isSoulSoil ? Material.SOUL_SOIL : Material.MUDDY_MANGROVE_ROOTS;
						Block block = l.getBlock();
						if (block.getType() != type && block.getType() != Material.AIR && block.getType() != Material.WATER) {
							if (TemporaryBlockChangeManager.INSTANCE.changeBlock(block, type, mCastTime)) {
								mChangedBlocks.add(block);
							}
						}

						if (degree % 3 == 0) {
							new PPExplosion(Particle.EXPLOSION_NORMAL, l.clone().add(0, 1, 0))
								.speed(1)
								.count(1)
								.extraRange(0.10, 0.2)
								.spawnAsBoss();
						}

						if (degree % 5 == 0 && mRad % 3 == 0) {
							world.playSound(l, Sound.BLOCK_SOUL_SOIL_BREAK, SoundCategory.HOSTILE, 0.15f, 1f);
							world.playSound(l, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.HOSTILE, 0.15f, 2f);
						}

						isSoulSoil = !isSoulSoil;
					}
					world.playSound(mBoss.getLocation(), Sound.BLOCK_SOUL_SOIL_BREAK, SoundCategory.HOSTILE, 0.15f, 1f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.HOSTILE, 0.15f, 2f);
					mRad += 1;
				}

				if (mT >= mCastTime) {
					TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(mChangedBlocks, Material.SOUL_SOIL);
					TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(mChangedBlocks, Material.MUDDY_MANGROVE_ROOTS);
					mChangedBlocks.clear();

					List<BoundingBox> boxes = new ArrayList<>();
					for (double degree = 165; degree < 375; degree += 4) {
						for (double r = 0; r < mRadius; r++) {
							double rad = Math.toRadians(degree);
							Vector vec = new Vector(FastUtils.cos(rad) * r, 0, FastUtils.sin(rad) * r);
							vec = VectorUtils.rotateYAxis(vec, mYaw);

							Location l = mSpawnLoc.clone().add(0, -1, 0).add(vec);
							boxes.add(BoundingBox.of(l, 0.65, 15, 0.65));

							if (degree % 4 == 0 && r % 2 == 0) {
								new PPExplosion(Particle.BLOCK_DUST, l.clone().add(0, 1, 0))
									.speed(1)
									.count(4)
									.extraRange(0.10, 0.2)
									.data(Material.SOUL_SOIL.createBlockData())
									.spawnAsBoss();
								new PPExplosion(Particle.EXPLOSION_NORMAL, l.clone().add(0, 1, 0))
									.speed(1)
									.count(1)
									.extraRange(0.10, 0.2)
									.spawnAsBoss();
							}
						}
					}

					world.playSound(mBoss.getLocation(), Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.HOSTILE, 0.25f, 2f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.3f, 0.5f);
					world.playSound(mBoss.getLocation(), Sound.BLOCK_SOUL_SOIL_BREAK, SoundCategory.HOSTILE, 0.3f, 1f);

					for (Player p : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						for (BoundingBox box : boxes) {
							if (p.getBoundingBox().overlaps(box)) {
								DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MELEE, mDamage, null, false, true, ABILITY_NAME);
								MovementUtils.knockAway(mBoss.getLocation(), p, 0f, 1f, false);
							}
						}
					}

					for (Entity e : mSpawnLoc.getNearbyEntities(mRadius, mRadius, mRadius)) {
						if (EntityUtils.isHostileMob(e) && e instanceof LivingEntity le) {
							for (BoundingBox box : boxes) {
								if (le.getBoundingBox().overlaps(box)) {
									MovementUtils.knockAway(mBoss.getLocation(), le, 0f, 1f, false);
								}
							}
						}
					}

					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
