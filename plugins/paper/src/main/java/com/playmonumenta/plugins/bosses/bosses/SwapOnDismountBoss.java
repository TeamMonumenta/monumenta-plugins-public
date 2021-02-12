package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellSwapOnDismount;

public class SwapOnDismountBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_swapondismount";
	public static final int detectionRange = 35;

	Mob mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new SwapOnDismountBoss(plugin, boss);
	}

	public SwapOnDismountBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		if (!(boss instanceof Mob)) {
			throw new Exception("boss_swapondismount only works on mobs!");
		}

		mBoss = (Mob)boss;
		List<Spell> passiveSpells = Arrays.asList(new SpellSwapOnDismount(mBoss));

		super.constructBoss(null, passiveSpells, detectionRange, null);
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Mob && !(event.getDamager() instanceof Player)) {
			event.setCancelled(true);
		}
	}
}
