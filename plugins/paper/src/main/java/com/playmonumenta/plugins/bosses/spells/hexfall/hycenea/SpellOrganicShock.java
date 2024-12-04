package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellOrganicShock extends Spell {
	private static final String ABILITY_NAME = "Organic Shock";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final double mMinRadius;
	private final double mRadius;
	private final int mDamage;
	private final int mCastTime;
	private final int mCooldown;
	private final Location mSpawnLoc;
	private final List<Block> mChangedBlocks = new ArrayList<>();

	public SpellOrganicShock(Plugin plugin, LivingEntity boss, double minRadius, double radius, int damage, int castTime, int cooldown, Location mSpawnLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mMinRadius = minRadius;
		mRadius = radius;
		mDamage = damage;
		mCastTime = castTime;
		mCooldown = cooldown;
		this.mSpawnLoc = mSpawnLoc;
	}

	@Override
	public void run() {

		World world = mBoss.getWorld();

		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {

				if (mT % 15 == 0) {
					for (double degree = 0; degree < 360; degree += 4) {
						for (double r = mMinRadius; r < mRadius; r += 0.5) {
							double rad = Math.toRadians(degree);
							Vector vec = new Vector(FastUtils.cos(rad) * r, 0, FastUtils.sin(rad) * r);
							Location l = mSpawnLoc.clone().subtract(0, 1, 0).add(vec);

							if (l.getBlock().getType() != Material.OAK_LEAVES && l.getBlock().getType() != Material.AIR) {
								if (FastUtils.RANDOM.nextInt(15) == 0) {
									if (TemporaryBlockChangeManager.INSTANCE.changeBlock(l.getBlock(), Material.OAK_LEAVES, mCastTime)) {
										mChangedBlocks.add(l.getBlock());
									}

									if (degree % 4 == 0) {
										world.playSound(l, Sound.BLOCK_AZALEA_LEAVES_PLACE, 0.75f, 1f);
										world.playSound(l, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.01f, 0.75f);
									}
								}
							}
						}
					}
				}

				if (mT >= mCastTime) {
					TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(mChangedBlocks, Material.OAK_LEAVES);
					mChangedBlocks.clear();

					for (double rad = mMinRadius; rad < mRadius; rad += 0.5) {
						new PPCircle(Particle.BLOCK_CRACK, mSpawnLoc, rad).data(Material.OAK_LEAVES.createBlockData()).ringMode(true).count(5).spawnAsBoss();
						new PPCircle(Particle.BLOCK_CRACK, mSpawnLoc, rad).data(Material.OAK_WOOD.createBlockData()).ringMode(true).count(5).spawnAsBoss();
					}

					world.playSound(mBoss.getLocation(), Sound.BLOCK_VINE_BREAK, SoundCategory.HOSTILE, 1.5f, 1);
					world.playSound(mBoss.getLocation(), Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 1.5f, 1);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 1.5f, 0.5f);
					List<Player> hitPlayers = HexfallUtils.playersInBossInXZRange(mSpawnLoc, mRadius, true);
					hitPlayers.removeAll(HexfallUtils.playersInBossInXZRange(mSpawnLoc, mMinRadius, true));
					for (Player player : hitPlayers) {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, mDamage, null, true, true, ABILITY_NAME);
						MovementUtils.knockAway(mBoss.getLocation(), player, 0f, 1f, false);
					}

					this.cancel();
				}
				mT++;
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
