package com.playmonumenta.plugins.depths.abilities.curses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.Arrays;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class CurseOfPride extends DepthsAbility {
	public static final String ABILITY_NAME = "Curse of Pride";
	public static final int MAX_TREE_ABILITIES = 3;
	public static final double VULN_PER_ABILITY = 0.1;

	public static final DepthsAbilityInfo<CurseOfPride> INFO =
		new DepthsAbilityInfo<>(CurseOfPride.class, ABILITY_NAME, CurseOfPride::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.displayItem(Material.PURPLE_GLAZED_TERRACOTTA)
			.gain(CurseOfPride::gain)
			.descriptions(CurseOfPride::getDescription);

	private double mVulnerability;

	public CurseOfPride(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		// Since all abilities are refreshed when we get a new ability, we only have to calculate the value once
		mVulnerability = 0;
		Bukkit.getScheduler().runTask(plugin, () -> {
			for (DepthsTree tree : DepthsTree.OWNABLE_TREES) {
				int count = DepthsManager.getInstance().getPlayerAbilities(player).stream()
					.filter(ability -> ability.getDepthsTree() == tree)
					.toList().size();

				if (count > MAX_TREE_ABILITIES) {
					mVulnerability += count * VULN_PER_ABILITY;
				}
			}
		});
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (DamageEvent.DamageType.getScalableDamageType().contains(event.getType()) && mVulnerability != 0) {
			event.updateDamageWithMultiplier(1 + mVulnerability);
		}
	}

	public static void gain(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		dp.mEligibleTrees = Arrays.stream(DepthsTree.OWNABLE_TREES).toList();
	}

	private static Description<CurseOfPride> getDescription() {
		return new DescriptionBuilder<CurseOfPride>()
			.add("Unlock all trees. Having more than ")
			.add(MAX_TREE_ABILITIES)
			.add(" abilities from one tree makes you take ")
			.addPercent(VULN_PER_ABILITY)
			.add(" more damage per ability. ")
			.add((a, p) -> a != null ? Component.text("\nCurrent damage taken: " + (a.mVulnerability > 0 ? "+" : "") + StringUtils.multiplierToPercentageWithSign(a.mVulnerability)) : Component.empty());
	}
}
