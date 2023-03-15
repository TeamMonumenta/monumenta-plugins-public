package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Taunt extends DepthsAbility {

	public static final String ABILITY_NAME = "Taunt";
	private static final int COOLDOWN = 20 * 18;
	private static final double[] ABSORPTION = {1, 1.25, 1.5, 1.75, 2, 2.5};
	private static final int CAST_RANGE = 12;
	private static final int MAX_ABSORB = 6;
	private static final int ABSORPTION_DURATION = 20 * 8;

	public static final DepthsAbilityInfo<Taunt> INFO =
		new DepthsAbilityInfo<>(Taunt.class, ABILITY_NAME, Taunt::new, DepthsTree.EARTHBOUND, DepthsTrigger.SHIFT_LEFT_CLICK)
			.linkedSpell(ClassAbility.TAUNT)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Taunt::cast,
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true).keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE), HOLDING_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.GOLDEN_CHESTPLATE))
			.descriptions(Taunt::getDescription);

	public Taunt(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, CAST_RANGE);
		mobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));

		if (mobs.size() > 0) {
			putOnCooldown();

			// add rarity% absorption for each affected mob, up to 6
			AbsorptionUtils.addAbsorption(mPlayer, Math.min(mobs.size(), MAX_ABSORB) * ABSORPTION[mRarity - 1], MAX_ABSORB * ABSORPTION[mRarity - 1], ABSORPTION_DURATION);
			for (LivingEntity le : mobs) {
				EntityUtils.applyTaunt(le, mPlayer);
				new PartialParticle(Particle.BLOCK_DUST, le.getLocation(), 50, 0.1, 0.1, 0.1, 0.1, Material.DIRT.createBlockData()).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.FIREWORKS_SPARK, le.getLocation(), 30, 0.1, 0.1, 0.1, 0.2).spawnAsPlayerActive(mPlayer);
			}
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 1, 1.2f);
			new PartialParticle(Particle.BLOCK_DUST, mPlayer.getLocation(), 50, 0.1, 0.1, 0.1, 0.1, Material.DIRT.createBlockData()).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation(), 30, 0.1, 0.1, 0.1, 0.2).spawnAsPlayerActive(mPlayer);
		}
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Left click while sneaking and holding a weapon to have all enemies within " + CAST_RANGE + " blocks target you, and you gain ")
			.append(Component.text(StringUtils.to2DP(ABSORPTION[rarity - 1]), color))
			.append(Component.text(" absorption for every enemy (up to " + MAX_ABSORB + " enemies) afflicted, for " + ABSORPTION_DURATION / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s."));
	}


}
