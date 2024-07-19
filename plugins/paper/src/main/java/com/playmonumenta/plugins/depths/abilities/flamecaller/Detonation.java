package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.shadow.DummyDecoy;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class Detonation extends DepthsAbility {

	public static final String ABILITY_NAME = "Detonation";
	public static final double[] DAMAGE = {2.0, 2.5, 3.0, 3.5, 4.0, 5.0};
	public static final int DEATH_RADIUS = 8;
	public static final int DAMAGE_RADIUS = 2;

	public static final DepthsAbilityInfo<Detonation> INFO =
		new DepthsAbilityInfo<>(Detonation.class, ABILITY_NAME, Detonation::new, DepthsTree.FLAMECALLER, DepthsTrigger.PASSIVE)
			.linkedSpell(ClassAbility.DETONATION)
			.displayItem(Material.TNT)
			.descriptions(Detonation::getDescription)
			.singleCharm(false);

	private final double mDamageRadius;
	private final double mDamage;
	private final double mDeathRadius;

	public Detonation(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageRadius = CharmManager.getRadius(mPlayer, CharmEffects.DETONATION_DAMAGE_RADIUS.mEffectName, DAMAGE_RADIUS);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.DETONATION_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mDeathRadius = CharmManager.getRadius(mPlayer, CharmEffects.DETONATION_DEATH_RADIUS.mEffectName, DEATH_RADIUS);
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		Entity entity = event.getEntity();
		if (entity.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG) && !DummyDecoy.DUMMY_NAME.equals(entity.getName())) {
			return;
		}
		Location location = entity.getLocation();
		World world = mPlayer.getWorld();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(location, mDamageRadius)) {
			new PartialParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, mob.getLocation().add(0, 1, 0), 2).spawnAsPlayerActive(mPlayer);
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true, false);
		}
		new PartialParticle(Particle.EXPLOSION_LARGE, location.add(0, 0.5, 0), 1).minimumCount(1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, location.add(0, 1, 0), 3).spawnAsPlayerActive(mPlayer);
		world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5f, 1);
	}

	@Override
	public double entityDeathRadius() {
		return mDeathRadius;
	}

	private static Description<Detonation> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Detonation>(color)
			.add("When an enemy dies within ")
			.add(a -> a.mDeathRadius, DEATH_RADIUS)
			.add(" blocks of you, it explodes, dealing ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage to other enemies within ")
			.add(a -> a.mDamageRadius, DAMAGE_RADIUS)
			.add(" blocks.");
	}
}
