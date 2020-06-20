package com.playmonumenta.plugins.abilities.warrior;

import java.util.Iterator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
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
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class DefensiveLine extends Ability {

	private static final Integer DEFENSIVE_LINE_DURATION = 14 * 20;
	private static final float DEFENSIVE_LINE_RADIUS = 8.0f;
	private static final Integer DEFENSIVE_LINE_1_COOLDOWN = 50 * 20;
	private static final Integer DEFENSIVE_LINE_2_COOLDOWN = 30 * 20;

	public DefensiveLine(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Defensive Line");
		mInfo.mLinkedSpell = Spells.DEFENSIVE_LINE;
		mInfo.mScoreboardId = "DefensiveLine";
		mInfo.mShorthandName = "DL";
		mInfo.mDescriptions.add("When you block while sneaking, you and your allies in an 8 block radius gain Resistance II for 14 seconds. Upon activating this skill mobs in a 3 block radius of you and your allies are knocked back. Cooldown: 50 seconds.");
		mInfo.mDescriptions.add("The cooldown is decreased to 30 seconds. In addition mobs that are knocked back are given 10 seconds of Weakness 1.");
		// NOTE: getAbilityScore() can only be used after the scoreboardId is set!
		mInfo.mCooldown = getAbilityScore() == 1 ? DEFENSIVE_LINE_1_COOLDOWN : DEFENSIVE_LINE_2_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast(Action action) {
		// This timer makes sure that the player actually blocked instead of some other right click interaction
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mPlayer.isHandRaised()) {
					mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.25f, 1.35f);
					mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.25f, 1.1f);
					mWorld.spawnParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation(), 35, 0.2, 0, 0.2, 0.25);

					List<Player> players = PlayerUtils.playersInRange(mPlayer, DEFENSIVE_LINE_RADIUS, true);

					for (Player player : players) {
						// Don't buff players that have their class disabled
						if (player.getScoreboardTags().contains("disable_class")) {
							continue;
						}

						Location loc = player.getLocation().add(0, 1, 0);
						mWorld.spawnParticle(Particle.SPELL_INSTANT, loc, 35, 0.4, 0.4, 0.4, 0.25);

						mPlugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION,
						                                 new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,
						                                                  DEFENSIVE_LINE_DURATION,
						                                                  1, true, true));
						for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, 3, mPlayer)) {
							MovementUtils.knockAway(player, mob, 0.25f);
							if (getAbilityScore() > 1) {
								PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.WEAKNESS, 20 * 10, 0, false, true));
							}
						}
					}

					new BukkitRunnable() {
						final List<Player> mPlayers = players;
						final double mRadius = 1.25;
						double mY = 0.15;
						@Override
						public void run() {
							mY += 0.2;

							// Store calculations instead of doing them again for each player
							double[] dx = new double[20];
							double[] dz = new double[20];
							for (int i = 0; i < 20; i++) {
								double radians = Math.toRadians(i * 18);
								dx[i] = Math.cos(radians) * mRadius;
								dz[i] = Math.sin(radians) * mRadius;
							}

							Iterator<Player> iter = mPlayers.iterator();
							while (iter.hasNext()) {
								Player player = iter.next();

								if (player.isDead() || !player.isOnline()) {
									iter.remove();
								} else {
									Location loc = player.getLocation().add(0, mY, 0);

									for (int i = 0; i < 20; i++) {
										loc.add(dx[i], 0, dz[i]);
										mWorld.spawnParticle(Particle.CRIT_MAGIC, loc, 3, 0.1, 0.1, 0.1, 0.125);
										mWorld.spawnParticle(Particle.SPELL_INSTANT, loc, 1, 0, 0, 0, 0);
										loc.subtract(dx[i], 0, dz[i]);
									}
								}
							}

							if (mY >= 1.8) {
								this.cancel();
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);

					putOnCooldown();
				}
				this.cancel();
			}
		}.runTaskLater(mPlugin, 1);
	}

	@Override
	public boolean runCheck() {
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSneaking()
		       && ((mainHand.getType() == Material.SHIELD && offHand.getType() != Material.BOW)
		           || (offHand.getType() == Material.SHIELD && mainHand.getType() != Material.BOW));
	}
}
