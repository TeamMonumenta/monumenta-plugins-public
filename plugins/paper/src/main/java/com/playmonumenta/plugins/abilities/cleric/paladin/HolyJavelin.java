package com.playmonumenta.plugins.abilities.cleric.paladin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


public class HolyJavelin extends Ability {
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 50), 1.0f);

	private static final int RANGE = 12;
	private static final int UNDEAD_DAMAGE_1 = 22;
	private static final int UNDEAD_DAMAGE_2 = 36;
	private static final int DAMAGE_1 = 11;
	private static final int DAMAGE_2 = 22;
	private static final int FIRE_DURATION = 5 * 20;
	private static final int COOLDOWN = 10 * 20;

	private final double mDamage;
	private final double mUndeadDamage;

	public static final String CHARM_DAMAGE = "Holy Javelin Damage";
	public static final String CHARM_COOLDOWN = "Holy Javelin Cooldown";
	public static final String CHARM_RANGE = "Holy Javelin Range";

	public static final AbilityInfo<HolyJavelin> INFO =
		new AbilityInfo<>(HolyJavelin.class, "Holy Javelin", HolyJavelin::new)
			.linkedSpell(ClassAbility.HOLY_JAVELIN)
			.scoreboardId("HolyJavelin")
			.shorthandName("HJ")
			.actionBarColor(TextColor.color(255, 255, 50))
			.descriptions(
				"While sprinting, left-clicking with a non-pickaxe throws a piercing spear of light, instantly travelling up to 12 blocks or until it hits a solid block. " +
					"It deals 22 magic damage to all undead enemies in a 0.75-block cube around it along its path, and 11 magic damage to non-undead, and sets them all on fire for 5s. Cooldown: 10s.",
				"Attacking an undead enemy with that left-click now transmits any passive Divine Justice and Luminous Infusion damage to other enemies pierced by the spear. " +
					"Damage is increased to 36 against undead, and to 22 against non-undead.")
			.simpleDescription("Throw a piercing spear of light that ignites and damages mobs.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", HolyJavelin::cast,
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(false).sprinting(true)
					.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.TRIDENT)
			.priorityAmount(1001); // shortly after divine justice and luminous infusion

	private @Nullable Crusade mCrusade;
	private @Nullable DivineJustice mDivineJustice;
	private @Nullable LuminousInfusion mLuminousInfusion;

	public HolyJavelin(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mUndeadDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? UNDEAD_DAMAGE_1 : UNDEAD_DAMAGE_2);

		Bukkit.getScheduler().runTask(plugin, () -> {
			mCrusade = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Crusade.class);
			mDivineJustice = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, DivineJustice.class);
			mLuminousInfusion = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, LuminousInfusion.class);
		});
	}

	public void cast() {
		execute(0, null);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE
			    && mCustomTriggers.get(0).check(mPlayer, AbilityTrigger.Key.LEFT_CLICK)) {
			double sharedPassiveDamage = 0;
			if (mLuminousInfusion != null) {
				sharedPassiveDamage += mLuminousInfusion.mLastPassiveMeleeDamage;
				sharedPassiveDamage += mLuminousInfusion.mLastPassiveDJDamage;
				mLuminousInfusion.mLastPassiveMeleeDamage = 0;
				mLuminousInfusion.mLastPassiveDJDamage = 0;
			}
			if (mDivineJustice != null) {
				sharedPassiveDamage += mDivineJustice.mLastPassiveDamage;
				mDivineJustice.mLastPassiveDamage = 0;
			}
			execute(sharedPassiveDamage, enemy);
		}
		return false;
	}

	public void execute(double bonusDamage, @Nullable LivingEntity triggeringEnemy) {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();
		double range = CharmManager.getRadius(mPlayer, CHARM_RANGE, RANGE);

		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1, 1.75f);
		world.playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1, 0.9f);
		Location startLoc = mPlayer.getEyeLocation();
		Vector dir = startLoc.getDirection();
		new PartialParticle(Particle.EXPLOSION_NORMAL, startLoc.clone().add(dir), 10, 0, 0, 0, 0.125f).spawnAsPlayerActive(mPlayer);

		Location endLoc = LocationUtils.rayTraceToBlock(mPlayer, range, loc -> {
			new PartialParticle(Particle.CLOUD, loc, 30, 0, 0, 0, 0.125f).spawnAsPlayerActive(mPlayer);
			world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 1.65f);
			world.playSound(loc, Sound.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 1, 0.9f);
		});

		new PPLine(Particle.EXPLOSION_NORMAL, startLoc, endLoc).shiftStart(0.75).countPerMeter(2).minParticlesPerMeter(0).delta(0).extra(0.025).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, startLoc, endLoc).shiftStart(0.75).countPerMeter(22).delta(0.25).data(COLOR).spawnAsPlayerActive(mPlayer);

		for (LivingEntity enemy : Hitbox.approximateCylinder(startLoc, endLoc, 0.95, true).accuracy(0.5).getHitMobs()) {
			double damage = Crusade.enemyTriggersAbilities(enemy, mCrusade) ? mUndeadDamage : mDamage;
			if (enemy != triggeringEnemy) {
				// Triggering enemy would've already received the melee damage from Luminous Infusion
				damage += bonusDamage;
			}
			EntityUtils.applyFire(mPlugin, FIRE_DURATION, enemy, mPlayer);
			DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true);
		}
	}
}
