package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.WormBoss;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.ZeroArgumentEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class Cloaked implements Enchantment {
	public static final int MOB_CAP = 2;
	public static final int MOB_CAP_HALF_EFFECT = 3;
	public static final int RADIUS = 5;
	public static final int CLOAKED_DURATION = 6 * 20;
	private static final String CLOAKED_EFFECT_NAME = "CloakedEffect";

	@Override
	public String getName() {
		return "Cloaked";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CLOAKED;
	}

	@Override
	public void onKill(Plugin plugin, Player player, double value, EntityDeathEvent event, LivingEntity enemy) {
		if (EntityUtils.isBoss(enemy) || EntityUtils.isElite(enemy)) {
			plugin.mEffectManager.addEffect(player, CLOAKED_EFFECT_NAME, new ZeroArgumentEffect(CLOAKED_DURATION, CLOAKED_EFFECT_NAME) {
				@Override
				public String toString() {
					return String.format("%s duration:%d", CLOAKED_EFFECT_NAME, getDuration());
				}
			});
		}
	}

	public static double applyCloaked(DamageEvent event, Plugin plugin, Player player) {
		Effect cloakedEffect = plugin.mEffectManager.getActiveEffect(player, CLOAKED_EFFECT_NAME);
		if (cloakedEffect != null) {
			return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.CLOAKED);
		}

		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(player.getLocation(), RADIUS);
		mobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
		mobs.removeIf(mob -> mob.getScoreboardTags().contains(WormBoss.IGNORE_WORM_TAG));
		int mobCount = mobs.size();
		if (mobCount <= MOB_CAP) {
			return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.CLOAKED);
		} else if (mobCount <= MOB_CAP_HALF_EFFECT) {
			return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.CLOAKED) * 0.5;
		} else {
			return 0;
		}
	}
}
