package com.example.security;

import com.example.domain.OrgMembership;
import com.example.domain.Organization;
import com.example.domain.UacnRegistry;
import com.example.multitenancy.TenantContext;
import com.example.repository.OrgMembershipRepository;
import com.example.repository.OrganizationRepository;
import com.example.repository.UacnRegistryRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DomainUserDetailsService implements UserDetailsService {

    private final UacnRegistryRepository uacnRegistryRepository;
    private final OrganizationRepository organizationRepository;
    private final OrgMembershipRepository orgMembershipRepository;

    public DomainUserDetailsService(UacnRegistryRepository uacnRegistryRepository,
            OrganizationRepository organizationRepository,
            OrgMembershipRepository orgMembershipRepository) {
        this.uacnRegistryRepository = uacnRegistryRepository;
        this.organizationRepository = organizationRepository;
        this.orgMembershipRepository = orgMembershipRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String uacn) throws UsernameNotFoundException {
        // 1. Verify user exists in Registry
        UacnRegistry registry = uacnRegistryRepository.findById(uacn)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + uacn));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER")); // Base role

        // 2. Check Role in Current Tenant
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null) {
            Optional<Organization> orgOpt = organizationRepository.findBySchemaName(currentTenant);
            if (orgOpt.isPresent()) {
                Optional<OrgMembership> membershipOut = orgMembershipRepository.findByMemberUacnAndOrgId(uacn,
                        orgOpt.get().getId());
                membershipOut.ifPresent(m -> {
                    String role = "ROLE_" + m.getRole(); // e.g. ROLE_ADMIN
                    authorities.add(new SimpleGrantedAuthority(role));
                });
            }
        }

        // 3. For Teachers/Students, their roles might be implied by their existence in
        // the tenant tables?
        // But for this exercise, we rely on "ROLE_ADMIN" for Principal.
        // And the Aspect checks "ROLE_ADMIN" bypass.
        // For Teachers, the Aspect checks DB tables (SubjectAssignment).
        // So they don't strictly need a "ROLE_TEACHER" authority for the Aspect to
        // work,
        // as long as they are authenticated.

        return User.builder()
                .username(uacn)
                .password("{noop}password") // Default password for testing
                .authorities(authorities)
                .build();
    }
}
