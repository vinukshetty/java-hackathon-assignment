package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Create Warehouse use case.
 *
 * Covers successful creation, duplicate code rejection,
 * invalid location, and capacity/stock validations.
 */
@QuarkusTest
public class CreateWarehouseUseCaseTest {

  @Inject
  WarehouseRepository warehouseRepository;

  @Inject
  LocationGateway locationResolver;

  @Inject
  EntityManager em;

  private CreateWarehouseUseCase createWarehouseUseCase;

  @BeforeEach
  @Transactional
  public void setup() {
    em.createQuery("DELETE FROM DbWarehouse").executeUpdate();
    createWarehouseUseCase = new CreateWarehouseUseCase(warehouseRepository, locationResolver);
  }

  @Test
  @Transactional
  public void testCreateWarehouseSuccessfully() {
    Warehouse warehouse = buildWarehouse("CREATE-001", "AMSTERDAM-001", 80, 30);

    createWarehouseUseCase.create(warehouse);

    Warehouse saved = warehouseRepository.findByBusinessUnitCode("CREATE-001");
    assertNotNull(saved);
    assertEquals("AMSTERDAM-001", saved.location);
    assertEquals(80, saved.capacity);
    assertEquals(30, saved.stock);
  }

  @Test
  @Transactional
  public void testCreatedAtIsSetOnCreate() {
    Warehouse warehouse = buildWarehouse("CREATE-TS-001", "AMSTERDAM-001", 50, 10);

    createWarehouseUseCase.create(warehouse);

    assertNotNull(warehouse.createdAt, "createdAt should be set after creation");
  }

  @Test
  @Transactional
  public void testCannotCreateWithDuplicateBusinessUnitCode() {
    Warehouse first = buildWarehouse("CREATE-DUP-001", "AMSTERDAM-001", 60, 20);
    createWarehouseUseCase.create(first);

    Warehouse duplicate = buildWarehouse("CREATE-DUP-001", "ZWOLLE-001", 30, 10);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> createWarehouseUseCase.create(duplicate));

    assertTrue(ex.getMessage().contains("already exists"));
  }

  @Test
  @Transactional
  public void testCannotCreateWithInvalidLocation() {
    Warehouse warehouse = buildWarehouse("CREATE-LOC-001", "INVALID-LOCATION", 50, 10);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> createWarehouseUseCase.create(warehouse));

    assertTrue(ex.getMessage().contains("not valid"));
  }

  @Test
  @Transactional
  public void testCannotCreateWhenCapacityExceedsLocationMax() {
    // AMSTERDAM-001 max capacity is 100
    Warehouse warehouse = buildWarehouse("CREATE-CAP-001", "AMSTERDAM-001", 150, 50);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> createWarehouseUseCase.create(warehouse));

    assertTrue(ex.getMessage().contains("exceeds location max capacity"));
  }

  @Test
  @Transactional
  public void testCannotCreateWhenStockExceedsCapacity() {
    // ZWOLLE-001 max capacity is 40; capacity=30, stock=40 → stock > capacity
    Warehouse warehouse = buildWarehouse("CREATE-STK-001", "ZWOLLE-001", 30, 40);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> createWarehouseUseCase.create(warehouse));

    assertTrue(ex.getMessage().contains("exceeds warehouse capacity"));
  }

  @Test
  @Transactional
  public void testCreateWarehouseWithZeroStock() {
    Warehouse warehouse = buildWarehouse("CREATE-ZERO-001", "TILBURG-001", 40, 0);

    createWarehouseUseCase.create(warehouse);

    Warehouse saved = warehouseRepository.findByBusinessUnitCode("CREATE-ZERO-001");
    assertNotNull(saved);
    assertEquals(0, saved.stock);
  }

  @Test
  @Transactional
  public void testCreateWarehouseAtExactLocationMaxCapacity() {
    // HELMOND-001 max capacity is 45
    Warehouse warehouse = buildWarehouse("CREATE-EXACT-001", "HELMOND-001", 45, 0);

    createWarehouseUseCase.create(warehouse);

    Warehouse saved = warehouseRepository.findByBusinessUnitCode("CREATE-EXACT-001");
    assertNotNull(saved);
    assertEquals(45, saved.capacity);
  }

  // ---- helper ----

  private Warehouse buildWarehouse(String code, String location, int capacity, int stock) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = code;
    w.location = location;
    w.capacity = capacity;
    w.stock = stock;
    return w;
  }
}
