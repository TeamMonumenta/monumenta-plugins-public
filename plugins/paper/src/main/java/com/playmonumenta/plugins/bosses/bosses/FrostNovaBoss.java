package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellFrostNova;
import com.playmonumenta.plugins.utils.BossUtils;

/**
 * @deprecated use boss_nova instead, like this:
 * <blockquote><pre>
 * /bos var Tags add boss_nova
 * /bos var Tags add boss_nova[radius=8,damage=18,duration=80,detection=20]
 * /bos var Tags add boss_nova[effect=slow,effectDuration=50,effectAmplified=3]
 * /bos var Tags add boss_nova[soundCharge=ENTITY_SNOWBALL_THROW,ParticleAirNumber=7,ParticleLoad=snowball,ParticleExplodeMain=Cloud,ParticleExplodeSecond=snowball,SoundCast=BLOCK_GLASS_BREAK]
 *  you can use multiple raw if needed
 * </pre></blockquote>
 * @G3m1n1Boy
 */
public class FrostNovaBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_frostnova";

	public static class Parameters {
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

		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellFrostNova(plugin, boss, p.RADIUS, p.DAMAGE, p.DAMAGE, p.DURATION, p.COOLDOWN)
		));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}
