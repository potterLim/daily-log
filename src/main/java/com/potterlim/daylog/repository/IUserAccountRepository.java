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
}
