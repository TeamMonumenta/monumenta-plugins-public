package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.QuickdrawCS;
import com.playmonumenta.plugins.itemstats.ItemStat;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.perRegion;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

public class Quickdraw extends Ability {
	private static final int R1_DAMAGE = 11;
	private static final int R2_DAMAGE = 17;
	private static final int R3_DAMAGE = 22;
	private static final int COOLDOWN_1 = Constants.TICKS_PER_SECOND * 6;
	private static final int COOLDOWN_2 = Constants.TICKS_PER_SECOND * 3;
	private static final int PIERCE_LVL = 1;
	private static final int ARROWS = 1;

	public static final String CHARM_DAMAGE = "Quickdraw Damage";
	public static final String CHARM_COOLDOWN = "Quickdraw Cooldown";
	public static final String CHARM_PIERCING = "Quickdraw Piercing";
	public static final String CHARM_ARROWS = "Quickdraw Arrows";

	public static final String SOURCE_QUICKDRAW_TAG = "SourceQuickDraw";
	public static final String SOURCE_QUICKDRAW_VOLLEY_TAG = "SourceQuickDrawVolley";

	public static final AbilityInfo<Quickdraw> INFO =
		new AbilityInfo<>(Quickdraw.class, "Quickdraw", Quickdraw::new)
			.linkedSpell(ClassAbility.QUICKDRAW)
			.scoreboardId("Quickdraw")
			.shorthandName("Qd")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Instantly fire a held projectile weapon.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Quickdraw::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(Material.BLAZE_POWDER);

	public @Nullable Projectile mProjectile;

	private final double mDamage;
	private final int mPiercing;
	private final int mArrows;
	private final QuickdrawCS mCosmetic;

	public Quickdraw(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, ServerProperties.getAbilityEnhancementsEnabled(player) ? R3_DAMAGE : ServerProperties.getClassSpecializationsEnabled(player) ? R2_DAMAGE : R1_DAMAGE);
		mPiercing = PIERCE_LVL + (int) CharmManager.getLevel(mPlayer, CHARM_PIERCING);
		mArrows = ARROWS + (int) CharmManager.getLevel(mPlayer, CHARM_ARROWS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new QuickdrawCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		final ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		final ItemStack inOffHand = mPlayer.getInventory().getItemInOffHand();
		if ((ItemStatUtils.hasEnchantment(inMainHand, EnchantmentType.TWO_HANDED)
			&& !(ItemUtils.isNullOrAir(inOffHand) || ItemStatUtils.hasEnchantment(inOffHand, EnchantmentType.WEIGHTLESS)))
			|| ItemUtils.isShootableItem(inOffHand)) {
			return false;
		}
		mCosmetic.quickdrawCast(mPlayer);

		for (int i = 0; i < mArrows; i++) {
			if (!shootProjectile(inMainHand, -0.5 * mArrows + 0.5 + i)) {
				return false;
			}
		}
		putOnCooldown();
		return true;
	}

