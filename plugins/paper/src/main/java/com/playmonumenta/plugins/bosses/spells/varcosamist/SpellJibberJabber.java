package com.playmonumenta.plugins.bosses.spells.varcosamist;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellJibberJabber extends Spell {
	private final String[] mSpeak;
	private final Location mLocation;
	private final int mRadius;
	private int mTimer = 20 * 50;

	public SpellJibberJabber(LivingEntity boss, String[] speak, int radius) {
		mSpeak = speak;
		mLocation = boss.getLocation();
		mRadius = radius;
	}

	@Override
	public void run() {
		mTimer--;
		if (mTimer == 0) {
			mTimer = 20 * 50;
			mTimer = FastUtils.RANDOM.nextInt(20 * 40) + (20 * 20);
			for (Player players : PlayerUtils.playersInRange(mLocation, mRadius, true)) {
				players.sendMessage(Component.text(mSpeak[FastUtils.RANDOM.nextInt(mSpeak.length)], NamedTextColor.RED));
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
