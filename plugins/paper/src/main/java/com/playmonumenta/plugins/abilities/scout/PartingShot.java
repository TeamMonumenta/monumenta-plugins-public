package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.bosses.bosses.abilities.PartingShotDummyBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.PartingShotCS;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.NegateDamage;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Recoil;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.EnumSet;
import java.util.Objects;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class PartingShot extends Ability implements AbilityWithDuration {
	public static final String PARTING_SHOT_IMBUEMENT = "PartingShotImbuement";
	private static final String DUMMY_SHOT = "PartingShotDummy";
	private static final Style DUMMY_COLOR = Style.style(TextColor.color(0x836850));

	private static final double TRIGGER_THRESHOLD = 0.35;
	private static final int DURATION = 3 * Constants.TICKS_PER_SECOND;
	private static final double PARTING_RADIUS = 8;
	private static final int RECOIL_STRENGTH_L1 = 4;
	private static final int RECOIL_STRENGTH_L2 = 6;
	private static final float KNOCKBACK = 0.75f;
	private static final int FALL_NEGATION = 1;
	private static final double WEAKNESS_AMPLIFIER = 0.4;
	private static final int WEAKNESS_DURATION = 10 * Constants.TICKS_PER_SECOND;
	private static final int STAGGER_DURATION = 4 * Constants.TICKS_PER_SECOND;
	private static final int BUFF_DURATION = 6 * Constants.TICKS_PER_SECOND;
	private static final double RECHARGE_RATE = 0.01;
	private static final int STUN_DURATION = Constants.TICKS_PER_SECOND;

	// Enhancement
	private static final int HEALTH = 100;
	private static final double DUMMY_RADIUS = 5;
	private static final int DUMMY_DURATION = Constants.TICKS_PER_SECOND * 10;
	private static final double DUMMY_DAMAGE = 20;
	private static final double VULN_AMPLIFIER = 0.3;
	private static final int REVEAL_DURATION = 10 * Constants.TICKS_PER_SECOND;
	private static final double REVEAL_RADIUS = 10;

	private static final int COOLDOWN = 60 * Constants.TICKS_PER_SECOND;

	public static final String CHARM_TRIGGER_THRESHOLD = "Parting Shot Threshold";
	public static final String CHARM_DURATION = "Parting Shot Duration";
	public static final String CHARM_PARTING_RADIUS = "Parting Shot Radius";
	public static final String CHARM_RECOIL_STRENGTH = "Parting Shot Recoil Strength";
	public static final String CHARM_KNOCKBACK = "Parting Shot Knockback";
	public static final String CHARM_FALL_NEGATION = "Parting Shot Fall Negation";
	public static final String CHARM_WEAKNESS_DURATION = "Parting Shot Weakness Duration";
	public static final String CHARM_WEAKNESS_AMPLIFIER = "Parting Shot Weakness Amplifier";
	public static final String CHARM_STAGGER_DURATION = "Parting Shot Stagger Duration";
	public static final String CHARM_BUFF_DURATION = "Parting Shot Buff Duration";
	public static final String CHARM_RECHARGE = "Parting Shot Cooldown Recharge Rate";

	// Enhancement
	public static final String CHARM_HEALTH = "Parting Shot Dummy Health";
	public static final String CHARM_DUMMY_DURATION = "Parting Shot Dummy Duration";
	public static final String CHARM_DUMMY_RADIUS = "Parting Shot Dummy Radius";
	public static final String CHARM_DUMMY_DAMAGE = "Parting Shot Dummy Damage";
	public static final String CHARM_DUMMY_STUN = "Parting Shot Dummy Stun Duration";
	public static final String CHARM_VULN_AMPLIFIER = "Parting Shot Reveal Vulnerability Amplifier";
	public static final String CHARM_REVEAL_DURATION = "Parting Shot Reveal Duration";
	public static final String CHARM_REVEAL_RADIUS = "Parting Shot Enhancement Radius";

	public static final String CHARM_COOLDOWN = "Parting Shot Cooldown";

	public static final String GLOWING_OPTION_SCOREBOARD_NAME = "EagleEyeGlowingOption";

	public enum GlowingOption {
		ALL("Mobs affected by any player's Parting Shot Enhancement will glow (default)"),
		OWN("Only mobs affected by your own Parting Shot Enhancement will glow"),
		NEVER("No mobs affected by Parting Shot Enhancement will glow");

		public final String mDescription;

		GlowingOption(String description) {
			this.mDescription = description;
		}
	}

	public static final AbilityInfo<PartingShot> INFO =
		new AbilityInfo<>(PartingShot.class, "Parting Shot", PartingShot::new)
			.linkedSpell(ClassAbility.PARTING_SHOT)
			.scoreboardId("PartingShot")
			.shorthandName("PS")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("When health drops below a threshold, amplify your next shot with recoil.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.TRIPWIRE_HOOK)
			.priorityAmount(10000);

	private final double mThreshold;
	private final int mDuration;
	private final double mPartingRadius;
	private final int mRecoilStrength;
	private final float mKnockback;
	private final int mFallNegation;
	private final double mWeaknessAmplifer;
	private final int mWeaknessDuration;
	private final int mStaggerDuration;
	private final int mBuffDuration;
	private final int mStunDuration;
	private final double mRechargeRate;

	private final int mHealth;
	private final double mDummyDamage;
	private final double mDummyRadius;
	private final int mDummyDuration;
	private final double mVulnLevel;
	private final int mRevealDuration;
	private final double mRevealRadius;
	private final PartingShotCS mCosmetic;

	private boolean mHasExpired = true;

	public PartingShot(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mThreshold = TRIGGER_THRESHOLD + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_TRIGGER_THRESHOLD);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mPartingRadius = CharmManager.getRadius(mPlayer, CHARM_PARTING_RADIUS, PARTING_RADIUS);
		mRecoilStrength = (isLevelOne() ? RECOIL_STRENGTH_L1 : RECOIL_STRENGTH_L2) + (int) CharmManager.getLevel(mPlayer, CHARM_RECOIL_STRENGTH);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK);
		mFallNegation = FALL_NEGATION + (int) CharmManager.getLevel(mPlayer, CHARM_FALL_NEGATION);
		mWeaknessAmplifer = WEAKNESS_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKNESS_AMPLIFIER);
		mWeaknessDuration = CharmManager.getDuration(mPlayer, CHARM_WEAKNESS_DURATION, WEAKNESS_DURATION);
		mStaggerDuration = CharmManager.getDuration(mPlayer, CHARM_STAGGER_DURATION, STAGGER_DURATION);
		mBuffDuration = CharmManager.getDuration(mPlayer, CHARM_BUFF_DURATION, BUFF_DURATION);
		mRechargeRate = RECHARGE_RATE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RECHARGE);

		mHealth = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALTH, HEALTH);
		mDummyDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DUMMY_DAMAGE, DUMMY_DAMAGE);
		mDummyRadius = CharmManager.getRadius(mPlayer, CHARM_DUMMY_RADIUS, DUMMY_RADIUS);
		mDummyDuration = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DUMMY_DURATION, DUMMY_DURATION);
		mStunDuration = CharmManager.getDuration(mPlayer, CHARM_DUMMY_STUN, STUN_DURATION);
		mVulnLevel = VULN_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_VULN_AMPLIFIER);
		mRevealDuration = CharmManager.getDuration(mPlayer, CHARM_REVEAL_DURATION, REVEAL_DURATION);
		mRevealRadius = CharmManager.getRadius(mPlayer, CHARM_REVEAL_RADIUS, REVEAL_RADIUS);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new PartingShotCS());
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.isUnblockable()
			|| !event.getType().isDefendable()
			|| event.isBlocked()
			|| isOnCooldown()) {
			return;
		}

		double newHealth = mPlayer.getHealth() - event.getFinalDamage(true);
		if (newHealth > EntityUtils.getMaxHealth(mPlayer) * mThreshold) {
			return;
		}

		mPlugin.mEffectManager.damageEvent(event);
		event.setLifelineCancel(true);

		if (event.isCancelled() || event.isBlocked()) {
			return;
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();

		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();

		if (mainhand.getItemMeta() instanceof CrossbowMeta crossbowMeta) {
			if (!crossbowMeta.hasChargedProjectiles()) {
				PlayerUtils.loadCrossbow(mPlayer, mainhand);
			}
		} else if (mainhand.getType().equals(Material.TRIDENT)
			|| mainhand.getType().equals(Material.SNOWBALL)
			|| ItemStatUtils.hasEnchantment(mainhand, EnchantmentType.THROWING_KNIFE)) {
			mPlayer.setCooldown(mainhand.getType(), 0);
		}

		MessagingUtils.sendActionBarMessage(mPlayer, "Parting Shot has been activated");

		mPlugin.mEffectManager.addEffect(mPlayer, PARTING_SHOT_IMBUEMENT, new Aesthetics(mDuration,
			(entity, fourHertz, twoHertz, oneHertz) -> mCosmetic.tickEffect(mPlayer, fourHertz, twoHertz, oneHertz),
			(entity) -> {
				if (mHasExpired) {
					mCosmetic.expire(mPlayer.getWorld(), mPlayer, mPlayer.getLocation());
				}
			})
			.deleteOnAbilityUpdate(true)
			.deleteOnLogout(true)
		);

		mHasExpired = true;
		event.setCancelled(true);
		updateAbility();
		doPartingAbility();
		mCosmetic.dodge(world, mPlayer, loc);
		putOnCooldown();
	}

	private void doPartingAbility() {
		for (LivingEntity e : EntityUtils.getNearbyMobs(mPlayer.getLocation(), mPartingRadius)) {
			EntityUtils.applyStagger(mPlugin, mStaggerDuration, e);
			HuntingCompanion.staggerApplied(mPlayer, e);
			MovementUtils.knockAway(mPlayer, e, mKnockback, true);
			mCosmetic.hitMob(mPlayer.getWorld(), mPlayer, e.getEyeLocation());
		}
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (!hasImbuement(mPlayer)) {
			return true;
		}
		projectile.setMetadata(Sharpshooter.NO_TRACKING_METADATA, new FixedMetadataValue(mPlugin, 0));

		Material itemType = mPlayer.getInventory().getItemInMainHand().getType();

		if (projectile instanceof AbstractArrow arrow && itemType.equals(Material.BOW)) {
			float projSpeed = 3f;
			arrow.setVelocity(mPlayer.getLocation().getDirection().multiply(projSpeed));
			arrow.setCritical(true);
		}

		if (!mPlayer.isSneaking() && !ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
			projectile.addScoreboardTag("NoRecoil");

			ItemStack item = mPlayer.getInventory().getItemInMainHand();
			double recoil = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.RECOIL);

			Vector velocity = Recoil.getRecoilVector(mPlayer, recoil + mRecoilStrength);
			mPlayer.setFallDistance(0);
			mPlayer.setVelocity(velocity);

			EntityUtils.applyRecoilDisable(mPlugin, 9999, 1, mPlayer);
		}

		World world = mPlayer.getWorld();
		Location loc = LocationUtils.getHalfHeightLocation(mPlayer);
		mCosmetic.shoot(world, mPlayer, loc, projectile);

		mHasExpired = false;
		mPlugin.mEffectManager.addEffect(mPlayer, "PartingShotFallDamage",
			new NegateDamage(mBuffDuration, mFallNegation, EnumSet.of(DamageEvent.DamageType.FALL))
				.deleteOnAbilityUpdate(true)
				.deleteOnLogout(true));
		mPlugin.mEffectManager.clearEffects(mPlayer, PARTING_SHOT_IMBUEMENT);

		if (isEnhanced()) {
			setDummyArrow(projectile);
		}
		updateAbility();

		return true;
	}

	private void setDummyArrow(Projectile proj) {
		proj.setMetadata(DUMMY_SHOT, new FixedMetadataValue(mPlugin, 0));

		mPlugin.mProjectileEffectTimers.addEntity(proj, Particle.EXPLOSION_NORMAL);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {

				if (mT > mDummyDuration || !proj.isValid() || !proj.hasMetadata(DUMMY_SHOT)) {
					proj.remove();
					this.cancel();
					return;
				}

				if (proj.getVelocity().length() < .05 || proj.isOnGround()) {
					spawnDummy(proj, proj.getLocation());
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
		if (event.getType() == DamageEvent.DamageType.PROJECTILE
			&& damager instanceof AbstractArrow arrow
			&& arrow.isValid()
			&& arrow.hasMetadata(DUMMY_SHOT)) {
			spawnDummy(arrow, enemy.getLocation());
		}
		return false; // prevents multiple calls itself
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (proj instanceof Snowball && proj.hasMetadata(DUMMY_SHOT)) {
			spawnDummy(proj, proj.getLocation());
		}
	}

	private void spawnDummy(Projectile proj, Location loc) {
		proj.removeMetadata(DUMMY_SHOT, mPlugin);
		proj.remove();

		LivingEntity e = Objects.requireNonNull((LivingEntity) LibraryOfSoulsIntegration.summon(loc, mCosmetic.getDummyName()));
		EntityUtils.setMaxHealthAndHealth(e, mHealth);

		PartingShotDummyBoss dummyBoss = mPlugin.mBossManager.getBoss(e, PartingShotDummyBoss.class);
		if (dummyBoss != null) {
			dummyBoss.spawn(this, mDummyRadius, mDummyDuration);
		} else {
			e.remove();
		}
	}

	public void revealMobs(Location loc) {
		for (LivingEntity e : EntityUtils.getNearbyMobs(loc, mDummyRadius)) {
			DamageUtils.damage(mPlayer, e, DamageEvent.DamageType.PROJECTILE_SKILL, mDummyDamage,
				ClassAbility.PARTING_SHOT, true);
			EntityUtils.applyStun(mPlugin, mStunDuration, e);
			MovementUtils.knockAway(loc, e, 0.5f, 0.3f);
		}

		mCosmetic.revealStart(loc.getWorld(), mPlayer, loc, mRevealRadius);
		new Hitbox.SphereHitbox(loc, mRevealRadius)
			.getHitMobs()
			.forEach(this::enhancementMark);
	}

	private void enhancementMark(LivingEntity mob) {
		GlowingManager.startGlowing(mob, NamedTextColor.WHITE, mRevealDuration,
			GlowingManager.PLAYER_ABILITY_PRIORITY, p -> canSeeGlowing(p, mPlayer), null);
		EntityUtils.applyVulnerability(Plugin.getInstance(), mRevealDuration, mVulnLevel, mob);
		mCosmetic.revealOnTarget(mPlayer.getWorld(), mPlayer, mob);
	}

	private static boolean canSeeGlowing(Player player, Player sourcePlayer) {
		int value = Math.min(Math.max(0, ScoreboardUtils.getScoreboardValue(player, GLOWING_OPTION_SCOREBOARD_NAME)
			.orElse(0)), GlowingOption.values().length);
		GlowingOption option = GlowingOption.values()[value];
		return option == GlowingOption.ALL || (option == GlowingOption.OWN && player == sourcePlayer);
	}

	public static boolean hasImbuement(Player player) {
		return Plugin.getInstance().mEffectManager.hasEffect(player, PARTING_SHOT_IMBUEMENT);
	}

	@Override
	public void playerDeathEvent(PlayerDeathEvent e) {
		mHasExpired = false;
		mPlugin.mEffectManager.clearEffects(mPlayer, PARTING_SHOT_IMBUEMENT);
	}

	public double getRechargeRate() {
		return mRechargeRate;
	}

	@Override
	public int getInitialAbilityDuration() {
		return mDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		Effect partingBuff = mPlugin.mEffectManager.getActiveEffect(mPlayer, PARTING_SHOT_IMBUEMENT);
		return partingBuff != null ? partingBuff.getDuration() : 0;
	}


	private static Description<PartingShot> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("When your health drops below %p HP, block the attack")
			.statValues(stat(a -> a.mThreshold, TRIGGER_THRESHOLD))
			.addLine("instead. Stagger and weaken all nearby mobs.")
			.addLine()
			.addLine("Your weapon instantly reloads, and your")
			.addLine("next projectile will recoil you backwards.")
			.addLine("(Bows will fire at full charge, sneak to cancel recoil)")
			.addLine()
			.addStat("Radius: %r")
			.statValues(stat(a -> a.mPartingRadius, PARTING_RADIUS))
			.addStat("Effect: Stagger for %t")
			.statValues(stat(a -> a.mStaggerDuration, STAGGER_DURATION))
			.addStat("Effect: %p Weakness for %t")
			.statValues(stat(a -> a.mWeaknessAmplifer, WEAKNESS_AMPLIFIER), stat(a -> a.mWeaknessDuration, WEAKNESS_DURATION))
			.addStat("Cooldown: %t")
			.statValues(cooldown(COOLDOWN))
			.addDashedLine();
	}

	private static Description<PartingShot> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("*Parting Shot* recharges %p faster").styles(UNDERLINED)
			.statValues(stat(a -> a.mRechargeRate, RECHARGE_RATE))
			.addLine("per *Sharpshooter* stack.").styles(UNDERLINED)
			.addDashedLine();
	}

	private static Description<PartingShot> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("*Parting Shot*'s projectile spawns a *Dummy*").styles(UNDERLINED, DUMMY_COLOR)
			.addLine("that taunts and explodes when killed.")
			.addLine()
			.addStat("Health: %d")
			.statValues(stat(a -> a.mHealth, HEALTH))
			.addStat("Damage: %d")
			.statValues(stat(a -> a.mDummyDamage, DUMMY_DAMAGE))
			.addStat("Radius: %r")
			.statValues(stat(a -> a.mDummyRadius, DUMMY_RADIUS))
			.addLine()
			.addLine("The *Dummy's* explosion stuns and reveals nearby").styles(DUMMY_COLOR)
			.addLine("mobs, inflicting them with vulnerability.")
			.addLine()
			.addStat("Effect: Stun for %t")
			.statValues(stat(a -> a.mStunDuration, STUN_DURATION))
			.addStat("Effect: Glowing for %t")
			.statValues(stat(a -> a.mRevealDuration, REVEAL_DURATION))
			.addStat("Effect: %p Vulnerability for %t")
			.statValues(stat(a -> a.mVulnLevel, VULN_AMPLIFIER), stat(a -> a.mRevealDuration, REVEAL_DURATION))
			.addDashedLine();
	}
}
