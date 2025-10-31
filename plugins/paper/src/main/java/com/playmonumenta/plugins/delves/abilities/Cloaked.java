package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.bosses.bosses.StealthBoss;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;


public class Cloaked {

	// Mechanics
	private static final double CLOAKED_CHANCE = 0.7f;
	private static final int PLAYER_RADIUS_BASE = 8;
	private static final int PLAYER_RADIUS_DECREASE_PER_LEVEL = 2;
	private static final int MOB_RADIUS = 96;
	// 30 second cooldown
	private static final int COOLDOWN = 600;
	// Particles
	private static final ParticlesList PARTICLE_STEALTH = ParticlesList.builder()
		.add(new ParticlesList.CParticle(Particle.SMOKE_LARGE, 10, 0.2, 0, 0.2, 0.05))
		.build();
	private static final ParticlesList PARTICLE_EXIT = ParticlesList.builder()
		.add(new ParticlesList.CParticle(Particle.WHITE_SMOKE, 15, 0.5, 1, 0.5, 0.15))
		.build();
	// Sounds
	private static final SoundsList SOUND_STEALTH = SoundsList.EMPTY;
	private static final SoundsList SOUND_EXIT = SoundsList.builder()
		.add(new SoundsList.CSound(Sound.BLOCK_AZALEA_LEAVES_STEP, 1.0f, 0.5f))
		.build();
	private static final SoundsList SOUND_HIT = SoundsList.builder()
		.add(new SoundsList.CSound(Sound.BLOCK_ANCIENT_DEBRIS_FALL, 1.6f, 1.5f))
		.build();

	public static final String DESCRIPTION = "Enemies spawn invisible, and decloak when they get near.";
	public static final String AVOID_CLOAKED = "boss_cloakedimmune";


	public static Component[] rankDescription(int level) {
		return new Component[]{
			Component.text("When mobs spawn, they have a " + Math.round(100 * CLOAKED_CHANCE) + "% chance to spawn Cloaked."),
			Component.text("Cloaked mobs are invisible and do not render armour."),
			Component.text("Mobs will uncloak when within " + (PLAYER_RADIUS_BASE - PLAYER_RADIUS_DECREASE_PER_LEVEL * level) + " blocks of a player."),
			Component.text("Mobs will recloak after " + COOLDOWN / 20 + " seconds.")
		};
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (Chivalrous.isChivalrousName(mob.getName())
			|| !(DelvesUtils.isDelveMob(mob)
			|| mob.getScoreboardTags().contains(AVOID_CLOAKED)
			|| mob.getVehicle() instanceof LivingEntity)) {
			if (CLOAKED_CHANCE >= 1 || FastUtils.RANDOM.nextDouble() < CLOAKED_CHANCE) {
				// This runs prior to BossManager parsing, so we can just add tags directly
				mob.addScoreboardTag(StealthBoss.identityTag);
				mob.addScoreboardTag(StealthBoss.identityTag
					+ "[duration=" + "-1"
					+ ",damage=" + "0"
					+ ",delay=" + "0"
					+ ",cooldown=" + COOLDOWN
					+ ",detection=" + MOB_RADIUS
					+ ",proximitycheck=" + "true"
					+ ",proximity=" + (PLAYER_RADIUS_BASE - PLAYER_RADIUS_DECREASE_PER_LEVEL * level)
					+ ",particlestealth=" + PARTICLE_STEALTH.toString()
					+ ",particleexit=" + PARTICLE_EXIT
					+ ",soundstealth=" + SOUND_STEALTH
					+ ",soundexit=" + SOUND_EXIT
					+ ",soundhit=" + SOUND_HIT
					+ "]");
			}
		}
	}
}
