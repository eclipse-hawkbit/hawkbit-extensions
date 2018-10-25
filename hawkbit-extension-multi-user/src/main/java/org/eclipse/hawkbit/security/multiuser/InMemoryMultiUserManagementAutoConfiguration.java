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
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
        final List<UserDetails> userDetails = new ArrayList<>();
        for (MultiUserProperties.User user : multiUserProperties.getUser()) {
            List<GrantedAuthority> authorityList;
            // Allows ALL as a shorthand for all permissions
            if (user.getPermissions().size() == 1 && user.getPermissions().get(0).equals("ALL"))
                authorityList = PermissionUtils.createAllAuthorityList();
            else
                authorityList = PermissionUtils.createAuthorityList(user.getPermissions());
            final UserPrincipal userPrincipal = new UserPrincipal(user.getUsername(), user.getPassword(),
                    user.getFirstname(), user.getLastname(), user.getUsername(), user.getEmail(), defaultTenant,
                    authorityList);
            userDetails.add(userPrincipal);
        }

        // If no users are configured through the multi user properties, set up
        // the default user from security properties
        if (userDetails.isEmpty()) {
            final String name = securityProperties.getUser().getName();
            final String password = securityProperties.getUser().getPassword();
            userDetails.add(new UserPrincipal(name, password, name, name, name, null, defaultTenant,
                    PermissionUtils.createAllAuthorityList()));
        }

        return new FixedInMemoryUserDetailsService(userDetails);
    }

    private static class FixedInMemoryUserDetailsService implements UserDetailsService {
        private final HashMap<String, UserDetails> userDetailsMap = new HashMap<>();

        public FixedInMemoryUserDetailsService(Collection<UserDetails> userDetails) {
            for (UserDetails user : userDetails) {
                userDetailsMap.put(user.getUsername(), user);
            }
        }

        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            UserDetails userDetails = userDetailsMap.get(username);
            if (userDetails == null)
                throw new UsernameNotFoundException("No such user");
            return userDetails;
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