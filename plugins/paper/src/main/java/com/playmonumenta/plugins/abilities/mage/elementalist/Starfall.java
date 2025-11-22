package com.playmonumenta.plugins.abilities.mage.elementalist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.elementalist.StarfallCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Starfall extends Ability {
	public static final String NAME = "Starfall";
	public static final ClassAbility ABILITY = ClassAbility.STARFALL;

	public static final int DAMAGE_1 = 15;
	public static final int DAMAGE_2 = 27;
	public static final int SIZE = 6;
	public static final int DISTANCE = 25;
	public static final int FIRE_TICKS = 5 * 20;
	public static final float KNOCKBACK = 0.7f;
	public static final int COOLDOWN_TICKS = 18 * 20;
	public static final double FALL_INCREMENT = 0.25;

	public static final String CHARM_DAMAGE = "Starfall Damage";
	public static final String CHARM_RANGE = "Starfall Range";
	public static final String CHARM_COOLDOWN = "Starfall Cooldown";
	public static final String CHARM_RADIUS = "Starfall Radius";
	public static final String CHARM_FIRE = "Starfall Fire Duration";
	public static final String CHARM_FALL_SPEED = "Starfall Fall Speed";

	public static final AbilityInfo<Starfall> INFO =
		new AbilityInfo<>(Starfall.class, NAME, Starfall::new)
			.linkedSpell(ABILITY)
			.scoreboardId(NAME)
			.shorthandName("SF")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Summon a meteor, which upon impact damages and ignites mobs.")
			.cooldown(COOLDOWN_TICKS, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Starfall::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false),
				AbilityTriggerInfo.HOLDING_MAGIC_WAND_RESTRICTION))
			.displayItem(Material.MAGMA_BLOCK);

	private final double mLevelDamage;
	private final double mDistance;
	private final double mRadius;
	private final int mFireDuration;
	private final StarfallCS mCosmetic;

	public Starfall(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mLevelDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mDistance = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, DISTANCE);
		mRadius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RADIUS, SIZE);
		mFireDuration = CharmManager.getDuration(mPlayer, CHARM_FIRE, FIRE_TICKS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new StarfallCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		World world = mPlayer.getWorld();

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
		float damage = SpellPower.getSpellDamage(mPlugin, mPlayer, (float) mLevelDamage);
		mCosmetic.starfallCastEffect(world, mPlayer, mPlayer.getLocation());
		Vector dir = loc.getDirection().normalize();
		int dist = (int) Math.ceil(mDistance);
		for (int i = 0; i < dist; i++) {
			loc.add(dir);

			mCosmetic.starfallCastTrail(loc, mPlayer);
			int size = EntityUtils.getNearbyMobs(loc, 2, mPlayer).size();
			if (!loc.isChunkLoaded() || loc.getBlock().getType().isSolid() || i >= dist - 1 || size > 0) {
				break;
			}
		}
		launchMeteor(loc, mPlayer.getLocation(), playerItemStats, damage);

		return true;
	}

	private void launchMeteor(final Location loc, final Location ogPlayerLoc, final ItemStatManager.PlayerItemStats playerItemStats, final float damage) {
		Location ogLoc = loc.clone();
		loc.add(0, 40, 0);

		new BukkitRunnable() {
			double mT = 0;

			@Override
			public void run() {
				mT += 1;
				World world = mPlayer.getWorld();
				for (int i = 0; i < 8; i++) {
					loc.subtract(0, CharmManager.getExtraPercent(mPlayer, CHARM_FALL_SPEED, FALL_INCREMENT), 0);
					if (!loc.isChunkLoaded() || loc.getBlock().getType().isSolid()) {
						if (loc.getY() - ogLoc.getY() <= 2) {
							mCosmetic.starfallLandEffect(world, mPlayer, loc, ogPlayerLoc, mRadius);
							this.cancel();
							Hitbox hitbox = new Hitbox.SphereHitbox(loc, mRadius);
							for (LivingEntity e : hitbox.getHitMobs()) {
								EntityUtils.applyFire(mPlugin, mFireDuration, e, mPlayer, playerItemStats);
								DamageUtils.damage(mPlayer, e, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), damage, true, true, false);
								MovementUtils.knockAway(loc, e, KNOCKBACK, true);
							}
							break;
						}
					}
				}
				mCosmetic.starfallFallEffect(world, mPlayer, loc, ogPlayerLoc, ogLoc, mT);

				if (mT >= 50) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private static Description<Starfall> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to mark where you're looking, up to ")
			.add(a -> a.mDistance, DISTANCE)
			.add(" blocks away. You summon a falling meteor above the mark that lands strongly, dealing ")
			.add(a -> a.mLevelDamage, DAMAGE_1, false, Ability::isLevelOne)
			.add(" fire magic damage to all enemies within ")
			.add(a -> a.mRadius, SIZE)
			.add(" blocks around it, setting them on fire for ")
			.addDuration(a -> a.mFireDuration, FIRE_TICKS)
			.add("s , and knocking them away.")
			.addCooldown(COOLDOWN_TICKS);
	}

	private static Description<Starfall> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Damage is increased to ")
			.add(a -> a.mLevelDamage, DAMAGE_2, false, Ability::isLevelTwo)
			.add(".");
	}
}
