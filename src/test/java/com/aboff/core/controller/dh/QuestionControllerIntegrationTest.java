package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateQuestionRequest;
import com.aboff.core.model.dto.dh.request.UpdateQuestionRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Question;
import com.aboff.core.model.enums.QuestionType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.QuestionRepository;
import com.aboff.core.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for QuestionController.
 * Tests all CRUD endpoints for Question resources with proper authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class QuestionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExpansionRepository expansionRepository;


    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User adminUser;
    private User regularUser;
    private String adminToken;
    private String userToken;
    private Expansion testExpansion;

    @BeforeEach
    void setUp() {
        adminUser = createUserWithRole("admin", "admin@example.com", Role.ADMIN);
        regularUser = createUserWithRole("user", "user@example.com", Role.USER);

        adminToken = jwtTokenProvider.generateToken(adminUser);
        userToken = jwtTokenProvider.generateToken(regularUser);

        storeTokenInDatabase(adminUser.getId(), adminToken);
        storeTokenInDatabase(regularUser.getId(), userToken);

        testExpansion = createExpansion("Core Rulebook", true);
    }

    // ==================== GET ALL QUESTIONS TESTS ====================

    @Test
    void getAllQuestions_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        createQuestion("What is your background?", QuestionType.BACKGROUND, testExpansion);
        createQuestion("Who do you know?", QuestionType.CONNECTION, testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/questions")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllQuestions_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/questions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllQuestions_WithPagination_ReturnsCorrectPage() throws Exception {
        // Arrange
        for (int i = 1; i <= 5; i++) {
            createQuestion("Question " + i, QuestionType.BACKGROUND, testExpansion);
        }

        // Act & Assert
        mockMvc.perform(get("/api/dh/questions")
                        .param("page", "1")
                        .param("size", "2")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void getAllQuestions_FilterByQuestionType_ReturnsFiltered() throws Exception {
        // Arrange
        createQuestion("Background question", QuestionType.BACKGROUND, testExpansion);
        createQuestion("Connection question", QuestionType.CONNECTION, testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/questions")
                        .param("questionType", "BACKGROUND")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].questionType").value("BACKGROUND"));
    }

    @Test
    void getAllQuestions_FilterByExpansionId_ReturnsFiltered() throws Exception {
        // Arrange
        Expansion expansion2 = createExpansion("Second Expansion", true);
        createQuestion("Question 1", QuestionType.BACKGROUND, testExpansion);
        createQuestion("Question 2", QuestionType.CONNECTION, expansion2);

        // Act & Assert
        mockMvc.perform(get("/api/dh/questions")
                        .param("expansionId", testExpansion.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getAllQuestions_WithExpand_IncludesExpansion() throws Exception {
        // Arrange
        createQuestion("Test question", QuestionType.BACKGROUND, testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/questions")
                        .param("expand", "expansion")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].expansion").exists())
                .andExpect(jsonPath("$.content[0].expansion.name").value("Core Rulebook"));
    }

    // ==================== GET QUESTION BY ID TESTS ====================

    @Test
    void getQuestionById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        Question question = createQuestion("What is your background?", QuestionType.BACKGROUND, testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/questions/{id}", question.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(question.getId()))
                .andExpect(jsonPath("$.questionText").value("What is your background?"))
                .andExpect(jsonPath("$.questionType").value("BACKGROUND"));
    }

    @Test
    void getQuestionById_WithExpand_IncludesExpansion() throws Exception {
        // Arrange
        Question question = createQuestion("Test question", QuestionType.BACKGROUND, testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/questions/{id}", question.getId())
                        .param("expand", "expansion")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expansion").exists())
                .andExpect(jsonPath("$.expansion.name").value("Core Rulebook"));
    }

    @Test
    void getQuestionById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/questions/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE QUESTION TESTS ====================

    @Test
    void createQuestion_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateQuestionRequest request = CreateQuestionRequest.builder()
                .questionText("What is your greatest fear?")
                .questionType(QuestionType.BACKGROUND)
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/questions")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.questionText").value("What is your greatest fear?"))
                .andExpect(jsonPath("$.questionType").value("BACKGROUND"));

        assertThat(questionRepository.findAll()).hasSize(1);
    }

    @Test
    void createQuestion_AsUser_Returns403() throws Exception {
        // Arrange
        CreateQuestionRequest request = CreateQuestionRequest.builder()
                .questionText("What is your greatest fear?")
                .questionType(QuestionType.BACKGROUND)
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/questions")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(questionRepository.findAll()).isEmpty();
    }

    // ==================== CREATE QUESTIONS BULK TESTS ====================

    @Test
    void createQuestionsBulk_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateQuestionRequest request1 = CreateQuestionRequest.builder()
                .questionText("Question 1")
                .questionType(QuestionType.BACKGROUND)
                .expansionId(testExpansion.getId())
                .build();
        CreateQuestionRequest request2 = CreateQuestionRequest.builder()
                .questionText("Question 2")
                .questionType(QuestionType.CONNECTION)
                .expansionId(testExpansion.getId())
                .build();
        List<CreateQuestionRequest> requests = List.of(request1, request2);

        // Act & Assert
        mockMvc.perform(post("/api/dh/questions/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        assertThat(questionRepository.findAll()).hasSize(2);
    }

    @Test
    void createQuestionsBulk_AsUser_Returns403() throws Exception {
        // Arrange
        CreateQuestionRequest request = CreateQuestionRequest.builder()
                .questionText("Question 1")
                .questionType(QuestionType.BACKGROUND)
                .expansionId(testExpansion.getId())
                .build();
        List<CreateQuestionRequest> requests = List.of(request);

        // Act & Assert
        mockMvc.perform(post("/api/dh/questions/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isForbidden());
    }

    // ==================== UPDATE QUESTION TESTS ====================

    @Test
    void updateQuestion_AsAdmin_Returns200() throws Exception {
        // Arrange
        Question question = createQuestion("Original question", QuestionType.BACKGROUND, testExpansion);
        UpdateQuestionRequest request = UpdateQuestionRequest.builder()
                .questionText("Updated question")
                .questionType(QuestionType.CONNECTION)
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/questions/{id}", question.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(question.getId()))
                .andExpect(jsonPath("$.questionText").value("Updated question"))
                .andExpect(jsonPath("$.questionType").value("CONNECTION"));
    }

    @Test
    void updateQuestion_AsUser_Returns403() throws Exception {
        // Arrange
        Question question = createQuestion("Original question", QuestionType.BACKGROUND, testExpansion);
        UpdateQuestionRequest request = UpdateQuestionRequest.builder()
                .questionText("Updated question")
                .questionType(QuestionType.CONNECTION)
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/questions/{id}", question.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateQuestion_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateQuestionRequest request = UpdateQuestionRequest.builder()
                .questionText("Updated question")
                .questionType(QuestionType.CONNECTION)
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/questions/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE QUESTION TESTS ====================

    @Test
    void deleteQuestion_AsAdmin_Returns204() throws Exception {
        // Arrange
        Question question = createQuestion("To delete", QuestionType.BACKGROUND, testExpansion);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/questions/{id}", question.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        Question deleted = questionRepository.findById(question.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteQuestion_AsUser_Returns403() throws Exception {
        // Arrange
        Question question = createQuestion("To delete", QuestionType.BACKGROUND, testExpansion);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/questions/{id}", question.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        Question notDeleted = questionRepository.findById(question.getId()).orElseThrow();
        assertThat(notDeleted.getDeletedAt()).isNull();
    }

    @Test
    void deleteQuestion_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/questions/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== RESTORE QUESTION TESTS ====================

    @Test
    void restoreQuestion_AsAdmin_Returns200() throws Exception {
        // Arrange
        Question question = createQuestion("Deleted question", QuestionType.BACKGROUND, testExpansion);
        question.setDeletedAt(LocalDateTime.now());
        questionRepository.save(question);

        // Act & Assert
        mockMvc.perform(post("/api/dh/questions/{id}/restore", question.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(question.getId()))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        Question restored = questionRepository.findById(question.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void restoreQuestion_AsUser_Returns403() throws Exception {
        // Arrange
        Question question = createQuestion("Deleted question", QuestionType.BACKGROUND, testExpansion);
        question.setDeletedAt(LocalDateTime.now());
        questionRepository.save(question);

        // Act & Assert
        mockMvc.perform(post("/api/dh/questions/{id}/restore", question.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreQuestion_NotFound_Returns404() throws Exception {
        mockMvc.perform(post("/api/dh/questions/{id}/restore", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== HELPER METHODS ====================

    private User createUserWithRole(String username, String email, Role role) {
        User user = User.builder()
                .username(username)
                .email(email)
                .role(role)
                .build();
        return userRepository.save(user);
    }

    private void storeTokenInDatabase(Long userId, String token) {
        String tokenHash = jwtTokenProvider.hashToken(token);
        ActiveToken activeToken = ActiveToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .deviceInfo("Test Device")
                .ipAddress("127.0.0.1")
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        activeTokenRepository.save(activeToken);
    }

    private Expansion createExpansion(String name, Boolean isPublished) {
        Expansion expansion = Expansion.builder()
                .name(name)
                .isPublished(isPublished)
                .build();
        return expansionRepository.save(expansion);
    }

    private Question createQuestion(String questionText, QuestionType questionType, Expansion expansion) {
        Question question = Question.builder()
                .questionText(questionText)
                .questionType(questionType)
                .expansion(expansion)
                .build();
        return questionRepository.save(question);
    }
}
