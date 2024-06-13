package com.playmonumenta.plugins.depths.abilities.prismatic;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityCollection;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsListener;
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
import com.playmonumenta.plugins.depths.abilities.steelsage.PrecisionStrike;
import com.playmonumenta.plugins.depths.abilities.windwalker.Whirlwind;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;

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
			activate(block);
		}
		return true;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		LivingEntity entity = event.getEntity();
		if (EntityUtils.isElite(entity)) {
			Location loc = entity.getLocation();
			activate(loc);
			DepthsListener.attemptSundrops(loc, mPlayer);
		}
	}

	private void activate(Location loc) {
		activate(loc, loc.getBlock());
	}

	private void activate(Block block) {
		activate(block.getLocation().add(0.5, 0, 0.5), block);
	}

	private void activate(Location loc, Block block) {
		Set<AbilityInfo<?>> spawnerAbilities = getSpawnerAbilities(mPlugin, mPlayer);
		for (AbilityInfo<?> info : spawnerAbilities) {
			if (info == Entrench.INFO) {
				Entrench.onSpawnerBreak(mPlugin, mPlayer, mRarity, loc);
			} else if (info == FlameSpirit.INFO) {
				FlameSpirit.onSpawnerBreak(mPlugin, mPlayer, mRarity, loc);
			} else if (info == Permafrost.INFO) {
				Permafrost.onSpawnerBreak(mPlayer, mRarity, block);
			} else if (info == Whirlwind.INFO) {
				Whirlwind.onSpawnerBreak(mPlugin, mPlayer, mRarity, loc);
			} else if (info == PrecisionStrike.INFO) {
				PrecisionStrike.onSpawnerBreak(mPlugin, mPlayer, mRarity);
			}
		}
	}

	private static Set<AbilityInfo<?>> getSpawnerAbilities(Plugin plugin, Player player) {
		DepthsParty party = DepthsManager.getInstance().getDepthsParty(player);
		if (party == null) {
			return Set.of();
		}
		Set<AbilityInfo<?>> spawnerAbilities = new HashSet<>();
		party.mPlayersInParty.stream().map(DepthsPlayer::getPlayer).filter(Objects::nonNull).map(plugin.mAbilityManager::getPlayerAbilities).map(AbilityCollection::getAbilitiesIgnoringSilence)
			.forEach(abilities -> abilities.stream().map(Ability::getInfo).filter(info -> info instanceof DepthsAbilityInfo<?> dinfo && dinfo.getDepthsTrigger() == DepthsTrigger.SPAWNER).filter(info -> info != INFO).forEach(spawnerAbilities::add));
		return spawnerAbilities;
	}

	private static Description<Convergence> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Convergence>(color)
			.add("Breaking a spawner or killing an Elite mob triggers each of your living teammates' spawner break abilities at ")
			.add(DepthsUtils.getRarityComponent(rarity))
			.add(" level.")
			.add(ability -> {
					if (ability == null) {
						return Component.empty();
					}
					Player player = ability.getPlayer();
					Set<AbilityInfo<?>> spawnerAbilities = getSpawnerAbilities(Plugin.getInstance(), player);
					if (spawnerAbilities.isEmpty()) {
						return Component.empty();
					}
					List<Component> names = spawnerAbilities.stream()
						.map(info -> info instanceof DepthsAbilityInfo dInfo ? dInfo.getColoredName() : null)
						.filter(Objects::nonNull).toList();
					Component namesComp = MessagingUtils.concatenateComponents(names, Component.text(", "));
					return Component.text("\n\nCurrent abilities: ").append(namesComp);
			 });
	}
}
