package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Bulwark extends DepthsAbility {

	public static final String ABILITY_NAME = "Bulwark";
	public static final int[] COOLDOWN = {18 * 20, 16 * 20, 14 * 20, 12 * 20, 10 * 20, 7 * 20};

	public static final DepthsAbilityInfo<Bulwark> INFO =
		new DepthsAbilityInfo<>(Bulwark.class, ABILITY_NAME, Bulwark::new, DepthsTree.EARTHBOUND, DepthsTrigger.PASSIVE)
			.linkedSpell(ClassAbility.BULWARK)
			.cooldown(COOLDOWN)
			.displayItem(new ItemStack(Material.NETHERITE_HELMET))
			.descriptions(Bulwark::getDescription, MAX_RARITY);

	public Bulwark(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (source != null
			    && event.getType() == DamageType.MELEE
			    && !event.isBlocked()) {
			Location loc = mPlayer.getLocation();
			World world = mPlayer.getWorld();
			Location particleLoc = loc.add(0, mPlayer.getHeight() / 2, 0);
			world.spawnParticle(Particle.CLOUD, particleLoc, 10, 0.35, 0.35, 0.35, 0.125);
			world.spawnParticle(Particle.BLOCK_DUST, particleLoc, 20, 0.5, 0.5, 0.5, 0.125, Material.STRIPPED_DARK_OAK_WOOD.createBlockData());
			world.playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1, 1.2f);
			event.setCancelled(true);
			putOnCooldown();
		}
	}

	private static String getDescription(int rarity) {
		return "You block the next melee attack that would have hit you, nullifying the damage. Cooldown: " + DepthsUtils.getRarityColor(rarity) + COOLDOWN[rarity - 1] / 20 + "s" + ChatColor.WHITE + ".";
	}
}
