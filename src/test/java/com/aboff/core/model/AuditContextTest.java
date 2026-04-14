package com.aboff.core.model;

import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.Role;
import com.aboff.core.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuditContext}.
 */
class AuditContextTest {

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /**
     * Builds a mocked {@link Authentication} whose principal is a {@link CustomUserDetails}
     * wrapping the given {@link User}.
     */
    private Authentication mockAuthentication(long userId, String username, Role role) {
        User user = User.builder().id(userId).username(username).role(role).build();
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        when(userDetails.getUsername()).thenReturn(username);
        when(userDetails.getUser()).thenReturn(user);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        return auth;
    }

    // ---------------------------------------------------------------------------
    // forUser
    // ---------------------------------------------------------------------------

    @Test
    void forUser_withValidAuthentication_includesUserIdUsernameAndRole() {
        Authentication auth = mockAuthentication(42L, "mwaboff", Role.OWNER);

        String result = AuditContext.forUser(auth).build().format();

        assertThat(result).isEqualTo("[user_id: 42; username: mwaboff; role: owner]");
    }

    @Test
    void forUser_roleIsLowercased() {
        Authentication auth = mockAuthentication(1L, "adminUser", Role.ADMIN);

        String result = AuditContext.forUser(auth).build().format();

        assertThat(result).contains("role: admin");
    }

    @Test
    void forUser_withNullAuthentication_producesEmptyContext() {
        String result = AuditContext.forUser(null).build().format();

        assertThat(result).isEqualTo("[]");
    }

    @Test
    void forUser_withNonCustomUserDetailsPrincipal_producesEmptyContext() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("anonymousUser");

        String result = AuditContext.forUser(auth).build().format();

