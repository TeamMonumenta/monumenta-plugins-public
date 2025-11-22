package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.DepthsBrittle;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
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
import org.jetbrains.annotations.Nullable;

public class Entrench extends DepthsAbility {
	public static final String ABILITY_NAME = "Entrench";
	public static final int[] DURATION = {30, 35, 40, 45, 50, 70};
	public static final int COOLDOWN = 18 * 20;
	public static final int RADIUS = 6;
	public static final double SLOW_MODIFIER = 0.99;
	public static final float KNOCKBACK_SPEED = 2f;
	public static final String CHARM_COOLDOWN = "Entrench Cooldown";

	public static final DepthsAbilityInfo<Entrench> INFO =
		new DepthsAbilityInfo<>(Entrench.class, ABILITY_NAME, Entrench::new, DepthsTree.EARTHBOUND, DepthsTrigger.WILDCARD)
			.linkedSpell(ClassAbility.ENTRENCH)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.SOUL_SAND)
			.descriptions(Entrench::getDescription);

	private final int mDuration;
	private final double mRadius;
	private final float mKnockbackSpeed;

	public Entrench(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = CharmManager.getDuration(player, CharmEffects.ENTRENCH_ROOT_DURATION.mEffectName, DURATION[mRarity - 1]);
		mRadius = CharmManager.getRadius(player, CharmEffects.ENTRENCH_RADIUS.mEffectName, RADIUS);
		mKnockbackSpeed = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.ENTRENCH_KNOCKBACK.mEffectName, KNOCKBACK_SPEED);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (isOnCooldown()) {
			return;
		}
		if (event.getType() == DamageEvent.DamageType.MELEE && (event.isBlockedByShield() || mPlayer.getAbsorptionAmount() > 0)) {
			putOnCooldown();
			Location loc = mPlayer.getLocation();
			World world = mPlayer.getWorld();
			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, mRadius)) {
				if (EntityUtils.isBoss(mob)) {
					continue;
				}
				EntityUtils.applySlow(mPlugin, mDuration, SLOW_MODIFIER, mob);
				mPlugin.mEffectManager.addEffect(mob, DepthsBrittle.effectID, new DepthsBrittle(mDuration, mKnockbackSpeed));
			}
			world.playSound(loc, Sound.BLOCK_NETHER_BRICKS_BREAK, SoundCategory.PLAYERS, 1.2f, 0.45f);
			world.playSound(loc, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, SoundCategory.PLAYERS, 1, 0.6f);
			double mult = mRadius / RADIUS;
			new PartialParticle(Particle.BLOCK_CRACK, loc, (int) (35 * mult), 1.5 * mult, 1.5 * mult, 1.5 * mult, 1, Material.SOUL_SOIL.createBlockData()).spawnAsPlayerActive(mPlayer);
		}
	}

	private static Description<Entrench> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.add("Blocking with a shield or absorption roots mobs within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks for ")
			.addDuration(a -> a.mDuration, DURATION[rarity - 1], false, true)
			.add(" seconds. The next melee hit against a rooted mob will knock them back.")
			.addCooldown(COOLDOWN);
	}
}
