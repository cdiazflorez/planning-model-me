package com.mercadolibre.planning.model.me.controller.editor;

import com.mercadolibre.planning.model.me.enums.ProcessName;

import java.beans.PropertyEditorSupport;

public class ProcessNameEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(final String text) {
        setValue(ProcessName.from(text));
    }
}
