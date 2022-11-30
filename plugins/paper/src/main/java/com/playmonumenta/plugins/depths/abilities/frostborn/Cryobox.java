package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.CrowdControlImmunityBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Cryobox extends DepthsAbility {

	public static final String ABILITY_NAME = "Cryobox";
	public static final int[] ABSORPTION_HEALTH = {8, 10, 12, 14, 16, 20};
	public static final int COOLDOWN = 90 * 20;
	private static final double TRIGGER_HEALTH = 0.25;
	private static final int KNOCKBACK_RADIUS = 4;
	private static final int ELEVATE_RADIUS = 2;
	private static final float KNOCKBACK_SPEED = 0.7f;
	private static final int DURATION = 12 * 20;
	private static final int ICE_DURATION = 15 * 20;

	public static final DepthsAbilityInfo<Cryobox> INFO =
		new DepthsAbilityInfo<>(Cryobox.class, ABILITY_NAME, Cryobox::new, DepthsTree.FROSTBORN, DepthsTrigger.LIFELINE)
			.linkedSpell(ClassAbility.CRYOBOX)
			.cooldown(COOLDOWN)
			.displayItem(new ItemStack(Material.GHAST_TEAR))
			.descriptions(Cryobox::getDescription, MAX_RARITY)
			.priorityAmount(10000);

	public Cryobox(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.isBlocked() || isOnCooldown()) {
			return;
		}

		// Calculate whether this effect should not be run based on player health.
		// It is intentional that Cryobox saves you from death if you take a buttload of damage somehow.
		double healthRemaining = mPlayer.getHealth() - event.getFinalDamage(true);

		// Health is less than 0 but does not penetrate the absorption shield
		boolean dealDamageLater = healthRemaining < 0 && healthRemaining > -ABSORPTION_HEALTH[mRarity - 1];

		if (healthRemaining > EntityUtils.getMaxHealth(mPlayer) * TRIGGER_HEALTH) {
			return;
		} else if (dealDamageLater) {
			// The player has taken fatal damage BUT will be saved by the absorption, so set damage to 0 and compensate later
			event.setCancelled(true);
		}

		// Put on cooldown before processing results to prevent infinite recursion
		putOnCooldown();

		Location center = mPlayer.getLocation();

		// Conditions match - prismatic shield
		for (LivingEntity mob : EntityUtils.getNearbyMobs(center, KNOCKBACK_RADIUS, mPlayer)) {
			MovementUtils.knockAway(mPlayer, mob, KNOCKBACK_SPEED, true);
		}
		for (LivingEntity mob : EntityUtils.getNearbyMobs(center, ELEVATE_RADIUS, mPlayer)) {
			if (EntityUtils.isBoss(mob) || ScoreboardUtils.checkTag(mob, CrowdControlImmunityBoss.identityTag) || ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG)) {
				continue;
			}
			Location mobLoc = mob.getLocation();
			mobLoc.setY(center.getY() + 4);
			mob.teleport(mobLoc);
		}

		AbsorptionUtils.addAbsorption(mPlayer, ABSORPTION_HEALTH[mRarity - 1], ABSORPTION_HEALTH[mRarity - 1], DURATION);
		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.FIREWORKS_SPARK, center.clone().add(0, 1.15, 0), 150, 0.2, 0.35, 0.2, 0.5);
		world.spawnParticle(Particle.SPELL_INSTANT, center.clone().add(0, 1.15, 0), 100, 0.2, 0.35, 0.2, 1);
		world.playSound(center, Sound.ITEM_TOTEM_USE, 1, 1.35f);
		MessagingUtils.sendActionBarMessage(mPlayer, "Cryobox has been activated!");

		if (dealDamageLater) {
			mPlayer.setHealth(1);
			AbsorptionUtils.subtractAbsorption(mPlayer, 1 - (float) healthRemaining);
		}

		//Ripped this straight from frost giant, epic
		Location[] locs = new Location[] {
			//First Layer
			center.clone().add(1, 0, 0),
			center.clone().add(-1, 0, 0),
			center.clone().add(0, 0, 1),
			center.clone().add(0, 0, -1),

			//Second Layer
			center.clone().add(1, 1, 0),
			center.clone().add(-1, 1, 0),
			center.clone().add(0, 1, 1),
			center.clone().add(0, 1, -1),

			//Top & Bottom
			center.clone().add(0, 2, 0),
			center.clone().add(0, -1, 0),
			};

		for (int i = 0; i < locs.length; i++) {
			DepthsUtils.spawnIceTerrain(locs[i], ICE_DURATION, mPlayer);
		}
	}

	@Override
	public void onHurtFatal(DamageEvent event) {
		onHurt(event, null, null);
	}

	private static String getDescription(int rarity) {
		return "When your health drops below " + (int) DepthsUtils.roundPercent(TRIGGER_HEALTH) + "%, gain " + DepthsUtils.getRarityColor(rarity) + (ABSORPTION_HEALTH[rarity - 1] / 2) + ChatColor.WHITE + " absorption hearts for " + DURATION / 20 + " seconds, knock enemies away, and encase yourself in a cage of ice for " + ICE_DURATION / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s.";
	}


}

