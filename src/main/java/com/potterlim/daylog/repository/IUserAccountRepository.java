package com.potterlim.daylog.repository;

import java.util.Optional;
import com.potterlim.daylog.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IUserAccountRepository extends JpaRepository<UserAccount, Long> {

    @Query("""
        select userAccount
        from UserAccount userAccount
        where userAccount.mUserName = :userName
        """)
    Optional<UserAccount> findByUserName(@Param("userName") String userName);

    @Query("""
        select userAccount
        from UserAccount userAccount
        where lower(userAccount.mEmailAddress) = lower(:emailAddress)
        """)
    Optional<UserAccount> findByEmailAddress(@Param("emailAddress") String emailAddress);

    @Query("""
        select userAccount
        from UserAccount userAccount
        where userAccount.mUserName = :loginIdentifier
            or lower(userAccount.mEmailAddress) = lower(:loginIdentifier)
        """)
    Optional<UserAccount> findByLoginIdentifier(@Param("loginIdentifier") String loginIdentifier);
}
