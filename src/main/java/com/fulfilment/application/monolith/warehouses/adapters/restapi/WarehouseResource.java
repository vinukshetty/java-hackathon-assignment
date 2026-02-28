package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import java.util.List;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;

import jakarta.validation.constraints.NotNull;

public interface WarehouseResource {

    List<Warehouse> listAllWarehousesUnits();

    Warehouse createANewWarehouseUnit(@NotNull Warehouse data);

    Warehouse getAWarehouseUnitByID(String id);

    void archiveAWarehouseUnitByID(String id);

    Warehouse replaceTheCurrentActiveWarehouse(String businessUnitCode, @NotNull Warehouse data);

}
