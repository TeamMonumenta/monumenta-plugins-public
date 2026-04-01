package com.playmonumenta.plugins.abilities.scout.hunter;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Scout;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.AbilityUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class Deadeye extends Ability {
	private static final double DAMAGE_BONUS = 0.02;
	private static final int MAX_DISTANCE = 25;

	public static final AbilityInfo<Deadeye> INFO =
		new AbilityInfo<>(Deadeye.class, "Deadeye", Deadeye::new)
			.description(getDescription())
			.canUse(player -> AbilityUtils.getSpecNum(player) == Scout.HUNTER_SPEC_ID);

	private final double mDamageBonusPerBlock;
	private final int mMaxDistance;

	private static final Set<DamageEvent.DamageType> PROJECTILE_TYPES = new HashSet<>(List.of(
		DamageEvent.DamageType.PROJECTILE,
		DamageEvent.DamageType.PROJECTILE_SKILL,
		DamageEvent.DamageType.PROJECTILE_ENCH
	));

	public Deadeye(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageBonusPerBlock = DAMAGE_BONUS;
		mMaxDistance = MAX_DISTANCE;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (!PROJECTILE_TYPES.contains(event.getType())) {
			return false;
		}

		double distance = mPlayer.getLocation().distance(enemy.getLocation());
		double dmgBoost = mDamageBonusPerBlock * Math.min(distance, mMaxDistance);

		if (ClassAbility.PREDATOR_STRIKE.equals(event.getAbility())) {
			dmgBoost *= 2;
		}

		dmgBoost += 1;

		event.updateDamageWithMultiplier(dmgBoost);

		return false;
	}

	public static Description<Deadeye> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO)
			.addLine("Deal an additional %p (p) per block")
			.statValues(stat(a -> a.mDamageBonusPerBlock, DAMAGE_BONUS))
			.addLine("distance to the mob, up to %d block scaling.")
			.statValues(stat(a -> a.mMaxDistance, MAX_DISTANCE))
			.addLine("*Predator Strike* gains double the damage bonus.").styles(UNDERLINED);
	}
}
