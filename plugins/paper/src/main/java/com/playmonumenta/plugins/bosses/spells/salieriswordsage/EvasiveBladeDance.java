package com.playmonumenta.plugins.bosses.spells.salieriswordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.SalieriTheSwordsage;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class EvasiveBladeDance extends Spell {
	private final Plugin mPlugin;
	private final SalieriTheSwordsage mBossClass;
	private final LivingEntity mBoss;
	private final double mDamage;


	private static final int DANCE_RADIUS = 5;
	private static final double SLOWNESS_AMPLIFIER = 0.4;
	private static final double WEAKEN_AMP = 0.7;
	private static final int DURATION = 20 * 2;
	private static final Particle.DustOptions SWORDSAGE_COLOR = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.0f);

	public EvasiveBladeDance(Plugin plugin, LivingEntity boss, SalieriTheSwordsage bossClass, int damage) {
		mPlugin = plugin;
		mBoss = boss;
		mBossClass = bossClass;
		mDamage = damage;
	}

	@Override
	public void run() {
		mBossClass.mSpellActive = true;

		Location loc = mBoss.getLocation();
		World world = mBoss.getWorld();


		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, 1f, 0.75f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.5f);
		world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
		new PartialParticle(Particle.CLOUD, loc.clone().add(0, 1, 0), 20).delta(0.25, 0.5, 0.25).extra(0.15).spawnAsBoss();
		new PartialParticle(Particle.REDSTONE, loc.clone().add(0, 1, 0), 6).delta(0.45, 0.5, 0.45).data(SWORDSAGE_COLOR).spawnAsBoss();

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			float mPitch = 0.5f;

			@Override
			public void run() {
				mTicks += 1;
				double r = DANCE_RADIUS - (3 * mPitch);
				new PartialParticle(Particle.SWEEP_ATTACK, loc, 3).delta(r).spawnAsBoss();
				new PartialParticle(Particle.REDSTONE, loc, 4).delta(r).data(SWORDSAGE_COLOR).spawnAsBoss();
				new PartialParticle(Particle.CLOUD, loc, 4).delta(r).spawnAsBoss();
				if (mTicks % 2 == 0) {
					world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, mPitch);
					mPitch += 0.05f;
				}

				EntityUtils.selfRoot(mBoss, 30);

				if (mTicks >= 30) {

					world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 1);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 2f);
					world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 0.75f);

					for (Player player : PlayerUtils.playersInRange(loc, DANCE_RADIUS, true)) {
						BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MELEE_SKILL, mDamage);
						MovementUtils.knockAway(mBoss, player, 2f, 1f);

						EntityUtils.applySlow(mPlugin, DURATION, SLOWNESS_AMPLIFIER, player);
						EntityUtils.applyWeaken(mPlugin, DURATION, WEAKEN_AMP, player);

						Location mobLoc = player.getLocation().add(0, 1, 0);
						new PartialParticle(Particle.SWEEP_ATTACK, mobLoc, 5).delta(0.35, 0.5, 0.35).spawnAsBoss();
						new PartialParticle(Particle.CRIT, mobLoc, 10).delta(0.25, 0.5, 0.25).extra(0.3).spawnAsBoss();
						new PartialParticle(Particle.REDSTONE, mobLoc, 15).delta(0.35, 0.5, 0.35).data(SWORDSAGE_COLOR).spawnAsBoss();

					}

					BukkitRunnable runnable2 = new BukkitRunnable() {
						int mTicks = 0;
						double mRadians = 0;

						@Override
						public void run() {
							Vector vec = new Vector(FastUtils.cos(mRadians) * DANCE_RADIUS / 1.5, 0, FastUtils.sin(mRadians) * DANCE_RADIUS / 1.5);

							Location loc2 = mBoss.getEyeLocation().add(vec);
							new PartialParticle(Particle.SWEEP_ATTACK, loc2, 5).delta(1, 0.25, 1).spawnAsBoss();
							new PartialParticle(Particle.CRIT, loc2, 10).delta(1, 0.25, 1).extra(0.3).spawnAsBoss();
							new PartialParticle(Particle.REDSTONE, loc2, 15).delta(1, 0.25, 1).data(SWORDSAGE_COLOR).spawnAsBoss();
							world.playSound(loc2, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1.5f);

							if (mTicks >= 5) {
								this.cancel();
							}

							mTicks++;
							mRadians += Math.toRadians(72);
						}
					};
					runnable2.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(runnable2);

					EntityUtils.cancelSelfRoot(mBoss);
					this.cancel();
					mBossClass.mSpellActive = false;
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 16;
	}
}
