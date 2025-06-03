package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.effects.RetaliationEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.EntityUtils;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.jetbrains.annotations.Nullable;

public class Retaliation implements Enchantment {
	private static final String EFFECT_SOURCE = "RetaliationEffect";
	private static final int EFFECT_DURATION = 4 * 20;
	public static final double BASE_DAMAGE = 0.35;
	public static final double ELITE_DAMAGE = 0.50;
	public static final double BOSS_DAMAGE = 0.65;
	private static final List<String> DOT_DEBUFFS = List.of("fire", "wither", "poison");
	public static final String DOT_NAME = "☠";
	private static final List<String> WEAK_DEBUFFS = List.of("weakness", "CustomDamageDecrease", "hunger", "CustomVulnerability");
	public static final String WEAK_NAME = "\uD83D\uDDE1";
	private static final List<String> SLOW_DEBUFFS = List.of("slow", "CustomSlow", "silence", "CustomAntiHeal");
	public static final String SLOW_NAME = "⚓";

	private int mLastProcTick = 0;

	@Override
	public String getName() {
		return "Retaliation";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.OFFHAND);
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.RETALIATION;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (!event.isBlockedByShield()) {
			return;
		}

		startEffect(player, event.getEffects(), event.getDamager(), false);
	}

	public void startEffect(Player player, List<EffectsList.Effect> effectsList, @Nullable Entity damager, boolean isBossAbility) {
		if (Bukkit.getCurrentTick() - mLastProcTick < 10) {
			return;
		}
		mLastProcTick = Bukkit.getCurrentTick();

		// Clear active effect to apply new debuffs
		Plugin.getInstance().mEffectManager.clearEffects(player, EFFECT_SOURCE);

		// Bosstag effects
		Set<String> debuffTypes = new ObjectArraySet<>();
		for (EffectsList.Effect effect : effectsList) {
			if (DOT_DEBUFFS.contains(effect.mName)) {
				debuffTypes.add(DOT_NAME);
			}
			if (WEAK_DEBUFFS.contains(effect.mName)) {
				debuffTypes.add(WEAK_NAME);
			}
			if (SLOW_DEBUFFS.contains(effect.mName)) {
				debuffTypes.add(SLOW_NAME);
			}
		}

		// Mob type effects, don't apply if this was triggered by an ability
		if (damager != null && !isBossAbility) {
			switch (damager.getType()) {
				case WITHER_SKELETON, WITHER_SKULL, BEE, CAVE_SPIDER, PUFFERFISH -> debuffTypes.add(DOT_NAME);
				case HUSK -> debuffTypes.add(WEAK_NAME);
				default -> {
				}
			}
		}

		if (damager instanceof Projectile projectile) {
			damager = (Entity) projectile.getShooter();
			// Blazes and flame arrows
			if (projectile.getFireTicks() > 0) {
				debuffTypes.add(DOT_NAME);
			}
			// Stray arrows
			if (damager != null && damager.getType() == EntityType.STRAY) {
				debuffTypes.add(SLOW_NAME);
			}
		}

		double damage = damager != null && EntityUtils.isElite(damager) ? ELITE_DAMAGE : (damager != null && EntityUtils.isBoss(damager) ? BOSS_DAMAGE : BASE_DAMAGE);

		if (damage == BOSS_DAMAGE) {
			player.playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1f, 1.4f);
			player.playSound(player, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 0.65f, 1.2f);
		} else if (damage == ELITE_DAMAGE) {
			player.playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1f, 1.6f);
			player.playSound(player, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 0.5f, 1.4f);
		} else {
			player.playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1f, 1.6f);
			player.playSound(player, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 0.8f, 1.0f);
		}

		StringBuilder debuffTypesString = new StringBuilder();
		debuffTypes.forEach(debuffTypesString::append);
		Plugin.getInstance().mEffectManager.addEffect(player, EFFECT_SOURCE, new RetaliationEffect(EFFECT_DURATION, damage, debuffTypesString.toString()));
	}
}