	private boolean shootProjectile(final ItemStack inMainHand, final double deviation) {
		final Vector direction = mPlayer.getLocation().getDirection();
		if (deviation != 0) {
			final Location l = mPlayer.getLocation();
			l.setPitch(l.getPitch() - 90);
			direction.rotateAroundNonUnitAxis(l.getDirection(), deviation * 10.0 * Math.PI / 180);
		}

		final World world = mPlayer.getWorld();
		final Location eyeLoc = mPlayer.getEyeLocation();
		final Projectile proj;
		switch (inMainHand.getType()) {
			case BOW, CROSSBOW ->
				proj = world.spawnArrow(eyeLoc, direction, ItemUtils.getVanillaProjectileSpeed(inMainHand), 0, Arrow.class);
			case TRIDENT ->
				proj = world.spawnArrow(eyeLoc, direction, ItemUtils.getVanillaProjectileSpeed(inMainHand), 0, Trident.class);
			case SNOWBALL -> {
				final Snowball snowball = world.spawn(eyeLoc, Snowball.class);
				ItemUtils.setSnowballItem(snowball, inMainHand);
				proj = snowball;
				proj.setVelocity(direction.normalize().multiply(ItemUtils.getVanillaProjectileSpeed(inMainHand)));
			}
			default -> {
				if (ItemStatUtils.hasEnchantment(inMainHand, EnchantmentType.THROWING_KNIFE)) {
					proj = world.spawnArrow(eyeLoc, direction, 3f, 0, Arrow.class);
				} else {
					// And you may ask yourself,
					// How did we get here?
					return false;
				}
			}
		}
		mProjectile = proj;

		if (!mPlayer.isSneaking() && ItemStatUtils.getEnchantmentLevel(inMainHand, EnchantmentType.RECOIL) > 0) {
			if (EntityUtils.isRecoilDisable(mPlugin, mPlayer, 1)) {
				proj.addScoreboardTag("NoRecoil");
			}
			EntityUtils.applyRecoilDisable(mPlugin, 9999, 1, mPlayer);
		}
		if (!mPlayer.isSneaking() && ItemStatUtils.getEnchantmentLevel(inMainHand, EnchantmentType.GRAPPLING) > 0) {
			if (EntityUtils.isRecoilDisable(mPlugin, mPlayer, 1)) {
				proj.addScoreboardTag("NoGrapple");
			}
			EntityUtils.applyRecoilDisable(mPlugin, 9999, 1, mPlayer);
			Bukkit.getScheduler().runTaskLater(mPlugin, mPlayer::updateInventory, 1);
		}

		proj.addScoreboardTag(SOURCE_QUICKDRAW_TAG);
		proj.setShooter(mPlayer);

		final ProjectileLaunchEvent eventLaunch = new ProjectileLaunchEvent(proj);
		Bukkit.getPluginManager().callEvent(eventLaunch);

		if (!eventLaunch.isCancelled()) {
			mCosmetic.quickdrawProjectileEffect(mPlugin, proj);
		}

		if (proj instanceof final AbstractArrow arrow) {
			arrow.setPierceLevel(isEnhanced() ? mPiercing : 0);
			arrow.setCritical(true);
			arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
		}

		final ItemStatManager.PlayerItemStats stats = DamageListener.getProjectileItemStats(proj);
		if (stats != null) {
			final ItemStatManager.PlayerItemStats.ItemStatsMap map = stats.getItemStats();

			if (map != null) {
				final ItemStat projDamageAdd = Objects.requireNonNull(AttributeType.PROJECTILE_DAMAGE_ADD.getItemStat());
				final ItemStat sniper = Objects.requireNonNull(EnchantmentType.SNIPER.getItemStat());
				final ItemStat pointBlank = Objects.requireNonNull(EnchantmentType.POINT_BLANK.getItemStat());
				// I know this doesn't make sense but trust me on this because Tridents were applying spec enchants
				final ItemStat smite = Objects.requireNonNull(EnchantmentType.SMITE.getItemStat());
				final ItemStat slayer = Objects.requireNonNull(EnchantmentType.SLAYER.getItemStat());
				final ItemStat duelist = Objects.requireNonNull(EnchantmentType.DUELIST.getItemStat());
				final ItemStat hexEater = Objects.requireNonNull(EnchantmentType.HEX_EATER.getItemStat());
				final ItemStat chaotic = Objects.requireNonNull(EnchantmentType.CHAOTIC.getItemStat());
				map.set(projDamageAdd, mDamage);
				map.set(sniper, 0);
				map.set(pointBlank, 0);
				map.set(smite, 0);
				map.set(slayer, 0);
				map.set(duelist, 0);
				map.set(hexEater, 0);
				map.set(chaotic, 0);
				// See DamageListener for damage implementation
			}
		}

		return !eventLaunch.isCancelled() || proj instanceof Trident;
	}

	public boolean isQuickDraw(final Projectile projectile) {
		return projectile == mProjectile;
	}

	private static Description<Quickdraw> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Instantly fire an arrow with your weapon.")
			.addIf((a, p) -> p != null && AdvancementUtils.checkAdvancement(p, "monumenta:handbook/enchantments/recoil"), desc -> desc
				.addLine("(Can only use Recoil once while midair)"))
			.addLine()
			.addStat("Damage: %d0R (p)")
				.statValues(perRegion(a -> a.mDamage, R1_DAMAGE, R2_DAMAGE, R3_DAMAGE))
			.addStat("Cooldown: %t1")
				.statValues(cooldown(COOLDOWN_1))
			.addDashedLine();
	}

	private static Description<Quickdraw> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Reduce *Quickdraw*'s cooldown.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Cooldown: %t1 -> %t2")
				.statValues(cooldown(COOLDOWN_1), cooldown(COOLDOWN_2))
			.addDashedLine();
	}

	private static Description<Quickdraw> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("*Quickdraw*'s arrows gain *Piercing* %d.").styles(UNDERLINED, WHITE)
				.statValues(stat(a -> a.mPiercing, PIERCE_LVL))
			.addDashedLine();
	}
}
