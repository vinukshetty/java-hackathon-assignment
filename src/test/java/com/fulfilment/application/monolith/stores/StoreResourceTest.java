package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the Store REST API.
 *
 * Covers get, getSingle, create, update, patch, and delete operations
 * including validation and 404 error scenarios.
 */
@QuarkusTest
public class StoreResourceTest {

  // ---- GET all ----

  @Test
  public void testGetAllStoresReturnsOk() {
    given()
        .when().get("/store")
        .then()
        .statusCode(200)
        .body("$", instanceOf(java.util.List.class));
  }

  // ---- GET single ----

  @Test
  public void testGetSingleStore_NotFound() {
    given()
        .when().get("/store/99999")
        .then()
        .statusCode(404);
  }

  // ---- CREATE ----

  @Test
  public void testCreateStoreSuccess() {
    String uniqueName = "StoreResourceTest_" + System.nanoTime();
    String body = "{\"name\": \"" + uniqueName + "\", \"quantityProductsInStock\": 10}";

    int storeId = given()
        .contentType("application/json")
        .body(body)
        .when().post("/store")
        .then()
        .statusCode(201)
        .body("name", is(uniqueName))
        .extract().path("id");

    // Verify get single
    given()
        .when().get("/store/" + storeId)
        .then()
        .statusCode(200)
        .body("name", is(uniqueName));
  }

  @Test
  public void testCreateStore_WithIdSetReturns422() {
    String body = "{\"id\": 999, \"name\": \"INVALID\"}";

    given()
        .contentType("application/json")
        .body(body)
        .when().post("/store")
        .then()
        .statusCode(422);
  }

  // ---- UPDATE (PUT) ----

  @Test
  public void testUpdateStoreSuccess() {
    String uniqueName = "StoreUpdate_" + System.nanoTime();
    String createBody = "{\"name\": \"" + uniqueName + "\", \"quantityProductsInStock\": 5}";

    int storeId = given()
        .contentType("application/json")
        .body(createBody)
        .when().post("/store")
        .then()
        .statusCode(201)
        .extract().path("id");

    String updatedName = uniqueName + "_updated";
    String updateBody = "{\"name\": \"" + updatedName + "\", \"quantityProductsInStock\": 99}";

    given()
        .contentType("application/json")
        .body(updateBody)
        .when().put("/store/" + storeId)
        .then()
        .statusCode(200)
        .body("name", is(updatedName))
        .body("quantityProductsInStock", is(99));
  }

  @Test
  public void testUpdateStore_MissingNameReturns422() {
    String body = "{\"quantityProductsInStock\": 10}";

    given()
        .contentType("application/json")
        .body(body)
        .when().put("/store/1")
        .then()
        .statusCode(422);
  }

  @Test
  public void testUpdateStore_NotFoundReturns404() {
    String body = "{\"name\": \"Ghost Store\", \"quantityProductsInStock\": 0}";

    given()
        .contentType("application/json")
        .body(body)
        .when().put("/store/99999")
        .then()
        .statusCode(404);
  }

  // ---- PATCH ----

  @Test
  public void testPatchStoreSuccess() {
    String uniqueName = "StorePatch_" + System.nanoTime();
    String createBody = "{\"name\": \"" + uniqueName + "\", \"quantityProductsInStock\": 5}";

    int storeId = given()
        .contentType("application/json")
        .body(createBody)
        .when().post("/store")
        .then()
        .statusCode(201)
        .extract().path("id");

    String patchedName = uniqueName + "_patched";
    String patchBody = "{\"name\": \"" + patchedName + "\", \"quantityProductsInStock\": 77}";

    given()
        .contentType("application/json")
        .body(patchBody)
        .when().patch("/store/" + storeId)
        .then()
        .statusCode(200)
        .body("name", is(patchedName));
  }

  @Test
  public void testPatchStore_MissingNameReturns422() {
    String body = "{\"quantityProductsInStock\": 10}";

    given()
        .contentType("application/json")
        .body(body)
        .when().patch("/store/1")
        .then()
        .statusCode(422);
  }

  @Test
  public void testPatchStore_NotFoundReturns404() {
    String body = "{\"name\": \"Ghost Store\"}";

    given()
        .contentType("application/json")
        .body(body)
        .when().patch("/store/99999")
        .then()
        .statusCode(404);
  }

  // ---- DELETE ----

  @Test
  public void testDeleteStoreSuccess() {
    String uniqueName = "StoreDelete_" + System.nanoTime();
    String createBody = "{\"name\": \"" + uniqueName + "\", \"quantityProductsInStock\": 3}";

    int storeId = given()
        .contentType("application/json")
        .body(createBody)
        .when().post("/store")
        .then()
        .statusCode(201)
        .extract().path("id");

    given()
        .when().delete("/store/" + storeId)
        .then()
        .statusCode(204);

    given()
        .when().get("/store/" + storeId)
        .then()
        .statusCode(404);
  }

  @Test
  public void testDeleteStore_NotFoundReturns404() {
    given()
        .when().delete("/store/99999")
        .then()
        .statusCode(404);
  }
}
