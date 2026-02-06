package com.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.util.UUID;
import java.time.LocalDate;

@Entity
@Table(name = "org_memberships", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgMembership {
    @Id
    private UUID id;

    @Column(name = "org_id")
    private UUID orgId;

    @Column(name = "member_uacn")
    private String memberUacn;

    private String role;

    @Column(name = "joined_at")
    @Temporal(TemporalType.DATE)
    private LocalDate joinedAt;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class OrgMembershipId implements Serializable {
    private String uacn;
    private UUID orgId;
}
