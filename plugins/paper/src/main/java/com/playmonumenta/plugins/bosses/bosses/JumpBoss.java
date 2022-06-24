package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSlam;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class JumpBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_jump";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int DELAY = 100;
		@BossParam(help = "not written")
		public int MIN_RANGE = 0;
		@BossParam(help = "not written")
		public int DETECTION = 32;
		@BossParam(help = "not written")
		public int JUMP_HEIGHT = 1;
		@BossParam(help = "not written")
		public int RUN_DISTANCE = 0;
		@BossParam(help = "not written")
		public int COOLDOWN = 20 * 8;
		@BossParam(help = "not written")
		public double VELOCITY_MULTIPLIER = 0.45;


		@BossParam(help = "Jumping Sound")
		public SoundsList SOUND_JUMP = SoundsList.fromString("[(ENTITY_PILLAGER_CELEBRATE,1,1.1)]");
		@BossParam(help = "Jump Start Sound")
		public SoundsList SOUND_JUMP_START = SoundsList.fromString("[(ENTITY_HORSE_JUMP,1,1)]");
		@BossParam(help = "Jump Landing Sound")
		public SoundsList SOUND_LANDING = SoundsList.fromString("[(ENTITY_HORSE_GALLOP,1.3,0),(ENTITY_HORSE_GALLOP,2,1.25)]");


		@BossParam(help = "Starting Particles")
		public ParticlesList PARTICLE_START = ParticlesList.fromString("[(CLOUD,15,1,0,1,0)]");
		@BossParam(help = "Starting Particles on the Ground")
		public ParticlesList PARTICLE_START_GROUND = ParticlesList.fromString("[(CLOUD,15,1,0,1,0)]");
		@BossParam(help = "Air Particles")
		public ParticlesList PARTICLE_AIR = ParticlesList.fromString("[(REDSTONE,4,0.5,0.5,0.5,1,#FFFFFF,1)]");
		@BossParam(help = "Landing Particles")
		public ParticlesList PARTICLE_LAND = ParticlesList.fromString("[(CLOUD,60,0,0,0,0.2),(EXPLOSION_NORMAL,20,0,0,0,0.3)]");
		@BossParam(help = "Landing Particles on the ground")
		public ParticlesList PARTICLE_LAND_GROUND = ParticlesList.fromString("[(CLOUD,1,0.1,0.1,0.1,0.1)]");
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new JumpBoss(plugin, boss);
	}

	public JumpBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		SpellManager manager = new SpellManager(Arrays.asList(new SpellBaseSlam(plugin, boss, p.JUMP_HEIGHT, p.DETECTION, p.MIN_RANGE, p.RUN_DISTANCE, p.COOLDOWN, p.VELOCITY_MULTIPLIER,
				(World world, Location loc) -> {
					p.SOUND_JUMP.play(loc);
					p.PARTICLE_START.spawn(boss, loc, 1, 0, 1);
				}, (World world, Location loc) -> {
			p.SOUND_JUMP_START.play(loc);
			p.PARTICLE_START_GROUND.spawn(boss, loc, 1, 0, 1);
				}, (World world, Location loc) -> {
			p.PARTICLE_AIR.spawn(boss, loc, 4, 0.5, 0.5);
				}, (World world, Player player, Location loc, Vector dir) -> {
					ParticleUtils.explodingRingEffect(plugin, loc, 4, 1, 4,
							Arrays.asList(
									new AbstractMap.SimpleEntry<Double, SpawnParticleAction>(0.5, (Location location) -> {
										p.PARTICLE_LAND_GROUND.spawn(boss, loc, 1, 0.1, 0.1);
									})
							));

			p.SOUND_LANDING.play(loc);
			p.PARTICLE_LAND.spawn(boss, loc);
					})));
		super.constructBoss(manager, Collections.emptyList(), p.DETECTION, null, p.DELAY);
	}
}
