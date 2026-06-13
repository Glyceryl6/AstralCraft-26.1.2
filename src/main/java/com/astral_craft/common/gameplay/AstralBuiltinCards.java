package com.astral_craft.common.gameplay;

import com.astral_craft.common.items.cards.*;

/** Forces built-in card classes to load before the custom card-definition registry fires. */
public class AstralBuiltinCards {

    public static void bootstrap() {
        touch(HandcardAllOrNothing.DEFINITION);
        touch(HandcardAshenFeather.DEFINITION);
        touch(HandcardAttackG.DEFINITION);
        touch(HandcardAttackL.DEFINITION);
        touch(HandcardAttackM.DEFINITION);
        touch(HandcardBarricade.DEFINITION);
        touch(HandcardBarrier.DEFINITION);
        touch(HandcardBerserk.DEFINITION);
        touch(HandcardBite.DEFINITION);
        touch(HandcardBlast.DEFINITION);
        touch(HandcardBothHave.DEFINITION);
        touch(HandcardBrick.DEFINITION);
        touch(HandcardCharge.DEFINITION);
        touch(HandcardCheerUp.DEFINITION);
        touch(HandcardChocolateCake.DEFINITION);
        touch(HandcardColourfulFeather.DEFINITION);
        touch(HandcardDefenseG.DEFINITION);
        touch(HandcardDefenseL.DEFINITION);
        touch(HandcardDefenseM.DEFINITION);
        touch(HandcardDemolition.DEFINITION);
        touch(HandcardDirectedBoost.DEFINITION);
        touch(HandcardDragonRoar.DEFINITION);
        touch(HandcardEnergyBar.DEFINITION);
        touch(HandcardEnhancedBarricade.DEFINITION);
        touch(HandcardEntrapment.DEFINITION);
        touch(HandcardExpiredBento.DEFINITION);
        touch(HandcardEyeForAnEye.DEFINITION);
        touch(HandcardFateGuidance.DEFINITION);
        touch(HandcardFightFireWithFire.DEFINITION);
        touch(HandcardFirecrackers.DEFINITION);
        touch(HandcardFortune.DEFINITION);
        touch(HandcardGawuCut.DEFINITION);
        touch(HandcardHamburger.DEFINITION);
        touch(HandcardHurry.DEFINITION);
        touch(HandcardImmovable.DEFINITION);
        touch(HandcardKingPower.DEFINITION);
        touch(HandcardLaser.DEFINITION);
        touch(HandcardLivingBook.DEFINITION);
        touch(HandcardLuxuriousFeast.DEFINITION);
        touch(HandcardMisfortune.DEFINITION);
        touch(HandcardOverflowingFortune.DEFINITION);
        touch(HandcardPoisonFang.DEFINITION);
        touch(HandcardPowerfulAttack.DEFINITION);
        touch(HandcardProblemStudent.DEFINITION);
        touch(HandcardQuirkyEnchanted.DEFINITION);
        touch(HandcardRailgun.DEFINITION);
        touch(HandcardRandomPortal.DEFINITION);
        touch(HandcardRandomSelect.DEFINITION);
        touch(HandcardRedirection.DEFINITION);
        touch(HandcardReleaseAzureSoul.DEFINITION);
        touch(HandcardReleaseScarletSoul.DEFINITION);
        touch(HandcardScavenging.DEFINITION);
        touch(HandcardSelfExplosion.DEFINITION);
        touch(HandcardShadowAttack.DEFINITION);
        touch(HandcardSlingshot.DEFINITION);
        touch(HandcardSmartDice.DEFINITION);
        touch(HandcardSmartieGummy.DEFINITION);
        touch(HandcardSnatch.DEFINITION);
        touch(HandcardSnowballAttack.DEFINITION);
        touch(HandcardSoulLink.DEFINITION);
        touch(HandcardSupport.DEFINITION);
        touch(HandcardSupportGum.DEFINITION);
        touch(HandcardTimeBomb.DEFINITION);
    }

    private static void touch(CardDefinition definition) {
        // Deliberately empty. Accessing the definition triggers each card class static registration.
    }
}
