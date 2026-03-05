package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.TotemicProjectionCS;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class TotemicProjection extends MultipleChargeAbility {
	private static final int COOLDOWN = 5 * 20;
	private static final int MAX_CHARGES = 2;
	private static final double DISTRIBUTION_RADIUS = 3;

	public static final String CHARM_COOLDOWN = "Totemic Projection Cooldown";
	public static final String CHARM_CHARGES = "Totemic Projection Charges";
	public static final String CHARM_DISTRIBUTION_RADIUS = "Totemic Projection Distribution Radius";

	public static final AbilityInfo<TotemicProjection> INFO =
		new AbilityInfo<>(TotemicProjection.class, "Totemic Projection", TotemicProjection::new)
			.linkedSpell(ClassAbility.TOTEMIC_PROJECTION)
			.scoreboardId("TotemicProjection")
			.shorthandName("TP")
			.description(getDescription())
			.canUse(player -> AbilityUtils.getClassNum(player) == Shaman.CLASS_ID)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", TotemicProjection::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).doubleClick().enabled(false)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE, AbilityTrigger.KeyOptions.NO_BLOCKS, AbilityTrigger.KeyOptions.NO_POTION, AbilityTrigger.KeyOptions.NO_FOOD)))
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.ENDER_PEARL);

	public final double mDistributionRadius;
	public final TotemicProjectionCS mCosmetic;

	private final Map<ThrowableProjectile, ItemStatManager.PlayerItemStats> mProjectionProjectiles = new WeakHashMap<>();

	public TotemicProjection(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mMaxCharges = MAX_CHARGES + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mCharges = getChargesOffCooldown();
		mDistributionRadius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DISTRIBUTION_RADIUS, DISTRIBUTION_RADIUS);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new TotemicProjectionCS());
	}

	public boolean cast() {
		if (ShamanPassiveManager.getTotemList(mPlayer).isEmpty() || !useCharge()) {
			return false;
		}

		ThrowableProjectile proj = AbilityUtils.spawnAbilitySnowball(mPlugin, mPlayer, mPlayer.getWorld(), TotemAbility.VELOCITY, "Totemic Projection Projectile", null);
		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
		mProjectionProjectiles.put(proj, playerItemStats);
		int cd = getModifiedCooldown();

		new BukkitRunnable() {
			int mT = 0;
			final Location mPlayerLocation = mPlayer.getLocation();
			@Override
			public void run() {
				if (mProjectionProjectiles.get(proj) != playerItemStats) {
					this.cancel();
				}

				Location projLoc = proj.getLocation();
				projLoc.setY(mPlayer.getLocation().getY());
				if (mT >= TotemAbility.TIME_TO_DROP
					|| projLoc.distance(mPlayerLocation) >= TotemAbility.XZ_DISTANCE_TO_DROP) {
					proj.setVelocity(new Vector(0, -2, 0));
				}


				if (cd <= 0) {
					proj.remove();
					this.cancel();
				}

				if (proj.isDead()) {
					if (mProjectionProjectiles.remove(proj) != null) {
						moveTotems(proj.getLocation());
					}
					this.cancel();
				}
				mT++;
			}

			@Override
			public synchronized void cancel() {
				proj.remove();
				super.cancel();
			}
		}.runTaskTimer(mPlugin, 0, 1);
		return true;
	}

	private void moveTotems(Location targetLoc) {
		List<LivingEntity> totems = ShamanPassiveManager.getTotemList(mPlayer);

		// Zig's old code for projection, hopefully I can just copy and paste here, and it all works ok!
		if (totems.isEmpty()) {
			return;
		} else if (totems.size() == 1) {
			LivingEntity totem = totems.getFirst();
			Location loc = targetLoc.clone().add(0, 0.05, 0);
			if (loc.getBlock().isPassable()) {
				totem.teleport(loc);
			} else {
				totem.teleport(targetLoc);
			}
		} else {
			Vector forward = targetLoc.getDirection().setY(0).normalize().multiply(mDistributionRadius);
			int currentDeg;
			switch (totems.size()) {
				case 2 -> currentDeg = -90;
				case 3 -> currentDeg = -60;
				default -> currentDeg = -45;
			}
			int degIncrement = 360 / totems.size();
			for (LivingEntity totem : totems) {
				Vector dir = VectorUtils.rotateYAxis(forward, currentDeg);
				Location locLower = targetLoc.clone().add(dir);
				Location loc = locLower.clone().add(0, 0.05, 0);
				if (loc.getBlock().isPassable()) {
					totem.teleport(loc);
				} else if (locLower.getBlock().isPassable()) {
					totem.teleport(locLower);
				} else {
					totem.teleport(targetLoc);
				}
				currentDeg += degIncrement;
			}
		}

		mCosmetic.projectionCollision(mPlayer, targetLoc);
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if ((proj instanceof Snowball || proj instanceof Trident)
			&& proj.getTicksLived() <= 160 && mProjectionProjectiles.containsKey(proj)) {
			ItemStatManager.PlayerItemStats stats = mProjectionProjectiles.remove(proj);
			if (!mPlayer.getWorld().equals(proj.getWorld()) || mPlayer.getLocation().distance(proj.getLocation()) >= 50) {
				return;
			}
			if (stats != null) {
				TotemicProjection projSkill = AbilityManager.getManager().getPlayerAbility(mPlayer, TotemicProjection.class);
				if (projSkill != null) {
					moveTotems(proj.getLocation());
				}
			}
		}
	}

	public boolean useCharge() {
		return consumeCharge();
	}

	@Override
	public int getAbilityScore(TotemicProjection this) {
		return 1;
	}

	public static Description<TotemicProjection> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO)
			.addLine("Recast a *Totem* ability to fire a projectile").styles(Shaman.TOTEM_COLOR)
			.addLine("that teleports *Totems* to its landing point,").styles(Shaman.TOTEM_COLOR)
			.addLine("with a spread of %d blocks.")
				.statValues(stat(a -> a.mDistributionRadius, DISTRIBUTION_RADIUS))
			.addStat("Charges: %d")
				.statValues(stat(a -> a.mMaxCharges, MAX_CHARGES))
			.addStat("Cooldown: %t")
				.statValues(cooldown(COOLDOWN));
	}
}
