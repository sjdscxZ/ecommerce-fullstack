# ecommerce-fullstack

Full-stack e-commerce reference: **Spring Boot 3** backend + **Angular 18** standalone frontend. Product catalog, cart, checkout flow with stock decrement, customer order history.

## Stack

| Layer | Tech |
|---|---|
| Backend | Java 17, Spring Boot 3.3, Spring Data JPA, Spring Security (stateless), Jakarta Validation, JJWT 0.12 |
| DB | PostgreSQL 16 (H2 fallback for tests) |
| Frontend | Angular 18 (standalone components, signals), TypeScript 5.4, RxJS |
| Orchestration | Docker Compose (Postgres + backend + frontend dev server) |
| Tests | JUnit 5 + MockMvc (backend) |

## Run locally

### Option 1 — Docker Compose

```bash
docker compose up --build
```
- Frontend: http://localhost:4200
- Backend API: http://localhost:8080/api
- Postgres: localhost:5432 (shop / shop)

### Option 2 — Independent

```bash
# Terminal A — backend
cd backend
./mvnw spring-boot:run                # uses H2 in-memory by default

# Terminal B — frontend
cd frontend
npm install
npm run start                         # http://localhost:4200
```

## Try it

```bash
# Browse products (also pre-seeded by data.sql)
curl http://localhost:8080/api/products

# Place an order
curl -X POST http://localhost:8080/api/orders/checkout \
  -H 'Content-Type: application/json' \
  -d '{
    "email":"alice@example.com",
    "items":[
      {"productId":1,"quantity":2},
      {"productId":2,"quantity":1}
    ]
  }'

# Look up customer history
curl "http://localhost:8080/api/orders/by-email?email=alice@example.com"
```

The Angular UI presents the product grid, supports add-to-cart with signal-based reactivity, and shows the running cart total in the header.

## Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/products` | List products |
| GET | `/api/products/{id}` | One product |
| POST | `/api/products` | Create a product (admin scaffold) |
| POST | `/api/orders/checkout` | Place an order, decrement stock, return order |
| GET | `/api/orders/by-email?email=…` | Customer order history |
| GET | `/actuator/health` | Health check |

## Project layout

```
ecommerce-fullstack/
├── backend/                          # Spring Boot 3
│   ├── pom.xml
│   ├── src/main/java/com/sjdscxz/ecommerce/EcommerceApplication.java
│   ├── src/main/resources/{application.yml, data.sql}
│   └── src/test/java/com/sjdscxz/ecommerce/EcommerceApplicationTest.java
├── frontend/                         # Angular 18 standalone
│   ├── package.json
│   ├── angular.json
│   ├── tsconfig.json
│   └── src/
│       ├── main.ts
│       ├── index.html
│       ├── app/
│       │   ├── app.component.ts
│       │   ├── core/product.service.ts
│       │   ├── products/product-list.component.ts
│       │   └── cart/cart.service.ts
│       └── environments/environment.ts
└── docker-compose.yml
```

## Design choices

- **Standalone Angular components** (no NgModules) — Angular 18 native style, smaller bundle, cleaner imports.
- **Signals for cart state** (`CartService.lines = signal(...)`) — reactive UI without Subjects or async pipes.
- **CORS configured only for `http://localhost:4200`** — narrow allowed origins.
- **Stock decrement happens atomically inside the checkout endpoint** — if any item lacks stock, the whole order returns 409 and no stock is taken.
- **DTO records** for checkout request — concise validation via Jakarta Bean Validation.
- **`data.sql`** seeds the catalog at startup; pure JPA `ddl-auto: update` handles schema.

## Roadmap

- [ ] JWT-protected admin endpoints + login UI
- [ ] Stripe (or stub) payment integration on the checkout
- [ ] Order line history page in the Angular UI
- [ ] Product search / filter / category facets
- [ ] e2e tests with Playwright

## Resume reference

> *"Full-stack e-commerce: Spring Boot 3 backend + Angular 18 frontend, JWT auth, PostgreSQL, Docker Compose"*

This repo demonstrates Spring Boot + Angular integration end-to-end. JWT scaffolding is in `pom.xml` and `SecurityConfig`; login flow is on the roadmap.

## License

MIT — see [LICENSE](LICENSE).
