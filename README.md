# Ticket Booking Service

A booking backend built around one deliberately reproduced bug: **the double-booking race**.
Two requests hit the same seat at the same time and both succeed. The project reproduces it
on demand, then fixes it two different ways and measures what each fix costs.

**Stack:** Java 17 · Spring Boot · MySQL · Spring Data JPA · Redis · Docker · JUnit 5 · Testcontainers

**Status:** not started · build last, after the other two

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

## Setup

```bash
cd ~/projects/ticket-booking-service
curl -G https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java -d javaVersion=17 \
  -d groupId=com.swayam -d artifactId=booking -d name=booking \
  -d packageName=com.swayam.booking \
  -d dependencies=web,data-jpa,mysql,data-redis,security,validation,actuator,testcontainers \
  -o starter.zip && unzip starter.zip && rm starter.zip
```

```bash
podman run -d --name booking-mysql -p 3307:3306 \
  -e MYSQL_ROOT_PASSWORD=booking -e MYSQL_DATABASE=booking docker.io/library/mysql:8.4
podman run -d --name booking-redis -p 6379:6379 docker.io/library/redis:7
```

> Port **3307** — the Udemy course's local MySQL 8.4 already owns 3306, and per your RAM
> notes `mysqld` is started on demand. Don't fight it for the port.

**8 GB of RAM.** MySQL + Redis + the JVM + Testcontainers spinning up two more containers is
the heaviest thing in this repo set. Stop the ledger and import containers before M9:

```bash
podman stop ledger-pg import-pg
```

k6 for M7:

```bash
sudo dnf install k6    # or: podman run --rm -i docker.io/grafana/k6 run - < script.js
```

## Git

```bash
git add -A && git commit -m "docs: project spec and build plan"
```
