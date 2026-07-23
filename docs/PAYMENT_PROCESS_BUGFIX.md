# Payment Process Fix - Transaction Rollback Issue

## Problem
The payment confirmation was experiencing a silent transaction rollback with the following error:
```
2026-07-13 20:00:02 INFO - Paiement 18 confirmé (COMPLETED) pour la commande 11
2026-07-13 20:00:02 ERROR - Erreur interne non gérée: Transaction silently rolled back because it has been marked as rollback-only
org.springframework.transaction.UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only
```

## Root Cause
The issue occurred in the `PaymentServiceImpl.confirmPayment()` method:

1. The payment confirmation was successfully saved to the database
2. The transaction then attempted to reconcile the order payment status via `reconcileOrderPaymentStatus()`
3. Inside reconciliation, when the order status changed to COMPLETED, the code tried to generate a receipt by calling `ticketService.ensureReceiptForOrder()`
4. **The critical issue**: Both methods share the same `@Transactional` context
5. If receipt generation encountered any error (database constraint, service exception, etc.), it would mark the ENTIRE transaction as "rollback-only"
6. This caused the previously confirmed payment to be silently rolled back, creating data inconsistency

## Solution
The fix implements two key changes:

### 1. **Isolated Receipt Generation (PaymentServiceImpl)**
Moved the receipt generation out of the transactional context by using `TransactionUtils.runAfterCommit()`:

```java
if (newStatus == PaymentStatus.COMPLETED) {
    // BUGFIX: Execute receipt generation AFTER payment confirmation commits
    TransactionUtils.runAfterCommit(() -> {
        try {
            String reference = ticketService.ensureReceiptForOrder(saved, null);
            log.info("Reçu {} disponible pour la commande {}", reference, orderId);
        } catch (Exception e) {
            log.error("Échec de la génération automatique du reçu {}", orderId, e);
        }
    });
}
```

**Benefits:**
- Receipt generation now executes **after** the payment transaction commits
- Receipt generation failures no longer affect payment confirmation
- The system is resilient to receipt generation delays or failures

### 2. **Enhanced Exception Handling (GlobalExceptionHandler)**
Added a dedicated handler for `UnexpectedRollbackException`:

```java
@ExceptionHandler(UnexpectedRollbackException.class)
public ResponseEntity<ApiResponse<Void>> handleUnexpectedRollback(UnexpectedRollbackException ex) {
    log.error("Transaction marquée comme rollback-only: {}", ex.getMessage(), ex);
    return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(
                    "La transaction a échoué de manière inattendue. Veuillez réessayer."));
}
```

**Benefits:**
- Catches any remaining transaction rollback exceptions
- Returns a user-friendly error message instead of a raw exception
- Provides clear logging for debugging

## Files Modified
1. **PaymentServiceImpl.java** - Lines 215-231
   - Wrapped receipt generation in `TransactionUtils.runAfterCommit()`
   - Added comprehensive comments explaining the fix

2. **GlobalExceptionHandler.java** - Lines 206-220
   - Added `UnexpectedRollbackException` handler
   - Added import for `UnexpectedRollbackException`

## Testing Recommendations
1. **Happy path**: Confirm payment successfully generates receipt
2. **Error handling**: Simulate receipt generation failure (e.g., database constraint) and verify payment confirmation persists
3. **Concurrency**: Test simultaneous payment confirmation and receipt generation
4. **Audit**: Check payment and receipt records are created in correct order

## Impact on Other Components
- **No breaking changes** to API contracts
- **No changes** to payment status logic
- **No changes** to receipt generation logic
- **Only improvement** to transaction isolation and resilience

## Performance Considerations
- **Minimal impact**: Receipt generation happens asynchronously after transaction commit
- **No blocking**: Payment confirmation response is sent immediately
- **Best-effort**: Receipt generation failures don't block the payment workflow

## Future Improvements
1. Consider implementing a background job for receipt generation with retry logic
2. Add metrics/monitoring for transaction rollback events
3. Consider implementing the notification service similarly with separate transactions

