package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.Collection;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

public class DepthsDodging extends DepthsAbility {

	public static final String ABILITY_NAME = "Dodging";
	public static final int[] COOLDOWN = {20 * 16, 20 * 14, 20 * 12, 20 * 10, 20 * 8, 20 * 5};

	public static final DepthsAbilityInfo<DepthsDodging> INFO =
		new DepthsAbilityInfo<>(DepthsDodging.class, ABILITY_NAME, DepthsDodging::new, DepthsTree.WINDWALKER, DepthsTrigger.PASSIVE)
			.linkedSpell(ClassAbility.DODGING)
			.cooldown(COOLDOWN)
			.displayItem(new ItemStack(Material.COBWEB))
			.descriptions(DepthsDodging::getDescription, MAX_RARITY);

	private int mTriggerTick = 0;

	public DepthsDodging(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean playerCombustByEntityEvent(EntityCombustByEntityEvent event) {
		// Don't proc on Fire Aspect
		if (!(event.getCombuster() instanceof Projectile)) {
			return true;
		}

		// See if we should dodge. If false, allow the event to proceed normally
		if (!dodge()) {
			return true;
		}

		event.setDuration(0);
		return false;
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		// See if we should dodge. If false, allow the event to proceed normally
		if (event.getType() == DamageType.PROJECTILE && !event.isBlocked() && dodge()) {
			event.setCancelled(true);
		}
	}

	@Override
	public boolean playerHitByProjectileEvent(ProjectileHitEvent event) {
		Projectile proj = event.getEntity();
		// See if we should dodge. If false, allow the event to proceed normally
		// This probably doesn't properly check for blocking whereas the other method does
		if (proj.getShooter() instanceof Player) {
			return true;
		}
		if (mPlayer.getActiveItem() != null && mPlayer.getActiveItem().getType() == Material.SHIELD) {
			return true;
		}
		if (!dodge()) {
			return true;
		}

		if (proj instanceof Arrow arrow) {
			arrow.setBasePotionData(new PotionData(PotionType.MUNDANE));
			arrow.clearCustomEffects();
		}
		proj.remove();
		return true;
	}

	@Override
	public boolean playerSplashedByPotionEvent(Collection<LivingEntity> affectedEntities,
	                                           ThrownPotion potion, PotionSplashEvent event) {
		if (!(event.getEntity().getShooter() instanceof LivingEntity) || event.getEntity().getShooter() instanceof Player) {
			return true;
		}
		return !dodge();
	}

	private boolean dodge() {
		if (mTriggerTick == mPlayer.getTicksLived()) {
			// Dodging was activated this tick - allow it
			return true;
		}

		/*
		 * Must check with cooldown timers directly because isAbilityOnCooldown always returns
		 * false (because ignoreCooldown is true)
		 */
		if (isOnCooldown()) {
			/*
			 * This ability is actually on cooldown (and was not triggered this tick)
			 * Don't process dodging
			 */
			return false;

		}

		/*
		 * Make note of which tick this triggered on so that any other event that triggers this
		 * tick will also be dodged
		 */
		mTriggerTick = mPlayer.getTicksLived();
		putOnCooldown();

		Location loc = mPlayer.getLocation().add(0, 1, 0);
		World world = mPlayer.getWorld();
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 90, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 2f);
		return true;
	}

	private static String getDescription(int rarity) {
		return "You dodge the next projectile or potion attack that would have hit you, nullifying the damage. Cooldown: " + DepthsUtils.getRarityColor(rarity) + COOLDOWN[rarity - 1] / 20 + "s" + ChatColor.WHITE + ".";
	}
}

