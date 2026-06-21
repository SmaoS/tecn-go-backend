package com.tecngo.technician_wallet.service;

import com.tecngo.payments.entity.Payment;
import com.tecngo.shared.exception.CodedForbiddenException;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.technician_wallet.dto.*;
import com.tecngo.technician_wallet.entity.*;
import com.tecngo.technician_wallet.repository.*;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import com.tecngo.wompi.service.WompiPaymentService;
import com.tecngo.wompi.dto.WompiTransactionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
public class TechnicianWalletService {
    private final TechnicianWalletRepository wallets;
    private final TechnicianWalletTransactionRepository transactions;
    private final TechnicianRechargeRepository recharges;
    private final SystemParameterService parameters;
    private final WompiPaymentService wompi;
    private final UserRepository users;

    @Transactional
    public TechnicianWallet ensureWallet(User technician) {
        if (!technician.hasRole(Role.TECHNICIAN)) {
            throw new ForbiddenException("Technician role is required");
        }
        return wallets.findByTechnicianId(technician.getId())
                .orElseGet(() -> wallets.save(TechnicianWallet.builder()
                        .technician(technician)
                        .balance(BigDecimal.ZERO)
                        .currency("COP")
                        .build()));
    }

    @Transactional(readOnly = true)
    public TechnicianWalletResponse mine(User technician) {
        requireTechnician(technician);
        TechnicianWallet wallet = wallets.findByTechnicianId(technician.getId())
                .orElseGet(() -> TechnicianWallet.builder()
                        .technician(technician).balance(BigDecimal.ZERO).currency("COP").build());
        return map(wallet);
    }

