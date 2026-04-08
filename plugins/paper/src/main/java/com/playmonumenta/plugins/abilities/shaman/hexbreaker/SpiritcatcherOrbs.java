package com.playmonumenta.plugins.abilities.shaman.hexbreaker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.hexbreaker.SpiritcatcherOrbsCS;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.SpiritcatcherOrbsSpiritflames;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.utils.DescriptionUtils.DARK_GREY;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;
import static com.playmonumenta.plugins.utils.EntityUtils.VULNERABILITY_EFFECT_NAME;

public class SpiritcatcherOrbs extends Ability implements AbilityWithChargesOrStacks {
	private static final int ORB_MAX_STACKS = 12;
	private static final int ORB_COUNT = 3;
	private static final double ORB_VELOCITY = 1;
	private static final int ORB_LIFETIME = 12 * 20;
	private static final int ORB_STACK_DECAY_TIME = 8 * 20;
	private static final int SPIRITFLAME_DURATION_1 = 20 * 4;
	private static final int SPIRITFLAME_DURATION_2 = 20 * 6;
	private static final double SPIRITFLAME_RANGE = 12;
	private static final double SPIRITFLAME_DAMAGE_1 = 5;
	private static final double SPIRITFLAME_DAMAGE_2 = 6;
	private static final double SPIRITFLAME_MAGIC_VULN = 0.1;
	private static final double SPIRITFLAME_WEAKEN = 0.2;
	private static final double IMBUED_DAMAGE_BONUS = 0.2;
	private static final String SPIRITFLAME_EFFECT_NAME = "Spiritcatcher Orbs Spiritflame";
	private static final EnumSet<DamageEvent.DamageType> WEAKEN_DAMAGE_TYPES = EnumSet.of(
		DamageEvent.DamageType.MELEE,
		DamageEvent.DamageType.PROJECTILE,
		DamageEvent.DamageType.MAGIC,
		DamageEvent.DamageType.BLAST,
		DamageEvent.DamageType.FIRE
	);

	public static final String CHARM_COUNT = "Spiritcatcher Orbs Orb Count";
	public static final String CHARM_VELOCITY = "Spiritcatcher Orbs Orb Velocity";
	public static final String CHARM_LIFETIME = "Spiritcatcher Orbs Orb Lifetime";
	public static final String CHARM_SPIRITFLAME_DURATION = "Spiritcatcher Orbs Spiritflame Duration";
	public static final String CHARM_SPIRITFLAME_RANGE = "Spiritcatcher Orbs Spiritflame Range";
	public static final String CHARM_SPIRITFLAME_DAMAGE = "Spiritcatcher Orbs Spiritflame Damage";
	public static final String CHARM_SPIRITFLAME_VULN = "Spiritcatcher Orbs Spiritflame Magic Vulnerability Amplifier";
	public static final String CHARM_SPIRITFLAME_WEAKEN = "Spiritcatcher Orbs Spiritflame Weakness Amplifier";
	public static final String CHARM_MAX_STACKS = "Spiritcatcher Orbs Max Stacks";
	public static final String CHARM_STACK_DECAY_TIME = "Spiritcatcher Orbs Stack Decay Time";
	public static final String CHARM_IMBUED_DAMAGE_BONUS = "Spiritcatcher Orbs Imbuement Damage Bonus Amplifier";

	public static final Style SPIRITFLAME_COLOR = Style.style(TextColor.color(0x2BA1AF));

	public static final AbilityInfo<SpiritcatcherOrbs> INFO =
		new AbilityInfo<>(SpiritcatcherOrbs.class, "Spiritcatcher Orbs", SpiritcatcherOrbs::new)
			.linkedSpell(ClassAbility.SPIRITCATCHER_ORBS)
			.scoreboardId("SpiritcatcherOrbs")
			.shorthandName("SO")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Your totems burst into Spiritcatcher Orbs which bind to and debuff attacked mobs.")
			.displayItem(Material.SOUL_LANTERN);

	private final int mMaxOrbStacks;
	private final int mDecayDuration;
	private final int mOrbCount;
	private final double mOrbVelocity;
	private final int mLifetime;
	private final int mFlameDuration;
	private final double mFlameRange;
	private final double mFlameDamage;
	private final double mFlameVuln;
	private final double mFlameWeaken;
	private final double mImbuedDamageBonus;
	private final SpiritcatcherOrbsCS mCosmetic;

	private int mOrbStacks = 0;
	private int mDecayTimer = 0;
	private final List<Item> mActiveOrbs = new ArrayList<>();

