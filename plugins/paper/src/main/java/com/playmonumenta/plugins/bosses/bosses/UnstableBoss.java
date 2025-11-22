package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import java.util.Collections;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.SPAWNER_COUNT_METAKEY;

public class UnstableBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_unstable";

	public static class Parameters extends BossParameters {
		public int DETECTION = 20;
		public int EXPLOSION_POWER = 4;
		public boolean BREAK_BLOCK = true;
		public boolean SET_FIRE = false;
		public LoSPool POOL = LoSPool.LibraryPool.EMPTY;
	}

	private final Parameters mPFinal;

	public UnstableBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mPFinal = BossParameters.getParameters(boss, identityTag, new Parameters());
		// Boss effectively does nothing lol
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), mPFinal.DETECTION, null);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		// The cause of death is NOT equal to a player attack
		if (event == null || event.getEntity().getKiller() == null) {
			mBoss.getLocation().getWorld().createExplosion(mBoss, mPFinal.EXPLOSION_POWER, mPFinal.SET_FIRE,
				mPFinal.BREAK_BLOCK);
			Entity entity = mPFinal.POOL.spawn(mBoss.getLocation());

			// Include the original mob's metadata for spawner counting to prevent mob farming
			if (entity != null && mBoss.hasMetadata(SPAWNER_COUNT_METAKEY)) {
				entity.setMetadata(SPAWNER_COUNT_METAKEY, mBoss.getMetadata(SPAWNER_COUNT_METAKEY).get(0));
			}
		}
	}
}
