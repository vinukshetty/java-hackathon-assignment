# Questions

Here are 2 questions related to the codebase. There's no right or wrong answer - we want to understand your reasoning.

## Question 1: API Specification Approaches

When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded everything directly.

What are your thoughts on the pros and cons of each approach? Which would you choose and why?

**Answer:**
```txt
Spec-first (OpenAPI YAML → generated code):

Pros:
- The YAML is the single source of truth for the contract. Consumers (frontend,
  other services) can read or mock from it before any implementation exists.
- Generated stubs are always consistent with the spec; no accidental drift between
  documentation and actual behaviour.
- Tooling is free: Swagger UI, mock servers, contract-testing frameworks all
  consume the same file.
- Easier to version and review API changes as a diff on the YAML, separate from
  implementation PRs.

Cons:
- An extra build step and a layer of generated code that is harder to debug or
  customise (touching generated files is risky).
- The mapping between generated API beans and domain models adds boilerplate
  (see WarehouseResourceImpl.toWarehouseResponse).
- Complex response structures (streaming, multipart) can be awkward to express
  cleanly in OpenAPI.

Hand-coded (Product / Store):
Pros:
- Simpler: fewer moving parts, no generation step, full control over the code.
- Faster to iterate on during early development.

Cons:
- The documentation (if any) lives separately and drifts easily.
- No machine-readable contract for consumers; breaking changes are harder to catch.

My choice: spec-first for any API that crosses a team or service boundary.
The upfront cost of maintaining the YAML pays off through better collaboration,
automated contract testing, and a clear changelog. For purely internal, rapidly
changing endpoints I would start hand-coded and migrate to spec-first once the
contract stabilises.
```

---

## Question 2: Testing Strategy

Given the need to balance thorough testing with time and resource constraints, how would you prioritize tests for this project?

Which types of tests (unit, integration, parameterized, etc.) would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Priority order (highest value per unit of effort):

1. Integration tests for critical paths — transaction boundaries and side-effect
   integrity (e.g. StoreTransactionIntegrationTest). These catch the bugs that
   matter most in production and are hard to find any other way.

2. Parameterized use-case tests for validation rules — a single @ParameterizedTest
   with a data provider (like WarehouseValidationTest) covers many edge cases
   cheaply and documents all the business rules in one place.

3. Concurrency / optimistic-locking tests — race conditions are notoriously hard
   to reproduce manually. Dedicated tests (ArchiveWarehouseUseCaseTest concurrent
   scenario, WarehouseConcurrencyIT) give confidence that the locking strategy
   works under load.

4. Repository integration tests against a real database — tests like
   WarehouseTestcontainersIT verify query correctness, constraint enforcement, and
   transaction rollback behaviour that an in-memory mock cannot simulate.

5. End-to-end REST tests (RestAssured) — useful for smoke-testing the full stack
   (routing, serialisation, status codes), but kept slim because they are slow and
   their failures often duplicate what lower-level tests already cover.

Ensuring coverage over time:
- Enforce a "test alongside the code" rule in PR reviews: every new use case or
  business rule must ship with at least one parameterized scenario and one
  transaction/concurrency case where relevant.
- Use mutation testing (e.g. PIT) periodically to detect tests that pass even
  when the logic is broken — this surfaces gaps that line-coverage metrics miss.
- Keep the integration test suite fast by scoping @BeforeEach cleanups tightly
  and sharing a single Quarkus test instance across the test run (the default
  Quarkus behaviour) rather than restarting per class.
- Tag slow concurrency tests so they run in CI on every merge but not on every
  local build, reducing friction while keeping the signal.
```
