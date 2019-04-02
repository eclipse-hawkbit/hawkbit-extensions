package org.eclipse.hawkbit.security.multiuser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.eclipse.hawkbit.im.authentication.MultitenancyIndicator;
import org.eclipse.hawkbit.im.authentication.PermissionUtils;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.im.authentication.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.core.Ordered;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties({ MultiUserProperties.class })
public class InMemoryMultiUserManagementAutoConfiguration extends GlobalAuthenticationConfigurerAdapter {

    @Autowired
    private MultiUserProperties multiUserProperties;

    @Autowired
    private SecurityProperties securityProperties;

    @Override
    public void configure(final AuthenticationManagerBuilder auth) throws Exception {
        final DaoAuthenticationProvider userDaoAuthenticationProvider = new TenantDaoAuthenticationProvider();
        userDaoAuthenticationProvider.setUserDetailsService(userDetailsService());
        auth.authenticationProvider(userDaoAuthenticationProvider);
    }

    /**
     * @return the user details service to load a user from memory user manager.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        final String defaultTenant = "DEFAULT";
        final List<UserPrincipal> userPrincipals = new ArrayList<>();
        for (MultiUserProperties.User user : multiUserProperties.getUser()) {
            List<GrantedAuthority> authorityList;
            // Allows ALL as a shorthand for all permissions
            if (user.getPermissions().size() == 1 && user.getPermissions().get(0).equals("ALL")) {
                authorityList = PermissionUtils.createAllAuthorityList();
            } else {
                authorityList = new ArrayList<>(user.getPermissions().size());
                for (final String permission : user.getPermissions()) {
                    authorityList.add(new SimpleGrantedAuthority(permission));
                    authorityList.add(new SimpleGrantedAuthority("ROLE_" + permission));
                }
            }

            final UserPrincipal userPrincipal = new UserPrincipal(user.getUsername(), user.getPassword(),
                    user.getFirstname(), user.getLastname(), user.getUsername(), user.getEmail(), defaultTenant,
                    authorityList);
            userPrincipals.add(userPrincipal);
        }

        // If no users are configured through the multi user properties, set up
        // the default user from security properties
        if (userPrincipals.isEmpty()) {
            final String name = securityProperties.getUser().getName();
            final String password = securityProperties.getUser().getPassword();
            userPrincipals.add(new UserPrincipal(name, password, name, name, name, null, defaultTenant,
                    PermissionUtils.createAllAuthorityList()));
        }

        return new FixedInMemoryUserPrincipalUserDetailsService(userPrincipals);
    }

    private static class FixedInMemoryUserPrincipalUserDetailsService implements UserDetailsService {
        private final HashMap<String, UserPrincipal> userPrincipalMap = new HashMap<>();

        public FixedInMemoryUserPrincipalUserDetailsService(Collection<UserPrincipal> userPrincipals) {
            for (UserPrincipal user : userPrincipals) {
                userPrincipalMap.put(user.getUsername(), user);
            }
        }

        private static UserPrincipal clone(UserPrincipal a) {
            return new UserPrincipal(a.getUsername(), a.getPassword(), a.getFirstname(), a.getLastname(),
                    a.getLoginname(), a.getEmail(), a.getTenant(), a.getAuthorities());
        }

        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            UserPrincipal userPrincipal = userPrincipalMap.get(username);
            if (userPrincipal == null)
                throw new UsernameNotFoundException("No such user");
            // Spring mutates the data, so we must return a copy here
            return clone(userPrincipal);
        }

    }

    /**
     * @return the multi-tenancy indicator to disallow multi-tenancy
     */
    @Bean
    public MultitenancyIndicator multiTenancyIndicator() {
        return () -> false;
    }

    private static class TenantDaoAuthenticationProvider extends DaoAuthenticationProvider {
        @Override
        protected Authentication createSuccessAuthentication(final Object principal,
                final Authentication authentication, final UserDetails user) {
            final UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(principal,
                    authentication.getCredentials(), user.getAuthorities());
            result.setDetails(new TenantAwareAuthenticationDetails("DEFAULT", false));
            return result;
        }
    }

}