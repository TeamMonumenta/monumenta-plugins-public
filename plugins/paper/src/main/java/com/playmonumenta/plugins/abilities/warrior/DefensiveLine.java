package com.playmonumenta.plugins.abilities.warrior;

import java.util.EnumSet;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.abilities.BaseAbilityEnchantment;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class DefensiveLine extends Ability {
	public static class DefensiveLineCooldownEnchantment extends BaseAbilityEnchantment {
		public DefensiveLineCooldownEnchantment() {
			super("Defensive Line Cooldown", EnumSet.of(ItemSlot.ARMOR));
		}
	}

	private static final String PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "DefensiveLinePercentDamageReceivedEffect";
	private static final double PERCENT_DAMAGE_RECEIVED_EFFECT_1 = -0.20;
	private static final double PERCENT_DAMAGE_RECEIVED_EFFECT_2 = -0.30;
	private static final int DURATION = 20 * 10;

	private static final int COOLDOWN = 20 * 30;
	private static final int RADIUS = 8;
	private static final int KNOCK_AWAY_RADIUS = 3;
	private static final float KNOCK_AWAY_SPEED = 0.25f;

	private final double mPercentDamageReceived;

	public DefensiveLine(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Defensive Line");
		mInfo.mLinkedSpell = ClassAbility.DEFENSIVE_LINE;
		mInfo.mScoreboardId = "DefensiveLine";
		mInfo.mShorthandName = "DL";
		mInfo.mDescriptions.add("When you block while sneaking, you and your allies in an 8 block radius gain 20% Resistance for 10 seconds. Upon activating this skill mobs in a 3 block radius of you and your allies are knocked back. Cooldown: 30s.");
		mInfo.mDescriptions.add("The effect is increased to 30% Resistance.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.CHAIN, 1);
		mPercentDamageReceived = getAbilityScore() == 1 ? PERCENT_DAMAGE_RECEIVED_EFFECT_1 : PERCENT_DAMAGE_RECEIVED_EFFECT_2;
	}

	@Override
	public void cast(Action action) {
		// This timer makes sure that the player actually blocked instead of some other right click interaction
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mPlayer != null && mPlayer.isHandRaised()) {
					World world = mPlayer.getWorld();
					world.playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.25f, 1.35f);
					world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.25f, 1.1f);
					world.spawnParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation(), 35, 0.2, 0, 0.2, 0.25);

					List<Player> players = PlayerUtils.playersInRange(mPlayer.getLocation(), RADIUS, true);

					for (Player player : players) {
						// Don't buff players that have their class disabled
						if (player.getScoreboardTags().contains("disable_class")) {
							continue;
						}

						Location loc = player.getLocation().add(0, 1, 0);
						world.spawnParticle(Particle.SPELL_INSTANT, loc, 35, 0.4, 0.4, 0.4, 0.25);

						mPlugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_RECEIVED_EFFECT_NAME, new PercentDamageReceived(DURATION, mPercentDamageReceived));

						for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, KNOCK_AWAY_RADIUS, mPlayer)) {
							MovementUtils.knockAway(player, mob, KNOCK_AWAY_SPEED);
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
								dx[i] = FastUtils.cos(radians) * mRadius;
								dz[i] = FastUtils.sin(radians) * mRadius;
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
										world.spawnParticle(Particle.CRIT_MAGIC, loc, 3, 0.1, 0.1, 0.1, 0.125);
										world.spawnParticle(Particle.SPELL_INSTANT, loc, 1, 0, 0, 0, 0);
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
			}
		}.runTaskLater(mPlugin, 1);
	}

	@Override
	public boolean runCheck() {
		if (mPlayer == null) {
			return false;
		}
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSneaking()
			&& !ItemUtils.isSomeBow(mainHand)
			&& (mainHand.getType() == Material.SHIELD || offHand.getType() == Material.SHIELD);
	}

	@Override
	public Class<? extends BaseAbilityEnchantment> getCooldownEnchantment() {
		return DefensiveLineCooldownEnchantment.class;
	}
}
