package com.bingo.backend.controller;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.cloud.FirestoreClient;
import com.bingo.backend.dto.AuthRequest;
import com.bingo.backend.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @PostMapping("/google")
    public ResponseEntity<?> authenticateWithGoogle(@RequestBody AuthRequest request) {
        try {
            //Verify firebase id token from frontend
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();

            //Only allow um siswamail to login
            if (email == null || !email.endsWith("@siswa.um.edu.my")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "ACCESS_DENIED", "message", "Only UM siswamail accounts are allowed."));
            }

            //connect database to check user
            Firestore db = FirestoreClient.getFirestore();
            DocumentReference docRef = db.collection("users").document(uid);
            DocumentSnapshot document = docRef.get().get();

            User user;
            if (document.exists()) {
                //existed user: read from database
                user = document.toObject(User.class);
            } else {
                //new user: create new data
                user = new User(uid, name, email, "", 0, 0.0);
                docRef.set(user).get();
            }

            //return status and user info to frontend
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("user", user);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "INVALID_TOKEN", "message", e.getMessage()));
        }
    }
}
