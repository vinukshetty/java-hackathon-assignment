package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import java.util.List;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("warehouse")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface WarehouseResource {

    @GET
    List<Warehouse> listAllWarehousesUnits();

    @POST
    Warehouse createANewWarehouseUnit(@NotNull Warehouse data);

    @GET
    @Path("{id}")
    Warehouse getAWarehouseUnitByID(@PathParam("id") String id);

    @DELETE
    @Path("{id}")
    void archiveAWarehouseUnitByID(@PathParam("id") String id);

    @POST
    @Path("{businessUnitCode}/replacement")
    Warehouse replaceTheCurrentActiveWarehouse(
            @PathParam("businessUnitCode") String businessUnitCode,
            @NotNull Warehouse data);

    @GET
    @Path("search")
    List<Warehouse> searchWarehouses(
            @QueryParam("location") String location,
            @QueryParam("minCapacity") Integer minCapacity,
            @QueryParam("maxCapacity") Integer maxCapacity,
            @QueryParam("sortBy") @DefaultValue("createdAt") String sortBy,
            @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("10") int pageSize);
}
