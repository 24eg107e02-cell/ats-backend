package com.akshay.ats.chat;

import com.akshay.ats.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    public record MessageRequest(@NotBlank(message = "Please type a question") @Size(max = 500, message = "Keep questions under 500 characters") String message) {}

    private final ChatService chatService;

    @PostMapping("/message")
    public ResponseEntity<ChatService.ChatReply> message(@Valid @RequestBody MessageRequest request,
                                                         @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.reply(request.message(), user));
    }

    @PostMapping(value = "/resume-match", consumes = "multipart/form-data")
    public ResponseEntity<ChatService.ResumeMatchResponse> resumeMatch(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(chatService.matchResume(file));
    }
}
