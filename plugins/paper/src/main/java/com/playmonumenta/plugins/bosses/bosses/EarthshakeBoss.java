package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.SpellEarthshake;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class EarthshakeBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_earthshake";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int RADIUS = 2;

		@BossParam(help = "not written")
		public int RANGE = 12;

		@BossParam(help = "not written")
		public int DELAY = 100;

		@BossParam(help = "not written")
		public int DAMAGE = 35;

		@BossParam(help = "not written")
		public int COOLDOWN = 160;

		@BossParam(help = "not written")
		public int DETECTION = 20;

		@BossParam(help = "not written")
		public int FUSE_TIME = 50;

		@BossParam(help = "not written")
		public boolean FLY_BLOCKS = true;

		@BossParam(help = "not written")
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

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}
