package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.NegateDamage;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
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

public class DefensiveLine extends Ability {

	private static final String PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "DefensiveLinePercentDamageReceivedEffect";
	private static final String NEGATE_DAMAGE_EFFECT_NAME = "DefensiveLineNegateDamageEffect";
	private static final double PERCENT_DAMAGE_RECEIVED_EFFECT_1 = -0.20;
	private static final double PERCENT_DAMAGE_RECEIVED_EFFECT_2 = -0.30;
	private static final int DURATION = 20 * 10;
	private static final int COOLDOWN = 20 * 30;
	private static final int RADIUS = 8;
	private static final int KNOCK_AWAY_RADIUS = 3;
	private static final float KNOCK_AWAY_SPEED = 0.25f;

	public static final String CHARM_REDUCTION = "Defensive Line Resistance";
	public static final String CHARM_DURATION = "Defensive Line Duration";
	public static final String CHARM_COOLDOWN = "Defensive Line Cooldown";
	public static final String CHARM_RADIUS = "Defensive Line Range";
	public static final String CHARM_KNOCKBACK = "Defensive Line Knockback";
	public static final String CHARM_NEGATIONS = "Defensive Line Damage Negation";

	private final double mPercentDamageReceived;

	public DefensiveLine(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Defensive Line");
		mInfo.mLinkedSpell = ClassAbility.DEFENSIVE_LINE;
		mInfo.mScoreboardId = "DefensiveLine";
		mInfo.mShorthandName = "DL";
		mInfo.mDescriptions.add("When you block while sneaking, you and your allies in an 8 block radius gain 20% Resistance for 10 seconds. Upon activating this skill mobs in a 3 block radius of you and your allies are knocked back. Cooldown: 30s.");
		mInfo.mDescriptions.add("The effect is increased to 30% Resistance.");
		mInfo.mDescriptions.add("Additionally, all affected players negate the next melee attack dealt to them within the duration.");
		mInfo.mCooldown = CharmManager.getCooldown(mPlayer, CHARM_COOLDOWN, COOLDOWN);
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.CHAIN, 1);
		mPercentDamageReceived = (isLevelOne() ? PERCENT_DAMAGE_RECEIVED_EFFECT_1 : PERCENT_DAMAGE_RECEIVED_EFFECT_2) - CharmManager.getLevelPercentDecimal(mPlayer, CHARM_REDUCTION);
	}

	@Override
	public void cast(Action action) {
		// This timer makes sure that the player actually blocked instead of some other right click interaction
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mPlayer != null && mPlayer.isHandRaised()) {
					World world = mPlayer.getWorld();
					Location location = mPlayer.getLocation();
					world.playSound(location, Sound.BLOCK_ANVIL_PLACE, 1.25f, 1.35f);
					world.playSound(location, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.25f, 1.1f);
					new PartialParticle(Particle.FIREWORKS_SPARK, location, 35, 0.2, 0, 0.2, 0.25).spawnAsPlayerActive(mPlayer);

					int duration = DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION);

					List<Player> players = PlayerUtils.playersInRange(location, CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS), true);
					players.removeIf(player -> player.getScoreboardTags().contains("disable_class"));

					for (Player player : players) {
						Location loc = player.getLocation();
						new PartialParticle(Particle.SPELL_INSTANT, loc.clone().add(0, 1, 0), 35, 0.4, 0.4, 0.4, 0.25).spawnAsPlayerActive(mPlayer);

						mPlugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_RECEIVED_EFFECT_NAME, new PercentDamageReceived(duration, mPercentDamageReceived));
						if (isEnhanced()) {
							mPlugin.mEffectManager.addEffect(player, NEGATE_DAMAGE_EFFECT_NAME, new NegateDamage(duration, (int) (1 + CharmManager.getLevel(mPlayer, CHARM_NEGATIONS)), EnumSet.of(DamageEvent.DamageType.MELEE), new PartialParticle(Particle.FIREWORKS_SPARK, loc, 5, 0, 0, 0, 0.25f)));
						}

						for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, CharmManager.getRadius(mPlayer, CHARM_RADIUS, KNOCK_AWAY_RADIUS), mPlayer)) {
							MovementUtils.knockAway(player, mob, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCK_AWAY_SPEED), true);
						}
					}

					new BukkitRunnable() {
						final List<Player> mPlayers = players;
						final double mRadius = 1.25;
						double mY = 0.15;
						@Override
						public void run() {
							mY += 0.2;

							Iterator<Player> iter = mPlayers.iterator();
							while (iter.hasNext()) {
								Player player = iter.next();

								if (player.isDead() || !player.isOnline()) {
									iter.remove();
								} else {
									Location loc = player.getLocation().add(0, mY, 0);

									new PPCircle(Particle.CRIT_MAGIC, loc, mRadius).count(60).delta(0.1).extra(0.125).spawnAsPlayerBuff(player);
									new PPCircle(Particle.SPELL_INSTANT, loc, mRadius).count(20).spawnAsPlayerBuff(player);
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
}
