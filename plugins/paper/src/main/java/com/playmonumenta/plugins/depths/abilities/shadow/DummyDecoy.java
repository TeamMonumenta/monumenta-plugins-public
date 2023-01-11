package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.abilities.DummyDecoyBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.aspects.BowAspect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import java.util.Objects;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
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

public class DummyDecoy extends DepthsAbility {

	public static final String ABILITY_NAME = "Dummy Decoy";

	public static final int COOLDOWN = 25 * 20;
	public static final String DUMMY_NAME = "AlluringShadow";
	public static final int[] HEALTH = {40, 50, 60, 70, 80, 150};
	public static final int[] STUN_TICKS = {20, 25, 30, 35, 40, 60};
	public static final int MAX_TICKS = 10 * 20;
	public static final int AGGRO_RADIUS = 8;
	public static final int STUN_RADIUS = 4;
	public static final String DUMMY_DECOY_ARROW_METADATA = "DummyDecoyArrow";

	public static final DepthsAbilityInfo<DummyDecoy> INFO =
		new DepthsAbilityInfo<>(DummyDecoy.class, ABILITY_NAME, DummyDecoy::new, DepthsTree.SHADOWDANCER, DepthsTrigger.SHIFT_BOW)
			.linkedSpell(ClassAbility.DUMMY_DECOY)
			.cooldown(COOLDOWN)
			.displayItem(new ItemStack(Material.ARMOR_STAND))
			.descriptions(DummyDecoy::getDescription, MAX_RARITY);

	public DummyDecoy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
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

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {

				if (mT > MAX_TICKS || !proj.isValid() || !proj.hasMetadata(DUMMY_DECOY_ARROW_METADATA)) {
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
		EntityUtils.setAttributeBase(e, Attribute.GENERIC_MAX_HEALTH, HEALTH[mRarity - 1]);
		e.setHealth(HEALTH[mRarity - 1]);

		BossManager bossManager = BossManager.getInstance();
		if (bossManager != null) {
			List<BossAbilityGroup> abilities = bossManager.getAbilities(e);
			for (BossAbilityGroup ability : abilities) {
				if (ability instanceof DummyDecoyBoss dummyDecoyBoss) {
					dummyDecoyBoss.spawn(STUN_TICKS[mRarity - 1]);
					break;
				}
			}
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				if (e.isValid() && !e.isDead()) {
					DamageUtils.damage(null, e, DamageType.OTHER, 10000);
				}
			}
		}.runTaskLater(mPlugin, MAX_TICKS);
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

	private static String getDescription(int rarity) {
		return "Shooting a projectile while sneaking fires a cursed projectile. When the projectile lands, it spawns a dummy decoy at that location with " + DepthsUtils.getRarityColor(rarity) + HEALTH[rarity - 1] + ChatColor.WHITE + " health that lasts for up to " + MAX_TICKS / 20 + " seconds. The decoy aggros mobs within " + AGGRO_RADIUS + " blocks on a regular interval. On death, the decoy explodes, stunning mobs in a " + STUN_RADIUS + " block radius for " + DepthsUtils.getRarityColor(rarity) + DepthsUtils.roundDouble(STUN_TICKS[rarity - 1] / 20.0) + ChatColor.WHITE + " seconds. Cooldown: " + COOLDOWN / 20 + "s.";
	}


}

