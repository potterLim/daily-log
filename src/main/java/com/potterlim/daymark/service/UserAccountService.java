package com.potterlim.daymark.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import com.potterlim.daymark.dto.auth.RegisterGoogleUserAccountCommand;
import com.potterlim.daymark.dto.auth.RegisterUserAccountCommand;
import com.potterlim.daymark.entity.UserAccount;
import com.potterlim.daymark.entity.UserAccountId;
import com.potterlim.daymark.repository.IUserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService implements IUserAccountService {

    private final IUserAccountRepository mUserAccountRepository;
    private final PasswordEncoder mPasswordEncoder;
    private final AdministratorWorkspaceIdPolicy mAdministratorWorkspaceIdPolicy;
    private final Clock mClock;

    public UserAccountService(
        IUserAccountRepository userAccountRepository,
        PasswordEncoder passwordEncoder,
        AdministratorWorkspaceIdPolicy administratorWorkspaceIdPolicy,
        Clock clock
    ) {
        mUserAccountRepository = userAccountRepository;
        mPasswordEncoder = passwordEncoder;
        mAdministratorWorkspaceIdPolicy = administratorWorkspaceIdPolicy;
        mClock = clock;
    }

    @Override
    @Transactional
    public UserAccount registerUserAccount(RegisterUserAccountCommand registerUserAccountCommand) {
        validateRegisterUserAccountCommand(registerUserAccountCommand);

        String normalizedUserName = normalizeUserName(registerUserAccountCommand.getUserName());
        String normalizedEmailAddress = normalizeEmailAddress(registerUserAccountCommand.getEmailAddress());
        String rawPassword = registerUserAccountCommand.getRawPassword();

        if (mUserAccountRepository.findByUserName(normalizedUserName).isPresent()) {
            throw new DuplicateUserNameException(normalizedUserName);
        }

        if (mUserAccountRepository.findByEmailAddress(normalizedEmailAddress).isPresent()) {
            throw new DuplicateEmailException(normalizedEmailAddress);
        }

        UserAccount userAccount = UserAccount.createRegularUser(
            normalizedUserName,
            normalizedEmailAddress,
            mPasswordEncoder.encode(rawPassword)
        );
        grantAdministratorRoleIfConfigured(userAccount);

        try {
            return mUserAccountRepository.save(userAccount);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            throw resolveDuplicateRegistrationException(
                normalizedUserName,
                normalizedEmailAddress,
                dataIntegrityViolationException
            );
        }
    }

    @Override
    @Transactional
    public UserAccount registerGoogleUserAccount(RegisterGoogleUserAccountCommand registerGoogleUserAccountCommand) {
        validateRegisterGoogleUserAccountCommand(registerGoogleUserAccountCommand);

        String normalizedUserName = normalizeUserName(registerGoogleUserAccountCommand.getUserName());
        String normalizedEmailAddress = normalizeEmailAddress(registerGoogleUserAccountCommand.getEmailAddress());
        String normalizedGoogleSubject = registerGoogleUserAccountCommand.getGoogleSubject().trim();
        String rawPassword = registerGoogleUserAccountCommand.getRawPassword();

        if (mUserAccountRepository.findByUserName(normalizedUserName).isPresent()) {
            throw new DuplicateUserNameException(normalizedUserName);
        }

        if (mUserAccountRepository.findByEmailAddress(normalizedEmailAddress).isPresent()) {
            throw new DuplicateEmailException(normalizedEmailAddress);
        }

        if (mUserAccountRepository.findByGoogleSubject(normalizedGoogleSubject).isPresent()) {
            throw new DuplicateEmailException(normalizedEmailAddress);
        }

        UserAccount userAccount = UserAccount.createGoogleVerifiedUser(
            normalizedUserName,
            normalizedEmailAddress,
            mPasswordEncoder.encode(rawPassword),
            normalizedGoogleSubject,
            LocalDateTime.now(mClock)
        );
        grantAdministratorRoleIfConfigured(userAccount);

        try {
            return mUserAccountRepository.save(userAccount);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            throw resolveDuplicateRegistrationException(
                normalizedUserName,
                normalizedEmailAddress,
                dataIntegrityViolationException
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findUserAccountByLoginIdentifier(String loginIdentifierOrNull) {
        if (loginIdentifierOrNull == null || loginIdentifierOrNull.isBlank()) {
            return Optional.empty();
        }

        return mUserAccountRepository.findByLoginIdentifier(loginIdentifierOrNull.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findUserAccountByEmailAddress(String emailAddressOrNull) {
        if (emailAddressOrNull == null || emailAddressOrNull.isBlank()) {
            return Optional.empty();
        }

        return mUserAccountRepository.findByEmailAddress(normalizeEmailAddress(emailAddressOrNull));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findUserAccountByGoogleSubject(String googleSubjectOrNull) {
        if (googleSubjectOrNull == null || googleSubjectOrNull.isBlank()) {
            return Optional.empty();
        }

        return mUserAccountRepository.findByGoogleSubject(googleSubjectOrNull.trim());
    }

    @Override
    @Transactional
    public Optional<UserAccount> connectGoogleIdentityByEmailAddress(
        String emailAddressOrNull,
        String googleSubjectOrNull
    ) {
        if (emailAddressOrNull == null || emailAddressOrNull.isBlank()) {
            return Optional.empty();
        }

        if (googleSubjectOrNull == null || googleSubjectOrNull.isBlank()) {
            return Optional.empty();
        }

        String normalizedEmailAddress = normalizeEmailAddress(emailAddressOrNull);
        String normalizedGoogleSubject = googleSubjectOrNull.trim();
        Optional<UserAccount> existingGoogleUserAccountOrEmpty =
            mUserAccountRepository.findByGoogleSubject(normalizedGoogleSubject);
        if (existingGoogleUserAccountOrEmpty.isPresent()) {
            return existingGoogleUserAccountOrEmpty;
        }

        return mUserAccountRepository.findByEmailAddress(normalizedEmailAddress)
            .map(userAccount -> {
                userAccount.connectGoogleIdentity(normalizedGoogleSubject, LocalDateTime.now(mClock));
                return userAccount;
            });
    }

    @Override
    @Transactional
    public void changePassword(UserAccountId userAccountId, String currentRawPassword, String newRawPassword) {
        validatePasswordChangeInput(userAccountId, currentRawPassword, newRawPassword);

        UserAccount userAccount = getRequiredUserAccount(userAccountId);
        if (!mPasswordEncoder.matches(currentRawPassword, userAccount.getPassword())) {
            throw new InvalidCurrentPasswordException();
        }

        applyNewPassword(userAccount, newRawPassword);
    }

    private static void validateRegisterUserAccountCommand(RegisterUserAccountCommand registerUserAccountCommand) {
        if (registerUserAccountCommand == null) {
            throw new IllegalArgumentException("registerUserAccountCommand must not be null.");
        }

        if (registerUserAccountCommand.getUserName() == null || registerUserAccountCommand.getUserName().isBlank()) {
            throw new IllegalArgumentException("userName must not be blank.");
        }

        if (registerUserAccountCommand.getEmailAddress() == null || registerUserAccountCommand.getEmailAddress().isBlank()) {
            throw new IllegalArgumentException("emailAddress must not be blank.");
        }

        if (registerUserAccountCommand.getRawPassword() == null || registerUserAccountCommand.getRawPassword().isBlank()) {
            throw new IllegalArgumentException("rawPassword must not be blank.");
        }

        validateRawPassword(registerUserAccountCommand.getRawPassword());
    }

    private static void validateRegisterGoogleUserAccountCommand(
        RegisterGoogleUserAccountCommand registerGoogleUserAccountCommand
    ) {
        if (registerGoogleUserAccountCommand == null) {
            throw new IllegalArgumentException("registerGoogleUserAccountCommand must not be null.");
        }

        if (registerGoogleUserAccountCommand.getUserName() == null
            || registerGoogleUserAccountCommand.getUserName().isBlank()) {
            throw new IllegalArgumentException("userName must not be blank.");
        }

        if (registerGoogleUserAccountCommand.getEmailAddress() == null
            || registerGoogleUserAccountCommand.getEmailAddress().isBlank()) {
            throw new IllegalArgumentException("emailAddress must not be blank.");
        }

        if (registerGoogleUserAccountCommand.getGoogleSubject() == null
            || registerGoogleUserAccountCommand.getGoogleSubject().isBlank()) {
            throw new IllegalArgumentException("googleSubject must not be blank.");
        }

        if (registerGoogleUserAccountCommand.getRawPassword() == null
            || registerGoogleUserAccountCommand.getRawPassword().isBlank()) {
            throw new IllegalArgumentException("rawPassword must not be blank.");
        }

        validateRawPassword(registerGoogleUserAccountCommand.getRawPassword());
    }

    private static void validatePasswordChangeInput(
        UserAccountId userAccountId,
        String currentRawPassword,
        String newRawPassword
    ) {
        if (userAccountId == null) {
            throw new IllegalArgumentException("userAccountId must not be null.");
        }

        if (currentRawPassword == null || currentRawPassword.isBlank()) {
            throw new IllegalArgumentException("currentRawPassword must not be blank.");
        }

        validateRawPassword(newRawPassword);
    }

    private static void validateRawPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("rawPassword must not be blank.");
        }

        int rawPasswordLength = rawPassword.length();
        if (rawPasswordLength < 8 || rawPasswordLength > 72) {
            throw new IllegalArgumentException("rawPassword length must be between 8 and 72.");
        }
    }

    private static String normalizeUserName(String userName) {
        return userName.trim();
    }

    private static String normalizeEmailAddress(String emailAddress) {
        return emailAddress.trim().toLowerCase(Locale.ROOT);
    }

    private UserAccount getRequiredUserAccount(UserAccountId userAccountId) {
        return mUserAccountRepository.findById(userAccountId.getValue())
            .orElseThrow(() -> new IllegalStateException("User account not found."));
    }

    private void applyNewPassword(UserAccount userAccount, String newRawPassword) {
        validateRawPassword(newRawPassword);
        userAccount.changePasswordHash(mPasswordEncoder.encode(newRawPassword));
    }

    private void grantAdministratorRoleIfConfigured(UserAccount userAccount) {
        if (mAdministratorWorkspaceIdPolicy.isAdministratorWorkspaceId(userAccount.getUsername())) {
            userAccount.grantAdministratorRole();
        }
    }

    private RuntimeException resolveDuplicateRegistrationException(
        String normalizedUserName,
        String normalizedEmailAddress,
        DataIntegrityViolationException dataIntegrityViolationException
    ) {
        if (mUserAccountRepository.findByUserName(normalizedUserName).isPresent()) {
            return new DuplicateUserNameException(normalizedUserName);
        }

        if (mUserAccountRepository.findByEmailAddress(normalizedEmailAddress).isPresent()) {
            return new DuplicateEmailException(normalizedEmailAddress);
        }

        return dataIntegrityViolationException;
    }
}
