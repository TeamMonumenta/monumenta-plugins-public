package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.ArcaneStrikeCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.EnumSet;
import net.kyori.adventure.text.format.TextColor;
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

public class ArcaneStrike extends Ability {
	private static final float RADIUS = 4.0f;
	private static final int DAMAGE_1 = 4;
	private static final int DAMAGE_2 = 7;
	private static final double WAND_SCALING_1 = 0.1;
	private static final double WAND_SCALING_2 = 0.2;
	private static final int BONUS_DAMAGE_1 = 2;
	private static final int BONUS_DAMAGE_2 = 3;
	private static final double ENHANCEMENT_DAMAGE_MULTIPLIER = 1.15;
	private static final double ENHANCEMENT_WEAKNESS_POTENCY = 0.2;
	private static final int ENHANCEMENT_WEAKNESS_DURATION = Constants.TICKS_PER_SECOND * 8;
	private static final int COOLDOWN = 5 * 20;

	public static final String CHARM_DAMAGE = "Arcane Strike Damage";
	public static final String CHARM_RADIUS = "Arcane Strike Radius";
	public static final String CHARM_BONUS = "Arcane Strike Bonus Damage";
	public static final String CHARM_COOLDOWN = "Arcane Strike Cooldown";
	public static final String CHARM_WEAKEN_POTENCY = "Arcane Strike Weakness Potency";
	public static final String CHARM_WEAKEN_DURATION = "Arcane Strike Weakness Duration";

