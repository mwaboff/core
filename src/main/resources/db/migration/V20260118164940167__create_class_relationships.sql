-- Migration: create_class_relationships
-- Description: Creates join tables for class many-to-many relationships

-- Class to Domains relationship
CREATE TABLE class_domains (
    class_id BIGINT NOT NULL,
    domain_id BIGINT NOT NULL,
    PRIMARY KEY (class_id, domain_id),

    CONSTRAINT fk_class_domains_class FOREIGN KEY (class_id)
        REFERENCES classes(id) ON DELETE CASCADE,
    CONSTRAINT fk_class_domains_domain FOREIGN KEY (domain_id)
        REFERENCES domains(id) ON DELETE CASCADE
);

CREATE INDEX idx_class_domains_domain ON class_domains(domain_id);

-- Class to Hope Features relationship
CREATE TABLE class_hope_features (
    class_id BIGINT NOT NULL,
    feature_id BIGINT NOT NULL,
    PRIMARY KEY (class_id, feature_id),

    CONSTRAINT fk_class_hope_features_class FOREIGN KEY (class_id)
        REFERENCES classes(id) ON DELETE CASCADE,
    CONSTRAINT fk_class_hope_features_feature FOREIGN KEY (feature_id)
        REFERENCES features(id) ON DELETE CASCADE
);

CREATE INDEX idx_class_hope_features_feature ON class_hope_features(feature_id);

-- Class to Class Features relationship
CREATE TABLE class_class_features (
    class_id BIGINT NOT NULL,
    feature_id BIGINT NOT NULL,
    PRIMARY KEY (class_id, feature_id),

    CONSTRAINT fk_class_class_features_class FOREIGN KEY (class_id)
        REFERENCES classes(id) ON DELETE CASCADE,
    CONSTRAINT fk_class_class_features_feature FOREIGN KEY (feature_id)
        REFERENCES features(id) ON DELETE CASCADE
);

CREATE INDEX idx_class_class_features_feature ON class_class_features(feature_id);

-- Class to Background Questions relationship
CREATE TABLE class_background_questions (
    class_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    PRIMARY KEY (class_id, question_id),

    CONSTRAINT fk_class_background_questions_class FOREIGN KEY (class_id)
        REFERENCES classes(id) ON DELETE CASCADE,
    CONSTRAINT fk_class_background_questions_question FOREIGN KEY (question_id)
        REFERENCES questions(id) ON DELETE CASCADE
);

CREATE INDEX idx_class_background_questions_question ON class_background_questions(question_id);

-- Class to Connection Questions relationship
CREATE TABLE class_connection_questions (
    class_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    PRIMARY KEY (class_id, question_id),

    CONSTRAINT fk_class_connection_questions_class FOREIGN KEY (class_id)
        REFERENCES classes(id) ON DELETE CASCADE,
    CONSTRAINT fk_class_connection_questions_question FOREIGN KEY (question_id)
        REFERENCES questions(id) ON DELETE CASCADE
);

CREATE INDEX idx_class_connection_questions_question ON class_connection_questions(question_id);
