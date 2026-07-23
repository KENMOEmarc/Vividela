# Quick Start: Client Notification System

## Overview

The laundry backend now automatically notifies clients when important events occur:
1. **Order Created** → Email, SMS, In-App notification with order details & barcode
2. **Ticket Generated** → Email, SMS, In-App notification with barcode code
3. **All Garments Same State** → In-App only notification
4. **All Garments Ready** → Email, SMS, In-App notification
5. **Payment Received** → Email, SMS, In-App notification

## Key Endpoints

### 1. View Your Notifications (Client)
```bash
curl -X GET "http://localhost:8080/api/notifications" \
  -H "Authorization: Bearer {your_jwt_token}"
```

Response:
```json
{
  "success": true,
  "message": "Notifications récupérées",
  "data": [
    {
      "id": 1,
      "user": {...},
      "order": {...},
      "subject": "Votre commande a été créée (#123)",
      "message": "Détails: dépôt le 2026-07-02. Montant estimé: 45.99",
      "notificationType": "IN_APP",
      "status": "SUCCESS",
      "isRead": false,
      "sentAt": "2026-07-02T10:00:00Z"
    }
  ]
}
```

### 2. Mark Notification as Read
```bash
curl -X PATCH "http://localhost:8080/api/notifications/1/read" \
  -H "Authorization: Bearer {your_jwt_token}"
```

### 3. Create Order (Automatically Triggers Notification)
```bash
curl -X POST "http://localhost:8080/api/orders" \
  -H "Authorization: Bearer {your_jwt_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "john_doe",
    "depositDate": "2026-07-02",
    "expectedDeliveryDate": "2026-07-09",
    "shippingAddress": "123 Main St",
    "notes": "Delicate items"
  }'
```

**Notifications Generated:**
- ✉️ Email: "Votre commande a été créée (#<order_id>)"
- 📱 SMS: Order confirmation with deposit date
- 📲 In-App: Order notification with order details

### 4. Generate Ticket/Barcode (Automatically Triggers Notification)
```bash
curl -X POST "http://localhost:8080/api/orders/123/ticket" \
  -H "Authorization: Bearer {your_jwt_token}"
```

**Notifications Generated:**
- ✉️ Email: "Ticket généré — commande #123"
- 📱 SMS: Barcode code
- 📲 In-App: Ticket notification

### 5. Update Article Status (May Trigger Notification)
```bash
curl -X PUT "http://localhost:8080/api/orders/123/items/456" \
  -H "Authorization: Bearer {your_jwt_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "IRONING"
  }'
```

**Notifications (when all articles have same status):**
- If all articles are NOT COMPLETED:
  - 📲 In-App only: "Tous les vêtements ont changé d'état"
- If all articles are COMPLETED:
  - ✉️ Email: "Votre commande est prête"
  - 📱 SMS: "Commande prête pour récupération"
  - 📲 In-App: Ready notification

### 6. Record Payment (Automatically Triggers Notification)
```bash
curl -X POST "http://localhost:8080/api/payments" \
  -H "Authorization: Bearer {your_jwt_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 123,
    "paymentMethod": "CASH",
    "amount": 45.99,
    "payerPhone": "+33612345678",
    "transactionReference": "TXN-123456"
  }'
```

**Notifications Generated:**
- ✉️ Email: "Paiement reçu — commande #123"
- 📱 SMS: "Paiement de 45.99 reçu"
- 📲 In-App: Payment confirmation notification

## Notification Types

| Event | Email | SMS | In-App | Channels |
|-------|:-----:|:---:|:------:|----------|
| Order Created | ✓ | ✓ | ✓ | 3 |
| Ticket Generated | ✓ | ✓ | ✓ | 3 |
| All Same State | ✗ | ✗ | ✓ | 1 |
| All Ready | ✓ | ✓ | ✓ | 3 |
| Payment Received | ✓ | ✓ | ✓ | 3 |

## Notification Messages

### Order Created
```
Subject: Votre commande a été créée (#<ID>)
Message: Détails: dépôt le <DATE>. Montant estimé: <AMOUNT>
```

### Ticket Generated
```
Subject: Ticket généré — commande #<ID>
Message: Votre ticket a été généré. Code-barre: <BARCODE>. 
         Détails de la commande disponibles en ligne.
```

### All Garments Same State
```
Subject: Tous les vêtements ont changé d'état
Message: Tous les vêtements de votre commande #<ID> sont maintenant : <STATE>
```

