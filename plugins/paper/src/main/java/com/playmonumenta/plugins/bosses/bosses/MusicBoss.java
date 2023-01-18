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

		@BossParam(help = "The volume the song is played at")
		public float VOLUME = 2.0f;

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

		@BossParam(help = "Whether or not the song is forced to play (i.e. replace an existing song if currently playing)")
		public boolean FORCE = true;
	}

	public MusicBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		double range = Math.max(p.RADIUS_INNER, p.RADIUS_OUTER);
		SpellMusic spell = new SpellMusic(boss, p.TRACK, p.DURATION, p.VOLUME, p.DELAY, p.RADIUS_INNER, range, p.CLEAR, p.CLEAR_DELAY, p.FORCE);
		super.constructBoss(new SpellManager(List.of(spell)), Collections.emptyList(), (int) (range * 2), null);
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new MusicBoss(plugin, boss);
	}
}
