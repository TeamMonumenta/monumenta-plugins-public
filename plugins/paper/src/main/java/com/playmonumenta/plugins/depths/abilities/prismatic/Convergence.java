package com.playmonumenta.plugins.depths.abilities.prismatic;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityCollection;
import com.playmonumenta.plugins.abilities.AbilityInfo;
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
import com.playmonumenta.plugins.depths.abilities.earthbound.Entrench;
import com.playmonumenta.plugins.depths.abilities.flamecaller.FlameSpirit;
import com.playmonumenta.plugins.depths.abilities.frostborn.Permafrost;
import com.playmonumenta.plugins.depths.abilities.windwalker.Whirlwind;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

public class Convergence extends DepthsAbility {
	public static final String ABILITY_NAME = "Convergence";

	public static final DepthsAbilityInfo<Convergence> INFO =
		new DepthsAbilityInfo<>(Convergence.class, ABILITY_NAME, Convergence::new, DepthsTree.PRISMATIC, DepthsTrigger.SPAWNER)
			.displayItem(Material.RECOVERY_COMPASS)
			.descriptions(Convergence::getDescription);

	public Convergence(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean blockBreakEvent(BlockBreakEvent event) {
		if (event.isCancelled()) {
			return true;
		}
		Block block = event.getBlock();
		if (ItemUtils.isPickaxe(mPlayer.getInventory().getItemInMainHand()) && block.getType() == Material.SPAWNER) {
			DepthsParty party = DepthsManager.getInstance().getDepthsParty(mPlayer);
			if (party == null) {
				return true;
			}
			Set<AbilityInfo<?>> spawnerAbilities = new HashSet<>();
			party.mPlayersInParty.stream().map(DepthsPlayer::getPlayer).filter(Objects::nonNull).map(mPlugin.mAbilityManager::getPlayerAbilities).map(AbilityCollection::getAbilitiesIgnoringSilence)
				.forEach(abilities -> abilities.stream().map(Ability::getInfo).filter(info -> info instanceof DepthsAbilityInfo<?> dinfo && dinfo.getDepthsTrigger() == DepthsTrigger.SPAWNER).forEach(spawnerAbilities::add));
			for (AbilityInfo<?> info : spawnerAbilities) {
				if (info == Entrench.INFO) {
					Entrench.onSpawnerBreak(mPlugin, mPlayer, mRarity, block);
				} else if (info == FlameSpirit.INFO) {
					FlameSpirit.onSpawnerBreak(mPlugin, mPlayer, mRarity, block);
				} else if (info == Permafrost.INFO) {
					Permafrost.onSpawnerBreak(mPlayer, mRarity, block);
				} else if (info == Whirlwind.INFO) {
					Whirlwind.onSpawnerBreak(mPlugin, mPlayer, mRarity, block);
				}
			}
		}
		return true;
	}

	private static Description<Convergence> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Convergence>(color)
			.add("Breaking a spawner triggers each of your living teammates' spawner break abilities at ")
			.add(DepthsUtils.getRarityComponent(rarity))
			.add(" level");
	}
}
