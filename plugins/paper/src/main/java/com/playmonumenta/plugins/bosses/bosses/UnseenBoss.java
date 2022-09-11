package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.Collections;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class UnseenBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_unseen";
	public static final int detectionRange = 50;

	public static class Parameters extends BossParameters {
		public double DAMAGE_INCREASE = 1.25;
	}

	final Parameters mParam;


	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new UnseenBoss(plugin, boss);
	}

	public UnseenBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = Parameters.getParameters(boss, identityTag, new UnseenBoss.Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (damagee instanceof Player player) {
			Vector lineOfSight = new Vector(0, 0, 1);
			lineOfSight.rotateAroundY(Math.toRadians(-player.getLocation().getYaw()));
			Vector mobToPlayer = mBoss.getLocation().toVector().subtract(player.getLocation().toVector());
			// If the mob is outside of 60 degrees to left or right increase damage
			if (lineOfSight.angle(mobToPlayer) > Math.toRadians(60.0)) {
				player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.8f);
				event.setDamage(event.getDamage() * mParam.DAMAGE_INCREASE);
			}
		}
	}
}


