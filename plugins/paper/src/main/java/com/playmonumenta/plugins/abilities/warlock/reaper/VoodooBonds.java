package com.playmonumenta.plugins.abilities.warlock.reaper;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.reaper.VoodooBondsCS;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.VoodooBondsOtherPlayer;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class VoodooBonds extends Ability {

	private static final int COOLDOWN_1 = 22 * 20;
	private static final int COOLDOWN_2 = 12 * 20;
	private static final int ACTIVE_RADIUS = 8;
	private static final int PASSIVE_RADIUS = 3;
	private static final double DAMAGE = 0.15;
	private static final int DURATION_1 = 20 * 5;
	private static final int DURATION_2 = 20 * 7;
	public static final String EFFECT_NAME = "VoodooBondsEffect";

	public static final String CHARM_COOLDOWN = "Voodoo Bonds Cooldown";
	public static final String CHARM_TRANSFER_DAMAGE = "Voodoo Bonds Transfer Damage";
	public static final String CHARM_TRANSFER_TIME = "Voodoo Bonds Transfer Time Limit";
	public static final String CHARM_DAMAGE = "Voodoo Bonds Damage";
	public static final String CHARM_RADIUS = "Voodoo Bonds Radius";

	public static final AbilityInfo<VoodooBonds> INFO =
		new AbilityInfo<>(VoodooBonds.class, "Voodoo Bonds", VoodooBonds::new)
			.linkedSpell(ClassAbility.VOODOO_BONDS)
			.scoreboardId("VoodooBonds")
			.shorthandName("VB")
			.descriptions(
				"Melee strikes to a mob apply 15% of the damage to all mobs of the same type within 3 blocks. " +
					"Additionally, right-click while sneaking and looking down to cast a protective spell on all players within an 8 block radius. " +
					"The next hit every player (including the Reaper) takes has all damage ignored (or 50% if attack is from a Boss), " +
					"but that damage will transfer to the Reaper in 5s unless it is passed on again. " +
					"Passing that damage requires a melee strike, in which 33% of the initial damage blocked is added to the damage of the strike (Bosses are immune to this bonus). " +
					"The damage directed to the Reaper is calculated by the percentage of health the initial hit would have taken from that player, " +
					"and can never kill you, only leave you at 1 HP. Cooldown: 22s.",
				"The duration before damage transfer increases to 7s, the on-hit damage when passing a hit increases to 66% of the blocked damage, and the cooldown is reduced to 12s.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", VoodooBonds::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true).lookDirections(AbilityTrigger.LookDirection.DOWN),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(new ItemStack(Material.JACK_O_LANTERN, 1));

	private final int mTransferDuration;
	private final VoodooBondsCS mCosmetic;

	public VoodooBonds(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mTransferDuration = CharmManager.getExtraDuration(player, CHARM_TRANSFER_TIME) + (isLevelOne() ? DURATION_1 : DURATION_2);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new VoodooBondsCS(), VoodooBondsCS.SKIN_LIST);
	}


	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		putOnCooldown();
		final double maxRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, ACTIVE_RADIUS);
		mCosmetic.bondsStartEffect(mPlayer.getWorld(), mPlayer, maxRadius);
		for (Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), CharmManager.getRadius(mPlayer, CHARM_RADIUS, ACTIVE_RADIUS), true)) {
			//better effects
			mCosmetic.bondsApplyEffect(mPlayer, p);
			mPlugin.mEffectManager.addEffect(p, EFFECT_NAME,
					new VoodooBondsOtherPlayer(getModifiedCooldown(), mTransferDuration, mPlayer, mPlugin));
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())) {
			EntityType type = enemy.getType();
			Location eLoc = enemy.getLocation();
			double damage = event.getDamage();

			if (mPlugin.mEffectManager.hasEffect(mPlayer, PercentDamageDealt.class)) {
				for (Effect priorityEffects : mPlugin.mEffectManager.getPriorityEffects(mPlayer).values()) {
					if (priorityEffects instanceof PercentDamageDealt damageEffect) {
						EnumSet<DamageType> types = damageEffect.getAffectedDamageTypes();
						if (types == null || types.contains(DamageType.MELEE)) {
							damage = damage * (1 + damageEffect.getMagnitude() * (damageEffect.isBuff() ? 1 : -1));
						}
					}
				}
			}

			for (LivingEntity mob : EntityUtils.getNearbyMobs(eLoc, CharmManager.getRadius(mPlayer, CHARM_RADIUS, PASSIVE_RADIUS), mPlayer)) {
				if (mob.getType().equals(type) && mob != enemy) {
					Location mLoc = mob.getLocation();
					DamageUtils.damage(mPlayer, mob, DamageType.OTHER, damage * DAMAGE, mInfo.getLinkedSpell(), true);
					mCosmetic.bondsSpreadParticle(mPlayer, mLoc, eLoc);
				}
			}
			return true;
		}
		return false;
	}
}
