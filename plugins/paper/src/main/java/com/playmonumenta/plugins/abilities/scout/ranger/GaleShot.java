package com.playmonumenta.plugins.abilities.scout.ranger;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.abilities.scout.Volley;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.ranger.GaleShotCS;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Grappling;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.WeakHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class GaleShot extends Ability implements AbilityWithChargesOrStacks, AbilityWithDuration {
	public static final AbilityInfo<GaleShot> INFO =
		new AbilityInfo<>(GaleShot.class, "Gale Shot", GaleShot::new)
			.linkedSpell(ClassAbility.GALE_SHOT)
			.scoreboardId("GaleShot")
			.shorthandName("GS")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Your projectile will be enhanced after casting multiple skills.")
			.displayItem(Material.BONE_MEAL);

	private static final String GALE_SHOT_IMBUEMENT = "GaleShotImbuement";
	private static final String GALE_SHOT_PROJECTILE_METAKEY = "GaleShotProjectile";
	private static final double DAMAGE = 14;
	private static final double DAMAGE_PERCENT = 1.2;
	private static final int ABILITY_REQ = 2;
	private static final int SHOT_REQ = 2;
	private static final int DURATION = Constants.TICKS_PER_SECOND * 8;
	private static final int SLOWNESS_DURATION = Constants.TICKS_PER_SECOND * 3;
	private static final double SLOWNESS_AMPLIFIER = 0.25;

	public static final String CHARM_DAMAGE_FLAT = "Gale Shot Flat Damage";
	public static final String CHARM_DAMAGE_PERCENT = "Gale Shot Damage Multiplier";
	public static final String CHARM_RANGE = "Gale Shot Range";
	public static final String CHARM_ABILITY_REQUIREMENT = "Gale Shot Ability Requirement";
	public static final String CHARM_SHOT_REQUIREMENT = "Gale Shot Landing Requirement";
	public static final String CHARM_DURATION = "Gale Shot Duration";
	public static final String CHARM_SLOWNESS_DURATION = "Gale Shot Slowness Duration";
	public static final String CHARM_SLOWNESS_AMPLIFIER = "Gale Shot Slowness Amplifier";
	public static final String CHARM_COUNT = "Gale Shot Count";

	private final double mDamageFlat;
	private final double mDamagePercent;
	private final int mAbilityRequirement;
	private final int mShotRequirement;
	private final int mDuration;
	private final double mSlownessAmplifier;
	private final int mSlownessDuration;
	private final int mShotCount;
	private final WeakHashMap<LivingEntity, Integer> mMarkedMobs = new WeakHashMap<>();
	private final GaleShotCS mCosmetic;
	private int mAbilityCount = 0;

	private @Nullable Sharpshooter mSharpshooter;
	private int mCount;

	public GaleShot(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mDamageFlat = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE_FLAT, DAMAGE);
		mDamagePercent = CharmManager.getExtraPercent(mPlayer, CHARM_DAMAGE_PERCENT, DAMAGE_PERCENT);
		mAbilityRequirement = Math.max(0, ABILITY_REQ + (int) CharmManager.getLevel(mPlayer, CHARM_ABILITY_REQUIREMENT));
		mShotRequirement = Math.max(0, SHOT_REQ + (int) CharmManager.getLevel(mPlayer, CHARM_SHOT_REQUIREMENT));
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mSlownessDuration = CharmManager.getDuration(mPlayer, CHARM_SLOWNESS_DURATION, SLOWNESS_DURATION);
		mSlownessAmplifier = CharmManager.getExtraPercent(mPlayer, CHARM_SLOWNESS_AMPLIFIER, SLOWNESS_AMPLIFIER);
		mShotCount = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_COUNT);

		Bukkit.getScheduler().runTask(plugin, () ->
			mSharpshooter = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, Sharpshooter.class));

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new GaleShotCS());
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (!EntityUtils.isAbilityTriggeringProjectile(projectile, true)
			|| Volley.isVolleyShot(mPlayer)
			|| Grappling.playerHoldingHook(mPlayer)
			|| !hasImbuement()) {
			return true;
		}

		if (--mCount <= 0) {
			mAbilityCount = 0;
			mPlugin.mEffectManager.clearEffects(mPlayer, GALE_SHOT_IMBUEMENT);
		}

		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		float projSpeed = ItemUtils.getVanillaProjectileSpeed(mainHand);
		Arrow galeArrow = (Arrow) EntityUtils.spawnProjectile(mPlayer, 0, 0, new Vector(0, 0, 0), projSpeed, EntityType.ARROW);

		// Destroy the original projectile and use an arrow instead because it pierces

		AbilityUtils.inheritProjectileStats(mPlayer, galeArrow, projectile);
		ProjectileLaunchEvent event = new ProjectileLaunchEvent(galeArrow);
		Bukkit.getPluginManager().callEvent(event);

		galeArrow.setPierceLevel(67);
		galeArrow.setCritical(true);
		galeArrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);

		AbilityUtils.removeProjectile(projectile);

		galeArrow.setMetadata(GALE_SHOT_PROJECTILE_METAKEY, new FixedMetadataValue(mPlugin, 0));

		if (mSharpshooter != null) {
			mSharpshooter.doNotTrack(projectile);
			mSharpshooter.trackArrow(galeArrow);
		}

		updateAbility();
		PlayerUtils.callAbilityCastEvent(mPlayer, this, ClassAbility.GALE_SHOT, 0);
		mCosmetic.fire(mPlayer, galeArrow);

		return true;
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		if (hasImbuement()) {
			return false;
		}

		ClassAbility ability = event.getSpell();

		if (ability == null
			|| ability.equals(ClassAbility.GALE_SHOT)
			|| ability.equals(ClassAbility.SWIFTNESS)) {
			return false;
		}

		if (++mAbilityCount >= mAbilityRequirement) {
			imbue();
		}
		updateAbility();

		return false;
	}

	private void imbue() {
		if (hasImbuement()) {
			return;
		}

		mCosmetic.imbue(mPlayer.getLocation());
		mCount = mShotCount;
		mPlugin.mEffectManager.addEffect(mPlayer, GALE_SHOT_IMBUEMENT, new Aesthetics(mDuration,
			(entity, fourHertz, twoHertz, oneHertz) -> mCosmetic.tick(mPlayer, mPlayer.getLocation()),
			entity -> Bukkit.getScheduler().runTask(mPlugin, () -> {
				mAbilityCount = 0;
				mCount = 0;
				updateAbility();
			})
		).deleteOnAbilityUpdate(true));
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond) {
			mMarkedMobs.entrySet().removeIf((entry) -> !entry.getKey().isValid() || entry.getKey().isDead());
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.PROJECTILE
			&& event.getDamager() instanceof Arrow galeArrow
			&& galeArrow.hasMetadata(GALE_SHOT_PROJECTILE_METAKEY)
		) {
			event.setAbility(ClassAbility.GALE_SHOT);
			event.setFlatDamage(mDamagePercent * event.getFlatDamage() + mDamageFlat);

			Location enemyLoc = enemy.getLocation();
			enemyLoc.setY(Math.clamp(galeArrow.getY(), enemy.getY(), enemy.getHeight() + enemy.getY()));

			mCosmetic.hit(mPlayer, enemyLoc);
			if (isLevelTwo()) {
				EntityUtils.applySlow(mPlugin, mSlownessDuration, mSlownessAmplifier, enemy);

				mMarkedMobs.compute(enemy, (k, v) -> {
					int next = (v == null ? 1 : v + 1);
					boolean canImbue = next >= mShotRequirement;
					if (canImbue) {
						mAbilityCount = mAbilityRequirement;
						imbue();
						updateAbility();
						return -1337; // Prevents gaining gale shot from the same mob
					} else {
						return next;
					}
				});
			}
		}
		return false;
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (proj.hasMetadata(GALE_SHOT_PROJECTILE_METAKEY) && event.getHitBlock() != null) {
			mCosmetic.hitBlock(mPlayer, proj.getLocation());
		}
	}

	private boolean hasImbuement() {
		return mPlugin.mEffectManager.hasEffect(mPlayer, GALE_SHOT_IMBUEMENT);
	}

	@Override
	public int getCharges() {
		return mAbilityCount;
	}

	@Override
	public int getMaxCharges() {
		return mAbilityRequirement;
	}

	@Override
	public @Nullable String getMode() {
		return mAbilityCount == mAbilityRequirement ? "max" : null;
	}

	private static Description<GaleShot> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Casting %d abilities turns your next")
			.statValues(stat(a -> a.mAbilityRequirement, ABILITY_REQ))
			.addLine("projectile into a *Gale Shot*, granting").styles(UNDERLINED)
			.addLine("infinite pierce and increased damage.")
			.addLine("(Only imbues the central projectile.)")
			.addLine()
			.addStat("Damage: %d + %p (p) (of weapon damage)")
			.statValues(stat(a -> a.mDamageFlat, DAMAGE), stat(a -> a.mDamagePercent, DAMAGE_PERCENT))
			.addIf((a, p) -> a != null && a.mShotCount != 1, desc -> desc
				.addStat("Shots: %d")
				.statValues(stat(a -> a.mShotCount, 1)))
			.addDashedLine();
	}

	private static Description<GaleShot> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("*Gale Shot* now applies slowness.").styles(UNDERLINED)
			.addLine()
			.addStat("Effect: %p Slowness for %t")
			.statValues(stat(a -> a.mSlownessAmplifier, SLOWNESS_AMPLIFIER), stat(a -> a.mSlownessDuration, SLOWNESS_DURATION))
			.addLine()
			.addLine("Landing %d *Gale Shots* on the same").styles(UNDERLINED)
			.statValues(stat(a -> a.mShotRequirement, SHOT_REQ))
			.addLine("mob refreshes *Gale Shot*.").styles(UNDERLINED)
			.addLine("(Can only be triggered once per mob.)")
			.addDashedLine();
	}

	@Override
	public int getInitialAbilityDuration() {
		return mDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		Effect galeBuff = mPlugin.mEffectManager.getActiveEffect(mPlayer, GALE_SHOT_IMBUEMENT);
		return galeBuff != null ? galeBuff.getDuration() : 0;
	}
}
