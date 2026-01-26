package com.example.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "org_memberships", schema = "public")
@IdClass(OrgMembershipId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgMembership {
    @Id
    private String uacn;
    @Id
    private UUID orgId;
    private String role;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class OrgMembershipId implements Serializable {
    private String uacn;
    private UUID orgId;
}
