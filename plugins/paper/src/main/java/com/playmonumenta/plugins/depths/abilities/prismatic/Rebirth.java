package com.playmonumenta.plugins.depths.abilities.prismatic;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPFlower;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class Rebirth extends DepthsAbility {

	public static final String ABILITY_NAME = "Rebirth";
	public static final String RESISTANCE_EFFECT_NAME = "RebirthResistance";
	public static final int UPGRADE_LEVELS = 2;
	public static final int[] RESISTANCE_DURATION = {3 * 20, 4 * 20, 5 * 20, 6 * 20, 7 * 20, 10 * 20};
	public static final int[] EXTRA_ABILITIES = {0, 1, 2, 3, 4, 6};

	// Give it a dummy cooldown so that its icon appears for Monumenta Mod users.
	// The cooldown doesn't matter, since the ability is removed immediately upon use.
	public static final DepthsAbilityInfo<Rebirth> INFO =
		new DepthsAbilityInfo<>(Rebirth.class, ABILITY_NAME, Rebirth::new, DepthsTree.PRISMATIC, DepthsTrigger.PASSIVE)
			.linkedSpell(ClassAbility.REBIRTH)
			.displayItem(Material.CRIMSON_HYPHAE)
			.descriptions(Rebirth::getDescription)
			.priorityAmount(10000);

	public Rebirth(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void onHurtFatal(DamageEvent event) {
		onHurt(event, null, null);
	}

	public void activationEffects() {
		new PPFlower(Particle.REDSTONE, mPlayer.getLocation(), 7).petals(11).angleStep(0.065)
			.transitionColors(Color.fromRGB(255, 255, 0), Color.fromRGB(255, 0, 0), 1.5f)
			.spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.END_ROD, mPlayer.getLocation(), 2).countPerMeter(2.5).directionalMode(true)
			.delta(0, 1, 0).extra(1).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.END_ROD, mPlayer.getLocation(), 2).countPerMeter(2.5).directionalMode(true)
			.delta(0, 1, 0).extra(0.85).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.END_ROD, mPlayer.getLocation(), 2).countPerMeter(2.5).directionalMode(true)
			.delta(0, 1, 0).extra(0.7).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.END_ROD, mPlayer.getLocation(), 2).countPerMeter(2.5).directionalMode(true)
			.delta(0, 1, 0).extra(0.5).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.END_ROD, mPlayer.getLocation(), 2).countPerMeter(2.5).directionalMode(true)
			.delta(0, 1, 0).extra(0.3).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.END_ROD, mPlayer.getLocation(), 2).countPerMeter(2.5).directionalMode(true)
			.delta(0, 1, 0).extra(0.1).spawnAsPlayerActive(mPlayer);
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 2, 0.85f);
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 2, 1);
	}

	public void rerollAbilities(DepthsPlayer dp) {
		List<DepthsAbilityInfo<?>> playerAbilities = DepthsManager.getInstance().getPlayerAbilities(mPlayer);

		ArrayList<DepthsAbilityInfo<?>> eligibleAbilities = new ArrayList<>(
			DepthsManager.getAbilities().stream().filter(info ->
				info.getDisplayName() != null &&
				!info.getDisplayName().equals(ABILITY_NAME) &&
				!info.getDepthsTrigger().equals(DepthsTrigger.WEAPON_ASPECT)
			).toList()
		);

		playerAbilities.forEach(abilityInfo -> {
			// Skip weapon aspects
			if (abilityInfo.getDepthsTrigger().equals(DepthsTrigger.WEAPON_ASPECT)) {
				return;
			}
			// Filter to keep the eligible ones that have the same trigger as the ability being replaced
			DepthsTrigger trigger = abilityInfo.getDepthsTrigger();
			ArrayList<DepthsAbilityInfo<?>> eligibleCopy = new ArrayList<>(eligibleAbilities);
			eligibleCopy.removeIf(info -> !info.getDepthsTrigger().equals(trigger));
			if (eligibleCopy.isEmpty()) {
				return;
			}
			DepthsAbilityInfo<?> chosenOne = FastUtils.getRandomElement(eligibleCopy);
			// Remove the chosen ability from the original list so that it can't get chosen again
			eligibleAbilities.remove(chosenOne);
			// Get the upgraded level, capped at Legendary (or keep Twisted level if the ability was Twisted level)
			int level = DepthsManager.getInstance().getPlayerLevelInAbility(abilityInfo.getDisplayName(), mPlayer);
			int finalLevel = level == 6 ? 6 : Math.min(5, level + UPGRADE_LEVELS);
			// Remove the old ability, and add the new chosen ability
			DepthsManager.getInstance().setPlayerLevelInAbility(abilityInfo.getDisplayName(), mPlayer, 0, false);
			DepthsManager.getInstance().setPlayerLevelInAbility(chosenOne.getDisplayName(), mPlayer, finalLevel, false);
		});

		int[] chances = {80, 15, 5, 0, 0};
		for (int i = 0; i < EXTRA_ABILITIES[mRarity - 1]; i++) {
			DepthsManager.getInstance().getRandomAbility(mPlayer, dp, chances, false, false, false);
		}

		DepthsManager.getInstance().validateOfferings(dp);
	}

	public void applyResistance() {
		mPlugin.mEffectManager.addEffect(mPlayer, RESISTANCE_EFFECT_NAME, new PercentDamageReceived(RESISTANCE_DURATION[mRarity - 1], -1));
	}

	private static Description<Rebirth> getDescription(int rarity, TextColor textColor) {
		DescriptionBuilder<Rebirth> desc = new DescriptionBuilder<Rebirth>(textColor)
			.add("When you would permanently die, you are reborn, gaining 100% Resistance for ")
			.addDuration(a -> RESISTANCE_DURATION[rarity - 1], RESISTANCE_DURATION[rarity - 1], false, true)
			.add(" seconds and refunding the revive duration lost from that death.")
			.add(" When this happens, all of your abilities are replaced with random new abilities in the same slot from any tree, including Prismatic.")
			.add(" These abilities are upgraded by " + UPGRADE_LEVELS + " levels.");
		if (rarity > 1) {
			desc = desc.add(" In addition, gain ")
				.add(a -> EXTRA_ABILITIES[rarity - 1], EXTRA_ABILITIES[rarity - 1], false, null, true)
				.add(" more random abilities.");
		}
		return desc;
	}


}
