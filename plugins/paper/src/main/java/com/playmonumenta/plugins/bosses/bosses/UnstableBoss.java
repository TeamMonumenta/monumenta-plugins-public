package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import java.util.Collections;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

public class UnstableBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_unstable";

	public static class Parameters extends BossParameters {
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
		mPFinal = BossParameters.getParameters(boss, identityTag, new Parameters());
		// Boss effectively does nothing lol
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), mPFinal.DETECTION, null);
	}

	@Override
	public void death(EntityDeathEvent event) {
		// The cause of death is NOT equal to a player attack
		if (event.getEntity().getKiller() == null) {
			mBoss.getLocation().getWorld().createExplosion(mBoss, mPFinal.EXPLOSION_POWER, mPFinal.SET_FIRE,
				mPFinal.BREAK_BLOCK);
		}
	}
}
