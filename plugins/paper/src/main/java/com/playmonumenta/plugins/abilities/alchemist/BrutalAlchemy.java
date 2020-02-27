package com.playmonumenta.plugins.abilities.alchemist;

import java.util.Collection;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class BrutalAlchemy extends Ability {
	private static final int BRUTAL_ALCHEMY_DAMAGE_1 = 3;
	private static final int BRUTAL_ALCHEMY_DAMAGE_2 = 5;
	private static final int BRUTAL_ALCHEMY_WITHER_1_DURATION = 4 * 20 + 10;
	private static final int BRUTAL_ALCHEMY_WITHER_2_DURATION = 6 * 20 + 10;
	public static final String BRUTAL_ALCHEMY_SCOREBOARD = "BrutalAlchemy";

	private int mDamage;
	private int mWitherDuration;

	public BrutalAlchemy(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Brutal Alchemy");
		mInfo.scoreboardId = BRUTAL_ALCHEMY_SCOREBOARD;
		mInfo.mShorthandName = "BA";
		mInfo.mDescriptions.add("Killing a mob gives you an Alchemist's Potion. Your Alchemist's Potions deal 3 magic damage and 4s of Wither II. The first skill point spent on either Gruesome Alchemy or Brutal Alchemy will give you a potion per kill and a 30% chance of getting a second potion.");
		mInfo.mDescriptions.add("Your Alchemist's Potions now deal 5 magic damage and 6s of Wither II");
		mDamage = getAbilityScore() == 1 ? BRUTAL_ALCHEMY_DAMAGE_1 : BRUTAL_ALCHEMY_DAMAGE_2;
		mWitherDuration = getAbilityScore() == 1 ? BRUTAL_ALCHEMY_WITHER_1_DURATION : BRUTAL_ALCHEMY_WITHER_2_DURATION;
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
		EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.ALCHEMY);
		PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.WITHER, mWitherDuration, 1, false, true));
	}

}
