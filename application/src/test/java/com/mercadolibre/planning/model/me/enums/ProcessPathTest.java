package com.mercadolibre.planning.model.me.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProcessPathTest {

    @ParameterizedTest
    @EnumSource(value = ProcessPath.class)
    public void testGetProcessPathOk(final ProcessPath processPath) {
        assertFalse(processPath.getName().isEmpty());
    }

}
