package com.playmonumenta.plugins.abilities.cleric;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.UUID;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.attribute.Attribute;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.ThuribleBonusHealing;

public class ClericPassive extends Ability {

	private static final int RADIUS = 12;
	private static final int HEAL_INTERVAL = 20 * 5;
	private static final double PERCENT_HEAL = 0.05;
	private static final double HEALTH_LIMIT = 0.5;

	private static final String PERCENT_HEAL_EFFECT_NAME = "ThuribleProcessionPercentHealEffect";

	private static final Map<UUID, Integer> LAST_HEAL_TICK = new HashMap<>();

	private int mTimer = 0;

	public ClericPassive(Plugin plugin, Player player) {
		super(plugin, player, null);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class") == 3;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond && !mPlayer.isDead()) {
			mTimer += 20;
			if (mTimer % HEAL_INTERVAL == 0) {
				for (Player player : PlayerUtils.playersInRange(mPlayer, RADIUS, true)) {
					// Don't buff players that have their class disabled or who have PvP enabled
					if (player.getScoreboardTags().contains("disable_class") || AbilityManager.getManager().isPvPEnabled(player)) {
						continue;
					}

					double healPercent = PERCENT_HEAL;
					NavigableSet<Effect> effectGroup = mPlugin.mEffectManager.getEffects(player, PERCENT_HEAL_EFFECT_NAME);
					if (effectGroup != null) {
						ThuribleBonusHealing effect = (ThuribleBonusHealing) effectGroup.last();
						healPercent = effect.getMagnitude();
					}
					Integer lastHealTick = LAST_HEAL_TICK.get(player.getUniqueId());
					if (lastHealTick == null || player.getTicksLived() - LAST_HEAL_TICK.get(player.getUniqueId()) >= HEAL_INTERVAL) {
						LAST_HEAL_TICK.put(player.getUniqueId(), player.getTicksLived());
						double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
						double hp = player.getHealth() / player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
						if (hp <= HEALTH_LIMIT) {
							PlayerUtils.healPlayer(player, healPercent * maxHealth);
							int numHearts = (int) (healPercent * 20);
							player.getWorld().spawnParticle(Particle.HEART, (player.getLocation()).add(0, 2, 0), numHearts, 0.07, 0.07, 0.07, 0.001);
						}
					}
				}
			}
		}
	}

}
