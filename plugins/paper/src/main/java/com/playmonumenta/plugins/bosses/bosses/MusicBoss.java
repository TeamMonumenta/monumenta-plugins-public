package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.SpellMusic;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class MusicBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_music";

	public static class Parameters extends BossParameters {
		@BossParam(help = "The name of the track to play. Should start with 'epic:music.'")
		public String TRACK = "epic:music.placeholder";

		@BossParam(help = "The duration of the track, in ticks")
		public int DURATION = 1200;

		@BossParam(help = "The amount of time before the track restarts, in ticks")
		public int INTERVAL = 20;

		@BossParam(help = "The amount of time before the track starts for the first time, in ticks")
		public int DELAY = 0;

		@BossParam(help = "The radius within which music will be started to play to players")
		public double RADIUS_INNER = 50;

		@BossParam(help = "The maximum radius for music to play, if CLEAR = true. If unset, uses RADIUS_INNER instead")
		public double RADIUS_OUTER = -1;

		@BossParam(help = "If true, clears music when the boss dies or players move further than RADIUS_OUTER")
		public boolean CLEAR = true;

		@BossParam(help = "The amount of time after clearing is triggered that the sound is stopped, in ticks")
		public int CLEAR_DELAY = 0;
	}

	private Parameters mParams;

	public MusicBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());
		double range = Math.max(mParams.RADIUS_INNER, mParams.RADIUS_OUTER);
		SpellMusic spell = new SpellMusic(boss, mParams.TRACK, mParams.DURATION, mParams.INTERVAL, mParams.DELAY, mParams.RADIUS_INNER, range, mParams.CLEAR, mParams.CLEAR_DELAY);
		super.constructBoss(new SpellManager(List.of(spell)), Collections.emptyList(), (int) (range * 2), null);
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new MusicBoss(plugin, boss);
	}
}
