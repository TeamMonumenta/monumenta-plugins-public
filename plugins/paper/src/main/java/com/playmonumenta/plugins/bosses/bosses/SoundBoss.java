package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SoundBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_sound";

	public static class Parameters extends BossParameters {
		@BossParam(help = "sounds played when spawned")
		public SoundsList SPAWN_SOUND = SoundsList.EMPTY;

		@BossParam(help = "sounds played each AMBIENT_SOUND_TIMER ticks")
		public SoundsList AMBIENT_SOUND = SoundsList.EMPTY;

		@BossParam(help = "timer to play the sound AMBIENT_SOUND")
		public int AMBIENT_SOUND_TIMER = 20 * 2;

		@BossParam(help = "sounds played when hurt by a player")
		public SoundsList PLAYER_HURT = SoundsList.EMPTY;

		@BossParam(help = "sounds played when hurt by ambient")
		public SoundsList AMBIENT_HURT = SoundsList.EMPTY;

		@BossParam(help = "sounds played when hurt by other mobs")
		public SoundsList MOB_HURT = SoundsList.EMPTY;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new SoundBoss(plugin, boss);
	}

	private final Parameters mParams;

	public SoundBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mBoss.setSilent(true);
		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());

		mParams.SPAWN_SOUND.play(mBoss.getLocation());

		List<Spell> spellList;
		if (!mParams.AMBIENT_SOUND.isEmpty()) {
			spellList = List.of(new Spell() {
				int mTimer = 0;
				@Override public void run() {
					mTimer += 5;
					if (mTimer % 10 == 0) {
						mBoss.removeScoreboardTag("HasDoneSoundThisHalfSecond");
					}

					if (mTimer >= mParams.AMBIENT_SOUND_TIMER) {
						mTimer = 0;
						mParams.AMBIENT_SOUND.play(mBoss.getEyeLocation());
					}
				}

				@Override public int cooldownTicks() {
					return 5;
				}
			});
		} else {
			spellList = Collections.emptyList();
		}

		super.constructBoss(SpellManager.EMPTY, spellList, -1, null);
	}

	@Override public void onHurt(DamageEvent event) {
		if (event.getFinalDamage(false) <= 0) {
			//no sound when no damage
			return;
		}
		//simple way to don't spam sound when the boss get hit multiple time
		if (mBoss.getScoreboardTags().contains("HasDoneSoundThisHalfSecond")) {
			return;
		}

		LivingEntity source = event.getSource();
		if (source instanceof Player) {
			//player damage
			mParams.PLAYER_HURT.play(mBoss.getLocation());
		} else if (source != null) {
			//mobs damage
			mParams.MOB_HURT.play(mBoss.getLocation());
		} else {
			//ambient damage
			mParams.AMBIENT_HURT.play(mBoss.getLocation());
		}
		mBoss.addScoreboardTag("HasDoneSoundThisHalfSecond");
	}
}
