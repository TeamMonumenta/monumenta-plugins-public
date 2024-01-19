package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.bosses.spells.SpellTargetVisiblePlayer;
import org.bukkit.entity.Dolphin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Golem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;

public class PlayerTargetBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_targetplayer";
	public static final int detectionRange = 30;

	SpellTargetVisiblePlayer mSpellTargetPlayer;

	public PlayerTargetBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		if (!(boss instanceof Mob mob)) {
			throw new Exception(identityTag + " only works on mobs!");
		}

		if (boss instanceof Wolf || boss instanceof Golem || boss instanceof Dolphin || boss instanceof Ocelot) {
			boss.setRemoveWhenFarAway(true);
		}

		mSpellTargetPlayer = new SpellTargetVisiblePlayer(mob, detectionRange, 60, 160);

		Spell spell = new SpellRunAction(() -> {
			if (boss instanceof Wolf wolf && wolf.isTamed()) {
				wolf.setAngry(false);
			} else {
				mSpellTargetPlayer.run();
			}
		});

		super.constructBoss(spell, detectionRange);
	}

	/* Only allow mobs with this ability to target players */
	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		if (!isTargetable(event.getTarget())) {
			event.setCancelled(true);
		}
	}

	public void setTarget(Player target) {
		mSpellTargetPlayer.setTarget(target);
	}

	public static boolean isTargetable(Entity entity) {
		return entity instanceof Player || entity.getScoreboardTags().contains("playertarget_override");
	}

}

