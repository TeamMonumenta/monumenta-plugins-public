package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.AbilityCooldownRechargeRate;
import com.playmonumenta.plugins.effects.AbilitySilence;
import com.playmonumenta.plugins.effects.ArrowSaving;
import com.playmonumenta.plugins.effects.BonusSoulThreads;
import com.playmonumenta.plugins.effects.DurabilitySaving;
import com.playmonumenta.plugins.effects.PercentAttackSpeed;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentHealthBoost;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.effects.PercentPotionRecharge;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class IJustMadeSomeBS extends Ability {
	private static final int COOLDOWN = 60 * 20;
	private static final int DURATION = 10 * 20;
	private static final List<String> EFFECTS = List.of(AbilityCooldownRechargeRate.effectID, AbilitySilence.effectID, ArrowSaving.effectID, BonusSoulThreads.effectID, DurabilitySaving.effectID, PercentHealthBoost.effectID, "Absorption", PercentAttackSpeed.effectID, PercentDamageDealt.effectID, PercentDamageReceived.effectID, PercentPotionRecharge.effectID, PercentSpeed.effectID, PercentKnockbackResist.effectID);
	private static final List<PotionEffectType> POTION_EFFECTS = List.of(PotionEffectType.BLINDNESS, PotionEffectType.CONDUIT_POWER, PotionEffectType.FAST_DIGGING, PotionEffectType.HUNGER, PotionEffectType.INVISIBILITY, PotionEffectType.JUMP, PotionEffectType.NIGHT_VISION, PotionEffectType.WATER_BREATHING, PotionEffectType.POISON);

	public static final AbilityInfo<IJustMadeSomeBS> INFO =
		new AbilityInfo<>(IJustMadeSomeBS.class, "I just made some BULLLLLLSHITTTTT!!!!!!", IJustMadeSomeBS::new)
			.linkedSpell(ClassAbility.I_JUST_MADE_SOME_BS)
			.scoreboardId("IJustMadeSomeBS")
			.shorthandName("IJMSBS")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Never cook or brew again.")
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", IJustMadeSomeBS::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true).onGround(false),
				PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(Material.SUSPICIOUS_STEW);

	private @Nullable AlchemistPotions mAlchemistPotions;

	public IJustMadeSomeBS(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		Bukkit.getScheduler().runTask(plugin, () ->
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class)
		);
	}

	public boolean cast() {
		if (mAlchemistPotions == null || isOnCooldown()) {
			return false;
		}
		if (mAlchemistPotions.decrementCharges(3)) {
			Location loc = mPlayer.getLocation();
			World world = mPlayer.getWorld();
			world.playSound(loc, Sound.ENTITY_PLAYER_BURP, 1f, 1f);
			world.playSound(loc, Sound.ENTITY_GENERIC_EAT, 1f, 1f);
			putOnCooldown();
			List<String> effects = new ArrayList<>(EFFECTS);
			List<PotionEffectType> potionEffects = new ArrayList<>(POTION_EFFECTS);
			for (int i = 0; i < 10; i++) {
				int last = effects.size() + potionEffects.size() - 1;
				int select = FastUtils.randomIntInRange(0, last);
				if (select <= effects.size() - 1) {
					double potency = FastUtils.randomDoubleInRange(-0.5, 0.5);
					switch (effects.get(select)) {
						case AbilityCooldownRechargeRate.effectID -> mPlugin.mEffectManager.addEffect(mPlayer, "IJMSBSCooldownRechargeRate", new AbilityCooldownRechargeRate(DURATION, potency));
						case AbilitySilence.effectID -> mPlugin.mEffectManager.addEffect(mPlayer, "IJMSBSSilence", new AbilitySilence(DURATION));
						case ArrowSaving.effectID -> mPlugin.mEffectManager.addEffect(mPlayer, "IJMSBSArrowSaving", new ArrowSaving(DURATION, potency));
						case BonusSoulThreads.effectID -> mPlugin.mEffectManager.addEffect(mPlayer, "IJMSBSSoulThreadBonus", new BonusSoulThreads(DURATION, potency));
						case DurabilitySaving.effectID -> mPlugin.mEffectManager.addEffect(mPlayer, "IJMSBSDurabilitySaving", new DurabilitySaving(DURATION, potency));
						case PercentHealthBoost.effectID -> mPlugin.mEffectManager.addEffect(mPlayer, "IJMSBSPercentHealthBoost", new PercentHealthBoost(DURATION, potency, "IJMSBSFlatHealthBoost"));
						case "Absorption" -> AbsorptionUtils.addAbsorption(mPlayer, potency * EntityUtils.getMaxHealth(mPlayer), potency * EntityUtils.getMaxHealth(mPlayer), DURATION);
						case PercentAttackSpeed.effectID -> mPlugin.mEffectManager.addEffect(mPlayer, "IJMSBSAttackSpeed", new PercentAttackSpeed(DURATION, potency, "IJMSBSAttackSpeed"));
						case PercentDamageDealt.effectID -> mPlugin.mEffectManager.addEffect(mPlayer, "IJMSBSDamageDealt", new PercentDamageDealt(DURATION, potency));
						case PercentDamageReceived.effectID -> mPlugin.mEffectManager.addEffect(mPlayer, "IJMSBSDamageReceived", new PercentDamageReceived(DURATION, potency));
						case PercentPotionRecharge.effectID -> {
							if (mAlchemistPotions != null) {
							mPlugin.mEffectManager.addEffect(mPlayer, "IJMSBSPotionRecharge", new PercentPotionRecharge(DURATION, potency, "IJMSBSPotionRecharge", mAlchemistPotions));
							}
						}
						case PercentSpeed.effectID -> mPlugin.mEffectManager.addEffect(mPlayer, "IJMSBSSpeed", new PercentSpeed(DURATION, potency, "IJMSBSSpeed"));
						case PercentKnockbackResist.effectID -> mPlugin.mEffectManager.addEffect(mPlayer, "IJMSBSKnockbackResistance", new PercentKnockbackResist(DURATION, potency, "IJMSBSKnockbackResistance"));
						default -> { }
					}
					effects.remove(select);
				} else {
					int potency = FastUtils.randomIntInRange(0, 4); // 1 to 5;
					PotionEffect potion = potionEffects.get(select - effects.size()).createEffect(DURATION, potency);
					PotionUtils.applyPotion(mPlugin, mPlayer, potion);
					potionEffects.remove(select - effects.size());
				}
			}
			return true;
		}
		return false;
	}


	private static Description<IJustMadeSomeBS> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addDashedLine();
	}

	private static Description<IJustMadeSomeBS> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addDashedLine();
	}

	private static Description<IJustMadeSomeBS> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addTrigger()
			.addDashedLine()
			.addLine("Pour 3 Alchemist Potions into a pot and cook some")
			.addLine("horrible stinky food. Just absolutely vile. The")
			.addLine("concoction grants you 25 random potion effects")
			.addLine("with a random potency from 100% to 500%. Do not")
			.addLine("cook again.")
			.addDashedLine();
	}
}
