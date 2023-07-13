package com.cupid.jikting.member.entity;

import com.cupid.jikting.common.entity.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Getter
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "id", column = @Column(name = "member_id"))
@Entity
public class Member extends BaseEntity {

    @Builder.Default
    @OneToMany(mappedBy = "member")
    private final List<MemberCompany> memberCompanies = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "member")
    private final List<MemberCertification> memberCertifications = new ArrayList<>();

    private String username;
    private String password;
    private String phone;
    private String name;
    private String type;
    private Gender gender;
}