        assertThat(result).isEqualTo("[]");
    }

    @Test
    void forUser_withNullRole_omitsRoleField() {
        User user = User.builder().id(7L).username("norole").role(null).build();
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(7L);
        when(userDetails.getUsername()).thenReturn("norole");
        when(userDetails.getUser()).thenReturn(user);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);

        String result = AuditContext.forUser(auth).build().format();

        assertThat(result).isEqualTo("[user_id: 7; username: norole]");
    }

    // ---------------------------------------------------------------------------
    // forIp
    // ---------------------------------------------------------------------------

    @Test
    void forIp_withValidIp_producesIpOnlyContext() {
        String result = AuditContext.forIp("192.168.1.1").build().format();

        assertThat(result).isEqualTo("[ip: 192.168.1.1]");
    }

    @Test
    void forIp_withNullIp_producesEmptyContext() {
        String result = AuditContext.forIp(null).build().format();

        assertThat(result).isEqualTo("[]");
    }

    // ---------------------------------------------------------------------------
    // Builder — withIp
    // ---------------------------------------------------------------------------

    @Test
    void withIp_addsIpField() {
        String result = AuditContext.forUser(null).withIp("10.0.0.1").build().format();

        assertThat(result).isEqualTo("[ip: 10.0.0.1]");
    }

    @Test
    void withIp_withNullIp_omitsField() {
        String result = AuditContext.forUser(null).withIp(null).build().format();

        assertThat(result).isEqualTo("[]");
    }

    // ---------------------------------------------------------------------------
    // Builder — withCampaignId
    // ---------------------------------------------------------------------------

    @Test
    void withCampaignId_addsCampaignIdField() {
        String result = AuditContext.forUser(null).withCampaignId(5L).build().format();

        assertThat(result).isEqualTo("[campaign_id: 5]");
    }

    @Test
    void withCampaignId_withNullCampaignId_omitsField() {
        String result = AuditContext.forUser(null).withCampaignId(null).build().format();

        assertThat(result).isEqualTo("[]");
    }

    // ---------------------------------------------------------------------------
    // Builder — withCharacterSheetId
    // ---------------------------------------------------------------------------

    @Test
    void withCharacterSheetId_addsCharacterSheetIdField() {
        String result = AuditContext.forUser(null).withCharacterSheetId(99L).build().format();

        assertThat(result).isEqualTo("[character_sheet_id: 99]");
    }

    @Test
    void withCharacterSheetId_withNullCharacterSheetId_omitsField() {
        String result = AuditContext.forUser(null).withCharacterSheetId(null).build().format();

        assertThat(result).isEqualTo("[]");
    }

    // ---------------------------------------------------------------------------
    // Builder — withTargetUserId
    // ---------------------------------------------------------------------------

    @Test
    void withTargetUserId_addsTargetUserIdField() {
        String result = AuditContext.forUser(null).withTargetUserId(77L).build().format();

        assertThat(result).isEqualTo("[target_user_id: 77]");
    }

    @Test
    void withTargetUserId_withNullTargetUserId_omitsField() {
        String result = AuditContext.forUser(null).withTargetUserId(null).build().format();

        assertThat(result).isEqualTo("[]");
    }

    // ---------------------------------------------------------------------------
    // Builder — withEntityType
    // ---------------------------------------------------------------------------

    @Test
    void withEntityType_addsEntityTypeField() {
        String result = AuditContext.forUser(null).withEntityType("weapon").build().format();

        assertThat(result).isEqualTo("[entity_type: weapon]");
    }

    @Test
    void withEntityType_withNullEntityType_omitsField() {
        String result = AuditContext.forUser(null).withEntityType(null).build().format();

        assertThat(result).isEqualTo("[]");
    }

    // ---------------------------------------------------------------------------
    // Combining multiple fields
    // ---------------------------------------------------------------------------

    @Test
    void forUser_withCampaignIdAndIp_includesAllFields() {
        Authentication auth = mockAuthentication(42L, "mwaboff", Role.OWNER);

        String result = AuditContext.forUser(auth)
                .withCampaignId(3L)
                .withIp("10.0.0.1")
                .build()
                .format();

        assertThat(result).isEqualTo("[user_id: 42; username: mwaboff; role: owner; campaign_id: 3; ip: 10.0.0.1]");
    }

    @Test
    void forUser_withAllBuilderFields_includesAllFields() {
        Authentication auth = mockAuthentication(42L, "mwaboff", Role.USER);

        String result = AuditContext.forUser(auth)
                .withIp("10.0.0.1")
                .withCampaignId(3L)
                .withCharacterSheetId(7L)
                .withTargetUserId(99L)
                .withEntityType("armor")
                .build()
                .format();

        assertThat(result).isEqualTo(
                "[user_id: 42; username: mwaboff; role: user; ip: 10.0.0.1; campaign_id: 3; character_sheet_id: 7; target_user_id: 99; entity_type: armor]"
        );
    }

    @Test
    void forIp_withCampaignId_includesBothFields() {
        String result = AuditContext.forIp("172.16.0.5").withCampaignId(12L).build().format();

        assertThat(result).isEqualTo("[ip: 172.16.0.5; campaign_id: 12]");
    }

    // ---------------------------------------------------------------------------
    // Null fields mixed with valid fields
    // ---------------------------------------------------------------------------

    @Test
    void nullCampaignId_doesNotAppearInOutput_otherFieldsPresent() {
        Authentication auth = mockAuthentication(42L, "mwaboff", Role.OWNER);

        String result = AuditContext.forUser(auth)
                .withCampaignId(null)
                .withIp("10.0.0.1")
                .build()
                .format();

        assertThat(result)
                .doesNotContain("campaign_id")
                .contains("ip: 10.0.0.1");
    }

    // ---------------------------------------------------------------------------
    // Empty context
    // ---------------------------------------------------------------------------

    @Test
    void emptyBuilder_formatReturnsEmptyBrackets() {
        AuditContext ctx = AuditContext.forUser(null).build();

        assertThat(ctx.format()).isEqualTo("[]");
    }

    @Test
    void emptyBuilder_withAllNullFields_formatReturnsEmptyBrackets() {
        AuditContext ctx = AuditContext.forUser(null)
                .withIp(null)
                .withCampaignId(null)
                .withCharacterSheetId(null)
                .withTargetUserId(null)
                .withEntityType(null)
                .build();

        assertThat(ctx.format()).isEqualTo("[]");
    }
}
