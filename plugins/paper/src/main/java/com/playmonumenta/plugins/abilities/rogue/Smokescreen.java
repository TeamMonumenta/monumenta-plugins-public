package com.playmonumenta.plugins.abilities.rogue;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class Smokescreen extends Ability {

	private static final int SMOKESCREEN_RANGE = 6;
	private static final int SMOKESCREEN_DURATION = 8 * 20;
	private static final double SMOKESCREEN_SLOWNESS_AMPLIFIER = 0.2;
	private static final double WEAKEN_EFFECT_1 = 0.2;
	private static final double WEAKEN_EFFECT_2 = 0.4;
	private static final int SMOKESCREEN_COOLDOWN = 20 * 20;

	private final double mWeakenEffect;

	public Smokescreen(Plugin plugin, Player player) {
		super(plugin, player, "Smoke Screen");
		mInfo.mLinkedSpell = ClassAbility.SMOKESCREEN;
		mInfo.mScoreboardId = "SmokeScreen";
		mInfo.mShorthandName = "Smk";
		mInfo.mDescriptions.add("When holding two swords, right-click while sneaking and looking down to release a cloud of smoke, afflicting all enemies in a 6 block radius with 8s of 20% Weaken and 20% Slowness. Cooldown: 20s.");
		mInfo.mDescriptions.add("The Weaken debuff is increased to 40%.");
		mInfo.mCooldown = SMOKESCREEN_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.DEAD_TUBE_CORAL, 1);
		mWeakenEffect = getAbilityScore() == 1 ? WEAKEN_EFFECT_1 : WEAKEN_EFFECT_2;
	}

	@Override
	public void cast(Action action) {
		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 750, 4.5, 0.8, 4.5, 0.05);
		world.spawnParticle(Particle.SMOKE_NORMAL, loc, 1500, 4.5, 0.2, 4.5, 0.1);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.35f);
		for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, SMOKESCREEN_RANGE, mPlayer)) {
			EntityUtils.applySlow(mPlugin, SMOKESCREEN_DURATION, SMOKESCREEN_SLOWNESS_AMPLIFIER, mob);
			EntityUtils.applyWeaken(mPlugin, SMOKESCREEN_DURATION, mWeakenEffect, mob);
		}
		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		if (mPlayer.isSneaking() && mPlayer.getLocation().getPitch() > 50) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
			return InventoryUtils.rogueTriggerCheck(mainHand, offHand);
		}
		return false;
	}

}
