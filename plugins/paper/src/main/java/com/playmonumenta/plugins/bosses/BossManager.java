package com.playmonumenta.plugins.bosses;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.*;
import com.playmonumenta.plugins.bosses.bosses.abilities.AbilityMarkerEntityBoss;
import com.playmonumenta.plugins.bosses.bosses.abilities.AlchemicalAberrationBoss;
import com.playmonumenta.plugins.bosses.bosses.abilities.DummyDecoyBoss;
import com.playmonumenta.plugins.bosses.bosses.abilities.MetalmancyBoss;
import com.playmonumenta.plugins.bosses.bosses.abilities.RestlessSoulsBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.BlueStrikeDaggerCraftingBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.BlueStrikeTargetNPCBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.BlueStrikeTurretBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.DropShardBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.LavaCannonBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.Samwell;
import com.playmonumenta.plugins.bosses.bosses.gray.GrayBookSummoner;
import com.playmonumenta.plugins.bosses.bosses.gray.GrayDemonSummoner;
import com.playmonumenta.plugins.bosses.bosses.gray.GrayGolemSummoner;
import com.playmonumenta.plugins.bosses.bosses.gray.GrayScarabSummoner;
import com.playmonumenta.plugins.bosses.bosses.gray.GraySummoned;
import com.playmonumenta.plugins.bosses.bosses.lich.LichAlchBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichClericBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichConquestBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichCurseBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichDemiseBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichJudgementBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichKeyGlowBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichMageBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichRogueBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichScoutBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichShieldBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichStrifeBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichWarlockBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichWarriorBoss;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.delves.mobabilities.DreadfulSummonBoss;
import com.playmonumenta.plugins.delves.mobabilities.SpectralSummonBoss;
import com.playmonumenta.plugins.delves.mobabilities.StatMultiplierBoss;
import com.playmonumenta.plugins.delves.mobabilities.TwistedMiniBoss;
import com.playmonumenta.plugins.depths.bosses.Davey;
import com.playmonumenta.plugins.depths.bosses.Hedera;
import com.playmonumenta.plugins.depths.bosses.Nucleus;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.gallery.bosses.GalleryMobRisingBoss;
import com.playmonumenta.plugins.gallery.bosses.GallerySummonMobBoss;
import com.playmonumenta.plugins.parrots.RainbowParrot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;

public class BossManager implements Listener {

	/********************************************************************************
	 * Classes/Interfaces
	 *******************************************************************************/

	@FunctionalInterface
	public interface StatelessBossConstructor {
		@Nullable BossAbilityGroup construct(Plugin plugin, LivingEntity entity) throws Exception;
	}

	@FunctionalInterface
	public interface StatefulBossConstructor {
		@Nullable BossAbilityGroup construct(Plugin plugin, LivingEntity entity, Location spawnLoc, Location endLoc);
	}

	@FunctionalInterface
	public interface BossDeserializer {
		@Nullable BossAbilityGroup deserialize(Plugin plugin, LivingEntity entity) throws Exception;
	}

	/********************************************************************************
	 * Static Fields
	 *******************************************************************************/

	/*
	 * Holds a static reference to the most recently instantiated boss manager
	 */
	private static @MonotonicNonNull BossManager INSTANCE = null;

	private static final Map<String, StatelessBossConstructor> mStatelessBosses;
	private static final Map<String, StatefulBossConstructor> mStatefulBosses;
	private static final Map<String, BossDeserializer> mBossDeserializers;
	public static final Map<String, BossParameters> mBossParameters;

