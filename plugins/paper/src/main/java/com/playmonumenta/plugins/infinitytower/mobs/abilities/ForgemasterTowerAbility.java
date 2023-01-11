package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseCharge;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

public class ForgemasterTowerAbility extends TowerAbility {

	private static final int DAMAGE = 8;
	private static final int GROUND_DAMAGE = 2;
	private static final int FIRE_DURATION = 20 * 4;

	public ForgemasterTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);

		Spell spell = new SpellBaseCharge(
			plugin,
			boss,
			160,
			20,
			false,
			0,
			0,
			0,
			() -> {
				List<LivingEntity> list = (mIsPlayerMob ? mGame.getFloorMobs() : mGame.getPlayerMobs());
				Collections.shuffle(list);
				if (list.size() > 0) {
					return list.subList(0, 1);
				}
				return list;
			},
			(LivingEntity player) -> {
				new PartialParticle(Particle.VILLAGER_ANGRY, boss.getLocation(), 50, 2, 2, 2, 0).spawnAsEntityActive(mBoss);
				boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4));
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1f, 1.75f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1f, 1.15f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_VINDICATOR_AMBIENT, SoundCategory.HOSTILE, 1f, 0.85f);
			},
			// Warning particles
			(Location loc) -> {
				new PartialParticle(Particle.CRIT, loc, 2, 0.65, 0.65, 0.65, 0).spawnAsEntityActive(mBoss);
			},
			// Charge attack sound/particles at boss location
			(LivingEntity player) -> {
				new PartialParticle(Particle.SMOKE_NORMAL, boss.getLocation(), 125, 0.4, 0.4, 0.4, 0.25).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.CLOUD, boss.getLocation(), 45, 0.15, 0.4, 0.15, 0.15).spawnAsEntityActive(mBoss);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1f, 0.75f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 1f, 0.9f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1f, 1.4f);
			},
			// Attack hit a player
			(LivingEntity player) -> {
				player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 5, 0.4,
					0.4, 0.4, 0.4, Material.REDSTONE_BLOCK.createBlockData());
				player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 12, 0.4,
					0.4, 0.4, 0.4, Material.REDSTONE_WIRE.createBlockData());
				DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE);
			},
			// Attack particles
			(Location loc) -> {
				new PartialParticle(Particle.SWEEP_ATTACK, loc, 4, 0.5, 0.5, 0.5, 0.075).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.CRIT, loc, 8, 0.5, 0.5, 0.5, 0.75).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.SMOKE_NORMAL, loc, 12, 0.5, 0.5, 0.5, 0.2).spawnAsEntityActive(mBoss);

				World world = boss.getWorld();

				//Damaging trail left behind
				new BukkitRunnable() {
					private int mT = 0;
					private BoundingBox mHitbox = new BoundingBox().shift(loc).expand(0.5);
					private Location mParticleLoc = loc.clone().subtract(0, 1, 0);

					@Override
					public void run() {
						if (mT >= 20 * 5 || boss.isDead() || !boss.isValid()) {
							this.cancel();
						}

						if (mT % 20 == 0) {
							world.playSound(mParticleLoc, Sound.BLOCK_LAVA_EXTINGUISH, SoundCategory.HOSTILE, 0.025f, 0f);
							new PartialParticle(Particle.LAVA, mParticleLoc, 1, 0.3, 0.1, 0.3, 0.02).spawnAsEntityActive(mBoss);
						}
						new PartialParticle(Particle.SMOKE_LARGE, mParticleLoc, 1, 0.3, 0.1, 0.3, 0).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.DRIP_LAVA, mParticleLoc, 1, 0.3, 0.2, 0.3, 0).spawnAsEntityActive(mBoss);

						for (LivingEntity target : mIsPlayerMob ? mGame.getFloorMobs() : mGame.getPlayerMobs()) {
							if (mHitbox.overlaps(target.getBoundingBox())) {
								world.playSound(mParticleLoc, Sound.ENTITY_GENERIC_BURN, SoundCategory.HOSTILE, 0.5f, 1f);
								target.setFireTicks(FIRE_DURATION);
								DamageUtils.damage(mBoss, target, DamageEvent.DamageType.MAGIC, GROUND_DAMAGE, null, true);
							}
						}

						mT += 10;
					}
				}.runTaskTimer(plugin, 0, 10);
			},
			// Ending particles on boss
			() -> {
				new PartialParticle(Particle.SMOKE_NORMAL, boss.getLocation(), 125, 0.4, 0.4, 0.4, 0.25).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.CLOUD, boss.getLocation(), 45, 0.15, 0.4, 0.15, 0.15).spawnAsEntityActive(mBoss);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 0.1f, 1.5f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1f, 0.75f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 1f, 0.9f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1f, 1.4f);
			});

		SpellManager active = new SpellManager(List.of(spell));

		super.constructBoss(active, Collections.emptyList(), -1, null, (int) (FastUtils.RANDOM.nextDouble() * 100) + 20);
	}
}
