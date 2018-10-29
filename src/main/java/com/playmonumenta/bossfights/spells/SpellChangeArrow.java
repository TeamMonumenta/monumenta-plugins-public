package com.playmonumenta.bossfights.spells;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public class SpellChangeArrow implements Spell {
	private LivingEntity mLauncher;
	private Random mRand = new Random();
	
	public SpellChangeArrow(LivingEntity launcher) {
		mLauncher = launcher;
	}
	private ItemStack getTippedArrow() {
		int rand = mRand.nextInt(4); 
		ItemStack stack = new ItemStack(Material.TIPPED_ARROW, 1);

		PotionMeta meta = (PotionMeta)stack.getItemMeta();
		if(rand==0) {
			meta.addCustomEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1) , false);
		}
		if(rand==1) {
			meta.addCustomEffect(new PotionEffect(PotionEffectType.SLOW, 100, 1) , false);
		}
		if(rand==2) {
			meta.addCustomEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0) , false);
		}
		if(rand==3) {
			meta.addCustomEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 0) , false);
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
	public int duration() {
		return 160;
	}
}
