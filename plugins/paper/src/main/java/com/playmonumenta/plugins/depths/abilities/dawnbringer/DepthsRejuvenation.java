package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DepthsRejuvenation extends DepthsAbility {

	public static final String ABILITY_NAME = "Rejuvenation";
	public static final int[] HEAL_INTERVAL = {100, 90, 80, 70, 60, 50};
	public static final int RADIUS = 12;
	public static final double PERCENT_HEAL = .05;

	private static final Map<UUID, Integer> LAST_HEAL_TICK = new HashMap<>();

	public static final DepthsAbilityInfo<DepthsRejuvenation> INFO =
		new DepthsAbilityInfo<>(DepthsRejuvenation.class, ABILITY_NAME, DepthsRejuvenation::new, DepthsTree.DAWNBRINGER, DepthsTrigger.PASSIVE)
			.displayItem(new ItemStack(Material.NETHER_STAR))
			.descriptions(DepthsRejuvenation::getDescription);

	private int mTimer = 0;

	public DepthsRejuvenation(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		int healInterval = HEAL_INTERVAL[mRarity - 1];
		if (oneSecond && !mPlayer.isDead()) {
			mTimer += 20;
			if (mTimer % healInterval == 0) {
				for (Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), RADIUS, true)) {
					// Prevents stacking
					boolean highestLevel = true;
					for (Player checkPlayer : PlayerUtils.playersInRange(player.getLocation(), RADIUS, true)) {
						int teammateRarity = DepthsManager.getInstance().getPlayerLevelInAbility(ABILITY_NAME, checkPlayer);
						if (checkPlayer == mPlayer || teammateRarity == 0) {
							continue;
						} else if (teammateRarity >= mRarity) {
							highestLevel = false;
							break;
						}
					}
					Integer lastHealTick = LAST_HEAL_TICK.get(player.getUniqueId());
					if (lastHealTick == null || (player.getTicksLived() - lastHealTick >= healInterval && highestLevel)) {
						LAST_HEAL_TICK.put(player.getUniqueId(), player.getTicksLived());

						double maxHealth = EntityUtils.getMaxHealth(player);
						if (player.getHealth() != maxHealth) {
							PlayerUtils.healPlayer(mPlugin, player, PERCENT_HEAL * maxHealth, mPlayer);
							new PartialParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 1, 0.07, 0.07, 0.07, 0.001)
								.spawnAsPlayerBuff(player);
						}
					}
				}
			}
		}
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("All players within " + RADIUS + " blocks of you (including yourself) heal " + StringUtils.multiplierToPercentage(PERCENT_HEAL) + "% of their max health every ")
			.append(Component.text(StringUtils.to2DP(HEAL_INTERVAL[rarity - 1] / 20.0), color))
			.append(Component.text(" seconds. A given player will only be healed by the highest Rejuvenation that affects them."));
	}

}

