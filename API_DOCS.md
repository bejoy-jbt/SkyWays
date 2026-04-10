# Flight System API Documentation

This document describes the current REST APIs in the project, what each endpoint is used for, auth requirements, and where each is consumed.

## Base URL and Gateway

- Base URL from frontend: `/api`
- API Gateway route config: `api-gateway/src/main/resources/application.yml`

## Authentication and Authorization

- JWT required (`Authorization: Bearer <token>`) for:
  - `/api/flights/**`
  - `/api/bookings/**`
  - `/api/payments/**`
  - `/api/admin/**`
- Public routes:
  - `/api/auth/**` (register/login flow)
- Gateway forwards identity headers to downstream services:
  - `X-User-Email`
  - `X-User-Role`
- Admin guard:
  - `/api/admin/**` requires role `ADMIN`

Reference: `api-gateway/src/main/java/com/flightapp/apigateway/filter/JwtAuthFilter.java`

---

## 1) Auth APIs (`auth-service`)

Base path: `/api/auth`

### POST `/api/auth/register`
- Purpose: Register a new user and return auth payload/token.
- Used in: `frontend/src/pages/RegisterPage.tsx` (`authApi.register`)

### POST `/api/auth/login`
- Purpose: Login and return JWT + user details.
- Used in: `frontend/src/pages/LoginPage.tsx` (`authApi.login`)

### GET `/api/auth/me`
- Purpose: Get current user profile.
- Used in frontend: Not currently used.

### GET `/api/auth/admin/users`
- Purpose: Admin list of users.
- Used in: `frontend/src/components/admin/AdminUsers.tsx` (`authApi.getUsers`)

### PATCH `/api/auth/admin/users/{id}/toggle?enabled=true|false`
- Purpose: Admin enable/disable user.
- Used in: `frontend/src/components/admin/AdminUsers.tsx` (`authApi.toggleUser`)

---

## 2) Flight APIs (`flight-service`)

Base path: `/api/flights`

### POST `/api/flights/search`
- Purpose: Search flights by origin, destination, date, passengers.
- Used in: `frontend/src/pages/SearchPage.tsx` (`flightApi.search`)

### GET `/api/flights`
- Purpose: List flights.
- Used in: `frontend/src/components/admin/AdminFlights.tsx` (`flightApi.getAll`)

### GET `/api/flights/{id}`
- Purpose: Get details for one flight.
- Used in:
  - `frontend/src/pages/FlightDetailPage.tsx`
  - `frontend/src/pages/BookingPage.tsx`

### GET `/api/flights/{id}/seats`
- Purpose: Seat map and seat status for a flight.
- Used in: `frontend/src/pages/FlightDetailPage.tsx` (`flightApi.getSeats`)

### POST `/api/flights/{id}/seats/lock` *(internal saga endpoint)*
- Purpose: Lock seat during booking creation.
- Used by: `booking-service` (`BookingService.createBooking`)

### POST `/api/flights/{id}/seats/{seatNumber}/confirm` *(internal saga endpoint)*
- Purpose: Confirm locked seat after payment success.
- Used by: `booking-service` (`BookingService.onPaymentSuccess`)

### POST `/api/flights/{id}/seats/{seatNumber}/release` *(internal saga endpoint)*
- Purpose: Release seat on payment failure/cancel/delete.
- Used by: `booking-service` (`onPaymentFailure`, `cancelBooking`, `deleteBooking`, ticket cancellation flows)

---

## 3) Admin Flight APIs (`flight-service`)

Base path: `/api/admin/flights`

### POST `/api/admin/flights`
- Purpose: Create flight (admin).
- Used in: `frontend/src/components/admin/AdminFlights.tsx` (`flightApi.create`)

### PUT `/api/admin/flights/{id}`
- Purpose: Update flight (admin).
- Used in: `frontend/src/components/admin/AdminFlights.tsx` (`flightApi.update`)

### DELETE `/api/admin/flights/{id}`
- Purpose: Delete flight (admin).
- Used in: `frontend/src/components/admin/AdminFlights.tsx` (`flightApi.delete`)

---

## 4) Booking APIs (`booking-service`)

Base path: `/api/bookings`

### POST `/api/bookings`
- Purpose: Create booking. Supports multi-passenger and multi-seat flow.
- Used in: `frontend/src/pages/BookingPage.tsx` (`bookingApi.create`)

### GET `/api/bookings/my`
- Purpose: Get current user’s bookings.
- Used in: `frontend/src/pages/MyBookingsPage.tsx` (`bookingApi.getMy`)

### GET `/api/bookings/{id}`
- Purpose: Get booking by ID.
- Used in frontend: Not currently wired directly.

### POST `/api/bookings/{id}/cancel`
- Purpose: Cancel full booking (release seats + trigger refund/cancellation events).
- Used in frontend: Not currently wired.

### DELETE `/api/bookings/{id}`
- Purpose: Delete full booking record (with compensation/refund behavior when needed).
- Used in: `frontend/src/pages/MyBookingsPage.tsx` (`bookingApi.delete`)

### DELETE `/api/bookings/{id}/tickets/{seatNumber}`
- Purpose: Cancel one ticket by seat number in a multi-ticket booking.
- Used in frontend: Not currently wired.

### DELETE `/api/bookings/{id}/tickets/passenger/{passengerIndex}`
- Purpose: Cancel one passenger ticket by index in a multi-ticket booking.
- Used in: `frontend/src/pages/MyBookingsPage.tsx` (`bookingApi.deletePassengerTicket`)

### GET `/api/bookings/admin/all`
- Purpose: Admin list of all bookings.
- Used in: `frontend/src/components/admin/AdminBookings.tsx` (`bookingApi.getAll`)

---

## 5) Payment APIs (`payment-service`)

Base path: `/api/payments`

### POST `/api/payments/validate-card`
- Purpose: Validate card details before booking submission.
- Used in: `frontend/src/pages/BookingPage.tsx` (raw `api.post('/payments/validate-card')`)

### GET `/api/payments/my`
- Purpose: Get current user payments.
- Used in frontend: Exposed via `paymentApi.getMy` (not rendered in a dedicated user payments page currently).

### GET `/api/payments/admin/all`
- Purpose: Admin list of payments.
- Used in: `frontend/src/components/admin/AdminPayments.tsx` (`paymentApi.getAll`)

### GET `/api/payments/health`
- Purpose: Service health endpoint.
- Used in frontend: Not currently used.

### Note: `create-intent` mismatch
- `frontend/src/services/api.ts` has `paymentApi.createIntent -> POST /api/payments/create-intent`
- This endpoint is not implemented in `payment-service` controller currently.

---

## 6) Event Topics (Kafka, Saga Integration)

These are not REST endpoints but are core to booking/payment/notification behavior:

- `payment.requested` (booking -> payment)
- `payment.success` (payment -> booking)
- `payment.failed` (payment -> booking)
- `payment.refund.requested` (booking -> payment)
- `booking.confirmed` (booking -> notification)
- `booking.failed` (booking -> notification)
- `booking.cancelled` (booking -> notification)
- `booking.ticket.cancelled` (booking -> notification)

Primary topic configuration: `booking-service/src/main/java/com/flightapp/bookingservice/config/KafkaTopicConfig.java`

---

## 7) Frontend API Client Reference

Main client module: `frontend/src/services/api.ts`

- `authApi`: register, login, admin user management
- `flightApi`: search/list/detail/seats + admin create/update/delete
- `bookingApi`: create/my/get/delete + delete passenger ticket + admin list
- `paymentApi`: my/all (+ currently mismatched createIntent)

