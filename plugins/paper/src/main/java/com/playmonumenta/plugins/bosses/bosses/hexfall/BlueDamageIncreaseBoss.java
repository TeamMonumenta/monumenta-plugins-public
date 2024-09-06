package com.playmonumenta.plugins.bosses.bosses.hexfall;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.hexfall.BluePercentDamageDealt;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

public class BlueDamageIncreaseBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_bluedamageincrease";
	public static final int detectionRange = 36;
	public static final int BUFF_RANGE = 40;

	private final Plugin mMonumentaPlugin;

	public BlueDamageIncreaseBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mMonumentaPlugin = plugin;
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (event != null) {
			for (Player player : PlayerUtils.playersInRange(event.getEntity().getLocation(), BUFF_RANGE, true, false)) {
				Effect currentIncrease = mMonumentaPlugin.mEffectManager.getActiveEffect(player, BluePercentDamageDealt.class);

				double amount = 1;
				if (currentIncrease != null) {
					amount = amount + currentIncrease.getMagnitude();
					currentIncrease.clearEffect();
				}

				mMonumentaPlugin.mEffectManager.addEffect(player, BluePercentDamageDealt.GENERIC_NAME, new BluePercentDamageDealt(60 * 20, amount, null, 0, (entity, enemy) -> enemy.getScoreboardTags().contains("boss_harrakfar")));

				new PPExplosion(Particle.SOUL_FIRE_FLAME, player.getLocation())
					.count(5)
					.directionalMode(true)
					.delta(1, 1, 1)
					.spawnAsBoss();
			}
		}
	}
}

