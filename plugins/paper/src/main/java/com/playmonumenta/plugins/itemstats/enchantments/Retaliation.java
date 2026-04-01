package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.OnHitBoss;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.effects.RetaliationEffect;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageShieldedEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class Retaliation implements Enchantment {
	private static final String EFFECT_SOURCE = "RetaliationEffect";
	private static final int EFFECT_DURATION = 4 * 20;
	public static final double BASE_DAMAGE = 0.35;
	public static final double ELITE_DAMAGE = 0.50;
	public static final double BOSS_DAMAGE = 0.65;
	private static final List<String> DOT_DEBUFFS = List.of("fire", "wither", "poison");
	public static final String DOT_NAME = "☠";
	private static final List<String> WEAK_DEBUFFS = List.of("PercentAttackSpeed", "PercentDamageDealt", "PercentDamageReceived", "weakness", "CustomDamageDecrease", "hunger", "CustomVulnerability");
	public static final String WEAK_NAME = "\uD83D\uDDE1";
	private static final List<String> SLOW_DEBUFFS = List.of("PercentSpeed", "AbilitySilence", "slow", "CustomSlow", "silence", "CustomAntiHeal");
	public static final String SLOW_NAME = "⚓";

	private final HashMap<Player, RetaliationDebuff> mEffects = new HashMap<>();

	private int mLastProcTick = 0;
	private int mLastFireTick = 0;

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
	public void onDamageShielded(Plugin plugin, Player player, double value, DamageShieldedEvent event) {
		//isBossAbility always being determined based on DamageCause.CUSTOM is not flawless, but it is as close as it gets.
		//Boss_onHit will be counted as a non-ability but this is irrelevant
		boolean isAbility = event.getCause().equals(EntityDamageEvent.DamageCause.CUSTOM);
		primeRetaliation(player, event.getSource(), isAbility);
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		vanillaFireCheck(player, source);
	}

	//Compares fire ticks for a decent but imperfect estimation of  whether the player got hit with a vanilla fire afflicting attack
	private void vanillaFireCheck(Player player, @Nullable LivingEntity source) {
		if (player.getFireTicks() >= mLastFireTick && player.getFireTicks() > 0) {
			int currentTick = Bukkit.getCurrentTick();
			mEffects.entrySet().removeIf(e -> e.getValue().mApplicationTick != currentTick);

			if (mEffects.containsKey(player)) {
				mEffects.get(player).mDebuffTypes.add(DOT_NAME);
			} else {
				mEffects.put(player, new RetaliationDebuff(new ObjectArraySet<>(Collections.singleton(DOT_NAME)), currentTick, source));
			}
		}
		mLastFireTick = player.getFireTicks();
	}

	@Override
	public void onCustomEffectApply(Plugin plugin, Player player, double value, CustomEffectApplyEvent event) {
		if (!event.getEffect().isDebuff()) {
			return;
		}

		Set<String> debuffTypes = new ObjectArraySet<>();
		String effectID = event.getEffect().getEffectID();

		if (DOT_DEBUFFS.contains(effectID)) {
			debuffTypes.add(DOT_NAME);
		}
		if (WEAK_DEBUFFS.contains(effectID)) {
			debuffTypes.add(WEAK_NAME);
		}
		if (SLOW_DEBUFFS.contains(effectID)) {
			debuffTypes.add(SLOW_NAME);
		}

		if (debuffTypes.isEmpty()) {
			return;
		}

		int currentTick = Bukkit.getCurrentTick();
		mEffects.entrySet().removeIf(e -> e.getValue().mApplicationTick != currentTick);

		if (mEffects.containsKey(player)) {
			mEffects.get(player).mDebuffTypes.addAll(debuffTypes);
		} else {
			mEffects.put(player, new RetaliationDebuff(debuffTypes, currentTick, null));
		}
	}

	private void primeRetaliation(Player player, @Nullable LivingEntity source, boolean isBossAbility) {
		final int tick = Bukkit.getCurrentTick();
		if (tick - mLastProcTick < 10) {
			return;
		}
		mLastProcTick = tick;

		new BukkitRunnable() {
			@Override
			public void run() {
				RetaliationDebuff debuff = mEffects.get(player);
				Set<String> debuffTypes = new ObjectArraySet<>();

				if (debuff != null && debuff.mApplicationTick == tick && (debuff.mSourceEntity == null || debuff.mSourceEntity.equals(source))) {
					debuffTypes.addAll(debuff.mDebuffTypes);
				} else if (source != null && !isBossAbility) {
					debuffTypes.addAll(vanillaMobEffects(source));
				}
				if (source != null) {
					debuffTypes.addAll(onHitBossEffects(source));
				}
				retaliate(player, debuffTypes, source);

			}

		}.runTask(Plugin.getInstance());
	}

	//Gets the effects a boss_onHit mob would have applied if not blocked
	private Set<String> onHitBossEffects(LivingEntity source) {
		OnHitBoss onHitBoss = BossUtils.getBossOfClass(source, OnHitBoss.class);
		Set<String> debuffTypes = new ObjectArraySet<>();
		if (onHitBoss != null) {
			EffectsList effectsList = onHitBoss.mParams.EFFECTS;
			for (EffectsList.Effect effect : effectsList.mEffectList()) {
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
		}
		return debuffTypes;
	}

	//Gets the default vanilla effects that would be applied by a mob if the hit wasn't blocked
	private Set<String> vanillaMobEffects(LivingEntity source) {
		Set<String> debuffTypes = new ObjectArraySet<>();
		switch (source.getType()) {
			case WITHER_SKELETON, WITHER_SKULL, BEE, CAVE_SPIDER, PUFFERFISH ->
				debuffTypes.add(DOT_NAME);
			case HUSK -> debuffTypes.add(WEAK_NAME);
			default -> {
			}
		}
		return debuffTypes;
	}

	private void retaliate(Player player, Set<String> debuffs, @Nullable Entity damager) {
		// Clear active effect to apply new debuffs
		Plugin.getInstance().mEffectManager.clearEffects(player, EFFECT_SOURCE);

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
		debuffs.forEach(debuffTypesString::append);
		Plugin.getInstance().mEffectManager.addEffect(player, EFFECT_SOURCE, new RetaliationEffect(EFFECT_DURATION, damage, debuffTypesString.toString()));
	}

	private static class RetaliationDebuff {

		public final Set<String> mDebuffTypes;
		public final int mApplicationTick;
		public final @Nullable LivingEntity mSourceEntity;

		public RetaliationDebuff(Set<String> debuffTypes, int applicationTick, @Nullable LivingEntity sourceEntity) {
			mDebuffTypes = debuffTypes;
			mApplicationTick = applicationTick;
			mSourceEntity = sourceEntity;
		}

	}

}
