package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
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
import com.playmonumenta.plugins.utils.StringUtils;
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

	public static final AbilityInfo<IronTincture> INFO =
		new AbilityInfo<>(IronTincture.class, "Iron Tincture", IronTincture::new)
			.linkedSpell(ClassAbility.IRON_TINCTURE)
			.scoreboardId("IronTincture")
			.shorthandName("IT")
			.descriptions(
				("Crouch and right-click to throw a tincture. If you walk over the tincture, gain %s absorption health " +
				"for %ss, up to %s absorption health. If an ally walks over it, or is hit by it, you both gain the effect. " +
				"If it isn't grabbed before it disappears, it will quickly come off cooldown. " +
				"When another player grabs the tincture, you gain 2 Alchemist's Potions. When you grab the tincture, " +
				"you gain 1 Alchemist's Potion. Cooldown: %ss.")
					.formatted(
							IRON_TINCTURE_1_ABSORPTION,
							StringUtils.ticksToSeconds(IRON_TINCTURE_ABSORPTION_DURATION),
							IRON_TINCTURE_1_ABSORPTION,
							StringUtils.ticksToSeconds(IRON_TINCTURE_USE_COOLDOWN)
					),
				"Effect and effect cap increased to %s absorption health."
					.formatted(IRON_TINCTURE_2_ABSORPTION),
				("The tincture now grants %s%% damage resistance when absorption is present, for the duration of the absorption. " +
				"Additionally, mobs within a %s block radius of the player who picks it up will be stunned for %ss.")
					.formatted(
						StringUtils.multiplierToPercentage(IRON_TINCTURE_ENHANCEMENT_RESISTANCE),
						IRON_TINCTURE_ENHANCEMENT_STUN_RADIUS,
						StringUtils.ticksToSeconds(IRON_TINCTURE_ENHANCEMENT_STUN_DURATION)
					)
			)
			.simpleDescription("Throw a potion on the ground that you or other players can collect to gain absorption hearts.")
			.cooldown(IRON_TINCTURE_USE_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", IronTincture::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true)
					.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.SPLASH_POTION);

	private @Nullable AlchemistPotions mAlchemistPotions;
	private final double mAbsorption;
	private final IronTinctureCS mCosmetic;

	public IronTincture(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, isLevelOne() ? IRON_TINCTURE_1_ABSORPTION : IRON_TINCTURE_2_ABSORPTION);

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
							mAlchemistPotions.incrementCharges(IRON_TINCTURE_ALLY_POTION_REFILL + (int) CharmManager.getLevel(mPlayer, CHARM_ALLY_REFILL));
						}
					} else {
						if (mAlchemistPotions != null) {
							mAlchemistPotions.incrementCharges(IRON_TINCTURE_POTION_REFILL + (int) CharmManager.getLevel(mPlayer, CHARM_REFILL));
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
		int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, IRON_TINCTURE_ABSORPTION_DURATION);
		AbsorptionUtils.addAbsorption(player, mAbsorption, mAbsorption, duration);

		if (isEnhanced()) {
			Hitbox hitbox = new Hitbox.SphereHitbox(player.getLocation(), IRON_TINCTURE_ENHANCEMENT_STUN_RADIUS);
			hitbox.getHitMobs().forEach(mob -> EntityUtils.applyStun(mPlugin, IRON_TINCTURE_ENHANCEMENT_STUN_DURATION, mob));

			double resistance = IRON_TINCTURE_ENHANCEMENT_RESISTANCE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RESISTANCE);
			mPlugin.mEffectManager.addEffect(player, "IronTinctureEnhancementResistanceEffect", new PercentDamageReceived(duration, -resistance) {
				@Override
				public void onHurt(LivingEntity entity, DamageEvent event) {
					if (event.getType() == DamageEvent.DamageType.TRUE) {
						return;
					}
					if (entity instanceof Player player) {
						if (AbsorptionUtils.getAbsorption(player) > 0) {
							event.setDamage(event.getDamage() * (1 - resistance));
						}
					}
				}
			});
		}

		mCosmetic.pickupEffectsForPlayer(player, tinctureLocation);

	}
}
