package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DepthsDethroner extends DepthsAbility {

	public static final String ABILITY_NAME = "Dethroner";
	public static final double[] ELITE_DAMAGE = {0.14, 0.21, 0.28, 0.35, 0.42, 0.56};
	public static final double[] BOSS_DAMAGE = {0.10, 0.15, 0.20, 0.25, 0.30, 0.40};

	public static final DepthsAbilityInfo<DepthsDethroner> INFO =
		new DepthsAbilityInfo<>(DepthsDethroner.class, ABILITY_NAME, DepthsDethroner::new, DepthsTree.SHADOWDANCER, DepthsTrigger.PASSIVE)
			.displayItem(Material.DRAGON_HEAD)
			.descriptions(DepthsDethroner::getDescription)
			.singleCharm(false);

	private final double mBossDamage;
	private final double mEliteDamage;

	public DepthsDethroner(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mBossDamage = CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.DETHRONER_BOSS_DAMAGE_MULTIPLIER.mEffectName) + BOSS_DAMAGE[mRarity - 1];
		mEliteDamage = CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.DETHRONER_ELITE_DAMAGE_MULTIPLIER.mEffectName) + ELITE_DAMAGE[mRarity - 1];
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		DamageEvent.DamageType type = event.getType();
		if (type == DamageEvent.DamageType.TRUE || type == DamageEvent.DamageType.OTHER) {
			return false;
		}

		if (EntityUtils.isBoss(enemy)) {
			event.updateDamageWithMultiplier(1 + mBossDamage);
		} else if (EntityUtils.isElite(enemy)) {
			event.updateDamageWithMultiplier(1 + mEliteDamage);
		}
		return false; // only changes event damage
	}

	private static Description<DepthsDethroner> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.add("You deal ")
			.addPercent(a -> a.mEliteDamage, ELITE_DAMAGE[rarity - 1], false, true)
			.add(" more damage to elites and ")
			.addPercent(a -> a.mBossDamage, BOSS_DAMAGE[rarity - 1], false, true)
			.add(" more damage to bosses.");
	}


}

