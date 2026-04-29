package com.potterlim.daymark.service;

import java.util.Optional;
import com.potterlim.daymark.dto.auth.RegisterGoogleUserAccountCommand;
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
     * Registers a new user account after Google has confirmed the email identity.
     *
     * <p>Preconditions: the command must contain a non-blank user name, email address, Google subject,
     * and raw password. The user name, email address, and Google subject must not already exist.</p>
     *
     * @return The persisted Google-connected user account.
     */
    UserAccount registerGoogleUserAccount(RegisterGoogleUserAccountCommand registerGoogleUserAccountCommand);

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
     * Finds a user account by Google subject.
     *
     * <p>Preconditions: the Google subject may be null or blank. In that case, the method returns an
     * empty result instead of querying the database.</p>
     *
     * @return The matching user account when it exists, otherwise an empty result.
     */
    Optional<UserAccount> findUserAccountByGoogleSubject(String googleSubjectOrNull);

    /**
     * Connects a verified Google identity to the account with the same email address.
     *
     * <p>Preconditions: the email address and Google subject must be non-blank. If the email address
     * does not belong to an account, the method returns an empty result.</p>
     *
     * @return The connected account when one exists, otherwise an empty result.
     */
    Optional<UserAccount> connectGoogleIdentityByEmailAddress(String emailAddressOrNull, String googleSubjectOrNull);

    /**
     * Changes the password for the authenticated user after verifying the current password.
     *
     * <p>Preconditions: the user account id, current password, and new password must all be
     * non-null and non-blank.</p>
     */
    void changePassword(UserAccountId userAccountId, String currentRawPassword, String newRawPassword);

}
