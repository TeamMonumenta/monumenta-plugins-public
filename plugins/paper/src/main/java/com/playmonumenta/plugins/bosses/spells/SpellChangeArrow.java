package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpellChangeArrow extends Spell {
	private LivingEntity mLauncher;

	public SpellChangeArrow(LivingEntity launcher) {
		mLauncher = launcher;
	}

	private static ItemStack getTippedArrow() {
		int rand = FastUtils.RANDOM.nextInt(4);
		ItemStack stack = new ItemStack(Material.TIPPED_ARROW, 1);

		PotionMeta meta = (PotionMeta) stack.getItemMeta();
		if (rand == 0) {
			meta.addCustomEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1), false);
		} else if (rand == 1) {
			meta.addCustomEffect(new PotionEffect(PotionEffectType.SLOW, 100, 1), false);
		} else if (rand == 2) {
			meta.addCustomEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0), false);
		} else if (rand == 3) {
			meta.addCustomEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 0), false);
		}
		stack.setItemMeta(meta);
		return stack;
	}

	@Override
	public void run() {
		ItemStack arrows = getTippedArrow();
		mLauncher.getEquipment().setItemInOffHand(arrows);
	}

	@Override
	public int cooldownTicks() {
		return 160;
	}
}
