package galacticwars.clonewars.recruitment;

import galacticwars.clonewars.economy.CreditTransactionService;
import net.minecraft.server.level.ServerPlayer;

public final class RecruitmentPaymentService {
    private RecruitmentPaymentService() {
    }

    public static int creditCount(ServerPlayer player) {
        return CreditTransactionService.playerBalance(player);
    }

    public static boolean withdrawCredits(ServerPlayer player, int amount) {
        return CreditTransactionService.withdrawPlayer(player, amount);
    }

    public static void refundCredits(ServerPlayer player, int amount) {
        CreditTransactionService.refundPlayer(player, amount);
    }
}
