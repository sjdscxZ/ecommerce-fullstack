# STUDY notes — ecommerce-fullstack

Interview prep for the Spring Boot + Angular flagship.

## Headline claims to own

1. "Built a full-stack e-commerce demo: Spring Boot 3 backend + Angular 18 frontend."
2. "Used JPA for catalog + orders, Spring Security stateless filter chain, CORS scoped to the dev origin."
3. "Atomic checkout with stock decrement; tested with MockMvc."

## Likely interview questions + answers

### Q1. "How does the frontend talk to the backend?"
- Angular `HttpClient` issues calls from `ProductService` to `http://localhost:8080/api/*`.
- Base URL comes from `src/environments/environment.ts` — production build swaps to `environment.prod.ts`.
- CORS configured on the backend (`SecurityConfig.addCorsMappings`) to allow `http://localhost:4200`.

### Q2. "Why standalone Angular components?"
Angular 17+ pushes standalone as the default. Benefits:
- No `@NgModule` ceremony
- Each component declares its own imports
- Tree-shaking is cleaner — unused features don't ship
- Easier to refactor and unit-test in isolation

### Q3. "Walk me through the checkout endpoint."
1. Validate request (`@Valid` on `CheckoutRequest`).
2. For each item: fetch product, check stock ≥ quantity. If not → return 409.
3. Decrement product stock, save.
4. Build `OrderLine` referencing the same `Product` entity.
5. Create the `CustomerOrder` with all lines, save. `cascade=ALL` saves lines transitively.
6. Return the saved order.

Caveat: **this is not transactional yet**. In a real system the whole method should be wrapped in `@Transactional` so a failure mid-way rolls back stock changes. The repo has this on the roadmap.

### Q4. "Why signals over RxJS BehaviorSubject in CartService?"
- Cleaner ergonomics: `cart.count()` returns a value, not a stream you have to subscribe to.
- `computed()` auto-tracks dependencies. `count`, `total` derive from `lines` without manually wiring `Subject.next` calls.
- Templates use `cart.count()` directly — no `| async` pipe needed.
- RxJS is still useful for streams (HTTP responses, timers); signals are for state.

### Q5. "How would you secure admin endpoints?"
- Add `JwtFilter` (same pattern as blog-api).
- `POST /api/products` → `@PreAuthorize("hasRole('ADMIN')")`.
- Frontend stores token after admin login, sends `Authorization: Bearer ...`.
- Refresh token + automatic re-issue handled by an Angular `HttpInterceptor`.

### Q6. "What's `cascade=ALL, orphanRemoval=true` doing here?"
- `cascade=ALL`: saving the order also saves its lines; deleting the order deletes the lines.
- `orphanRemoval=true`: if you `setLines(newList)`, any line not in the new list is deleted from the DB.
- Watch out: cascading to `Product` would be wrong — orders shouldn't delete products. So `OrderLine.product` has no cascade.

### Q7. "Scaling concerns?"
- N+1 queries on `getLines()` if we lazy-load — currently it's `@OneToMany(mappedBy="order")` without fetch hint, so accessing `order.lines` triggers a separate query. For list pages, use `JOIN FETCH` or a DTO projection.
- Product list grows: add pagination (already used in blog-api).
- Hot product page: cache `findById` results.
- Stock updates: under high concurrency, two orders for the last item could both succeed. Need optimistic locking (`@Version`) or `SELECT … FOR UPDATE`.

### Q8. "Why H2 in dev/test but Postgres in prod?"
H2's `MODE=PostgreSQL` mimics PG syntax closely enough that JPA-managed schemas behave identically for most cases. Tests run fast (in-memory, no Docker dependency). Real PG goes through `docker compose up`.

## Files to know

- `backend/.../EcommerceApplication.java` — entities, repos, controllers all in one file (compact for demo; in prod, split per package)
- `backend/.../application.yml` — datasource + JPA config
- `backend/.../data.sql` — seed data
- `frontend/src/app/cart/cart.service.ts` — signal-based state
- `frontend/src/app/products/product-list.component.ts` — standalone component with `@if`/`@for`
- `docker-compose.yml` — full stack

## Honest gaps

- No checkout transaction wrapping → not atomic.
- No payment integration — checkout just creates an order.
- No login/admin UI in the Angular frontend.
- Stock race conditions possible.
- Frontend has only product list — no cart drawer, no checkout page, no order history view.
- No e2e tests.

## Behavioral framing

**"What was tricky?"**
- *"Bridging the two ports cleanly — getting Angular's dev proxy and the backend's CORS to align. I chose CORS over a dev proxy to keep the deployment story consistent (in production the SPA is served as static files behind a reverse proxy, so CORS rules apply there too)."*

**"What would you do differently next time?"**
- *"Wrap checkout in `@Transactional` from the start. The current happy path works, but the failure mode (run out of stock on item 3 of 5) leaves the first 2 stocks decremented and no order created. Easy fix, but it should not have been a 'later' item."*
