package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.Mage;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class Channeling extends Ability {
	public static final String CHARM_DAMAGE = "Channeling Damage Modifier";
	public static final String CHARM_HITS = "Channeling Hits";
	public static final double PERCENT_MELEE_INCREASE = 0.2;
	public static final int HITS = 1;

	public static final AbilityInfo<Channeling> INFO =
		new AbilityInfo<>(Channeling.class, "Channeling", Channeling::new)
			.description(getDescription())
			.canUse(player -> AbilityUtils.getClassNum(player) == Mage.CLASS_ID)
			.priorityAmount(999)
			.displayItem(Material.DEAD_TUBE_CORAL);

	private final double mDamage;
	private final int mHits;

	private int mCast = 0;

	public Channeling(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = PERCENT_MELEE_INCREASE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mHits = HITS + (int) CharmManager.getLevel(player, CHARM_HITS);
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		mCast = mHits;
		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE
			&& mCast > 0
			&& mPlugin.mItemStatManager.getEnchantmentLevel(mPlayer, EnchantmentType.MAGIC_WAND) > 0) {
			event.updateDamageWithMultiplier(1 + mDamage);
			mCast--;
		}
		return false; // only changes event damage
	}

	public static DescriptionBuilder<Channeling> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO)
			.addLine("Your ability damage is boosted by")
			.addLine("your wand's Spell Power stat.")
			.addLine()
			.addLine("After using an ability, your next")
			.addIfElse((a, p) -> a != null && a.mHits != HITS && ServerProperties.getClassSpecializationsEnabled(p),
				desc -> desc.addLine("%d attacks (m) deal %p more")
					.statValues(stat(a -> a.mHits, HITS), stat(a -> a.mDamage, PERCENT_MELEE_INCREASE))
					.addLine("damage."),
				desc -> desc.addLine("attack (m) deals %p more damage.")
					.statValues(stat(a -> a.mDamage, PERCENT_MELEE_INCREASE)));
	}
}
