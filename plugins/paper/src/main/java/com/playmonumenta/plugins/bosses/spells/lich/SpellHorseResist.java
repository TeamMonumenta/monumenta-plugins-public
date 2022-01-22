package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class SpellHorseResist extends Spell {

	private final LivingEntity mBoss;
	private final Location mCenter;
	private final double mRange;

	public SpellHorseResist(LivingEntity boss, Location loc, double range) {
		mBoss = boss;
		mCenter = loc;
		mRange = range;
	}

	@Override
	public void run() {
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mCenter, mRange, 10, mRange);
		mobs.removeIf(m -> !m.getScoreboardTags().contains("Boss"));
		if (mobs.size() > 1) {
			mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 2, 0));
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
