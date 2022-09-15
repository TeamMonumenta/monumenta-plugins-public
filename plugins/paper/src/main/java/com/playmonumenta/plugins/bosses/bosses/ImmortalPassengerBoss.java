package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class ImmortalPassengerBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_immortalpassenger";
	public static final int detectionRange = 40;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ImmortalPassengerBoss(plugin, boss);
	}

	public ImmortalPassengerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		List<Spell> passiveSpells = List.of(
			new SpellRunAction(() -> {
				if (boss.getVehicle() == null) {
					boss.setHealth(0);
					boss.remove();
				}
			}, 1, true)
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}

	@Override
	public void onHurt(DamageEvent event) {
		event.setDamage(0);
	}

}

