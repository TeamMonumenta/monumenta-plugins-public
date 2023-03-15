package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.aspects.BowAspect;
import com.playmonumenta.plugins.depths.abilities.steelsage.RapidFire;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Skyhook extends DepthsAbility {
	public static final String ABILITY_NAME = "Skyhook";
	public static final int[] COOLDOWN = {16 * 20, 14 * 20, 12 * 20, 10 * 20, 8 * 20, 4 * 20};
	public static final int MAX_TICKS = 20 * 20;
	public static final String SKYHOOK_ARROW_METADATA = "SkyhookArrow";

	public static final DepthsAbilityInfo<Skyhook> INFO =
		new DepthsAbilityInfo<>(Skyhook.class, ABILITY_NAME, Skyhook::new, DepthsTree.WINDWALKER, DepthsTrigger.SHIFT_BOW)
			.linkedSpell(ClassAbility.SKYHOOK)
			.cooldown(COOLDOWN)
			.displayItem(new ItemStack(Material.FISHING_ROD))
			.descriptions(Skyhook::getDescription);

	public Skyhook(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		Entity damager = event.getDamager();
		if (event.getType() == DamageType.PROJECTILE && damager != null && damager.hasMetadata(SKYHOOK_ARROW_METADATA)) {
			hook(damager);
			damager.removeMetadata(SKYHOOK_ARROW_METADATA, mPlugin);
		}
		return false; // prevents multiple calls itself
	}

	// Since Snowballs disappear after landing, we need an extra detection for when it hits the ground.
	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (proj instanceof Snowball && proj.hasMetadata(SKYHOOK_ARROW_METADATA)) {
			hook(proj);
			proj.removeMetadata(SKYHOOK_ARROW_METADATA, mPlugin);
		}
	}

	private void hook(Entity projectile) {
		Location loc = projectile.getLocation();
		Location playerStartLoc = mPlayer.getLocation();
		World world = mPlayer.getWorld();

		if (LocationUtils.blinkCollisionCheck(mPlayer, loc.toVector())) {
			loc = mPlayer.getLocation();
			Location pLoc = loc.clone().add(0, 0.5, 0);
			Vector dir = playerStartLoc.toVector().subtract(loc.toVector()).normalize();
			double distanceTraveled = playerStartLoc.distance(loc);
			for (int i = 0; i <= distanceTraveled; i++) {
				pLoc.add(dir);

				new PartialParticle(Particle.SWEEP_ATTACK, pLoc, 5, 0.25, 0.25, 0.25, 0).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.CLOUD, pLoc, 10, 0.05, 0.05, 0.05, 0.05).spawnAsPlayerActive(mPlayer);
			}

			world.playSound(playerStartLoc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1, 1.5f);
			new PartialParticle(Particle.SMOKE_LARGE, playerStartLoc, 10, .5, .2, .5, 0.65).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CLOUD, loc, 10, .5, .2, .5, 0.65).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SWEEP_ATTACK, loc, 5, .5, .2, .5, 0.65).spawnAsPlayerActive(mPlayer);

			//Refund cooldowns
			for (Ability ability : AbilityManager.getManager().getPlayerAbilities(mPlayer).getAbilities()) {
				AbilityInfo<?> info = ability.getInfo();
				ClassAbility spell = info.getLinkedSpell();
				if (spell == null || spell == mInfo.getLinkedSpell()) {
					continue;
				}
				int totalCD = ability.getModifiedCooldown();
				int reducedCD = (int) (totalCD * distanceTraveled / 100.0);
				mPlugin.mTimers.updateCooldown(mPlayer, spell, reducedCD);
			}
		}
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1, 1.5f);

		projectile.remove();
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (isOnCooldown()
			    || projectile.hasMetadata(RapidFire.META_DATA_TAG)
			    || !mPlayer.isSneaking()
			    || !EntityUtils.isAbilityTriggeringProjectile(projectile, false)) {
			return true;
		}
		putOnCooldown((int) (getModifiedCooldown() * BowAspect.getCooldownReduction(mPlayer)));
		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		world.playSound(loc, Sound.ITEM_CROSSBOW_QUICK_CHARGE_3, SoundCategory.PLAYERS, 1, 1.0f);

		if (projectile instanceof AbstractArrow arrow) {
			arrow.setPierceLevel(0);
			arrow.setCritical(true);
			arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
		}
		projectile.setMetadata(SKYHOOK_ARROW_METADATA, new FixedMetadataValue(mPlugin, 0));

		mPlugin.mProjectileEffectTimers.addEntity(projectile, Particle.FIREWORKS_SPARK);

		cancelOnDeath(new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT > MAX_TICKS) {
					mPlugin.mProjectileEffectTimers.removeEntity(projectile);
					projectile.removeMetadata(SKYHOOK_ARROW_METADATA, mPlugin);
					projectile.remove();
					this.cancel();
				}

				if (projectile.getVelocity().length() < .05 || projectile.isOnGround()) {
					hook(projectile);
					this.cancel();
				}
				mT++;
			}

		}.runTaskTimer(mPlugin, 0, 1));

		return true;
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Shooting a projectile while sneaking shoots out a skyhook. When the skyhook lands, you dash to the location and reduce all other ability cooldowns by 1% per block traveled. Cooldown: ")
			.append(Component.text(COOLDOWN[rarity - 1] / 20 + "s", color))
			.append(Component.text("."));
	}


}
