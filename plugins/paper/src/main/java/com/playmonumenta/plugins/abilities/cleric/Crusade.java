package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.Cleric;
import com.playmonumenta.plugins.effects.CrusadeTag;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;


public class Crusade extends Ability {
	public static final int TAG_DURATION = 10 * 20;
	public static final String CHARM_DURATION = "Crusade Duration";

	public static final AbilityInfo<Crusade> INFO =
		new AbilityInfo<>(Crusade.class, "Crusade", Crusade::new)
			.description(getDescription())
			.canUse(player -> AbilityUtils.getClassNum(player) == Cleric.CLASS_ID);

	private final int mDuration;

	public Crusade(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, TAG_DURATION);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getAbility() == null || event.getAbility().isFake()) {
			return false;
		}
		addCrusadeTag(enemy);

		return false;
	}

	public static boolean enemyTriggersAbilities(LivingEntity enemy) {
		return EntityUtils.isUndead(enemy) || EntityUtils.isHumanlike(enemy) || Plugin.getInstance().mEffectManager.hasEffect(enemy, CrusadeTag.class);
	}

	private void addCrusadeTag(LivingEntity enemy) {
		if (!EntityUtils.isUndead(enemy) && !EntityUtils.isHumanlike(enemy)) {
			mPlugin.mEffectManager.addEffect(enemy, "CrusadeTag", new CrusadeTag(mDuration));
		}
	}

	public static void addCrusadeTag(LivingEntity enemy, @Nullable Crusade crusade) {
		if (crusade == null) {
			return;
		}
		crusade.addCrusadeTag(enemy);
	}

	private static Description<Crusade> getDescription() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("After being damaged or debuffed by an ability, any mob will be treated as a Heretic by your abilities for ")
			.addDuration(a -> a.mDuration, TAG_DURATION)
			.add(" seconds.");
	}
}
