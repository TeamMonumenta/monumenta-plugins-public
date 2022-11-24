package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.Cleric;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.ThuribleBonusHealing;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;



public class Rejuvenation extends Ability {

	private static final int RADIUS = 12;
	private static final int HEAL_INTERVAL = 20 * 5;
	private static final double PERCENT_HEAL = 0.05;
	private static final double HEALTH_LIMIT = 0.5;

	private static final String PERCENT_HEAL_EFFECT_NAME = "ThuribleProcessionPercentHealEffect";

	private static final Map<UUID, Integer> LAST_HEAL_TICK = new HashMap<>();

	public static final String CHARM_THRESHOLD = "Rejuvenation Health Threshold";

	public static final AbilityInfo<Rejuvenation> INFO =
		new AbilityInfo<>(Rejuvenation.class, null, Rejuvenation::new)
			.canUse(player -> ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME).orElse(0) == Cleric.CLASS_ID);

	private int mTimer = 0;

	public Rejuvenation(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond && !mPlayer.isDead()) {
			mTimer += 20;
			if (mTimer % HEAL_INTERVAL == 0) {
				double healthLimit = HEALTH_LIMIT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_THRESHOLD);
				for (Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), RADIUS, true)) {
					// Don't buff players that have their class disabled
					if (player.getScoreboardTags().contains("disable_class")) {
						continue;
					}

					double healPercent = PERCENT_HEAL;
					NavigableSet<Effect> effectGroup = mPlugin.mEffectManager.getEffects(player, PERCENT_HEAL_EFFECT_NAME);
					if (effectGroup != null) {
						ThuribleBonusHealing effect = (ThuribleBonusHealing) effectGroup.last();
						healPercent = effect.getMagnitude();
					}
					Integer lastHealTick = LAST_HEAL_TICK.get(player.getUniqueId());
					if (lastHealTick == null || Bukkit.getServer().getCurrentTick() - lastHealTick >= HEAL_INTERVAL) {
						LAST_HEAL_TICK.put(player.getUniqueId(), Bukkit.getServer().getCurrentTick());
						double maxHealth = EntityUtils.getMaxHealth(player);
						double hp = player.getHealth() / maxHealth;
						if (hp <= healthLimit) {
							PlayerUtils.healPlayer(mPlugin, player, healPercent * maxHealth, mPlayer);
							int numHearts = (int) (healPercent * 20);
							new PartialParticle(Particle.HEART, player.getLocation().add(0, 2, 0), numHearts, 0.07, 0.07, 0.07, 0.001).spawnAsPlayerBuff(player);
						}
					}
				}
			}
		}
	}

}
