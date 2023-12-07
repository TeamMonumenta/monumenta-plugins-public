package com.playmonumenta.plugins.depths.abilities.prismatic;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class Multiplicity extends DepthsAbility {

	public static final String ABILITY_NAME = "Multiplicity";
	public static final double[] CDR = {0.2, 0.25, 0.3, 0.35, 0.4, 0.5};
	public static final double[] DAMAGE = {0.12, 0.15, 0.18, 0.21, 0.24, 0.3};
	public static final int DURATION = 5 * 20;
	public static final String DAMAGE_EFFECT = "MultiplicityPercentDamageDealtEffect";

	public static final DepthsAbilityInfo<Multiplicity> INFO =
		new DepthsAbilityInfo<>(Multiplicity.class, ABILITY_NAME, Multiplicity::new, DepthsTree.PRISMATIC, DepthsTrigger.PASSIVE)
			.displayItem(Material.AMETHYST_CLUSTER)
			.descriptions(Multiplicity::getDescription);

	private final Map<DepthsTree, DepthsAbility> mLastAbilities = new HashMap<>();

	public Multiplicity(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		//TODO bulwark + dodging maybe shouldn't count?
		if (!(event.getAbility() instanceof DepthsAbility ability)) {
			return true;
		}

		DepthsAbilityInfo<?> info = ability.getInfo();
		DepthsTree tree = info.getDepthsTree();
		if (tree == null) {
			return true;
		}

		if (mLastAbilities.containsKey(tree)) {
			if (mLastAbilities.size() > 1) {
				// "You fucked up" effects
				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, SoundCategory.PLAYERS, 2, 1);
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, SoundCategory.PLAYERS, 2, 0.75f), 4);
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, SoundCategory.PLAYERS, 2, 0.5f), 8);
			}
			mLastAbilities.clear();
			mLastAbilities.put(tree, ability);
			return true;
		}

		mLastAbilities.put(tree, ability);
		if (mLastAbilities.size() == 2) {
			// "It's about to activate" effects
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, SoundCategory.PLAYERS, 2, 0.65f);
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, SoundCategory.PLAYERS, 2, 0.95f), 6);
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, SoundCategory.PLAYERS, 2, 1.25f), 12);
		}

		if (mLastAbilities.size() >= 3) {
			mLastAbilities.values().forEach(a -> {
				AbilityInfo<?> otherInfo = a.getInfo();
				ClassAbility spell = otherInfo.getLinkedSpell();
				if (spell != null) {
					mPlugin.mTimers.updateCooldown(mPlayer, spell, (int) (CDR[mRarity - 1] * a.getInfo().getModifiedCooldown(mPlayer, a.getAbilityScore())));
				}
			});
			mPlugin.mEffectManager.addEffect(mPlayer, DAMAGE_EFFECT, new PercentDamageDealt(DURATION, DAMAGE[mRarity - 1], DamageEvent.DamageType.getAllMeleeTypes()));
			mLastAbilities.clear();

			// Activation Effects
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, SoundCategory.PLAYERS, 2, 0.85f);
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, SoundCategory.PLAYERS, 2, 1.15f), 2);
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, SoundCategory.PLAYERS, 2, 1.45f), 4);
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, SoundCategory.PLAYERS, 2, 1.75f), 6);
			new PPCircle(Particle.TOTEM, mPlayer.getLocation(), 1).count(20).ringMode(true).rotateDelta(true)
				.delta(1, 0.5, 0).extra(0.5).spawnAsPlayerActive(mPlayer);
		}

		return true;
	}

	private static Description<Multiplicity> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Multiplicity>(color)
			.add("After casting 3 abilities from different trees in a row, refund ")
			.addPercent(a -> CDR[rarity - 1], CDR[rarity - 1], false, true)
			.add(" of those abilities' cooldowns and gain ")
			.addPercent(a -> DAMAGE[rarity - 1], DAMAGE[rarity - 1], false, true)
			.add(" melee damage for ")
			.addDuration(DURATION)
			.add(" seconds.");
	}
}
