package com.potterlim.daylog.service;

import java.util.Optional;
import com.potterlim.daylog.dto.auth.RegisterUserAccountCommand;
import com.potterlim.daylog.entity.UserAccount;

public interface IUserAccountService {

    /**
     * Registers a new user account.
     *
     * <p>Preconditions: the command must contain a non-blank user name and raw password. The user
     * name must not already exist.</p>
     *
     * @return The persisted user account.
     */
    UserAccount registerUserAccount(RegisterUserAccountCommand registerUserAccountCommand);

    /**
     * Finds a user account by user name.
     *
     * <p>Preconditions: the user name may be null or blank. In that case, the method returns an
     * empty result instead of querying the database.</p>
     *
     * @return The matching user account when it exists, otherwise an empty result.
     */
    Optional<UserAccount> findUserAccountByUserName(String userNameOrNull);
}
