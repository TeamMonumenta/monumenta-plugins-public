package com.playmonumenta.plugins.bosses.spells.bluestrike;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellBaseGrenadeLauncher;
import com.playmonumenta.plugins.effects.AbilitySilence;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellSamwellSmokeBomb extends SpellBaseGrenadeLauncher {
	private static final Material MATERIAL = Material.TNT;
	private static final int SILENCE_DURATION = 3 * 20;
	private static final ParticlesList.CParticle mPGrenade = new ParticlesList.CParticle(Particle.CRIT, 5, 0.1, 0.1, 0.1);
	private static final ParticlesList.CParticle mPExplode = new ParticlesList.CParticle(Particle.EXPLOSION_HUGE, 10, 2.5, 2.5, 2.5);
	private static final ParticlesList PARTICLE_LINGERING_RING = ParticlesList.fromString("[(REDSTONE,5,0.15,0.3,0.15,0.2,WHITE,1.5)]");
	private static final ParticlesList PARTICLE_LINGERING_CENTER = ParticlesList.fromString("[(LAVA,10,0,0,0,1.5)]");
	private static final SoundsList SOUND_LINGERING = SoundsList.fromString("[(ENTITY_BLAZE_BURN,4,1.5)]");
	private static final String SPELL_NAME = "Lava Bomb";
	private boolean mCooldown = false;
	private Plugin mPlugin;

	// Normal Grenade Launcher attack. 3 Lobs of 2 Grenades, which explode.
	// 55 Damage + 3s Silence Direct
	public SpellSamwellSmokeBomb(Plugin plugin, LivingEntity boss, int phase) {
		super(plugin, boss, MATERIAL, true, 20, 3, 20, 70, getCooldown(phase), 0, 0,
			() -> {
				// Player Targets for Grenade throw
				List<Player> potentialTargets = PlayerUtils.playersInRange(boss.getLocation(), 50, false);
				Collections.shuffle(potentialTargets);
				List<Player> results = new ArrayList<>();

				// Gets 2 targets
				if (potentialTargets.size() > 0) {
					results.add(potentialTargets.remove(0));
				}
				if (potentialTargets.size() > 0) {
					results.add(potentialTargets.remove(0));
				}

				return results;
			},
			(Location location) -> {
				// Explosion Targets
				return PlayerUtils.playersInRange(location, 4, true);
			},
			(LivingEntity bosss, Location loc) -> {
				// Boss Aesthetics
				bosss.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 5, 1);
			},
			(LivingEntity bosss, Location loc) -> {
				// Grenade Aesthetics
				mPGrenade.spawn(bosss, loc);
				bosss.getWorld().playSound(loc, Sound.BLOCK_ANVIL_FALL, 5, 1);
			},
			(LivingEntity bosss, Location loc) -> {
				// Explosion Aesthetics
				mPExplode.spawn(bosss, loc);
				bosss.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3, 1);
			},
			(LivingEntity bosss, LivingEntity target, Location loc) -> {
				// Hit Action on Explosion Targets
				directHit(plugin, boss, target, loc);
			},
			(Location loc) -> {
				// Ring Aesthetics
				PARTICLE_LINGERING_RING.spawn(boss, loc);
			},
			(Location loc, int ticks) -> {
				// Center Aesthetics
				if (ticks % 20 == 0) {
					SOUND_LINGERING.play(loc);
				}
				PARTICLE_LINGERING_CENTER.spawn(boss, loc);
			},
			(LivingEntity bosss, LivingEntity target, Location loc) -> {
				// Lingering stuff, not needed.
			});

		mPlugin = plugin;
	}

	@Override
	public void run() {
		super.run();
		mCooldown = true;
		new BukkitRunnable() {

			@Override
			public void run() {
				mCooldown = false;
			}

		}.runTaskLater(mPlugin, cooldownTicks() + 20);
	}

	private static int getCooldown(int phase) {
		if (phase <= 3) {
			return 10 * 20;
		} else {
			return 5 * 20;
		}
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

	private static void directHit(Plugin plugin, LivingEntity boss, LivingEntity player, Location location) {
		int damage = 55;

		BossUtils.blockableDamage(boss, player, DamageEvent.DamageType.BLAST, damage, SPELL_NAME, location);
		plugin.mEffectManager.addEffect(player, "SamwellSilence", new AbilitySilence(SILENCE_DURATION));
	}
}
