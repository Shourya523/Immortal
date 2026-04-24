package com.immortal.backend.service;

import com.immortal.backend.model.Match;
import com.immortal.backend.model.Problem;
import com.immortal.backend.model.User;
import com.immortal.backend.repository.MatchRepository;
import com.immortal.backend.repository.ProblemRepository;
import com.immortal.backend.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

@Service
public class MatchmakingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private static final Logger log = Logger.getLogger(MatchmakingService.class.getName());

    private static final String MATCHMAKING_QUEUE_KEY = "matchmaking_queue";
    private static final int ELO_THRESHOLD = 200;

    public MatchmakingService(RedisTemplate<String, String> redisTemplate, MatchRepository matchRepository, UserRepository userRepository, ProblemRepository problemRepository, SimpMessagingTemplate messagingTemplate) {
        this.redisTemplate = redisTemplate;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public void joinQueue(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        redisTemplate.opsForZSet().add(MATCHMAKING_QUEUE_KEY, String.valueOf(userId), user.getRating());
        log.info("User " + userId + " joined the matchmaking queue.");
    }

    public void leaveQueue(Long userId) {
        redisTemplate.opsForZSet().remove(MATCHMAKING_QUEUE_KEY, String.valueOf(userId));
        log.info("User " + userId + " left the matchmaking queue.");
    }

    @Scheduled(fixedRate = 3000)
    public void matchPlayers() {
        Set<String> queue = redisTemplate.opsForZSet().range(MATCHMAKING_QUEUE_KEY, 0, -1);
        if (queue == null || queue.size() < 2) return;

        String[] playerIds = queue.toArray(new String[0]);
        for (int i = 0; i < playerIds.length - 1; i++) {
            if (playerIds[i] == null) continue;
            
            Long p1Id = Long.parseLong(playerIds[i]);
            Double p1Score = redisTemplate.opsForZSet().score(MATCHMAKING_QUEUE_KEY, playerIds[i]);

            for (int j = i + 1; j < playerIds.length; j++) {
                if (playerIds[j] == null) continue;
                
                Long p2Id = Long.parseLong(playerIds[j]);
                Double p2Score = redisTemplate.opsForZSet().score(MATCHMAKING_QUEUE_KEY, playerIds[j]);

                if (Math.abs(p1Score - p2Score) <= ELO_THRESHOLD) {
                    createMatch(p1Id, p2Id);
                    
                    redisTemplate.opsForZSet().remove(MATCHMAKING_QUEUE_KEY, playerIds[i], playerIds[j]);
                    playerIds[i] = null;
                    playerIds[j] = null;
                    break;
                }
            }
        }
    }

    private void createMatch(Long player1Id, Long player2Id) {
        User p1 = userRepository.findById(player1Id).orElseThrow();
        User p2 = userRepository.findById(player2Id).orElseThrow();

        Optional<Problem> problemOpt = problemRepository.findById(1L);
        if (problemOpt.isEmpty()) {
            log.warning("No problems available for match.");
            return;
        }

        Match match = new Match();
        match.setPlayer1(p1);
        match.setPlayer2(p2);
        match.setProblem(problemOpt.get());
        match.setStatus(Match.MatchStatus.ONGOING);
        match.setStartTime(LocalDateTime.now());
        
        matchRepository.save(match);
        log.info("Match created between " + p1.getUsername() + " and " + p2.getUsername());

        notifyPlayer(p1.getUsername(), match);
        notifyPlayer(p2.getUsername(), match);
    }

    private void notifyPlayer(String username, Match match) {
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/match",
                "Match Found! Match ID: " + match.getId()
        );
    }
}
