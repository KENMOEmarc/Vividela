package com.vivid.service.impl;

import com.vivid.exception.PasswordMismatchException;
import com.vivid.exception.UserAlreadyExistsException;
import com.vivid.model.payloads.requests.RegisterRequest;
import com.vivid.model.payloads.requests.UserFormRequest;
import com.vivid.model.dto.UserDto;
import com.vivid.model.entity.User;
import com.vivid.model.enums.RoleType;
import com.vivid.model.mapper.UserMapper;
import com.vivid.repository.UserRepository;
import com.vivid.service.NotificationService;
import com.vivid.service.UserService;
import com.vivid.common.TransactionUtils;
import com.vivid.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    // ── Inscription publique ───────────────────────────────────────────────
    private final OrderRepository orderRepository;

    // ── Lecture ────────────────────────────────────────────────────────────

    @Override
    public UserDto register(RegisterRequest request) {
        log.info("Tentative d'inscription pour l'email: {}", request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "Un compte existe déjà avec l'email: " + request.getEmail());
        }
        if (userRepository.existsByUserNameIgnoreCase(request.getUserName())) {
            throw new UserAlreadyExistsException(
                    "Le nom d'utilisateur '" + request.getUserName() + "' est déjà pris");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException("Les mots de passe ne correspondent pas");
        }

        User user = User.builder()
                .userName(request.getUserName().toLowerCase().trim())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .role(RoleType.CUSTOMER)
                .isActive(true)
                .createdAt(Instant.now())
                .loyaltyPoints(0)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Utilisateur inscrit. ID: {}, username: {}, role: {}", savedUser.getId(), savedUser.getUserName(), savedUser.getRole());
        return toDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto findByUserName(String userName) {
        return toDto(userRepository.findByUserNameIgnoreCase(userName).orElseThrow(() -> new UsernameNotFoundException(
                "Utilisateur non trouvé avec le nom : " + userName
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto findByEmail(String email) {
        return toDto(userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouvé avec l'email: " + email)));
    }

    // ── CRUD Admin ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserDto findById(Long id) {
        return toDto(userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouvé avec l'ID: " + id)));
    }

    @Override
    public UserDto create(UserFormRequest request, Long currentUserId) {
        log.info("Création admin d'un utilisateur: {}", request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "Un compte existe déjà avec l'email: " + request.getEmail());
        }
        if (userRepository.existsByUserNameIgnoreCase(request.getUserName())) {
            throw new UserAlreadyExistsException(
                    "Le nom d'utilisateur '" + request.getUserName() + "' est déjà pris");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new PasswordMismatchException("Le mot de passe est requis pour la création");
        }

        RoleType role = request.getRole() != null ? request.getRole() : RoleType.CUSTOMER;

        // BUGFIX : l'ancienne version résolvait le "créateur" via request.getUserId(),
        // un champ jamais renseigné par le frontend → userRepository.findById(null)
        // levait systématiquement une exception et cassait la création d'utilisateur.
        // On résout désormais le créateur depuis l'utilisateur réellement authentifié
        // (currentUserId, fourni par le contrôleur via le token JWT), et on tolère
        // son absence (creator optionnel, ex: tout premier admin du système).
        User creator = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;

        // RÈGLE MÉTIER : un utilisateur ne peut attribuer qu'un rôle inférieur ou égal
        // au sien (ADMIN > MANAGER > EMPLOYEE > CUSTOMER). Concrètement :
        //   - seul un ADMIN peut créer un autre ADMIN ;
        //   - seul un ADMIN ou un MANAGER peut créer un autre MANAGER ;
        //   - un EMPLOYEE ne peut créer que des EMPLOYEE/CUSTOMER.
        // (Défense en profondeur : @PreAuthorize côté contrôleur restreint déjà cet
        // endpoint à ADMIN/MANAGER/EMPLOYEE ; cette vérification empêche en plus un
        // rôle inférieur de s'auto-élever ou d'élever un tiers au-delà de son niveau.)
        if (roleRank(role) > roleRank(creator != null ? RoleType.valueOf(creator.getRole().toString()) : null)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Vous n'avez pas les droits pour attribuer le rôle " + role);
        }

        User user = User.builder()
                .userName(request.getUserName().toLowerCase().trim())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .role(role)
                .isActive(request.isEnabled())
                .createdBy(creator)
                .build();

        User saved = userRepository.save(user);
        log.info("Utilisateur créé par admin. ID: {}, role: {}", saved.getId(), saved.getRole());
        return toDto(saved);
    }

    @Override
    public UserDto update(Long id, UserFormRequest request, Long currentUserId) {
        log.info("Mise à jour de l'utilisateur ID: {}", id);

        User user = findEntityById(id);

        // Unicité email (exclut l'utilisateur courant)
        if (!user.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "Un compte existe déjà avec l'email: " + request.getEmail());
        }
        // Unicité username (exclut l'utilisateur courant)
        if (!user.getUserName().equalsIgnoreCase(request.getUserName())
                && userRepository.existsByUserNameIgnoreCase(request.getUserName())) {
            throw new UserAlreadyExistsException(
                    "Le nom d'utilisateur '" + request.getUserName() + "' est déjà pris");
        }

        // RÈGLE MÉTIER : mêmes règles de hiérarchie que pour la création (voir create()).
        // On empêche également de modifier un compte dont le rôle actuel est déjà
        // supérieur au rôle de l'appelant (ex : un MANAGER ne peut pas modifier un ADMIN).
        User currentUser = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;
        RoleType callerRole = currentUser != null ? RoleType.valueOf(currentUser.getRole().toString().toUpperCase()) : null;
        RoleType targetCurrentRole = RoleType.valueOf(user.getRole().toString().toUpperCase());
        RoleType targetNewRole = request.getRole() != null ? request.getRole() : targetCurrentRole;

        if (roleRank(targetCurrentRole) > roleRank(callerRole) || roleRank(targetNewRole) > roleRank(callerRole)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Vous n'avez pas les droits pour modifier ce compte ou lui attribuer ce rôle");
        }

        // RÈGLE MÉTIER : seul un ADMIN peut changer le mot de passe d'un compte
        // existant. Un MANAGER ou un EMPLOYEE modifiant une fiche utilisateur ne
        // doit pas pouvoir en profiter pour réinitialiser/changer son mot de
        // passe, même le sien (défense en profondeur : le champ n'est de toute
        // façon jamais pré-rempli côté frontend hors rôle ADMIN).
        if (request.getPassword() != null && !request.getPassword().isBlank()
                && callerRole != RoleType.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Seul un administrateur peut modifier le mot de passe d'un utilisateur");
        }

        user.setUserName(request.getUserName().toLowerCase().trim());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPhone(request.getPhone().trim());
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setCreatedAt((request.getCreatedAt() == null) ? Instant.now() : request.getCreatedAt());
        user.setIsActive(request.isEnabled());

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        // Mise à jour du mot de passe uniquement si fourni
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return toDto(userRepository.save(user));
    }

    @Override
    public void delete(Long id, Long currentUserId) {
        log.info("Suppression de l'utilisateur ID: {}", id);
        User user = findEntityById(id);

        // AJOUT : garde-fous manquants sur delete() — voir revue de code,
        // règle manquante n°1 (section Utilisateurs / Clients) :
        // ni auto-suppression, ni suppression du dernier ADMIN, ni présence
        // de commandes/paiements liés n'étaient vérifiés (contrairement à
        // deleteOrder() qui protège déjà explicitement l'historique comptable).

        // 1) Un utilisateur ne peut pas se supprimer lui-même.
        if (currentUserId != null && currentUserId.equals(id)) {
            throw new IllegalStateException("Vous ne pouvez pas supprimer votre propre compte.");
        }

        // 2) Impossible de supprimer le dernier compte ADMIN restant.
        if (RoleType.ADMIN.toString().equalsIgnoreCase(user.getRole().toString())) {
            long remainingAdmins = userRepository.findByRoleIn(List.of(RoleType.ADMIN.toString())).stream()
                    .filter(u -> !u.getId().equals(id))
                    .count();
            if (remainingAdmins == 0) {
                throw new IllegalStateException(
                        "Impossible de supprimer ce compte : il s'agit du dernier administrateur du système.");
            }
        }

        // 3) Impossible de supprimer un utilisateur ayant des commandes liées
        // (préserve l'historique comptable, même logique que deleteOrder()).
        if (!orderRepository.findByClientUserId(id).isEmpty()) {
            throw new IllegalStateException(
                    "Impossible de supprimer cet utilisateur : il possède des commandes enregistrées. "
                            + "Désactivez son compte (isActive=false) plutôt que de le supprimer, afin de "
                            + "conserver l'historique comptable.");
        }

        userRepository.delete(user);
    }

    /**
     * Rang hiérarchique d'un rôle, utilisé pour comparer les droits entre le
     * créateur/modificateur d'un compte et le rôle cible attribué :
     * ADMIN(4) > MANAGER(3) > EMPLOYEE(2) > CUSTOMER(1) > aucun rôle connu(0).
     */
    private int roleRank(RoleType role) {
        if (role == null) {
            return 0;
        }
        return switch (role) {
            case ADMIN -> 4;
            case MANAGER -> 3;
            case EMPLOYEE -> 2;
            case CUSTOMER -> 1;
        };
    }

    // ── Gestion des clients (page "Clients" du dashboard) ────────────────────

    @Override
    public List<UserDto> findAllCustomer() {
        return findAll().stream()
                .filter(userDto -> userDto.role().equals(RoleType.CUSTOMER))
                .toList();
    }

    @Override
    public UserDto createCustomer(UserFormRequest request, Long currentUserId) {
        log.info("Création d'un client: {}", request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "Un compte existe déjà avec l'email: " + request.getEmail());
        }
        // AJOUT : unicité du téléphone — voir revue de code, règle manquante
        // n°2 (section Utilisateurs / Clients). resolveCustomer()
        // (OrderServiceImpl) utilise le téléphone comme critère de
        // recherche ; un doublon rendrait la résolution du client ambiguë.
        if (hasText(request.getPhone()) && userRepository.existsByPhone(request.getPhone().trim())) {
            throw new UserAlreadyExistsException(
                    "Un compte existe déjà avec le téléphone: " + request.getPhone());
        }

        User creator = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;

        User user = User.builder()
                .userName(generateUniqueUserName(request.getFirstName(), request.getLastName()))
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone().trim())
                // Compte créé par le personnel : mot de passe temporaire aléatoire.
                // Le client utilisera la procédure "mot de passe oublié" pour en définir un.
                .password(passwordEncoder.encode(generateRandomPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .role(RoleType.CUSTOMER)
                .isActive(true)
                .createdBy(creator)
                .loyaltyPoints(0)
                .build();

        User saved = userRepository.save(user);
        log.info("Client créé. ID: {}, username généré: {}", saved.getId(), saved.getUserName());

        // CORRECTION : le client n'était jamais notifié de la création de son
        // compte (ni email, ni SMS, ni notification in-app). Voir revue de
        // code — "createCustomer ne notifie jamais le client créé".
        TransactionUtils.runAfterCommit(() -> notificationService.notifyClientCreated(saved));

        return toDto(saved);
    }

    @Override
    public UserDto updateCustomerInfo(Long id, UserFormRequest request) {
        log.info("Mise à jour du client ID: {}", id);

        User user = findEntityById(id);

        if (!user.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "Un compte existe déjà avec l'email: " + request.getEmail());
        }
        if (hasText(request.getPhone()) && !request.getPhone().trim().equalsIgnoreCase(user.getPhone())
                && userRepository.existsByPhone(request.getPhone().trim())) {
            throw new UserAlreadyExistsException(
                    "Un compte existe déjà avec le téléphone: " + request.getPhone());
        }

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPhone(request.getPhone().trim());

        return toDto(userRepository.save(user));
    }

    /**
     * Génère un nom d'utilisateur unique à partir du prénom/nom
     * (ex: "jean.dupont", puis "jean.dupont2" en cas de collision).
     */
    private String generateUniqueUserName(String firstName, String lastName) {
        String base = (firstName.trim() + "." + lastName.trim())
                .toLowerCase()
                .replaceAll("[^a-z0-9.]", "");
        if (base.isBlank()) {
            base = "client";
        }
        String candidate = base;
        int suffix = 2;
        while (userRepository.existsByUserNameIgnoreCase(candidate)) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }

    private String generateRandomPassword() {
        // 16 caractères aléatoires — jamais communiqué tel quel à l'utilisateur,
        // simplement nécessaire car User.password est NOT NULL.
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Récupère l'entité User brute (utilisée en interne pour les opérations
     * de modification/suppression qui ont besoin de l'entité complète).
     */
    private User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouvé avec l'ID: " + id));
    }

    /**
     * Convertit une entité User en UserDto via le mapper.
     */
    private UserDto toDto(User user) {
        return UserMapper.toDto(user);
    }
}