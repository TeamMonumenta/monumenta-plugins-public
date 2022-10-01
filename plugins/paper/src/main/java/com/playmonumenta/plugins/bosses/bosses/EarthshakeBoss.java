package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellEarthshake;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class EarthshakeBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_earthshake";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Explosion radius of the spell")
		public int RADIUS = 4;

		@BossParam(help = "Range at which the spell can be cast")
		public int RANGE = 12;

		@BossParam(help = "not written")
		public int DELAY = 100;

		@BossParam(help = "Blast damage that this spell deals to players")
		public int DAMAGE = 40;

		@BossParam(help = "not written")
		public int COOLDOWN = 160;

		@BossParam(help = "not written")
		public int DETECTION = 20;

		@BossParam(help = "Time between casting the spell and the resulting explosion")
		public int FUSE_TIME = 50;

		@BossParam(help = "Whether the explosion also makes blocks fly around")
		public boolean FLY_BLOCKS = true;

		@BossParam(help = "Chance for a block to be thrown around and/or replaced")
		public double FLY_BLOCKS_CHANCE = 0.5;

		@BossParam(help = "Material to place where blocks were thrown from. If not air, will also work if throwing blocks is disabled and will replace blocks without throwing them.")
		public Material REPLACE_BLOCKS = Material.AIR;

		@BossParam(help = "Players hit will be pushed up by this amount, plus 0.5 if standing close to the center")
		public double KNOCK_UP_SPEED = 1.0;

		@BossParam(help = "You should not use this. use TARGETS instead.", deprecated = true)
		public boolean LINE_OF_SIGHT = true;

		@BossParam(help = "target of this spell")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET_LINE_OF_SIGHT;

		@BossParam(help = "Sound played at the targeted player when the boss starts charging the ability ability")
		public SoundsList SOUND_WARNING = SoundsList.fromString("[(ENTITY_ELDER_GUARDIAN_CURSE,5,0.75)]");

		@BossParam(help = "Sound played once a second at the spell's target while the spell is charging")
		public SoundsList SOUND_CHARGE_TARGET = SoundsList.fromString("[(ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR,3,0.5),(BLOCK_GRAVEL_BREAK,3,0.5)]");

		@BossParam(help = "Sound played every tick at the caster while the spell is charging. Pitch is automatically increased by 0.01 every tick")
		public SoundsList SOUND_CHARGE_BOSS = SoundsList.fromString("[(BLOCK_ENDER_CHEST_OPEN,1,0.25)]");

		@BossParam(help = "Sound played when the explosion happens")
		public SoundsList SOUND_EXPLOSION = SoundsList.fromString("[(ENTITY_GENERIC_EXPLODE,3,1.35),(ENTITY_GENERIC_EXPLODE,3,0.5),(ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR,3,0.5)]");

		@BossParam(help = "Sound played when the explosion hits a player")
		public SoundsList SOUND_EXPLOSION_PLAYER = SoundsList.fromString("[(ENTITY_WITHER_BREAK_BLOCK,1,1)]");

		@BossParam(help = "Particles to spawn on explosion.")
		public ParticlesList PARTICLES_EXPLOSION = ParticlesList.fromString("[(CLOUD,150,0,0,0,0.5),(LAVA,35,2,1,2),(BLOCK_CRACK,200,2,1,2,0,DIRT),(CAMPFIRE_COSY_SMOKE,35,2,1,2,0.1)]");

		@BossParam(help = "Particles to spawn on explosion. 100 of these will be spawned, spread over the explosion area.")
		public ParticlesList PARTICLES_EXPLOSION_DIRECTIONAL = ParticlesList.fromString("[(SMOKE_LARGE,0,0,1,0)]");

		@BossParam(help = "Particles to spawn at the boss while the spell is charging.")
		public ParticlesList PARTICLES_CHARGE_BOSS = ParticlesList.fromString("[(DRIP_LAVA,2,0.25,0.45,0.25,1)]");

		@BossParam(help = "Particles to spawn every tick at the target location while the spell is charging.")
		public ParticlesList PARTICLES_CHARGE = ParticlesList.fromString("[(BLOCK_CRACK,2,2,0.1,2,0,STONE),(LAVA,2,0.25,0.25,0.25,0.1)]");

		@BossParam(help = "Particles to spawn every 2 ticks at the target location while the spell is charging.")
		public ParticlesList PARTICLES_CHARGE_TWO_TICKS = ParticlesList.fromString("[(CAMPFIRE_COSY_SMOKE,4,4,4,1,0.05)]");

		@BossParam(help = "Particles to spawn every 20 ticks at the target location while the spell is charging.")
		public ParticlesList PARTICLES_CHARGE_TWENTY_TICKS = ParticlesList.fromString("[(BLOCK_CRACK,80,2,0.1,2,0,DIRT),(CAMPFIRE_COSY_SMOKE,8,2,0.1,2)]");

		@BossParam(help = "Particles to spawn every 20 ticks at the circular border of the spell's area of effect.")
		public ParticlesList PARTICLES_CHARGE_TWENTY_TICKS_BORDER = ParticlesList.fromString("[(SMOKE_NORMAL,1,0.1,0.1,0.1)]");

	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new EarthshakeBoss(plugin, boss);
	}

	public EarthshakeBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		if (p.TARGETS == EntityTargets.GENERIC_PLAYER_TARGET_LINE_OF_SIGHT) {
			p.TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, p.RANGE, false, EntityTargets.Limit.DEFAULT_ONE, p.LINE_OF_SIGHT ? List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT) : Collections.emptyList());
		}

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellEarthshake(plugin, boss, p)
		));
		super.constructBoss(activeSpells, Collections.emptyList(), p.DETECTION, null, p.DELAY);
	}
}