	static {
		/* Stateless bosses are those that have no end location set where a redstone block would be spawned when they die */
		mStatelessBosses = new HashMap<>();
		mStatelessBosses.put(BulletHellSurvivalBoss.identityTag, BulletHellSurvivalBoss::new);
		mStatelessBosses.put(BlockLockBoss.identityTag, BlockLockBoss::new);
		mStatelessBosses.put(ImmortalPassengerBoss.identityTag, ImmortalPassengerBoss::new);
		mStatelessBosses.put(ChestLockBoss.identityTag, ChestLockBoss::new);
		mStatelessBosses.put(UnyieldingBoss.identityTag, UnyieldingBoss::new);
		mStatelessBosses.put(ToughBoss.identityTag, ToughBoss::new);
		mStatelessBosses.put(UnseenBoss.identityTag, UnseenBoss::new);
		mStatelessBosses.put(WhispersBoss.identityTag, WhispersBoss::new);
		mStatelessBosses.put(GenericBoss.identityTag, GenericBoss::new);
		mStatelessBosses.put(HiddenBoss.identityTag, HiddenBoss::new);
		mStatelessBosses.put(InvisibleBoss.identityTag, InvisibleBoss::new);
		mStatelessBosses.put(FireResistantBoss.identityTag, FireResistantBoss::new);
		mStatelessBosses.put(HungerCloudBoss.identityTag, HungerCloudBoss::new);
		mStatelessBosses.put(BlockBreakBoss.identityTag, BlockBreakBoss::new);
		mStatelessBosses.put(PulseLaserBoss.identityTag, PulseLaserBoss::new);
		mStatelessBosses.put(ArcaneLaserBoss.identityTag, ArcaneLaserBoss::new);
		mStatelessBosses.put(WeaponSwitchBoss.identityTag, WeaponSwitchBoss::new);
		mStatelessBosses.put(ShieldSwitchBoss.identityTag, ShieldSwitchBoss::new);
		mStatelessBosses.put(ChargerBoss.identityTag, ChargerBoss::new);
		mStatelessBosses.put(BlastResistBoss.identityTag, BlastResistBoss::new);
		mStatelessBosses.put(InfestedBoss.identityTag, InfestedBoss::new);
		mStatelessBosses.put(FireballBoss.identityTag, FireballBoss::new);
		mStatelessBosses.put(TpBehindBoss.identityTag, TpBehindBoss::new);
		mStatelessBosses.put(TpBehindTargetedBoss.identityTag, TpBehindTargetedBoss::new);
		mStatelessBosses.put(TpSwapBoss.identityTag, TpSwapBoss::new);
		mStatelessBosses.put(FlameNovaBoss.identityTag, FlameNovaBoss::new);
		mStatelessBosses.put(PlayerTargetBoss.identityTag, PlayerTargetBoss::new);
		mStatelessBosses.put(DamageReducedBoss.identityTag, DamageReducedBoss::new);
		mStatelessBosses.put(WinterSnowmanEventBoss.identityTag, WinterSnowmanEventBoss::new);
		mStatelessBosses.put(TrainingDummyBoss.identityTag, TrainingDummyBoss::new);
		mStatelessBosses.put(FestiveTesseractSnowmanBoss.identityTag, FestiveTesseractSnowmanBoss::new);
		mStatelessBosses.put(CrowdControlImmunityBoss.identityTag, CrowdControlImmunityBoss::new);
		mStatelessBosses.put(FloatBoss.identityTag, FloatBoss::new);
		mStatelessBosses.put(FrostNovaBoss.identityTag, FrostNovaBoss::new);
		mStatelessBosses.put(DebuffHitBoss.identityTag, DebuffHitBoss::new);
		mStatelessBosses.put(IceAspectBoss.identityTag, IceAspectBoss::new);
		mStatelessBosses.put(TsunamiChargerBoss.identityTag, TsunamiChargerBoss::new);
		mStatelessBosses.put(BombTossBoss.identityTag, BombTossBoss::new);
		mStatelessBosses.put(BombTossNoBlockBreakBoss.identityTag, BombTossNoBlockBreakBoss::new);
		mStatelessBosses.put(RejuvenationBoss.identityTag, RejuvenationBoss::new);
		mStatelessBosses.put(HandSwapBoss.identityTag, HandSwapBoss::new);
		mStatelessBosses.put(UnstableBoss.identityTag, UnstableBoss::new);
		mStatelessBosses.put(BerserkerBoss.identityTag, BerserkerBoss::new);
		mStatelessBosses.put(SnowballDamageBoss.identityTag, SnowballDamageBoss::new);
		mStatelessBosses.put(CorruptInfestedBoss.identityTag, CorruptInfestedBoss::new);
		mStatelessBosses.put(FlameLaserBoss.identityTag, FlameLaserBoss::new);
		mStatelessBosses.put(SpecterParticleBoss.identityTag, SpecterParticleBoss::new);
		mStatelessBosses.put(DreadnaughtParticleBoss.identityTag, DreadnaughtParticleBoss::new);
		mStatelessBosses.put(DreadlingBoss.identityTag, DreadlingBoss::new);
		mStatelessBosses.put(ProjectileDeflectionBoss.identityTag, ProjectileDeflectionBoss::new);
		mStatelessBosses.put(LivingBladeBoss.identityTag, LivingBladeBoss::new);
		mStatelessBosses.put(PrimordialElementalKaulBoss.identityTag, PrimordialElementalKaulBoss::new);
		mStatelessBosses.put(ImmortalElementalKaulBoss.identityTag, ImmortalElementalKaulBoss::new);
		mStatelessBosses.put(CyanSummonBoss.identityTag, CyanSummonBoss::new);
		mStatelessBosses.put(WitherHitBoss.identityTag, WitherHitBoss::new);
		mStatelessBosses.put(VolatileBoss.identityTag, VolatileBoss::new);
		mStatelessBosses.put(SwapOnDismountBoss.identityTag, SwapOnDismountBoss::new);
		mStatelessBosses.put(PlayerDamageOnlyBoss.identityTag, PlayerDamageOnlyBoss::new);
		mStatelessBosses.put(GrayDemonSummoner.identityTag, GrayDemonSummoner::new);
		mStatelessBosses.put(GrayGolemSummoner.identityTag, GrayGolemSummoner::new);
		mStatelessBosses.put(GrayScarabSummoner.identityTag, GrayScarabSummoner::new);
		mStatelessBosses.put(GrayBookSummoner.identityTag, GrayBookSummoner::new);
		mStatelessBosses.put(GraySummoned.identityTag, GraySummoned::new);
		mStatelessBosses.put(IceBreakBoss.identityTag, IceBreakBoss::new);
		mStatelessBosses.put(PunchResistBoss.identityTag, PunchResistBoss::new);
		mStatelessBosses.put(HalloweenCreeperBoss.identityTag, HalloweenCreeperBoss::new);
		mStatelessBosses.put(NoExperienceBoss.identityTag, NoExperienceBoss::new);
		mStatelessBosses.put(FocusFireBoss.identityTag, FocusFireBoss::new);
		mStatelessBosses.put(ForceBoss.identityTag, ForceBoss::new);
		mStatelessBosses.put(AvengerBoss.identityTag, AvengerBoss::new);
		mStatelessBosses.put(RageBoss.identityTag, RageBoss::new);
		mStatelessBosses.put(EarthshakeBoss.identityTag, EarthshakeBoss::new);
		mStatelessBosses.put(MagicArrowBoss.identityTag, MagicArrowBoss::new);
		mStatelessBosses.put(SeekingProjectileBoss.identityTag, SeekingProjectileBoss::new);
		mStatelessBosses.put(TrackingProjectileBoss.identityTag, TrackingProjectileBoss::new);
		mStatelessBosses.put(WrathBoss.identityTag, WrathBoss::new);
		mStatelessBosses.put(LeapBoss.identityTag, LeapBoss::new);
		mStatelessBosses.put(BarrierBoss.identityTag, BarrierBoss::new);
		mStatelessBosses.put(CrowdControlResistanceBoss.identityTag, CrowdControlResistanceBoss::new);
		mStatelessBosses.put(MeteorSlamBoss.identityTag, MeteorSlamBoss::new);
		mStatelessBosses.put(SwingBoss.identityTag, SwingBoss::new);
		mStatelessBosses.put(MistMob.identityTag, MistMob::new);
		mStatelessBosses.put(HookBoss.identityTag, HookBoss::new);
		mStatelessBosses.put(FrostGiantIcicle.identityTag, FrostGiantIcicle::new);
		mStatelessBosses.put(SpellSlingerBoss.identityTag, SpellSlingerBoss::new);
		mStatelessBosses.put(VindictiveBoss.identityTag, VindictiveBoss::new);
		mStatelessBosses.put(ShadowTrailBoss.identityTag, ShadowTrailBoss::new);
		mStatelessBosses.put(KineticProjectileBoss.identityTag, KineticProjectileBoss::new);
		mStatelessBosses.put(FlameTrailBoss.identityTag, FlameTrailBoss::new);
		mStatelessBosses.put(ShadeParticleBoss.identityTag, ShadeParticleBoss::new);
		mStatelessBosses.put(FireBombTossBoss.identityTag, FireBombTossBoss::new);
		mStatelessBosses.put(CommanderBoss.identityTag, CommanderBoss::new);
		mStatelessBosses.put(ShadePossessedBoss.identityTag, ShadePossessedBoss::new);
		mStatelessBosses.put(TwistedDespairBoss.identityTag, TwistedDespairBoss::new);
		mStatelessBosses.put(CoordinatedAttackBoss.identityTag, CoordinatedAttackBoss::new);
		mStatelessBosses.put(AbilitySilenceBoss.identityTag, AbilitySilenceBoss::new);
		mStatelessBosses.put(ShiftingBoss.identityTag, ShiftingBoss::new);
		mStatelessBosses.put(BulletHellBoss.identityTag, BulletHellBoss::new);
		mStatelessBosses.put(CarapaceBoss.identityTag, CarapaceBoss::new);
		mStatelessBosses.put(KamikazeBoss.identityTag, KamikazeBoss::new);
		mStatelessBosses.put(TinyBombTossBoss.identityTag, TinyBombTossBoss::new);
		mStatelessBosses.put(AntiRangeBoss.identityTag, AntiRangeBoss::new);
		mStatelessBosses.put(AntiMeleeBoss.identityTag, AntiMeleeBoss::new);
		mStatelessBosses.put(AntiSuffocationBoss.identityTag, AntiSuffocationBoss::new);
		mStatelessBosses.put(DamageCapBoss.identityTag, DamageCapBoss::new);
		mStatelessBosses.put(AntiRangeChivalrousBoss.identityTag, AntiRangeChivalrousBoss::new);
		mStatelessBosses.put(ImmortalMountBoss.identityTag, ImmortalMountBoss::new);
		mStatelessBosses.put(SilenceOnHitBoss.identityTag, SilenceOnHitBoss::new);
		mStatelessBosses.put(FalseSpiritPortal.identityTag, FalseSpiritPortal::new);
		mStatelessBosses.put(TffBookSummonBoss.identityTag, TffBookSummonBoss::new);
		mStatelessBosses.put(ArcaneProjectileBoss.identityTag, ArcaneProjectileBoss::new);
		mStatelessBosses.put(JumpBoss.identityTag, JumpBoss::new);
		mStatelessBosses.put(RebornBoss.identityTag, RebornBoss::new);
		mStatelessBosses.put(NoFireBoss.identityTag, NoFireBoss::new);
		mStatelessBosses.put(DistanceCloserBoss.identityTag, DistanceCloserBoss::new);
		mStatelessBosses.put(WeakHookBoss.identityTag, WeakHookBoss::new);
		mStatelessBosses.put(AuraEffectBoss.identityTag, AuraEffectBoss::new);
		mStatelessBosses.put(DummyDecoyBoss.identityTag, DummyDecoyBoss::new);
		mStatelessBosses.put(LaserBoss.identityTag, LaserBoss::new);
		mStatelessBosses.put(SpinBoss.identityTag, SpinBoss::new);
		mStatelessBosses.put(OnHitBoss.identityTag, OnHitBoss::new);
		mStatelessBosses.put(NovaBoss.identityTag, NovaBoss::new);
		mStatelessBosses.put(ProjectileBoss.identityTag, ProjectileBoss::new);
		mStatelessBosses.put(RainbowParrot.identityTag, RainbowParrot::new);
		mStatelessBosses.put(SpawnMobsBoss.identityTag, SpawnMobsBoss::new);
		mStatelessBosses.put(LandSlowBoss.identityTag, LandSlowBoss::new);
		mStatelessBosses.put(PounceBoss.identityTag, PounceBoss::new);
		mStatelessBosses.put(NoAbilityDamageBoss.identityTag, NoAbilityDamageBoss::new);
		mStatelessBosses.put(NoGlowingBoss.identityTag, NoGlowingBoss::new);
		mStatelessBosses.put(GenericTargetBoss.identityTag, GenericTargetBoss::new);
		mStatelessBosses.put(MobRisingBoss.identityTag, MobRisingBoss::new);
		mStatelessBosses.put(GrenadeLauncherBoss.identityTag, GrenadeLauncherBoss::new);
		mStatelessBosses.put(SizeChangerBoss.identityTag, SizeChangerBoss::new);
		mStatelessBosses.put(DelveScalingBoss.identityTag, DelveScalingBoss::new);
		mStatelessBosses.put(DeathSummonBoss.identityTag, DeathSummonBoss::new);
		mStatelessBosses.put(SummonOnExplosionBoss.identityTag, SummonOnExplosionBoss::new);
		mStatelessBosses.put(HostileBoss.identityTag, HostileBoss::new);
		mStatelessBosses.put(StarfallBoss.identityTag, StarfallBoss::new);
		mStatelessBosses.put(ShatterBoss.identityTag, ShatterBoss::new);
		mStatelessBosses.put(StatMultiplierBoss.identityTag, StatMultiplierBoss::new);
		mStatelessBosses.put(SpectralSummonBoss.identityTag, SpectralSummonBoss::new);
		mStatelessBosses.put(DreadfulSummonBoss.identityTag, DreadfulSummonBoss::new);
		mStatelessBosses.put(FriendlyBoss.identityTag, FriendlyBoss::new);
		mStatelessBosses.put(MageCosmicMoonbladeBoss.identityTag, MageCosmicMoonbladeBoss::new);
		mStatelessBosses.put(WarriorShieldWallBoss.identityTag, WarriorShieldWallBoss::new);
		mStatelessBosses.put(DodgeBoss.identityTag, DodgeBoss::new);
		mStatelessBosses.put(BlockPlacerBoss.identityTag, BlockPlacerBoss::new);
		mStatelessBosses.put(ScoutVolleyBoss.identityTag, ScoutVolleyBoss::new);
		mStatelessBosses.put(WarlockAmpHexBoss.identityTag, WarlockAmpHexBoss::new);
		mStatelessBosses.put(LimitedLifespanBoss.identityTag, LimitedLifespanBoss::new);
		mStatelessBosses.put(BlueFireBoss.identityTag, BlueFireBoss::new);
		mStatelessBosses.put(BlueEarthBoss.identityTag, BlueEarthBoss::new);
		mStatelessBosses.put(BlueAirBoss.identityTag, BlueAirBoss::new);
		mStatelessBosses.put(BlueWaterBoss.identityTag, BlueWaterBoss::new);
		mStatelessBosses.put(DropShardBoss.identityTag, DropShardBoss::new);
		mStatelessBosses.put(BlueStrikeDaggerCraftingBoss.identityTag, BlueStrikeDaggerCraftingBoss::construct);
		mStatelessBosses.put(BlueStrikeTargetNPCBoss.identityTag, BlueStrikeTargetNPCBoss::new);
		mStatelessBosses.put(BlueStrikeTurretBoss.identityTag, BlueStrikeTurretBoss::new);
		mStatelessBosses.put(LavaCannonBoss.identityTag, LavaCannonBoss::construct);
		mStatelessBosses.put(PhasesManagerBoss.identityTag, PhasesManagerBoss::new);
		mStatelessBosses.put(SoundBoss.identityTag, SoundBoss::new);
		mStatelessBosses.put(RedstoneBoss.identityTag, RedstoneBoss::new);
		mStatelessBosses.put(SlashAttackBoss.identityTag, SlashAttackBoss::new);
		mStatelessBosses.put(DashBoss.identityTag, DashBoss::new);

		mStatelessBosses.put(LichMageBoss.identityTag, LichMageBoss::new);
		mStatelessBosses.put(LichRogueBoss.identityTag, LichRogueBoss::new);
		mStatelessBosses.put(LichClericBoss.identityTag, LichClericBoss::new);
		mStatelessBosses.put(LichWarlockBoss.identityTag, LichWarlockBoss::new);
		mStatelessBosses.put(LichAlchBoss.identityTag, LichAlchBoss::new);
		mStatelessBosses.put(LichScoutBoss.identityTag, LichScoutBoss::new);
		mStatelessBosses.put(LichWarriorBoss.identityTag, LichWarriorBoss::new);
		mStatelessBosses.put(LichConquestBoss.identityTag, LichConquestBoss::new);
		mStatelessBosses.put(LichDemiseBoss.identityTag, LichDemiseBoss::new);
		mStatelessBosses.put(LichJudgementBoss.identityTag, LichJudgementBoss::new);
		mStatelessBosses.put(LichStrifeBoss.identityTag, LichStrifeBoss::new);
		mStatelessBosses.put(LichCurseBoss.identityTag, LichCurseBoss::new);
		mStatelessBosses.put(LichShieldBoss.identityTag, LichShieldBoss::new);
		mStatelessBosses.put(LichKeyGlowBoss.identityTag, LichKeyGlowBoss::new);
		mStatelessBosses.put(FestiveTessUpgradeSnowmenBoss.identityTag, FestiveTessUpgradeSnowmenBoss::new);
		mStatelessBosses.put(MetalmancyBoss.identityTag, MetalmancyBoss::new);
		mStatelessBosses.put(RestlessSoulsBoss.identityTag, RestlessSoulsBoss::new);
		mStatelessBosses.put(AlchemicalAberrationBoss.identityTag, AlchemicalAberrationBoss::new);
		mStatelessBosses.put(AbilityMarkerEntityBoss.identityTag, AbilityMarkerEntityBoss::new);
		mStatelessBosses.put(ThrowSummonBoss.identityTag, ThrowSummonBoss::new);
		mStatelessBosses.put(PotionThrowBoss.identityTag, PotionThrowBoss::new);
		mStatelessBosses.put(TwistedMiniBoss.identityTag, TwistedMiniBoss::new);
		mStatelessBosses.put(BrownPositiveBoss.identityTag, BrownPositiveBoss::new);
		mStatelessBosses.put(BrownNegativeBoss.identityTag, BrownNegativeBoss::new);
		mStatelessBosses.put(BrownMagnetSwapBoss.identityTag, BrownMagnetSwapBoss::new);
		mStatelessBosses.put(ParadoxSwapBoss.identityTag, ParadoxSwapBoss::new);
		mStatelessBosses.put(TemporalShieldBoss.identityTag, TemporalShieldBoss::new);
		mStatelessBosses.put(GalleryMobRisingBoss.identityTag, GalleryMobRisingBoss::new);
		mStatelessBosses.put(GallerySummonMobBoss.identityTag, GallerySummonMobBoss::new);
		mStatelessBosses.put(TagScalingBoss.identityTag, TagScalingBoss::new);
		mStatelessBosses.put(CancelDamageBoss.identityTag, CancelDamageBoss::new);


		/* Stateful bosses have a remembered spawn location and end location where a redstone block is set when they die */
		mStatefulBosses = new HashMap<>();
		mStatefulBosses.put(CAxtal.identityTag, CAxtal::new);
		mStatefulBosses.put(Masked.identityTag, Masked::new);
		mStatefulBosses.put(Virius.identityTag, Virius::new);
		mStatefulBosses.put(Orangyboi.identityTag, Orangyboi::new);
		mStatefulBosses.put(Azacor.identityTag, Azacor::new);
		mStatefulBosses.put(CShura.identityTag, CShura::new);
		mStatefulBosses.put(SwordsageRichter.identityTag, SwordsageRichter::new);
		mStatefulBosses.put(Kaul.identityTag, Kaul::new);
		mStatefulBosses.put(TCalin.identityTag, TCalin::new);
		mStatefulBosses.put(CrownbearerBoss.identityTag, CrownbearerBoss::new);
		mStatefulBosses.put(RabbitGodBoss.identityTag, RabbitGodBoss::new);
		mStatefulBosses.put(OldLabsBoss.identityTag, OldLabsBoss::new);
		mStatefulBosses.put(HeadlessHorsemanBoss.identityTag, HeadlessHorsemanBoss::new);
		mStatefulBosses.put(Varcosa.identityTag, Varcosa::new);
		mStatefulBosses.put(FrostGiant.identityTag, FrostGiant::new);
		mStatefulBosses.put(TealQuestBoss.identityTag, TealQuestBoss::new);
		mStatefulBosses.put(VarcosaSummonerBoss.identityTag, VarcosaSummonerBoss::new);
		mStatefulBosses.put(VarcosasLastBreathBoss.identityTag, VarcosasLastBreathBoss::new);
		mStatefulBosses.put(VarcosaLingeringWillBoss.identityTag, VarcosaLingeringWillBoss::new);
		mStatefulBosses.put(MimicQueen.identityTag, MimicQueen::new);
		mStatefulBosses.put(FalseSpirit.identityTag, FalseSpirit::new);
		mStatefulBosses.put(SnowSpirit.identityTag, SnowSpirit::new);
		mStatefulBosses.put(Lich.identityTag, Lich::new);
		mStatefulBosses.put(Hedera.identityTag, Hedera::new);
		mStatefulBosses.put(Davey.identityTag, Davey::new);
		mStatefulBosses.put(Nucleus.identityTag, Nucleus::new);
		mStatefulBosses.put(Ghalkor.identityTag, Ghalkor::new);
		mStatefulBosses.put(Svalgot.identityTag, Svalgot::new);
		mStatefulBosses.put(BeastOfTheBlackFlame.identityTag, BeastOfTheBlackFlame::new);
		mStatefulBosses.put(RKitxet.identityTag, RKitxet::new);
		mStatefulBosses.put(VerdantMinibossBoss.identityTag, VerdantMinibossBoss::new);
		mStatefulBosses.put(PortalBoss.identityTag, PortalBoss::new);
		mStatefulBosses.put(ImperialConstruct.identityTag, ImperialConstruct::new);
		mStatefulBosses.put(Samwell.identityTag, Samwell::new);
		mStatefulBosses.put(TealSpirit.identityTag, TealSpirit::new);

		/* All bosses have a deserializer which gives the boss back their abilities when chunks re-load */
		mBossDeserializers = new HashMap<String, BossDeserializer>();
		mBossDeserializers.put(BulletHellSurvivalBoss.identityTag, BulletHellSurvivalBoss::deserialize);
		mBossDeserializers.put(BlockLockBoss.identityTag, BlockLockBoss::deserialize);
		mBossDeserializers.put(ImmortalPassengerBoss.identityTag, ImmortalPassengerBoss::deserialize);
		mBossDeserializers.put(ChestLockBoss.identityTag, ChestLockBoss::deserialize);
		mBossDeserializers.put(UnyieldingBoss.identityTag, UnyieldingBoss::deserialize);
		mBossDeserializers.put(ToughBoss.identityTag, ToughBoss::deserialize);
		mBossDeserializers.put(UnseenBoss.identityTag, UnseenBoss::deserialize);
		mBossDeserializers.put(WhispersBoss.identityTag, WhispersBoss::deserialize);
		mBossDeserializers.put(GenericBoss.identityTag, GenericBoss::deserialize);
		mBossDeserializers.put(InvisibleBoss.identityTag, InvisibleBoss::deserialize);
		mBossDeserializers.put(HiddenBoss.identityTag, HiddenBoss::deserialize);
		mBossDeserializers.put(FireResistantBoss.identityTag, FireResistantBoss::deserialize);
		mBossDeserializers.put(HungerCloudBoss.identityTag, HungerCloudBoss::deserialize);
		mBossDeserializers.put(BlockBreakBoss.identityTag, BlockBreakBoss::deserialize);
		mBossDeserializers.put(PulseLaserBoss.identityTag, PulseLaserBoss::deserialize);
		mBossDeserializers.put(ArcaneLaserBoss.identityTag, ArcaneLaserBoss::deserialize);
		mBossDeserializers.put(WeaponSwitchBoss.identityTag, WeaponSwitchBoss::deserialize);
		mBossDeserializers.put(ShieldSwitchBoss.identityTag, ShieldSwitchBoss::deserialize);
		mBossDeserializers.put(ChargerBoss.identityTag, ChargerBoss::deserialize);
		mBossDeserializers.put(BlastResistBoss.identityTag, BlastResistBoss::deserialize);
		mBossDeserializers.put(InfestedBoss.identityTag, InfestedBoss::deserialize);
		mBossDeserializers.put(FireballBoss.identityTag, FireballBoss::deserialize);
		mBossDeserializers.put(TpBehindBoss.identityTag, TpBehindBoss::deserialize);
		mBossDeserializers.put(TpBehindTargetedBoss.identityTag, TpBehindTargetedBoss::deserialize);
		mBossDeserializers.put(TpSwapBoss.identityTag, TpSwapBoss::deserialize);
		mBossDeserializers.put(FlameNovaBoss.identityTag, FlameNovaBoss::deserialize);
		mBossDeserializers.put(PlayerTargetBoss.identityTag, PlayerTargetBoss::deserialize);
		mBossDeserializers.put(DamageReducedBoss.identityTag, DamageReducedBoss::deserialize);
		mBossDeserializers.put(WinterSnowmanEventBoss.identityTag, WinterSnowmanEventBoss::deserialize);
		mBossDeserializers.put(TrainingDummyBoss.identityTag, TrainingDummyBoss::deserialize);
		mBossDeserializers.put(FestiveTesseractSnowmanBoss.identityTag, FestiveTesseractSnowmanBoss::deserialize);
		mBossDeserializers.put(CrowdControlImmunityBoss.identityTag, CrowdControlImmunityBoss::deserialize);
		mBossDeserializers.put(FloatBoss.identityTag, FloatBoss::deserialize);
		mBossDeserializers.put(FrostNovaBoss.identityTag, FrostNovaBoss::deserialize);
		mBossDeserializers.put(DebuffHitBoss.identityTag, DebuffHitBoss::deserialize);
		mBossDeserializers.put(IceAspectBoss.identityTag, IceAspectBoss::deserialize);
		mBossDeserializers.put(CAxtal.identityTag, CAxtal::deserialize);
		mBossDeserializers.put(Masked.identityTag, Masked::deserialize);
		mBossDeserializers.put(Virius.identityTag, Virius::deserialize);
		mBossDeserializers.put(Orangyboi.identityTag, Orangyboi::deserialize);
		mBossDeserializers.put(Azacor.identityTag, Azacor::deserialize);
		mBossDeserializers.put(CShura.identityTag, CShura::deserialize);
		mBossDeserializers.put(TsunamiChargerBoss.identityTag, TsunamiChargerBoss::deserialize);
		mBossDeserializers.put(BombTossBoss.identityTag, BombTossBoss::deserialize);
		mBossDeserializers.put(BombTossNoBlockBreakBoss.identityTag, BombTossNoBlockBreakBoss::deserialize);
		mBossDeserializers.put(RejuvenationBoss.identityTag, RejuvenationBoss::deserialize);
		mBossDeserializers.put(HandSwapBoss.identityTag, HandSwapBoss::deserialize);
		mBossDeserializers.put(UnstableBoss.identityTag, UnstableBoss::deserialize);
		mBossDeserializers.put(BerserkerBoss.identityTag, BerserkerBoss::deserialize);
		mBossDeserializers.put(SnowballDamageBoss.identityTag, SnowballDamageBoss::deserialize);
		mBossDeserializers.put(CorruptInfestedBoss.identityTag, CorruptInfestedBoss::deserialize);
		mBossDeserializers.put(FlameLaserBoss.identityTag, FlameLaserBoss::deserialize);
		mBossDeserializers.put(SwordsageRichter.identityTag, SwordsageRichter::deserialize);
		mBossDeserializers.put(SpecterParticleBoss.identityTag, SpecterParticleBoss::deserialize);
		mBossDeserializers.put(DreadnaughtParticleBoss.identityTag, DreadnaughtParticleBoss::deserialize);
		mBossDeserializers.put(DreadlingBoss.identityTag, DreadlingBoss::deserialize);
		mBossDeserializers.put(ProjectileDeflectionBoss.identityTag, ProjectileDeflectionBoss::deserialize);
		mBossDeserializers.put(LivingBladeBoss.identityTag, LivingBladeBoss::deserialize);
		mBossDeserializers.put(Kaul.identityTag, Kaul::deserialize);
		mBossDeserializers.put(PrimordialElementalKaulBoss.identityTag, PrimordialElementalKaulBoss::deserialize);
		mBossDeserializers.put(ImmortalElementalKaulBoss.identityTag, ImmortalElementalKaulBoss::deserialize);
		mBossDeserializers.put(TCalin.identityTag, TCalin::deserialize);
		mBossDeserializers.put(CrownbearerBoss.identityTag, CrownbearerBoss::deserialize);
		mBossDeserializers.put(CyanSummonBoss.identityTag, CyanSummonBoss::deserialize);
		mBossDeserializers.put(WitherHitBoss.identityTag, WitherHitBoss::deserialize);
		mBossDeserializers.put(VolatileBoss.identityTag, VolatileBoss::deserialize);
		mBossDeserializers.put(PlayerDamageOnlyBoss.identityTag, PlayerDamageOnlyBoss::deserialize);
		mBossDeserializers.put(RabbitGodBoss.identityTag, RabbitGodBoss::deserialize);
		mBossDeserializers.put(GrayDemonSummoner.identityTag, GrayDemonSummoner::deserialize);
		mBossDeserializers.put(GrayGolemSummoner.identityTag, GrayGolemSummoner::deserialize);
		mBossDeserializers.put(GrayScarabSummoner.identityTag, GrayScarabSummoner::deserialize);
		mBossDeserializers.put(GrayBookSummoner.identityTag, GrayBookSummoner::deserialize);
		mBossDeserializers.put(GraySummoned.identityTag, GraySummoned::deserialize);
		mBossDeserializers.put(IceBreakBoss.identityTag, IceBreakBoss::deserialize);
		mBossDeserializers.put(PunchResistBoss.identityTag, PunchResistBoss::deserialize);
		mBossDeserializers.put(OldLabsBoss.identityTag, OldLabsBoss::deserialize);
		mBossDeserializers.put(HalloweenCreeperBoss.identityTag, HalloweenCreeperBoss::deserialize);
		mBossDeserializers.put(HeadlessHorsemanBoss.identityTag, HeadlessHorsemanBoss::deserialize);
		mBossDeserializers.put(NoExperienceBoss.identityTag, NoExperienceBoss::deserialize);
		mBossDeserializers.put(FocusFireBoss.identityTag, FocusFireBoss::deserialize);
		mBossDeserializers.put(ForceBoss.identityTag, ForceBoss::deserialize);
		mBossDeserializers.put(AvengerBoss.identityTag, AvengerBoss::deserialize);
		mBossDeserializers.put(RageBoss.identityTag, RageBoss::deserialize);
		mBossDeserializers.put(Varcosa.identityTag, Varcosa::deserialize);
		mBossDeserializers.put(VarcosaSummonerBoss.identityTag, VarcosaSummonerBoss::deserialize);
		mBossDeserializers.put(VarcosasLastBreathBoss.identityTag, VarcosasLastBreathBoss::deserialize);
		mBossDeserializers.put(VarcosaLingeringWillBoss.identityTag, VarcosaLingeringWillBoss::deserialize);
		mBossDeserializers.put(EarthshakeBoss.identityTag, EarthshakeBoss::deserialize);
		mBossDeserializers.put(MagicArrowBoss.identityTag, MagicArrowBoss::deserialize);
		mBossDeserializers.put(SeekingProjectileBoss.identityTag, SeekingProjectileBoss::deserialize);
		mBossDeserializers.put(TrackingProjectileBoss.identityTag, TrackingProjectileBoss::deserialize);
		mBossDeserializers.put(MimicQueen.identityTag, MimicQueen::deserialize);
		mBossDeserializers.put(WrathBoss.identityTag, WrathBoss::deserialize);
		mBossDeserializers.put(LeapBoss.identityTag, LeapBoss::deserialize);
		mBossDeserializers.put(BarrierBoss.identityTag, BarrierBoss::deserialize);
		mBossDeserializers.put(CrowdControlResistanceBoss.identityTag, CrowdControlResistanceBoss::deserialize);
		mBossDeserializers.put(MeteorSlamBoss.identityTag, MeteorSlamBoss::deserialize);
		mBossDeserializers.put(SwingBoss.identityTag, SwingBoss::deserialize);
		mBossDeserializers.put(TealQuestBoss.identityTag, TealQuestBoss::deserialize);
		mBossDeserializers.put(MistMob.identityTag, MistMob::deserialize);
		mBossDeserializers.put(HookBoss.identityTag, HookBoss::deserialize);
		mBossDeserializers.put(FrostGiant.identityTag, FrostGiant::deserialize);
		mBossDeserializers.put(FrostGiantIcicle.identityTag, FrostGiantIcicle::deserialize);
		mBossDeserializers.put(SpellSlingerBoss.identityTag, SpellSlingerBoss::deserialize);
		mBossDeserializers.put(VindictiveBoss.identityTag, VindictiveBoss::deserialize);
		mBossDeserializers.put(ShadowTrailBoss.identityTag, ShadowTrailBoss::deserialize);
		mBossDeserializers.put(KineticProjectileBoss.identityTag, KineticProjectileBoss::deserialize);
		mBossDeserializers.put(FlameTrailBoss.identityTag, FlameTrailBoss::deserialize);
		mBossDeserializers.put(ShadeParticleBoss.identityTag, ShadeParticleBoss::deserialize);
		mBossDeserializers.put(FireBombTossBoss.identityTag, FireBombTossBoss::deserialize);
		mBossDeserializers.put(CommanderBoss.identityTag, CommanderBoss::deserialize);
		mBossDeserializers.put(ShadePossessedBoss.identityTag, ShadePossessedBoss::deserialize);
		mBossDeserializers.put(TwistedDespairBoss.identityTag, TwistedDespairBoss::deserialize);
		mBossDeserializers.put(CoordinatedAttackBoss.identityTag, CoordinatedAttackBoss::deserialize);
		mBossDeserializers.put(AbilitySilenceBoss.identityTag, AbilitySilenceBoss::deserialize);
		mBossDeserializers.put(ShiftingBoss.identityTag, ShiftingBoss::deserialize);
		mBossDeserializers.put(CarapaceBoss.identityTag, CarapaceBoss::deserialize);
		mBossDeserializers.put(BulletHellBoss.identityTag, BulletHellBoss::deserialize);
		mBossDeserializers.put(KamikazeBoss.identityTag, KamikazeBoss::deserialize);
		mBossDeserializers.put(TinyBombTossBoss.identityTag, TinyBombTossBoss::deserialize);
		mBossDeserializers.put(AntiRangeBoss.identityTag, AntiRangeBoss::deserialize);
		mBossDeserializers.put(AntiMeleeBoss.identityTag, AntiMeleeBoss::deserialize);
		mBossDeserializers.put(AntiSuffocationBoss.identityTag, AntiSuffocationBoss::deserialize);
		mBossDeserializers.put(DamageCapBoss.identityTag, DamageCapBoss::deserialize);
		mBossDeserializers.put(AntiRangeChivalrousBoss.identityTag, AntiRangeChivalrousBoss::deserialize);
		mBossDeserializers.put(ImmortalMountBoss.identityTag, ImmortalMountBoss::deserialize);
		mBossDeserializers.put(SilenceOnHitBoss.identityTag, SilenceOnHitBoss::deserialize);
		mBossDeserializers.put(FalseSpirit.identityTag, FalseSpirit::deserialize);
		mBossDeserializers.put(FalseSpiritPortal.identityTag, FalseSpiritPortal::deserialize);
		mBossDeserializers.put(SnowSpirit.identityTag, SnowSpirit::deserialize);
		mBossDeserializers.put(TffBookSummonBoss.identityTag, TffBookSummonBoss::deserialize);
		mBossDeserializers.put(ArcaneProjectileBoss.identityTag, ArcaneProjectileBoss::deserialize);
		mBossDeserializers.put(JumpBoss.identityTag, JumpBoss::deserialize);
		mBossDeserializers.put(RebornBoss.identityTag, RebornBoss::deserialize);
		mBossDeserializers.put(NoFireBoss.identityTag, NoFireBoss::deserialize);
		mBossDeserializers.put(Ghalkor.identityTag, Ghalkor::deserialize);
		mBossDeserializers.put(Svalgot.identityTag, Svalgot::deserialize);
		mBossDeserializers.put(BeastOfTheBlackFlame.identityTag, BeastOfTheBlackFlame::deserialize);
		mBossDeserializers.put(DistanceCloserBoss.identityTag, DistanceCloserBoss::deserialize);
		mBossDeserializers.put(WeakHookBoss.identityTag, WeakHookBoss::deserialize);
		mBossDeserializers.put(AuraEffectBoss.identityTag, AuraEffectBoss::deserialize);
		mBossDeserializers.put(DummyDecoyBoss.identityTag, DummyDecoyBoss::deserialize);
		mBossDeserializers.put(Hedera.identityTag, Hedera::deserialize);
		mBossDeserializers.put(Davey.identityTag, Davey::deserialize);
		mBossDeserializers.put(Nucleus.identityTag, Nucleus::deserialize);
		mBossDeserializers.put(LaserBoss.identityTag, LaserBoss::deserialize);
		mBossDeserializers.put(SpinBoss.identityTag, SpinBoss::deserialize);
		mBossDeserializers.put(OnHitBoss.identityTag, OnHitBoss::deserialize);
		mBossDeserializers.put(NovaBoss.identityTag, NovaBoss::deserialize);
		mBossDeserializers.put(ProjectileBoss.identityTag, ProjectileBoss::deserialize);
		mBossDeserializers.put(RainbowParrot.identityTag, RainbowParrot::deserialize);
		mBossDeserializers.put(SpawnMobsBoss.identityTag, SpawnMobsBoss::deserialize);
		mBossDeserializers.put(LandSlowBoss.identityTag, LandSlowBoss::deserialize);
		mBossDeserializers.put(PounceBoss.identityTag, PounceBoss::deserialize);
		mBossDeserializers.put(NoAbilityDamageBoss.identityTag, NoAbilityDamageBoss::deserialize);
		mBossDeserializers.put(NoGlowingBoss.identityTag, NoGlowingBoss::deserialize);
		mBossDeserializers.put(RKitxet.identityTag, RKitxet::deserialize);
		mBossDeserializers.put(VerdantMinibossBoss.identityTag, VerdantMinibossBoss::deserialize);
		mBossDeserializers.put(GenericTargetBoss.identityTag, GenericTargetBoss::deserialize);
		mBossDeserializers.put(MobRisingBoss.identityTag, MobRisingBoss::deserialize);
		mBossDeserializers.put(GrenadeLauncherBoss.identityTag, GrenadeLauncherBoss::deserialize);
		mBossDeserializers.put(SizeChangerBoss.identityTag, SizeChangerBoss::deserialize);
		mBossDeserializers.put(DelveScalingBoss.identityTag, DelveScalingBoss::deserialize);
		mBossDeserializers.put(DeathSummonBoss.identityTag, DeathSummonBoss::deserialize);
		mBossDeserializers.put(StarfallBoss.identityTag, StarfallBoss::deserialize);
		mBossDeserializers.put(ShatterBoss.identityTag, ShatterBoss::deserialize);
		mBossDeserializers.put(SpectralSummonBoss.identityTag, SpectralSummonBoss::deserialize);
		mBossDeserializers.put(DreadfulSummonBoss.identityTag, DreadfulSummonBoss::deserialize);
		mBossDeserializers.put(StatMultiplierBoss.identityTag, StatMultiplierBoss::deserialize);
		mBossDeserializers.put(SummonOnExplosionBoss.identityTag, SummonOnExplosionBoss::deserialize);
		mBossDeserializers.put(WarriorShieldWallBoss.identityTag, WarriorShieldWallBoss::deserialize);
		mBossDeserializers.put(WarlockAmpHexBoss.identityTag, WarlockAmpHexBoss::deserialize);
		mBossDeserializers.put(DodgeBoss.identityTag, DodgeBoss::deserialize);
		mBossDeserializers.put(BlockPlacerBoss.identityTag, BlockPlacerBoss::deserialize);
		mBossDeserializers.put(PotionThrowBoss.identityTag, PotionThrowBoss::deserialize);
		mBossDeserializers.put(LimitedLifespanBoss.identityTag, LimitedLifespanBoss::deserialize);
		mBossDeserializers.put(BlueFireBoss.identityTag, BlueFireBoss::deserialize);
		mBossDeserializers.put(BlueEarthBoss.identityTag, BlueEarthBoss::deserialize);
		mBossDeserializers.put(BlueAirBoss.identityTag, BlueAirBoss::deserialize);
		mBossDeserializers.put(BlueWaterBoss.identityTag, BlueWaterBoss::deserialize);
		mBossDeserializers.put(Samwell.identityTag, Samwell::deserialize);
		mBossDeserializers.put(BlueStrikeDaggerCraftingBoss.identityTag, BlueStrikeDaggerCraftingBoss::deserialize);
		mBossDeserializers.put(BlueStrikeTargetNPCBoss.identityTag, BlueStrikeTargetNPCBoss::deserialize);
		mBossDeserializers.put(BlueStrikeTurretBoss.identityTag, BlueStrikeTurretBoss::deserialize);
		mBossDeserializers.put(LavaCannonBoss.identityTag, LavaCannonBoss::deserialize);
		mBossDeserializers.put(PhasesManagerBoss.identityTag, PhasesManagerBoss::deserialize);
		mBossDeserializers.put(SoundBoss.identityTag, SoundBoss::deserialize);
		mBossDeserializers.put(RedstoneBoss.identityTag, RedstoneBoss::deserialize);
		mBossDeserializers.put(SlashAttackBoss.identityTag, SlashAttackBoss::deserialize);
		mBossDeserializers.put(DashBoss.identityTag, DashBoss::deserialize);

		mBossDeserializers.put(Lich.identityTag, Lich::deserialize);
		mBossDeserializers.put(LichAlchBoss.identityTag, LichAlchBoss::deserialize);
		mBossDeserializers.put(LichClericBoss.identityTag, LichClericBoss::deserialize);
		mBossDeserializers.put(LichMageBoss.identityTag, LichMageBoss::deserialize);
		mBossDeserializers.put(LichRogueBoss.identityTag, LichRogueBoss::deserialize);
		mBossDeserializers.put(LichScoutBoss.identityTag, LichScoutBoss::deserialize);
		mBossDeserializers.put(LichWarlockBoss.identityTag, LichWarlockBoss::deserialize);
		mBossDeserializers.put(LichWarriorBoss.identityTag, LichWarriorBoss::deserialize);
		mBossDeserializers.put(LichConquestBoss.identityTag, LichConquestBoss::deserialize);
		mBossDeserializers.put(LichDemiseBoss.identityTag, LichDemiseBoss::deserialize);
		mBossDeserializers.put(LichJudgementBoss.identityTag, LichJudgementBoss::deserialize);
		mBossDeserializers.put(LichStrifeBoss.identityTag, LichStrifeBoss::deserialize);
		mBossDeserializers.put(LichCurseBoss.identityTag, LichCurseBoss::deserialize);
		mBossDeserializers.put(LichShieldBoss.identityTag, LichShieldBoss::deserialize);
		mBossDeserializers.put(LichKeyGlowBoss.identityTag, LichKeyGlowBoss::deserialize);
		mBossDeserializers.put(FestiveTessUpgradeSnowmenBoss.identityTag, FestiveTessUpgradeSnowmenBoss::deserialize);
		mBossDeserializers.put(MetalmancyBoss.identityTag, MetalmancyBoss::deserialize);
		mBossDeserializers.put(RestlessSoulsBoss.identityTag, RestlessSoulsBoss::deserialize);
		mBossDeserializers.put(AlchemicalAberrationBoss.identityTag, AlchemicalAberrationBoss::deserialize);
		mBossDeserializers.put(AbilityMarkerEntityBoss.identityTag, AbilityMarkerEntityBoss::deserialize);
		mBossDeserializers.put(ThrowSummonBoss.identityTag, ThrowSummonBoss::deserialize);
		mBossDeserializers.put(HostileBoss.identityTag, HostileBoss::deserialize);
		mBossDeserializers.put(FriendlyBoss.identityTag, FriendlyBoss::deserialize);
		mBossDeserializers.put(PortalBoss.identityTag, PortalBoss::deserialize);
		mBossDeserializers.put(ImperialConstruct.identityTag, ImperialConstruct::deserialize);
		mBossDeserializers.put(ScoutVolleyBoss.identityTag, ScoutVolleyBoss::deserialize);
		mBossDeserializers.put(MageCosmicMoonbladeBoss.identityTag, MageCosmicMoonbladeBoss::deserialize);
		mBossDeserializers.put(TwistedMiniBoss.identityTag, TwistedMiniBoss::deserialize);
		mBossDeserializers.put(BrownNegativeBoss.identityTag, BrownNegativeBoss::deserialize);
		mBossDeserializers.put(BrownPositiveBoss.identityTag, BrownPositiveBoss::deserialize);
		mBossDeserializers.put(BrownMagnetSwapBoss.identityTag, BrownMagnetSwapBoss::deserialize);
		mBossDeserializers.put(ParadoxSwapBoss.identityTag, ParadoxSwapBoss::deserialize);
		mBossDeserializers.put(TealSpirit.identityTag, TealSpirit::deserialize);
		mBossDeserializers.put(TemporalShieldBoss.identityTag, TemporalShieldBoss::deserialize);
		mBossDeserializers.put(GalleryMobRisingBoss.identityTag, GalleryMobRisingBoss::deserialize);
		mBossDeserializers.put(GallerySummonMobBoss.identityTag, GallerySummonMobBoss::deserialize);
		mBossDeserializers.put(MusicBoss.identityTag, MusicBoss::deserialize);
		mBossDeserializers.put(TagScalingBoss.identityTag, TagScalingBoss::deserialize);
		mBossDeserializers.put(CancelDamageBoss.identityTag, CancelDamageBoss::deserialize);

		/*
		 Boss Parameters
		 */
		mBossParameters = new HashMap<>();
		mBossParameters.put(BulletHellSurvivalBoss.identityTag, new BulletHellBoss.Parameters());
		mBossParameters.put(BlockLockBoss.identityTag, new BlockLockBoss.Parameters());
		mBossParameters.put(ChestLockBoss.identityTag, new ChestLockBoss.Parameters());
		mBossParameters.put(AntiRangeBoss.identityTag, new AntiRangeBoss.Parameters());
		mBossParameters.put(AntiMeleeBoss.identityTag, new AntiMeleeBoss.Parameters());
		mBossParameters.put(DamageCapBoss.identityTag, new DamageCapBoss.Parameters());
		mBossParameters.put(UnyieldingBoss.identityTag, new UnyieldingBoss.Parameters());
		mBossParameters.put(ToughBoss.identityTag, new ToughBoss.Parameters());
		mBossParameters.put(UnseenBoss.identityTag, new UnseenBoss.Parameters());
		mBossParameters.put(WhispersBoss.identityTag, new WhispersBoss.Parameters());
		mBossParameters.put(NovaBoss.identityTag, new NovaBoss.Parameters());
		mBossParameters.put(LaserBoss.identityTag, new LaserBoss.Parameters());
		mBossParameters.put(ProjectileBoss.identityTag, new ProjectileBoss.Parameters());
		mBossParameters.put(AuraEffectBoss.identityTag, new AuraEffectBoss.Parameters());
		mBossParameters.put(AvengerBoss.identityTag, new AvengerBoss.Parameters());
		mBossParameters.put(BarrierBoss.identityTag, new BarrierBoss.Parameters());
		mBossParameters.put(BulletHellBoss.identityTag, new BulletHellBoss.Parameters());
		mBossParameters.put(ChargerBoss.identityTag, new ChargerBoss.Parameters());
		mBossParameters.put(CommanderBoss.identityTag, new CommanderBoss.Parameters());
		mBossParameters.put(CrowdControlResistanceBoss.identityTag, new CrowdControlResistanceBoss.Parameters());
		mBossParameters.put(EarthshakeBoss.identityTag, new EarthshakeBoss.Parameters());
		mBossParameters.put(FireBombTossBoss.identityTag, new FireBombTossBoss.Parameters());
		mBossParameters.put(FlameTrailBoss.identityTag, new FlameTrailBoss.Parameters());
		mBossParameters.put(ForceBoss.identityTag, new ForceBoss.Parameters());
		mBossParameters.put(JumpBoss.identityTag, new JumpBoss.Parameters());
		mBossParameters.put(MeteorSlamBoss.identityTag, new MeteorSlamBoss.Parameters());
		mBossParameters.put(OnHitBoss.identityTag, new OnHitBoss.Parameters());
		mBossParameters.put(PounceBoss.identityTag, new PounceBoss.Parameters());
		mBossParameters.put(RejuvenationBoss.identityTag, new RejuvenationBoss.Parameters());
		mBossParameters.put(SpawnMobsBoss.identityTag, new SpawnMobsBoss.Parameters());
		mBossParameters.put(SwingBoss.identityTag, new SwingBoss.Parameters());
		mBossParameters.put(TpBehindBoss.identityTag, new TpBehindBoss.Parameters());
		mBossParameters.put(TpSwapBoss.identityTag, new TpSwapBoss.Parameters());
		mBossParameters.put(UnstableBoss.identityTag, new UnstableBoss.Parameters());
		mBossParameters.put(SeekingProjectileBoss.identityTag, new SeekingProjectileBoss.Parameters());
		mBossParameters.put(MobRisingBoss.identityTag, new MobRisingBoss.Parameters());
		mBossParameters.put(FestiveTessUpgradeSnowmenBoss.identityTag, new FestiveTessUpgradeSnowmenBoss.Parameters());
		mBossParameters.put(GrenadeLauncherBoss.identityTag, new GrenadeLauncherBoss.Parameters());
		mBossParameters.put(SizeChangerBoss.identityTag, new SizeChangerBoss.Parameters());
		mBossParameters.put(WrathBoss.identityTag, new WrathBoss.Parameters());
		mBossParameters.put(ThrowSummonBoss.identityTag, new ThrowSummonBoss.Parameters());
		mBossParameters.put(DelveScalingBoss.identityTag, new DelveScalingBoss.Parameters());
		mBossParameters.put(DeathSummonBoss.identityTag, new DeathSummonBoss.Parameters());
		mBossParameters.put(SummonOnExplosionBoss.identityTag, new SummonOnExplosionBoss.Parameters());
		mBossParameters.put(ScoutVolleyBoss.identityTag, new ScoutVolleyBoss.Parameters());
		mBossParameters.put(HostileBoss.identityTag, new HostileBoss.Parameters());
		mBossParameters.put(FriendlyBoss.identityTag, new FriendlyBoss.Parameters());
		mBossParameters.put(RebornBoss.identityTag, new RebornBoss.Parameters());
		mBossParameters.put(BlockBreakBoss.identityTag, new BlockBreakBoss.Parameters());
		mBossParameters.put(GenericBoss.identityTag, new GenericBoss.Parameters());
		mBossParameters.put(MageCosmicMoonbladeBoss.identityTag, new MageCosmicMoonbladeBoss.Parameters());
		mBossParameters.put(StarfallBoss.identityTag, new StarfallBoss.Parameters());
		mBossParameters.put(ShatterBoss.identityTag, new ShatterBoss.Parameters());
		mBossParameters.put(WarriorShieldWallBoss.identityTag, new WarriorShieldWallBoss.Parameters());
		mBossParameters.put(WarlockAmpHexBoss.identityTag, new WarlockAmpHexBoss.Parameters());
		mBossParameters.put(DodgeBoss.identityTag, new DodgeBoss.Parameters());
		mBossParameters.put(PotionThrowBoss.identityTag, new PotionThrowBoss.Parameters());
		mBossParameters.put(GenericTargetBoss.identityTag, new GenericTargetBoss.Parameters());
		mBossParameters.put(LimitedLifespanBoss.identityTag, new LimitedLifespanBoss.Parameters());
		mBossParameters.put(ImmortalMountBoss.identityTag, new ImmortalMountBoss.Parameters());
		mBossParameters.put(ImmortalPassengerBoss.identityTag, new ImmortalPassengerBoss.Parameters());
		mBossParameters.put(BrownMagnetSwapBoss.identityTag, new BrownMagnetSwapBoss.Parameters());
		mBossParameters.put(BrownPositiveBoss.identityTag, new BrownPositiveBoss.Parameters());
		mBossParameters.put(BrownNegativeBoss.identityTag, new BrownNegativeBoss.Parameters());
		mBossParameters.put(MusicBoss.identityTag, new MusicBoss.Parameters());
		mBossParameters.put(SoundBoss.identityTag, new SoundBoss.Parameters());
		mBossParameters.put(TagScalingBoss.identityTag, new TagScalingBoss.Parameters());
		mBossParameters.put(RedstoneBoss.identityTag, new RedstoneBoss.Parameters());
		mBossParameters.put(SlashAttackBoss.identityTag, new SlashAttackBoss.Parameters());
		mBossParameters.put(DashBoss.identityTag, new DashBoss.Parameters());
	}

