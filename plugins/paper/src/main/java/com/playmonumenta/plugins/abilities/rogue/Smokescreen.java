package com.playmonumenta.plugins.abilities.rogue;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class Smokescreen extends Ability {

	private static final int SMOKESCREEN_RANGE = 7;
	private static final int SMOKESCREEN_DURATION = 8 * 20;
	private static final int SMOKESCREEN_1_SLOWNESS_AMPLIFIER = 1;
	private static final int SMOKESCREEN_2_SLOWNESS_AMPLIFIER = 2;
	private static final int SMOKESCREEN_1_WEAKNESS_AMPLIFIER = 0;
	private static final int SMOKESCREEN_2_WEAKNESS_AMPLIFIER = 1;
	private static final int SMOKESCREEN_COOLDOWN = 20 * 20;

	private int mSlownessAmplifier;
	private int mWeaknessAmplifier;

	public Smokescreen(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Smoke Screen");
		mInfo.linkedSpell = Spells.SMOKESCREEN;
		mInfo.scoreboardId = "SmokeScreen";
		mInfo.mShorthandName = "Smk";
		mInfo.mDescriptions.add("When holding two swords, right-click while sneaking and looking down to release a cloud of smoke, afflicting all enemies in a 6 block radius with 8 s of Weakness I and Slowness II. (Cooldown: 20 s)");
		mInfo.mDescriptions.add("The Weakness debuff is increased to level II.");
		mInfo.cooldown = SMOKESCREEN_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
		mSlownessAmplifier = getAbilityScore() == 1 ? SMOKESCREEN_1_SLOWNESS_AMPLIFIER : SMOKESCREEN_2_SLOWNESS_AMPLIFIER;
		mSlownessAmplifier = getAbilityScore() == 1 ? SMOKESCREEN_1_WEAKNESS_AMPLIFIER : SMOKESCREEN_2_WEAKNESS_AMPLIFIER;
	}

	@Override
	public void cast(Action action) {
		Location loc = mPlayer.getLocation();
		mWorld.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 300, 2.5, 0.8, 2.5, 0.05);
		mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 600, 2.5, 0.2, 2.5, 0.1);
		mWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.35f);
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), SMOKESCREEN_RANGE, mPlayer)) {
			PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, SMOKESCREEN_DURATION, mSlownessAmplifier, false, true));
			PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.WEAKNESS, SMOKESCREEN_DURATION, mWeaknessAmplifier, false, true));
		}
		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		if (mPlayer.isSneaking() && mPlayer.getLocation().getPitch() > 50) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
			return InventoryUtils.isSwordItem(mainHand) && InventoryUtils.isSwordItem(offHand);
		}
		return false;
	}

}
