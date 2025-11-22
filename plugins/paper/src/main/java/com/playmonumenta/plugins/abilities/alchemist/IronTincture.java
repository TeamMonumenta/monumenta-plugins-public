package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.IronTinctureCS;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class IronTincture extends Ability {

	private static final int IRON_TINCTURE_THROW_COOLDOWN = 10 * 20;
	private static final int IRON_TINCTURE_USE_COOLDOWN = 50 * 20;
	private static final int IRON_TINCTURE_1_ABSORPTION = 6;
	private static final int IRON_TINCTURE_2_ABSORPTION = 10;
	private static final int IRON_TINCTURE_ABSORPTION_DURATION = 20 * 50;
	private static final int IRON_TINCTURE_TICK_PERIOD = 2;
	private static final int IRON_TINCTURE_POTION_REFILL = 1;
	private static final int IRON_TINCTURE_ALLY_POTION_REFILL = 2;
	private static final double IRON_TINCTURE_VELOCITY = 0.7;

	private static final double IRON_TINCTURE_ENHANCEMENT_RESISTANCE = 0.05;
	private static final int IRON_TINCTURE_ENHANCEMENT_STUN_RADIUS = 3;
	private static final int IRON_TINCTURE_ENHANCEMENT_STUN_DURATION = 30;

	public static final String CHARM_COOLDOWN = "Iron Tincture Cooldown";
	public static final String CHARM_ABSORPTION = "Iron Tincture Absorption Health";
	public static final String CHARM_DURATION = "Iron Tincture Duration";
	public static final String CHARM_VELOCITY = "Iron Tincture Velocity";
	public static final String CHARM_RESISTANCE = "Iron Tincture Enhancement Resistance Amplifier";
	public static final String CHARM_REFILL = "Iron Tincture Potion Refill";
	public static final String CHARM_ALLY_REFILL = "Iron Tincture Ally Potion Refill";
	public static final String CHARM_STUN_RADIUS = "Iron Tincture Stun Radius";
	public static final String CHARM_STUN_DURATION = "Iron Tincture Stun Duration";

	public static final AbilityInfo<IronTincture> INFO =
		new AbilityInfo<>(IronTincture.class, "Iron Tincture", IronTincture::new)
			.linkedSpell(ClassAbility.IRON_TINCTURE)
			.scoreboardId("IronTincture")
			.shorthandName("IT")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Throw a potion on the ground that you or other players can collect to gain absorption hearts.")
			.cooldown(IRON_TINCTURE_USE_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", IronTincture::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.SPLASH_POTION);

	private final double mAbsorption;
	private final int mRefill;
	private final int mAllyRefill;
	private final int mDuration;
	private final double mResistance;
	private final double mStunRadius;
	private final int mStunDuration;
	private final IronTinctureCS mCosmetic;
	private @Nullable AlchemistPotions mAlchemistPotions;

	public IronTincture(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, isLevelOne() ? IRON_TINCTURE_1_ABSORPTION : IRON_TINCTURE_2_ABSORPTION);
		mRefill = IRON_TINCTURE_POTION_REFILL + (int) CharmManager.getLevel(mPlayer, CHARM_REFILL);
		mAllyRefill = IRON_TINCTURE_ALLY_POTION_REFILL + (int) CharmManager.getLevel(mPlayer, CHARM_ALLY_REFILL);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, IRON_TINCTURE_ABSORPTION_DURATION);
		mResistance = IRON_TINCTURE_ENHANCEMENT_RESISTANCE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RESISTANCE);
		mStunRadius = CharmManager.getRadius(mPlayer, CHARM_STUN_RADIUS, IRON_TINCTURE_ENHANCEMENT_STUN_RADIUS);
		mStunDuration = CharmManager.getDuration(mPlayer, CHARM_STUN_DURATION, IRON_TINCTURE_ENHANCEMENT_STUN_DURATION);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new IronTinctureCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getEyeLocation();
		double velocity = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, IRON_TINCTURE_VELOCITY);
		Item tincture = AbilityUtils.spawnAbilityItem(world, loc, Material.SPLASH_POTION, mCosmetic.tinctureName(), false, velocity, true, true);
		mCosmetic.onThrow(world, loc);

		// Full duration cooldown - is shortened if not picked up
		putOnCooldown();

		new BukkitRunnable() {
			int mTinctureDecay = 0;

			@Override
			public void run() {
				Location l = tincture.getLocation();
				mCosmetic.onGroundEffect(l, mPlayer, mTinctureDecay / IRON_TINCTURE_TICK_PERIOD);

				for (Player p : new Hitbox.UprightCylinderHitbox(l, 0.7, 0.7).getHitPlayers(true)) {
					// Prevent players from picking up their own tincture instantly
					if (p == mPlayer && tincture.getTicksLived() < 12) {
						continue;
					}

					mCosmetic.pickupEffects(l.getWorld(), l, p);

					tincture.remove();

					execute(mPlayer, l);
					if (p != mPlayer) {
						execute(p, l);
						if (mAlchemistPotions != null) {
							mAlchemistPotions.incrementCharges(mAllyRefill);
						}
					} else {
						if (mAlchemistPotions != null) {
							mAlchemistPotions.incrementCharges(mRefill);
						}
					}

					mPlugin.mTimers.removeCooldown(mPlayer, ClassAbility.IRON_TINCTURE);
					putOnCooldown();

					this.cancel();
					return;
				}

				mTinctureDecay += IRON_TINCTURE_TICK_PERIOD;
				if (mTinctureDecay >= IRON_TINCTURE_THROW_COOLDOWN || !tincture.isValid() || tincture.isDead()) {
					mCosmetic.tinctureExpireEffects(l, mPlayer);
					tincture.remove();
					this.cancel();

					// Take the skill off cooldown (by setting to 0)
					mPlugin.mTimers.setCooldown(mPlayer, ClassAbility.IRON_TINCTURE, 0);
				}
			}

		}.runTaskTimer(mPlugin, 0, IRON_TINCTURE_TICK_PERIOD);

		return true;
	}

	private void execute(Player player, Location tinctureLocation) {
		AbsorptionUtils.addAbsorption(player, mAbsorption, mAbsorption, mDuration);

		if (isEnhanced()) {
			Hitbox hitbox = new Hitbox.SphereHitbox(tinctureLocation, mStunRadius);
			hitbox.getHitMobs().forEach(mob -> EntityUtils.applyStun(mPlugin, mStunDuration, mob));

			mPlugin.mEffectManager.addEffect(player, "IronTinctureEnhancementResistanceEffect", new PercentDamageReceived(mDuration, -mResistance) {
				@Override
				public void onHurt(LivingEntity entity, DamageEvent event) {
					if (event.getType() == DamageEvent.DamageType.TRUE) {
						return;
					}
					if (entity instanceof Player player) {
						if (AbsorptionUtils.getAbsorption(player) > 0) {
							event.setFlatDamage(event.getDamage() * (1 - mResistance));
						}
					}
				}
			}.deleteOnAbilityUpdate(true));
		}

		mCosmetic.pickupEffectsForPlayer(player, tinctureLocation);
	}

	private static Description<IronTincture> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to throw a tincture. If you walk over the tincture, gain ")
			.add(a -> a.mAbsorption, IRON_TINCTURE_1_ABSORPTION, false, Ability::isLevelOne)
			.add(" absorption health for ")
			.addDuration(a -> a.mDuration, IRON_TINCTURE_ABSORPTION_DURATION)
			.add(" seconds. If an ally walks over it, you both gain the effect. If it isn't grabbed before it disappears, it will quickly come off cooldown. When another player grabs the tincture, you gain 2 Alchemist's Potions. When you grab the tincture, you gain 1 Alchemist's Potion.")
			.addCooldown(IRON_TINCTURE_USE_COOLDOWN);
	}

	private static Description<IronTincture> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Absorption is increased to ")
			.add(a -> a.mAbsorption, IRON_TINCTURE_2_ABSORPTION, false, Ability::isLevelTwo)
			.add(".");
	}

	private static Description<IronTincture> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The tincture now grants ")
			.addPercent(a -> a.mResistance, IRON_TINCTURE_ENHANCEMENT_RESISTANCE)
			.add(" damage resistance when absorption is present, for the duration of the absorption. Additionally, mobs within ")
			.add(a -> a.mStunRadius, IRON_TINCTURE_ENHANCEMENT_STUN_RADIUS)
			.add(" blocks of the player who picks it up will be stunned for ")
			.addDuration(a -> a.mStunDuration, IRON_TINCTURE_ENHANCEMENT_STUN_DURATION)
			.add(" seconds.");
	}
}
