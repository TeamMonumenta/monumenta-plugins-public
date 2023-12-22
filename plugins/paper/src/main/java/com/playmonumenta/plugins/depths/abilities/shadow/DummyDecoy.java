package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.bosses.bosses.abilities.DummyDecoyBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.aspects.BowAspect;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Objects;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class DummyDecoy extends DepthsAbility {

	public static final String ABILITY_NAME = "Dummy Decoy";

	public static final int COOLDOWN = 22 * 20;
	public static final String DUMMY_NAME = "AlluringShadow";
	public static final int[] HEALTH = {40, 50, 60, 70, 80, 150};
	public static final int[] STUN_TICKS = {20, 25, 30, 35, 40, 60};
	public static final int MAX_TICKS = 10 * 20;
	public static final int AGGRO_RADIUS = 8;
	public static final int STUN_RADIUS = 4;
	public static final String DUMMY_DECOY_ARROW_METADATA = "DummyDecoyArrow";

	public static final String CHARM_COOLDOWN = "Dummy Decoy Cooldown";

	public static final DepthsAbilityInfo<DummyDecoy> INFO =
		new DepthsAbilityInfo<>(DummyDecoy.class, ABILITY_NAME, DummyDecoy::new, DepthsTree.SHADOWDANCER, DepthsTrigger.SHIFT_BOW)
			.linkedSpell(ClassAbility.DUMMY_DECOY)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.ARMOR_STAND)
			.descriptions(DummyDecoy::getDescription)
			.priorityAmount(949); // Needs to trigger before Rapid Fire

	private final double mHealth;
	private final int mStunDuration;
	private final int mDummyDuration;
	private final double mAggroRadius;
	private final double mStunRadius;

	public DummyDecoy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mHealth = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.DUMMY_DECOY_HEALTH.mEffectName, HEALTH[mRarity - 1]);
		mStunDuration = CharmManager.getDuration(mPlayer, CharmEffects.DUMMY_DECOY_STUN_DURATION.mEffectName, STUN_TICKS[mRarity - 1]);
		mDummyDuration = CharmManager.getDuration(mPlayer, CharmEffects.DUMMY_DECOY_MAX_LIFE_DURATION.mEffectName, MAX_TICKS);
		mAggroRadius = CharmManager.getRadius(mPlayer, CharmEffects.DUMMY_DECOY_AGGRO_RADIUS.mEffectName, AGGRO_RADIUS);
		mStunRadius = CharmManager.getRadius(mPlayer, CharmEffects.DUMMY_DECOY_STUN_RADIUS.mEffectName, STUN_RADIUS);
	}

	public void execute(Projectile proj) {

		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, SoundCategory.NEUTRAL, 1, 1.4f);

		if (proj instanceof AbstractArrow arrow) {
			arrow.setPierceLevel(0);
			arrow.setCritical(true);
			arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
		}

		proj.setMetadata(DUMMY_DECOY_ARROW_METADATA, new FixedMetadataValue(mPlugin, 0));

		mPlugin.mProjectileEffectTimers.addEntity(proj, Particle.SPELL_WITCH);

		int healthTicks = CharmManager.getDuration(mPlayer, CharmEffects.DUMMY_DECOY_MAX_LIFE_DURATION.mEffectName, MAX_TICKS);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {

				if (mT > healthTicks || !proj.isValid() || !proj.hasMetadata(DUMMY_DECOY_ARROW_METADATA)) {
					proj.remove();
					this.cancel();
					return;
				}

				if (proj.getVelocity().length() < .05 || proj.isOnGround()) {
					spawnDecoy(proj, proj.getLocation());
					this.cancel();
					return;
				}
				mT++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		Entity damager = event.getDamager();
		if (event.getType() == DamageType.PROJECTILE && damager instanceof AbstractArrow && damager.isValid() && damager.hasMetadata(DUMMY_DECOY_ARROW_METADATA)) {
			spawnDecoy(damager, enemy.getLocation());
		}
		return false; // prevents multiple calls itself
	}

	// Since Snowballs disappear after landing, we need an extra detection for when it hits the ground.
	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (proj instanceof Snowball && proj.hasMetadata(DUMMY_DECOY_ARROW_METADATA)) {
			spawnDecoy(proj, proj.getLocation());
		}
	}

	private void spawnDecoy(Entity arrow, Location loc) {
		arrow.removeMetadata(DUMMY_DECOY_ARROW_METADATA, mPlugin);
		arrow.remove();

		LivingEntity e = Objects.requireNonNull((LivingEntity) LibraryOfSoulsIntegration.summon(loc, DUMMY_NAME));
		EntityUtils.setMaxHealthAndHealth(e, mHealth * DepthsUtils.getDamageMultiplier());
		DummyDecoyBoss dummyDecoyBoss = mPlugin.mBossManager.getBoss(e, DummyDecoyBoss.class);
		if (dummyDecoyBoss != null) {
			dummyDecoyBoss.spawn(mStunDuration, mAggroRadius, mStunRadius);
		}

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			if (e.isValid() && !e.isDead()) {
				DamageUtils.damage(null, e, DamageType.OTHER, 10000);
			}
		}, mDummyDuration);
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (isOnCooldown()
			    || !mPlayer.isSneaking()
			    || !EntityUtils.isAbilityTriggeringProjectile(projectile, false)) {
			return true;
		}
		putOnCooldown((int) (getModifiedCooldown() * BowAspect.getCooldownReduction(mPlayer)));
		execute(projectile);

		return true;
	}

	private static Description<DummyDecoy> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<DummyDecoy>(color)
			.add("Shooting a projectile while sneaking fires a cursed projectile. When the projectile lands, it spawns a dummy decoy at that location with ")
			.addDepthsDamage(a -> a.mHealth, HEALTH[rarity - 1], true)
			.add(" health that lasts for up to ")
			.addDuration(a -> a.mDummyDuration, MAX_TICKS)
			.add(" seconds. The decoy aggros mobs within ")
			.add(a -> a.mAggroRadius, AGGRO_RADIUS)
			.add(" blocks on a regular interval. On death, the decoy explodes, stunning mobs in a ")
			.add(a -> a.mStunRadius, STUN_RADIUS)
			.add(" block radius for ")
			.addDuration(a -> a.mStunDuration, STUN_TICKS[rarity - 1], false, true)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}
}

