package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.Warlock;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public class Culling extends Ability {
	private static final int PASSIVE_DURATION = TICKS_PER_SECOND * 6;
	private static final String WARLOCK_PASSIVE_EFFECT_NAME = "CullingPercentDamageResistEffect";
	private static final double WARLOCK_PASSIVE_DAMAGE_REDUCTION_PERCENT = 0.1;

	public static final String CHARM_RESISTANCE = "Culling Resistance Amplifier";
	public static final String CHARM_DURATION = "Culling Duration";

	public static final AbilityInfo<Culling> INFO =
		new AbilityInfo<>(Culling.class, "Culling", Culling::new)
			.description(getDescription())
			.canUse(player -> AbilityUtils.getClassNum(player) == Warlock.CLASS_ID);

	private final double mResistancePotency;
	private final int mResistanceDuration;

	public Culling(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mResistancePotency = WARLOCK_PASSIVE_DAMAGE_REDUCTION_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RESISTANCE);
		mResistanceDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, PASSIVE_DURATION);
	}

	@Override
	public void entityDeathEvent(final EntityDeathEvent event, final boolean shouldGenDrops) {
		if (EntityUtils.isHostileMob(event.getEntity()) && ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())) {
			mPlugin.mEffectManager.addEffect(mPlayer, WARLOCK_PASSIVE_EFFECT_NAME,
				new PercentDamageReceived(mResistanceDuration, -mResistancePotency).deleteOnAbilityUpdate(true));
		}
	}

	private static Description<Culling> getDescription() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Killing an enemy while holding a scythe grants ")
			.addPercent(a -> a.mResistancePotency, WARLOCK_PASSIVE_DAMAGE_REDUCTION_PERCENT)
			.add(" damage reduction for ")
			.addDuration(a -> a.mResistanceDuration, PASSIVE_DURATION)
			.add(" seconds.");
	}
}
