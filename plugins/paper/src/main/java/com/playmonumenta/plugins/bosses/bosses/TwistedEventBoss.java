package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.abilities.delves.Twisted;
import com.playmonumenta.plugins.utils.FastUtils;

public class TwistedEventBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_twistedevent";
	public static final int detectionRange = 24;

	private static final int DELAY_MIN = 20 * 2;
	private static final int DELAY_MAX = 20 * 8;

	private final com.playmonumenta.plugins.Plugin mPlugin;
	private boolean mTriggered = false;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TwistedEventBoss(plugin, boss);
	}

	public TwistedEventBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mPlugin = com.playmonumenta.plugins.Plugin.getInstance();

		super.constructBoss(null, null, detectionRange, null);
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		if (mTriggered) {
			return;
		}

		Entity target = event.getTarget();
		if (target instanceof Player) {
			mTriggered = true;

			BukkitRunnable runnable = new BukkitRunnable() {
				@Override
				public void run() {
					Twisted.runEvent(mPlugin, mBoss);
				}
			};

			runnable.runTaskLater(mPlugin, FastUtils.RANDOM.nextInt(DELAY_MAX - DELAY_MIN) + DELAY_MIN);
		}
	}

}
