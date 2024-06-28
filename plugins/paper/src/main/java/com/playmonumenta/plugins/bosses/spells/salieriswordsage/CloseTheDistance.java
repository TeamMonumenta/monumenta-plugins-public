package com.playmonumenta.plugins.bosses.spells.salieriswordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.SalieriTheSwordsage;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSlam;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class CloseTheDistance extends Spell {
	private final SalieriTheSwordsage mBossClass;
	private final LivingEntity mBoss;
	private final SpellBaseSlam mSweepLeap;
	private final int mDamage;
	private static final int MIN_RANGE = 0;
	private static final int RUN_DISTANCE = 0;
	private static final double VELOCITY_MULTIPLIER = 0.5;
	private static final double DAMAGE_RADIUS = 3;
	private static final int JUMP_HEIGHT = 1;
	private int mTicks = 0;

	public CloseTheDistance(Plugin plugin, LivingEntity boss, int damage, SalieriTheSwordsage bossClass) {
		mBoss = boss;
		mDamage = damage;
		mBossClass = bossClass;

		mSweepLeap = new SpellBaseSlam(plugin, boss, JUMP_HEIGHT, SalieriTheSwordsage.detectionRange, MIN_RANGE, RUN_DISTANCE, 0, VELOCITY_MULTIPLIER,
			(World world, Location loc) -> {
				world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.5f, 0);
				world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 2f);
				new PartialParticle(Particle.CLOUD, loc, 15).delta(1, 0, 1).spawnAsBoss();
			}, (World world, Location loc) -> {
				world.playSound(loc, Sound.ENTITY_HORSE_JUMP, SoundCategory.PLAYERS, 1, 1);
				new PartialParticle(Particle.CLOUD, loc, 15).delta(1, 0, 1).spawnAsBoss();
			}, (World world, Location loc) -> {
				new PartialParticle(Particle.REDSTONE, loc, 4).delta(0.5, 0.5, 0.5).data(new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f)).spawnAsBoss();
			}, (World world, @Nullable Player player, Location loc, Vector dir) -> {
				ParticleUtils.explodingRingEffect(plugin, loc, 4, 1, 4,
					List.of(
						new SimpleEntry<Double, SpawnParticleAction>(0.5, (Location location) -> {
							new PartialParticle(Particle.SWEEP_ATTACK, loc, 1).spawnAsBoss();

						})
					));

				world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2, 1.25F);
				world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.HOSTILE, 2, 0);

				world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 2f);

				new PartialParticle(Particle.SMOKE_LARGE, loc, 60).extra(0.3).spawnAsBoss();

				if (player != null) {
					BossUtils.blockableDamage(boss, player, DamageEvent.DamageType.MELEE_SKILL, mDamage);
					return;
				}
				for (Player p : PlayerUtils.playersInRange(loc, DAMAGE_RADIUS, true)) {
					BossUtils.blockableDamage(boss, p, DamageEvent.DamageType.MELEE_SKILL, mDamage);
				}
			}
		);
	}

	@Override
	public void run() {
		if (mBossClass.mSpellActive) {
			mTicks = 0;
			return;
		}

		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 40, true)) {
			if (player.getLocation().distance(mBoss.getLocation()) > 9) {
				mTicks += 5;

				if (mTicks >= 20 * 2) {
					mTicks = 0;
					mSweepLeap.run();
				}

				return;
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
