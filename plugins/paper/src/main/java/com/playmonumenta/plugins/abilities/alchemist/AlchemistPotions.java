package com.playmonumenta.plugins.abilities.alchemist;

import java.util.Collection;
import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker.KillTriggeredAbility;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.AlchemicalAmalgam;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.InvigoratingOdor;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.WardingRemedy;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.NightmarishAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.PurpleHaze;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.ScorchedEarth;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * This is a utility "ability" that makes it possible to give stacked potions in one place
 */
public class AlchemistPotions extends Ability implements KillTriggeredAbility {

	private static final double DAMAGE_PER_SKILL_POINT = 0.5;
	private static final double DAMAGE_PER_SPEC_POINT = 1;

	private final KillTriggeredAbilityTracker mTracker;

	private double mDamage = -1;

	public AlchemistPotions(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, null);
		mInfo.linkedSpell = Spells.ALCHEMIST_POTION;
		mTracker = new KillTriggeredAbilityTracker(this);
	}

	/*
	 * Passive potion damage woo, potions for everyone
	 */
	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class") == 5;
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion) {
		if (InventoryUtils.testForItemWithName(potion.getItem(), "Alchemist's Potion")) {
			mPlugin.mProjectileEffectTimers.addEntity(potion, Particle.SPELL);
			potion.setMetadata("AlchemistPotion", new FixedMetadataValue(mPlugin, 0));
		}
		return true;
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
		// Initialize the damage once we're certain we can actually read the ability scores
		if (mDamage == -1) {
			setDamage();
		}

		EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.ALCHEMY, true, mInfo.linkedSpell);
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		mTracker.updateDamageDealtToBosses(event);
		return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		mTracker.updateDamageDealtToBosses(event);
		return true;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		triggerOnKill(event.getEntity());
	}

	@Override
	public void triggerOnKill(LivingEntity mob) {
		int newPot = 1;
		if (mRandom.nextDouble() < 0.50) {
			newPot++;
		}

		AbilityUtils.addAlchemistPotions(mPlayer, newPot);
	}

	private void setDamage() {
		mDamage = 0;
		Ability[] classAbilities = new Ability[8];
		Ability[] specializationAbilities = new Ability[6];
		classAbilities[0] = AbilityManager.getManager().getPlayerAbility(mPlayer, GruesomeAlchemy.class);
		classAbilities[1] = AbilityManager.getManager().getPlayerAbility(mPlayer, BrutalAlchemy.class);
		classAbilities[2] = AbilityManager.getManager().getPlayerAbility(mPlayer, IronTincture.class);
		classAbilities[3] = AbilityManager.getManager().getPlayerAbility(mPlayer, BasiliskPoison.class);
		classAbilities[4] = AbilityManager.getManager().getPlayerAbility(mPlayer, PowerInjection.class);
		classAbilities[5] = AbilityManager.getManager().getPlayerAbility(mPlayer, UnstableArrows.class);
		classAbilities[6] = AbilityManager.getManager().getPlayerAbility(mPlayer, EnfeeblingElixir.class);
		classAbilities[7] = AbilityManager.getManager().getPlayerAbility(mPlayer, AlchemicalArtillery.class);
		specializationAbilities[0] = AbilityManager.getManager().getPlayerAbility(mPlayer, PurpleHaze.class);
		specializationAbilities[1] = AbilityManager.getManager().getPlayerAbility(mPlayer, NightmarishAlchemy.class);
		specializationAbilities[2] = AbilityManager.getManager().getPlayerAbility(mPlayer, ScorchedEarth.class);
		specializationAbilities[3] = AbilityManager.getManager().getPlayerAbility(mPlayer, WardingRemedy.class);
		specializationAbilities[4] = AbilityManager.getManager().getPlayerAbility(mPlayer, InvigoratingOdor.class);
		specializationAbilities[5] = AbilityManager.getManager().getPlayerAbility(mPlayer, AlchemicalAmalgam.class);

		for (int i = 0; i < classAbilities.length; i++) {
			if (classAbilities[i] != null) {
				mDamage += DAMAGE_PER_SKILL_POINT * classAbilities[i].getAbilityScore();
			}
		}

		for (int i = 0; i < specializationAbilities.length; i++) {
			if (specializationAbilities[i] != null) {
				mDamage += DAMAGE_PER_SPEC_POINT * specializationAbilities[i].getAbilityScore();
			}
		}
	}
}
