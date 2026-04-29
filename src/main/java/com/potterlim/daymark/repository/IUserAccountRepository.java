package com.potterlim.daymark.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import com.potterlim.daymark.entity.EUserRole;
import com.potterlim.daymark.entity.UserAccount;
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
        where lower(userAccount.mUserName) = lower(:userName)
        """)
    Optional<UserAccount> findByUserNameIgnoringCase(@Param("userName") String userName);

    @Query("""
        select userAccount
        from UserAccount userAccount
        where lower(userAccount.mEmailAddress) = lower(:emailAddress)
        """)
    Optional<UserAccount> findByEmailAddress(@Param("emailAddress") String emailAddress);

    @Query("""
        select userAccount
        from UserAccount userAccount
        where userAccount.mGoogleSubject = :googleSubject
        """)
    Optional<UserAccount> findByGoogleSubject(@Param("googleSubject") String googleSubject);

    @Query("""
        select userAccount
        from UserAccount userAccount
        where userAccount.mUserName = :loginIdentifier
            or lower(userAccount.mEmailAddress) = lower(:loginIdentifier)
        """)
    Optional<UserAccount> findByLoginIdentifier(@Param("loginIdentifier") String loginIdentifier);

    @Query("""
        select count(userAccount)
        from UserAccount userAccount
        where userAccount.mUserRole <> :excludedUserRole
        """)
    long countExcludingUserRole(@Param("excludedUserRole") EUserRole excludedUserRole);

    @Query("""
        select count(userAccount)
        from UserAccount userAccount
        where userAccount.mCreatedAt >= :startDateTime
            and userAccount.mCreatedAt < :endExclusiveDateTime
            and userAccount.mUserRole <> :excludedUserRole
        """)
    long countCreatedWithinExcludingUserRole(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endExclusiveDateTime") LocalDateTime endExclusiveDateTime,
        @Param("excludedUserRole") EUserRole excludedUserRole
    );
}
