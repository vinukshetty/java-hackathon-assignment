package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the Product REST API.
 *
 * Each test creates its own data to avoid ordering dependencies
 * with other test classes that modify seeded products.
 */
@QuarkusTest
public class ProductResourceTest {

  // ---- GET all ----

  @Test
  public void testGetAllProductsReturnsOkList() {
    given()
        .when().get("/product")
        .then()
        .statusCode(200)
        .body("$", instanceOf(java.util.List.class));
  }

  // ---- GET single ----

  @Test
  public void testGetSingleProductById_Found() {
    // Create a product first so we control its existence
    String name = "GET-SINGLE-" + System.nanoTime();
    int id = given()
        .contentType("application/json")
        .body("{\"name\": \"" + name + "\", \"stock\": 1}")
        .when().post("/product")
        .then()
        .statusCode(201)
        .extract().path("id");

    given()
        .when().get("/product/" + id)
        .then()
        .statusCode(200)
        .body("name", is(name));
  }

  @Test
  public void testGetSingleProductById_NotFound() {
    given()
        .when().get("/product/99999")
        .then()
        .statusCode(404);
  }

  // ---- CREATE ----

  @Test
  public void testCreateProductSuccess() {
    String name = "CREATE-PROD-" + System.nanoTime();
    String body = "{\"name\": \"" + name + "\", \"description\": \"A test product\", \"price\": 9.99, \"stock\": 5}";

    given()
        .contentType("application/json")
        .body(body)
        .when().post("/product")
        .then()
        .statusCode(201)
        .body("name", is(name))
        .body("stock", is(5));
  }

  @Test
  public void testCreateProduct_WithIdSetReturns422() {
    String body = "{\"id\": 999, \"name\": \"INVALID-PRODUCT\"}";

    given()
        .contentType("application/json")
        .body(body)
        .when().post("/product")
        .then()
        .statusCode(422);
  }

  // ---- UPDATE ----

  @Test
  public void testUpdateProductSuccess() {
    // Create a product to update
    String name = "UPDATE-PROD-" + System.nanoTime();
    int id = given()
        .contentType("application/json")
        .body("{\"name\": \"" + name + "\", \"stock\": 3}")
        .when().post("/product")
        .then()
        .statusCode(201)
        .extract().path("id");

    String updatedName = name + "-UPDATED";
    String updateBody = "{\"name\": \"" + updatedName + "\", \"stock\": 20}";

    given()
        .contentType("application/json")
        .body(updateBody)
        .when().put("/product/" + id)
        .then()
        .statusCode(200)
        .body("name", is(updatedName))
        .body("stock", is(20));
  }

  @Test
  public void testUpdateProduct_MissingNameReturns422() {
    String body = "{\"description\": \"No name provided\", \"price\": 10.00}";

    given()
        .contentType("application/json")
        .body(body)
        .when().put("/product/2")
        .then()
        .statusCode(422);
  }

  @Test
  public void testUpdateProduct_NotFoundReturns404() {
    String body = "{\"name\": \"GHOST-PRODUCT\", \"stock\": 0}";

    given()
        .contentType("application/json")
        .body(body)
        .when().put("/product/99999")
        .then()
        .statusCode(404);
  }

  // ---- DELETE ----

  @Test
  public void testDeleteProductSuccess() {
    // Create a product, then delete it
    String name = "DELETE-PROD-" + System.nanoTime();
    int id = given()
        .contentType("application/json")
        .body("{\"name\": \"" + name + "\", \"stock\": 0}")
        .when().post("/product")
        .then()
        .statusCode(201)
        .extract().path("id");

    given()
        .when().delete("/product/" + id)
        .then()
        .statusCode(204);

    given()
        .when().get("/product/" + id)
        .then()
        .statusCode(404);
  }

  @Test
  public void testDeleteProduct_NotFoundReturns404() {
    given()
        .when().delete("/product/99999")
        .then()
        .statusCode(404);
  }
}