	public SpiritcatcherOrbs(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mOrbCount = ORB_COUNT + (int) CharmManager.getLevel(player, CHARM_COUNT);
		mOrbVelocity = CharmManager.calculateFlatAndPercentValue(player, CHARM_VELOCITY, ORB_VELOCITY);
		mLifetime = CharmManager.getDuration(player, CHARM_LIFETIME, ORB_LIFETIME);
		mFlameDuration = CharmManager.getDuration(player, CHARM_SPIRITFLAME_DURATION, isLevelOne() ? SPIRITFLAME_DURATION_1 : SPIRITFLAME_DURATION_2);
		mFlameRange = CharmManager.calculateFlatAndPercentValue(player, CHARM_SPIRITFLAME_RANGE, SPIRITFLAME_RANGE);
		mFlameDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_SPIRITFLAME_DAMAGE, isLevelOne() ? SPIRITFLAME_DAMAGE_1 : SPIRITFLAME_DAMAGE_2);
		mFlameVuln = isLevelOne() ? 0 : (SPIRITFLAME_MAGIC_VULN + CharmManager.getLevelPercentDecimal(player, CHARM_SPIRITFLAME_VULN));
		mFlameWeaken = isLevelOne() ? 0 : (SPIRITFLAME_WEAKEN + CharmManager.getLevelPercentDecimal(player, CHARM_SPIRITFLAME_WEAKEN));
		mImbuedDamageBonus = IMBUED_DAMAGE_BONUS + CharmManager.getLevelPercentDecimal(player, CHARM_IMBUED_DAMAGE_BONUS);
		mMaxOrbStacks = ORB_MAX_STACKS + (int) CharmManager.getLevel(player, CHARM_MAX_STACKS);
		mDecayDuration = CharmManager.getDuration(player, CHARM_STACK_DECAY_TIME, ORB_STACK_DECAY_TIME);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new SpiritcatcherOrbsCS());
	}

	public void summonOrbs(Location standLocation) {
		mCosmetic.createOrbs(standLocation);

		double angleInterval = 2 * Math.PI / mOrbCount;
		double randomAngle = FastUtils.randomDoubleInRange(0, angleInterval);
		for (int i = 0; i < mOrbCount; i++) {
			double angle = randomAngle + i * angleInterval;
			double velocity = 0.2 * mOrbVelocity;
			Vector velocityVector = new Vector(velocity * Math.sin(angle), 0.3, velocity * Math.cos(angle));

			summonOrb(standLocation.getWorld(), standLocation, velocityVector);
		}
	}

	private void summonOrb(World world, Location location, Vector velocity) {
		Item orb = AbilityUtils.spawnAbilityItem(world, location, mCosmetic.getOrbItemMaterial(), mCosmetic.getOrbName(), true, 0, true, true);
		mActiveOrbs.add(orb);
		orb.setVelocity(velocity);

		cancelOnDeath(new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT++;
				Location itemLoc = orb.getLocation();
				mCosmetic.orbTick(mT, itemLoc, mPlayer);
				if (!new Hitbox.UprightCylinderHitbox(itemLoc, 0.7, 0.7).getHitPlayers(true).isEmpty()) {
					incrementStack();
					mActiveOrbs.remove(orb);
					orb.remove();
					mCosmetic.orbPickup(itemLoc, mPlayer);
					this.cancel();
					return;
				}

				if (mT >= mLifetime || orb.isDead()) {
					mActiveOrbs.remove(orb);
					orb.remove();
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1));
	}

	@Override
	public boolean blockBreakEvent(final BlockBreakEvent event) {
		if (event.getBlock().getType() == Material.SPAWNER) {
			for (Item orb : mActiveOrbs) {
				incrementStack();
				orb.remove();
				mCosmetic.orbPickup(orb.getLocation(), mPlayer);
			}
		}
		return true;
	}

	private void incrementStack() {
		if (mOrbStacks < mMaxOrbStacks) {
			mOrbStacks++;
		}
		mDecayTimer = 0;

		ClientModHandler.updateAbility(mPlayer, this);
		showChargesMessage();
	}

	private void decrementStack() {
		if (mOrbStacks <= 0) {
			return;
		}
		mOrbStacks--;
		mDecayTimer = 0;

		ClientModHandler.updateAbility(mPlayer, this);
		showChargesMessage();
	}

	@Override
	public boolean onDamage(final DamageEvent event, final LivingEntity enemy) {
		if (getCharges() <= 0) {
			return false;
		}

		boolean meleeAttack = event.getType() == DamageEvent.DamageType.MELEE;
		boolean projectileAttack = event.getDamager() instanceof Projectile projectile && EntityUtils.isAbilityTriggeringProjectile(projectile, false);
		if (!meleeAttack && !projectileAttack) {
			return false;
		}

		if (enemy.isDead() || !MetadataUtils.checkOnceThisTick(mPlugin, enemy, "SpiritflameAppliedThisTick")) {
			return false;
		}

		decrementStack();
		mCosmetic.spiritflamesApplied(enemy, mPlayer);
		event.updateDamageWithMultiplier(1 + mImbuedDamageBonus);
		if (mPlugin.mEffectManager.getActiveEffect(enemy, SPIRITFLAME_EFFECT_NAME) instanceof SpiritcatcherOrbsSpiritflames flames) {
			flames.extendDuration(mFlameDuration);
		}
		else {
			mPlugin.mEffectManager.addEffect(enemy, SPIRITFLAME_EFFECT_NAME, new SpiritcatcherOrbsSpiritflames(mFlameDuration, mFlameDamage, mFlameRange, mPlayer, ClassAbility.SPIRITCATCHER_ORBS, mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer), mCosmetic));
		}
		if (isLevelTwo()) {
			EntityUtils.applyWeaken(mPlugin, mFlameDuration, mFlameWeaken, enemy, WEAKEN_DAMAGE_TYPES);
			mPlugin.mEffectManager.addEffect(enemy, VULNERABILITY_EFFECT_NAME, new PercentDamageReceived(mFlameDuration, mFlameVuln, EnumSet.of(DamageEvent.DamageType.MAGIC)));
		}

		return false;
	}

	@Override
	public void playerDeathEvent(PlayerDeathEvent event) {
		for (Item orb : mActiveOrbs) {
			orb.remove();
		}
		mActiveOrbs.clear();
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mOrbStacks <= 0) {
			return;
		}
		mDecayTimer += 5;

		if (mDecayTimer >= mDecayDuration) {
			decrementStack();
		}
	}

	@Override
	public int getCharges() {
		return mOrbStacks;
	}

	@Override
	public int getMaxCharges() {
		return mMaxOrbStacks;
	}

	@Override
	public @Nullable Component getHotbarMessage() {
		TextColor color = INFO.getActionBarColor();
		String name = INFO.getHotbarName();

		int charges = getCharges();
		int maxCharges = getMaxCharges();

		// String output.
		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text(name != null ? name : "Error", color))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		output = output.append(Component.text(charges + "/" + maxCharges, (charges == 0 ? NamedTextColor.GRAY : (charges >= maxCharges ? NamedTextColor.GREEN : NamedTextColor.YELLOW))));

		return output;
	}

	private static Description<SpiritcatcherOrbs> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("When a *Totem* expires or is destroyed, it leaves").styles(Shaman.TOTEM_COLOR)
			.addLine("behind %d orbs that can be picked up by players.")
				.statValues(stat(a -> a.mOrbCount, ORB_COUNT))
			.addLine("Breaking a spawner picks up all orbs.")
			.addLine()
			.addLine("You can hold up to %d orbs at once, and they")
				.statValues(stat(a -> a.mMaxOrbStacks, ORB_MAX_STACKS))
			.addLine("begin to decay after %t of not gaining any.")
				.statValues(stat(a -> a.mDecayDuration, ORB_STACK_DECAY_TIME))
			.addLine()
			.addLine("Your attacks and projectiles consume *1* orb").styles(WHITE)
			.addLine("to deal more damage and afflict the target")
			.addLine("with *Spiritflame*.").styles(SPIRITFLAME_COLOR)
				.statValues(stat(a -> a.mFlameDuration, SPIRITFLAME_DURATION_1))
			.addLine()
			.addStat("Damage Boost: +%p (m/p)")
			.statValues(stat(a -> a.mImbuedDamageBonus, IMBUED_DAMAGE_BONUS))
			.addLine()
			.addLine("*Spiritflame* deals damage over time to the mob and").styles(SPIRITFLAME_COLOR)
			.addLine("nearby mobs with the same name. If the mob dies, the")
			.addLine("*Spiritflame* continues to burn where it died.").styles(SPIRITFLAME_COLOR)
			.addLine()
			.addStat("Damage: %d1 (s) every 1s")
				.statValues(stat(a -> a.mFlameDamage, SPIRITFLAME_DAMAGE_1))
			.addStat("Radius: %r")
				.statValues(stat(a -> a.mFlameRange, SPIRITFLAME_RANGE))
			.addStat("Duration: %t1")
				.statValues(stat(a -> a.mFlameDuration, SPIRITFLAME_DURATION_1))
			.addDashedLine();
	}

	private static Description<SpiritcatcherOrbs> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase the damage and duration of *Spiritflame*.").styles(SPIRITFLAME_COLOR)
			.addLine()
			.addStatComparison("Damage: %d1 -> %d2 (s) every 1s")
				.statValues(stat(SPIRITFLAME_DAMAGE_1), stat(a -> a.mFlameDamage, SPIRITFLAME_DAMAGE_2))
			.addStatComparison("Duration: %t1 -> %t2")
				.statValues(stat(SPIRITFLAME_DURATION_1), stat(a -> a.mFlameDuration, SPIRITFLAME_DURATION_2))
			.addLine()
			.addLine("*Spiritflame* now weakens the afflicted mob and").styles(SPIRITFLAME_COLOR)
			.addLine("makes them take increased magic damage.")
			.addLine("*(Weakens all damage types, instead of*").styles(DARK_GREY)
			.addLine("*only melee or projectile damage)*").styles(DARK_GREY)
			.addLine()
			.addStat("Effect: %p Weakness")
				.statValues(stat(a -> a.mFlameWeaken, SPIRITFLAME_WEAKEN))
			.addStat("Effect: %p Magic Vulnerability")
				.statValues(stat(a -> a.mFlameVuln, SPIRITFLAME_MAGIC_VULN))
			.addDashedLine();
	}
}
