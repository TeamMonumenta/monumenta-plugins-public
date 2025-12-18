package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.Rogue;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class Dethroner extends Ability {

	public static final double PASSIVE_DAMAGE_ELITE_MODIFIER = 1.3;
	public static final double PASSIVE_DAMAGE_BOSS_MODIFIER = 1.15;

	public static final AbilityInfo<Dethroner> INFO =
		new AbilityInfo<>(Dethroner.class, "Dethroner", Dethroner::new)
			.description(getDescription())
			.canUse(player -> AbilityUtils.getClassNum(player) == Rogue.CLASS_ID)
			.displayItem(Material.NETHERITE_SWORD);

	public Dethroner(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (enemy != null
			&& (event.getType() == DamageEvent.DamageType.MELEE || event.getType() == DamageEvent.DamageType.MELEE_ENCH || event.getType() == DamageEvent.DamageType.MELEE_SKILL)
			&& InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer)) {
			if (EntityUtils.isElite(enemy)) {
				event.updateDamageWithMultiplier(PASSIVE_DAMAGE_ELITE_MODIFIER);
			} else if (EntityUtils.isBoss(enemy)) {
				event.updateDamageWithMultiplier(PASSIVE_DAMAGE_BOSS_MODIFIER);
			}
		}
		return false; // increases event damage and does not cause another damage instance, so no recursion
	}

	public static Description<Dethroner> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO)
			.addLine("While holding two swords, you deal %p")
				.statValues(stat(PASSIVE_DAMAGE_ELITE_MODIFIER - 1))
			.addLine("more melee damage to Elites and %p")
				.statValues(stat(PASSIVE_DAMAGE_BOSS_MODIFIER - 1))
			.addLine("more melee damage to Bosses.");
	}
}
