package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

public class ChestLockBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_lock_chest";

	public static class Parameters extends BossParameters {
		public double X = 0;
		public double Y = 0;
		public double Z = 0;
	}

	final Parameters mParam;
	final Location mLoc;

	public ChestLockBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = Parameters.getParameters(boss, identityTag, new Parameters());
		mLoc = new Location(mBoss.getWorld(), mParam.X, mParam.Y, mParam.Z);
		if (mLoc.getBlock().getState() instanceof Chest chest) {
			chest.setLock("ChestLockBoss");
			chest.update();
		}
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), 40, null);
	}

	@Override
	public void death(EntityDeathEvent event) {
		if (mLoc.getBlock().getState() instanceof Chest chest) {
			chest.setLock(null);
			chest.update();
		}
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ChestLockBoss(plugin, boss);
	}
}
