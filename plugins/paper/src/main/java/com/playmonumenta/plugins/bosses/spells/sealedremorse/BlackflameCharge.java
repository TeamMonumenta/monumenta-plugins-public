package com.playmonumenta.plugins.bosses.spells.sealedremorse;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import com.playmonumenta.plugins.bosses.bosses.BeastOfTheBlackFlame;
import com.playmonumenta.plugins.bosses.bosses.Ghalkor;
import com.playmonumenta.plugins.bosses.spells.SpellBaseCharge;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class BlackflameCharge extends SpellBaseCharge {

	private static final int DAMAGE = 30;
	private static final int GROUND_DAMAGE = 20;
	private static final int FIRE_DURATION = 20 * 4;

	private BeastOfTheBlackFlame mBossClass;

	public BlackflameCharge(Plugin plugin, LivingEntity boss, BeastOfTheBlackFlame bossClass) {
		super(plugin, boss, 20, 20 * 6, 10, false,
				// Warning sound/particles at boss location and slow boss
				(Player player) -> {
					boss.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, boss.getLocation(), 50, 2, 2, 2, 0);
					boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4));
					boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.75f);
					boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.15f);
					boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_VINDICATOR_AMBIENT, 1f, 0.85f);
				},
				// Warning particles
				(Location loc) -> {
					loc.getWorld().spawnParticle(Particle.CRIT, loc, 2, 0.65, 0.65, 0.65, 0);
				},
				// Charge attack sound/particles at boss location
				(Player player) -> {
					boss.getWorld().spawnParticle(Particle.SMOKE_NORMAL, boss.getLocation(), 125, 0.4, 0.4, 0.4, 0.25);
					boss.getWorld().spawnParticle(Particle.CLOUD, boss.getLocation(), 45, 0.15, 0.4, 0.15, 0.15);
					boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.75f);
					boss.getWorld().playSound(boss.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 0.9f);
					boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.4f);
				},
				// Attack hit a player
				(Player player) -> {
					player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 5, 0.4,
							0.4, 0.4, 0.4, Material.REDSTONE_BLOCK.createBlockData());
					player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 12, 0.4,
							0.4, 0.4, 0.4, Material.REDSTONE_WIRE.createBlockData());
					BossUtils.bossDamage(boss, player, DAMAGE, boss.getLocation(), "Blackflame Charge");
				},
				// Attack particles
				(Location loc) -> {
					loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 4, 0.5, 0.5, 0.5, 0.075);
					loc.getWorld().spawnParticle(Particle.CRIT, loc, 8, 0.5, 0.5, 0.5, 0.75);
					loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 12, 0.5, 0.5, 0.5, 0.2);

					List<Player> players = PlayerUtils.playersInRange(boss.getLocation(), Ghalkor.detectionRange, true);
					World world = boss.getWorld();

					//Damaging trail left behind
					new BukkitRunnable() {
						private int mT = 0;
						private BoundingBox mHitbox = new BoundingBox().shift(loc).expand(1);
						private Location mParticleLoc = loc.clone().subtract(0, 1, 0);
						@Override
						public void run() {
							if (mT >= 20 * 5 || boss.isDead() || !boss.isValid()) {
								this.cancel();
							}

							if (mT % 20 == 0) {
								world.playSound(mParticleLoc, Sound.BLOCK_BEACON_DEACTIVATE, 0.025f, 0f);
								world.spawnParticle(Particle.SQUID_INK, mParticleLoc, 1, 0.6, 0.2, 0.6, 0.02);
							}
							world.spawnParticle(Particle.SMOKE_LARGE, mParticleLoc, 1, 0.6, 0.2, 0.6, 0);
							world.spawnParticle(Particle.ASH, mParticleLoc, 1, 0.6, 0.4, 0.6, 0);

							for (Player player : players) {
								if (mHitbox.overlaps(player.getBoundingBox())) {
									world.playSound(mParticleLoc, Sound.ENTITY_BLAZE_HURT, 0.5f, 0f);
									player.setFireTicks(FIRE_DURATION);
									NmsUtils.unblockableEntityDamageEntity(player, GROUND_DAMAGE, boss);
								}
							}

							mT += 10;
						}
					}.runTaskTimer(plugin, 0, 10);
				},
				// Ending particles on boss
				() -> {
					boss.getWorld().spawnParticle(Particle.SMOKE_NORMAL, boss.getLocation(), 125, 0.4, 0.4, 0.4, 0.25);
					boss.getWorld().spawnParticle(Particle.CLOUD, boss.getLocation(), 45, 0.15, 0.4, 0.15, 0.15);
					boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.1f, 1.5f);
					boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.75f);
					boss.getWorld().playSound(boss.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 0.9f);
					boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.4f);
				});
		mBossClass = bossClass;
	}

	@Override
	public int cooldownTicks() {
		return (int) (6 * 20 * mBossClass.mCastSpeed);
	}

}
