package com.playmonumenta.plugins.bosses.bosses;

import java.util.AbstractMap;
import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSlam;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;
import com.playmonumenta.plugins.utils.PlayerUtils;

//general version of bossMeteorSlam
public class PounceBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_pounce";

	public static class Parameters {

		public int MIN_RANGE = 0;
		public int DETECTION = 32;
		public int DELAY = 20 * 5;
		public int JUMP_HEIGHT = 1;
		public int RUN_DISTANCE = 0;
		public int COOLDOWN = 20 * 8;
		public double VELOCITY_MULTIPLIER = 0.5;

		public double DAMAGE_RADIUS = 3;
		public double DAMAGE = 0;
		public double DAMAGE_PERCENT = 0;

		public EffectsList EFFECTS = EffectsList.EMPTY;

		//Particles & Sounds
		/** Sound played when the ability start */
		public SoundsList SOUND_START = SoundsList.fromString("[(ENTITY_ENDER_DRAGON_GROWL,1,1)]");
		/** Particle spawned when the ability start */
		public ParticlesList PARTICLE_START = ParticlesList.fromString("[(LAVA,15)]");
		/** Particle used at the start of the leap */
		public ParticlesList PARTICLE_LEAP = ParticlesList.fromString("[(LAVA,15)]");
		/** Sound used at the start of the leap */
		public SoundsList SOUND_LEAP = SoundsList.fromString("[(ENTITY_HORSE_JUMP,1,1)]");
		/** Particle used for the leaping */
		public ParticlesList PARTICLE_LEAPING = ParticlesList.fromString("[(REDSTONE,4,0.5,0.5,0.5,#ffffff,1)]");
		/** Particle summoned when the boss hit the ground as a ring */
		public ParticlesList PARTICLE_RING = ParticlesList.fromString("[(FLAME,1,0.1,0.1,0.1,0.1),(CLOUD,1,0.1,0.1,0.1,0.1)]");
		/** Sound played when a player got hit by the ability or the boss hit the ground*/
		public SoundsList SOUND_HIT = SoundsList.fromString("[(ENTITY_GENERIC_EXPLODE,1.3,0),(ENTITY_GENERIC_EXPLODE,2,1.25)]");
		/** Particle summoned when a player got hit by the ability or the boss hit the ground*/
		public ParticlesList PARTICLE_HIT = ParticlesList.fromString("[(FLAME,60,0,0,0,0.2),(EXPLOSION_NORMAL,20,0,0,0,0.3),(LAVA,27,3,0.25,3,0)]");
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new PounceBoss(plugin, boss);
	}

	public PounceBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());
		SpellManager manager = new SpellManager(Arrays.asList(new SpellBaseSlam(plugin, boss, p.JUMP_HEIGHT, p.DETECTION, p.MIN_RANGE, p.RUN_DISTANCE, p.COOLDOWN, p.VELOCITY_MULTIPLIER,
				(World world, Location loc) -> {
					p.SOUND_START.play(loc, 1f, 1f);
					p.PARTICLE_START.spawn(loc, 1d, 0d, 1d);
				}, (World world, Location loc) -> {
					p.SOUND_LEAP.play(loc, 1f, 1f);
					p.PARTICLE_LEAP.spawn(loc, 1d, 0f, 1d, 0d);
				}, (World world, Location loc) -> {
					p.PARTICLE_LEAPING.spawn(loc, 0.5, 0.5, 0.5, 1d);
				}, (World world, Player player, Location loc, Vector dir) -> {
					ParticleUtils.explodingRingEffect(plugin, loc, 4, 1, 4,
							Arrays.asList(
									new AbstractMap.SimpleEntry<Double, SpawnParticleAction>(0.5, (Location location) -> {
										p.PARTICLE_RING.spawn(loc, 0.1, 0.1, 0.1, 0.1);
									})
							));
					p.SOUND_HIT.play(loc, 1, 1);
					p.PARTICLE_HIT.spawn(loc);

					if (player != null) {
						if (p.DAMAGE > 0) {
							BossUtils.bossDamage(boss, player, p.DAMAGE);
						}

						if (p.DAMAGE_PERCENT > 0.0) {
							BossUtils.bossDamagePercent(boss, player, p.DAMAGE_PERCENT);
						}
						p.EFFECTS.apply(player, boss);

						return;
					}
					for (Player players : PlayerUtils.playersInRange(loc, p.DAMAGE_RADIUS, true)) {
						if (p.DAMAGE > 0) {
							BossUtils.bossDamage(boss, players, p.DAMAGE);
						}

						if (p.DAMAGE_PERCENT > 0.0) {
							BossUtils.bossDamagePercent(boss, players, p.DAMAGE_PERCENT);
						}
						p.EFFECTS.apply(players, boss);
					}
					})));
		super.constructBoss(manager, null, p.DETECTION, null, p.DELAY);
	}
}