    @Transactional(readOnly = true)
    public List<TechnicianWalletTransactionResponse> myTransactions(User technician) {
        requireTechnician(technician);
        return transactions.findByTechnicianIdOrderByCreatedAtDesc(technician.getId()).stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public RechargeResponse createRecharge(User technician, BigDecimal amount) {
        return createRecharge(technician, amount, false);
    }

    @Transactional
    public RechargeResponse createRecharge(User technician, BigDecimal amount, boolean mobile) {
        requireTechnician(technician);
        if (!parameters.technicianRechargeEnabled()) {
            throw new ConflictException("Technician recharges are disabled");
        }
        validateRechargeAmount(amount);
        TechnicianWallet wallet = wallets.findByTechnicianIdForUpdate(technician.getId())
                .orElseGet(() -> ensureWallet(technician));
        String reference = "TECNGO-TECH-" + technician.getId() + "-" + System.currentTimeMillis();
        transaction(wallet, WalletTransactionType.RECHARGE_PENDING, BigDecimal.ZERO,
                reference, "Recarga iniciada por Wompi");
        String paymentUrl = wompi.checkoutUrl(reference, amount, "COP", mobile);
        TechnicianRecharge recharge = recharges.save(TechnicianRecharge.builder()
                .technician(technician)
                .amount(amount)
                .currency("COP")
                .reference(reference)
                .status(RechargeStatus.PENDING)
                .paymentUrl(paymentUrl)
                .build());
        return new RechargeResponse(recharge.getId(), paymentUrl, reference, amount, "COP");
    }

    @Transactional
    public void approveRecharge(String reference, String wompiTransactionId) {
        TechnicianRecharge recharge = recharges.findByReferenceForUpdate(reference)
                .orElseThrow(() -> new NotFoundException("Recharge not found"));
        approveRecharge(recharge, wompiTransactionId);
    }

    private void approveRecharge(TechnicianRecharge recharge, String wompiTransactionId) {
        if (recharge.getStatus() == RechargeStatus.APPROVED) return;
        if (recharge.getStatus() != RechargeStatus.PENDING) {
            throw new ConflictException("Recharge is not pending");
        }
        if (wompiTransactionId != null && !wompiTransactionId.isBlank()
                && recharges.findByWompiTransactionId(wompiTransactionId)
                .filter(existing -> !existing.getId().equals(recharge.getId()))
                .isPresent()) {
            return;
        }
        TechnicianWallet wallet = wallets.findByTechnicianIdForUpdate(recharge.getTechnician().getId())
                .orElseGet(() -> ensureWallet(recharge.getTechnician()));
        transaction(wallet, WalletTransactionType.RECHARGE_APPROVED, recharge.getAmount(),
                recharge.getReference(), "Recarga aprobada por Wompi");
        recharge.setStatus(RechargeStatus.APPROVED);
        recharge.setWompiTransactionId(clean(wompiTransactionId));
        recharge.setApprovedAt(Instant.now());
    }

    @Transactional
    public void rejectRecharge(String reference, String wompiTransactionId) {
        TechnicianRecharge recharge = recharges.findByReferenceForUpdate(reference)
                .orElseThrow(() -> new NotFoundException("Recharge not found"));
        rejectRecharge(recharge, wompiTransactionId);
    }

    private void rejectRecharge(TechnicianRecharge recharge, String wompiTransactionId) {
        if (recharge.getStatus() != RechargeStatus.PENDING) return;
        TechnicianWallet wallet = wallets.findByTechnicianIdForUpdate(recharge.getTechnician().getId())
                .orElseGet(() -> ensureWallet(recharge.getTechnician()));
        transaction(wallet, WalletTransactionType.RECHARGE_REJECTED, BigDecimal.ZERO,
                recharge.getReference(), "Recarga rechazada por Wompi");
        recharge.setStatus(RechargeStatus.REJECTED);
        recharge.setWompiTransactionId(clean(wompiTransactionId));
        recharge.setRejectedAt(Instant.now());
    }

    public void attachAndReconcile(UUID rechargeId, String transactionId, User technician) {
        requireTechnician(technician);
        TechnicianRecharge recharge = recharges.findByIdAndTechnicianId(rechargeId, technician.getId())
                .orElseThrow(() -> new NotFoundException("Recharge not found"));
        if (recharge.getStatus() != RechargeStatus.PENDING) return;
        WompiTransactionSnapshot snapshot = wompi.transaction(transactionId.trim());
        if (!recharge.getReference().equals(snapshot.reference())) {
            throw new ConflictException("Wompi transaction does not belong to this recharge");
        }
        applyWompiTransaction(snapshot);
    }

    public void attachAndReconcile(String transactionId, User technician) {
        requireTechnician(technician);
        WompiTransactionSnapshot snapshot = wompi.transaction(transactionId.trim());
        TechnicianRecharge recharge = recharges.findByReference(snapshot.reference())
                .filter(item -> item.getTechnician().getId().equals(technician.getId()))
                .orElseThrow(() -> new NotFoundException("Recharge not found"));
        if (recharge.getStatus() != RechargeStatus.PENDING) return;
        applyWompiTransaction(snapshot);
    }

    @Transactional
    public void applyWompiTransaction(WompiTransactionSnapshot snapshot) {
        if (snapshot.reference() == null || !snapshot.reference().startsWith("TECNGO-TECH-")) return;
        TechnicianRecharge recharge = recharges.findByReferenceForUpdate(snapshot.reference())
                .orElseThrow(() -> new NotFoundException("Recharge not found"));
        validateSnapshot(recharge, snapshot);
        if (recharge.getWompiTransactionId() != null
                && !recharge.getWompiTransactionId().equals(snapshot.transactionId())) {
            throw new ConflictException("Recharge is linked to another Wompi transaction");
        }
        recharge.setWompiTransactionId(clean(snapshot.transactionId()));
        recharge.setLastReconciledAt(Instant.now());
        recharge.setReconciliationAttempts(recharge.getReconciliationAttempts() + 1);
        recharge.setLastReconciliationError(null);
        switch (snapshot.status().toUpperCase()) {
            case "APPROVED" -> approveRecharge(recharge, snapshot.transactionId());
            case "DECLINED", "VOIDED", "ERROR" -> rejectRecharge(recharge, snapshot.transactionId());
            default -> recharge.setNextReconciliationAt(Instant.now().plusSeconds(300));
        }
    }

    @Transactional
    public void recordReconciliationFailure(String reference, Exception exception, int delaySeconds) {
        recharges.findByReferenceForUpdate(reference).ifPresent(recharge -> {
            recharge.setReconciliationAttempts(recharge.getReconciliationAttempts() + 1);
            recharge.setLastReconciledAt(Instant.now());
            recharge.setNextReconciliationAt(Instant.now().plusSeconds(delaySeconds));
            recharge.setLastReconciliationError(cleanError(exception));
        });
    }

    @Transactional(readOnly = true)
    public List<TechnicianRecharge> reconciliationCandidates(int limit) {
        return recharges.findReconciliationCandidates(Instant.now(),
                PageRequest.of(0, Math.max(1, Math.min(limit, 100))));
    }

    @Transactional(readOnly = true)
    public void requireCanQuote(User technician) {
        requireTechnician(technician);
        if (!parameters.technicianRechargeEnabled()) return;
        BigDecimal balance = wallets.findByTechnicianId(technician.getId())
                .map(TechnicianWallet::getBalance)
                .orElse(BigDecimal.ZERO);
        if (parameters.technicianBlockWhenNegativeBalance() && balance.signum() < 0) {
            throw new CodedForbiddenException("TECHNICIAN_BALANCE_REQUIRED",
                    "Tu saldo en TecnGo está pendiente. Recarga para poder cotizar nuevos servicios.");
        }
    }

    @Transactional
    public UUID debitCommissionIfEnabled(Payment payment) {
        if (!parameters.technicianRechargeEnabled()
                || payment.isCommissionWaived()
                || payment.getPlatformFee() == null
                || payment.getPlatformFee().signum() <= 0) {
            return null;
        }
        TechnicianWallet wallet = wallets.findByTechnicianIdForUpdate(payment.getTechnician().getId())
                .orElseGet(() -> ensureWallet(payment.getTechnician()));
        TechnicianWalletTransaction tx = transaction(wallet, WalletTransactionType.COMMISSION_DEBIT,
                payment.getPlatformFee().negate(), payment.getServiceRequest().getId().toString(),
                "Comisión de plataforma por servicio pagado");
        return tx.getId();
    }

    @Transactional(readOnly = true)
    public List<TechnicianWalletResponse> adminWallets(User admin) {
        requireAdmin(admin);
        return wallets.findAllByOrderByUpdatedAtDesc().stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public TechnicianWalletResponse adminWallet(UUID technicianId, User admin) {
        requireAdmin(admin);
        User technician = users.findById(technicianId)
                .orElseThrow(() -> new NotFoundException("Technician not found"));
        if (!technician.hasRole(Role.TECHNICIAN)) throw new ForbiddenException("Technician role is required");
        return map(wallets.findByTechnicianId(technicianId)
                .orElseGet(() -> TechnicianWallet.builder()
                        .technician(technician).balance(BigDecimal.ZERO).currency("COP").build()));
    }

    @Transactional
    public TechnicianWalletTransactionResponse adminAdjustment(UUID technicianId, BigDecimal amount,
                                                              String comment, User admin) {
        requireAdmin(admin);
        if (amount == null || amount.signum() == 0) throw new IllegalArgumentException("Amount must be different from zero");
        User technician = users.findById(technicianId)
                .orElseThrow(() -> new NotFoundException("Technician not found"));
        if (!technician.hasRole(Role.TECHNICIAN)) throw new ForbiddenException("Technician role is required");
        TechnicianWallet wallet = wallets.findByTechnicianIdForUpdate(technicianId)
                .orElseGet(() -> ensureWallet(technician));
        return map(transaction(wallet, WalletTransactionType.ADMIN_ADJUSTMENT, amount,
                "ADMIN-" + System.currentTimeMillis(), comment.trim()));
    }

    private TechnicianWalletTransaction transaction(TechnicianWallet wallet, WalletTransactionType type,
                                                    BigDecimal amount, String reference, String description) {
        BigDecimal before = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
        BigDecimal after = before.add(amount);
        wallet.setBalance(after);
        wallets.save(wallet);
        return transactions.save(TechnicianWalletTransaction.builder()
                .wallet(wallet)
                .technician(wallet.getTechnician())
                .type(type)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .reference(reference)
                .description(description)
                .build());
    }

    private TechnicianWalletResponse map(TechnicianWallet wallet) {
        BigDecimal balance = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
        boolean enabled = parameters.technicianRechargeEnabled();
        boolean low = enabled && parameters.technicianLowBalanceWarningEnabled()
                && balance.compareTo(parameters.technicianLowBalanceMinimum()) < 0
                && balance.signum() >= 0;
        boolean blocked = enabled && parameters.technicianBlockWhenNegativeBalance()
                && balance.signum() < 0;
        var items = transactions.findByTechnicianIdOrderByCreatedAtDesc(wallet.getTechnician().getId());
        BigDecimal approvedRecharges = items.stream()
                .filter(item -> item.getType() == WalletTransactionType.RECHARGE_APPROVED)
                .map(TechnicianWalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debits = items.stream()
                .filter(item -> item.getType() == WalletTransactionType.COMMISSION_DEBIT)
                .map(TechnicianWalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .abs();
        User technician = wallet.getTechnician();
        return new TechnicianWalletResponse(wallet.getId(), technician.getId(), technician.getFullName(),
                technician.getEmail(), technician.getPhone(), balance, wallet.getCurrency(), enabled,
                low, blocked, parameters.technicianLowBalanceMinimum(),
                parameters.technicianMinRechargeAmount(), parameters.technicianMaxRechargeAmount(),
                approvedRecharges, debits, technician.getCompletedServicesCount(), wallet.getUpdatedAt());
    }

    private TechnicianWalletTransactionResponse map(TechnicianWalletTransaction item) {
        return new TechnicianWalletTransactionResponse(item.getId(), item.getType(), item.getAmount(),
                item.getBalanceBefore(), item.getBalanceAfter(), item.getReference(),
                item.getDescription(), item.getCreatedAt());
    }

    private void validateRechargeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Recharge amount must be greater than zero");
        if (amount.compareTo(parameters.technicianMinRechargeAmount()) < 0) {
            throw new IllegalArgumentException("Recharge amount is below the minimum");
        }
        if (amount.compareTo(parameters.technicianMaxRechargeAmount()) > 0) {
            throw new IllegalArgumentException("Recharge amount exceeds the maximum");
        }
    }

    private void validateSnapshot(TechnicianRecharge recharge, WompiTransactionSnapshot snapshot) {
        if (snapshot.transactionId() == null || snapshot.transactionId().isBlank()) {
            throw new ConflictException("Wompi transaction id is missing");
        }
        if (snapshot.amount() == null || recharge.getAmount().compareTo(snapshot.amount()) != 0) {
            throw new ConflictException("Wompi transaction amount does not match the recharge");
        }
        if (snapshot.currency() == null || !recharge.getCurrency().equalsIgnoreCase(snapshot.currency())) {
            throw new ConflictException("Wompi transaction currency does not match the recharge");
        }
    }

    private String cleanError(Exception exception) {
        String message = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private void requireTechnician(User user) {
        if (!user.isActiveAs(Role.TECHNICIAN)) throw new ForbiddenException("Technician mode is required");
    }

    private void requireAdmin(User user) {
        if (user.getRole() != Role.ADMIN) throw new ForbiddenException("Admin role is required");
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
