package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellDistanceCloser;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

public class DistanceCloserBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_distancecloser";
	public static final int detectionRange = 24;

	public DistanceCloserBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		if (!(boss instanceof Mob mob)) {
			return;
		}

		double distance = 0;
		double speed = 0;

		for (String tag : boss.getScoreboardTags()) {
			if (tag.startsWith(identityTag) && !tag.equals(identityTag)) {
				try {
					String[] values = tag.substring(identityTag.length()).split(",");
					distance = Integer.parseInt(values[0]);
					speed = Integer.parseInt(values[1]) / 100.0;
				} catch (Exception e) {
					e.printStackTrace();
				}

				break;
			}
		}

		List<Spell> passiveSpells = List.of(
			new SpellDistanceCloser(mob, distance, speed)
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}

}
