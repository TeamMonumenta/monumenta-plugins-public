package com.playmonumenta.plugins.abilities.cleric.hierophant;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;



public class HallowedBeam extends MultipleChargeAbility {

	private static final int HALLOWED_1_MAX_CHARGES = 2;
	private static final int HALLOWED_2_MAX_CHARGES = 3;
	private static final int HALLOWED_1_COOLDOWN = 20 * 16;
	private static final int HALLOWED_2_COOLDOWN = 20 * 12;
	private static final double HALLOWED_HEAL_PERCENT = 0.3;
	private static final double HALLOWED_DAMAGE_REDUCTION_PERCENT = -0.1;
	private static final int HALLOWED_DAMAGE_REDUCTION_DURATION = 20 * 5;
	private static final String PERCENT_DAMAGE_RESIST_EFFECT_NAME = "HallowedPercentDamageResistEffect";
	private static final int HALLOWED_RADIUS = 4;
	private static final int HALLOWED_UNDEAD_STUN = 10; // 20 * 0.5
	private static final int HALLOWED_LIVING_STUN = 20 * 2;
	private static final int CAST_RANGE = 30;
	private static final String MODE_SCOREBOARD = "HallowedBeamMode";

	private @Nullable Crusade mCrusade;

	private enum Mode {
		DEFAULT, HEALING, ATTACK
	}

	private Mode mMode = Mode.DEFAULT;
	private int mLastCastTicks = 0;

