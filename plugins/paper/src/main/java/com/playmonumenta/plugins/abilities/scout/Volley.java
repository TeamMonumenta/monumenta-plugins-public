package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.abilities.scout.hunter.QuiverStorm;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.VolleyCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Grappling;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class Volley extends MultipleChargeAbility {
	public static final String ENHANCEMENT_METADATA = "VolleyMultishotEnhancement";
	public static final String VOLLEY_METADATA = "VolleyThisTick";

	private static final int VOLLEY_COOLDOWN = 10 * Constants.TICKS_PER_SECOND;
	private static final int VOLLEY_CHARGES = 2;
	private static final int VOLLEY_1_ARROW_COUNT = 5;
	private static final int VOLLEY_2_ARROW_COUNT = 9;
	private static final double VOLLEY_1_DAMAGE_PERCENT = 1.0;
	private static final double VOLLEY_2_DAMAGE_PERCENT = 1.2;
	private static final double VOLLEY_1_DAMAGE_FLAT = 6;
	private static final double VOLLEY_2_DAMAGE_FLAT = 8;
	private static final int MULTISHOT_BUFF = 1;
	private static final int MULTISHOT_SHOTS = 1;

	public Set<Projectile> mVolley;
	private final Map<LivingEntity, Integer> mVolleyHitMap;

	public static final String CHARM_COOLDOWN = "Volley Cooldown";
	public static final String CHARM_ARROWS = "Volley Arrows";
	public static final String CHARM_DAMAGE = "Volley Damage";
	public static final String CHARM_PIERCING = "Volley Piercing";
	public static final String CHARM_CHARGES = "Volley Charges";
	public static final String CHARM_MULTISHOT_LEVEL = "Volley Multishot Level";
	public static final String CHARM_MULTISHOT_SHOT = "Volley Multishot Shots";

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
	private final double mPercentDamage;
	private final double mFlatDamage;
	private final int mMultishotLevel;
	private final int mMultishotShots;
	private final VolleyCS mCosmetic;
	private int mEnhancementShots = 0;

	private int mVolleyTime;
	private int mMultishotTime;

	public Volley(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mArrows = (isLevelOne() ? VOLLEY_1_ARROW_COUNT : VOLLEY_2_ARROW_COUNT) + (int) CharmManager.getLevel(mPlayer, CHARM_ARROWS);
		mPercentDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? VOLLEY_1_DAMAGE_PERCENT : VOLLEY_2_DAMAGE_PERCENT);
		mFlatDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? VOLLEY_1_DAMAGE_FLAT : VOLLEY_2_DAMAGE_FLAT);
		mMaxCharges = 2 + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mMultishotLevel = MULTISHOT_BUFF + (int) CharmManager.getLevel(mPlayer, CHARM_MULTISHOT_LEVEL);
		mMultishotShots = MULTISHOT_SHOTS + (int) CharmManager.getLevel(mPlayer, CHARM_MULTISHOT_SHOT);
		mVolley = new HashSet<>();
		mVolleyHitMap = new HashMap<>();
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new VolleyCS());

		mCharges = getChargesOffCooldown();
		mVolleyTime = Bukkit.getServer().getCurrentTick();
		mMultishotTime = Bukkit.getServer().getCurrentTick();
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		// Multishot pre-req.
		if (!EntityUtils.isAbilityTriggeringProjectile(projectile, false)
			|| projectile.hasMetadata(QuiverStorm.ARROW_METADATA)
			|| Grappling.playerHoldingHook(mPlayer)
		) {
			return true;
		}

		int tick = Bukkit.getServer().getCurrentTick();

		if (mEnhancementShots > 0
			&& !mVolley.contains(projectile)
			&& !isVolleyShot(mPlayer)
			&& tick - mMultishotTime >= 2
		) {
			mEnhancementShots--;
			mMultishotTime = tick;
			multishotEnhancement(projectile);
		}

		// Volley pre-req.
		if (!mPlayer.isSneaking()
			|| tick - mVolleyTime < 5
			|| !consumeCharge()
		) {
			return true;
		}

		ClientModHandler.updateAbility(mPlayer, this);
		MetadataUtils.markThisTick(mPlugin, mPlayer, VOLLEY_METADATA);
		mVolleyTime = tick;
		mCosmetic.volleyEffect(mPlayer);

		if (isEnhanced()) {
			mEnhancementShots = mMultishotShots;
		}

		float arrowSpeed = ItemUtils.getVanillaProjectileSpeed(mPlayer.getInventory().getItemInMainHand());
		// Give time for other skills to set data
		new BukkitRunnable() {
			@Override
			public void run() {
				List<Projectile> projectiles
					= EntityUtils.spawnVolley(mPlayer, mArrows, arrowSpeed, 8, projectile.getType());

				int piercing = (projectile instanceof AbstractArrow) ? ((AbstractArrow) projectile).getPierceLevel() + (int) CharmManager.getLevel(mPlayer, CHARM_PIERCING) : 0;

				for (Projectile proj : projectiles) {
					mVolley.add(proj);

					AbilityUtils.inheritProjectileStats(mPlayer, proj, projectile);
					ProjectileLaunchEvent event = new ProjectileLaunchEvent(proj);
					Bukkit.getPluginManager().callEvent(event);

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
				AbilityUtils.removeProjectile(projectile);
			}
		}.runTaskLater(mPlugin, 0);

		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		Entity proj = event.getDamager();
		if (proj instanceof Projectile && mVolley.contains(proj)) {
			if (notBeenHit(enemy)) {
				event.setFlatDamage(event.getFlatDamage() * mPercentDamage + mFlatDamage);
				event.setAbility(ClassAbility.VOLLEY);

				mCosmetic.volleyHit(mPlayer, enemy);
			}
		}
		return false;
	}

	public static boolean isVolleyShot(Player player) {
		return MetadataUtils.happenedThisTick(player, VOLLEY_METADATA);
	}

	private boolean notBeenHit(LivingEntity enemy) {
		// Basically the same logic as with MetadataUtils.happenedThisTick but with a hashmap in its stead
		if (mVolleyHitMap.get(enemy) != null && mVolleyHitMap.get(enemy) == enemy.getTicksLived()) {
			return false;
		}
		mVolleyHitMap.put(enemy, enemy.getTicksLived());
		return true;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		// Garbage Collector at home
		if (oneSecond) {
			mVolley.removeIf(t -> !t.isValid());
			mVolleyHitMap.keySet().removeIf(e -> !e.isValid());
		}
	}

	private void multishotEnhancement(Projectile projectile) {
		boolean hasMultishot = ItemStatUtils.hasEnchantment(mPlayer.getInventory().getItemInMainHand(), EnchantmentType.MULTISHOT);
		float arrowSpeed = ItemUtils.getVanillaProjectileSpeed(mPlayer.getInventory().getItemInMainHand());
		int piercing = (projectile instanceof AbstractArrow) ? ((AbstractArrow) projectile).getPierceLevel() : 0;

		// Multishot arrows are spaced by 10 from the main arrow
		final List<Projectile> projectiles = new ArrayList<>();
		int spacing = hasMultishot ? 20 : 0;

		for (int i = 0; i < mMultishotLevel; i++) {
			spacing += 20;
			projectiles.addAll(EntityUtils.spawnVolley(mPlayer, 2, arrowSpeed, spacing, projectile.getType()));
		}

		for (Projectile proj : projectiles) {
			proj.setMetadata(ENHANCEMENT_METADATA, new FixedMetadataValue(mPlugin, 0));
			AbilityUtils.inheritProjectileStats(mPlayer, proj, projectile);

			ProjectileLaunchEvent event = new ProjectileLaunchEvent(proj);
			Bukkit.getPluginManager().callEvent(event);

			if (proj instanceof AbstractArrow arrow) {
				arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
				arrow.setCritical(projectile instanceof AbstractArrow projectileArrow && projectileArrow.isCritical());
				arrow.setPierceLevel(piercing);
			} else if (proj instanceof ThrowableProjectile throwable && projectile instanceof ThrowableProjectile oldThrowable) {
				ItemUtils.setSnowballItem(throwable, oldThrowable.getItem());
			}
		}
	}

	private static Description<Volley> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Firing a projectile while sneaking")
			.addLine("fires a volley of %d1 projectiles")
			.statValues(stat(a -> a.mArrows, VOLLEY_1_ARROW_COUNT))
			.addLine("that deal increased damage.")
			.addLine()
			.addStat("Damage: %d1 + %p1 (p) (of weapon damage)")
			.statValues(stat(a -> a.mFlatDamage, VOLLEY_1_DAMAGE_FLAT), stat(a -> a.mPercentDamage, VOLLEY_1_DAMAGE_PERCENT))
			.addStat("Charges: %d")
			.statValues(stat(a -> a.mMaxCharges, VOLLEY_CHARGES))
			.addStat("Cooldown: %t (per charge)")
			.statValues(cooldown(VOLLEY_COOLDOWN))
			.addDashedLine();
	}

	private static Description<Volley> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Volley*'s damage and projectile count.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Damage Boost: %d1 + %p1 -> %d2 + %p2 (p)")
			.statValues(stat(VOLLEY_1_DAMAGE_FLAT), stat(VOLLEY_1_DAMAGE_PERCENT), stat(a -> a.mFlatDamage, VOLLEY_2_DAMAGE_FLAT), stat(a -> a.mPercentDamage, VOLLEY_2_DAMAGE_PERCENT))
			.addStatComparison("Projectiles: %d1 -> %d2")
			.statValues(stat(VOLLEY_1_ARROW_COUNT), stat(a -> a.mArrows, VOLLEY_2_ARROW_COUNT))
			.addDashedLine();
	}

	private static Description<Volley> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Casting *Volley* empowers your").styles(UNDERLINED)
			.addLine("next shot with Multishot.")
			.addLine("(Works with any weapon type)")
			.addIf((a, p) -> a != null && (a.mMultishotLevel != 1 || a.mMultishotShots != 1),
				FormattedDescriptionBuilder::addLine)
			.addIf((a, p) -> a != null && a.mMultishotLevel != 1,
				desc -> desc.addStat("Multishot Level: %d")
					.statValues(stat(a -> a.mMultishotLevel, 1)))
			.addIf((a, p) -> a != null && a.mMultishotShots != 1,
				desc -> desc.addStat("Empowered Shots: %d")
					.statValues(stat(a -> a.mMultishotShots, 1)))
			.addDashedLine();
	}
}
