package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Collections;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class UnseenBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_unseen";
	public static final int detectionRange = 50;

	public static class Parameters extends BossParameters {
		public double DAMAGE_INCREASE = 1.25;
	}

	final Parameters mParam;

	public UnseenBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = Parameters.getParameters(boss, identityTag, new UnseenBoss.Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (damagee instanceof Player player && !EntityUtils.isInFieldOfView(player, mBoss)) {
			player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.HOSTILE, 1f, 0.8f);
			event.setFlatDamage(event.getDamage() * mParam.DAMAGE_INCREASE);
		}
	}
}


