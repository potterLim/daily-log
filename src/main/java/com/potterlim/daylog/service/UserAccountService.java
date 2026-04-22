package com.potterlim.daylog.service;

import java.util.Optional;
import com.potterlim.daylog.dto.auth.RegisterUserAccountCommand;
import com.potterlim.daylog.entity.EUserRole;
import com.potterlim.daylog.entity.UserAccount;
import com.potterlim.daylog.repository.IUserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService implements IUserAccountService {

    private final IUserAccountRepository mUserAccountRepository;
    private final PasswordEncoder mPasswordEncoder;

    public UserAccountService(IUserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        mUserAccountRepository = userAccountRepository;
        mPasswordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserAccount registerUserAccount(RegisterUserAccountCommand registerUserAccountCommand) {
        validateRegisterUserAccountCommand(registerUserAccountCommand);

        String normalizedUserName = registerUserAccountCommand.getUserName().trim();
        String rawPassword = registerUserAccountCommand.getRawPassword();

        if (mUserAccountRepository.findByUserName(normalizedUserName).isPresent()) {
            throw new DuplicateUserNameException(normalizedUserName);
        }

        UserAccount userAccount = new UserAccount(
            normalizedUserName,
            mPasswordEncoder.encode(rawPassword),
            EUserRole.USER
        );

        return mUserAccountRepository.save(userAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findUserAccountByUserName(String userNameOrNull) {
        if (userNameOrNull == null || userNameOrNull.isBlank()) {
            return Optional.empty();
        }

        return mUserAccountRepository.findByUserName(userNameOrNull.trim());
    }

    private static void validateRegisterUserAccountCommand(RegisterUserAccountCommand registerUserAccountCommand) {
        if (registerUserAccountCommand == null) {
            throw new IllegalArgumentException("registerUserAccountCommand must not be null.");
        }

        if (registerUserAccountCommand.getUserName() == null || registerUserAccountCommand.getUserName().isBlank()) {
            throw new IllegalArgumentException("userName must not be blank.");
        }

        if (registerUserAccountCommand.getRawPassword() == null || registerUserAccountCommand.getRawPassword().isBlank()) {
            throw new IllegalArgumentException("rawPassword must not be blank.");
        }
    }
}
