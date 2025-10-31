package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SpellPushPlayersAway extends Spell {
	private final Entity mLauncher;
	private final int mRadius;
	private final int mMaxNearTime;

	private final int mHorizontalOffset;
	private final PPCircle mParticles;
	private final PPLine mOuterLine;
	private final PPLine mInnerLine;

	/* Tracks how long players have been too close */
	Map<UUID, Integer> mPlayerNearTimes = new HashMap<>();

	// Tracks if player has been a given hint about this spell
	Map<UUID, Boolean> mPlayerHintsGiven = new HashMap<>();

	/* Push players away that have been too close for too long */
	public SpellPushPlayersAway(Entity launcher, int radius, int maxNearTime, int horizontalOffset) {
		mLauncher = launcher;
		mRadius = radius;
		mMaxNearTime = maxNearTime;
		mHorizontalOffset = horizontalOffset;
		mParticles = new PPCircle(Particle.SMOKE_NORMAL, mLauncher.getLocation().add(0, mHorizontalOffset, 0), mRadius)
			.ringMode(true)
			.countPerMeter(3);
		mOuterLine = new PPLine(Particle.REDSTONE, mLauncher.getLocation(), mLauncher.getLocation())
			.countPerMeter(2.25)
			.data(new Particle.DustOptions(Color.fromRGB(125, 125, 125), 2.0f)); // 125 125 125 -> 200 0 0
		mInnerLine = new PPLine(Particle.REDSTONE, mLauncher.getLocation(), mLauncher.getLocation())
			.countPerMeter(5)
			.data(new Particle.DustOptions(Color.fromRGB(0, 0, 0), 0.5f));
	}

	@Override
	public void run() {
		mParticles.spawnAsBoss();
		for (Player player : PlayerUtils.playersInRange(mLauncher.getLocation(), mRadius * 4, true)) {
			Integer nearTime = 0;
			Location pLoc = player.getLocation();
			if (pLoc.distance(mLauncher.getLocation()) < mRadius) {
				nearTime = mPlayerNearTimes.get(player.getUniqueId());
				if (nearTime == null) {
					nearTime = 0;
				}
				// Give player a hint if they haven't had one already
				if (mPlayerHintsGiven.get(player.getUniqueId()) == null || !mPlayerHintsGiven.get(player.getUniqueId())) {
					player.sendMessage(Component.text("I shouldn't stay in this ring for too long.", NamedTextColor.DARK_AQUA));
					mPlayerHintsGiven.put(player.getUniqueId(), true);
				}

				nearTime++;

				// Outer line color shifts from (125, 125, 125) to (200, 0, 0) as it gets closer to finishing
				float timePercent = (float) nearTime / mMaxNearTime;
				mOuterLine.data(new Particle.DustOptions(Color.fromRGB((int) (125 + timePercent * 75), (int) (125 - timePercent * 125), (int) (125 - timePercent * 125)), 2.0f))
					.location(mLauncher.getLocation().add(0, -3, 0), pLoc)
					.spawnAsBoss();
				mInnerLine.location(mLauncher.getLocation().add(0, -3, 0), pLoc)
					.spawnAsBoss();
				// Sound effects
				player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 2.0f - timePercent);

				if (nearTime >= mMaxNearTime) {
					Location lLoc = mLauncher.getLocation();
					Vector vect = new Vector(pLoc.getX() - lLoc.getX(), 0, pLoc.getZ() - lLoc.getZ());
					vect.normalize().setY(0.7f).multiply(2);
					player.setVelocity(vect);
					nearTime = 0;
					player.sendMessage(Component.text("[Masked Man] ", NamedTextColor.GOLD).append(Component.text("Get back, cur!", NamedTextColor.WHITE)));

					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 1.65f);
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 0.25f, 1.0f);
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 0.25f, 1.0f);
				}
			}
			mPlayerNearTimes.put(player.getUniqueId(), nearTime);
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}
}
