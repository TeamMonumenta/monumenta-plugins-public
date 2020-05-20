package com.playmonumenta.plugins.abilities.alchemist;

import java.util.Collection;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class BrutalAlchemy extends Ability {
	private static final int BRUTAL_ALCHEMY_1_DAMAGE = 1;
	private static final int BRUTAL_ALCHEMY_2_DAMAGE = 2;
	private static final int BRUTAL_ALCHEMY_DURATION = 20 * 8;
	private static final int BRUTAL_ALCHEMY_1_VULNERABILITY_AMPLIFIER = 2;
	private static final int BRUTAL_ALCHEMY_2_VULNERABILITY_AMPLIFIER = 4;

	private final int mDamage;
	private final int mVulnerabilityAmplifier;

	public BrutalAlchemy(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Brutal Alchemy");
		mInfo.linkedSpell = Spells.BRUTAL_ALCHEMY;
		mInfo.scoreboardId = "BrutalAlchemy";
		mInfo.mShorthandName = "BA";
		mInfo.mDescriptions.add("Your Alchemist's Potions deal 1 damage and 15% Vulnerability for 8 seconds.");
		mInfo.mDescriptions.add("Your Alchemist's Potions now deal 2 damage and 25% Vulnerability.");
		mDamage = getAbilityScore() == 1 ? BRUTAL_ALCHEMY_1_DAMAGE : BRUTAL_ALCHEMY_2_DAMAGE;
		mVulnerabilityAmplifier = getAbilityScore() == 1 ? BRUTAL_ALCHEMY_1_VULNERABILITY_AMPLIFIER : BRUTAL_ALCHEMY_2_VULNERABILITY_AMPLIFIER;
	}

	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata("AlchemistPotion")) {
			if (affectedEntities != null && !affectedEntities.isEmpty()) {
				for (LivingEntity entity : affectedEntities) {
					if (EntityUtils.isHostileMob(entity)) {
						apply(entity);
					}
				}
			}
		}
		return true;
	}

	public void apply(LivingEntity mob) {
		PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.UNLUCK, BRUTAL_ALCHEMY_DURATION, mVulnerabilityAmplifier, false, true));
		EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.ALCHEMY, true, mInfo.linkedSpell);
	}

}
