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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
        if (technician.getRole() != Role.TECHNICIAN) {
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
        String paymentUrl = wompi.checkoutUrl(reference, amount, "COP");
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
        if (recharge.getStatus() == RechargeStatus.APPROVED) return;
        if (recharge.getStatus() != RechargeStatus.PENDING) {
            throw new ConflictException("Recharge is not pending");
        }
        if (wompiTransactionId != null && !wompiTransactionId.isBlank()
                && recharges.findByWompiTransactionId(wompiTransactionId).isPresent()) {
            return;
        }
        TechnicianWallet wallet = wallets.findByTechnicianIdForUpdate(recharge.getTechnician().getId())
                .orElseGet(() -> ensureWallet(recharge.getTechnician()));
        transaction(wallet, WalletTransactionType.RECHARGE_APPROVED, recharge.getAmount(),
                reference, "Recarga aprobada por Wompi");
        recharge.setStatus(RechargeStatus.APPROVED);
        recharge.setWompiTransactionId(clean(wompiTransactionId));
        recharge.setApprovedAt(Instant.now());
    }

    @Transactional
    public void rejectRecharge(String reference, String wompiTransactionId) {
        TechnicianRecharge recharge = recharges.findByReferenceForUpdate(reference)
                .orElseThrow(() -> new NotFoundException("Recharge not found"));
        if (recharge.getStatus() != RechargeStatus.PENDING) return;
        TechnicianWallet wallet = wallets.findByTechnicianIdForUpdate(recharge.getTechnician().getId())
                .orElseGet(() -> ensureWallet(recharge.getTechnician()));
        transaction(wallet, WalletTransactionType.RECHARGE_REJECTED, BigDecimal.ZERO,
                reference, "Recarga rechazada por Wompi");
        recharge.setStatus(RechargeStatus.REJECTED);
        recharge.setWompiTransactionId(clean(wompiTransactionId));
        recharge.setRejectedAt(Instant.now());
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
        if (technician.getRole() != Role.TECHNICIAN) throw new ForbiddenException("Technician role is required");
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
        if (technician.getRole() != Role.TECHNICIAN) throw new ForbiddenException("Technician role is required");
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

    private void requireTechnician(User user) {
        if (user.getRole() != Role.TECHNICIAN) throw new ForbiddenException("Technician role is required");
    }

    private void requireAdmin(User user) {
        if (user.getRole() != Role.ADMIN) throw new ForbiddenException("Admin role is required");
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
