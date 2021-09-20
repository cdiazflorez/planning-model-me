package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mercadolibre.planning.model.me.gateways.planningmodel.ProcessVisitor;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProcessName {

    PICKING(1, true, false) {
        @Override
        public <T, U> U accept(ProcessVisitor<T, U> visitor, T input) {
            return visitor.visitPicking(input);
        }
    },
    PACKING(2, false, false) {
        @Override
        public <T, U> U accept(ProcessVisitor<T, U> visitor, T input) {
            return visitor.visitPacking(input);
        }
    },
    PACKING_WALL(3, false, false) {
        @Override
        public <T, U> U accept(ProcessVisitor<T, U> visitor, T input) {
            return visitor.visitPackingWall(input);
        }
    },
    WAVING(null, false, true) {
        @Override
        public <T, U> U accept(ProcessVisitor<T, U> visitor, T input) {
            return visitor.visitWaving(input);
        }
    },
    BATCH_SORTER(null, false, false) {
        @Override
        public <T, U> U accept(ProcessVisitor<T, U> visitor, T input) {
            return visitor.visitBatchSorter(input);
        }
    },
    WALL_IN(null, false, false) {
        @Override
        public <T, U> U accept(ProcessVisitor<T, U> visitor, T input) {
            return visitor.visitWallIn(input);
        }
    },
    GLOBAL(null, false, false) {
        @Override
        public <T, U> U accept(ProcessVisitor<T, U> visitor, T input) {
            return visitor.visitGlobal(input);
        }
    };

    private final Integer index;

    private final boolean hasAreas;

    private final boolean hasTargetBacklog;

    public abstract <T, U> U accept(ProcessVisitor<T, U> visitor, T input);

    @JsonCreator
    public static ProcessName from(final String value) {
        return valueOf(value.toUpperCase());
    }

    @JsonValue
    public String getName() {
        return name().toLowerCase();
    }

    public boolean hasAreas() {
        return hasAreas;
    }

    public boolean hasTargetBacklog() {
        return hasTargetBacklog;
    }

}
