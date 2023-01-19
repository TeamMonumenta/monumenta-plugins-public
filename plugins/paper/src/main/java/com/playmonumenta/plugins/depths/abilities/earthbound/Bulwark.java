package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class Bulwark extends DepthsAbility {

	public static final String ABILITY_NAME = "Bulwark";
	public static final int[] COOLDOWN = {18 * 20, 16 * 20, 14 * 20, 12 * 20, 10 * 20, 7 * 20};

	public static final DepthsAbilityInfo<Bulwark> INFO =
		new DepthsAbilityInfo<>(Bulwark.class, ABILITY_NAME, Bulwark::new, DepthsTree.EARTHBOUND, DepthsTrigger.PASSIVE)
			.linkedSpell(ClassAbility.BULWARK)
			.cooldown(COOLDOWN)
			.displayItem(new ItemStack(Material.NETHERITE_HELMET))
			.descriptions(Bulwark::getDescription);

	public Bulwark(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (source != null
			    && event.getType() == DamageType.MELEE
			    && !event.isBlocked()
			    && !isOnCooldown()) {
			Location loc = mPlayer.getLocation();
			World world = mPlayer.getWorld();
			Location particleLoc = loc.add(0, mPlayer.getHeight() / 2, 0);
			new PartialParticle(Particle.CLOUD, particleLoc, 10, 0.35, 0.35, 0.35, 0.125).spawnAsPlayerPassive(mPlayer);
			new PartialParticle(Particle.BLOCK_DUST, particleLoc, 20, 0.5, 0.5, 0.5, 0.125, Material.STRIPPED_DARK_OAK_WOOD.createBlockData()).spawnAsPlayerPassive(mPlayer);
			world.playSound(loc, Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1, 1.2f);
			event.setCancelled(true);
			putOnCooldown();
		}
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("You block the next melee attack that would have hit you, nullifying the damage. Cooldown: ")
			.append(Component.text(COOLDOWN[rarity - 1] / 20 + "s", color))
			.append(Component.text("."));
	}
}
