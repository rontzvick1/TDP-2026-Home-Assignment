package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing and resolving @mentions in text.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MentionService {

    private final UserRepository userRepository;

    // Pattern to match @username (alphanumeric and underscore)
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_]+)");

    /**
     * Parses the given text for @usernames and returns a set of matching {@link User} entities.
     * Invalid or non-existent usernames are safely ignored.
     *
     * @param text the content to parse (e.g., from a Comment)
     * @return a Set of valid User entities found in the text
     */
    public Set<User> extractMentionedUsers(String text) {
        Set<User> mentionedUsers = new HashSet<>();
        
        if (text == null || text.isBlank()) {
            return mentionedUsers;
        }

        Matcher matcher = MENTION_PATTERN.matcher(text);
        Set<String> uniqueUsernames = new HashSet<>();

        while (matcher.find()) {
            // Group 1 is the part after the '@'
            uniqueUsernames.add(matcher.group(1));
        }

        for (String username : uniqueUsernames) {
            userRepository.findByUsername(username).ifPresent(mentionedUsers::add);
        }

        return mentionedUsers;
    }
}
