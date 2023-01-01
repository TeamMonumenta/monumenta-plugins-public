package com.playmonumenta.plugins.abilities.mage.elementalist;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;


public class Blizzard extends Ability {
	public static final String NAME = "Blizzard";

	public static final int DAMAGE_1 = 3;
	public static final int DAMAGE_2 = 5;
	public static final int SIZE_1 = 7;
	public static final int SIZE_2 = 10;
	public static final double SLOW_MULTIPLIER_1 = 0.25;
	public static final double SLOW_MULTIPLIER_2 = 0.3;
	public static final int DAMAGE_INTERVAL = 1 * Constants.TICKS_PER_SECOND;
	public static final int SLOW_INTERVAL = (int) (0.5 * Constants.TICKS_PER_SECOND);
	public static final int DURATION_TICKS = 10 * Constants.TICKS_PER_SECOND;
	public static final int SLOW_TICKS = 5 * Constants.TICKS_PER_SECOND;
	public static final int COOLDOWN_TICKS = 30 * Constants.TICKS_PER_SECOND;

	public static final String CHARM_DAMAGE = "Blizzard Damage";
	public static final String CHARM_COOLDOWN = "Blizzard Cooldown";
	public static final String CHARM_RANGE = "Blizzard Range";
	public static final String CHARM_DURATION = "Blizzard Duration";
	public static final String CHARM_SLOW = "Blizzard Slowness Amplifier";

	public static final AbilityInfo<Blizzard> INFO =
		new AbilityInfo<>(Blizzard.class, NAME, Blizzard::new)
			.linkedSpell(ClassAbility.BLIZZARD)
			.scoreboardId(NAME)
			.shorthandName("Bl")
			.descriptions(
				String.format("Right click while sneaking, looking upwards, and holding a wand to create a storm of ice and snow that follows the player, " +
					              "dealing %s ice magic damage every second to all enemies in a %s block radius around you. The blizzard lasts for %ss, and chills enemies within it, slowing them by %s%%." +
					              " Players in the blizzard are extinguished if they are on fire, and the ability's damage bypasses iframes. This ability does not interact with Spellshock. Cooldown: %ss.",
					DAMAGE_1,
					SIZE_1,
					DURATION_TICKS / 20,
					(int) (SLOW_MULTIPLIER_1 * 100),
					COOLDOWN_TICKS / 20),
				String.format("Damage is increased from %s to %s, aura size is increased from %s to %s blocks, slowness increased to %s%%.",
					DAMAGE_1,
					DAMAGE_2,
					SIZE_1,
					SIZE_2,
					(int) (SLOW_MULTIPLIER_2 * 100)))
			.cooldown(COOLDOWN_TICKS, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Blizzard::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true).lookDirections(AbilityTrigger.LookDirection.UP),
				AbilityTriggerInfo.HOLDING_MAGIC_WAND_RESTRICTION))
			.displayItem(new ItemStack(Material.SNOWBALL, 1));

	private final float mLevelDamage;
	private final float mLevelSize;
	private final double mLevelSlowMultiplier;

	public Blizzard(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mLevelDamage = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mLevelSize = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_RANGE, isLevelOne() ? SIZE_1 : SIZE_2);
		mLevelSlowMultiplier = (isLevelOne() ? SLOW_MULTIPLIER_1 : SLOW_MULTIPLIER_2) + CharmManager.getLevelPercentDecimal(player, CHARM_SLOW);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();
		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 2);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.75f);

		float spellDamage = SpellPower.getSpellDamage(mPlugin, mPlayer, mLevelDamage);
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

				if (mTicks % DAMAGE_INTERVAL == 0) {
					for (LivingEntity mob : mobs) {
						DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), spellDamage, false, false, false);
					}
				}

				new PartialParticle(Particle.SNOWBALL, loc, 6, 2, 2, 2, 0.1).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.CLOUD, loc, 4, 2, 2, 2, 0.05).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.CLOUD, loc, 3, 0.1, 0.1, 0.1, 0.15).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
				if (mTicks >= CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION_TICKS)
					    || mPlugin.mAbilityManager.getPlayerAbility(mPlayer, Blizzard.class) == null
					    || !mPlayer.isValid()) { // Ensure player is not dead, is still online?
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

}
