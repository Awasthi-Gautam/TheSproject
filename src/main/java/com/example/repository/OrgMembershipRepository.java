package com.example.repository;

import com.example.domain.OrgMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrgMembershipRepository extends JpaRepository<OrgMembership, UUID> {
    Optional<OrgMembership> findByMemberUacnAndOrgId(String memberUacn, UUID orgId);
}
