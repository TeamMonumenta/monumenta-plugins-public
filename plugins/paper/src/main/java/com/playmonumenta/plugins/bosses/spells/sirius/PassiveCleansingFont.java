package com.playmonumenta.plugins.bosses.spells.sirius;

import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.FlatHealthBoost;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class PassiveCleansingFont extends Spell {
	private int mCount;
	private final Location mFontLocOne;
	private final Location mFontLocTwo;
	private boolean mWeakened;
	private final Sirius mSirius;
	private static final int RADIUS = 3;
	private static final int TICKSBETWEENCLEANSE = 2 * 20;
	private static final int HEALAMOUNT = 4;

	public PassiveCleansingFont(Sirius sirius) {
		mFontLocOne = sirius.mBoss.getLocation().clone().add(-6.5, 12, 17);
		mFontLocTwo = sirius.mBoss.getLocation().clone().add(-6.5, 12, -17);
		mSirius = sirius;
		mWeakened = false;
		mCount = 0;
	}

	@Override
	public void run() {
		if (mSirius.mBlocks <= 10 && !mWeakened) {
			new PPExplosion(Particle.END_ROD, mFontLocOne).count(25).delta(RADIUS).spawnAsBoss();
			new PPExplosion(Particle.END_ROD, mFontLocTwo).count(25).delta(RADIUS).spawnAsBoss();
			for (Player p : mSirius.getPlayersInArena(false)) {
				MessagingUtils.sendNPCMessage(p, "Tuulen", Component.text("Be warned! I am focusing more of my power away from the cleansing fonts so we can focus all our fire on Sirius!", NamedTextColor.GRAY));
			}
			mWeakened = true;
		}
		if (mWeakened && mSirius.mBlocks > 11) {
			mWeakened = false;
			for (Player p : mSirius.getPlayersInArena(false)) {
				MessagingUtils.sendNPCMessage(p, "Tuulen", Component.text("Sirius has advanced I am redirecting my power into the cleansing fonts.", NamedTextColor.GRAY));
			}
		}
		new PPCircle(Particle.WAX_OFF, mFontLocOne, RADIUS).count(20).ringMode(true).spawnAsBoss();
		new PPCircle(Particle.WAX_OFF, mFontLocTwo, RADIUS).count(20).ringMode(true).spawnAsBoss();
		for (Player p : PlayerUtils.playersInRange(mFontLocOne, RADIUS, true, true)) {
			cleanse(p);
		}
		for (Player p : PlayerUtils.playersInRange(mFontLocTwo, RADIUS, true, true)) {
			cleanse(p);
		}
		mCount++;
		if (mCount >= 15 && mSirius.mCheeseLock) {
			mCount = 0;
			mSirius.mStarBlightConverter.restoreFullCircle(mFontLocOne, RADIUS + 1);
			mSirius.mStarBlightConverter.restoreFullCircle(mFontLocTwo, RADIUS + 1);
		}

	}

	private void cleanse(Player p) {
		Effect blight = EffectManager.getInstance().getActiveEffect(p, PassiveStarBlight.STARBLIGHTAG);
		if (blight != null) {
			double magnitude = blight.getMagnitude() * -1;
			if (PassiveStarBlight.STARBLIGHTDURATION - blight.getDuration() >= TICKSBETWEENCLEANSE) {
				EffectManager.getInstance().clearEffects(p, PassiveStarBlight.STARBLIGHTAG);
				if (magnitude + HEALAMOUNT < 0) {
					p.playSound(p, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 0.7f, 1.2f);
					p.playSound(p, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 0.8f, 1f);
					p.playSound(p, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.HOSTILE, 0.5f, 1.5f);
					Effect fail = EffectManager.getInstance().getActiveEffect(p, Sirius.FAIL_PARTICIPATION_TAG);
					if (fail == null || fail.getMagnitude() < 3) {
						if (!mWeakened) {
							EffectManager.getInstance().addEffect(p, PassiveStarBlight.STARBLIGHTAG, new FlatHealthBoost(PassiveStarBlight.STARBLIGHTDURATION, magnitude + HEALAMOUNT, PassiveStarBlight.STARBLIGHTAG));
						} else {
							EffectManager.getInstance().addEffect(p, PassiveStarBlight.STARBLIGHTAG, new FlatHealthBoost(PassiveStarBlight.STARBLIGHTDURATION, magnitude + (HEALAMOUNT / 2.0), PassiveStarBlight.STARBLIGHTAG));
						}
					} else {
						if (!mWeakened) {
							EffectManager.getInstance().addEffect(p, PassiveStarBlight.STARBLIGHTAG, new FlatHealthBoost(PassiveStarBlight.STARBLIGHTDURATION, magnitude + (HEALAMOUNT / 2.0), PassiveStarBlight.STARBLIGHTAG));
						}
					}
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
