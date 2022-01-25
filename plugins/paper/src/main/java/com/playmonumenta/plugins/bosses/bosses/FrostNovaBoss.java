package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.Collections;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellFrostNova;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

/**
 * @deprecated use boss_nova instead, like this:
 * <blockquote><pre>
 * /bos var Tags add boss_nova
 * /bos var Tags add boss_nova[damage=18,duration=80,detection=20,effects=[(slow,50,3)]]
 * /bos var Tags add boss_nova[soundCharge=ENTITY_SNOWBALL_THROW,SoundCast=[(BLOCK_GLASS_BREAK,1.5,0.65)]]
 * /bos var Tags add boss_nova[ParticleAir=[(CLOUD,2,4.5,4.5,4.5,0.05)],ParticleLoad=[(snowball,1,0.25,0.25,0.25,0.1)],ParticleExplode=[(Cloud,1,0.1,0.1,0.1,0.3),(snowball,2,0.25,0.25,0.25,0.1)]]
 * </pre></blockquote>
 * G3m1n1Boy
 */
@Deprecated
public class FrostNovaBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_frostnova";

	public static class Parameters extends BossParameters {
		public int RADIUS = 8;
		public int DELAY = 100;
		public int DAMAGE = 18;
		public int DURATION = 80;
		public int COOLDOWN = 160;
		public int DETECTION = 20;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FrostNovaBoss(plugin, boss);
	}

	public FrostNovaBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellFrostNova(plugin, boss, p.RADIUS, p.DAMAGE, p.DAMAGE, p.DURATION, p.COOLDOWN)
		));

		super.constructBoss(activeSpells, Collections.emptyList(), p.DETECTION, null, p.DELAY);
	}
}
