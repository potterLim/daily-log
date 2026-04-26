package com.potterlim.daymark.service;

import java.util.Optional;
import com.potterlim.daymark.dto.auth.RegisterUserAccountCommand;
import com.potterlim.daymark.entity.UserAccount;
import com.potterlim.daymark.entity.UserAccountId;

public interface IUserAccountService {

    /**
     * Registers a new user account.
     *
     * <p>Preconditions: the command must contain a non-blank user name, email address, and raw
     * password. The user name and email address must not already exist.</p>
     *
     * @return The persisted user account.
     */
    UserAccount registerUserAccount(RegisterUserAccountCommand registerUserAccountCommand);

    /**
     * Finds a user account by login identifier.
     *
     * <p>Preconditions: the login identifier may be null or blank. In that case, the method returns an
     * empty result instead of querying the database.</p>
     *
     * @return The matching user account when it exists, otherwise an empty result.
     */
    Optional<UserAccount> findUserAccountByLoginIdentifier(String loginIdentifierOrNull);

    /**
     * Finds a user account by email address.
     *
     * <p>Preconditions: the email address may be null or blank. In that case, the method returns
     * an empty result instead of querying the database.</p>
     *
     * @return The matching user account when it exists, otherwise an empty result.
     */
    Optional<UserAccount> findUserAccountByEmailAddress(String emailAddressOrNull);

    /**
     * Changes the password for the authenticated user after verifying the current password.
     *
     * <p>Preconditions: the user account id, current password, and new password must all be
     * non-null and non-blank.</p>
     */
    void changePassword(UserAccountId userAccountId, String currentRawPassword, String newRawPassword);

    /**
     * Replaces the password for the given user account without requiring the current password.
     *
     * <p>Preconditions: the user account id and new password must be non-null and non-blank.</p>
     */
    void resetPassword(UserAccountId userAccountId, String newRawPassword);
}