	public HallowedBeam(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Hallowed Beam");
		mInfo.mScoreboardId = "HallowedBeam";
		mInfo.mShorthandName = "HB";
		mInfo.mDescriptions.add("Left-click with a bow or crossbow while looking directly at a player or mob to shoot a beam of light. If aimed at a player, the beam instantly heals them for 30% of their max health, knocking back enemies within 4 blocks. If aimed at an Undead, it instantly deals projectile damage equal to the used weapon's projectile damage to the target, and stuns them for half a second. If aimed at a non-undead mob, it instantly stuns them for 2s. Two charges. Pressing Swap while holding a bow will change the mode of Hallowed Beam between 'Default' (default), 'Healing' (only heals players, does not work on mobs), and 'Attack' (only applies mob effects, does not heal). This skill can only apply Recoil twice before touching the ground. Cooldown: 16s each charge.");
		mInfo.mDescriptions.add("Hallowed Beam gains a third charge (and can apply Recoil three times before touching the ground), the cooldown is reduced to 12 seconds, and players healed by it gain 10% damage resistance for 5 seconds.");
		mInfo.mLinkedSpell = ClassAbility.HALLOWED_BEAM;
		mInfo.mCooldown = isLevelOne() ? HALLOWED_1_COOLDOWN : HALLOWED_2_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.BOW, 1);
		mMaxCharges = isLevelOne() ? HALLOWED_1_MAX_CHARGES : HALLOWED_2_MAX_CHARGES;
		mMaxCharges = getAbilityScore() == 1 ? HALLOWED_1_MAX_CHARGES : HALLOWED_2_MAX_CHARGES;
		mCharges = getTrackedCharges();
		if (player != null) {
			int modeIndex = ScoreboardUtils.getScoreboardValue(player, MODE_SCOREBOARD).orElse(0);
			mMode = Mode.values()[Math.max(0, Math.min(modeIndex, Mode.values().length - 1))];
		}

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mCrusade = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Crusade.class);
			});
		}
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}
		LivingEntity e = EntityUtils.getEntityAtCursor(mPlayer, CAST_RANGE, true, true, true);
		if (e instanceof Player || EntityUtils.isHostileMob(e)) {
			Player player = mPlayer;

			PlayerInventory inventory = mPlayer.getInventory();
			ItemStack inMainHand = inventory.getItemInMainHand();

			if (ItemUtils.isBowOrTrident(inMainHand) && !ItemUtils.isShootableItem(inventory.getItemInOffHand()) && !ItemStatUtils.isShattered(inMainHand)) {
				int ticks = mPlayer.getTicksLived();
				// Prevent double casting on accident
				if (ticks - mLastCastTicks <= 5 || !consumeCharge()) {
					return;
				}
				mLastCastTicks = ticks;

				//Unsure why the runnable needs to exist, but it breaks if I don't have it
				new BukkitRunnable() {
					@Override
					public void run() {
						Location loc = player.getEyeLocation();
						Vector dir = loc.getDirection();
						World world = mPlayer.getWorld();

						LivingEntity applyE = e;
						//Check if heal should override damage
						for (Entity en : e.getNearbyEntities(1.5, 1.5, 1.5)) {
							if (en instanceof Player enPlayer && enPlayer.getGameMode() != GameMode.SPECTATOR && !en.getUniqueId().equals(mPlayer.getUniqueId())) {
								Player newP = EntityUtils.getNearestPlayer(en.getLocation(), 1.5);
								// Don't count if the caster is the closest, can't do a self-heal
								if (newP != null && !newP.getUniqueId().equals(mPlayer.getUniqueId())) {
									applyE = newP;
								}
							}
						}
						if (applyE instanceof Player pe && pe.getGameMode() != GameMode.SPECTATOR) {
							if (mMode == Mode.ATTACK) {
								incrementCharge();
								this.cancel();
								return;
							}
							world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1, 0.85f);
							world.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1, 0.9f);
							for (int i = 0; i < CAST_RANGE; i++) {
								loc.add(dir);
								new PartialParticle(Particle.VILLAGER_HAPPY, loc, 5, 0.25, 0.25, 0.25, 0).spawnAsPlayerActive(mPlayer);
								new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.05f, 0.05f, 0.05f, 0.025f).spawnAsPlayerActive(mPlayer);
								if (loc.distance(e.getEyeLocation()) < 1.25) {
									loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.35f);
									loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_HIT, 1, 0.9f);
									break;
								}
							}
							Location eLoc = pe.getLocation().add(0, pe.getHeight() / 2, 0);
							new PartialParticle(Particle.SPELL_INSTANT, pe.getLocation(), 500, 2.5, 0.15f, 2.5, 1).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.VILLAGER_HAPPY, pe.getLocation(), 150, 2.55, 0.15f, 2.5, 1).spawnAsPlayerActive(mPlayer);
							world.playSound(player.getEyeLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, 2, 1.5f);

							PlayerUtils.healPlayer(mPlugin, pe, EntityUtils.getMaxHealth(pe) * HALLOWED_HEAL_PERCENT, mPlayer);

							if (isLevelTwo()) {
								mPlugin.mEffectManager.addEffect(pe, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(HALLOWED_DAMAGE_REDUCTION_DURATION, HALLOWED_DAMAGE_REDUCTION_PERCENT));
							}
							for (LivingEntity le : EntityUtils.getNearbyMobs(eLoc, HALLOWED_RADIUS)) {
								MovementUtils.knockAway(pe, le, 0.65f, true);
							}

							applyRecoil();

						} else if (Crusade.enemyTriggersAbilities(applyE, mCrusade)) {
							if (mMode == Mode.HEALING) {
								incrementCharge();
								this.cancel();
								return;
							}
							world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1, 0.85f);
							world.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1, 0.9f);
							for (int i = 0; i < CAST_RANGE; i++) {
								loc.add(dir);
								new PartialParticle(Particle.VILLAGER_HAPPY, loc, 5, 0.25, 0.25, 0.25, 0).spawnAsPlayerActive(mPlayer);
								new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.05f, 0.05f, 0.05f, 0.025f).spawnAsPlayerActive(mPlayer);
								if (loc.distance(e.getEyeLocation()) < 1.25) {
									loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.35f);
									loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_HIT, 1, 0.9f);
									break;
								}
							}

							double damage = ItemStatUtils.getAttributeAmount(player.getInventory().getItemInMainHand(), ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_ADD, ItemStatUtils.Operation.ADD, ItemStatUtils.Slot.MAINHAND);

							DamageUtils.damage(mPlayer, applyE, DamageType.PROJECTILE_SKILL, damage, mInfo.mLinkedSpell, true, true);

							Location eLoc = applyE.getLocation().add(0, applyE.getHeight() / 2, 0);
							new PartialParticle(Particle.SPIT, eLoc, 40, 0, 0, 0, 0.25f).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.FIREWORKS_SPARK, eLoc, 75, 0, 0, 0, 0.3f).spawnAsPlayerActive(mPlayer);

							applyRecoil();
						} else if (EntityUtils.isHostileMob(applyE)) {
							if (mMode == Mode.HEALING) {
								incrementCharge();
								this.cancel();
								return;
							}
							world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1, 0.85f);
							world.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1, 0.9f);
							for (int i = 0; i < CAST_RANGE; i++) {
								loc.add(dir);
								new PartialParticle(Particle.VILLAGER_HAPPY, loc, 5, 0.25, 0.25, 0.25, 0).spawnAsPlayerActive(mPlayer);
								new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.05f, 0.05f, 0.05f, 0.025f).spawnAsPlayerActive(mPlayer);
								if (loc.distance(e.getEyeLocation()) < 1.25) {
									loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.35f);
									loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_HIT, 1, 0.9f);
									break;
								}
							}

							if (Crusade.enemyTriggersAbilities(applyE, mCrusade)) {
								EntityUtils.applyStun(mPlugin, HALLOWED_UNDEAD_STUN, applyE);
							} else {
								EntityUtils.applyStun(mPlugin, HALLOWED_LIVING_STUN, applyE);
							}

							if (inMainHand.containsEnchantment(Enchantment.ARROW_FIRE)) {
								EntityUtils.applyFire(mPlugin, 20 * 15, applyE, player);
							}
							Location eLoc = applyE.getLocation().add(0, applyE.getHeight() / 2, 0);
							new PartialParticle(Particle.SPIT, eLoc, 40, 0, 0, 0, 0.25f).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.CRIT_MAGIC, loc, 30, 1, 1, 1, 0.25).spawnAsPlayerActive(mPlayer);

							applyRecoil();
						}
						this.cancel();
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}
		}
	}

	public void applyRecoil() {
		ItemStack item = mPlayer.getInventory().getItemInMainHand();
		if (ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.RECOIL) > 0) {
			if (!EntityUtils.isRecoilDisable(mPlugin, mPlayer, mMaxCharges)) {
				if (!mPlayer.isSneaking() && !ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES) && mPlayer.getLocation().getY() >= 0) {
					Vector velocity = NmsUtils.getVersionAdapter().getActualDirection(mPlayer).multiply(-0.5 * Math.sqrt(ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.RECOIL)));
					velocity.setY(Math.max(0.1, velocity.getY()));
					mPlayer.setVelocity(velocity);
				}
			}
			EntityUtils.applyRecoilDisable(mPlugin, 9999, (int) EntityUtils.getRecoilDisableAmount(mPlugin, mPlayer) + 1, mPlayer);
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			cast(Action.LEFT_CLICK_AIR);
		}
		return false;
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (mPlayer == null) {
			return;
		}
		PlayerInventory inventory = mPlayer.getInventory();
		ItemStack inMainHand = inventory.getItemInMainHand();

		if (ItemUtils.isBowOrTrident(inMainHand) && !mPlayer.isSneaking()) {
			event.setCancelled(true);
			if (mMode == Mode.DEFAULT) {
				mMode = Mode.HEALING;
				MessagingUtils.sendActionBarMessage(mPlayer, mInfo.mLinkedSpell.getName() + " Mode: " + "Healing");
			} else if (mMode == Mode.HEALING) {
				mMode = Mode.ATTACK;
				MessagingUtils.sendActionBarMessage(mPlayer, mInfo.mLinkedSpell.getName() + " Mode: " + "Attack");
			} else {
				mMode = Mode.DEFAULT;
				MessagingUtils.sendActionBarMessage(mPlayer, mInfo.mLinkedSpell.getName() + " Mode: " + "Default");
			}
			ScoreboardUtils.setScoreboardValue(mPlayer, MODE_SCOREBOARD, mMode.ordinal());
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	@Override
	public @Nullable String getMode() {
		return mMode.name().toLowerCase(Locale.ROOT);
	}
}
