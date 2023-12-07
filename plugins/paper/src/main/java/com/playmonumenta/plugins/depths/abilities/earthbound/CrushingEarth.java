package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CrushingEarth extends DepthsAbility {

	public static final String ABILITY_NAME = "Crushing Earth";
	private static final int COOLDOWN = 20 * 8;
	private static final int[] DAMAGE = {8, 10, 12, 14, 16, 24};
	private static final int CAST_RANGE = 4;
	private static final int[] STUN_DURATION = {20, 25, 30, 35, 40, 50};

	public static final String CHARM_COOLDOWN = "Crushing Earth Cooldown";

	public static final DepthsAbilityInfo<CrushingEarth> INFO =
		new DepthsAbilityInfo<>(CrushingEarth.class, ABILITY_NAME, CrushingEarth::new, DepthsTree.EARTHBOUND, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.CRUSHING_EARTH)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CrushingEarth::cast, DepthsTrigger.RIGHT_CLICK))
			.displayItem(Material.SHIELD)
			.descriptions(CrushingEarth::getDescription);

	private final double mRange;
	private final double mDamage;
	private final int mStunDuration;

	public CrushingEarth(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRange = CharmManager.getRadius(mPlayer, CharmEffects.CRUSHING_EARTH_RANGE.mEffectName, CAST_RANGE);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.CRUSHING_EARTH_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mStunDuration = CharmManager.getDuration(mPlayer, CharmEffects.CRUSHING_EARTH_STUN_DURATION.mEffectName, STUN_DURATION[mRarity - 1]);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		LivingEntity mob = EntityUtils.getHostileEntityAtCursor(mPlayer, mRange);
		if (mob == null) {
			return;
		}

		Location mobLoc = mob.getEyeLocation();
		Location eyeLoc = mPlayer.getEyeLocation();
		World world = eyeLoc.getWorld();
		new PartialParticle(Particle.CRIT, mobLoc, 50, 0, 0.25, 0, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT_MAGIC, mobLoc, 50, 0, 0.25, 0, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPIT, mobLoc, 5, 0.15, 0.5, 0.15, 0).spawnAsPlayerActive(mPlayer);
		world.playSound(eyeLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.5f, 0.5f);
		world.playSound(eyeLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.5f, 1.0f);

		EntityUtils.applyStun(mPlugin, mStunDuration, mob);
		DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mDamage, mInfo.getLinkedSpell());

		putOnCooldown();
	}


	private static Description<CrushingEarth> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<CrushingEarth>(color)
			.add("Right click while looking at an enemy within ")
			.add(a -> a.mRange, CAST_RANGE)
			.add(" blocks to deal ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" melee damage and stun them for ")
			.addDuration(a -> a.mStunDuration, STUN_DURATION[rarity - 1], true)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}


}
