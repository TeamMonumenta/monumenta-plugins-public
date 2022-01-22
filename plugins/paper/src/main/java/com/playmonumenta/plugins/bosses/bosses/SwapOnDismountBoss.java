package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellSwapOnDismount;
import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

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
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		if (damager instanceof Mob && !(damager instanceof Player)) {
			event.setCancelled(true);
		}
	}
}
