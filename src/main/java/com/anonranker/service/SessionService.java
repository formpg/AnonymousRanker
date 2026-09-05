package com.anonranker.service;

import com.anonranker.domain.Member;
import com.anonranker.domain.Session;
import com.anonranker.domain.VotingRule;
import com.anonranker.repository.SessionRepository;
import com.anonranker.service.exception.SessionNotFoundException;
import com.anonranker.web.dto.CreateSessionForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final PasswordService passwordService;

    public SessionService(SessionRepository sessionRepository, PasswordService passwordService) {
        this.sessionRepository = sessionRepository;
        this.passwordService = passwordService;
    }

    @Transactional
    public Session createSession(CreateSessionForm form) {
        List<String> names = form.getMemberNames().lines()
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .distinct()
                .toList();
        if (names.isEmpty()) {
            throw new IllegalArgumentException("メンバーを1人以上入力してください");
        }
        String groupId = form.getGroupId().trim();
        if (sessionRepository.existsByGroupId(groupId)) {
            throw new IllegalArgumentException("このグループIDは既に使われています。別のIDを指定してください");
        }

        Session session = new Session(
                form.getTitle(),
                groupId,
                form.getEventDate(),
                passwordService.hash(form.getPassword()),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );

        int order = 0;
        for (String name : names) {
            session.getMembers().add(new Member(session, name, order++));
        }
        session.setVotingRule(new VotingRule(session));

        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public Session getByAdminToken(String adminToken) {
        return sessionRepository.findByAdminToken(adminToken)
                .orElseThrow(() -> new SessionNotFoundException(adminToken));
    }

    @Transactional(readOnly = true)
    public Session getByVotingToken(String votingToken) {
        return sessionRepository.findByVotingToken(votingToken)
                .orElseThrow(() -> new SessionNotFoundException(votingToken));
    }

    public boolean checkPassword(Session session, String rawPassword) {
        return passwordService.matches(rawPassword, session.getPasswordHash());
    }

    /**
     * Looks up a previously created session by group ID + password so the
     * organizer can get back into it later without having kept the admin
     * link.
     */
    @Transactional(readOnly = true)
    public Optional<Session> loginByGroupIdAndPassword(String groupId, String rawPassword) {
        return sessionRepository.findByGroupId(groupId)
                .filter(session -> checkPassword(session, rawPassword));
    }
}
