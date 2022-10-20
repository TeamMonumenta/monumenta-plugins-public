package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.scout.hunter.PredatorStrike;
import com.playmonumenta.plugins.abilities.scout.ranger.Quickdraw;
import com.playmonumenta.plugins.abilities.scout.ranger.TacticalManeuver;
import com.playmonumenta.plugins.abilities.scout.ranger.WhirlingBlade;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Agility extends Ability {

	private static final int AGILITY_1_EFFECT_LVL = 0;
	private static final int AGILITY_2_EFFECT_LVL = 1;
	private static final int AGILITY_BONUS_DAMAGE = 1;
	private static final double SCALING_DAMAGE = 0.1;
	private static final double ENHANCEMENT_COOLDOWN_REFRESH = 0.05;

	private Ability[] mScoutAbilities = {};

	public Agility(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Agility");
		mInfo.mScoreboardId = "Agility";
		mInfo.mShorthandName = "Agl";
		mInfo.mDescriptions.add(String.format("You gain permanent Haste %s. Your melee attacks deal +%d extra damage.", StringUtils.toRoman(AGILITY_1_EFFECT_LVL + 1), AGILITY_BONUS_DAMAGE));
		mInfo.mDescriptions.add(String.format("You gain permanent Haste %s. Increase melee damage by +%d plus %d%% of final damage done.", StringUtils.toRoman(AGILITY_2_EFFECT_LVL + 1), AGILITY_BONUS_DAMAGE, (int)(SCALING_DAMAGE * 100)));
		mInfo.mDescriptions.add(
			String.format("Breaking a spawner refreshes the cooldown of all your skills by %s%%.",
				StringUtils.multiplierToPercentage(ENHANCEMENT_COOLDOWN_REFRESH)));
		mDisplayItem = new ItemStack(Material.GOLDEN_PICKAXE, 1);

		Bukkit.getScheduler().runTask(plugin, () -> {
			AbilityManager abilityManager = mPlugin.mAbilityManager;
			mScoutAbilities = Stream.of(WindBomb.class, Volley.class, HuntingCompanion.class, EagleEye.class,
					WhirlingBlade.class, TacticalManeuver.class, Quickdraw.class, PredatorStrike.class)
				.map(c -> abilityManager.getPlayerAbilityIgnoringSilence(player, c))
				.filter(Objects::nonNull)
				.toArray(Ability[]::new);
		});
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_ENCH || event.getType() == DamageType.MELEE_SKILL) {
			if (isLevelTwo()) {
				event.setDamage((event.getDamage() + AGILITY_BONUS_DAMAGE) * (1 + SCALING_DAMAGE));
			} else {
				event.setDamage(event.getDamage() + AGILITY_BONUS_DAMAGE);
			}
		}
		return false; // only changes event damage
	}

	@Override
	public boolean blockBreakEvent(BlockBreakEvent event) {
		if (mPlayer != null && isEnhanced() && event.getBlock().getType() == Material.SPAWNER) {
			UUID uuid = mPlayer.getUniqueId();
			for (Ability ability : mScoutAbilities) {
				if (mPlugin.mTimers.isAbilityOnCooldown(uuid, ability.mInfo.mLinkedSpell)) {
					int cooldownRefresh = (int) (ability.getModifiedCooldown() * ENHANCEMENT_COOLDOWN_REFRESH);
					mPlugin.mTimers.updateCooldown(mPlayer, ability.mInfo.mLinkedSpell, cooldownRefresh);
				}
			}
		}
		return true;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond) {
			int effectLevel = isLevelOne() ? AGILITY_1_EFFECT_LVL : AGILITY_2_EFFECT_LVL;
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
				new PotionEffect(PotionEffectType.FAST_DIGGING, 21, effectLevel, true, false));
		}
	}
}
