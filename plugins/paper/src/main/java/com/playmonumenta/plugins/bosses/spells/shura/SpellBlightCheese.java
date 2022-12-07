package com.playmonumenta.plugins.bosses.spells.shura;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellBlightCheese extends Spell {
	int mT = 0;
	LivingEntity mBoss;
	Location mCenter;
	double mRange;
	List<Player> mWarned = new ArrayList<>();
	private static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f);
	private PartialParticle mPRed;
	private PartialParticle mPWitch;

	public SpellBlightCheese(LivingEntity boss, double range, Location center) {
		mBoss = boss;
		mRange = range;
		mCenter = center;
		mPRed = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 3, 0.4, 0.4, 0.4, 0.1, RED);
		mPWitch = new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation(), 3, 0.4, 0.4, 0.4, 0.1);
	}

	@Override
	public void run() {
		mT += 5;
		if (mT > 20) {
			for (Player p : PlayerUtils.playersInRange(mCenter, mRange, true)) {
				if (p.getLocation().getY() > mCenter.getY() + 4 && !PlayerUtils.isFreeFalling(p)) {
					// too high
					BossUtils.bossDamagePercent(mBoss, p, 0.2, "Blight Remnant");
					World world = mBoss.getWorld();
					world.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.HOSTILE, 1, 2);
					mPRed.location(p.getEyeLocation()).spawnAsBoss();
					mPWitch.location(p.getLocation()).spawnAsBoss();
					if (!mWarned.contains(p)) {
						p.sendMessage(ChatColor.AQUA + "Blight around the Sanctum gathers above. Stay close to the ground!");
						mWarned.add(p);
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
