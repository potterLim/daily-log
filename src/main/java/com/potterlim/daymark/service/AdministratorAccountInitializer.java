package com.potterlim.daymark.service;

import com.potterlim.daymark.entity.UserAccount;
import com.potterlim.daymark.repository.IUserAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdministratorAccountInitializer implements ApplicationRunner {

    private final AdministratorWorkspaceIdPolicy mAdministratorWorkspaceIdPolicy;
    private final IUserAccountRepository mUserAccountRepository;

    public AdministratorAccountInitializer(
        AdministratorWorkspaceIdPolicy administratorWorkspaceIdPolicy,
        IUserAccountRepository userAccountRepository
    ) {
        mAdministratorWorkspaceIdPolicy = administratorWorkspaceIdPolicy;
        mUserAccountRepository = userAccountRepository;
    }

    @Override
    public void run(ApplicationArguments applicationArguments) {
        promoteConfiguredAdministratorAccounts();
    }

    @Transactional
    public void promoteConfiguredAdministratorAccounts() {
        for (String administratorWorkspaceId : mAdministratorWorkspaceIdPolicy.listAdministratorWorkspaceIds()) {
            mUserAccountRepository.findByUserNameIgnoringCase(administratorWorkspaceId)
                .filter(userAccount -> !userAccount.isAdministrator())
                .ifPresent(this::grantAdministratorRole);
        }
    }

    private void grantAdministratorRole(UserAccount userAccount) {
        userAccount.grantAdministratorRole();
        mUserAccountRepository.save(userAccount);
    }
}
