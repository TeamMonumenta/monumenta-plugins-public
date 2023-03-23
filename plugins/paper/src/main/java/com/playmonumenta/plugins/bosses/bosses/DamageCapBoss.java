package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Collections;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class DamageCapBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_capdamage";
	public static final int detectionRange = 40;
	private final double mAmount;
	private final boolean mPercent;

	public static class Parameters extends BossParameters {
		@BossParam(help = "Max amount of damage to be received in one packet. Default: 10")
		public double AMOUNT = 10;
		@BossParam(help = "Whether or not the amount specified should be % mob's max hp. Default: false")
		public boolean PERCENT = false;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new DamageCapBoss(plugin, boss);
	}

	public DamageCapBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		mAmount = p.AMOUNT;
		mPercent = p.PERCENT;

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		if (!(source instanceof Player)) {
			return;
		}

		if (!mPercent) {
			event.setDamage(Math.min(event.getDamage(), mAmount));
		} else {
			event.setDamage(Math.min(event.getDamage(), EntityUtils.getMaxHealth(mBoss) * mAmount / 100));
		}
	}
}
