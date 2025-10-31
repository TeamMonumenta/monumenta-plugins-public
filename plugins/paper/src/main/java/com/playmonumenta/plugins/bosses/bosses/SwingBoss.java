package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAoE;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;


public class SwingBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_swing";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Range in blocks that the launcher searches for players to target with this spell")
		public int DETECTION = 30;
		@BossParam(help = "Radius in blocks in which players are damaged when the launcher completes casting")
		public int RADIUS = 3;
		@BossParam(help = "Time in ticks between the launcher spawning and the first attempt to cast this spell")
		public int DELAY = 5 * 20;
		@BossParam(help = "Time in ticks it takes for this spell to complete its cast")
		public int DURATION = 15;
		@BossParam(help = "Time in ticks the launcher waits before casting any other spell when this spell is cast")
		public int COOLDOWN = 20 * 14;

		@BossParam(help = "Melee damage dealt")
		public int DAMAGE = 30;
		@BossParam(help = "Percent health True damage dealt")
		public double DAMAGE_PERCENT = 0.0;
		@BossParam(help = "Effects applied to players hit by the swing")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		//Particle & Sounds
		@BossParam(help = "Sound played every few ticks")
		public Sound SOUND = Sound.ENTITY_PLAYER_ATTACK_SWEEP;

		@BossParam(help = "Particle summon around the boss in the air ")
		public ParticlesList PARTICLE_CHARGE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SWEEP_ATTACK, 1, 0.0, 0.0, 0.0, 0.0))
			.build();

		@BossParam(help = "Particle summon around the boss on the terrain")
		public ParticlesList PARTICLE_CIRCLE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CRIT, 12, 0.0, 0.0, 0.0, 0.0))
			.build();

		@BossParam(help = "Sound played when the ability explode")
		public SoundsList SOUND_EXPLODE = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.5f, 0.65f))
			.build();

		@BossParam(help = "Particle summon when the ability explode")
		public ParticlesList PARTICLE_CIRCLE_EXPLODE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SWEEP_ATTACK, 24, 0.1, 0.1, 0.1, 0.3))
			.add(new ParticlesList.CParticle(Particle.REDSTONE, 48, 0.25, 0.25, 0.25, 0.0, new Particle.DustOptions(Color.WHITE, 2.0f)))
			.build();

	}

	public SwingBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		Spell spell = new SpellBaseAoE(plugin, boss, p.RADIUS, p.DURATION, p.COOLDOWN, true, p.SOUND) {
			@Override
			protected void chargeAuraAction(Location loc) {
				boss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 1, 2));
				p.PARTICLE_CHARGE.spawn(boss, loc, ((double) p.RADIUS) / 3, ((double) p.RADIUS) / 3, ((double) p.RADIUS) / 3, 0.05);
			}

			@Override
			protected void chargeCircleAction(Location loc, double radius) {
				p.PARTICLE_CIRCLE.spawn(boss, particle -> new PPCircle(particle, loc, radius).delta(0.25).extra(0.05));
			}

			@Override
			protected void outburstAction(Location loc) {
				p.SOUND_EXPLODE.play(loc, 1.5f, 0.65F);
			}

			@Override
			protected void circleOutburstAction(Location loc, double radius) {
				p.PARTICLE_CIRCLE_EXPLODE.spawn(boss, particle -> new PPCircle(particle, loc, radius).delta(0.2).extra(0.2));
			}

			@Override
			protected void dealDamageAction(Location loc) {
				double bossY = boss.getLocation().getY();
				for (Player player : PlayerUtils.playersInRange(boss.getLocation(), p.RADIUS, true)) {
					double playerY = player.getLocation().getY();
					//if the player is on ground increase the size of the swing to avoid slab cheating
					if ((playerY + player.getHeight() < bossY) || (PlayerUtils.isOnGround(player) ? bossY + 0.7 < playerY : bossY + 0.1 < playerY)) {
						continue;
					}

					if (p.DAMAGE > 0) {
						BossUtils.blockableDamage(boss, player, DamageType.MELEE, p.DAMAGE, p.EFFECTS.mEffectList());
					}

					if (p.DAMAGE_PERCENT > 0.0) {
						BossUtils.bossDamagePercent(mBoss, player, p.DAMAGE_PERCENT, mBoss.getLocation(), p.EFFECTS.mEffectList());
					}

					p.EFFECTS.apply(player, mBoss);
				}
			}
		};

		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}
