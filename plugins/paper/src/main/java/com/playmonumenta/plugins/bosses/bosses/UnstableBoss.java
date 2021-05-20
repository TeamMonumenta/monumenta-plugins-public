package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.utils.BossUtils;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

public class UnstableBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_unstable";

	public static class Parameters {
		public int DETECTION = 20;
		public int EXPLOSION_POWER = 4;
		public boolean BREAK_BLOCK = true;
		public boolean SET_FIRE = false;
	}

	private final Parameters mPFinal;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new UnstableBoss(plugin, boss);
	}

	public UnstableBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mPFinal = BossUtils.getParameters(boss, identityTag, new Parameters());
		// Boss effectively does nothing lol
		super.constructBoss(null, null, mPFinal.DETECTION, null);
	}

	@Override
	public void death(EntityDeathEvent event) {
		mBoss.getLocation().getWorld().createExplosion(mBoss, mPFinal.EXPLOSION_POWER, mPFinal.SET_FIRE, mPFinal.BREAK_BLOCK);
	}
}
