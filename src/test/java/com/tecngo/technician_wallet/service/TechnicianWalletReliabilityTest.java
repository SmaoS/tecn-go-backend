package com.tecngo.technician_wallet.service;

import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.technician_wallet.entity.*;
import com.tecngo.technician_wallet.repository.*;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import com.tecngo.wompi.dto.WompiTransactionSnapshot;
import com.tecngo.wompi.service.WompiPaymentService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TechnicianWalletReliabilityTest {
    private final TechnicianWalletRepository wallets = mock(TechnicianWalletRepository.class);
    private final TechnicianWalletTransactionRepository transactions =
            mock(TechnicianWalletTransactionRepository.class);
    private final TechnicianRechargeRepository recharges = mock(TechnicianRechargeRepository.class);
    private final TechnicianWalletService service = new TechnicianWalletService(
            wallets, transactions, recharges, mock(SystemParameterService.class),
            mock(WompiPaymentService.class), mock(UserRepository.class));

    @Test
    void repeatedApprovedSnapshotCreditsWalletOnlyOnce() {
        UUID rechargeId = UUID.randomUUID();
        User technician = User.builder().id(UUID.randomUUID()).role(Role.TECHNICIAN).build();
        TechnicianWallet wallet = TechnicianWallet.builder()
                .id(UUID.randomUUID()).technician(technician)
                .balance(BigDecimal.ZERO).currency("COP").build();
        TechnicianRecharge recharge = TechnicianRecharge.builder()
                .id(rechargeId).technician(technician).amount(new BigDecimal("50000.00"))
                .currency("COP").reference("TECNGO-TECH-test").status(RechargeStatus.PENDING)
                .paymentUrl("https://checkout.wompi.co").build();
        when(recharges.findByReferenceForUpdate(recharge.getReference()))
                .thenReturn(Optional.of(recharge));
        when(recharges.findByWompiTransactionId("wompi-1")).thenReturn(Optional.empty());
        when(wallets.findByTechnicianIdForUpdate(technician.getId())).thenReturn(Optional.of(wallet));
        when(transactions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WompiTransactionSnapshot snapshot = new WompiTransactionSnapshot(
                "wompi-1", recharge.getReference(), "APPROVED",
                new BigDecimal("50000.00"), "COP");
        service.applyWompiTransaction(snapshot);
        service.applyWompiTransaction(snapshot);

        assertThat(wallet.getBalance()).isEqualByComparingTo("50000.00");
        assertThat(recharge.getStatus()).isEqualTo(RechargeStatus.APPROVED);
        verify(transactions, times(1)).save(any());
    }

    @Test
    void mismatchedAmountNeverCreditsWallet() {
        User technician = User.builder().id(UUID.randomUUID()).role(Role.TECHNICIAN).build();
        TechnicianRecharge recharge = TechnicianRecharge.builder()
                .id(UUID.randomUUID()).technician(technician).amount(new BigDecimal("50000.00"))
                .currency("COP").reference("TECNGO-TECH-test").status(RechargeStatus.PENDING)
                .paymentUrl("https://checkout.wompi.co").build();
        when(recharges.findByReferenceForUpdate(recharge.getReference()))
                .thenReturn(Optional.of(recharge));

        assertThatThrownBy(() -> service.applyWompiTransaction(new WompiTransactionSnapshot(
                "wompi-2", recharge.getReference(), "APPROVED",
                new BigDecimal("1000.00"), "COP")))
                .hasMessageContaining("amount");

        verifyNoInteractions(wallets, transactions);
        assertThat(recharge.getStatus()).isEqualTo(RechargeStatus.PENDING);
    }
}
