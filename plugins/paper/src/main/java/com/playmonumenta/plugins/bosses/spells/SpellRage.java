package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.effects.CCImmuneEffect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class SpellRage extends SpellBaseAoE {

	public static final String STRENGTH_BUFF_NAME = "SpellRageStrength";
	public static final String SPEED_BUFF_NAME = "SpellRageSpeed";
	public static final String KBR_BUFF_NAME = "SpellRageKBR";
	public static final String CCIMMUNE_BUFF_NAME = "SpellRageCCImmune";

	public boolean mIsCasting = false;

	public int mBuffDuration;
	public double mStrengthAmount;
	public double mSpeedAmount;
	public double mKBRAmount;
	public boolean mCCImmuneBuff;

	public SoundsList mSoundCharge;
	public ParticlesList mParticleCharge;
	public ParticlesList mParticleChargeCircle;
	public SoundsList mSoundFinish;
	public ParticlesList mParticleFinishCircle;
	public ParticlesList mParticleFinish;

	public SpellRage(Plugin plugin, LivingEntity launcher, int radius, int time, int cooldown, int buffDuration, double strengthAmount, double speedAmount, double kbrAmount, boolean ccImmune, boolean canMoveWhileCasting,
	                 SoundsList soundCharge, ParticlesList particleCharge, ParticlesList particleChargeCircle, SoundsList soundFinish, ParticlesList particleFinish, ParticlesList particleFinishCircle) {
		super(plugin, launcher, radius, time, cooldown, canMoveWhileCasting, Sound.UI_TOAST_OUT);
		mBuffDuration = buffDuration;
		mSpeedAmount = speedAmount;
		mStrengthAmount = strengthAmount;
		mKBRAmount = kbrAmount;
		mCCImmuneBuff = ccImmune;

		mSoundCharge = soundCharge;
		mParticleCharge = particleCharge;
		mParticleChargeCircle = particleChargeCircle;
		mSoundFinish = soundFinish;
		mParticleFinish = particleFinish;
		mParticleFinishCircle = particleFinishCircle;
	}

	@Override
	protected void chargeAuraAction(Location loc) {
		if (!mIsCasting) {
			mSoundCharge.play(loc);
			mIsCasting = true;
		}

		mParticleCharge.spawn(mLauncher, loc, mRadius / 2.0, mRadius / 2.0, mRadius / 2.0, 0.05);
	}

	@Override
	protected void chargeCircleAction(Location loc, double radius) {
		mParticleChargeCircle.spawn(mLauncher, particle -> new PPCircle(particle, loc, radius));
	}

	@Override
	protected void outburstAction(Location loc) {
		mSoundFinish.play(loc);
	}

	@Override
	protected void circleOutburstAction(Location loc, double radius) {
		mParticleFinishCircle.spawn(mLauncher, particle -> new PPCircle(particle, loc, radius));
	}

	@Override
	protected void dealDamageAction(Location loc) {
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mLauncher.getLocation(), mRadius)) {
			if (Math.abs(mStrengthAmount) > 0) {
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mob, STRENGTH_BUFF_NAME, new PercentDamageDealt(mBuffDuration, mStrengthAmount));
			}
			if (Math.abs(mSpeedAmount) > 0) {
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mob, SPEED_BUFF_NAME, new PercentSpeed(mBuffDuration, mSpeedAmount, SPEED_BUFF_NAME));
			}
			if (Math.abs(mKBRAmount) > 0) {
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mob, KBR_BUFF_NAME, new PercentKnockbackResist(mBuffDuration, mKBRAmount, SPEED_BUFF_NAME));
			}
			if (mCCImmuneBuff) {
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mob, CCIMMUNE_BUFF_NAME, new CCImmuneEffect(mBuffDuration));
			}
			mParticleFinish.spawn(mLauncher, mLauncher.getLocation().add(0, mLauncher.getHeight() / 2.0, 0));
		}

		mIsCasting = false;
	}

}