	/********************************************************************************
	 * Member Variables
	 *******************************************************************************/
	private final Plugin mPlugin;
	private final Map<UUID, Boss> mBosses;
	private boolean mNearbyEntityDeathEnabled = false;
	private boolean mNearbyBlockBreakEnabled = false;
	private boolean mNearbyPlayerDeathEnabled = false;

	public BossManager(Plugin plugin) {
		INSTANCE = this;
		mPlugin = plugin;
		mBosses = new HashMap<>();

		/* When starting up, look for bosses in all current world entities */
		for (World world : Bukkit.getWorlds()) {
			for (Entity entity : world.getEntities()) {
				if (!(entity instanceof LivingEntity)) {
					continue;
				}

				processEntity((LivingEntity) entity);
			}
		}

	}

	/********************************************************************************
	 * Event Handlers
	 *******************************************************************************/

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityAddToWorldEvent(EntityAddToWorldEvent event) {
		if (event.getEntity() instanceof LivingEntity living
			    && !living.getScoreboardTags().isEmpty()) {
			// EntityAddToWorldEvent is called at an inconvenient time in Minecraft's code, which can cause deadlocks,
			// so we delay initialisation of boss data slightly to be outside the entity loading code.
			Bukkit.getScheduler().runTask(mPlugin, () -> {
				if (living.isValid()) {
					processEntity(living);
				}
			});
		}
	}

