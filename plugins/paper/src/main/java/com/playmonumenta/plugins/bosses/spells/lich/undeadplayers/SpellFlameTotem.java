package com.playmonumenta.plugins.bosses.spells.lich.undeadplayers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

/*
Undead Mage - Casts Magma shield every 10 second(s) dealing 30 damage in
a cone in front of it and setting players on fire for 5 seconds.
 */
public class SpellFlameTotem extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private @Nullable ArmorStand mTotem;

	public SpellFlameTotem(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, false));
		world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.HOSTILE, 1, 0.8f);
		mTotem = (ArmorStand) LibraryOfSoulsIntegration.summon(mBoss.getLocation().add((Math.random() - 0.5) * 3, 0.5, (Math.random() - 0.5) * 3), "InfernalTotem");
		if (mTotem != null) {
			mTotem.setMarker(false);
			mTotem.setGravity(true);
			mTotem.setDisabledSlots(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
			mTotem.addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);
		}

		BukkitRunnable manageTotem = new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				mT++;
				if (mT >= 20 * 6 || !mBoss.isValid()) {
					this.cancel();
					if (mTotem != null) {
						mTotem.remove();
					}
				}
			}
		};
		manageTotem.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(manageTotem);
	}

	@Override
	public boolean canRun() {
		return PlayerUtils.playersInRange(mBoss.getLocation(), 9, true).size() > 0;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 20;
	}

}
