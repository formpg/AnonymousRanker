package com.anonranker.service;

import com.anonranker.domain.Member;
import com.anonranker.domain.Session;
import com.anonranker.domain.VotingRule;
import com.anonranker.service.exception.InvalidBallotException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Uses a real (H2) persistence context via @DataJpaTest purely to obtain
 * genuine generated Member ids - BallotValidator itself has no repository
 * dependencies and is exercised directly, not through Spring.
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class BallotValidatorTest {

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private final BallotValidator ballotValidator = new BallotValidator();

    private Session session;
    private Member alice;
    private Member bob;
    private Member carol;
    private VotingRule rule;

    @BeforeEach
    void setUp() {
        session = new Session("Test Session", "group-ballot-test", LocalDate.now(), "hash", "admin-token", "voting-token");
        alice = new Member(session, "Alice", 0);
        bob = new Member(session, "Bob", 1);
        carol = new Member(session, "Carol", 2);
        session.getMembers().add(alice);
        session.getMembers().add(bob);
        session.getMembers().add(carol);
        rule = new VotingRule(session);
        rule.setVotesPerTopic(2);
        rule.setAllowSelfVote(false);
        session.setVotingRule(rule);

        entityManager.persist(session);
        entityManager.flush();
    }

    @Test
    void acceptsValidBallot() {
        List<Long> candidates = List.of(bob.getId(), carol.getId());

        ballotValidator.validate(rule, alice, session.getMembers(), candidates);
    }

    @Test
    void rejectsWrongCandidateCount() {
        List<Long> candidates = List.of(bob.getId());

        assertThatThrownBy(() -> ballotValidator.validate(rule, alice, session.getMembers(), candidates))
                .isInstanceOf(InvalidBallotException.class);
    }

    @Test
    void rejectsDuplicateCandidate() {
        List<Long> candidates = List.of(bob.getId(), bob.getId());

        assertThatThrownBy(() -> ballotValidator.validate(rule, alice, session.getMembers(), candidates))
                .isInstanceOf(InvalidBallotException.class);
    }

    @Test
    void rejectsSelfVoteWhenNotAllowed() {
        List<Long> candidates = List.of(alice.getId(), bob.getId());

        assertThatThrownBy(() -> ballotValidator.validate(rule, alice, session.getMembers(), candidates))
                .isInstanceOf(InvalidBallotException.class);
    }

    @Test
    void allowsSelfVoteWhenEnabled() {
        rule.setAllowSelfVote(true);
        List<Long> candidates = List.of(alice.getId(), bob.getId());

        ballotValidator.validate(rule, alice, session.getMembers(), candidates);
    }

    @Test
    void rejectsCandidateNotInSession() {
        List<Long> candidates = List.of(bob.getId(), 999_999L);

        assertThatThrownBy(() -> ballotValidator.validate(rule, alice, session.getMembers(), candidates))
                .isInstanceOf(InvalidBallotException.class);
    }
}