	// Creature spawn event is also listened to in order to set up boss data for initial spawn at the moment the mob is summoned,
	// which allows to immediately use the boss after summoning it.
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void creatureSpawnEvent(CreatureSpawnEvent event) {
		processEntity(event.getEntity());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityRemoveFromWorldEvent(EntityRemoveFromWorldEvent event) {
		if (event.getEntity() instanceof LivingEntity living) {
			unload(living, false);
		}
	}

	// Some entities seem to persist despite their chunk being unloaded,
	// so we manually load/unload their boss data when the chunk is loaded/unloaded.

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void chunkLoadEvent(ChunkLoadEvent event) {
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> { // delay by a tick as the chunk is loaded before entities in 1.18+
			if (event.getChunk().isLoaded()) { // the chunk may have unloaded already
				for (Entity entity : event.getChunk().getEntities()) {
					if (entity instanceof LivingEntity living) {
						processEntity(living);
					}
				}
			}
		}, 1);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void chunkUnloadEvent(ChunkUnloadEvent event) {
		for (Entity entity : event.getChunk().getEntities()) {
			if (entity instanceof LivingEntity living) {
				unload(living, false);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityDeathEvent(EntityDeathEvent event) {
		LivingEntity entity = event.getEntity();

		if (mNearbyEntityDeathEnabled) {
			/* For performance reasons this check is only enabled when there is a loaded
			 * boss that is using this feature
			 */
			for (LivingEntity m : EntityUtils.getNearbyMobs(entity.getLocation(), 12.0)) {
				Boss boss = mBosses.get(m.getUniqueId());
				if (boss != null) {
					boss.nearbyEntityDeath(event);
				}
			}
		}

		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			boss.death(event);
			if (entity.getHealth() <= 0) {
				unload(boss, false);
				mBosses.remove(entity.getUniqueId());

				/*
				 * Remove special serialization data from drops. Should not be
				 * necessary since loaded bosses already have this data stripped
				 */
				SerializationUtils.stripSerializationDataFromDrops(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		if (mNearbyBlockBreakEnabled) {
			/* For performance reasons this check is only enabled when there is a loaded
			 * boss that is using this feature
			 */
			for (LivingEntity m : EntityUtils.getNearbyMobs(event.getBlock().getLocation(), 62.0)) {
				Boss boss = mBosses.get(m.getUniqueId());
				if (boss != null) {
					boss.nearbyBlockBreak(event);
				}
			}
		}
	}

	// EntityExplodeEvent is used by various spells like SpellBlockBreak to conditionally destroy blocks.
	// We don't want those to trigger death events though. So we listen to the ExplosionPrimeEvent event
	// which precedes every real creeper explosion and only handle explosions after a prime event.
	// Handling the death in the prime event itself doesn't quite work, as the creeper is still alive and
	// will explode after the event, damaging any mobs the death handler spawned for example.
	// Delaying by a tick doesn't work either, as the creeper will be discarded just after the explosion.
	private @Nullable Creeper mLastPrimedCreeper = null;

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void explosionPrimeEvent(ExplosionPrimeEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof Creeper creeper) {
			mLastPrimedCreeper = creeper;
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		Entity entity = event.getEntity();
		if (entity == mLastPrimedCreeper) {
			Boss boss = mBosses.remove(entity.getUniqueId());
			if (boss != null) {
				boss.death(null);
				unload(boss, false);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void projectileLaunchEvent(ProjectileLaunchEvent event) {
		Projectile proj = event.getEntity();
		ProjectileSource shooter = proj.getShooter();
		if (shooter instanceof LivingEntity) {
			Boss boss = mBosses.get(((LivingEntity) shooter).getUniqueId());
			if (boss != null) {
				// May cancel the event
				boss.bossLaunchedProjectile(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void projectileHitEvent(ProjectileHitEvent event) {
		Projectile proj = event.getEntity();
		ProjectileSource shooter = proj.getShooter();
		if (shooter instanceof LivingEntity livingShooter) {
			Boss boss = mBosses.get(livingShooter.getUniqueId());
			if (boss != null) {
				boss.bossProjectileHit(event);
			}
		}
		if (event.getHitEntity() != null && event.getHitEntity() instanceof LivingEntity hit) {
			Boss boss = mBosses.get(hit.getUniqueId());
			if (boss != null) {
				boss.bossHitByProjectile(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void areaEffectCloudApplyEvent(AreaEffectCloudApplyEvent event) {
		// Make a copy, so it can be iterated while bosses modify the actual list
		for (LivingEntity entity : new ArrayList<>(event.getAffectedEntities())) {
			Boss boss = mBosses.get(entity.getUniqueId());
			if (boss != null) {
				boss.areaEffectAppliedToBoss(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void potionSplashEvent(PotionSplashEvent event) {
		// Make a copy, so it can be iterated while bosses modify the actual list
		for (LivingEntity entity : new ArrayList<>(event.getAffectedEntities())) {
			Boss boss = mBosses.get(entity.getUniqueId());
			if (boss != null) {
				boss.splashPotionAppliedToBoss(event);
			}
		}

		if (event.getEntity().getShooter() instanceof LivingEntity le) {
			Boss boss = mBosses.get(le.getUniqueId());
			if (boss != null) {
				boss.bossSplashPotion(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void entityPotionEffectEvent(EntityPotionEffectEvent event) {
		Boss boss = mBosses.get(event.getEntity().getUniqueId());
		if (boss != null) {
			boss.entityPotionEffectEvent(event);
		}
	}

	@SuppressWarnings("unchecked")
	public @Nullable <T extends BossAbilityGroup> T getBoss(Entity entity, Class<T> cls) {
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			for (BossAbilityGroup ability : boss.getAbilities()) {
				if (cls.isInstance(ability)) {
					return (T) ability;
				}
			}
		}

		return null;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onHurt(DamageEvent event) {
		LivingEntity damagee = event.getDamagee();
		Entity damager = event.getDamager();
		LivingEntity source = event.getSource();

		Boss boss = mBosses.get(damagee.getUniqueId());
		if (boss != null) {
			// May cancel the event
			boss.onHurt(event);
			if (damager != null) {
				boss.onHurtByEntity(event, damager);
				if (source != null) {
					boss.onHurtByEntityWithSource(event, damager, source);
				}
			}
			boss.setLastHitBy(source);
		}

		Entity vehicle = damagee.getVehicle();
		if (vehicle instanceof LivingEntity mount) {
			Boss mountBoss = mBosses.get(mount.getUniqueId());
			if (mountBoss != null) {
				mountBoss.onPassengerHurt(event);
			}
		}
	}

	// Must be before player items and abilities, which use priority HIGH
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onDamage(DamageEvent event) {
		LivingEntity damagee = event.getDamagee();
		LivingEntity source = event.getSource();
		if (source != null) {
			Boss boss = mBosses.get(source.getUniqueId());
			if (boss != null) {
				// May cancel the event
				boss.onDamage(event, damagee);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void spellCastEvent(SpellCastEvent event) {
		LivingEntity boss = event.getBoss();
		Boss b = mBosses.get(boss.getUniqueId());
		if (b != null && !EntityUtils.isSilenced(boss)) {
			b.bossCastAbility(event);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void entityPathfindEvent(EntityPathfindEvent event) {
		if (event.getEntity() instanceof Mob entity) {

			Boss boss = mBosses.get(entity.getUniqueId());
			if (boss != null) {
				boss.bossPathfind(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void entityTargetEvent(EntityTargetEvent event) {
		if (event.getEntity() instanceof Mob entity) {

			Boss boss = mBosses.get(entity.getUniqueId());
			if (boss != null) {
				boss.bossChangedTarget(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void slimeSplitEvent(SlimeSplitEvent event) {
		if (event.getEntity().getScoreboardTags().contains("boss_nosplit")) {
			/*
			 * This annoying special-case boss won't work with the normal system because
			 * the Boss that needs to be matched unloads before this event gets called
			 */
			event.setCancelled(true);
		}
	}

	/* Not actually an event handler - must be called by EntityUtils applyStun() */
	/* TODO: Probably make this an actual event? */
	public void entityStunned(Entity entity) {
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			boss.bossStunned();
		}
	}

	/* Not actually an event handler - must be called by EntityUtils applySilence() */
	/* TODO: Probably make this an actual event? */
	public void entitySilenced(Entity entity) {
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			boss.bossSilenced();
		}
	}

	public void entityKnockedAway(Entity entity, float speed) {
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			boss.bossKnockedAway(speed);
		}
	}

	/* Kind of a weird one - not hooked to bosses but used for snowman killer */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (player.hasMetadata(WinterSnowmanEventBoss.deathMetakey)) {
			Entity snowman = Bukkit.getEntity(UUID.fromString(player.getMetadata(WinterSnowmanEventBoss.deathMetakey).get(0).asString()));
			Component snowmanName = snowman != null ? snowman.customName() : null;
			Component deathMessage = Component.text("")
				.append(Component.selector(player.getName()))
				.append(Component.text(" was snowballed by "))
				.append(snowmanName != null ? snowmanName : Component.text("a snowman"));
			event.deathMessage(deathMessage);
			player.removeMetadata(WinterSnowmanEventBoss.deathMetakey, mPlugin);
		}

		if (mNearbyPlayerDeathEnabled) {
			/* For performance reasons this check is only enabled when there is a loaded
			 * boss that is using this feature
			 */
			for (LivingEntity m : EntityUtils.getNearbyMobs(player.getLocation(), 75.0)) {
				Boss boss = mBosses.get(m.getUniqueId());
				if (boss != null) {
					boss.nearbyPlayerDeath(event);
				}
			}
		}
	}

	/* Another weird one - used for exorcism potion */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void lingeringPotionSplashEvent(LingeringPotionSplashEvent event) {
		if (event.getEntityType() == EntityType.SPLASH_POTION) {
			ThrownPotion potEntity = event.getEntity();
			ItemStack potItem = potEntity.getItem();
			if (potItem.getType() == Material.LINGERING_POTION) {
				if (InventoryUtils.testForItemWithLore(potItem, "Exorcism")) {
					AreaEffectCloud cloud = event.getAreaEffectCloud();
					cloud.setMetadata("MonumentaBossesGrayExorcism", new FixedMetadataValue(mPlugin, 1));
					cloud.setRadius(6.5f);
					cloud.setDurationOnUse(0);
					cloud.setRadiusOnUse(0);
					cloud.setRadiusPerTick(-0.004f);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void playerLeashEntityEvent(PlayerLeashEntityEvent event) {
		if (mBosses.get(event.getEntity().getUniqueId()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void customEffectAppliedToBoss(CustomEffectApplyEvent event) {
		Boss boss = mBosses.get(event.getEntity().getUniqueId());

		if (boss != null) {
			boss.customEffectAppliedToBoss(event);
		}
	}

	@EventHandler(ignoreCancelled = false)
	public void bossExploded(EntityExplodeEvent event) {
		Boss boss = mBosses.get(event.getEntity().getUniqueId());

		if (boss != null) {
			boss.bossExploded(event);
		}
	}

	// Only acts on fire applied by the plugin, called in EntityUtils
	public void bossIgnited(Entity entity, int ticks) {
		Boss boss = mBosses.get(entity.getUniqueId());

		if (boss != null) {
			boss.bossIgnited(ticks);
		}
	}

	/********************************************************************************
	 * Static public methods
	 *******************************************************************************/
	public static BossManager getInstance() {
		if (INSTANCE == null) {
			throw new RuntimeException("BossManager used before it was loaded");
		}
		return INSTANCE;
	}

	public static void registerStatelessBoss(String identityTag, StatelessBossConstructor fn) {
		mStatelessBosses.put(identityTag, fn);
	}

	public static void registerStatefulBoss(String identityTag, StatefulBossConstructor fn) {
		mStatefulBosses.put(identityTag, fn);
	}

	public static void registerBossDeserializer(String identityTag, BossDeserializer fn) {
		mBossDeserializers.put(identityTag, fn);
	}

	/********************************************************************************
	 * Public Methods
	 *******************************************************************************/

	public List<BossAbilityGroup> getAbilities(Entity entity) {
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			return boss.getAbilities();
		}
		return Collections.emptyList();
	}

	/*
	 * Every way to unload a boss needs to bounce through this function to ensure
	 * state is updated correctly!
	 */
	public void unload(Boss boss, boolean shuttingDown) {
		if (!shuttingDown) {
			checkDisablePerformanceEvents(boss);
		}
		boss.unload(shuttingDown);
	}

	public void unload(LivingEntity entity, boolean shuttingDown) {
		Boss boss = mBosses.remove(entity.getUniqueId());
		if (boss != null) {
			unload(boss, shuttingDown);
		}
	}

	public void unloadAll(boolean shuttingDown) {
		for (Map.Entry<UUID, Boss> entry : mBosses.entrySet()) {
			unload(entry.getValue(), shuttingDown);
		}
		mBosses.clear();
	}

	public void removeAbility(LivingEntity entity, String identityTag) {
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			boss.removeAbility(identityTag);
		}
	}

	public static void createBoss(@Nullable CommandSender sender, LivingEntity targetEntity, String requestedTag) throws Exception {
		if (INSTANCE == null) {
			throw new Exception("BossManager is not loaded");
		}

		StatelessBossConstructor stateless = mStatelessBosses.get(requestedTag);
		if (stateless != null) {
			BossAbilityGroup bossAbilityGroup = stateless.construct(INSTANCE.mPlugin, targetEntity);
			if (bossAbilityGroup == null) {
				if (sender != null) {
					sender.sendMessage("Could not give '" + requestedTag + "' to mob!");
				}
				return;
			}
			INSTANCE.createBossInternal(targetEntity, bossAbilityGroup);
			if (sender != null) {
				sender.sendMessage("Successfully gave '" + requestedTag + "' to mob");
			}
		} else {
			if (mStatefulBosses.get(requestedTag) != null) {
				if (sender != null) {
					sender.sendMessage(ChatColor.GOLD + "There is a boss with the tag '" +
						ChatColor.GREEN + requestedTag + ChatColor.GOLD +
						"' but it requires positional arguments");
					sender.sendMessage(ChatColor.GOLD + "Try again with some ending location coordinates");
				}
			} else {
				if (sender != null) {
					sender.sendMessage(ChatColor.RED + "No boss found with the tag '" + requestedTag + "'");
				}
			}
		}
	}

	public static void createBoss(@Nullable CommandSender sender, LivingEntity targetEntity, String requestedTag, Location endLoc) throws Exception {
		if (INSTANCE == null) {
			throw new Exception("BossManager is not loaded");
		}

		StatefulBossConstructor stateful = mStatefulBosses.get(requestedTag);
		if (stateful != null) {
			BossAbilityGroup bossAbilityGroup = stateful.construct(INSTANCE.mPlugin, targetEntity, targetEntity.getLocation(), endLoc);
			if (bossAbilityGroup == null) {
				if (sender != null) {
					sender.sendMessage("Could not give '" + requestedTag + "' to mob!");
				}
				return;
			}
			INSTANCE.createBossInternal(targetEntity, bossAbilityGroup);
			if (sender != null) {
				sender.sendMessage("Successfully gave '" + requestedTag + "' to mob");
			}
		} else {
			if (mStatelessBosses.get(requestedTag) != null) {
				if (sender != null) {
					sender.sendMessage(ChatColor.GOLD + "There is a boss with the tag '" +
						ChatColor.GREEN + requestedTag + ChatColor.GOLD +
						"' but it does not take positional arguments");
					sender.sendMessage(ChatColor.GOLD + "Try again without the coordinates");
				}
			} else {
				if (sender != null) {
					sender.sendMessage(ChatColor.RED + "No boss found with the tag '" + requestedTag + "'");
				}
			}
		}
	}

	/* Machine-readable list */
	public String[] listBosses() {
		Set<String> allBossTags = new HashSet<>(mStatelessBosses.keySet());
		allBossTags.addAll(mStatefulBosses.keySet());
		return allBossTags.toArray(String[]::new);
	}

	/* Machine-readable list */
	public String[] listStatelessBosses() {
		return mStatelessBosses.keySet().toArray(String[]::new);
	}

	/********************************************************************************
	 * Private Methods
	 *******************************************************************************/

	/*
	 * This function should be called whenever an ability is loaded to enable events
	 * that are more performance sensitive (if applicable)
	 */
	private void checkEnablePerformanceEvents(BossAbilityGroup ability) {
		if (ability.hasNearbyEntityDeathTrigger()) {
			mNearbyEntityDeathEnabled = true;
		}

		if (ability.hasNearbyBlockBreakTrigger()) {
			mNearbyBlockBreakEnabled = true;
		}

		if (ability.hasNearbyPlayerDeathTrigger()) {
			mNearbyPlayerDeathEnabled = true;
		}
	}

	/*
	 * This function should be called whenever a boss is unloaded to disable events
	 * that are more performance sensitive (if applicable)
	 */
	private void checkDisablePerformanceEvents(Boss boss) {
		if (boss.hasNearbyEntityDeathTrigger()) {
			if (!mNearbyEntityDeathEnabled) {
				mPlugin.getLogger().log(Level.WARNING, "Unloaded Boss with hasNearbyEntityDeathTrigger but feature was not enabled. Definitely a bug!");
			}

			/*
			 * This boss was at least contributing to keeping this feature enabled
			 *
			 * Need to check all other loaded bosses to see if it still needs to be enabled
			 */
			if (mBosses.values().stream().noneMatch(Boss::hasNearbyEntityDeathTrigger)) {
				mNearbyEntityDeathEnabled = false;
			}
		}

		if (boss.hasNearbyBlockBreakTrigger()) {
			if (!mNearbyBlockBreakEnabled) {
				mPlugin.getLogger().log(Level.WARNING, "Unloaded Boss with hasNearbyBlockBreakTrigger but feature was not enabled. Definitely a bug!");
			}

			/*
			 * This boss was at least contributing to keeping this feature enabled
			 *
			 * Need to check all other loaded bosses to see if it still needs to be enabled
			 */
			if (mBosses.values().stream().noneMatch(Boss::hasNearbyBlockBreakTrigger)) {
				mNearbyBlockBreakEnabled = false;
			}
		}

		if (boss.hasNearbyPlayerDeathTrigger()) {
			if (!mNearbyBlockBreakEnabled) {
				mPlugin.getLogger().log(Level.WARNING, "Unloaded Boss with hasNearbyPlayerDeathTrigger but feature was not enabled. Definitely a bug!");
			}

			/*
			 * This boss was at least contributing to keeping this feature enabled
			 *
			 * Need to check all other loaded bosses to see if it still needs to be enabled
			 */
			if (mBosses.values().stream().noneMatch(Boss::hasNearbyPlayerDeathTrigger)) {
				mNearbyPlayerDeathEnabled = false;
			}
		}
	}

	public void createBossInternal(LivingEntity targetEntity, BossAbilityGroup ability) {
		/* Set up boss health / armor / etc */
		ability.init();

		checkEnablePerformanceEvents(ability);

		Boss boss = mBosses.get(targetEntity.getUniqueId());
		if (boss == null) {
			boss = new Boss(mPlugin, ability);
		} else {
			boss.add(ability);
		}

		mBosses.put(targetEntity.getUniqueId(), boss);
	}

	private void processEntity(LivingEntity entity) {
		if (mBosses.containsKey(entity.getUniqueId())) {
			// already loaded
			return;
		}

		Set<String> tags = entity.getScoreboardTags();
		if (!tags.isEmpty()) {
			Boss boss = null;
			/*
			 * Note - it is important to make a copy here to avoid concurrent modification exception
			 * which happens while iterating and the boss constructor changes the tags on the mob
			 */
			for (String tag : new ArrayList<>(tags)) {
				BossDeserializer deserializer = mBossDeserializers.get(tag);
				if (deserializer != null) {
					BossAbilityGroup ability;
					try {
						ability = deserializer.deserialize(mPlugin, entity);
						if (ability == null) {
							continue;
						}
					} catch (Exception ex) {
						mPlugin.getLogger().log(Level.SEVERE, "Failed to load boss!", ex);
						continue;
					}

					checkEnablePerformanceEvents(ability);

					if (boss == null) {
						boss = new Boss(mPlugin, ability);
						mBosses.put(entity.getUniqueId(), boss);
					} else {
						boss.add(ability);
					}
				}
			}
		}
	}

	public void sendBossDebugInfo(CommandSender sender) {
		sender.sendMessage("");
		sender.sendMessage("Total number of loaded bosses: " + mBosses.size());
		sender.sendMessage("mNearbyEntityDeathEnabled: " + mNearbyEntityDeathEnabled);
		sender.sendMessage("mNearbyBlockBreakEnabled: " + mNearbyBlockBreakEnabled);
		sender.sendMessage("mNearbyPlayerDeathEnabled: " + mNearbyPlayerDeathEnabled);

		Map<String, Integer> bossCounts = new HashMap<>();
		for (Boss boss : mBosses.values()) {
			String tags = String.join(" ", boss.getIdentityTags());
			Integer count = bossCounts.get(tags);
			if (count == null) {
				count = 1;
			} else {
				count += 1;
			}
			bossCounts.put(tags, count);
		}

		List<Map.Entry<String, Integer>> list = new ArrayList<>(bossCounts.entrySet());
		list.sort(Map.Entry.comparingByValue());

		for (Map.Entry<String, Integer> entry : list) {
			sender.sendMessage("  " + entry.getKey() + ": " + entry.getValue());
		}
	}

	public void manuallyRegisterBoss(LivingEntity entity, BossAbilityGroup ability) {
		checkEnablePerformanceEvents(ability);

		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss == null) {
			boss = new Boss(mPlugin, ability);
			mBosses.put(entity.getUniqueId(), boss);
		} else {
			boss.add(ability);
		}
	}

}
