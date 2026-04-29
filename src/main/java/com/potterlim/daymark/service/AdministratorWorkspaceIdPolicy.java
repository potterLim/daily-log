package com.potterlim.daymark.service;

import java.util.List;
import com.potterlim.daymark.config.DaymarkApplicationProperties;
import org.springframework.stereotype.Component;

@Component
public class AdministratorWorkspaceIdPolicy {

    private final DaymarkApplicationProperties mDaymarkApplicationProperties;

    public AdministratorWorkspaceIdPolicy(DaymarkApplicationProperties daymarkApplicationProperties) {
        mDaymarkApplicationProperties = daymarkApplicationProperties;
    }

    public boolean isAdministratorWorkspaceId(String workspaceIdOrNull) {
        if (workspaceIdOrNull == null || workspaceIdOrNull.isBlank()) {
            return false;
        }

        String normalizedWorkspaceId = workspaceIdOrNull.trim();
        for (String administratorWorkspaceId : listAdministratorWorkspaceIds()) {
            if (administratorWorkspaceId.equalsIgnoreCase(normalizedWorkspaceId)) {
                return true;
            }
        }

        return false;
    }

    public List<String> listAdministratorWorkspaceIds() {
        return mDaymarkApplicationProperties.getOperations().getAdministratorWorkspaceIds()
            .stream()
            .filter(administratorWorkspaceId -> administratorWorkspaceId != null && !administratorWorkspaceId.isBlank())
            .map(String::trim)
            .distinct()
            .toList();
    }
}
