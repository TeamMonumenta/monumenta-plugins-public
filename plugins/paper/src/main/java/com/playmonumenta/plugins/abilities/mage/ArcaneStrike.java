package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class ArcaneStrike extends Ability {

	private static final Particle.DustOptions COLOR_1 = new Particle.DustOptions(Color.fromRGB(220, 147, 249), 1.0f);
	private static final Particle.DustOptions COLOR_2 = new Particle.DustOptions(Color.fromRGB(217, 122, 255), 1.0f);

	private static final float RADIUS = 4.0f;
	private static final int DAMAGE_1 = 4;
	private static final int DAMAGE_2 = 7;
	private static final int BONUS_DAMAGE_1 = 2;
	private static final int BONUS_DAMAGE_2 = 3;
	private static final double ENHANCEMENT_DAMAGE_MULTIPLIER = 1.3;
	private static final int COOLDOWN = 5 * 20;

	public static final String CHARM_DAMAGE = "Arcane Strike Damage";
	public static final String CHARM_RADIUS = "Arcane Strike Radius";
	public static final String CHARM_BONUS = "Arcane Strike Bonus Damage";
	public static final String CHARM_COOLDOWN = "Arcane Strike Cooldown";

	public static final AbilityInfo<ArcaneStrike> INFO =
		new AbilityInfo<>(ArcaneStrike.class, "Arcane Strike", ArcaneStrike::new)
			.linkedSpell(ClassAbility.ARCANE_STRIKE)
			.scoreboardId("ArcaneStrike")
			.shorthandName("AS")
			.descriptions(
				String.format("When you attack an enemy with a wand, you unleash an arcane explosion dealing %s arcane magic damage to all mobs in a %s block radius around the target. " +
					              "Enemies that are on fire or slowed take %s extra damage. Arcane Strike can not trigger Spellshock's static. Cooldown: %ss.",
					DAMAGE_1,
					(int) RADIUS,
					BONUS_DAMAGE_1,
					COOLDOWN / 20
				),
				String.format("The damage is increased to %s. Mobs that are on fire or slowed take %s additional damage.",
					DAMAGE_2,
					BONUS_DAMAGE_2),
				"Your enchantment on-hit effects are now also applied to all other enemies hit in the radius. Also this skill's damage is increased by 30%.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(new ItemStack(Material.GOLDEN_SWORD, 1));

	private final float mDamageBonus;
	private final float mDamageBonusAffected;

	public ArcaneStrike(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageBonus = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mDamageBonusAffected = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_BONUS, isLevelOne() ? BONUS_DAMAGE_1 : BONUS_DAMAGE_2);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE
			    && !isOnCooldown()
			    && mPlayer.getCooledAttackStrength(0) == 1
			    && mPlugin.mItemStatManager.getPlayerItemStats(mPlayer).getItemStats().get(ItemStatUtils.EnchantmentType.MAGIC_WAND) > 0) {
			putOnCooldown();

			Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(enemy), CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RADIUS, RADIUS));
			for (LivingEntity mob : hitbox.getHitMobs()) {
				float preSpellPowerDamage = mDamageBonus;

				// Arcane Strike extra damage if on fire or slowed (but effect not applied this tick)
				if (EntityUtils.isSlowed(mPlugin, mob) || (mob.hasPotionEffect(PotionEffectType.SLOW)
					                                           && !MetadataUtils.happenedThisTick(mob, Constants.ENTITY_SLOWED_NONCE_METAKEY, 0))
					    || (mob.getFireTicks() > 0
						        && !MetadataUtils.happenedThisTick(mob, Constants.ENTITY_COMBUST_NONCE_METAKEY, 0))) {
					preSpellPowerDamage += mDamageBonusAffected;
				}

				float dmg = SpellPower.getSpellDamage(mPlugin, mPlayer, preSpellPowerDamage);

				ClassAbility ability = ClassAbility.ARCANE_STRIKE;
				if (isEnhanced()) {
					dmg = (float) (dmg * ENHANCEMENT_DAMAGE_MULTIPLIER);
					if (mob != enemy) {
						ability = ClassAbility.ARCANE_STRIKE_ENHANCED;
					}
				}

				DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, dmg, ability, true, true);

			}

			Location locD = enemy.getLocation().add(0, 1, 0);
			World world = mPlayer.getWorld();
			new PartialParticle(Particle.DRAGON_BREATH, locD, 75, 0, 0, 0, 0.25).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.EXPLOSION_NORMAL, locD, 35, 0, 0, 0, 0.2).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SPELL_WITCH, locD, 150, 2.5, 1, 2.5, 0.001).spawnAsPlayerActive(mPlayer);
			world.playSound(locD, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 0.75f, 1.5f);

			Location loc = mPlayer.getLocation().add(mPlayer.getLocation().getDirection().multiply(0.5));
			world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.75f, 1.65f);
			world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.75f, 0.5f);

			if (isEnhanced()) {
				//Visual feedback
				ItemStack item = mPlayer.getItemInHand();
				if (item == null) {
					return false;
				}

				//Get enchant levels on weapon
				ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.FIRE_ASPECT);
				int fire = ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.FIRE_ASPECT);
				int ice = ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.ICE_ASPECT);
				int thunder = ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.THUNDER_ASPECT);
				int decay = ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.DECAY);
				int bleed = ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.BLEEDING);
				int wind = ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.WIND_ASPECT);

				double radius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RADIUS, RADIUS);

				if (ice > 0) {
					mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.6f, 1.3f);
					new PartialParticle(Particle.SNOW_SHOVEL, loc, 20, radius, radius, radius).spawnAsPlayerActive(mPlayer);
				}
				if (thunder > 0) {
					mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.6f, 0.8f);
					new PartialParticle(Particle.REDSTONE, loc, 10, radius, radius, radius, new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f)).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, loc, 10, radius, radius, radius, new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f)).spawnAsPlayerActive(mPlayer);
				}
				if (decay > 0) {
					mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.4f, 0.7f);
					new PartialParticle(Particle.SQUID_INK, loc, 20, radius, radius, radius).spawnAsPlayerActive(mPlayer);
				}
				if (bleed > 0) {
					mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SLIME_SQUISH, SoundCategory.PLAYERS, 0.7f, 0.7f);
					new PartialParticle(Particle.REDSTONE, loc, 20, radius, radius, radius, new Particle.DustOptions(Color.fromRGB(210, 44, 44), 1.0f)).spawnAsPlayerActive(mPlayer);
				}
				if (wind > 0) {
					mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.0f, 0.30f);
					mPlayer.getWorld().spawnParticle(Particle.CLOUD, loc, 20, radius, radius, radius);
				}
				if (fire > 0) {
					mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_LAVA_POP, SoundCategory.PLAYERS, 0.6f, 0.9f);
					mPlayer.getWorld().spawnParticle(Particle.LAVA, loc, 20, radius, radius, radius);
				}
			}

			new BukkitRunnable() {
				double mD = 30;

				@Override
				public void run() {
					Vector vec;
					for (double degree = mD; degree < mD + 30; degree += 8) {
						double radian1 = Math.toRadians(degree);
						double cos = FastUtils.cos(radian1);
						double sin = FastUtils.sin(radian1);
						for (double r = 1; r < 4; r += 0.5) {
							vec = new Vector(cos * r, 1, sin * r);
							vec = VectorUtils.rotateXAxis(vec, loc.getPitch());
							vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

							Location l = loc.clone().add(vec);
							new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, COLOR_1).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, COLOR_2).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
						}
					}
					mD += 30;
					if (mD >= 150) {
						this.cancel();
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
		return false;
	}

}
