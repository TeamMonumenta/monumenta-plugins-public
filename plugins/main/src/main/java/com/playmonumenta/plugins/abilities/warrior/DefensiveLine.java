package com.playmonumenta.plugins.abilities.warrior;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class DefensiveLine extends Ability {

	private static final Integer DEFENSIVE_LINE_DURATION = 14 * 20;
	private static final float DEFENSIVE_LINE_RADIUS = 8.0f;
	private static final Integer DEFENSIVE_LINE_1_COOLDOWN = 50 * 20;
	private static final Integer DEFENSIVE_LINE_2_COOLDOWN = 30 * 20;

	public DefensiveLine(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.DEFENSIVE_LINE;
		mInfo.scoreboardId = "DefensiveLine";
		// NOTE: getAbilityScore() can only be used after the scoreboardId is set!
		mInfo.cooldown = getAbilityScore() == 1 ? DEFENSIVE_LINE_1_COOLDOWN : DEFENSIVE_LINE_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast() {
		// This timer makes sure that the player actually blocked instead of some other right click interaction
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mPlayer.isHandRaised()) {
					for (Player target : PlayerUtils.getNearbyPlayers(mPlayer, DEFENSIVE_LINE_RADIUS, true)) {
						// Don't buff players that have their class disabled
						if (target.getScoreboardTags().contains("disable_class")) {
							continue;
						}

						Location loc = target.getLocation();

						target.playSound(loc, Sound.ITEM_SHIELD_BLOCK, 0.4f, 1.0f);
						mPlugin.mPotionManager.addPotion(target, PotionID.APPLIED_POTION,
						                                 new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,
						                                                  DEFENSIVE_LINE_DURATION,
						                                                  1, true, true));
						for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, 3, mPlayer)) {
							MovementUtils.KnockAway(target, mob, 0.25f);
							if (getAbilityScore() > 1) {
								mob.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 10, 0, false, true));
							}
						}
					}

					ParticleUtils.explodingSphereEffect(mPlugin, mPlayer, DEFENSIVE_LINE_RADIUS, Particle.FIREWORKS_SPARK, 1.0f, Particle.CRIT, 1.0f);
					putOnCooldown();
				}
				this.cancel();
			}
		}.runTaskLater(mPlugin, 1);
	}

	@Override
	public boolean runCheck() {
		//  If we're sneaking and we block with a shield we can attempt to trigger the ability.
		if (mPlayer.isSneaking()) {
			ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (offHand.getType() == Material.SHIELD || mainHand.getType() == Material.SHIELD) {
				return true;
			}
		}
		return false;
	}
}
