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
 * Integration tests for the Warehouse REST API endpoints.
 *
 * Covers list, create, getById, replace, and archive operations,
 * including validation error scenarios (400/404).
 */
@QuarkusTest
public class WarehouseResourceTest {

  @Inject
  EntityManager em;

  @BeforeEach
  @Transactional
  public void setup() {
    em.createQuery("DELETE FROM DbWarehouse").executeUpdate();

    // Seed a couple of active warehouses
    createDb("WH-RESOURCE-001", "AMSTERDAM-001", 80, 20);
    createDb("WH-RESOURCE-002", "ZWOLLE-001", 30, 10);

    // Seed one archived warehouse
    DbWarehouse archived = createDb("WH-RESOURCE-ARC", "TILBURG-001", 40, 5);
    archived.archivedAt = LocalDateTime.now();
  }

  // ---- LIST ----

  @Test
  public void testListAllReturnsOnlyActiveWarehouses() {
    given()
        .when().get("/warehouse")
        .then()
        .statusCode(200)
        .body("size()", is(2))
        .body("businessUnitCode", hasItems("WH-RESOURCE-001", "WH-RESOURCE-002"))
        .body("businessUnitCode", not(hasItem("WH-RESOURCE-ARC")));
  }

  // ---- CREATE ----

  @Test
  public void testCreateWarehouseSuccess() {
    String body = """
        {
          "businessUnitCode": "WH-NEW-001",
          "location": "AMSTERDAM-001",
          "capacity": 60,
          "stock": 10
        }
        """;

    given()
        .contentType("application/json")
        .body(body)
        .when().post("/warehouse")
        .then()
        .statusCode(200)
        .body("businessUnitCode", is("WH-NEW-001"))
        .body("location", is("AMSTERDAM-001"))
        .body("capacity", is(60))
        .body("stock", is(10));
  }

  @Test
  public void testCreateWarehouse_DuplicateCodeReturns400() {
    String body = """
        {
          "businessUnitCode": "WH-RESOURCE-001",
          "location": "AMSTERDAM-001",
          "capacity": 50,
          "stock": 5
        }
        """;

    given()
        .contentType("application/json")
        .body(body)
        .when().post("/warehouse")
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateWarehouse_InvalidLocationReturns400() {
    String body = """
        {
          "businessUnitCode": "WH-INVALID-LOC",
          "location": "NOWHERE-001",
          "capacity": 50,
          "stock": 5
        }
        """;

    given()
        .contentType("application/json")
        .body(body)
        .when().post("/warehouse")
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateWarehouse_CapacityExceedsLocationMaxReturns400() {
    // ZWOLLE-001 max capacity is 40
    String body = """
        {
          "businessUnitCode": "WH-CAP-EXCEED",
          "location": "ZWOLLE-001",
          "capacity": 100,
          "stock": 5
        }
        """;

    given()
        .contentType("application/json")
        .body(body)
        .when().post("/warehouse")
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateWarehouse_StockExceedsCapacityReturns400() {
    String body = """
        {
          "businessUnitCode": "WH-STK-EXCEED",
          "location": "AMSTERDAM-001",
          "capacity": 20,
          "stock": 50
        }
        """;

    given()
        .contentType("application/json")
        .body(body)
        .when().post("/warehouse")
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateWarehouse_NullStockDefaultsToZero() {
    String body = """
        {
          "businessUnitCode": "WH-NO-STOCK",
          "location": "AMSTERDAM-001",
          "capacity": 50
        }
        """;

    given()
        .contentType("application/json")
        .body(body)
        .when().post("/warehouse")
        .then()
        .statusCode(200)
        .body("stock", is(0));
  }

  // ---- GET BY ID ----

  @Test
  public void testGetWarehouseById_Found() {
    given()
        .when().get("/warehouse/WH-RESOURCE-001")
        .then()
        .statusCode(200)
        .body("businessUnitCode", is("WH-RESOURCE-001"))
        .body("location", is("AMSTERDAM-001"))
        .body("capacity", is(80));
  }

  @Test
  public void testGetWarehouseById_NotFound() {
    given()
        .when().get("/warehouse/DOES-NOT-EXIST")
        .then()
        .statusCode(404);
  }

  // ---- REPLACE ----

  @Test
  public void testReplaceWarehouseSuccess() {
    String body = """
        {
          "location": "ZWOLLE-001",
          "capacity": 30,
          "stock": 5
        }
        """;

    given()
        .contentType("application/json")
        .body(body)
        .when().post("/warehouse/WH-RESOURCE-001/replacement")
        .then()
        .statusCode(200)
        .body("businessUnitCode", is("WH-RESOURCE-001"))
        .body("location", is("ZWOLLE-001"))
        .body("capacity", is(30))
        .body("stock", is(5));
  }

  @Test
  public void testReplaceWarehouse_NonExistentReturns400() {
    String body = """
        {
          "location": "AMSTERDAM-001",
          "capacity": 50,
          "stock": 10
        }
        """;

    given()
        .contentType("application/json")
        .body(body)
        .when().post("/warehouse/NON-EXISTENT/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  public void testReplaceWarehouse_ArchivedReturns400() {
    String body = """
        {
          "location": "AMSTERDAM-001",
          "capacity": 50,
          "stock": 10
        }
        """;

    given()
        .contentType("application/json")
        .body(body)
        .when().post("/warehouse/WH-RESOURCE-ARC/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  public void testReplaceWarehouse_InvalidLocationReturns400() {
    String body = """
        {
          "location": "INVALID-LOC",
          "capacity": 50,
          "stock": 10
        }
        """;

    given()
        .contentType("application/json")
        .body(body)
        .when().post("/warehouse/WH-RESOURCE-001/replacement")
        .then()
        .statusCode(400);
  }

  // ---- ARCHIVE ----

  @Test
  public void testArchiveWarehouseSuccess() {
    given()
        .when().delete("/warehouse/WH-RESOURCE-002")
        .then()
        .statusCode(204);

    // Verify it no longer appears in the list
    given()
        .when().get("/warehouse")
        .then()
        .statusCode(200)
        .body("businessUnitCode", not(hasItem("WH-RESOURCE-002")));
  }

  @Test
  public void testArchiveWarehouse_NotFoundReturns404() {
    given()
        .when().delete("/warehouse/DOES-NOT-EXIST")
        .then()
        .statusCode(404);
  }

  @Test
  public void testArchiveWarehouse_AlreadyArchivedReturns400() {
    given()
        .when().delete("/warehouse/WH-RESOURCE-ARC")
        .then()
        .statusCode(400);
  }

  // ---- helper ----

  @Transactional
  DbWarehouse createDb(String code, String location, int capacity, int stock) {
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
