package com.tourya.api.config.auth;

import com.tourya.api._utils.Utils;
import com.tourya.api.config.auth.request.AuthenticationRequest;
import com.tourya.api.config.auth.request.SocialAuthRequest;
import com.tourya.api.config.auth.request.RegistrationRequest;
import com.tourya.api.config.auth.response.AuthenticationResponse;
import com.tourya.api.config.security.JwtService;
import com.tourya.api.constans.enums.EmailTemplateNameEnum;
import com.tourya.api.exceptions.EmailAlreadyExistsException;
import com.tourya.api.exceptions.EmailInvalidFormatException;
import com.tourya.api.models.Token;
import com.tourya.api.models.User;
import com.tourya.api.models.responses.MetaResponse;
import com.tourya.api.repository.RoleRepository;
import com.tourya.api.repository.TokenRepository;
import com.tourya.api.repository.UserRepository;
import com.tourya.api.services.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final TokenRepository tokenRepository;
    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;

    public void register(RegistrationRequest request) throws MessagingException {
        if (!Utils.isValidEmail(request.getEmail())) {
            throw new EmailInvalidFormatException("Invalid email format: " + request.getEmail());
        }
        if (userRepository.findByEmail(request.getEmail().toLowerCase()).isPresent()) {
            throw new EmailAlreadyExistsException("Email address already exists: " + request.getEmail());
        }
        var userRole = roleRepository.findByName("USER")
                // todo - better exception handling
                .orElseThrow(() -> new IllegalStateException("ROLE USER was not initiated"));
        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .accountLocked(false)
                .enabled(false)
                .roles(List.of(userRole))
                .build();
        userRepository.save(user);
        sendValidationEmail(user);
    }

    private void sendValidationEmail(User user) throws MessagingException {
        var newToken = generateAndSaveActivationToken(user);
        String activationUrlFinal = activationUrl+newToken;
        emailService.sendEmail(
                user.getEmail(),
                user.fullName(),
                EmailTemplateNameEnum.ACTIVATE_ACCOUNT,
                activationUrlFinal,
                newToken,
                "Account activation"
        );
    }
    private String generateAndSaveActivationToken(User user) {
        // Generate a token
        String generatedToken = generateActivationCode(6);
        var token = Token.builder()
                .token(generatedToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();
        tokenRepository.save(token);

        return generatedToken;
    }

    private String generateActivationCode(int length) {
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();

        SecureRandom secureRandom = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }

        return codeBuilder.toString();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var claims = new HashMap<String, Object>();
        var user = ((User) auth.getPrincipal());
        claims.put("fullName", user.fullName());

        var jwtToken = jwtService.generateToken(claims, (User) auth.getPrincipal());
        MetaResponse metaResponse = new MetaResponse();
        return AuthenticationResponse.builder()
                .meta(metaResponse)
                .fullName(user.fullName())
                .email(user.getEmail())
                .token(jwtToken)
                .build();
    }

    public void activateAccount(String token) throws MessagingException {
        Token savedToken = tokenRepository.findByToken(token)
                // todo exception has to be defined
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            sendValidationEmail(savedToken.getUser());
            throw new RuntimeException("Activation token has expired. A new token has been send to the same email address");
        }

        var user = userRepository.findById(savedToken.getUser().getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);

        savedToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(savedToken);
    }

    @Transactional
    public AuthenticationResponse authenticateWithSocial(SocialAuthRequest request){
        if (!Utils.isValidEmail(request.getEmail())) {
            throw new EmailInvalidFormatException("Invalid email format: " + request.getEmail());
        }
        User user = userRepository.findByEmail(request.getEmail().toLowerCase()).orElse(null);
        if (user == null) {
            // Registrar al usuario
            var userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new IllegalStateException("ROLE USER was not initiated"));
            String tempPassword = generateTemporaryPassword();
            System.out.println("tempPassword : " +tempPassword);
            User newUser = User.builder()
                    .firstname(request.getFirstname())
                    .lastname(request.getLastname())
                    .email(request.getEmail().toLowerCase())
                    .password(passwordEncoder.encode(tempPassword))
                    .accountLocked(false)
                    .enabled(true) // Google ya verificó el correo electrónico
                    .roles(List.of(userRole))
                    .uuidSocial(request.getUuidSocial())
                    .build();
            userRepository.save(newUser);

            var claims = new HashMap<String, Object>();
            claims.put("fullName", newUser.fullName());

            var jwtToken = jwtService.generateToken(claims, newUser);
            MetaResponse metaResponse = new MetaResponse();
            return AuthenticationResponse.builder()
                    .meta(metaResponse)
                    .fullName(newUser.fullName())
                    .email(newUser.getEmail())
                    .token(jwtToken)
                    .build();
        } else if (!user.isEnabled()) {
            user.setEnabled(true);
            userRepository.save(user);

            var claims = new HashMap<String, Object>();
            claims.put("fullName", user.fullName());

            var jwtToken = jwtService.generateToken(claims, user);
            MetaResponse metaResponse = new MetaResponse();
            return AuthenticationResponse.builder()
                    .meta(metaResponse)
                    .fullName(user.fullName())
                    .email(user.getEmail())
                    .token(jwtToken)
                    .build();
        }

        // El usuario ya existe y está habilitado
        var claims = new HashMap<String, Object>();
        claims.put("fullName", user.fullName());

        var jwtToken = jwtService.generateToken(claims, user);
        MetaResponse metaResponse = new MetaResponse();
        return AuthenticationResponse.builder()
                .meta(metaResponse)
                .fullName(user.fullName())
                .email(user.getEmail())
                .token(jwtToken)
                .build();
    }
    private String generateTemporaryPassword() {
        // Generar una contraseña temporal segura
        return UUID.randomUUID().toString().substring(0, 16); // Ejemplo
    }
    public void sendEmailTest(){
        emailService.sendSimpleMessage("eowkin@gmail.com",  "test-email", "Prueba de email");
    }
}
