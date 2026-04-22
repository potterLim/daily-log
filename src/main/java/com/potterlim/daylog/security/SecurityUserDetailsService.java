package com.potterlim.daylog.security;

import com.potterlim.daylog.service.IUserAccountService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SecurityUserDetailsService implements UserDetailsService {

    private final IUserAccountService mUserAccountService;

    public SecurityUserDetailsService(IUserAccountService userAccountService) {
        mUserAccountService = userAccountService;
    }

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        return mUserAccountService.findUserAccountByLoginIdentifier(userName)
            .orElseThrow(() -> new UsernameNotFoundException("User account not found."));
    }
}
