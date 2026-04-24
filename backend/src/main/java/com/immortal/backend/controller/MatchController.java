package com.immortal.backend.controller;

import com.immortal.backend.model.Submission;
import com.immortal.backend.model.User;
import com.immortal.backend.repository.MatchRepository;
import com.immortal.backend.repository.UserRepository;
import com.immortal.backend.repository.SubmissionRepository;
import com.immortal.backend.service.CodeExecutionService;
import com.immortal.backend.service.MatchmakingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/match")
public class MatchController {

    private final MatchmakingService matchmakingService;
    private final CodeExecutionService codeExecutionService;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final SubmissionRepository submissionRepository;

    public MatchController(MatchmakingService matchmakingService, CodeExecutionService codeExecutionService, UserRepository userRepository, MatchRepository matchRepository, SubmissionRepository submissionRepository) {
        this.matchmakingService = matchmakingService;
        this.codeExecutionService = codeExecutionService;
        this.userRepository = userRepository;
        this.matchRepository = matchRepository;
        this.submissionRepository = submissionRepository;
    }

    @GetMapping("/find")
    public ResponseEntity<String> findMatch(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        matchmakingService.joinQueue(user.getId());
        return ResponseEntity.ok("Joined matchmaking queue");
    }

    @PostMapping("/submit")
    public ResponseEntity<String> submitCode(@RequestBody CodeSubmissionRequest request, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        var match = matchRepository.findById(request.matchId()).orElseThrow();

        Submission submission = new Submission();
        submission.setUser(user);
        submission.setMatch(match);
        submission.setCode(request.code());
        submission.setLanguage(request.language());
        submission.setStatus(Submission.SubmissionStatus.PENDING);
        
        submissionRepository.save(submission);

        new Thread(() -> codeExecutionService.evaluateSubmission(submission)).start();

        return ResponseEntity.ok("Code submitted for evaluation");
    }

    @GetMapping("/result/{matchId}")
    public ResponseEntity<?> getMatchResult(@PathVariable Long matchId) {
        var match = matchRepository.findById(matchId).orElseThrow();
        return ResponseEntity.ok(match);
    }
}

record CodeSubmissionRequest(Long matchId, String code, String language) {}
