package com.ineos.oxide.base.security.ui;

import com.ineos.oxide.base.security.model.entities.ApplicationRole;

public interface RoleDialogListener {
    void onRoleDialogClosed(ApplicationRole item, RoleDialog.Mode mode, boolean success);
}
