# Aviasales Backend

## MongoDB integration

### Run MongoDB locally
```bash
docker compose -f docker-compose.mongo.yml up -d
```

Spring will use:
```properties
spring.data.mongodb.uri=${MONGODB_URI:mongodb://localhost:27017/aviasales}
```

You can override URI:
```bash
export MONGODB_URI=mongodb://localhost:27017/aviasales
```

### Mongo-powered features
1. `GET /api/v1/flights` now uses Mongo cache for repeated identical searches.
2. Search analytics stored in Mongo:
- `GET /api/v1/flights/analytics/summary?days=30`
- `GET /api/v1/flights/analytics/top-routes?days=30&limit=10`
- `GET /api/v1/flights/analytics/top-airlines?days=30&limit=10`
3. Booking timeline in Mongo:
- `GET /api/v1/bookings/{bookingId}/timeline`

## Logical user pipeline
1. Search flights:
- `GET /api/v1/flights`
2. Check offer availability and fresh base fare:
- `POST /api/v1/orders/check-availability`
3. Create booking with `expectedPrice` from the previous step:
- `POST /api/v1/bookings`
4. Process payment for booked total:
- `POST /api/v1/payments/process`

### Booking request contract (important)
`CreateBookingRequest` now requires `expectedPrice`:
```json
{
  "offerId": "string",
  "expectedPrice": {
    "amount": 7999.00,
    "currency": "RUB"
  },
  "contactInfo": {
    "email": "user@example.com",
    "phone": "+79990000000"
  },
  "passengers": []
}
```

### Payment request contract (important)
`PaymentDataRequest` no longer accepts `amount` and `currency`.
The backend always charges the final amount from `booking.totalAmount`.

```json
{
  "bookingId": "string",
  "payment": {
    "paymentToken": "tok_test",
    "saveCard": true,
    "paymentMethod": "BANK_CARD"
  },
  "clientInfo": {
    "ipAddress": "127.0.0.1",
    "userAgent": "PostmanRuntime/7.0.0",
    "returnUrl": "https://example.com/return"
  }
}
```

## Existing utility commands

Connection to Postgres:
```bash
docker exec -it postgres-app bash
```

Отправка файлов на helios:
```bash
scp -P 2222 aviasales-0.0.1-SNAPSHOT.jar s408522@helios.se.ifmo.ru:./
```

Пробрасывание портов на helios:
```bash
ssh -p 2222 -L 8080:localhost:8080 s408522@helios.se.ifmo.ru
```
