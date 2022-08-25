package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BlueFireBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_bluefire";
	public static final int detectionRange = 20;

	public static final double[] FIRE_DAMAGE_MULTIPLIERS = {0, 0.2, 0.3, 0.4};
	public static final String FIRE_VULN_EFFECT_NAME = "BossBlueFireVulnerabilityEffect";

	private int mBlueTimeOfDay = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BlueFireBoss(plugin, boss);
	}

	public BlueFireBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		if (ScoreboardUtils.getScoreboardValue("$IsDungeon", "const").orElse(0) == 1) {
			long time = boss.getWorld().getTime();
			mBlueTimeOfDay = (int) Math.floor(time / 6000.0);

			// Pretty sure Time ranges from 0 to 23999, but just in case...
			if (mBlueTimeOfDay > 3) {
				mBlueTimeOfDay = 3;
			}
		}

		// Fire: Player takes 0 / 20 / 30 / 40% more fire damage
		List<Spell> passiveSpells = Arrays.asList(
			new SpellRunAction(() -> {
				if (mBlueTimeOfDay > 0) {
					List<Player> players = EntityUtils.getNearestPlayers(boss.getLocation(), 30);
					for (Player player : players) {
						EffectManager.getInstance().addEffect(player, FIRE_VULN_EFFECT_NAME, new PercentDamageReceived(100, FIRE_DAMAGE_MULTIPLIERS[mBlueTimeOfDay], EnumSet.of(DamageEvent.DamageType.FIRE)));
					}
				}
			})
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null, 100, 20);
	}
}
