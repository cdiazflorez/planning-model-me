package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mercadolibre.planning.model.me.gateways.planningmodel.ProcessVisitor;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProcessName {

    PICKING(1) {
        @Override
        public <T, U> U accept(ProcessVisitor<T, U> visitor, T input) {
            return visitor.visitPicking(input);
        }
    },
    PACKING(2) {
        @Override
        public <T, U> U accept(ProcessVisitor<T, U> visitor, T input) {
            return visitor.visitPacking(input);
        }
    },
    PACKING_WALL(3) {
        @Override
        public <T, U> U accept(ProcessVisitor<T, U> visitor, T input) {
            return visitor.visitPackingWall(input);
        }
    },
    WAVING(null) {
        @Override
        public <T, U> U accept(ProcessVisitor<T, U> visitor, T input) {
            return visitor.visitWaving(input);
        }
    },
    BATCH_SORTER(null) {
        @Override
        public <T, U> U accept(ProcessVisitor<T, U> visitor, T input) {
            return visitor.visitBatchSorter(input);
        }
    },
    WALL_IN(null) {
        @Override
        public <T, U> U accept(ProcessVisitor<T, U> visitor, T input) {
            return visitor.visitWallIn(input);
        }
    },
    GLOBAL(null) {
        @Override
        public <T, U> U accept(ProcessVisitor<T, U> visitor, T input) {
            return visitor.visitGlobal(input);
        }
    };

    private final Integer index;

    public abstract <T, U> U accept(ProcessVisitor<T, U> visitor, T input);

    @JsonCreator
    public static ProcessName from(final String value) {
        return valueOf(value.toUpperCase());
    }

    @JsonValue
    public String getName() {
        return name().toLowerCase();
    }

}