### All Garments Ready
```
Subject: Votre commande est prête — #<ID>
Message: Tous les vêtements de votre commande sont prêts à être récupérés.
```

### Payment Received
```
Subject: Paiement reçu — commande #<ID>
Message: Paiement de <AMOUNT> reçu. Référence: <REFERENCE>
```

## Database

### Notifications Table
```sql
SELECT * FROM notifications 
WHERE user_id = ? 
ORDER BY created_at DESC;
```

### Fields
- `id`: Unique notification ID
- `user_id`: Recipient user
- `order_id`: Related order (nullable)
- `subject`: Notification title
- `message`: Notification content
- `notification_type`: EMAIL, SMS, PUSH, IN_APP, PHONE_CALL
- `status`: PENDING, SUCCESS, FAILED
- `is_read`: Whether user has read the notification
- `sent_at`: When notification was sent

## Configuration

### Email Service (TODO)
Currently using placeholder. To activate:
1. Add Spring Mail dependency
2. Configure email properties in `application.properties`
3. Replace `SimpleEmailSender` with `JavaMailSender`

### SMS Service (TODO)
Currently using placeholder. To activate:
1. Add Twilio or Nexmo SDK
2. Configure API credentials
3. Replace `SimpleSmsSender` with provider-specific implementation

## Logging

All notifications are logged:
```
INFO com.template.auth.service.impl.NotificationServiceImpl - 
     Notification enregistrée pour user=456 order=123 subject=Votre commande a été créée (#123)
```

Check logs for:
- Sent notifications
- Email/SMS sending attempts
- Delivery status

## Security

- ✓ All endpoints require authentication
- ✓ Users can only view their own notifications
- ✓ Automatic creation via business events
- ✓ No manual notification injection possible
- ✓ Spring Security @PreAuthorize on all endpoints

## Testing

### Manual Testing Steps

1. **Create Test Order**
   ```bash
   curl -X POST "http://localhost:8080/api/orders" \
     -H "Authorization: Bearer {token}" \
     -H "Content-Type: application/json" \
     -d '{"userName":"test_customer",...}'
   ```
   ✓ Verify notification created in database
   ✓ Check application logs for email/SMS logging

2. **Check Notifications**
   ```bash
   curl -X GET "http://localhost:8080/api/notifications" \
     -H "Authorization: Bearer {token}"
   ```
   ✓ Verify notification appears in list

3. **Mark as Read**
   ```bash
   curl -X PATCH "http://localhost:8080/api/notifications/{id}/read" \
     -H "Authorization: Bearer {token}"
   ```
   ✓ Verify isRead changes to true

4. **Full Workflow**
   - Create order → Check notifications ✓
   - Generate ticket → Check notifications ✓
   - Update articles → Check notifications ✓
   - Record payment → Check notifications ✓

## Troubleshooting

### No Notifications Appearing
- Check application logs for errors
- Verify user email and phone are set
- Check database: `SELECT * FROM notifications`
- Verify Spring Security allows access

### Email/SMS Not Sending
- Currently using placeholder implementations (logging only)
- Replace with real providers (Twilio, SendGrid, etc.)
- Check configuration in `application.properties`
- Review logs for email/SMS sending

### Notifications Not in Database
- Check user has email and phone fields populated
- Verify transaction commits after notification save
- Check database connection
- Review Spring transaction configuration

## Best Practices

1. **Always retrieve notifications for logged-in user only**
   - Don't hardcode user IDs
   - Use `@AuthenticationPrincipal UserDetails`

2. **Mark notifications as read**
   - Helps track which customers have seen updates
   - Clean UI with unread count badge

3. **Don't override automatic notifications**
   - Let system handle all notification triggers
   - No manual notification injection needed

4. **Monitor notification logs**
   - Track delivery success/failure
   - Identify missing email/SMS configurations

5. **Test all notification types**
   - Order creation
   - Ticket generation
   - Article status changes
   - Payment recording

## Support

For issues or questions:
1. Check application logs
2. Review database notifications table
3. Verify Spring Security configuration
4. Ensure user email/phone are set
5. Test with curl examples above

## Next Steps

1. ✅ Core notification system implemented
2. ⏳ TODO: Integrate real email service
3. ⏳ TODO: Integrate real SMS provider
4. ⏳ TODO: Add notification templates/customization
5. ⏳ TODO: Add user notification preferences
6. ⏳ TODO: Add notification retry mechanism

