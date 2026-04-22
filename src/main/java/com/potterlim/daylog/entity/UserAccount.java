package com.potterlim.daylog.entity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "user_account")
public class UserAccount implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long mId;

    @Column(name = "user_name", nullable = false, unique = true, length = 100)
    private String mUserName;

    @Column(name = "password_hash", nullable = false)
    private String mPasswordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false, length = 30)
    private EUserRole mUserRole;

    @Column(name = "enabled", nullable = false)
    private boolean mIsEnabled;

    @Column(name = "locked", nullable = false)
    private boolean mIsLocked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime mCreatedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime mUpdatedAt;

    protected UserAccount() {
    }

    private UserAccount(String userName, String passwordHash, EUserRole userRole) {
        mUserName = userName;
        mPasswordHash = passwordHash;
        mUserRole = userRole;
        mIsEnabled = true;
        mIsLocked = false;
    }

    public static UserAccount createRegularUser(String userName, String passwordHash) {
        return new UserAccount(userName, passwordHash, EUserRole.USER);
    }

    public UserAccountId getUserAccountId() {
        return UserAccountId.from(mId);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(mUserRole.getAuthorityName()));
    }

    @Override
    public String getPassword() {
        return mPasswordHash;
    }

    @Override
    public String getUsername() {
        return mUserName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !mIsLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    @PrePersist
    public void handleBeforePersist() {
        LocalDateTime now = LocalDateTime.now();
        mCreatedAt = now;
        mUpdatedAt = now;
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        mUpdatedAt = LocalDateTime.now();
    }
}
