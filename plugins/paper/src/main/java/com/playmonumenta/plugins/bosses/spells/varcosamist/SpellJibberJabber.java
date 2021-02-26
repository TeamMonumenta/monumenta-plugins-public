package com.playmonumenta.plugins.bosses.spells.varcosamist;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellJibberJabber extends Spell {
	private String[] mSpeak;
	private Location mLocation;
	private int mRadius;
	private int mT = 20 * 50;

	public SpellJibberJabber(LivingEntity boss, String[] speak, int radius) {
		mSpeak = speak;
		mLocation = boss.getLocation();
		mRadius = radius;
	}

	@Override
	public void run() {
		mT--;
		if (mT == 0) {
			mT = 20 * 50;
			mT = FastUtils.RANDOM.nextInt(20 * 40) + (20 * 20);
			PlayerUtils.executeCommandOnNearbyPlayers(mLocation, mRadius, "tellraw @s [\"\",{\"text\":\"" + mSpeak[FastUtils.RANDOM.nextInt(mSpeak.length)] + "\",\"color\":\"red\"}]");
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
