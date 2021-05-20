package com.playmonumenta.plugins.abilities.alchemist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

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
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.abilities.BaseAbilityEnchantment;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * Handles giving potions on kills and the direct damage aspect
 */
public class AlchemistPotions extends Ability implements KillTriggeredAbility {

	public static class AlchemistPotionsDamageEnchantment extends BaseAbilityEnchantment {
		public AlchemistPotionsDamageEnchantment() {
			super("Alchemist Potion Damage", EnumSet.of(ItemSlot.ARMOR));
		}
	}

	private static final double EXTRA_POTION_CHANCE = 0.5;
	private static final double DAMAGE_PER_SKILL_POINT = 0.5;
	private static final double DAMAGE_PER_SPEC_POINT = 1;

	private final KillTriggeredAbilityTracker mTracker;

	private List<PotionAbility> mPotionAbilities = new ArrayList<PotionAbility>();
	private double mDamage = 0;

	public AlchemistPotions(Plugin plugin, Player player) {
		super(plugin, player, null);
		mInfo.mLinkedSpell = ClassAbility.ALCHEMIST_POTION;
		mTracker = new KillTriggeredAbilityTracker(this);

		if (player == null) {
			/* This is a reference ability, not one actually tied to a player */
			return;
		}

		/*
		 * Run this stuff 5 ticks later. As of now, the AbilityManager takes a tick
		 * to initialize everything, and the PotionAbility classes take a tick to
		 * initialize their damage values, but just give a few extra ticks for slight
		 * future-proofing.
		 */
		if (player != null) {
			new BukkitRunnable() {
				@Override
				public void run() {
					Ability[] classAbilities = new Ability[8];
					Ability[] specializationAbilities = new Ability[6];
					classAbilities[0] = AbilityManager.getManager().getPlayerAbility(mPlayer, GruesomeAlchemy.class);
					classAbilities[1] = AbilityManager.getManager().getPlayerAbility(mPlayer, BrutalAlchemy.class);
					classAbilities[2] = AbilityManager.getManager().getPlayerAbility(mPlayer, IronTincture.class);
					classAbilities[3] = AbilityManager.getManager().getPlayerAbility(mPlayer, BasiliskPoison.class);
					classAbilities[4] = AbilityManager.getManager().getPlayerAbility(mPlayer, PowerInjection.class);
					classAbilities[5] = AbilityManager.getManager().getPlayerAbility(mPlayer, UnstableArrows.class);
					classAbilities[6] = AbilityManager.getManager().getPlayerAbility(mPlayer, EnfeeblingElixir.class);
					classAbilities[7] = AbilityManager.getManager().getPlayerAbility(mPlayer, Bezoar.class);
					specializationAbilities[0] = AbilityManager.getManager().getPlayerAbility(mPlayer, PurpleHaze.class);
					specializationAbilities[1] = AbilityManager.getManager().getPlayerAbility(mPlayer, NightmarishAlchemy.class);
					specializationAbilities[2] = AbilityManager.getManager().getPlayerAbility(mPlayer, ScorchedEarth.class);
					specializationAbilities[3] = AbilityManager.getManager().getPlayerAbility(mPlayer, WardingRemedy.class);
					specializationAbilities[4] = AbilityManager.getManager().getPlayerAbility(mPlayer, InvigoratingOdor.class);
					specializationAbilities[5] = AbilityManager.getManager().getPlayerAbility(mPlayer, AlchemicalAmalgam.class);

					for (Ability classAbility : classAbilities) {
						if (classAbility != null) {
							mDamage += DAMAGE_PER_SKILL_POINT * classAbility.getAbilityScore();

							if (classAbility instanceof PotionAbility) {
								PotionAbility potionAbility = (PotionAbility) classAbility;
								mPotionAbilities.add(potionAbility);
								mDamage += potionAbility.getDamage();
							}
						}
					}

					for (Ability specializationAbility : specializationAbilities) {
						if (specializationAbility != null) {
							mDamage += DAMAGE_PER_SPEC_POINT * specializationAbility.getAbilityScore();

							if (specializationAbility instanceof PotionAbility) {
								PotionAbility potionAbility = (PotionAbility) specializationAbility;
								mPotionAbilities.add(potionAbility);
								mDamage += potionAbility.getDamage();
							}
						}
					}

				}
			}.runTaskLater(mPlugin, 5);
		}
	}

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
			createAura(potion.getLocation());

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

	public void createAura(Location loc) {
		for (PotionAbility potionAbility : mPotionAbilities) {
			potionAbility.createAura(loc);
		}
	}

	public void createAura(Location loc, double radius) {
		for (PotionAbility potionAbility : mPotionAbilities) {
			potionAbility.createAura(loc, radius);
		}
	}

	public void apply(LivingEntity mob) {
		// Apply effects first so stuff like Vulnerability properly stacks
		for (PotionAbility potionAbility : mPotionAbilities) {
			potionAbility.apply(mob);
		}
		double damage = mDamage + AlchemistPotionsDamageEnchantment.getExtraDamage(mPlayer, AlchemistPotionsDamageEnchantment.class);
		EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer, MagicType.ALCHEMY, true, mInfo.mLinkedSpell);
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		mTracker.updateDamageDealtToBosses(event);
		return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity damagee, EntityDamageByEntityEvent event) {
		mTracker.updateDamageDealtToBosses(event);
		return true;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		triggerOnKill(event.getEntity());
	}

	@Override
	public void triggerOnKill(LivingEntity mob) {
		AbilityUtils.addAlchemistPotions(mPlayer, FastUtils.RANDOM.nextDouble() < EXTRA_POTION_CHANCE ? 2 : 1);
	}

	public double getDamage() {
		return mDamage;
	}

}
