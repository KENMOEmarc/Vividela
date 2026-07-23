# Notification System Documentation

## Overview

A comprehensive notification system has been implemented to notify clients when:
1. A new order is created
2. A ticket/barcode is generated for an order
3. All garments in an order change to the same status
4. All garments in an order are ready (COMPLETED status)
5. A payment is received

Notifications are sent via three channels:
- **Email** (via configured mail service)
- **SMS** (via configured SMS provider)
- **In-app/Web** (stored in database and accessible through REST API)

## Architecture

### Core Components

#### 1. Entities
- **Notification**: JPA entity representing a notification in the database
  - `id`: Unique identifier
  - `user`: User receiving the notification
  - `order`: Related order (if applicable)
  - `subject`: Notification subject/title
  - `message`: Notification body/message
  - `notificationType`: Type (EMAIL, SMS, PUSH, IN_APP, PHONE_CALL)
  - `status`: Delivery status (PENDING, SUCCESS, FAILED)
  - `isRead`: Whether the user has read the notification
  - `sentAt`: Timestamp when notification was sent

#### 2. Services

**NotificationService Interface:**
- `notifyOrderCreated(Order order)`: Called when order is created
- `notifyTicketGenerated(Order order, Ticket ticket)`: Called when barcode is generated
- `notifyAllArticlesSameState(Order order, String stateLabel)`: Called when all items have same status
- `notifyAllArticlesReady(Order order)`: Called when all items are COMPLETED
- `notifyPaymentReceived(Order order, Payment payment)`: Called when payment is recorded

**NotificationServiceImpl:**
- Implements all notification methods
- Sends emails and SMS to customers
- Creates in-app notifications stored in database
- Handles all notification logic and content generation

**EmailSender / SmsSender:**
- Interfaces for extensibility
- `SimpleEmailSender` and `SimpleSmsSender` provide placeholder implementations
- TODO: Integrate with actual services (JavaMailSender, Twilio, Nexmo, etc.)

#### 3. Controllers

**NotificationController:**
- `GET /notifications`: Retrieve all notifications for the logged-in user
- `PATCH /notifications/{notificationId}/read`: Mark a notification as read

**PaymentController (Enhanced):**
- `POST /payments`: Record a payment and trigger notification

#### 4. Repositories

**NotificationRepository:**
- `findByUserIdOrderByCreatedAtDesc(Long userId)`: Get all notifications for a user

**PaymentRepository:**
- Extended with payment queries

## Integration Points

### 1. Order Creation
**Location:** `OrderServiceImpl.createOrder()`
```java
// After order is saved
notificationService.notifyOrderCreated(saved);
```
**Notifications sent:**
- Email: Order confirmation with deposit date and total amount
- SMS: Order confirmation
- In-app: Order confirmation with order ID link

### 2. Ticket Generation
**Location:** `TicketServiceImpl.getOrCreateTicket()`
```java
// After ticket is created
notificationService.notifyTicketGenerated(order, saved);
```
**Notifications sent:**
- Email: Ticket generated with barcode
- SMS: Ticket generated with barcode code
- In-app: Ticket generated notification

### 3. Article Status Changes
**Location:** `ArticleServiceImpl.updateArticle()`
- After an article status is updated, checks if all articles in the order have the same status
- Calls `checkAndNotifyArticleStatusChanges()` helper method

**Behavior:**
- If all articles have the same status: Notify in-app only
- If all articles reach COMPLETED: Notify via email, SMS, and in-app

### 4. Payment Recording
**Location:** `PaymentController.recordPayment()` → `PaymentServiceImpl.recordPayment()`
```java
// After payment is saved
notificationService.notifyPaymentReceived(order, saved);
```
**Notifications sent:**
- Email: Payment confirmation with amount and reference
- SMS: Payment confirmation with amount and reference
- In-app: Payment confirmation notification

## Notification Content

### Order Created
- **Subject:** "Votre commande a été créée (#<ORDER_ID>)"
- **Message:** "Détails: dépôt le <DATE>. Montant estimé: <AMOUNT>"

### Ticket Generated
- **Subject:** "Ticket généré — commande #<ORDER_ID>"
- **Message:** "Votre ticket a été généré. Code-barre: <BARCODE>. Détails de la commande disponibles en ligne."

### All Articles Same State
- **Subject:** "Tous les vêtements ont changé d'état"
- **Message:** "Tous les vêtements de votre commande #<ORDER_ID> sont maintenant : <STATE>"
- **Channels:** In-app only

### All Articles Ready
- **Subject:** "Votre commande est prête — #<ORDER_ID>"
- **Message:** "Tous les vêtements de votre commande sont prêts à être récupérés."
- **Channels:** Email, SMS, In-app

### Payment Received
- **Subject:** "Paiement reçu — commande #<ORDER_ID>"
- **Message:** "Paiement de <AMOUNT> reçu. Référence: <REFERENCE>"
- **Channels:** Email, SMS, In-app

## API Endpoints

### Retrieve User Notifications
```
GET /api/notifications
Authorization: Bearer <TOKEN>
Response: {
  "success": true,
  "message": "Notifications récupérées",
  "data": [
    {
      "id": 1,
      "subject": "Votre commande a été créée",
      "message": "...",
      "notificationType": "IN_APP",
      "status": "SUCCESS",
      "isRead": false,
      "sentAt": "2026-07-02T10:00:00Z"
    }
  ]
}
```

### Mark Notification as Read
```
PATCH /api/notifications/{notificationId}/read
Authorization: Bearer <TOKEN>
Response: {
  "success": true,
  "message": "Notification marquée comme lue",
  "data": null
}
```

### Record Payment
```
POST /api/payments
Authorization: Bearer <TOKEN>
Content-Type: application/json
Body: {
  "orderId": 123,
  "paymentMethod": "CASH",
  "amount": 45.99,
  "payerPhone": "+33612345678",
  "transactionReference": "TXN-123456"
}
Response: {
  "success": true,
  "message": "Paiement enregistré avec succès",
  "data": {
    "id": 1,
    "orderId": 123,
    "paymentMethod": "CASH",
    "amount": 45.99,
    ...
  }
}
```

## Database Schema Changes

### notifications table
```sql
ALTER TABLE notifications ADD COLUMN is_read BOOLEAN DEFAULT false NOT NULL;
```

## Configuration (TODO)

Currently, email and SMS senders are placeholder implementations. To activate real notifications:

### Email Configuration
Replace `SimpleEmailSender` with integration to:
- JavaMailSender (Spring Mail)
- SendGrid API
- AWS SES
- etc.

### SMS Configuration
Replace `SimpleSmsSender` with integration to:
- Twilio
- Nexmo/Vonage
- AWS SNS
- etc.

## Logging

All notifications are logged with:
```java
log.info("Notification enregistrée pour user={} order={} subject={}",
    userId, orderId, subject);
```

This allows monitoring of all notification activity in application logs.

## Security

- All notification endpoints require authentication
- Users can only view their own notifications
- Notification creation is automatic and triggered by business events
- No direct user manipulation of notification data

## Future Enhancements

1. Notification preferences/settings per user
2. Notification scheduling/batch processing
3. Notification templates management
4. Webhook support for external systems
5. Push notifications for mobile apps
6. Notification retries for failed deliveries
7. Notification analytics and reporting
8. Multi-language notification templates

