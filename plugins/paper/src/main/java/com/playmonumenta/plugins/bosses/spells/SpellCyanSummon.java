package com.playmonumenta.plugins.bosses.spells;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellCyanSummon extends Spell {
	private static final int PLAYER_RANGE = 16;
	private static final int MAX_NEARBY_SUMMONS = 12;
	private final EnumSet<EntityType> mTypes = EnumSet.of(
			EntityType.ZOMBIFIED_PIGLIN
			);

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
		List<LivingEntity> nearbyEntities = EntityUtils.getNearbyMobs(mBoss.getLocation(), PLAYER_RANGE, mTypes);

		if (nearbyEntities.size() > MAX_NEARBY_SUMMONS) {
			return false;

		}

		if (((mBoss instanceof Mob) && (((Mob)mBoss).getTarget() instanceof Player))) {
			return true;
		}

		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), PLAYER_RANGE, true)) {
			if (LocationUtils.hasLineOfSight(mBoss, player)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 20; //20 seconds
	}
}
