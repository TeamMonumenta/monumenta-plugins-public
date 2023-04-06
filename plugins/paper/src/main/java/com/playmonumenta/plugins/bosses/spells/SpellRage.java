package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.effects.CCImmuneEffect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
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

	public SpellRage(Plugin plugin, LivingEntity launcher, int radius, int time, int cooldown, int buffDuration, double strengthAmount, double speedAmount, double kbrAmount, boolean ccImmune) {
		super(plugin, launcher, radius, time, cooldown, false, Sound.UI_TOAST_OUT);
		mBuffDuration = buffDuration;
		mSpeedAmount = speedAmount;
		mStrengthAmount = strengthAmount;
		mKBRAmount = kbrAmount;
		mCCImmuneBuff = ccImmune;
	}

	@Override
	protected void chargeAuraAction(Location loc) {
		World world = loc.getWorld();

		if (!mIsCasting) {
			world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 1f, 0.6f);
			mIsCasting = true;
		}

		new PartialParticle(Particle.SPELL_WITCH, loc, 1, mRadius / 2.0, mRadius / 2.0, mRadius / 2.0, 0.05).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void chargeCircleAction(Location loc) {
		new PartialParticle(Particle.CRIT_MAGIC, loc, 1, 0.25, 0.25, 0.25, 0.1).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void outburstAction(Location loc) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 1.5f, 1.5f);
		world.playSound(loc, Sound.ENTITY_RAVAGER_HURT, SoundCategory.HOSTILE, 1.5f, 0.5f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1.5f, 0.75f);
	}

	@Override
	protected void circleOutburstAction(Location loc) {
		new PartialParticle(Particle.SPELL_WITCH, loc, 1, 0.1, 0.1, 0.1, 0.3).spawnAsEntityActive(mLauncher);
		new PartialParticle(Particle.BUBBLE_POP, loc, 2, 0.25, 0.25, 0.25, 0.1).spawnAsEntityActive(mLauncher);
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
			new PartialParticle(Particle.SPELL_WITCH, mob.getLocation().add(0, mob.getHeight() / 2, 0), 15, 0.25, 0.45, 0.25, 1).spawnAsEntityActive(mLauncher);
			new PartialParticle(Particle.VILLAGER_ANGRY, mob.getLocation().add(0, mob.getHeight() / 2, 0), 5, 0.35, 0.5, 0.35, 0).spawnAsEntityActive(mLauncher);
		}

		mIsCasting = false;
	}

}
