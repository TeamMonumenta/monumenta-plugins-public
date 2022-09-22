package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BezoarCS implements CosmeticSkill {

	private static final BlockData FALLING_DUST_DATA = Material.LIME_CONCRETE.createBlockData();
	public static final ImmutableMap<String, BezoarCS> SKIN_LIST = ImmutableMap.<String, BezoarCS>builder()
		.put(SunriseBrewCS.NAME, new SunriseBrewCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.BEZOAR;
	}

	@Override
	public Material getDisplayItem() {
		return Material.LIME_CONCRETE;
	}

	public ItemStack bezoarItem() {
		ItemStack itemBezoar = new ItemStack(Material.LIME_CONCRETE);
		ItemMeta bezoarMeta = itemBezoar.getItemMeta();
		bezoarMeta.displayName(Component.text("Bezoar", NamedTextColor.WHITE)
			.decoration(TextDecoration.ITALIC, false));
		itemBezoar.setItemMeta(bezoarMeta);
		ItemUtils.setPlainName(itemBezoar, "Bezoar");
		return itemBezoar;
	}

	public void bezoarTick(Player mPlayer, Location loc, int tick) {
		new PartialParticle(Particle.FALLING_DUST, loc, 1, 0.2, 0.2, 0.2, FALLING_DUST_DATA).spawnAsPlayerActive(mPlayer);
	}

	public void bezoarPickup(Player mPlayer, Location loc) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.BLOCK_STONE_BREAK, 1, 0.75f);
		world.playSound(loc, Sound.BLOCK_STONE_BREAK, 1, 0.75f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1f);
		new PartialParticle(Particle.BLOCK_CRACK, loc, 30, 0.15, 0.15, 0.15, 0.75F,
			Material.LIME_CONCRETE.createBlockData()).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.TOTEM, loc, 20, 0, 0, 0, 0.35F).spawnAsPlayerActive(mPlayer);
	}
}
