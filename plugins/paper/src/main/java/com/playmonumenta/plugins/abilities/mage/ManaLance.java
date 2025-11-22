package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.ManaLanceCS;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ManaLance extends MultipleChargeAbility {

	private static final double DAMAGE_1 = 6.0f;
	private static final double DAMAGE_2 = 7.0f;
	private static final int COOLDOWN_1 = 5 * 20;
	private static final int COOLDOWN_2 = 3 * 20;
	private static final int RANGE = 8;
	private static final float KNOCKBACK = 0.25f;
	private static final double SIZE = 0.70f;

	public static final String CHARM_DAMAGE = "Mana Lance Damage";
	public static final String CHARM_COOLDOWN = "Mana Lance Cooldown";
	public static final String CHARM_RANGE = "Mana Lance Range";
	public static final String CHARM_CHARGES = "Mana Lance Charge";
	public static final String CHARM_SIZE = "Mana Lance Size";

	public static final AbilityInfo<ManaLance> INFO =
		new AbilityInfo<>(ManaLance.class, "Mana Lance", ManaLance::new)
			.linkedSpell(ClassAbility.MANA_LANCE)
			.scoreboardId("ManaLance")
			.shorthandName("ML")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Shoot a beam that damages mobs in its path.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ManaLance::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(false),
				AbilityTriggerInfo.HOLDING_MAGIC_WAND_RESTRICTION))
			.displayItem(Material.LIGHT_BLUE_CANDLE);

	private final double mDamage;
	private final double mRange;
	private final double mSize;
	private int mLastCastTicks = 0;

	private final ManaLanceCS mCosmetic;

	public ManaLance(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mMaxCharges = (isEnhanced() ? 2 : 1) + (int) CharmManager.getLevel(player, CHARM_CHARGES);
		mCharges = getTrackedCharges();
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mRange = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, RANGE);
		mSize = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SIZE, SIZE);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ManaLanceCS());
	}

	public boolean cast() {
		int ticks = Bukkit.getServer().getCurrentTick();
		// Prevent double casting on accident
		if (ticks - mLastCastTicks <= 5 || !consumeCharge()) {
			return false;
		}
		mLastCastTicks = ticks;

		float damage = SpellPower.getSpellDamage(mPlugin, mPlayer, (float) mDamage);

		Location startLoc = mPlayer.getEyeLocation();
		World world = mPlayer.getWorld();

		Location endLoc = LocationUtils.rayTraceToBlock(mPlayer, mRange, loc -> mCosmetic.lanceHitBlock(mPlayer, loc, world));

		boolean hit = false;
		for (LivingEntity mob : Hitbox.approximateCylinder(startLoc, endLoc, mSize, true).accuracy(0.5).getHitMobs()) {
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true);

			MovementUtils.knockAway(mPlayer.getLocation(), mob, KNOCKBACK, KNOCKBACK, true);

			if (!hit) {
				mCosmetic.lanceHit(LocationUtils.getHalfHeightLocation(mob), mPlayer);
				hit = true;
			}
		}

		mCosmetic.lanceParticle(mPlayer, startLoc, endLoc, mSize);
		mCosmetic.lanceSound(world, mPlayer, mPlayer.getLocation());

		return true;
	}

	private static Description<ManaLance> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to fire forth a piercing beam of Mana going ")
			.add(a -> a.mRange, RANGE)
			.add(" blocks, dealing ")
			.add(a -> a.mDamage, DAMAGE_1, false, Ability::isLevelOne)
			.add(" arcane magic damage to enemies in the path of the beam. This beam will not go through solid blocks.")
			.addCooldown(COOLDOWN_1, Ability::isLevelOne);
	}

	private static Description<ManaLance> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The damage is increased to ")
			.add(a -> a.mDamage, DAMAGE_2, false, Ability::isLevelTwo)
			.add(".")
			.addCooldown(COOLDOWN_2, Ability::isLevelTwo);
	}

	private static Description<ManaLance> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Mana Lance now has two charges.");
	}

}
