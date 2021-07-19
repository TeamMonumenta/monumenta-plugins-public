package com.playmonumenta.plugins.depths.bosses.spells;

import java.util.Collections;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Davey;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellVoidGrenades extends Spell {
	private static final Particle.DustOptions BLACK_COLOR = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.0f);
	private static final Particle.DustOptions DARK_PURPLE_COLOR = new Particle.DustOptions(Color.fromRGB(97, 5, 105), 1.0f);
	private static final int DURATION = 8 * 20;

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private double mRange;
	private int mCooldownTicks;

	public SpellVoidGrenades(Plugin plugin, LivingEntity entity, double range, int cooldown) {
		mPlugin = plugin;
		mBoss = entity;
		mRange = range;
		mCooldownTicks = cooldown;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		// Choose random player within range that has line of sight to boss
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mRange, false);
		players.removeIf(player -> player.getLocation().distance(mBoss.getLocation()) <= 3);
		if (players.isEmpty()) {
			return;
		}
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), Davey.detectionRange, "tellraw @s [\"\",{\"text\":\"[Davey]\", \"color\":\"gold\"},{\"text\":\" Arr! Who be needin' Varcosa's cannons when She sends me this!\",\"color\":\"blue\"}]");

		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				mTicks++;

				world.spawnParticle(Particle.SOUL, mBoss.getLocation(), 40, 0, 0, 0, 0.1);
				world.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 35, 0, 0, 0, 0.1);
				world.playSound(mBoss.getLocation(), Sound.BLOCK_DISPENSER_LAUNCH, 3, 0.75f);
				Collections.shuffle(players);
				for (Player player : players) {
					if (LocationUtils.hasLineOfSight(mBoss, player)) {
						launch(player);
						break;
					}
				}
				if (mTicks >= 3) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 15);
	}

	public void launch(Player target) {
		Location sLoc = mBoss.getLocation();
		sLoc.setY(sLoc.getY() + 1.7f);
		try {
			FallingBlock fallingBlock = sLoc.getWorld().spawnFallingBlock(sLoc, Material.CRYING_OBSIDIAN.createBlockData());
			fallingBlock.setDropItem(false);

			Location pLoc = target.getLocation();
			Location tLoc = fallingBlock.getLocation();
			Vector vect = new Vector(pLoc.getX() - tLoc.getX(), 0, pLoc.getZ() - tLoc.getZ());
			vect.normalize().multiply((pLoc.distance(tLoc)) / 20).setY(0.7f);
			fallingBlock.setVelocity(vect);

			new BukkitRunnable() {
				World mWorld = mBoss.getWorld();
				@Override
				public void run() {
					mWorld.spawnParticle(Particle.SOUL, fallingBlock.getLocation().add(0, fallingBlock.getHeight() / 2, 0), 1, 0.25, .25, .25, 0.025);
					mWorld.spawnParticle(Particle.SPELL_WITCH, fallingBlock.getLocation().add(0, fallingBlock.getHeight() / 2, 0), 1, 0.25, .25, .25, 0.025);
					if (fallingBlock.isOnGround() || !fallingBlock.isValid()) {
						fallingBlock.remove();
						fallingBlock.getLocation().getBlock().setType(Material.AIR);
						mWorld.spawnParticle(Particle.SOUL, fallingBlock.getLocation(), 150, 0, 0, 0, 0.165);
						mWorld.spawnParticle(Particle.SPELL_WITCH, fallingBlock.getLocation(), 65, 0, 0, 0, 0.1);
						mWorld.spawnParticle(Particle.EXPLOSION_LARGE, fallingBlock.getLocation(), 1, 0, 0, 0, 0);
						mWorld.playSound(fallingBlock.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2, 0.85f);

						for (Player player : PlayerUtils.playersInRange(fallingBlock.getLocation(), 3, true)) {
							BossUtils.bossDamage(mBoss, player, 40);
							// Shields don'mTicks stop fire!
						}

						new BukkitRunnable() {
							int mTicks = 0;
							Location mLoc = fallingBlock.getLocation();
							@Override
							public void run() {
								mTicks += 2;
								Location alternateHeight = mLoc.clone();
								alternateHeight.setY(mLoc.getY() + 0.5);
								mWorld.spawnParticle(Particle.SOUL, alternateHeight, 10, 1, 0.15, 1, 0.025);
								mWorld.spawnParticle(Particle.REDSTONE, alternateHeight, 10, 1, 0.15, 1, BLACK_COLOR);
								for (double deg = 0; deg < 360; deg += 10) {
									mWorld.spawnParticle(Particle.REDSTONE, mLoc.clone().add(3 * FastUtils.cosDeg(deg), 0.5, 3 * FastUtils.sinDeg(deg)), 1, 0, 0, 0, DARK_PURPLE_COLOR);
									mWorld.spawnParticle(Particle.REDSTONE, mLoc.clone().add(3 * FastUtils.cosDeg(deg), 0.25, 3 * FastUtils.sinDeg(deg)), 1, 0, 0, 0, DARK_PURPLE_COLOR);
								}

								if (mTicks % 10 == 0) {
									for (Player player : PlayerUtils.playersInRange(fallingBlock.getLocation(), 3, true)) {
										/* Fire aura can not be blocked */
										BossUtils.bossDamagePercent(mBoss, player, 0.1, (Location)null);
									}
								}

								if (mBoss.isDead() || mTicks >= DURATION) {
									this.cancel();
								}
							}

						}.runTaskTimer(mPlugin, 10, 2);
						this.cancel();


					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		} catch (Exception e) {
			mPlugin.getLogger().warning("Failed to summon grenade for hellzone grenade toss: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}

}
