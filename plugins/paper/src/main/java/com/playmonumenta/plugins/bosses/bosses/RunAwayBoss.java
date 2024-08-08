package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellRunAway;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;

public class RunAwayBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_runaway";
	private final SpellRunAway mRunAwaySpell;

	public RunAwayBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mRunAwaySpell = new SpellRunAway(boss, 4, 8, 0.6, player -> true);
		super.constructBoss(new SpellManager(List.of()), List.of(mRunAwaySpell), 20, null, 0, 1, false);
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		if (mRunAwaySpell.getState() != SpellRunAway.State.AGGRESSIVE) {
			event.setCancelled(true);
		}
	}

}
