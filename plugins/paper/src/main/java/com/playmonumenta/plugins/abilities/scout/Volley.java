package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.VolleyCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Grappling;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitRunnable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

public class Volley extends Ability {

	private static final int VOLLEY_COOLDOWN = 15 * 20;
	private static final int VOLLEY_1_ARROW_COUNT = 7;
	private static final int VOLLEY_2_ARROW_COUNT = 11;
	private static final double VOLLEY_1_DAMAGE_MULTIPLIER = 1.3;
	private static final double VOLLEY_2_DAMAGE_MULTIPLIER = 1.5;
	public Set<Projectile> mVolley;
	private final Map<LivingEntity, Integer> mVolleyHitMap;

	public static final String CHARM_COOLDOWN = "Volley Cooldown";
	public static final String CHARM_ARROWS = "Volley Arrows";
	public static final String CHARM_DAMAGE = "Volley Damage";
	public static final String CHARM_PIERCING = "Volley Piercing";

	public static final AbilityInfo<Volley> INFO =
		new AbilityInfo<>(Volley.class, "Volley", Volley::new)
			.linkedSpell(ClassAbility.VOLLEY)
			.scoreboardId("Volley")
			.shorthandName("Vly")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Fire a volley of projectiles in front of you.")
			.cooldown(VOLLEY_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.ARROW)
			.priorityAmount(900); // cancels damage events of volley arrows, so needs to run before other abilities

	private final int mArrows;
	private final double mMultiplier;
	private final VolleyCS mCosmetic;

	public Volley(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mArrows = (isLevelOne() ? VOLLEY_1_ARROW_COUNT : VOLLEY_2_ARROW_COUNT) + (int) CharmManager.getLevel(mPlayer, CHARM_ARROWS);
		mMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? VOLLEY_1_DAMAGE_MULTIPLIER : VOLLEY_2_DAMAGE_MULTIPLIER);
		mVolley = new HashSet<>();
		mVolleyHitMap = new HashMap<>();
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new VolleyCS());
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (!mPlayer.isSneaking()
			|| isOnCooldown()
			|| !EntityUtils.isAbilityTriggeringProjectile(projectile, false)) {
			return true;
		}
		if (Grappling.playerHoldingHook(mPlayer)) {
			return true;
		}

		// Start the cooldown first so we don't cause an infinite loop of Volleys
		putOnCooldown();
		mCosmetic.volleyEffect(mPlayer);
		// Garbage Collector at home
		mVolley.clear();
		mVolleyHitMap.clear();
		float arrowSpeed = ItemUtils.getVanillaProjectileSpeed(mPlayer.getInventory().getItemInMainHand());
		// Give time for other skills to set data
		new BukkitRunnable() {
			@Override
			public void run() {
				List<Projectile> projectiles;
				if (!isEnhanced()) {
					projectiles = EntityUtils.spawnVolley(mPlayer, mArrows, arrowSpeed, 5, projectile.getType());
				} else {
					projectiles = EntityUtils.spawnVolley(mPlayer, mArrows * 2, arrowSpeed, 5, projectile.getType());
				}

				int piercing = (projectile instanceof AbstractArrow) ? ((AbstractArrow) projectile).getPierceLevel() + (int) CharmManager.getLevel(mPlayer, CHARM_PIERCING) : 0;

				for (Projectile proj : projectiles) {
					mVolley.add(proj);
					ProjectileLaunchEvent event = new ProjectileLaunchEvent(proj);
					Bukkit.getPluginManager().callEvent(event);

					if (projectile.getScoreboardTags().contains(Quickdraw.SOURCE_QUICKDRAW_TAG)) {
						proj.addScoreboardTag(Quickdraw.SOURCE_QUICKDRAW_VOLLEY_TAG);
						final ItemStatManager.PlayerItemStats itemStats = DamageListener.getProjectileItemStats(projectile);
						if (itemStats != null) {
							DamageListener.removeProjectileItemStats(proj);
							DamageListener.addProjectileItemStats(proj, itemStats);
						}
					}

					if (proj instanceof AbstractArrow arrow) {
						arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);

						arrow.setCritical(projectile instanceof AbstractArrow projectileArrow && projectileArrow.isCritical());
						arrow.setPierceLevel(piercing);
					} else if (proj instanceof ThrowableProjectile throwable && projectile instanceof ThrowableProjectile oldThrowable) {
						ItemUtils.setSnowballItem(throwable, oldThrowable.getItem());
					}

					mPlugin.mProjectileEffectTimers.addEntity(proj, Particle.SMOKE_NORMAL);
				}

				// We can't just use arrow.remove() because that cancels the event and refunds the arrow
				Location jankWorkAround = mPlayer.getLocation();
				jankWorkAround.setY(-15);
				projectile.teleport(jankWorkAround);
			}
		}.runTaskLater(mPlugin, 0);

		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		Entity proj = event.getDamager();
		if (proj instanceof Projectile && mVolley.contains(proj)) {
			if (notBeenHit(enemy)) {
				event.updateDamageWithMultiplier(mMultiplier);
				mCosmetic.volleyHit(mPlayer, enemy);
			} else {
				// Only let one Volley arrow hit a given mob
				event.setCancelled(true);
			}
		}
		return false; // only changes event damage
	}

	private boolean notBeenHit(LivingEntity enemy) {
		// Basically the same logic as with MetadataUtils.happenedThisTick but with a hashmap in its stead
		if (mVolleyHitMap.get(enemy) != null && mVolleyHitMap.get(enemy) == enemy.getTicksLived()) {
			return false;
		}
		mVolleyHitMap.put(enemy, enemy.getTicksLived());
		return true;
	}

	private static Description<Volley> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Firing a projectile while sneaking")
			.addLine("fires a volley of %d1 projectiles")
				.statValues(stat(a -> a.mArrows, VOLLEY_1_ARROW_COUNT))
			.addLine("that deal increased damage.")
			.addLine()
			.addStat("Damage Boost: +%p1 (p)")
				.statValues(stat(a -> a.mMultiplier - 1, VOLLEY_1_DAMAGE_MULTIPLIER - 1))
			.addStat("Cooldown: %t")
			.statValues(cooldown(VOLLEY_COOLDOWN))
			.addDashedLine();
	}

	private static Description<Volley> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Volley*'s number of").styles(UNDERLINED)
			.addLine("projectiles and its damage boost.")
			.addLine()
			.addStatComparison("Projectiles: %d1 -> %d2")
				.statValues(stat(VOLLEY_1_ARROW_COUNT), stat(a -> a.mArrows, VOLLEY_2_ARROW_COUNT))
			.addStatComparison("Damage Boost: +%p1 -> +%p2 (p)")
				.statValues(stat(VOLLEY_1_DAMAGE_MULTIPLIER - 1), stat(a -> a.mMultiplier - 1, VOLLEY_2_DAMAGE_MULTIPLIER - 1))
			.addDashedLine();
	}

	private static Description<Volley> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("*Volley* now fires *2* times the amount of").styles(UNDERLINED, WHITE)
			.addLine("projectiles an arc twice as wide.").styles(WHITE)
			.addDashedLine();
	}
}
