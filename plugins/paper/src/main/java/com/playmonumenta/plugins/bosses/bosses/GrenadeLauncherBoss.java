package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellBaseGrenadeLauncher;
import com.playmonumenta.plugins.utils.BossUtils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;


public class GrenadeLauncherBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_grenade_launcher";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int DAMAGE = 0;
		@BossParam(help = "not written")
		public double DAMAGE_PERCENTAGE = 0.0;
		@BossParam(help = "Delay of the spell")
		public int DELAY = 100;
		@BossParam(help = "Max duration of the bomb floating")
		public int DURATION = 70;
		@BossParam(help = "not written")
		public int DETECTION = 40;
		@BossParam(help = "How often the spell is cast")
		public int COOLDOWN = 14 * 20;
		@BossParam(help = "if the grenade should explode when collide with other entities")
		public boolean EXPLODE_ON_TOUCH = true;
		@BossParam(help = "if explodeOnTouch ?  ticks to wait before checking for entity overlap to this block : ticks to wait before the lob explode when hitting the ground")
		public int EXPLOSION_DELAY = 20;

		@BossParam(help = "The material used for the grenade")
		public Material BOMB_MATERIAL = Material.TNT;

		@BossParam(help = "Effect applied to players hit by the explosion")
		public EffectsList EFFECTS = EffectsList.EMPTY;
		@BossParam(help = "The spell name showed when the player die by this skill")
		public String SPELL_NAME = "";

		@BossParam(help = "Let you choose the targets of this spell")
		public EntityTargets BOMB_TARGET = EntityTargets.GENERIC_ONE_PLAYER_TARGET;

		@BossParam(help = "Contains both: the target of the explosion and the radius of the lingering")
		public EntityTargets EXPLOSION_TARGET = EntityTargets.GENERIC_PLAYER_TARGET;

		@BossParam(help = "Particle that follow the bomb")
		public ParticlesList PARTICLE_BOMB = ParticlesList.fromString("[(CRIT,5)]");

		@BossParam(help = "Particle summon for the explosion")
		public ParticlesList PARTICLE_EXPLOSION = ParticlesList.fromString("[(EXPLOSION_HUGE,10,2,2,2,1.5)]");

		@BossParam(help = "Sounds played at grenade location each tick")
		public SoundsList SOUND_GRENADE = SoundsList.fromString("[(BLOCK_ANVIL_FALL,3,0.5)]");

		@BossParam(help = "Sounds played when the grenade explode")
		public SoundsList SOUND_EXPLOSION = SoundsList.fromString("[(ENTITY_GENERIC_EXPLODE,10)]");


		//lingering stuff...
		@BossParam(help = "0 if no lingering, else the duration of the lingering")
		public int LINGERING_DURATION = 120;
		//For the lingering radius is used EXPLOSION_TARGET.getRange()

		@BossParam(help = "damage applied at each target inside the lingering each 10 ticks")
		public int LINGERING_DAMAGE = 0;

		@BossParam(help = "% damage applied at each target inside the lingering each 10 ticks")
		public double LINGERING_DAMAGE_PERCENTAGE = 0.0;

		@BossParam(help = "Effect applied to players hit by the lingering")
		public EffectsList LINGERING_EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "Particle used for the ring border")
		public ParticlesList PARTICLE_LINGERING_RING = ParticlesList.fromString("[(REDSTONE,40,0.15,0.3,0.15,0.2,GRAY,1.5)]");

		@BossParam(help = "Particle used for the ring center")
		public ParticlesList PARTICLE_LINGERING_CENTER = ParticlesList.fromString("[(LAVA,10,0,0,0,1.5)]");

		@BossParam(help = "Sound played at the ring center each second ")
		public SoundsList SOUND_LINGERING = SoundsList.fromString("[(ENTITY_BLAZE_BURN,4,1.5)]");

	}


	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new GrenadeLauncherBoss(plugin, boss);
	}

	public GrenadeLauncherBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseGrenadeLauncher(
				plugin,
				boss,
				p.BOMB_MATERIAL,
				p.EXPLODE_ON_TOUCH,
				p.EXPLOSION_DELAY,
				p.DURATION,
				p.COOLDOWN,
				p.LINGERING_DURATION,
				p.EXPLOSION_TARGET.getRange(),
				() -> {
					return p.BOMB_TARGET.getTargetsList(boss);
				},
				(Location loc) -> {
					return p.EXPLOSION_TARGET.getTargetsListByLocation(boss, loc);
				},
				(LivingEntity bosss, Location loc) -> {
					//init aesthetics
					//not used for now
				},
				(LivingEntity bosss, Location loc) -> {
					//aesthetics follow the grenade
					p.PARTICLE_BOMB.spawn(loc);
					p.SOUND_GRENADE.play(loc);

				},
				(LivingEntity bosss, Location loc) -> {
					//aesthetics
					p.PARTICLE_EXPLOSION.spawn(loc);
					p.SOUND_EXPLOSION.play(loc);
				},
				(LivingEntity bosss, LivingEntity target, Location loc) -> {
					//hit actions

					if (p.DAMAGE > 0) {
						if (p.SPELL_NAME.isEmpty()) {
							BossUtils.bossDamage(boss, target, p.DAMAGE);
						} else {
							BossUtils.bossDamage(boss, target, p.DAMAGE, loc, p.SPELL_NAME);
						}
					}

					if (p.DAMAGE_PERCENTAGE > 0.0) {
						if (p.SPELL_NAME.isEmpty()) {
							BossUtils.bossDamagePercent(mBoss, target, p.DAMAGE_PERCENTAGE);
						} else {
							BossUtils.bossDamagePercent(mBoss, target, p.DAMAGE_PERCENTAGE, p.SPELL_NAME);
						}
					}

					p.EFFECTS.apply(target, boss);
				},
				//Aesthetics and hit for the lingering ring
				(Location loc) -> {
					//particle for ring
					p.PARTICLE_LINGERING_RING.spawn(loc, 0.1, 0.2, 0.1, 0.1);
				},
				(Location loc, int ticks) -> {
					//particle for ring center
					if (ticks % 20 == 0) {
						p.SOUND_LINGERING.play(loc);
					}
					p.PARTICLE_LINGERING_CENTER.spawn(loc, p.EXPLOSION_TARGET.getRange() / 3, 0.2, p.EXPLOSION_TARGET.getRange() / 3, 0.5);
				},
				(LivingEntity bosss, LivingEntity target, Location loc) -> {
					//hit ring actions
					if (p.LINGERING_DAMAGE > 0) {
						if (p.SPELL_NAME.isEmpty()) {
							BossUtils.bossDamage(boss, target, p.LINGERING_DAMAGE);
						} else {
							BossUtils.bossDamage(boss, target, p.LINGERING_DAMAGE, loc, p.SPELL_NAME);
						}
					}

					if (p.LINGERING_DAMAGE_PERCENTAGE > 0.0) {
						if (p.SPELL_NAME.isEmpty()) {
							BossUtils.bossDamagePercent(mBoss, target, p.LINGERING_DAMAGE_PERCENTAGE);
						} else {
							BossUtils.bossDamagePercent(mBoss, target, p.LINGERING_DAMAGE_PERCENTAGE, p.SPELL_NAME);
						}
					}

					p.LINGERING_EFFECTS.apply(target, boss);
				}
			)
		));
		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);

	}

}
