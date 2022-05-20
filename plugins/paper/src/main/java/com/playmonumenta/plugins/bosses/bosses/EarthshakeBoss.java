package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.SpellEarthshake;
import java.util.Arrays;
import java.util.Collections;
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

		@BossParam(help = "Players hit will be pushed up by this amount, plus 0.5 if standing close to the center")
		public double KNOCK_UP_SPEED = 1.0;

		@BossParam(help = "not written")
		public boolean LINE_OF_SIGHT = true;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new EarthshakeBoss(plugin, boss);
	}

	public EarthshakeBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellEarthshake(plugin, boss, p.RADIUS, p.FUSE_TIME, p.DAMAGE, p.COOLDOWN, p.RANGE, p.KNOCK_UP_SPEED, p.LINE_OF_SIGHT, p.FLY_BLOCKS)
		));

		super.constructBoss(activeSpells, Collections.emptyList(), p.DETECTION, null, p.DELAY);
	}
}
