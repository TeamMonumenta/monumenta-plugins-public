package com.playmonumenta.plugins.depths.abilities.flamecaller;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import net.md_5.bungee.api.ChatColor;

public class Apocalypse extends DepthsAbility {
	public static final String ABILITY_NAME = "Apocalypse";
	public static final int COOLDOWN = 75 * 20;
	private static final double TRIGGER_HEALTH = 0.25;
	public static final int[] DAMAGE = {40, 50, 60, 70, 80, 100};
	public static final int RADIUS = 5;
	public static final double HEALING = 0.1; //percent health per kill

	public Apocalypse(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.ORANGE_DYE;
		mTree = DepthsTree.FLAMECALLER;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.APOCALYPSE;
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		if (event.isCancelled()) {
			return true;
		}

		execute(event);
		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		if (event.isCancelled()) {
			return true;
		}

		execute(event);
		return true;
	}

	/*
	 * Works against all types of damage
	 */
	@Override
	public boolean playerDamagedEvent(EntityDamageEvent event) {
		// Do not process cancelled damage events
		if (event.isCancelled() || event instanceof EntityDamageByEntityEvent) {
			return true;
		}

		execute(event);
		return true;
	}

	private void execute(EntityDamageEvent event) {
		if (AbilityUtils.isBlocked(event)) {
			return;
		}

		// Calculate whether this effect should not be run based on player health.
		double healthRemaining = mPlayer.getHealth() + AbsorptionUtils.getAbsorption(mPlayer) - EntityUtils.getRealFinalDamage(event);

		AttributeInstance maxHealth = mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH);
		if (healthRemaining > maxHealth.getValue() * TRIGGER_HEALTH) {
			return;
		}

		putOnCooldown();

		Location loc = mPlayer.getLocation();
		List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(loc, RADIUS);
		int count = 0;
		for (LivingEntity mob : nearbyMobs) {
			EntityUtils.damageEntity(mPlugin, mob, DAMAGE[mRarity - 1], mPlayer, MagicType.FIRE, true, mInfo.mLinkedSpell, false, false, true);
			if (mob == null || mob.isDead() || mob.getHealth() <= 0) {
				count++;
			}
		}

		if (maxHealth != null) {
			PlayerUtils.healPlayer(mPlayer, maxHealth.getValue() * count * HEALING);
		}

		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.EXPLOSION_HUGE, loc, 10, 2, 2, 2);
		world.spawnParticle(Particle.FLAME, loc, 100, 3.5, 3.5, 3.5, 0);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.5f);

		world.spawnParticle(Particle.HEART, loc.clone().add(0, 1, 0), count * 7, 0.5, 0.5, 0.5);
		world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f * count, 1.6f);
		MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Apocalypse has been activated!");
	}

	@Override
	public String getDescription(int rarity) {
		return "When your health drops below " + (int) DepthsUtils.roundPercent(TRIGGER_HEALTH) + "%, deal " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " damage in a " + RADIUS + " block radius. For each mob that is killed, heal " + (int) DepthsUtils.roundPercent(HEALING) + "% of your max health. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FLAMECALLER;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.LIFELINE;
	}
}
