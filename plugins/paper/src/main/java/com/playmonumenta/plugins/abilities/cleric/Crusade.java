package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
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
	public static final int ENHANCEMENT_MAX_MOBS = 6;
	public static final double ENHANCEMENT_BONUS_DAMAGE = 0.05;
	public static final String CHARM_DAMAGE = "Crusade Enhancement Damage Amplifier";

	public static final AbilityInfo<Crusade> INFO =
		new AbilityInfo<>(Crusade.class, NAME, Crusade::new)
			.linkedSpell(ABILITY)
			.scoreboardId(NAME)
			.shorthandName("Crs")
			.descriptions(
				"Your abilities now treat \"human-like\" enemies, such as illagers and witches, as Undead.",
				"After being damaged or debuffed by cleric abilities, any mob will count as undead for the next 10s.",
				String.format("Gain %s%% ability damage for every undead mob within %s blocks, including those marked by Crusade, capping at %s mobs.",
					(int) (ENHANCEMENT_BONUS_DAMAGE * 100), ENHANCEMENT_RADIUS, ENHANCEMENT_MAX_MOBS)
			)
			.simpleDescription("Passively count Humanoid mobs as Undead. Temporarily mark Monstrous mobs as Undead with abilities.")
			.displayItem(Material.ZOMBIE_HEAD);

	private final CrusadeCS mCosmetic;

	public Crusade(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CrusadeCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getAbility() == null || event.getAbility().isFake()) {
			return false;
		}

		if (isEnhanced()) {
			long numMobs = new Hitbox.SphereHitbox(mPlayer.getLocation(), ENHANCEMENT_RADIUS)
				.getHitMobs().stream().filter(e -> enemyTriggersAbilities(e, this)).limit(ENHANCEMENT_MAX_MOBS).count();
			double damagePerMob = ENHANCEMENT_BONUS_DAMAGE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
			event.setDamage(event.getDamage() * (1 + damagePerMob * numMobs));
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
			mPlugin.mEffectManager.addEffect(enemy, "CrusadeTag", new CrusadeTag(TAG_DURATION, mCosmetic));
		}
	}

	public static void addCrusadeTag(LivingEntity enemy, @Nullable Crusade crusade) {
		if (crusade == null) {
			return;
		}

		crusade.addCrusadeTag(enemy);
	}
}
