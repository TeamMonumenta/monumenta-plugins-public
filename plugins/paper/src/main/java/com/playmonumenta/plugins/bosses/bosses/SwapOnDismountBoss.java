package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellSwapOnDismount;
import com.playmonumenta.plugins.events.DamageEvent;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SwapOnDismountBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_swapondismount";
	public static final int detectionRange = 35;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new SwapOnDismountBoss(plugin, boss);
	}

	public SwapOnDismountBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		if (!(boss instanceof Mob)) {
			throw new Exception("boss_swapondismount only works on mobs!");
		}

		List<Spell> passiveSpells = Arrays.asList(new SpellSwapOnDismount(boss));

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}

	@Override
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		if (damager instanceof Mob && !(damager instanceof Player)) {
			event.setCancelled(true);
		}
	}
}
