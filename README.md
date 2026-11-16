# Booking System

REST API for managing open mic and comedy nights. Venues publish events with a schedule of time slots, performers manage their performances and availability, and guests book seats.

## Features

- Multi venue: every user, event and booking belongs to a venue, and data of other venues is never visible.
- Three roles: `ADMIN`, `PERFORMER` and `GUEST`, enforced with method level security.
- Admins create, update and delete events and assign performers to time slots. Performers and guests are notified by email when an event changes or is cancelled.
- Performers manage their performances and their weekly availability.
- Guests and admins create, modify and cancel bookings, with seat capacity checks that hold under concurrent requests.
- Stateless JWT authentication and per client rate limiting (10 requests per minute on `/auth/**`, 100 per minute elsewhere).
- Interactive API documentation with Swagger UI.

## Tech stack

Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA, PostgreSQL 16, JJWT, Bucket4j, MapStruct, Lombok, springdoc OpenAPI. Tests use JUnit 5, Mockito, AssertJ, H2 and Testcontainers.

## Getting started

### Prerequisites

- JDK 21
- Docker and Docker Compose

### 1. Start the database

```bash
docker compose up -d
```

This starts PostgreSQL on `localhost:5433` (container `booking-postgres`). If you change the database credentials after the first start, remove the old volume with `docker compose down -v`, because PostgreSQL only reads them when the data directory is created.

### 2. Run the application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` profile seeds sample data (3 venues, users, events and bookings). Without it the database starts empty, and since venues and their first admin cannot be created through the API, you would have no way to log in.

The API is available at `http://localhost:8080` and Swagger UI at `http://localhost:8080/swagger-ui/index.html`.

### 3. Try it

Log in with a seeded admin. The login request also needs the venue name.

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"adminA@example.com","password":"adminA_pass","venueName":"venue A"}'
```

The response is a JWT. Send it as `Authorization: Bearer <token>` on every other request, or paste it into the Authorize button in Swagger UI.

Seeded accounts:

| Venue | Email | Password | Role |
|-------|-------|----------|------|
| venue A | adminA@example.com | adminA_pass | ADMIN |
| venue B | adminB@example.com | adminB_pass | ADMIN |
| venue A | alice@example.com, ashley@example.com, ariana@example.com | password | GUEST |
| venue A | adam@example.com, alex@example.com, adrien@example.com | password | PERFORMER |
| venue B | bob@example.com, bert@example.com | password | GUEST |
| venue B | benjamin@example.com | password | PERFORMER |

## Configuration

Every value can be overridden with an environment variable. The defaults are meant for local development only and must not be used in a real deployment.

| Variable | Purpose |
|----------|---------|
| `DB_URL` | JDBC url of the database |
| `DB_USERNAME` | Database user, shared by the app and `docker-compose.yml` |
| `DB_PASSWORD` | Database password, shared by the app and `docker-compose.yml` |
| `JWT_SECRET` | Secret used to sign JWT tokens |
| `MAIL_USERNAME` | SMTP username |
| `MAIL_PASSWORD` | SMTP password |

The SMTP server defaults to `localhost:1025`, which fits a local mail catcher such as MailHog or Mailpit. Without one the API still works, but notification emails cannot be delivered. The application time zone is set by `app.timezone` in `application.properties`.

## API overview

| Area | Endpoints | Access |
|------|-----------|--------|
| Auth | `POST /auth/signup`, `POST /auth/login` | public |
| Events | `GET /events` (paginated list), `GET /events/{date}` (events on that date) | authenticated |
| Events | `POST /events`, `POST /events/{eventId}` (copy an event to a new location and date, shifting its schedule), `PUT /events/{id}`, `DELETE /events/{id}` | ADMIN |
| Bookings | `POST /bookings/{eventId}/book` (admins can book for another user with `userId`), `PUT /bookings/{bookingId}` (change the quantity), `DELETE /bookings/{id}` (cancel the booking), `GET /bookings/userBookings` (admins can pass `userId`) | ADMIN, GUEST |
| Performances | `GET /performances` (scheduled slots, admins can pass `userId`), `POST /performances` (admins can pass `userId`), `DELETE /performances` (takes `performanceId`, marks it INACTIVE instead of removing it), `PUT /performances/{performanceId}` (change type and duration) | ADMIN, PERFORMER |
| Users | `PUT /users/{userId}` (promote a guest to admin), `DELETE /users/{userId}` (soft delete, refused if the user has confirmed bookings), `GET /users/performers` (optionally only those available on a `date`), `GET /users/performers/{performerId}` | ADMIN |
| Users | `PUT /users/me/availability` (replaces the weekly availability, the body lists the available days) | PERFORMER |

Swagger UI lists the full request and response models.

## Running the tests

```bash
./mvnw test
```

Unit and integration tests run on an in memory H2 database. The concurrency tests in `src/test/java/.../concurrency` start a real PostgreSQL container through Testcontainers, so Docker must be running.

## Project structure

```
src/main/java/com/razza/bookingsystem
  config/       security, JWT, OpenAPI and sample data setup
  security/     JWT filter, rate limiting filter, user details
  controller/   REST endpoints
  service/      business logic
  repository/   Spring Data JPA repositories
  domain/       JPA entities and enums
  dto/          request and response objects
  mapper/       MapStruct mappers
  exception/    custom exceptions and the global exception handler
```
