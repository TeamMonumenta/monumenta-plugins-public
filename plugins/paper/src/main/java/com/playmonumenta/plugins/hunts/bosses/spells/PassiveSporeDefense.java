package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.SporousAmalgam;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class PassiveSporeDefense extends Spell {
	public static final int VULNERABILITY_DURATION = 20 * 10;
	public static final double RESISTANCE_MULTI = 0.10;
	public static final double VULNERABILITY_MULTI = 0.65;

	private final SporousAmalgam mSporeBeast;
	private final LivingEntity mBoss;

	private boolean mIsVulnerable;
	private int mVulnerabilityStartingTick;

	private int mTicks;

	public PassiveSporeDefense(SporousAmalgam sporeBeast) {
		mSporeBeast = sporeBeast;
		mBoss = sporeBeast.mBoss;

		mIsVulnerable = false;
		mVulnerabilityStartingTick = 0;
		mTicks = 1;
	}

	@Override
	public void run() {
		if (mIsVulnerable && mTicks - mVulnerabilityStartingTick >= VULNERABILITY_DURATION) {
			for (Player p : mSporeBeast.getPlayersInOutRange()) {
				for (int i = 0; i < 4; i++) {
					p.playSound(mBoss, Sound.ENTITY_RAVAGER_ROAR, 2f, 1f - 0.1f * i);
				}
			}
			mIsVulnerable = false;
		}
		mTicks++;
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (isVulnerable()) {
			event.setFlatDamage(event.getFlatDamage() * VULNERABILITY_MULTI);
		} else {
			event.setFlatDamage(event.getFlatDamage() * RESISTANCE_MULTI);
		}
		double health = mBoss.getHealth();
		double damage = event.getFinalDamage(false);
		if (health - damage < (SporousAmalgam.HEALTH / 100) * SporousAmalgam.LAST_PHASE) {
			mBoss.setHealth(SporousAmalgam.HEALTH / 100 * SporousAmalgam.LAST_PHASE - 1);
			event.setCancelled(true);
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	public boolean isVulnerable() {
		return mIsVulnerable;
	}

	public void setVulnerableState() {
		mIsVulnerable = true;
		mVulnerabilityStartingTick = mTicks;
	}

	public void removeVulnerable() {
		mIsVulnerable = false;
	}
}
