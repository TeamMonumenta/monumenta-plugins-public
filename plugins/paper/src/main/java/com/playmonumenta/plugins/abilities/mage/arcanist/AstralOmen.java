package com.playmonumenta.plugins.abilities.mage.arcanist;

import java.util.NavigableSet;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.enchantments.SpellDamage;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.effects.AstralOmenStacks;
import com.playmonumenta.plugins.effects.AstralOmenBonusDamage;

public class AstralOmen extends Ability {
	
	private static final String ASTRAL_NAME = "AstralOmenStacks";
	private static final String ASTRAL_DAMAGE_NAME = "AstralOmenBonusDamage";
	private static final int DURATION = 20 * 10;
	private static final int RADIUS = 3;
	private static final float PULL_SPEED = 0.175f;
	private static final double SLOW_AMOUNT = 0.1;
	private static final int DAMAGE = 6;
	private static final double BONUS_DAMAGE_1 = 0.2;
	private static final double BONUS_DAMAGE_2 = 0.3;
	
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(100, 50, 170), 1.0f);
	
	private final double mBonusDamage;

	public AstralOmen(Plugin plugin, Player player) {
		super(plugin, player, "Astral Omen");
		mInfo.mScoreboardId = "AstralOmen";
		mInfo.mShorthandName = "AO";
		mInfo.mDescriptions.add("Hitting an enemy with an active spell marks their fate, giving them an Astral Omen. An Omen decays after 10s of not gaining another Omen. If an enemy reaches three Omens their fate is sealed, causing a magical implosion to occur around them. The implosion removes all Omens on the mob and deals 6 damage to all mobs within 3 blocks. Additionally, the mob at the source of the implosion will take 20% extra damage from the caster for the next 8s. Implosions cannot apply an Astral Omen.");
		mInfo.mDescriptions.add("The bonus damage on imploding mobs is increased to 30%, and enemies caught in the implosion are pulled towards it and given 10% Slowness for 8s.");
		mInfo.mLinkedSpell = Spells.ASTRAL_OMEN;
		mBonusDamage = getAbilityScore() == 1 ? BONUS_DAMAGE_1 : BONUS_DAMAGE_2;
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		if (event.getSpell() == null || event.getSpell() == mInfo.mLinkedSpell) {
			return;
		}
		
		float damage = SpellDamage.getSpellDamage(mPlayer, DAMAGE);
		LivingEntity mob = event.getDamaged();
		World world = mob.getWorld();
		int astralStacks = EntityUtils.getAstralStacks(mPlugin, mob);
		mPlugin.mEffectManager.addEffect(mob, ASTRAL_NAME, new AstralOmenStacks(DURATION, astralStacks + 1, mPlayer, ASTRAL_NAME));
		if (astralStacks >= 2) {
			NavigableSet<Effect> stacks = mPlugin.mEffectManager.getEffects(mob, ASTRAL_NAME);
			if (stacks != null) {
				Effect omen = stacks.last();
				if (omen instanceof AstralOmenStacks) {
					AstralOmenStacks o = (AstralOmenStacks) omen;
					o.clearEffect();
				}	
				world.spawnParticle(Particle.ENCHANTMENT_TABLE, mob.getLocation(), 80, 0, 0, 0, 4);
				world.spawnParticle(Particle.REDSTONE, mob.getLocation(), 20, 3, 3, 3, 0.1, COLOR);
				world.playSound(mob.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.3f, 1.5f);
				for (LivingEntity m : EntityUtils.getNearbyMobs(mob.getLocation(), RADIUS, mPlayer)) {
					EntityUtils.damageEntity(mPlugin, m, damage, mPlayer, MagicType.ARCANE, true, mInfo.mLinkedSpell);
					world.spawnParticle(Particle.REDSTONE, m.getLocation(), 10, 0.2, 0.2, 0.2, 0.1, COLOR);
					if (getAbilityScore() > 1) {
						MovementUtils.pullTowards(mob, m, PULL_SPEED);
						EntityUtils.applySlow(mPlugin, DURATION, SLOW_AMOUNT, m);
					}
				}
				mPlugin.mEffectManager.addEffect(mob, ASTRAL_DAMAGE_NAME, new AstralOmenBonusDamage(DURATION, mBonusDamage, mPlayer));
			}
		}
	}
}
