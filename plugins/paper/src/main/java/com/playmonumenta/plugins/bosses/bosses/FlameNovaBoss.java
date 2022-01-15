package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellFlameNova;

/**
 * @author G3m1n1Boy
 * @deprecated use boss_nova instead, like this:
 * <blockquote><pre>
 * /bos var Tags add boss_nova
 * /bos var Tags add boss_nova[damage=17,duration=70,detection=20,effects=[(fire,80)]]
 * /bos var Tags add boss_nova[soundCharge=BLOCK_FIRE_AMBIENT,SoundCast=[(ENTITY_WITHER_SHOOT,1.5,0.65)]]
 * /bos var Tags add boss_nova[ParticleAir=[(lava,2,4.5,4.5,4.5,0.05)],ParticleLoad=[(flame,1,0.25,0.25,0.25,0.1)],ParticleExplode=[(flame,1,0.1,0.1,0.1,0.3),(smoke_normal,2,0.25,0.25,0.25,0.1)]]
 * </pre></blockquote>
 */
@Deprecated
public final class FlameNovaBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_flamenova";

	public static class Parameters extends BossParameters {
		public int RANGE = 9;
		public int DELAY = 100;
		public int DAMAGE = 17;
		public int DETECTION = 20;
		public int COOLDOWN = 160;
		public int FUSE_TIME = 70;
		public int FIRE_DURATION = 80;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FlameNovaBoss(plugin, boss);
	}

	public FlameNovaBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellFlameNova(plugin, boss, p.RANGE, p.FUSE_TIME, p.COOLDOWN, p.DAMAGE, p.FIRE_DURATION)
		));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}