	public static final AbilityInfo<ArcaneStrike> INFO =
		new AbilityInfo<>(ArcaneStrike.class, "Arcane Strike", ArcaneStrike::new)
			.linkedSpell(ClassAbility.ARCANE_STRIKE)
			.scoreboardId("ArcaneStrike")
			.shorthandName("AS")
			.actionBarColor(TextColor.color(220, 147, 249))
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Attack an enemy with a wand to damage nearby mobs.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.GOLDEN_SWORD);

	private final double mDamageBonus;
	private final double mDamageBonusAffected;
	private final double mRadius;
	private final double mWandScaling;
	private final double mWeakenPotency;
	private final int mWeakenDuration;

	private final ArcaneStrikeCS mCosmetic;

	public ArcaneStrike(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageBonus = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mDamageBonusAffected = CharmManager.calculateFlatAndPercentValue(player, CHARM_BONUS, isLevelOne() ? BONUS_DAMAGE_1 : BONUS_DAMAGE_2);
		mRadius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RADIUS, RADIUS);
		mWandScaling = isLevelOne() ? WAND_SCALING_1 : WAND_SCALING_2;
		mWeakenPotency = ENHANCEMENT_WEAKNESS_POTENCY + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKEN_POTENCY);
		mWeakenDuration = CharmManager.getDuration(mPlayer, CHARM_WEAKEN_DURATION, ENHANCEMENT_WEAKNESS_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ArcaneStrikeCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE
			&& !isOnCooldown()
			&& mPlayer.getCooledAttackStrength(0) == 1
			&& mPlugin.mItemStatManager.getEnchantmentLevel(mPlayer, EnchantmentType.MAGIC_WAND) > 0) {
			putOnCooldown();

			Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(enemy), mRadius);
			for (LivingEntity mob : hitbox.getHitMobs()) {
				double preSpellPowerDamage = mDamageBonus;

				// Arcane Strike extra damage if on fire or slowed (but effect not applied this tick)
				if ((mob.getFireTicks() > 0
					&& !MetadataUtils.happenedThisTick(mob, Constants.ENTITY_COMBUST_NONCE_METAKEY, 0))
					|| (EntityUtils.isSlowed(mPlugin, mob)
					&& !MetadataUtils.happenedThisTick(mob, Constants.ENTITY_SLOWED_NONCE_METAKEY, 0))
					|| (mob.hasPotionEffect(PotionEffectType.SLOW)
					&& !MetadataUtils.happenedThisTick(mob, Constants.ENTITY_SLOWED_NONCE_METAKEY, 0))) {
					preSpellPowerDamage += mDamageBonusAffected;
				}

				// Base damage scaling from wands + fist damage
				final ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
				double attackDamageAdd = ItemStatUtils.getAttributeAmount(inMainHand, AttributeType.ATTACK_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND);
				preSpellPowerDamage += (attackDamageAdd + 1) * mWandScaling;

				float dmg = SpellPower.getSpellDamage(mPlugin, mPlayer, (float) preSpellPowerDamage);

				ClassAbility ability = ClassAbility.ARCANE_STRIKE;
				if (isEnhanced()) {
					dmg = (float) (dmg * ENHANCEMENT_DAMAGE_MULTIPLIER);
					if (mob != enemy) {
						ability = ClassAbility.ARCANE_STRIKE_ENHANCED;
					}
				}

				DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, dmg, ability, true, true);
			}

			Location enemyLoc = enemy.getLocation().add(0, 1, 0);
			World world = mPlayer.getWorld();
			Location loc = mPlayer.getLocation().add(mPlayer.getLocation().getDirection().multiply(0.5));

			mCosmetic.onStrike(mPlugin, mPlayer, world, enemyLoc, loc, mRadius);

			if (isEnhanced()) {
				// Weakness Effect
				EntityUtils.applyWeaken(mPlugin, mWeakenDuration, mWeakenPotency, enemy,
					EnumSet.of(DamageType.MELEE, DamageType.PROJECTILE, DamageType.MAGIC, DamageType.BLAST, DamageType.FIRE));

				//Visual feedback
				ItemStack item = mPlayer.getInventory().getItemInMainHand();

				//Get enchant levels on weapon
				int fire = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.FIRE_ASPECT);
				int ice = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.ICE_ASPECT);
				int thunder = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.THUNDER_ASPECT);
				int decay = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.DECAY);
				int bleed = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.BLEEDING);
				int wind = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.WIND_ASPECT);

				if (ice > 0) {
					mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.6f, 1.3f);
					new PartialParticle(Particle.SNOW_SHOVEL, loc, 20, mRadius, mRadius, mRadius).spawnAsPlayerActive(mPlayer);
				}
				if (thunder > 0) {
					mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.6f, 0.8f);
					new PartialParticle(Particle.REDSTONE, loc, 10, mRadius, mRadius, mRadius, new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f)).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, loc, 10, mRadius, mRadius, mRadius, new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f)).spawnAsPlayerActive(mPlayer);
				}
				if (decay > 0) {
					mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.4f, 0.7f);
					new PartialParticle(Particle.SQUID_INK, loc, 20, mRadius, mRadius, mRadius).spawnAsPlayerActive(mPlayer);
				}
				if (bleed > 0) {
					mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SLIME_SQUISH, SoundCategory.PLAYERS, 0.7f, 0.7f);
					new PartialParticle(Particle.REDSTONE, loc, 20, mRadius, mRadius, mRadius, new Particle.DustOptions(Color.fromRGB(210, 44, 44), 1.0f)).spawnAsPlayerActive(mPlayer);
				}
				if (wind > 0) {
					mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.0f, 0.30f);
					new PartialParticle(Particle.CLOUD, loc, 20, mRadius, mRadius, mRadius).spawnAsPlayerActive(mPlayer);
				}
				if (fire > 0) {
					mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_LAVA_POP, SoundCategory.PLAYERS, 0.6f, 0.9f);
					new PartialParticle(Particle.LAVA, loc, 20, mRadius, mRadius, mRadius).spawnAsPlayerActive(mPlayer);
				}
			}
		}
		return false;
	}

	private static Description<ArcaneStrike> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("When you attack an enemy with a wand, you unleash an arcane explosion dealing ")
			.add(a -> a.mDamageBonus, DAMAGE_1, false, Ability::isLevelOne)
			.add(" + ")
			.addPercent(a -> a.mWandScaling, WAND_SCALING_1)
			.add(" of your wand's base damage as arcane magic damage to all mobs within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks around the target. Enemies that are on fire or slowed take ")
			.add(a -> a.mDamageBonusAffected, BONUS_DAMAGE_1, false, Ability::isLevelOne)
			.add(" extra damage. Arcane Strike can not trigger Spellshock's static.")
			.addCooldown(COOLDOWN);
	}

	private static Description<ArcaneStrike> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The damage is increased to ")
			.add(a -> a.mDamageBonus, DAMAGE_2, false, Ability::isLevelTwo)
			.add(" + ")
			.addPercent(a -> a.mWandScaling, WAND_SCALING_2)
			.add(" of your wand's base damage. Mobs that are on fire or slowed take ")
			.add(a -> a.mDamageBonusAffected, BONUS_DAMAGE_2, false, Ability::isLevelTwo)
			.add(" additional damage.");
	}

	private static Description<ArcaneStrike> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The damage is increased by ")
			.addPercent(ENHANCEMENT_DAMAGE_MULTIPLIER - 1)
			.add(". Your enchantment on-hit effects are now also applied to all other enemies hit in the radius. Additionally, apply a ")
			.addPercent(a -> a.mWeakenPotency, ENHANCEMENT_WEAKNESS_POTENCY)
			.add(" weaken effect to the hit mob that lasts for ")
			.addDuration(a -> a.mWeakenDuration, ENHANCEMENT_WEAKNESS_DURATION)
			.add(" seconds and applies to melee, projectile, magic, blast, and fire damage.");
	}

}
