package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.rogue.AdvancingShadows;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.rogue.AdvancingShadowsCS;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DepthsAdvancingShadows extends DepthsAbility {

	public static final String ABILITY_NAME = "Advancing Shadows";
	public static final double[] DAMAGE = {0.24, 0.30, 0.36, 0.42, 0.48, 0.6};

	private static final int ADVANCING_SHADOWS_RANGE = 12;
	private static final int COOLDOWN = 18 * 20;
	private static final int DAMAGE_DURATION = 5 * 20;

	public static final String CHARM_COOLDOWN = "Advancing Shadows Cooldown";

	public static final DepthsAbilityInfo<DepthsAdvancingShadows> INFO =
		new DepthsAbilityInfo<>(DepthsAdvancingShadows.class, ABILITY_NAME, DepthsAdvancingShadows::new, DepthsTree.SHADOWDANCER, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.ADVANCING_SHADOWS_DEPTHS)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", DepthsAdvancingShadows::cast, DepthsTrigger.RIGHT_CLICK))
			.displayItem(Material.WITHER_SKELETON_SKULL)
			.descriptions(DepthsAdvancingShadows::getDescription);

	private final double mRange;
	private final double mDamage;
	private final int mDamageDuration;

	public DepthsAdvancingShadows(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRange = CharmManager.getRadius(mPlayer, CharmEffects.ADVANCING_SHADOWS_RANGE.mEffectName, ADVANCING_SHADOWS_RANGE);
		mDamage = DAMAGE[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.ADVANCING_SHADOWS_DAMAGE_MULTIPLIER.mEffectName);
		mDamageDuration = CharmManager.getDuration(mPlayer, CharmEffects.ADVANCING_SHADOWS_DURATION.mEffectName, DAMAGE_DURATION);

	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		AdvancingShadowsCS cosmetic = new AdvancingShadowsCS();

		LivingEntity entity = EntityUtils.getHostileEntityAtCursor(mPlayer, mRange);
		if (entity == null) {
			return false;
		}
		Location tpDestination = AdvancingShadows.findTeleportLocation(entity, mPlayer, mRange, cosmetic);
		if (tpDestination == null) {
			return false;
		}
		if (!(mPlayer.getInventory().getItemInOffHand().getType() == Material.SHIELD) && !tpDestination.equals(mPlayer.getLocation())) {
			PlayerUtils.playerTeleport(mPlayer, tpDestination);
		}

		mPlugin.mEffectManager.addEffect(mPlayer, ABILITY_NAME, new PercentDamageDealt(mDamageDuration, mDamage, DamageEvent.DamageType.getAllMeleeTypes()));
		cosmetic.tpParticle(mPlayer, entity);
		cosmetic.tpSound(mPlayer.getWorld(), mPlayer);
		return true;
	}

	private static Description<DepthsAdvancingShadows> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<DepthsAdvancingShadows>(color)
			.add("Right click while looking directly at a hostile mob within ")
			.add(a -> a.mRange, ADVANCING_SHADOWS_RANGE)
			.add(" blocks to teleport to it and gain ")
			.addPercent(a -> a.mDamage, DAMAGE[rarity - 1], false, true)
			.add(" melee damage for ")
			.addDuration(a -> a.mDamageDuration, DAMAGE_DURATION)
			.add(" seconds. If you are holding a shield in your offhand, you will gain the damage buff but not be teleported.")
			.addCooldown(COOLDOWN);
	}


}

