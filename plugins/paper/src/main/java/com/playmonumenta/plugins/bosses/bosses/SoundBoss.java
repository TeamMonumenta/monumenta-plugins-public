package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
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

		@BossParam(help = "sounds played each time the mobs does a step")
		public SoundsList STEP_SOUND = SoundsList.EMPTY;

		@BossParam(help = "sounds played when the mob dies")
		public SoundsList DEATH_SOUND = SoundsList.EMPTY;
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
		spellList = List.of(new Spell() {
			final boolean mHasLegs = !(EntityUtils.isFlyingMob(mBoss) || EntityUtils.isWaterMob(mBoss));
			int mHalfSecondTimer = 0;
			int mAmbientTimer = 0;

			@Override
			public void run() {
				mHalfSecondTimer++;
				mAmbientTimer++;

				if (mHalfSecondTimer % 10 == 0) {
					mBoss.removeScoreboardTag("HasDoneSoundThisHalfSecond");
				}

				if (mAmbientTimer >= mParams.AMBIENT_SOUND_TIMER) {
					mAmbientTimer = 0;
					mParams.AMBIENT_SOUND.play(mBoss.getEyeLocation());
				}

				if (mHasLegs && !mParams.STEP_SOUND.isEmpty() && mBoss.isOnGround() && !EntityUtils.isInWater(mBoss) && mBoss.getVelocity().length() > 0.079) {
					mParams.STEP_SOUND.play(mBoss.getLocation(), 0.15F);
				}
			}

			@Override
			public int cooldownTicks() {
				return 1;
			}
		});

		super.constructBoss(SpellManager.EMPTY, spellList, -1, null, 1);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (event.getFinalDamage(false) <= 0 || event.getFinalDamage(true) >= mBoss.getHealth()) {
			//no sound when no damage or on death
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

	@Override
	public void death(EntityDeathEvent event) {
		mParams.DEATH_SOUND.play(mBoss.getLocation());
	}
}
