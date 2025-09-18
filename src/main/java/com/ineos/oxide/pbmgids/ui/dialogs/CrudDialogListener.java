package com.ineos.oxide.pbmgids.ui.dialogs;

public interface CrudDialogListener<T> {
    void onDialogClosed(T item, CrudMode mode, boolean success);
}
