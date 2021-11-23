package com.mercadolibre.planning.model.me.gateways.planningmodel;

@Deprecated
public interface ProcessVisitor<T, U> {
    U visitWaving(T input);

    U visitPicking(T input);

    U visitBatchSorter(T input);

    U visitWallIn(T input);

    U visitPacking(T input);

    U visitPackingWall(T input);

    U visitGlobal(T input);
}
