package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

/**
 * Integration tests for GET /warehouse/search endpoint.
 *
 * Covers filtering by location, capacity range, sorting, pagination,
 * and exclusion of archived warehouses.
 */
@QuarkusTest
public class WarehouseSearchTest {

  @Inject
  EntityManager em;

  @BeforeEach
  @Transactional
  public void setup() {
    em.createQuery("DELETE FROM DbWarehouse").executeUpdate();

    // Active warehouses
    createWarehouse("SEARCH-001", "AMSTERDAM-001", 80, 20);
    createWarehouse("SEARCH-002", "AMSTERDAM-001", 50, 10);
    createWarehouse("SEARCH-003", "ZWOLLE-001",    30, 5);
    createWarehouse("SEARCH-004", "TILBURG-001",   40, 15);

    // Archived — must never appear in search results
    DbWarehouse archived = createWarehouse("SEARCH-ARCHIVED", "AMSTERDAM-001", 60, 10);
    archived.archivedAt = LocalDateTime.now();
  }

  @Test
  public void testSearchReturnsOnlyActiveWarehouses() {
    given()
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("size()", is(4))
        .body("businessUnitCode", not(hasItem("SEARCH-ARCHIVED")));
  }

  @Test
  public void testSearchFilterByLocation() {
    given()
        .queryParam("location", "AMSTERDAM-001")
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("size()", is(2))
        .body("businessUnitCode", hasItems("SEARCH-001", "SEARCH-002"));
  }

  @Test
  public void testSearchFilterByMinCapacity() {
    given()
        .queryParam("minCapacity", 50)
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("size()", is(2))
        .body("businessUnitCode", hasItems("SEARCH-001", "SEARCH-002"));
  }

  @Test
  public void testSearchFilterByCapacityRange() {
    given()
        .queryParam("minCapacity", 30)
        .queryParam("maxCapacity", 50)
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("size()", is(3))
        .body("businessUnitCode", hasItems("SEARCH-002", "SEARCH-003", "SEARCH-004"));
  }

  @Test
  public void testSearchSortByCapacityAsc() {
    given()
        .queryParam("sortBy", "capacity")
        .queryParam("sortOrder", "asc")
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("capacity[0]", is(30))
        .body("capacity[3]", is(80));
  }

  @Test
  public void testSearchSortByCapacityDesc() {
    given()
        .queryParam("sortBy", "capacity")
        .queryParam("sortOrder", "desc")
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("capacity[0]", is(80))
        .body("capacity[3]", is(30));
  }

  @Test
  public void testSearchPagination() {
    given()
        .queryParam("sortBy", "capacity")
        .queryParam("sortOrder", "asc")
        .queryParam("page", 0)
        .queryParam("pageSize", 2)
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("size()", is(2))
        .body("capacity[0]", is(30));

    given()
        .queryParam("sortBy", "capacity")
        .queryParam("sortOrder", "asc")
        .queryParam("page", 1)
        .queryParam("pageSize", 2)
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("size()", is(2))
        .body("capacity[0]", is(50));
  }

  @Test
  public void testSearchWithNoMatchingFiltersReturnsEmpty() {
    given()
        .queryParam("location", "EINDHOVEN-001")
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("size()", is(0));
  }

  // ---- helper ----

  @Transactional
  DbWarehouse createWarehouse(String code, String location, int capacity, int stock) {
    DbWarehouse w = new DbWarehouse();
    w.businessUnitCode = code;
    w.location = location;
    w.capacity = capacity;
    w.stock = stock;
    w.createdAt = LocalDateTime.now();
    em.persist(w);
    em.flush();
    return w;
  }
}
