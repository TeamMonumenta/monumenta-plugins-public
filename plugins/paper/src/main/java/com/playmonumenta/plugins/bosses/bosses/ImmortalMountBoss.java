package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

public class ImmortalMountBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_immortalmount";
	public static final int detectionRange = 40;

	private final LivingEntity mBoss;
	
	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ImmortalMountBoss(plugin, boss);
	}

	public ImmortalMountBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;
		
		List<Spell> passiveSpells = Arrays.asList(
			new SpellRunAction(() -> {
				if (mBoss.getPassengers().size() == 0) {
					mBoss.setHealth(0);
				}
			})
		);

		super.constructBoss(plugin, identityTag, boss, null, passiveSpells, detectionRange, null);
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		event.setDamage(0);
	}

}
