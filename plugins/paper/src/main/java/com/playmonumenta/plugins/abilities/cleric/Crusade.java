package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.effects.CrusadeTag;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;


public class Crusade extends Ability {
	public static final String NAME = "Crusade";

	public static final int TAG_DURATION = 10 * 20;
	public static final double ENHANCEMENT_RADIUS = 8;
	public static final int ENHANCEMENT_MAX_MOBS = 6;
	public static final double ENHANCEMENT_BONUS_DAMAGE = 0.05;

	public static final AbilityInfo<Crusade> INFO =
		new AbilityInfo<>(Crusade.class, NAME, Crusade::new)
			.scoreboardId(NAME)
			.shorthandName("Crs")
			.descriptions(
				"Your abilities now treat \"human-like\" enemies, such as illagers and witches, as Undead.",
				"After being damaged or debuffed by cleric abilities, any mob will count as undead for the next 10s.",
				String.format("Gain %s%% ability damage for every mob affected by this ability within %s blocks, capping at %s mobs.",
					(int) (ENHANCEMENT_BONUS_DAMAGE * 100), ENHANCEMENT_RADIUS, ENHANCEMENT_MAX_MOBS)
			)
			.displayItem(new ItemStack(Material.ZOMBIE_HEAD, 1));

	public Crusade(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getAbility() == null || event.getAbility().isFake()) {
			return false;
		}

		if (isEnhanced()) {
			long numMobs = new Hitbox.SphereHitbox(mPlayer.getLocation(), ENHANCEMENT_RADIUS)
				.getHitMobs().stream().filter(e -> enemyTriggersAbilities(e, this)).limit(ENHANCEMENT_MAX_MOBS).count();
			event.setDamage(event.getDamage() * (1 + ENHANCEMENT_BONUS_DAMAGE * numMobs));
		}

		addCrusadeTag(enemy);

		return false; // only increases event damage
	}

	public static boolean enemyTriggersAbilities(LivingEntity enemy, @Nullable Crusade crusade) {
		return EntityUtils.isUndead(enemy) || (crusade != null && EntityUtils.isHumanlike(enemy)) || Plugin.getInstance().mEffectManager.hasEffect(enemy, CrusadeTag.class);
	}

	private void addCrusadeTag(LivingEntity enemy) {
		if (isLevelTwo() && !EntityUtils.isUndead(enemy) && !EntityUtils.isHumanlike(enemy)) {
			mPlugin.mEffectManager.addEffect(enemy, "CrusadeTag", new CrusadeTag(TAG_DURATION));
		}
	}

	public static void addCrusadeTag(LivingEntity enemy, @Nullable Crusade crusade) {
		if (crusade == null) {
			return;
		}

		crusade.addCrusadeTag(enemy);
	}
}
