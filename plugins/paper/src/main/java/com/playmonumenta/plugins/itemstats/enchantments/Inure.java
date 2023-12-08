package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.OnHitTimerEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Inure implements Enchantment {

	private static final double ARMOR_BONUS_PER_LEVEL = 0.2;
	private static final int PAST_HIT_DURATION_TIME = 20 * 60;
	private static final String INURE_MELEE_NAME = "MeleeInureEffect";
	private static final String INURE_PROJ_NAME = "ProjectileInureEffect";
	private static final String INURE_MAGIC_NAME = "MagicInureEffect";
	private static final String INURE_BLAST_NAME = "BlastInureEffect";
	private static final String INURE_MEMORY_NAME = "MemoryInureEffect";
	private static final String INURE_FULLBONUS_NAME = "FullInureEffect";

	@Override
	public String getName() {
		return "Inure";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.INURE;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		DamageType type = event.getType();
		// ignore environmental/unreduce-able damage
		if (event.isBlocked() || checkEnvironmental(type)) {
			return;
		}
		// clear inure add effect type to clear, limits effect size to 1 per name
		// 1 = melee
		// 2 = proj
		// 3 = magic
		// 4 = blast
		clearInure(plugin, player, INURE_MEMORY_NAME);
		if (type == DamageType.MELEE) {
			clearInure(plugin, player, INURE_MELEE_NAME);
			plugin.mEffectManager.addEffect(player, INURE_MELEE_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME, 1));
			plugin.mEffectManager.addEffect(player, INURE_MEMORY_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME, 1));
		} else if (type == DamageType.PROJECTILE) {
			clearInure(plugin, player, INURE_PROJ_NAME);
			plugin.mEffectManager.addEffect(player, INURE_PROJ_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME, 1));
			plugin.mEffectManager.addEffect(player, INURE_MEMORY_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME, 2));
		} else if (type == DamageType.MAGIC) {
			clearInure(plugin, player, INURE_MAGIC_NAME);
			plugin.mEffectManager.addEffect(player, INURE_MAGIC_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME, 1));
			plugin.mEffectManager.addEffect(player, INURE_MEMORY_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME, 3));
		} else if (type == DamageType.BLAST) {
			clearInure(plugin, player, INURE_BLAST_NAME);
			plugin.mEffectManager.addEffect(player, INURE_BLAST_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME, 1));
			plugin.mEffectManager.addEffect(player, INURE_MEMORY_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME, 4));
		}
	}

	// runs first from damage calc before running onhurt
	public static double applyInure(DamageEvent event, Plugin plugin, Player player) {
		DamageType type = event.getType();
		// environmental/unreduce-able damage should be ignored
		if (checkEnvironmental(type)) {
			return 0;
			// 2 effects check
		}
		NavigableSet<Effect> melee = plugin.mEffectManager.getEffects(player, INURE_MELEE_NAME);
		NavigableSet<Effect> proj = plugin.mEffectManager.getEffects(player, INURE_PROJ_NAME);
		NavigableSet<Effect> magic = plugin.mEffectManager.getEffects(player, INURE_MAGIC_NAME);
		NavigableSet<Effect> blast = plugin.mEffectManager.getEffects(player, INURE_BLAST_NAME);
		NavigableSet<Effect> memory = plugin.mEffectManager.getEffects(player, INURE_MEMORY_NAME);
		NavigableSet<Effect> full = plugin.mEffectManager.getEffects(player, INURE_FULLBONUS_NAME);

		// if type = memory id, always full bonus
		// else if type count > 1, half bonus or reset
		// else if type count > 0 + triggered full bonus, half bonus
		// else no bonus (due to full bonus matches memory)

		// memory check
		if (memory != null) {
			Effect mem = memory.last();
			if ((type == DamageType.MELEE && mem.getMagnitude() == 1) ||
				(type == DamageType.PROJECTILE && mem.getMagnitude() == 2) ||
				(type == DamageType.MAGIC && mem.getMagnitude() == 3) ||
				(type == DamageType.BLAST && mem.getMagnitude() == 4)) {
				plugin.mEffectManager.addEffect(player, INURE_FULLBONUS_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME, 1));
				resetDuration(melee, proj, magic, blast);
				return fullBonus(plugin, player);
			}
		}
		int meleeCount = (melee == null) ? 0 : melee.size();
		int projCount = (proj == null) ? 0 : proj.size();
		int magicCount = (magic == null) ? 0 : magic.size();
		int blastCount = (blast == null) ? 0 : blast.size();
		int effectCount = meleeCount + projCount + blastCount + magicCount;
		if (effectCount > 1) {
			// check if damage type is in effect list
			if ((type == DamageType.MELEE && melee != null) ||
				(type == DamageType.PROJECTILE && proj != null) ||
				(type == DamageType.MAGIC && magic != null) ||
				(type == DamageType.BLAST && blast != null)) {
				// half bonus
				resetDuration(melee, proj, magic, blast);
				return halfBonus(plugin, player);
			} else {
				// wrong damage type, reset + no bonus -> on hurt add effect
				resetInure(plugin, player);
				return 0;
			}
		// 1 effect but triggered full bonus, damage type switch half bonus
		} else if (full != null) {
			clearInure(plugin, player, INURE_FULLBONUS_NAME);
			resetDuration(melee, proj, magic, blast);
			return halfBonus(plugin, player);
		// 0 effect or 1 effect without triggering full effect, reset + no bonus
		} else {
			resetInure(plugin, player);
			return 0;
		}
	}

	private static boolean checkEnvironmental(DamageType type) {
		return type != DamageType.MELEE && type != DamageType.PROJECTILE &&
			       type != DamageType.MAGIC && type != DamageType.BLAST;
	}

	private static void clearInure(Plugin plugin, Player player, String name) {
		plugin.mEffectManager.clearEffects(player, name);
	}

	private static void resetInure(Plugin plugin, Player player) {
		clearInure(plugin, player, INURE_MELEE_NAME);
		clearInure(plugin, player, INURE_PROJ_NAME);
		clearInure(plugin, player, INURE_MAGIC_NAME);
		clearInure(plugin, player, INURE_BLAST_NAME);
		clearInure(plugin, player, INURE_MEMORY_NAME);
		clearInure(plugin, player, INURE_FULLBONUS_NAME);
	}

	private static void resetDuration(@Nullable NavigableSet<Effect> melee, @Nullable NavigableSet<Effect> proj,
	                                  @Nullable NavigableSet<Effect> magic, @Nullable NavigableSet<Effect> blast) {
		List<Effect> effects = new ArrayList<>();
		if (melee != null) {
			effects.addAll(melee);
		}
		if (proj != null) {
			effects.addAll(proj);
		}
		if (magic != null) {
			effects.addAll(magic);
		}
		if (blast != null) {
			effects.addAll(blast);
		}
		for (Effect e : effects) {
			e.setDuration(PAST_HIT_DURATION_TIME);
		}
	}

	private static double halfBonus(Plugin plugin, Player player) {
		player.playSound(player.getLocation(), Sound.BLOCK_SOUL_SOIL_BREAK, SoundCategory.HOSTILE, 0.7f, 1.0f);
		return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.INURE) * ARMOR_BONUS_PER_LEVEL / 2;
	}

	private static double fullBonus(Plugin plugin, Player player) {
		player.playSound(player.getLocation(), Sound.BLOCK_SOUL_SOIL_HIT, SoundCategory.HOSTILE, 0.7f, 1.0f);
		return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.INURE) * ARMOR_BONUS_PER_LEVEL;
	}
}
