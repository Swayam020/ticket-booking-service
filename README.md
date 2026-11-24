# Ticket Booking Service

A booking backend built around one deliberately reproduced bug: **the double-booking race**.
Two requests hit the same seat at the same time and both succeed. The project reproduces it
on demand, then fixes it two different ways and measures what each fix costs.

**Stack:** Java 17 · Spring Boot · MySQL · Spring Data JPA · Redis · Docker · JUnit 5 · Testcontainers

**Status:** M0 done — Spring Boot 4.1 app boots against MySQL 8.4, `/actuator/health` reports `db: UP`

---

## Why this project

This is the strongest of the three and the reason is narrow: **almost every fresher claims a
booking app, and almost none of them can show you the race.** Reproducing a concurrency bug
on demand, fixing it twice, and having numbers for both is a different conversation from
"I built a ticket booking system with Spring Boot."

Build it last. It needs everything the other two teach you, plus Redis, plus load testing.

---

## The questions this project has to let you answer

- Show me the race. Two threads, one seat — what's the exact interleaving that lets both
  bookings commit? Where is the read and where is the write?
- Why doesn't `@Transactional` alone fix it? What isolation level are you on, and what would
  `SERIALIZABLE` do to your throughput?
- You fixed it with a pessimistic lock. What SQL does `PESSIMISTIC_WRITE` actually emit, and
  what happens to the second request while the first holds the lock?
- You also fixed it with a Redis distributed lock. Why would anyone reach for that when the
  database lock already works?
- Your Redis lock has a TTL. What happens if the holder dies before releasing? What happens
  if it takes *longer* than the TTL and then releases someone else's lock?
- Why not optimistic locking with `@Version` here? When would that be the better choice?
- Cache-aside on the show catalogue: what's your invalidation path when a show changes, and
  what's the window where a client can read stale data?
- You measured X req/s versus Y. What exactly was the load, and what was the bottleneck in
  each case — the lock, the network round-trip, or the database?

**The honest answer to several of these is a trade-off, not a winner.** Interviewers are
testing whether you know that.

---

## Build plan

Longer than the other two. One milestone per sitting.

| # | Milestone | Done when |
|---|---|---|
| M0 | Scaffold + MySQL + `/actuator/health` | Boots against a real MySQL |
| M1 | Schema — `Venue`, `Show`, `Seat`, `Booking`, normalised | Rows persist, relationships load |
| M2 | REST API — browse shows, view seats, book a seat | Happy path works over HTTP |
| M3 | JWT-secured endpoints | Unauthenticated booking is rejected |
| M4 | **Reproduce the race** — fire N concurrent requests at one seat, both succeed | You have a failing test that proves the bug |
| M5 | Fix one — pessimistic locking. Same test now passes | Race test green, and you can name the SQL |
| M6 | Fix two — Redis distributed lock, behind a config switch | Both fixes exist, switchable |
| M7 | Measure both — k6 load test, record req/s for each | Two real numbers, plus the methodology |
| M8 | Redis cache-aside on catalogue reads + documented invalidation | Cache hit/miss visible, staleness window stated |
| M9 | Testcontainers — race test runs against real MySQL + Redis | The bug cannot regress silently |
| M10 | Docker, GitHub Actions CI against real MySQL/Redis/Kafka | Green pipeline on every push |
| M11 | Kafka — booking events published, notification service consumes async | Booking still succeeds if notification is down |
| M12 | React frontend — browse shows, pick seats, book, see confirmation | Can complete a booking in a browser, seat conflicts shown live |
| M13 | Deploy — Docker Compose on EC2 (or Render), frontend on its own host | Live URL for both, README updated with it |

> **M4 is the whole project.** If you skip straight to the fix you will have a booking app
> with a lock in it and nothing to talk about. Write the failing test first and keep it in
> the repo — the test that proves the bug is the artefact, not the fix.

---

## Run it locally

Needs Java 17 and Podman (or Docker) with `podman compose`.

```bash
podman compose up -d db     # MySQL 8.4 on host port 3307
./mvnw spring-boot:run      # API on :8080
curl localhost:8080/actuator/health
```

> Port **3307**, not 3306: a local MySQL used for coursework already owns 3306.

Tests start their own MySQL in a container (Testcontainers), so they don't need the compose
database. With Podman, point Testcontainers at its socket:

```bash
DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true ./mvnw test
```

Redis, Spring Security and k6 join at the milestones that need them (M3, M6, M7).
