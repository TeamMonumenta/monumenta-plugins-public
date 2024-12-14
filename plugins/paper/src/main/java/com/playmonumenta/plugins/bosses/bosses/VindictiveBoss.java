package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellVindictiveParticle;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class VindictiveBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_vindictive";
	public static final int detectionRange = 40;

	public static final String PERCENT_SPEED_EFFECT_NAME = "VindictivePercentSpeedEffect";
	public static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "VindictivePercentDamageDealtEffect";

	public static class Parameters extends BossParameters {
		@BossParam(help = "The duration of the buff. Default: 160")
		public int DURATION = 20 * 8;

		@BossParam(help = "The range in which mobs will be affected. Default: 12")
		public int RANGE = 12;

		@BossParam(help = "The amount the affected mobs will heal. Default: 100")
		public int HEAL = 100;

		@BossParam(help = "The percent speed increase of the affected mobs")
		public double PERCENT_SPEED_EFFECT = 0.3;

		@BossParam(help = "The percent damage increase of the affected mobs")
		public double PERCENT_DAMAGE_DEALT_EFFECT = 0.8;

		@BossParam(help = "Sound played for each affected mob")
		public SoundsList SOUND = SoundsList.fromString("[(ENTITY_BLAZE_AMBIENT,0.5,0.5)]");

		@BossParam(help = "Particles spawned at affected mobs")
		public ParticlesList PARTICLES = ParticlesList.fromString("[(FLAME,20,0,0,0,0.1)]");

		@BossParam(help = "Additional effects applied to the affected mobs")
		public EffectsList ADDITIONAL_EFFECTS = EffectsList.EMPTY;
	}

	public final Parameters mParams;

	public VindictiveBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		mParams = Parameters.getParameters(boss, identityTag, new Parameters());

		List<Spell> passiveSpells = List.of(
			new SpellVindictiveParticle(boss)
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		com.playmonumenta.plugins.Plugin plugin = com.playmonumenta.plugins.Plugin.getInstance();

		for (LivingEntity mob : EntityUtils.getNearbyMobs(mBoss.getLocation(), mParams.RANGE)) {
			Location loc = mob.getEyeLocation();
			mParams.SOUND.play(loc);
			mParams.PARTICLES.spawn(mBoss, loc);

			if (mParams.ADDITIONAL_EFFECTS != EffectsList.EMPTY) {
				mParams.ADDITIONAL_EFFECTS.apply(mob, mBoss);
			}

			plugin.mEffectManager.addEffect(mob, PERCENT_SPEED_EFFECT_NAME,
				new PercentSpeed(mParams.DURATION, mParams.PERCENT_SPEED_EFFECT, PERCENT_SPEED_EFFECT_NAME));
			plugin.mEffectManager.addEffect(mob, PERCENT_DAMAGE_DEALT_EFFECT_NAME,
				new PercentDamageDealt(mParams.DURATION, mParams.PERCENT_DAMAGE_DEALT_EFFECT));

			mob.setHealth(Math.min(EntityUtils.getMaxHealth(mob), mob.getHealth() + mParams.HEAL));
		}
	}
}
