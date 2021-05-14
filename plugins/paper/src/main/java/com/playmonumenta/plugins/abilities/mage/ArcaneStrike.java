package com.playmonumenta.plugins.abilities.mage;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.abilities.SpellPower;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

public class ArcaneStrike extends Ability {

	private static final Particle.DustOptions COLOR_1 = new Particle.DustOptions(Color.fromRGB(220, 147, 249), 1.0f);
	private static final Particle.DustOptions COLOR_2 = new Particle.DustOptions(Color.fromRGB(217, 122, 255), 1.0f);

	private static final float RADIUS = 4.0f;
	private static final int DAMAGE_1 = 4;
	private static final int DAMAGE_2 = 7;
	private static final int BONUS_DAMAGE_1 = 2;
	private static final int BONUS_DAMAGE_2 = 3;
	private static final int COOLDOWN = 5 * 20;

	private final int mDamageBonus;
	private final int mDamageBonusAffected;

	public ArcaneStrike(Plugin plugin, Player player) {
		super(plugin, player, "Arcane Strike");
		mInfo.mLinkedSpell = Spells.ARCANE_STRIKE;
		mInfo.mScoreboardId = "ArcaneStrike";
		mInfo.mShorthandName = "AS";
		mInfo.mDescriptions.add("When you attack an enemy with a wand, you unleash an arcane explosion dealing 4 damage to all mobs in a 4 block radius around the target. Enemies that are on fire or slowed take 2 extra damage. Arcane strike can not trigger Spellshock's static. Cooldown: 5s.");
		mInfo.mDescriptions.add("The damage is increased to 7. Mobs that are on fire or slowed take 3 additional damage.");
		mInfo.mCooldown = COOLDOWN;
		mDamageBonus = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
		mDamageBonusAffected = getAbilityScore() == 1 ? BONUS_DAMAGE_1 : BONUS_DAMAGE_2;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			putOnCooldown();

			LivingEntity damagee = (LivingEntity) event.getEntity();

			for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), RADIUS, mPlayer)) {
				float dmg = SpellPower.getSpellDamage(mPlayer, mDamageBonus);

				// Arcane Strike extra damage if on fire or slowed (but effect not applied this tick)
				if (EntityUtils.isSlowed(mPlugin, mob) || ((mob.hasPotionEffect(PotionEffectType.SLOW)
				     && !MetadataUtils.happenedThisTick(mPlugin, mob, Constants.ENTITY_SLOWED_NONCE_METAKEY, 0)))
				    || (mob.getFireTicks() > 0
				        && !MetadataUtils.happenedThisTick(mPlugin, mob, Constants.ENTITY_COMBUST_NONCE_METAKEY, 0))) {
					dmg += SpellPower.getSpellDamage(mPlayer, mDamageBonusAffected);
				}

				EntityUtils.damageEntity(mPlugin, mob, dmg, mPlayer, MagicType.ARCANE, true, mInfo.mLinkedSpell, true, false, false, true);
			}

			Location locD = damagee.getLocation().add(0, 1, 0);
			World world = mPlayer.getWorld();
			world.spawnParticle(Particle.DRAGON_BREATH, locD, 75, 0, 0, 0, 0.25);
			world.spawnParticle(Particle.EXPLOSION_NORMAL, locD, 35, 0, 0, 0, 0.2);
			world.spawnParticle(Particle.SPELL_WITCH, locD, 150, 2.5, 1, 2.5, 0.001);
			world.playSound(locD, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.75f, 1.5f);

			Location loc = mPlayer.getLocation().add(mPlayer.getLocation().getDirection().multiply(0.5));
			world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 0.75f, 1.65f);
			world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 0.5f);
			new BukkitRunnable() {
				double mD = 30;
				@Override
				public void run() {
					Vector vec;
					for (double degree = mD; degree < mD + 30; degree += 8) {
						double radian1 = Math.toRadians(degree);
						double cos = FastUtils.cos(radian1);
						double sin = FastUtils.sin(radian1);
						for (double r = 1; r < 4; r += 0.5) {
							vec = new Vector(cos * r, 1, sin * r);
							vec = VectorUtils.rotateXAxis(vec, loc.getPitch());
							vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

							Location l = loc.clone().add(vec);
							world.spawnParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, COLOR_1);
							world.spawnParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, COLOR_2);
						}
					}
					mD += 30;
					if (mD >= 150) {
						this.cancel();
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}

		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return InventoryUtils.isWandItem(mainHand);
	}
}
