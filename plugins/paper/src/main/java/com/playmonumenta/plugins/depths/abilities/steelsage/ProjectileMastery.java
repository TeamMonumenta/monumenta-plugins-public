package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ProjectileMastery extends DepthsAbility {

	public static final String ABILITY_NAME = "Projectile Mastery";
	public static final double[] SPELL_MOD = {0.08, 0.10, 0.12, 0.14, 0.16, 0.20};

	public static final DepthsAbilityInfo<ProjectileMastery> INFO =
		new DepthsAbilityInfo<>(ProjectileMastery.class, ABILITY_NAME, ProjectileMastery::new, DepthsTree.STEELSAGE, DepthsTrigger.PASSIVE)
			.displayItem(Material.BOW)
			.descriptions(ProjectileMastery::getDescription)
			.singleCharm(false);

	private final double mDamage;

	public ProjectileMastery(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = SPELL_MOD[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.PROJECTILE_MASTERY_DAMAGE_MULTIPLIER.mEffectName);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE || event.getType() == DamageType.PROJECTILE_SKILL) {
			event.setDamage(event.getDamage() * (1 + mDamage));
		}
		return false; // only changes event damage
	}

	private static Description<ProjectileMastery> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<ProjectileMastery>(color)
			.add("You deal ")
			.addPercent(a -> a.mDamage, SPELL_MOD[rarity - 1], false, true)
			.add(" more projectile damage.");
	}
}

