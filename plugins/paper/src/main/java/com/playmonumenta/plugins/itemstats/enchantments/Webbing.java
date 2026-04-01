package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Webbing implements Enchantment {

	Material BLOCK_TYPE = Material.COBWEB;
	private static final BlockFace[] BLOCK_FACES = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.WEBBING;
	}

	@Override
	public String getName() {
		return "Webbing";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET, Slot.PROJECTILE);
	}

	@Override
	public void onDamage(final Plugin plugin, final Player player, final double level, final DamageEvent event,
						 final LivingEntity target) {
		if (EntityUtils.isElite(target) && !EntityUtils.isCCImmuneMob(target)) {
			Block block = target.getLocation().getBlock();
			//first web always on elite feet.
			if (block.isEmpty() || block.isReplaceable()) {
				block.setType(BLOCK_TYPE);
			}
			//If level is 2+, spread cobwebs around.
			int i = 1;
			while (level > i) {
				block = block.getRelative(BLOCK_FACES[FastUtils.randomIntInRange(0, 5)]);
				if (block.isEmpty() || block.isReplaceable()) {
					block.setType(BLOCK_TYPE);
				}
				i++;
			}
		}
	}
}
