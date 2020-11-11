package com.playmonumenta.plugins.bosses.spells;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellCyanSummon extends Spell {
	private static final int PLAYER_RANGE = 16;
	private static final int MAX_NEARBY_SUMMONS = 12;

	private final LivingEntity mBoss;

	public SpellCyanSummon(LivingEntity boss) {
		mBoss = boss;
	}

	@Override
	public void run() {
		for (int i = 0; i < 3; i++) {
			LibraryOfSoulsIntegration.summon(mBoss.getLocation(), "SeekingFlesh");
		}
	}

	@Override
	public boolean canRun() {
		List<Entity> nearbyEntities = mBoss.getNearbyEntities(PLAYER_RANGE, PLAYER_RANGE, PLAYER_RANGE);

		if (nearbyEntities.stream().filter(
				e -> e.getType().equals(EntityType.ZOMBIFIED_PIGLIN)
			).count() > MAX_NEARBY_SUMMONS) {
			return false;

		}

		if (((mBoss instanceof Mob) && (((Mob)mBoss).getTarget() instanceof Player))) {
			return true;
		}

		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), PLAYER_RANGE)) {
			if (LocationUtils.hasLineOfSight(mBoss, player)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public int duration() {
		return 20 * 20; //20 seconds
	}
}
