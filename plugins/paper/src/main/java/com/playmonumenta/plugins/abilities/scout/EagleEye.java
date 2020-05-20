package com.playmonumenta.plugins.abilities.scout;


import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class EagleEye extends Ability {

	private static final int EAGLE_EYE_EFFECT_LVL = 0;
	private static final int EAGLE_EYE_DURATION = 10 * 20;
	private static final int EAGLE_EYE_COOLDOWN = 24 * 20;
	private static final int EAGLE_EYE_1_VULN_LEVEL = 3; // 20%
	private static final int EAGLE_EYE_2_VULN_LEVEL = 6; // 35%
	private static final int EAGLE_EYE_RADIUS = 20;

	public EagleEye(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Eagle Eye");
		mInfo.linkedSpell = Spells.EAGLE_EYE;
		mInfo.scoreboardId = "Tinkering"; // lmao
		mInfo.mShorthandName = "EE";
		mInfo.mDescriptions.add("When you left-click while sneaking you reveal all enemies in a 20 block range in front of you, giving them the glowing effect for 10 seconds. Affected enemies have 20% Vulnerability. If a mob under the effect of Eagle Eye dies the cooldown of Eagle Eye is reduced by 2 seconds. This skill can not be activated if you have a pickaxe in your mainhand. Cooldown 24s.");
		mInfo.mDescriptions.add("The effect is increased to 35% Vulnerability.");
		mInfo.cooldown = EAGLE_EYE_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
	}


	@Override
	public void cast(Action action) {
		Player player = mPlayer;
		int eagleEye = getAbilityScore();
		World world = player.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 1.25f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.25f);

		for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), EAGLE_EYE_RADIUS, mPlayer)) {
			// Don't apply vulnerability to arena mobs
			if (mob.getScoreboardTags().contains("arena_mob")) {
				continue;
			}

			PotionUtils.applyPotion(mPlayer, mob,
			                        new PotionEffect(PotionEffectType.GLOWING, EAGLE_EYE_DURATION, EAGLE_EYE_EFFECT_LVL, true, false));

			int eagleLevel = (eagleEye == 1) ? EAGLE_EYE_1_VULN_LEVEL : EAGLE_EYE_2_VULN_LEVEL;
			PotionUtils.applyPotion(mPlayer, mob,
			                        new PotionEffect(PotionEffectType.UNLUCK, EAGLE_EYE_DURATION, eagleLevel, true, false));

			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					mTicks++;
					if (mob.isDead() || !mob.isValid()) {
						mPlugin.mTimers.updateCooldown(mPlayer, Spells.EAGLE_EYE, 20 * 2);
						this.cancel();
					}
					if (mTicks >= EAGLE_EYE_DURATION) {
						this.cancel();
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
			world.playSound(mob.getLocation(), Sound.ENTITY_PARROT_IMITATE_SHULKER, 0.4f, 0.7f);
			world.spawnParticle(Particle.FIREWORKS_SPARK, mob.getLocation().add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
		}

		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking() && !InventoryUtils.isPickaxeItem(mPlayer.getInventory().getItemInMainHand());
	}

}
