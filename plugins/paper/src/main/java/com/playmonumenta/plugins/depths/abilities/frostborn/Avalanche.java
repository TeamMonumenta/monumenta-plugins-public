package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Avalanche extends DepthsAbility {

	public static final String ABILITY_NAME = "Avalanche";
	public static final int[] DAMAGE = {30, 35, 40, 45, 50, 60};
	public static final int COOLDOWN_TICKS = 20 * 20;
	public static final int SLOW_DURATION = 2 * 20;
	public static final double SLOW_MODIFIER = 0.99;
	public static final int RADIUS = 10;
	private static final Particle.DustOptions ICE_PARTICLE_COLOR = new Particle.DustOptions(Color.fromRGB(91, 187, 255), 1.0f);

	public static final DepthsAbilityInfo<Avalanche> INFO =
		new DepthsAbilityInfo<>(Avalanche.class, ABILITY_NAME, Avalanche::new, DepthsTree.FROSTBORN, DepthsTrigger.SWAP)
			.linkedSpell(ClassAbility.AVALANCHE)
			.cooldown(COOLDOWN_TICKS)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Avalanche::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP), HOLDING_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.SNOW_BLOCK))
			.descriptions(Avalanche::getDescription);

	public Avalanche(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();

		HashSet<Location> iceToBreak = new HashSet<>(DepthsUtils.iceActive.keySet());
		iceToBreak.removeIf(l -> l.getWorld() != loc.getWorld() || l.distance(loc) > RADIUS || !DepthsUtils.isIce(l.getBlock().getType()));

		if (iceToBreak.size() == 0) {
			return;
		}

		putOnCooldown();

		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1, 0.95f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1, 0.95f);

		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1, 0.75f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.BLOCKS, 1, 1.25f);

		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 0.5f, 1f);

		List<LivingEntity> hitMobs = new ArrayList<>();

		//Shatter all nearby ice
		for (Location l : iceToBreak) {
			Location aboveLoc = l.clone().add(0.5, 1, 0.5);

			//Damage and root mobs
			for (LivingEntity mob : EntityUtils.getNearbyMobs(aboveLoc, 1.0)) {
				if (!hitMobs.contains(mob)) {
					EntityUtils.applySlow(mPlugin, SLOW_DURATION, SLOW_MODIFIER, mob);
					DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, DAMAGE[mRarity - 1], mInfo.getLinkedSpell());
					hitMobs.add(mob);
				}
			}

			Block b = l.getBlock();
			if (b.getType() == Permafrost.PERMAFROST_ICE_MATERIAL) {
				//If special permafrost ice, set to normal ice instead of destroying
				b.setType(DepthsUtils.ICE_MATERIAL);
			} else {
				b.setBlockData(DepthsUtils.iceActive.get(l));
				DepthsUtils.iceActive.remove(l);
			}

			new PartialParticle(Particle.REDSTONE, aboveLoc, 15, 0.5, 0.5, 0.5, ICE_PARTICLE_COLOR).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, aboveLoc, 2, 0.5, 0.25, 0.5).spawnAsPlayerActive(mPlayer);
		}
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Swap hands to shatter all ice blocks within a radius of " + RADIUS + ", dealing ")
			       .append(Component.text(DAMAGE[rarity - 1], color))
			       .append(Component.text(" magic damage to enemies on the shattered ice. Affected enemies are rooted for " + SLOW_DURATION / 20 + " seconds. Cooldown: " + COOLDOWN_TICKS / 20 + "s."));
	}


}

