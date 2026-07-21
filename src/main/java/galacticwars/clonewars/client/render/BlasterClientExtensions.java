package galacticwars.clonewars.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** Keeps blasters shouldered in third person and stable at eye level in first person. */
public final class BlasterClientExtensions implements IClientItemExtensions {
    public static final BlasterClientExtensions INSTANCE = new BlasterClientExtensions();

    private BlasterClientExtensions() {
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(
            LivingEntity entity,
            InteractionHand hand,
            ItemStack stack
    ) {
        return HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }

    @Override
    public boolean applyForgeHandTransform(
            PoseStack poseStack,
            LocalPlayer player,
            HumanoidArm arm,
            ItemStack stack,
            float partialTick,
            float equipProgress,
            float swingProgress
    ) {
        float direction = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float ready = 1.0F - Mth.clamp(equipProgress, 0.0F, 1.0F);
        float recoil = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        float breathing = Mth.sin((player.tickCount + partialTick) * 0.11F) * 0.004F * ready;

        poseStack.translate(direction * 0.012F * ready, breathing, -0.035F * ready + 0.055F * recoil);
        poseStack.mulPose(Axis.XP.rotationDegrees(-2.0F * ready + 7.0F * recoil));
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * -1.5F * ready));
        return false;
    }
}
