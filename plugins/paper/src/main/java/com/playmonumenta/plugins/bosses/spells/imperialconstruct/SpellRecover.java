package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.bosses.bosses.ImperialConstruct;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellRecover extends Spell {

	private LivingEntity mBoss;
	private Location mTeleLoc;

	private int mTimer = 0;
	private int mDuration = 20;

	public SpellRecover(LivingEntity boss, Location currentLoc) {
		mBoss = boss;
		mTeleLoc = currentLoc.clone();
	}

	@Override
	public void run() {
		if (mTeleLoc.getY() - mBoss.getLocation().getY() >= 10 || mTeleLoc.getY() - mBoss.getLocation().getY() <= -10) {
			teleport(mTeleLoc);
		}
		mTimer += 2;
		if (mTimer >= mDuration) {
			mTimer = 0;

			List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), ImperialConstruct.detectionRange, true);
			if (players.isEmpty()) {
				return;
			}

			boolean jump = true;
			for (Player p : players) {
				if (p.getLocation().distance(mBoss.getLocation()) <= 8) {
					jump = false;
				}
			}

			if (jump) {
				mTimer = 0;
			}


		}
	}

	//Teleport with special effects
	private void teleport(Location loc) {
		World world = loc.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.CLOUD, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(mBoss);
		mBoss.teleport(loc);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(mBoss);
	}

	public void setLocation(Location loc) {
		mTeleLoc = loc.clone();
		teleport(mTeleLoc);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
