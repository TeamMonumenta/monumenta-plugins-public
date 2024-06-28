package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseGrenadeLauncher;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;


public class GrenadeLauncherBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_grenade_launcher";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Grenade damage")
		public int DAMAGE = 0;
		@BossParam(help = "Max Health % damage")
		public double DAMAGE_PERCENTAGE = 0.0;
		@BossParam(help = "Delay between the time the mob first spawns and first cast")
		public int DELAY = 100;
		@BossParam(help = "Maximum duration of bomb air time. Bomb detonates after DURATION ticks regardless of circumstances")
		public int DURATION = 70;
		@BossParam(help = "The spellcaster will check for players up to this many blocks away")
		public int DETECTION = 40;
		@BossParam(help = "How often the spell is cast")
		public int COOLDOWN = 14 * 20;
		@BossParam(help = "Should the grenade explode on contact with the ground and/or an entity?")
		public boolean EXPLODE_ON_TOUCH = true;
		@BossParam(help = "If EXPLODE_ON_TOUCH ? ticks to wait before checking for entity overlap : ticks to wait when the grenade hits the ground")
		public int EXPLOSION_DELAY = 20;

		@BossParam(help = "Number of grenades")
		public int LOBS = 1;

		@BossParam(help = "Tick delay between grenade lobs")
		public int LOBS_DELAY = 10;

		@BossParam(help = "The falling block entity material used for the grenade")
		public Material BOMB_MATERIAL = Material.TNT;

		@BossParam(help = "Effect applied to players hit by the explosion")
		public EffectsList EFFECTS = EffectsList.EMPTY;
		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";

		@BossParam(help = "Targets of the spell. Defaults to one player that the mob has line of sight to")
		public EntityTargets BOMB_TARGET = EntityTargets.GENERIC_ONE_PLAYER_TARGET.clone().setFilters(List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT));

		@BossParam(help = "Determines which nearby players should be hit by the explosion and the radius of the lingering")
		public EntityTargets EXPLOSION_TARGET = new EntityTargets(EntityTargets.TARGETS.PLAYER, 3, true, EntityTargets.Limit.DEFAULT, new ArrayList<>(), EntityTargets.TagsListFiter.DEFAULT);

		@BossParam(help = "Particles that follow the bomb throw arc")
		public ParticlesList PARTICLE_BOMB = ParticlesList.fromString("[(CRIT,5)]");

		@BossParam(help = "Particles summoned at the explosion")
		public ParticlesList PARTICLE_EXPLOSION = ParticlesList.fromString("[(EXPLOSION_HUGE,10,2,2,2,1.5)]");

		@BossParam(help = "Sounds played at the grenade location each tick")
		public SoundsList SOUND_GRENADE = SoundsList.fromString("[(BLOCK_ANVIL_FALL,3,0.5)]");

		@BossParam(help = "Sounds played when the grenade explodes")
		public SoundsList SOUND_EXPLOSION = SoundsList.fromString("[(ENTITY_GENERIC_EXPLODE,10)]");


		//lingering stuff...
		@BossParam(help = "0 if no lingering, else the duration of the lingering")
		public int LINGERING_DURATION = 120;
		//For the lingering radius is used EXPLOSION_TARGET.getRange()

		@BossParam(help = "Damage applied to targets inside the lingering radius every 10 ticks")
		public int LINGERING_DAMAGE = 0;

		@BossParam(help = "% damage applied to targets inside the lingering radius every 10 ticks")
		public double LINGERING_DAMAGE_PERCENTAGE = 0.0;

		@BossParam(help = "Effect(s) applied to players hit by the lingering pool")
		public EffectsList LINGERING_EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "Particles used for the lingering pool ring border")
		public ParticlesList PARTICLE_LINGERING_RING = ParticlesList.fromString("[(REDSTONE,5,0.15,0.3,0.15,0.2,GRAY,1.5)]");

		@BossParam(help = "Particles used for the lingering pool center")
		public ParticlesList PARTICLE_LINGERING_CENTER = ParticlesList.fromString("[(LAVA,2,0,0,0,1.5)]");

		@BossParam(help = "Sound played at the center of the lingering pool every 20 ticks")
		public SoundsList SOUND_LINGERING = SoundsList.fromString("[(ENTITY_BLAZE_BURN,4,1.5)]");

		@BossParam(help = "LibraryOfSouls name of the mob spawned when the grenade explodes")
		public LoSPool SPAWNED_MOB_POOL = LoSPool.EMPTY;

		public float Y_VELOCITY = 0.7f;

		public double LOB_VARIANCE = 0.0;

	}

	public GrenadeLauncherBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		Spell spell = new SpellBaseGrenadeLauncher(
			plugin,
			boss,
			p.BOMB_MATERIAL,
			p.EXPLODE_ON_TOUCH,
			p.EXPLOSION_DELAY,
			p.LOBS,
			p.LOBS_DELAY,
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
				p.PARTICLE_BOMB.spawn(boss, loc);
				p.SOUND_GRENADE.play(loc);

			},
			(LivingEntity bosss, Location loc) -> {
				//aesthetics
				p.PARTICLE_EXPLOSION.spawn(boss, loc);
				p.SOUND_EXPLOSION.play(loc);
			},
			(LivingEntity bosss, LivingEntity target, Location loc) -> {
				//hit actions

				if (p.DAMAGE > 0) {
					BossUtils.blockableDamage(boss, target, DamageType.BLAST, p.DAMAGE, p.SPELL_NAME, loc);
				}

				if (p.DAMAGE_PERCENTAGE > 0.0) {
					BossUtils.bossDamagePercent(mBoss, target, p.DAMAGE_PERCENTAGE, loc, p.SPELL_NAME);
				}

				p.EFFECTS.apply(target, boss);
			},
			//Aesthetics and hit for the lingering ring
			(Location loc) -> {
				//particle for ring
				p.PARTICLE_LINGERING_RING.spawn(boss, loc, 0.1, 0.2, 0.1, 0.1);
			},
			(Location loc, int ticks) -> {
				//particle for ring center
				if (ticks % 20 == 0) {
					p.SOUND_LINGERING.play(loc);
				}
				p.PARTICLE_LINGERING_CENTER.spawn(boss, loc, p.EXPLOSION_TARGET.getRange() / 3, 0.2, p.EXPLOSION_TARGET.getRange() / 3, 0.5);
			},
			(LivingEntity bosss, LivingEntity target, Location loc) -> {
				String spellName = p.SPELL_NAME.isEmpty() ? null : p.SPELL_NAME;
				//hit ring actions
				if (p.LINGERING_DAMAGE > 0) {
					DamageUtils.damage(boss, target, DamageType.BLAST, p.LINGERING_DAMAGE, null, false, false, spellName);
				}

				if (p.LINGERING_DAMAGE_PERCENTAGE > 0.0) {
					BossUtils.bossDamagePercent(mBoss, target, p.LINGERING_DAMAGE_PERCENTAGE, spellName);
				}

				p.LINGERING_EFFECTS.apply(target, boss);
			},
			p.SPAWNED_MOB_POOL, p.Y_VELOCITY, p.LOB_VARIANCE, null, null
		);
		super.constructBoss(spell, p.DETECTION, null, p.DELAY);

	}

}
