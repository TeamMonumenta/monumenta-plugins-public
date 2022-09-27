package com.playmonumenta.plugins.bosses.spells.bluestrike;

import com.playmonumenta.plugins.bosses.bosses.bluestrike.Samwell;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;

public class SpellSamwellRegeneration extends Spell {
	private final double mPercentage;
	private int mTimer = 20;
	private final LivingEntity mBoss;
	private final Samwell mSamwell;

	public SpellSamwellRegeneration(LivingEntity boss, Samwell samwell, double percentage) {
		mPercentage = percentage;
		mBoss = boss;
		mSamwell = samwell;
	}

	@Override
	public void run() {
		mTimer--;
		if (mTimer <= 0) {
			if (!mSamwell.mHealedBefore && mBoss.getHealth() < EntityUtils.getMaxHealth(mBoss) * mPercentage - 10) {
				mBoss.getLocation().getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, "Samwell", Component.text("Don't you remember? I have the Blue Wool! It can heal me far faster than you can damage me, fools.", NamedTextColor.RED)));
				mSamwell.mHealedBefore = true;
			}

			mTimer = 20;
			mBoss.setHealth(EntityUtils.getMaxHealth(mBoss) * mPercentage);
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
