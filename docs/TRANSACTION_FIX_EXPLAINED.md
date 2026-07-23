# Payment Confirmation Flow - Fixed Transaction Model

## Before Fix (Problematic Flow)
```
PaymentController.confirmPayment()
    ↓
PaymentServiceImpl.confirmPayment() [@Transactional - SHARED CONTEXT]
    ├─ Update Payment.status = COMPLETED ✓
    ├─ Save Payment ✓
    ├─ reconcileOrderPaymentStatus() [SAME TRANSACTION]
    │   ├─ Update Order.paymentStatus = COMPLETED ✓
    │   ├─ Save Order ✓
    │   └─ ticketService.ensureReceiptForOrder() [SAME TRANSACTION] 
    │       ├─ Try to create Receipt
    │       ├─ ❌ ERROR: DataIntegrityViolationException or any exception
    │       └─ Transaction marked as ROLLBACK-ONLY 🚨
    └─ ENTIRE TRANSACTION ROLLS BACK (including Payment confirmation!) ❌
```

## After Fix (Proper Transaction Isolation)
```
PaymentController.confirmPayment()
    ↓
PaymentServiceImpl.confirmPayment() [@Transactional]
    ├─ Update Payment.status = COMPLETED ✓
    ├─ Save Payment ✓
    ├─ reconcileOrderPaymentStatus() [SAME TRANSACTION]
    │   ├─ Update Order.paymentStatus = COMPLETED ✓
    │   ├─ Save Order ✓
    │   └─ Register Post-Commit Callback ✓
    └─ TRANSACTION COMMITS SUCCESSFULLY ✓
        ↓
    [Transaction Commit Boundary]
        ↓
    Post-Commit Callbacks Execute (Independent)
    ├─ ticketService.ensureReceiptForOrder()
    │   ├─ Try to create Receipt
    │   ├─ ✅ SUCCESS: Receipt created and persisted
    │   └─ Response to client ✓
    └─ 🔴 EVEN IF ERROR: Receipt generation fails
        ├─ Error is logged
        └─ Does NOT affect Payment confirmation ✅
```

## Code Flow Comparison

### Before (Transactional Coupling)
```java
@Transactional
public PaymentDto confirmPayment(Long paymentId, Long currentUserId) {
    // Payment confirmation happens in main transaction
    payment.setStatus(PaymentStatus.COMPLETED);
    paymentRepository.save(payment);
    
    // This call shares the SAME transaction context
    reconcileOrderPaymentStatus(payment.getOrder().getId());
    
    // Inside reconciliation:
    // if (newStatus == COMPLETED) {
    //     try {
    //         // This fails and marks transaction as rollback-only!
    //         ticketService.ensureReceiptForOrder(saved, null);
    //     } catch (Exception e) {
    //         // Too late - transaction already marked for rollback
    //         log.error(...);
    //     }
    // }
}
```

### After (Decoupled Post-Commit)
```java
@Transactional
public PaymentDto confirmPayment(Long paymentId, Long currentUserId) {
    // Payment confirmation happens in main transaction
    payment.setStatus(PaymentStatus.COMPLETED);
    paymentRepository.save(payment);
    
    // This call shares the SAME transaction context
    reconcileOrderPaymentStatus(payment.getOrder().getId());
    
    // Inside reconciliation (when payment is complete):
    // if (newStatus == COMPLETED) {
    //     // This registers a callback to run AFTER transaction commits
    //     TransactionUtils.runAfterCommit(() -> {
    //         try {
    //             ticketService.ensureReceiptForOrder(saved, null);
    //         } catch (Exception e) {
    //             // Safe to fail - main transaction already committed
    //             log.error(...);
    //         }
    //     });
    // }
}
// Transaction commits here ✓
// Post-commit callbacks execute here (independent of transaction)
```

## Transaction Semantics

| Aspect | Before Fix | After Fix |
|--------|-----------|----------|
| **Payment Save** | ❌ Can rollback | ✅ Guaranteed to persist |
| **Order Reconciliation** | ❌ Can rollback | ✅ Guaranteed to persist |
| **Receipt Generation** | ❌ Can block payment | ✅ Async after payment commit |
| **Error Handling** | ❌ Silent rollback | ✅ Logged, doesn't affect payment |
| **User Feedback** | ❌ "Transaction failed" | ✅ "Payment confirmed" |
| **Data Consistency** | ❌ Inconsistent | ✅ Consistent |

## Error Scenarios

### Scenario 1: Database Constraint on Receipt
```
BEFORE: Payment not saved ❌
AFTER:  Payment saved ✅, Receipt generation fails (logged) ⚠️
```

### Scenario 2: Service Timeout on Receipt Generation
```
BEFORE: Payment not saved ❌
AFTER:  Payment saved ✅, Receipt generation times out (logged) ⚠️
```

### Scenario 3: Network Error on Notification
```
BEFORE: Payment not saved ❌
AFTER:  Payment saved ✅, Notification fails (logged) ⚠️
```

## Key Changes Summary

1. **PaymentServiceImpl.reconcileOrderPaymentStatus()** (Lines 215-231)
   - Wrapped receipt generation in `TransactionUtils.runAfterCommit()`
   - Receipt generation now happens AFTER payment transaction commits
   - Exceptions in receipt generation don't affect payment confirmation

2. **GlobalExceptionHandler** (Lines 206-220)
   - Added handler for `UnexpectedRollbackException`
   - Provides graceful error response if transaction does rollback for other reasons
   - Includes full stack trace in logs for debugging

## Why This Works

The `TransactionUtils.runAfterCommit()` method:
1. Registers the action with Spring's `TransactionSynchronizationManager`
2. The action is ONLY executed if the transaction commits successfully
3. If the transaction rolls back, the action is never executed
4. Receipt generation is therefore "fire-and-forget" relative to the payment transaction

## Guarantees

✅ **Payment confirmation** always persists (if no exception before save)
✅ **Order reconciliation** always persists (if no exception before save)
✅ **Receipt generation** is best-effort (logs errors, doesn't block payment)
✅ **User receives success response** when payment is confirmed
✅ **Asynchronous receipt creation** doesn't delay API response

