package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.CrusadeCS;
import com.playmonumenta.plugins.effects.CrusadeTag;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;


public class Crusade extends Ability {
	public static final String NAME = "Crusade";
	public static final ClassAbility ABILITY = ClassAbility.CRUSADE;

	public static final int TAG_DURATION = 10 * 20;
	public static final double ENHANCEMENT_RADIUS = 8;
	public static final int ENHANCEMENT_MAX_MOBS = 5;
	public static final double ENHANCEMENT_BONUS_DAMAGE = 0.08;

	public static final String CHARM_DURATION = "Crusade Duration";
	public static final String CHARM_RADIUS = "Crusade Enhancement Radius";
	public static final String CHARM_DAMAGE = "Crusade Enhancement Damage Amplifier";
	public static final String CHARM_MAX_MOBS = "Crusade Enhancement Max Mobs";

	public static final AbilityInfo<Crusade> INFO =
		new AbilityInfo<>(Crusade.class, NAME, Crusade::new)
			.linkedSpell(ABILITY)
			.scoreboardId(NAME)
			.shorthandName("Crs")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Passively count Humanoid mobs as Undead. Temporarily mark Monstrous mobs as Undead with abilities.")
			.displayItem(Material.ZOMBIE_HEAD);

	private final int mDuration;
	private final double mRadius;
	private final double mDamagePerMob;
	private final int mMaxMobs;
	private final CrusadeCS mCosmetic;

	public Crusade(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, TAG_DURATION);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, ENHANCEMENT_RADIUS);
		mDamagePerMob = ENHANCEMENT_BONUS_DAMAGE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mMaxMobs = ENHANCEMENT_MAX_MOBS + (int) CharmManager.getLevel(mPlayer, CHARM_MAX_MOBS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CrusadeCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getAbility() == null || event.getAbility().isFake()) {
			return false;
		}

		if (isEnhanced()) {
			long numMobs = new Hitbox.SphereHitbox(mPlayer.getLocation(), mRadius)
				.getHitMobs().stream().filter(e -> enemyTriggersAbilities(e, this)).limit(mMaxMobs).count();
			event.updateDamageWithMultiplier(1 + mDamagePerMob * numMobs);
			mCosmetic.crusadeEnhancement(mPlayer, numMobs);
		}

		addCrusadeTag(enemy);

		return false; // only increases event damage
	}

	public static boolean enemyTriggersAbilities(LivingEntity enemy, @Nullable Crusade crusade) {
		return EntityUtils.isUndead(enemy) || (crusade != null && EntityUtils.isHumanlike(enemy)) || Plugin.getInstance().mEffectManager.hasEffect(enemy, CrusadeTag.class);
	}

	private void addCrusadeTag(LivingEntity enemy) {
		if (isLevelTwo() && !EntityUtils.isUndead(enemy) && !EntityUtils.isHumanlike(enemy)) {
			mPlugin.mEffectManager.addEffect(enemy, "CrusadeTag", new CrusadeTag(mDuration, mCosmetic));
		}
	}

	public static void addCrusadeTag(LivingEntity enemy, @Nullable Crusade crusade) {
		if (crusade == null) {
			return;
		}

		crusade.addCrusadeTag(enemy);
	}

	private static Description<Crusade> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Your abilities now treat \"human-like\" enemies, such as illagers and witches, as Undead.");
	}

	private static Description<Crusade> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("After being damaged or debuffed by Clark abilities, any mob will count as undead for the next ")
			.addDuration(a -> a.mDuration, TAG_DURATION)
			.add(" seconds.");
	}

	private static Description<Crusade> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Gain ")
			.addPercent(a -> a.mDamagePerMob, ENHANCEMENT_BONUS_DAMAGE)
			.add(" ability damage for every undead mob within ")
			.add(a -> a.mRadius, ENHANCEMENT_RADIUS)
			.add(" blocks, including those marked by Crusade, capping at ")
			.add(a -> a.mMaxMobs, ENHANCEMENT_MAX_MOBS)
			.add(" mobs.");
	}
}
