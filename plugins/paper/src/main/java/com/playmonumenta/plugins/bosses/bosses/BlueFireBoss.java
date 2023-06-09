package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BlueFireBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_bluefire";
	public static final int detectionRange = 20;

	public static final double[] FIRE_DAMAGE_MULTIPLIERS = {0, 0.1, 0.2, 0.3};
	public static final String FIRE_VULN_EFFECT_NAME = "BossBlueFireVulnerabilityEffect";

	private int mBlueTimeOfDay = 0;

	public BlueFireBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		mBlueTimeOfDay = BossUtils.getBlueTimeOfDay(boss);

		// Fire: Player takes 0 / 20 / 30 / 40% more fire damage
		List<Spell> passiveSpells = List.of(
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
