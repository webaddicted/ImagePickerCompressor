package com.example.deepaksharma.webaddicated.permission.Permission;

import java.util.List;

public interface PermissionListener {
    void onPermissionGranted(List<String> mCustomPermission);
    void onPermissionDenied(List<String> mCustomPermission);
}
