package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class CreipergeuseBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_creipergeuse";

	public static class Parameters extends BossParameters {
		@BossParam(help = "If the boss's target is in this radius, the boss gets 80% speed")
		public int DETECTION = 60;

		@BossParam(help = "Period in ticks to check for players and apply effects to boss")
		public int COOLDOWN = 5;

		@BossParam(help = "If the boss's target is in this radius, the boss gains no additional effects and the target hears HEARTBEAT_SOUNDS")
		public int CLOSE_RADIUS = 8;

		@BossParam(help = "If the boss's target is in this radius, the target receives the Darkness effect")
		public int DARKNESS_RADIUS = 10;

		@BossParam(help = "If the boss's target is in this radius but not CLOSE_RADIUS, the boss gets 40% Speed")
		public int MIDRANGE_RADIUS = 15;

		@BossParam(help = "Sounds to play to the boss's target within CLOSE_RADIUS")
		public SoundsList HEARTBEAT_SOUNDS = SoundsList.fromString("[(ENTITY_WARDEN_HEARTBEAT,1.0,1.0)]");
	}

	private static final String SPEED_SRC = "CreipergeuseSpeedBuff";
	private static final String TARGET_TAG = "CreipergeuseBossTargeted";
	private final com.playmonumenta.plugins.Plugin mMonumentaPlugin;
	private final Parameters mParams;
	private @Nullable Player mTargeted;

	public CreipergeuseBoss(final Plugin plugin, final LivingEntity boss) {
		super(plugin, identityTag, boss);
		mMonumentaPlugin = com.playmonumenta.plugins.Plugin.getInstance();
		mParams = BossParameters.getParameters(mBoss, identityTag, new Parameters());

		mBoss.setInvulnerable(true);
		mBoss.addScoreboardTag("NoTrickyTransformation");
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, mParams.DETECTION);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1.0);

		final Spell spell = new Spell() {
			@Override
			public void run() {
				final Mob mMob = (Mob) mBoss;
				final LivingEntity currentTarget = mMob.getTarget();

				if (currentTarget == null || currentTarget != mTargeted || !currentTarget.getScoreboardTags().contains(TARGET_TAG)) {
					final List<Player> nearbyPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), mParams.DETECTION, true);
					nearbyPlayers.forEach(player -> player.removeScoreboardTag(TARGET_TAG));

					if (nearbyPlayers.isEmpty()) {
						MMLog.warning(() -> "[CreipergeuseBoss] Could not find any nearby players when setting target for boss" + mBoss + "!");
						return;
					}

					if (currentTarget == null) {
						mTargeted = nearbyPlayers.get(0);
						mMob.setTarget(mTargeted);
					} else {
						mTargeted = (Player) currentTarget;
					}

					if (mTargeted != null) {
						mTargeted.addScoreboardTag(TARGET_TAG);
					}
				}

				if (mTargeted == null) {
					MMLog.warning(() -> "[CreipergeuseBoss] mTargeted is null while running the spell for boss" + mBoss + "!");
					return;
				}

				if (!mTargeted.getScoreboardTags().contains(TARGET_TAG)) {
					MMLog.warning(() -> "[CreipergeuseBoss] mTargeted " + mTargeted + " for boss " + mBoss + " is missing " + TARGET_TAG + "!");
					return;
				}

				/* Uses distanceSquared for better performance */
				final double distanceSquaredToTarget = mBoss.getLocation().distanceSquared(mTargeted.getLocation());

				if (distanceSquaredToTarget <= mParams.DARKNESS_RADIUS * mParams.DARKNESS_RADIUS) {
					mTargeted.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS,
						Constants.TICKS_PER_SECOND * 2, 0, true, false, false));
				}

				if (distanceSquaredToTarget <= mParams.CLOSE_RADIUS * mParams.CLOSE_RADIUS) {
					mParams.HEARTBEAT_SOUNDS.play(mTargeted);
					mMonumentaPlugin.mEffectManager.clearEffects(mBoss, SPEED_SRC);
				} else if (distanceSquaredToTarget <= mParams.MIDRANGE_RADIUS * mParams.MIDRANGE_RADIUS) {
					mMonumentaPlugin.mEffectManager.addEffect(mBoss, SPEED_SRC,
						new BaseMovementSpeedModifyEffect(Constants.TICKS_PER_SECOND, 0.4));
				} else {
					mMonumentaPlugin.mEffectManager.addEffect(mBoss, SPEED_SRC,
						new BaseMovementSpeedModifyEffect(Constants.TICKS_PER_SECOND, 0.8));
				}
			}

			@Override
			public int cooldownTicks() {
				return mParams.COOLDOWN;
			}
		};

		constructBoss(spell, mParams.DETECTION, null, Constants.TICKS_PER_SECOND);
	}

	@Override
	public void bossExploded(final EntityExplodeEvent event) {
		NmsUtils.getVersionAdapter().runConsoleCommandSilently("function monumenta:quests/r1/quest60/lose_creeper");
	}

	@Override
	public void death(final @Nullable EntityDeathEvent event) {
		PlayerUtils.playersInRange(mBoss.getLocation(), mParams.DETECTION, true)
			.forEach(player -> player.removeScoreboardTag(TARGET_TAG));
	}
}
