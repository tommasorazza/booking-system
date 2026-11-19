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

Every request is scoped to the venue of the logged-in user. "Anyone" means any user, regardless of role. "Owner" means the user the resource belongs to. Soft deleted resources are not removed from the database: events and bookings get the status `CANCELLED`, performances get the status `INACTIVE`, and users are flagged as deleted. Swagger UI lists the full request and response models.

### Auth (`auth-controller`)

| Endpoint | Description | Access |
|----------|-------------|--------|
| `POST /auth/signup` | Registers a new user account. | public |
| `POST /auth/login` | Authenticates a user and returns a JWT. | public |

### Events (`event-controller`)

| Endpoint | Description | Access |
|----------|-------------|--------|
| `GET /events` | Lists all non cancelled (`CONFIRMED`) events within a venue. Paginated. | anyone |
| `GET /events/{date}` | Returns the events on a given date. | anyone |
| `POST /events` | Creates a new event through an event DTO. | ADMIN |
| `POST /events/{eventId}` | Creates an event equal to the one having `eventId`, but with a different location and date. The schedule is shifted accordingly. | ADMIN |
| `PUT /events/{id}` | Given an event DTO, updates the event. | ADMIN |
| `DELETE /events/{id}` | Soft deletes (`CANCELLED`) an event. | ADMIN |

### Bookings (`booking-controller`)

Booking statuses are `CONFIRMED` and `CANCELLED`.

| Endpoint | Description | Access |
|----------|-------------|--------|
| `POST /bookings/{eventId}/book` | Creates a new booking given `eventId`, `userId` and quantity. Admins can book for another user with `userId`. | ADMIN, owner |
| `PUT /bookings/{bookingId}` | Modifies the number of seats tied to a specific booking. | ADMIN, owner |
| `DELETE /bookings/{id}` | Given a booking id, soft deletes (`CANCELLED`) the booking. Can only be done for future bookings. | ADMIN, owner |
| `GET /bookings/userBookings` | Given a `userId`, returns the list of bookings (both in the past and in the future). | ADMIN, owner |
| `GET /whoami` | Returns the currently authenticated user's info. | authenticated |

### Performances (`performance-controller`)

| Endpoint | Description | Access |
|----------|-------------|--------|
| `GET /performances` | Given a performer id, retrieves all the time slots of their performances in future and non cancelled events. | ADMIN, PERFORMER |
| `POST /performances` | Creates a new performance. Admins can pass `userId`. | ADMIN, owner |
| `PUT /performances/{performanceId}` | Given a performance id, modifies its duration and type (poetry, live music, etc.). | ADMIN, owner |
| `DELETE /performances` | Soft deletes (`INACTIVE`) a performance. Takes `performanceId`. | ADMIN, owner |

### Users (`user-controller`)

| Endpoint | Description | Access |
|----------|-------------|--------|
| `PUT /users/{userId}` | Makes a user admin. | ADMIN |
| `DELETE /users/{userId}` | Soft deletes a user. Can be performed on guests, performers and admins. Refused if the user has confirmed bookings. | ADMIN |
| `GET /users/performers` | Lists all performers available on a certain date. The date must be in the format `YYYY-MM-DD`. | ADMIN |
| `GET /users/performers/{performerId}` | Returns one performer's details. | ADMIN |
| `PUT /users/me/availability` | Lets the logged-in performer update their own availability by sending a string such as `"tuesday, friday"`. | PERFORMER |

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
