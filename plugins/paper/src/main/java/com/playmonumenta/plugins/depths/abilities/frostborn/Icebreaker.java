package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Inferno;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Icebreaker extends DepthsAbility {

	public static final String ABILITY_NAME = "Icebreaker";
	public static final double[] ICE_DAMAGE = {0.20, 0.27, 0.33, 0.40, 0.46, 0.60};
	public static final double[] EFFECT_DAMAGE = {0.10, 0.135, 0.165, 0.20, 0.23, 0.30};

	public static final DepthsAbilityInfo<Icebreaker> INFO =
		new DepthsAbilityInfo<>(Icebreaker.class, ABILITY_NAME, Icebreaker::new, DepthsTree.FROSTBORN, DepthsTrigger.PASSIVE)
			.displayItem(Material.TUBE_CORAL_FAN)
			.descriptions(Icebreaker::getDescription)
			.singleCharm(false);

	private final double mIceMultiplier;
	private final double mDebuffMultiplier;

	public Icebreaker(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mIceMultiplier = ICE_DAMAGE[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.ICEBREAKER_ICE_DAMAGE.mEffectName);
		mDebuffMultiplier = EFFECT_DAMAGE[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.ICEBREAKER_DEBUFF_DAMAGE.mEffectName);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		DamageEvent.DamageType type = event.getType();
		if (type == DamageEvent.DamageType.TRUE || type == DamageEvent.DamageType.OTHER) {
			return false;
		}
		event.updateDamageWithMultiplier(Math.max(getIceMultiplier(enemy), getDebuffMultiplier(enemy)));
		return false; // only changes event damage
	}

	private double getIceMultiplier(LivingEntity entity) {
		return DepthsUtils.isOnIce(entity) ? 1 + mIceMultiplier : 1;
	}

	private double getDebuffMultiplier(LivingEntity entity) {
		if (!PotionUtils.getNegativeEffects(mPlugin, entity).isEmpty() || EntityUtils.isStunned(entity) || EntityUtils.isParalyzed(mPlugin, entity) || EntityUtils.isBleeding(mPlugin, entity)
			|| EntityUtils.isSlowed(mPlugin, entity) || EntityUtils.isWeakened(mPlugin, entity) || EntityUtils.isSilenced(entity) || EntityUtils.isVulnerable(mPlugin, entity)
			|| entity.getFireTicks() > 0 || Inferno.hasInferno(mPlugin, entity) || EntityUtils.hasDamageOverTime(mPlugin, entity)) {
			return 1 + mDebuffMultiplier;
		}
		return 1;
	}

	private static Description<Icebreaker> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.add("Damage you deal is increased by ")
			.addPercent(a -> a.mIceMultiplier, ICE_DAMAGE[rarity - 1], false, true)
			.add(" if the mob is on ice or ")
			.addPercent(a -> a.mDebuffMultiplier, EFFECT_DAMAGE[rarity - 1], false, true)
			.add(" if the mob is debuffed.");
	}
}

