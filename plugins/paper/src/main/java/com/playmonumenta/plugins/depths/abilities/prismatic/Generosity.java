package com.playmonumenta.plugins.depths.abilities.prismatic;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Generosity extends DepthsAbility {

	public static final String ABILITY_NAME = "Generosity";

	public static final DepthsAbilityInfo<Generosity> INFO =
		new DepthsAbilityInfo<>(Generosity.class, ABILITY_NAME, Generosity::new, DepthsTree.PRISMATIC, DepthsTrigger.PASSIVE)
			.displayItem(Material.RAW_GOLD)
			.descriptions(Generosity::getDescription);

	public Generosity(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static Description<Generosity> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Generosity>(color)
			.add("Once per floor, removing or mutating away an ability gifts it to all other teammates, provided they have an open slot for it. The donated ability's level will be ")
			.add(DepthsUtils.getRarityComponent(rarity))
			.add(".");
	}

	public static void abilityRemoved(DepthsPlayer depthsPlayer, @Nullable String removedAbility) {
		if (depthsPlayer.mUsedGenerosity) {
			return;
		}
		int generosityLevel = depthsPlayer.getLevelInAbility(ABILITY_NAME);
		if (generosityLevel == 0 || removedAbility == null || removedAbility.equals(ABILITY_NAME)) {
			return;
		}
		DepthsParty party = DepthsManager.getInstance().getPartyFromId(depthsPlayer);
		DepthsAbilityInfo<?> removedAbilityInfo = null;
		for (DepthsAbilityInfo<?> info : DepthsManager.getAbilities()) {
			if (removedAbility.equals(info.getDisplayName())) {
				removedAbilityInfo = info;
				break;
			}
		}
		Player removedPlayer = depthsPlayer.getPlayer();
		boolean foundPlayer = false;
		if (party != null && removedAbilityInfo != null && removedPlayer != null) {
			DepthsAbilityInfo<?> finalRemovedAbilityInfo = removedAbilityInfo;
			for (DepthsPlayer dp : party.mPlayersInParty) {
				Player otherPlayer = dp.getPlayer();
				if (otherPlayer == null || dp == depthsPlayer) {
					continue;
				}
				int currentLevel = dp.getLevelInAbility(removedAbility);
				if (currentLevel < generosityLevel &&
					DepthsManager.getInstance().getPlayerAbilities(otherPlayer).stream()
						.filter(abilityInfo -> abilityInfo != finalRemovedAbilityInfo)
						.filter(abilityInfo -> !abilityInfo.getDepthsTrigger().equals(DepthsTrigger.PASSIVE))
						.noneMatch(abilityInfo -> abilityInfo.getDepthsTrigger().equals(finalRemovedAbilityInfo.getDepthsTrigger()))
				) {
					foundPlayer = true;
					dp.mEarnedRewards.add(DepthsRoomType.DepthsRewardType.GENEROSITY);
					dp.mGenerosityGifts.add(removedAbilityInfo.getAbilityItem(generosityLevel, otherPlayer, currentLevel));
					otherPlayer.playSound(otherPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
					Component abilityName = DepthsManager.getInstance().colorAbilityWithHover(removedAbility, generosityLevel, otherPlayer);
					dp.sendMessage(removedPlayer.displayName().append(Component.text(" has generously gifted you: ")).append(abilityName).append(Component.text(" at ")).append(DepthsUtils.getRarityComponent(generosityLevel)).append(Component.text(" level! You can accept the gift in the rewards in your trinket.")));
				}
			}
		}
		if (foundPlayer) {
			depthsPlayer.mUsedGenerosity = true;
		}
	}
}
