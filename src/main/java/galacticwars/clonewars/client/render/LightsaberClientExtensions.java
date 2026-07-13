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

public final class LightsaberClientExtensions implements IClientItemExtensions {
    public static final LightsaberClientExtensions INSTANCE = new LightsaberClientExtensions();

    private LightsaberClientExtensions() {
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(
            LivingEntity entity,
            InteractionHand hand,
            ItemStack stack
    ) {
        return HumanoidModel.ArmPose.ITEM;
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
        float slash = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        float idle = Mth.sin((player.tickCount + partialTick) * 0.14F) * 0.012F * ready;

        poseStack.translate(direction * 0.025F * ready, idle, -0.045F * ready);
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * (-10.0F * ready - 18.0F * slash)));
        poseStack.mulPose(Axis.XP.rotationDegrees(-5.0F * ready - 12.0F * slash));
        // Keep the subtle ready/slash offsets, then let Minecraft apply its
        // normal first-person hand and item transforms. Returning true here
        // claims the whole transform was handled and leaves the saber outside
        // the visible hand render.
        return false;
    }
}
