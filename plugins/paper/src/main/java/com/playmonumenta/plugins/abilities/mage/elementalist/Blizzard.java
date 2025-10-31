package com.playmonumenta.plugins.abilities.mage.elementalist;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.elementalist.BlizzardCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;


public class Blizzard extends Ability {
	public static final String NAME = "Blizzard";

	public static final int DAMAGE_1 = 3;
	public static final int DAMAGE_2 = 5;
	public static final int SIZE_1 = 7;
	public static final int SIZE_2 = 10;
	public static final double SLOW_MULTIPLIER_1 = 0.25;
	public static final double SLOW_MULTIPLIER_2 = 0.3;
	public static final int DAMAGE_INTERVAL = Constants.TICKS_PER_SECOND;
	public static final int SLOW_INTERVAL = (int) (0.5 * Constants.TICKS_PER_SECOND);
	public static final int DURATION_TICKS = 10 * Constants.TICKS_PER_SECOND;
	public static final int SLOW_TICKS = 5 * Constants.TICKS_PER_SECOND;
	public static final int COOLDOWN_TICKS = 30 * Constants.TICKS_PER_SECOND;

	public static final String CHARM_DAMAGE = "Blizzard Damage";
	public static final String CHARM_COOLDOWN = "Blizzard Cooldown";
	public static final String CHARM_RANGE = "Blizzard Range";
	public static final String CHARM_DURATION = "Blizzard Duration";
	public static final String CHARM_SLOW = "Blizzard Slowness Amplifier";
	public static final String CHARM_DELAY = "Blizzard Tick Delay";

	public static final AbilityInfo<Blizzard> INFO =
		new AbilityInfo<>(Blizzard.class, NAME, Blizzard::new)
			.linkedSpell(ClassAbility.BLIZZARD)
			.scoreboardId(NAME)
			.shorthandName("Bl")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("For a period of time, slightly damage and slow nearby mobs.")
			.cooldown(COOLDOWN_TICKS, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Blizzard::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true).lookDirections(AbilityTrigger.LookDirection.UP),
				AbilityTriggerInfo.HOLDING_MAGIC_WAND_RESTRICTION))
			.displayItem(Material.SNOWBALL);

	private final double mLevelDamage;
	private final double mLevelSize;
	private final double mLevelSlowMultiplier;
	private final int mDuration;
	private final int mTickDelay;

	private final BlizzardCS mCosmetic;

	public Blizzard(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mLevelDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mLevelSize = CharmManager.calculateFlatAndPercentValue(player, CHARM_RANGE, isLevelOne() ? SIZE_1 : SIZE_2);
		mLevelSlowMultiplier = (isLevelOne() ? SLOW_MULTIPLIER_1 : SLOW_MULTIPLIER_2) + CharmManager.getLevelPercentDecimal(player, CHARM_SLOW);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION_TICKS);
		mTickDelay = CharmManager.getDuration(mPlayer, CHARM_DELAY, DAMAGE_INTERVAL);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new BlizzardCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		World world = mPlayer.getWorld();
		mCosmetic.onCast(mPlayer, world, mPlayer.getLocation());

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
		float spellDamage = SpellPower.getSpellDamage(mPlugin, mPlayer, (float) mLevelDamage);

		cancelOnDeath(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				Location loc = mPlayer.getLocation();
				Hitbox hitbox = new Hitbox.UprightCylinderHitbox(LocationUtils.getHalfHeightLocation(mPlayer).add(0, -mLevelSize, 0), 2 * mLevelSize, mLevelSize);
				List<LivingEntity> mobs = hitbox.getHitMobs();
				mTicks++;
				if (mTicks % SLOW_INTERVAL == 0) {
					for (Player p : PlayerUtils.playersInRange(loc, mLevelSize, true)) {
						if (p.getFireTicks() > 1) {
							p.setFireTicks(1);
						}
					}

					for (LivingEntity mob : mobs) {
						EntityUtils.applySlow(mPlugin, SLOW_TICKS, mLevelSlowMultiplier, mob);
					}
				}

				if (mTicks % mTickDelay == 0) {
					for (LivingEntity mob : mobs) {
						DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), spellDamage, true, false, false);
					}
				}

				mCosmetic.tick(mPlayer, loc, mTicks, mLevelSize);

				if (mTicks >= mDuration) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));

		return true;
	}

	private static Description<Blizzard> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to create a storm of ice and snow that follows the player, dealing ")
			.add(a -> a.mLevelDamage, DAMAGE_1, false, Ability::isLevelOne)
			.add(" ice magic damage every second to all enemies within ")
			.add(a -> a.mLevelSize, SIZE_1, false, Ability::isLevelOne)
			.add(" blocks of you. The blizzard lasts for ")
			.addDuration(a -> a.mDuration, DURATION_TICKS)
			.add(" seconds, and chills enemies within it, slowing them by ")
			.addPercent(a -> a.mLevelSlowMultiplier, SLOW_MULTIPLIER_1, false, Ability::isLevelOne)
			.add(". Players in the blizzard are extinguished if they are on fire. This ability does not interact with Spellshock.")
			.addCooldown(COOLDOWN_TICKS);
	}

	private static Description<Blizzard> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Damage is increased to ")
			.add(a -> a.mLevelDamage, DAMAGE_2, false, Ability::isLevelTwo)
			.add(", aura size is increased to ")
			.add(a -> a.mLevelSize, SIZE_2, false, Ability::isLevelTwo)
			.add(" blocks, and slowness is increased to ")
			.addPercent(a -> a.mLevelSlowMultiplier, SLOW_MULTIPLIER_2, false, Ability::isLevelTwo)
			.add(".");
	}
}
