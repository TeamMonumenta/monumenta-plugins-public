package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellTpBehindPlayer;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;

public final class TpBehindBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_tpbehind";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Time (in ticks) it remains in place")
		public int STUN = 10;

		@BossParam(help = "DEPRECATED. Use targets for choosing the radius instead", deprecated = true)
		public int RANGE = 80;

		@BossParam(help = "Delay before teleporting")
		public int DELAY = 50;

		@BossParam(help = "Distance to a player before casting")
		public int DETECTION = 20;

		@BossParam(help = "DEPRECATED. Use prefertarget for teleporting to target", deprecated = true)
		public String RANDOM = "null";

		@BossParam(help = "not written")
		public int COOLDOWN = 12 * 20;

		@BossParam(help = "Delay before first cast")
		public int INIT_DELAY = 100;

		@BossParam(help = "Distance after teleporting behind the target (in blocks)")
		public int DISTANCE = 4;

		@BossParam(help = "Sound telegraph before teleport")
		public SoundsList SOUND_TEL = new SoundsList(List.of(
			new SoundsList.CSound(Sound.ENTITY_WITCH_AMBIENT, 1.4f, 0.5f)
		));

		@BossParam(help = "Sound after teleporting")
		public SoundsList SOUND_TP = new SoundsList(List.of(
			new SoundsList.CSound(Sound.ENTITY_ENDERMAN_TELEPORT, 3.0f, 0.7f)
		));

		@BossParam(help = "Particle telegraph before teleport")
		public ParticlesList PARTICLE_TEL = new ParticlesList(List.of(
			new ParticlesList.CParticle(Particle.PORTAL, 10, 1, 1, 1, 0.03)
		));

		@BossParam(help = "Particle after teleport")
		public ParticlesList PARTICLE_TP = new ParticlesList(List.of(
			new ParticlesList.CParticle(Particle.SPELL_WITCH, 30, 0.25, 0.45, 0.25, 1),
			new ParticlesList.CParticle(Particle.SMOKE_LARGE, 12, 0, 0.45, 0, 0.125)
		));
		@BossParam(help = "Teleports to the mob's target (Overwrites TARGETS parameter)")
		public boolean PREFER_TARGET = false;

		@BossParam(help = "not written")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET;
	}

	Parameters mParameters = new Parameters();

	public TpBehindBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells;

		if (EntityUtils.isFlyingMob(mBoss)) {
			//Flying mobs can't use this ability
			activeSpells = SpellManager.EMPTY;
		} else {
			activeSpells = new SpellManager(List.of(
				new SpellTpBehindPlayer(plugin, boss, mParameters)
			));
			// I can't think of a better way to deprecate this
			if (!mParameters.RANDOM.equals("null")) {
				mParameters.PREFER_TARGET = !Boolean.parseBoolean(mParameters.RANDOM);
			}
			if (mParameters.TARGETS == EntityTargets.GENERIC_PLAYER_TARGET) {
				mParameters.TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, mParameters.RANGE, false, new EntityTargets.Limit(EntityTargets.Limit.LIMITSENUM.ALL, EntityTargets.Limit.SORTING.RANDOM));
			}
		}
		Parameters.getParameters(boss, identityTag, mParameters);
		super.constructBoss(activeSpells, Collections.emptyList(), mParameters.DETECTION, null, mParameters.INIT_DELAY);
	}
}
