package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BezoarCS implements CosmeticSkill {

	private static final BlockData FALLING_DUST_DATA = Material.LIME_CONCRETE.createBlockData();
	private static final BlockData FALLING_DUST_DATA_PHILOSOPHER = Material.RED_CONCRETE.createBlockData();

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.BEZOAR;
	}

	@Override
	public Material getDisplayItem() {
		return Material.LIME_CONCRETE;
	}

	public ItemStack bezoarItem(boolean philosophersStone) {
		if (philosophersStone) {
			ItemStack itemStone = new ItemStack(Material.RED_CONCRETE);
			ItemMeta stoneMeta = itemStone.getItemMeta();
			stoneMeta.displayName(Component.text("Philosopher's Stone", NamedTextColor.WHITE)
				                      .decoration(TextDecoration.ITALIC, false));
			itemStone.setItemMeta(stoneMeta);
			ItemUtils.setPlainName(itemStone, "Philosopher's Stone");
			return itemStone;
		} else {
			ItemStack itemBezoar = new ItemStack(Material.LIME_CONCRETE);
			ItemMeta bezoarMeta = itemBezoar.getItemMeta();
			bezoarMeta.displayName(Component.text("Bezoar", NamedTextColor.WHITE)
				                       .decoration(TextDecoration.ITALIC, false));
			itemBezoar.setItemMeta(bezoarMeta);
			ItemUtils.setPlainName(itemBezoar, "Bezoar");
			return itemBezoar;
		}
	}

	public void periodicBezoarEffects(Player mPlayer, Location loc, int tick, boolean philosophersStone) {
		new PPPeriodic(Particle.FALLING_DUST, loc).count(1).delta(0.2).data(philosophersStone ? FALLING_DUST_DATA_PHILOSOPHER : FALLING_DUST_DATA).manualTimeOverride(tick).spawnAsPlayerActive(mPlayer);
	}

	public void targetEffects(Player player, Location loc, boolean philosophersStone) {
	}

	public void pickupEffects(Player player, Location loc, boolean philosophersStone) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1, 0.75f);
		world.playSound(loc, Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1, 0.75f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1, 1f);
		new PartialParticle(Particle.BLOCK_CRACK, loc, 30, 0.15, 0.15, 0.15, 0.75F,
			philosophersStone ? FALLING_DUST_DATA_PHILOSOPHER : FALLING_DUST_DATA).spawnAsPlayerActive(player);
		new PartialParticle(Particle.TOTEM, loc, 20, 0, 0, 0, 0.35F).spawnAsPlayerActive(player);

	}

	public void expireEffects(Player player, Location loc, boolean philosophersStone) {
	}

}
