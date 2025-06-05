package com.playmonumenta.plugins.depths.abilities.prismatic;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class Diversity extends DepthsAbility {
	public static final String ABILITY_NAME = "Diversity";
	public static final int TREES_REQUIRED = 6;
	public static final String SPEED_EFFECT_NAME = "DiversitySpeed";
	public static final String SPEED_EFFECT_ATTR_NAME = "DiversitySpeedAttr";
	public static final double[] SPEED = {0.10, 0.12, 0.14, 0.16, 0.18, 0.22};
	public static final double[] RARITY_INCREASE = {0.08, 0.1, 0.12, 0.14, 0.16, 0.2};

	public static final DepthsAbilityInfo<Diversity> INFO =
		new DepthsAbilityInfo<>(Diversity.class, ABILITY_NAME, Diversity::new, DepthsTree.PRISMATIC, DepthsTrigger.PASSIVE)
			.displayItem(Material.CHISELED_QUARTZ_BLOCK)
			.descriptions(Diversity::getDescription);

	private boolean mActive = false;
	private int mTreeCount = 0;

	public Diversity(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		DepthsPlayer depthsPlayer = DepthsManager.getInstance().getDepthsPlayer(player);
		if (depthsPlayer == null) {
			return;
		}

		if (depthsPlayer.mDiversityActive) {
			mTreeCount = TREES_REQUIRED;
			mActive = true;
		} else {
			// Since all abilities are refreshed when we get a new ability, we only have to calculate the value once
			Bukkit.getScheduler().runTask(plugin, () -> {
				Set<DepthsTree> uniqueTrees = DepthsManager.getInstance().getPlayerAbilities(depthsPlayer).stream()
					.map(DepthsAbilityInfo::getDepthsTree)
					.filter(Objects::nonNull) // Weapon Aspects have null tree
					.filter(depthsTree -> depthsTree != DepthsTree.CURSE && depthsTree != DepthsTree.GIFT)
					.collect(Collectors.toSet());

				mTreeCount = Math.min(uniqueTrees.size(), TREES_REQUIRED);
				if (mTreeCount == TREES_REQUIRED) {
					//Give a random prismatic the first time this effect is reached
					if (!depthsPlayer.mDiversityGift) {
						depthsPlayer.sendMessage(Component.text("Due to achieving your Diversity goal, you've received a prismatic ability reward in your trinket!"));
						depthsPlayer.addReward(DepthsRoomType.DepthsRewardType.PRISMATIC);
						depthsPlayer.mDiversityGift = true;
					}


					depthsPlayer.mDiversityActive = true;
					mActive = true;
					sendActionBarMessage("Diversity has activated!");
					activationSounds();
				}
			});
		}
	}

	private void activationSounds() {
		new PartialParticle(Particle.VILLAGER_HAPPY, LocationUtils.getHalfHeightLocation(mPlayer), 25).delta(0.2).spawnAsPlayerActive(mPlayer);
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 2, 0.5f);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 2, 0.65f), 15);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 2, 0.9f), 30);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 2, 1.2f), 45);
	}

	public double getRarityIncrease() {
		return mActive ? RARITY_INCREASE[mRarity - 1] : 0;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (!oneSecond || !mActive) {
			return;
		}

		Plugin.getInstance().mEffectManager.addEffect(mPlayer, SPEED_EFFECT_NAME, new PercentSpeed(80, SPEED[mRarity - 1], SPEED_EFFECT_ATTR_NAME).displaysTime(false));
	}

	private static Description<Diversity> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.add("After you possess abilities from six unique trees, gain ")
			.addPercent(a -> SPEED[rarity - 1], SPEED[rarity - 1], false, true)
			.add(" speed permanently, and your chances of finding better abilities are increased by ")
			.addPercent(a -> RARITY_INCREASE[rarity - 1], RARITY_INCREASE[rarity - 1], false, true)
			.add(". The first time you reach this goal, you will receive a selection of other Prismatics.")
			.add((a, p) -> a != null ? Component.text("\nCurrent status: " + (a.mActive ? "Active" : a.mTreeCount + "/" + TREES_REQUIRED)) : Component.empty());
	}
}
